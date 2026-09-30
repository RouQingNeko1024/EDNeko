/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.Reach
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isFaced
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.searchCenter
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.minecraft.entity.Entity
import org.lwjgl.input.Mouse
import kotlin.math.atan
import java.util.Random

object GunHack : Module("GunHack", Category.COMBAT) {

    private val range by float("Range", 4.4F, 1F..256F)
    private val horizontalAim by boolean("HorizontalAim", true)
    private val verticalAim by boolean("VerticalAim", true)
    private val predictClientMovement by int("PredictClientMovement", 2, 0..5)
    private val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, -1f..2f)

    private val highestBodyPointToTargetValue = choices(
        "HighestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Head"
    ) {
        verticalAim
    }.onChange { _, new ->
        val newPoint = RotationUtils.BodyPoint.fromString(new)
        val lowestPoint = RotationUtils.BodyPoint.fromString(lowestBodyPointToTarget)
        val coercedPoint = RotationUtils.coerceBodyPoint(newPoint, lowestPoint, RotationUtils.BodyPoint.HEAD)
        coercedPoint.displayName
    }

    private val highestBodyPointToTarget: String by highestBodyPointToTargetValue

    private val lowestBodyPointToTargetValue = choices(
        "LowestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Feet"
    ) {
        verticalAim
    }.onChange { _, new ->
        val newPoint = RotationUtils.BodyPoint.fromString(new)
        val highestPoint = RotationUtils.BodyPoint.fromString(highestBodyPointToTarget)
        val coercedPoint = RotationUtils.coerceBodyPoint(newPoint, RotationUtils.BodyPoint.FEET, highestPoint)
        coercedPoint.displayName
    }

    private val lowestBodyPointToTarget: String by lowestBodyPointToTargetValue

    private val horizontalBodySearchRange by floatRange("HorizontalBodySearchRange", 0f..1f, 0f..1f) { horizontalAim }

    private val fov by float("FOV", 180F, 1F..180F)
    private val lock by boolean("Lock", true) { horizontalAim || verticalAim }
    private val center by boolean("Center", false)
    private val headLock by boolean("Headlock", false) { center && lock }
    private val headLockBlockHeight by float("HeadBlockHeight", -1f, -2f..0f) { headLock && center && lock }
    private val breakBlocks by boolean("BreakBlocks", true)

    // 静默瞄准开关
    private val silentAim by boolean("SilentAim", false)

    // 是否在按住左键时停止移动
    private val stopMove by boolean("StopMove", true)

    // 自动点击右键
    private val autoRightClick by boolean("AutoRightClick", false)

    // 视角恢复延迟（毫秒范围）
    private val restoreDelay by intRange("RestoreDelay", 50..200, 0..1000)

    // 静默旋转设置
    private val rotationSettings = RotationSettings(this).withoutKeepRotation()

    // 左键状态
    private var leftClickDown = false

    // 视角恢复相关（仅非静默时使用）
    private var originalYaw = 0f
    private var originalPitch = 0f
    private var restoreAt = 0L
    private val random = Random()

    override fun onDisable() {
        leftClickDown = false
        restoreAt = 0L
        originalYaw = 0f
        originalPitch = 0f
    }

    val onGameTick = handler<GameTickEvent> {
        val previous = leftClickDown
        leftClickDown = Mouse.isButtonDown(0)

        // 自动右键：按下和松开各点一次
        if (autoRightClick) {
            if (leftClickDown && !previous) {
                mc.rightClickMouse()
            } else if (!leftClickDown && previous) {
                mc.rightClickMouse()
            }
        }

        // 仅在非静默模式下记录原始视角并安排恢复
        if (!silentAim) {
            if (leftClickDown && !previous) {
                originalYaw = mc.thePlayer?.rotationYaw ?: 0f
                originalPitch = mc.thePlayer?.rotationPitch ?: 0f
                restoreAt = 0L
            } else if (!leftClickDown && previous) {
                val delayRange = restoreDelay.last - restoreDelay.first + 1
                val delay = restoreDelay.first + random.nextInt(delayRange)
                restoreAt = System.currentTimeMillis() + delay
            }
        }
    }

    val onMotion = handler<MotionEvent> { event ->
        if (event.eventState != EventState.POST) return@handler

        val player = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler

        // 未按住左键
        if (!leftClickDown) {
            // 非静默模式：延迟恢复视角
            if (!silentAim && restoreAt > 0 && System.currentTimeMillis() >= restoreAt) {
                player.rotationYaw = originalYaw
                player.rotationPitch = originalPitch
                restoreAt = 0L
            }
            return@handler
        }

        // 左键按下：寻找目标并瞄准
        val entity = world.loadedEntityList.filter {
            Backtrack.runWithNearestTrackedDistance(it) {
                isSelected(
                    it,
                    true
                ) && player.canEntityBeSeen(it) && player.getDistanceToEntityBox(it) <= range && rotationDifference(it) <= fov
            }
        }.minByOrNull { player.getDistanceToEntityBox(it) } ?: return@handler

        if (!lock && isFaced(entity, range.toDouble())) return@handler

        findRotation(entity)
    }

    // 移动输入处理：按住左键时停止移动
    val onMovementInput = handler<MovementInputEvent> { event ->
        if (stopMove && leftClickDown) {
            event.originalInput.moveStrafe = 0f
            event.originalInput.moveForward = 0f
        }
    }

    private fun findRotation(entity: Entity) {
        val player = mc.thePlayer ?: return

        if (mc.playerController.isHittingBlock && breakBlocks) return

        val prediction = entity.currPos.subtract(entity.prevPos).times(2 + predictEnemyPosition.toDouble())

        val boundingBox = entity.hitBox.offset(prediction)
        val (currPos, oldPos) = player.currPos to player.prevPos

        val simPlayer = SimulatedPlayer.fromClientPlayer(RotationUtils.modifiedInput)
        repeat(predictClientMovement) {
            simPlayer.tick()
        }

        player.setPosAndPrevPos(simPlayer.pos)

        val destinationRotation = if (center) {
            toRotation(boundingBox.center, true)
        } else {
            searchCenter(
                boundingBox,
                false,
                outborder = false,
                predict = true,
                lookRange = range,
                attackRange = if (Reach.handleEvents()) Reach.combatReach else 3f,
                bodyPoints = listOf(highestBodyPointToTarget, lowestBodyPointToTarget),
                horizontalSearch = horizontalBodySearchRange
            )
        } ?: return

        // 头部锁定偏移
        if (headLock && center && lock) {
            val distance = player.getDistanceToEntityBox(entity)
            val playerEyeHeight = player.eyeHeight
            val blockHeight = headLockBlockHeight
            val pitchOffset = Math.toDegrees(atan((blockHeight + playerEyeHeight) / distance)).toFloat()
            destinationRotation.pitch -= pitchOffset
        }

        // 应用旋转
        if (silentAim) {
            // 静默：服务器旋转，玩家视角不变
            setTargetRotation(destinationRotation, rotationSettings, 1)
        } else {
            // 非静默：直接修改客户端视角
            destinationRotation.toPlayer(player, horizontalAim, verticalAim)
        }

        player.setPosAndPrevPos(currPos, oldPos)
    }
}
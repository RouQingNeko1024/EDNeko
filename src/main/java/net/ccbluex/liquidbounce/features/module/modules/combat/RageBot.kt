package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.MovementInputEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.rotation.RandomizationSettings
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.BodyPoint
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.coerceBodyPoint
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isEntityHeightVisible
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.modifiedInput
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.searchCenter
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color

object RageBot : Module("RageBot", Category.COMBAT) {

    private val aimRange by int("瞄准距离", 50, 1..100)
    private val throughWalls by boolean("穿墙瞄准", false)
    private val shootMode by choices("射击方式", arrayOf("右键", "左键"), "右键")
    private val shootInterval by int("射击间隔", 5, 0..40)
    
    private val onlyOnGround by boolean("只在地面时射击", false)
    private val onlyOnSneak by boolean("只在蹲下时开火", false)
    
    private val stopMovement by boolean("停止移动", false)
    private val stopMovementTicks by int("射击前停止移动时间", 1, 0..20) { stopMovement }
    
    private val sneakOnShoot by boolean("开火时下蹲", false)
    private val sneakTicks by int("射击前下蹲时间", 1, 0..20) { sneakOnShoot }
    
    private val predictClientMovement by int("预测玩家移动", 0, 0..5)
    private val predictOnlyWhenOutOfRange by boolean("仅超出范围时预测", false) { predictClientMovement != 0 }
    private val predictEnemyPosition by float("预测敌人位置", 1.5f, -1f..2f)
    
    private val options = RotationSettings(this).withoutKeepRotation()
    private val randomization = RandomizationSettings(this) { options.rotationsActive }
    
    private val highestBodyPointToTarget by choices(
        "瞄准部位上限", arrayOf("头部", "身体", "脚部"), "头部"
    ) { options.rotationsActive }
    
    private val lowestBodyPointToTarget by choices(
        "瞄准部位下限", arrayOf("头部", "身体", "脚部"), "脚部"
    ) { options.rotationsActive }
    
    private val horizontalBodySearchRange by floatRange(
        "瞄准身体范围", 0f..1f, 0f..1f
    ) { options.rotationsActive }
    
    private val markTarget by boolean("标记目标", true)
    private val markColor = ColorSettingsInteger(this, "标记颜色") { markTarget }.with(Color.RED)
    
    private var target: EntityLivingBase? = null
    private var shootTicks = 0
    private var stopTicks = 0
    private var isStopping = false
    private var shouldBlockInput = false
    private var sneakCountdown = 0
    private var isSneaking = false
    private var shouldSneak = false

    val onTick = handler<GameTickEvent> {
        if (mc.thePlayer == null || mc.theWorld == null) return@handler
        
        if (onlyOnGround && !mc.thePlayer.onGround) {
            clearTarget()
            return@handler
        }
        
        if (onlyOnSneak && !mc.thePlayer.isSneaking) {
            clearTarget()
            return@handler
        }
        
        target = findTarget()
        
        if (target == null) {
            clearTarget()
            return@handler
        }
        
        val canShoot = isEntityHeightVisible(target!!)
        
        if (options.rotationsActive) {
            aimAtTarget()
        }
        
        if (stopMovement && canShoot) {
            if (!isStopping) {
                isStopping = true
                stopTicks = stopMovementTicks
                shouldBlockInput = true
            }
            
            if (stopTicks > 0) {
                stopTicks--
                return@handler
            }
        }
        
        if (sneakOnShoot && canShoot) {
            if (!isSneaking) {
                isSneaking = true
                sneakCountdown = sneakTicks
                shouldSneak = true
            }
            
            if (sneakCountdown > 0) {
                sneakCountdown--
                return@handler
            }
        }
        
        shouldBlockInput = false
        shouldSneak = false
        
        shootTicks++
        if (shootTicks >= shootInterval && canShoot) {
            shoot()
            shootTicks = 0
            if (stopMovement) {
                isStopping = false
                stopTicks = stopMovementTicks
            }
            if (sneakOnShoot) {
                isSneaking = false
                sneakCountdown = sneakTicks
            }
        }
    }

    val onMovementInput = handler<MovementInputEvent> {
        if (shouldBlockInput) {
            it.originalInput.moveForward = 0f
            it.originalInput.moveStrafe = 0f
            it.originalInput.jump = false
            it.originalInput.sneak = false
        }
        if (shouldSneak) {
            it.originalInput.sneak = true
        }
    }

    val onRender3D = handler<Render3DEvent> {
        if (markTarget && target != null) {
            val color = markColor.color()
            drawEntityBox(target!!, Color(color.red, color.green, color.blue, color.alpha), true)
        }
    }

    private fun findTarget(): EntityLivingBase? {
        val player = mc.thePlayer ?: return null
        val world = mc.theWorld ?: return null
        
        var nearestTarget: EntityLivingBase? = null
        var nearestDistance = Double.MAX_VALUE
        
        for (entity in world.loadedEntityList) {
            if (entity !is EntityLivingBase) continue
            if (entity == player) continue
            if (entity.isDead || !entity.isEntityAlive) continue
            if (entity is EntityPlayer) {
                if (isBot(entity)) continue
                if (entity.isClientFriend()) continue
            }
            if (!isSelected(entity, false)) continue
            
            val distance = player.getDistanceToEntity(entity).toDouble()
            val maxRange = aimRange.toDouble()
            
            if (distance > maxRange) continue
            
            if (!throughWalls && !isEntityHeightVisible(entity)) continue
            
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearestTarget = entity
            }
        }
        
        return nearestTarget
    }

    private fun aimAtTarget() {
        val targetEntity = target ?: return
        val player = mc.thePlayer ?: return
        
        val prediction = targetEntity.currPos.subtract(targetEntity.prevPos).times(2 + predictEnemyPosition.toDouble())
        val targetBox = targetEntity.entityBoundingBox.offset(prediction)
            .expand(horizontalBodySearchRange.start.toDouble(), 0.0, horizontalBodySearchRange.start.toDouble())
            .expand(horizontalBodySearchRange.endInclusive.toDouble(), 0.0, horizontalBodySearchRange.endInclusive.toDouble())
        
        val highestPoint = when (highestBodyPointToTarget) {
            "头部" -> BodyPoint.HEAD
            "身体" -> BodyPoint.BODY
            else -> BodyPoint.FEET
        }
        
        val lowestPoint = when (lowestBodyPointToTarget) {
            "头部" -> BodyPoint.HEAD
            "身体" -> BodyPoint.BODY
            else -> BodyPoint.FEET
        }
        
        val bodyPoints = listOf(
            coerceBodyPoint(highestPoint, lowestPoint, BodyPoint.HEAD).displayName,
            coerceBodyPoint(lowestPoint, BodyPoint.FEET, highestPoint).displayName
        )
        
        val (currPos, oldPos) = player.currPos to player.prevPos
        
        val simPlayer = SimulatedPlayer.fromClientPlayer(modifiedInput)
        simPlayer.rotationYaw = (currentRotation ?: player.rotation).yaw
        
        var pos = currPos
        
        repeat(predictClientMovement) {
            val previousPos = simPlayer.pos
            
            simPlayer.tick()
            
            if (predictOnlyWhenOutOfRange) {
                player.setPosAndPrevPos(simPlayer.pos)
                
                val currDist = player.getDistanceToEntityBox(targetEntity)
                
                player.setPosAndPrevPos(previousPos)
                
                val prevDist = player.getDistanceToEntityBox(targetEntity)
                
                player.setPosAndPrevPos(currPos, oldPos)
                pos = simPlayer.pos
                
                if (currDist <= aimRange && currDist <= prevDist) {
                    return@repeat
                }
            }
            
            pos = previousPos
        }
        
        player.setPosAndPrevPos(pos)
        
        val rotation = searchCenter(
            bb = targetBox,
            distanceBasedSpot = false,
            outborder = false,
            randomization = if (randomization.randomizationChosen) randomization else null,
            predict = false,
            lookRange = aimRange.toFloat(),
            attackRange = aimRange.toFloat(),
            throughWallsRange = if (throughWalls) aimRange.toFloat() else 0f,
            bodyPoints = bodyPoints,
            horizontalSearch = horizontalBodySearchRange
        )
        
        player.setPosAndPrevPos(currPos, oldPos)
        
        if (rotation != null) {
            setTargetRotation(rotation, options)
        }
    }

    private fun shoot() {
        val player = mc.thePlayer ?: return
        
        when (shootMode) {
            "右键" -> {
                mc.playerController.sendUseItem(player, mc.theWorld, player.heldItem)
            }
            "左键" -> {
                mc.playerController.attackEntity(player, target)
            }
        }
    }

    private fun clearTarget() {
        target = null
        shootTicks = 0
        stopTicks = 0
        isStopping = false
        shouldBlockInput = false
        sneakCountdown = 0
        isSneaking = false
        shouldSneak = false
    }

    override fun onDisable() {
        clearTarget()
    }
}

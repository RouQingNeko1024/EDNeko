package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.attackEntityWithModifiedSprint
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0APacketAnimation
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.sqrt

object TimerRangeV2 : Module("TimerRangeV2", Category.COMBAT) {

    private val tagMode by choices(
        "TagMode",
        arrayOf("WorkRange", "IsWorking", "MinSpeed-MaxSpeed", "Custom"),
        "WorkRange"
    )
    private val customText by text("CustomTagText", "") { tagMode == "Custom" }
    private val workMode by choices("Mode", arrayOf("SlowFirst", "BoostFirst"), "BoostFirst")
    private val maxRange by float("MaxRange", 4.0f, 0.0f..8.0f).onChange { _, new -> new.coerceAtLeast(minRange) }
    private val minRange by float("MinRange", 3.0f, 0.0f..8.0f)
    private val boostTimer by float("BoostTimerSpeed", 2.0f, 1.0f..10.0f)
    private val boostTime by int("BoostTime", 100, 0..3000, "ms")
    private val slowTimer by float("SlowTimerSpeed", 0.5f, 0.01f..1.0f)
    private val slowTime by int("SlowTime", 100, 0..3000, "ms")
    private val cooldownTime by int("CooldownTime", 100, 0..3000, "ms")
    private val attackWhenBoosting by boolean("AttackWhenChangingTimer",false)
    private val attackTiming by multiChoices("AttackTiming",arrayOf("Boosting","Slowing"), setOf("Slowing")) {attackWhenBoosting}
    private val attackCount by int("TotalAttackCount",1,1..5) {attackWhenBoosting}
    private val onlyAttackWhenNotReachedCPSLimit by boolean("OnlyAttackWhenNotReachedCPSLimit",false) {attackWhenBoosting}
    private val CPSLimit by int("CPSLimit",20,1..100) {attackWhenBoosting && onlyAttackWhenNotReachedCPSLimit}
    private val attackMaxRange by float("AttackMaxRange",3.0f,0.0f..8.0f) {attackWhenBoosting}
    private val swingMode by choices("AttackSwingMode",arrayOf("Normal","Packet"),"Normal") {attackWhenBoosting}
    private val keepSprint by boolean("KeepSprint",false) {attackWhenBoosting}
    private val allowKeepSprintHurtTime by intRange("AllowKeepSprintHurtTime",0..10,0..10) {attackWhenBoosting && keepSprint}
    private val boostMotion by boolean("BoostMotion",false)
    private val boostTiming by multiChoices("BoostTiming",arrayOf("Boosting","Slowing"), setOf("Slowing")) {boostMotion}
    private val boostBoostingFactor by float("BoostFactorBoosting",0.0f,0.0f..2.0f) {"Boosting" in boostTiming && boostMotion}
    private val boostSlowingFactor by float("BoostFactorSlowing",0.0f,0.0f..2.0f) {"Slowing" in boostTiming && boostMotion}
    private val stopBoostingWhenHurting by boolean("StopBoostingWhenHurt",false)
    private val blinkOnWorking by boolean("BlinkOnBoosting",false)
    private val cancelC03 by boolean("CancelC03WhenWorking",false) {blinkOnWorking}
    private val onlyForward by boolean("OnlyForward",false)
    private val debugMessage by boolean("DebugMessage",false)
    private val safe by boolean("Safe", false)

    // 可视化预测系统
    private val visualPrediction by boolean("VisualPrediction", false)
    private val predictionBox by boolean("PredictionBox", true) { visualPrediction }
    private val predictionBoxColor by color("PredictionBoxColor", Color(255, 0, 0, 100)) { predictionBox }
    private val predictionLine by boolean("PredictionLine", true) { visualPrediction }
    private val predictionLineColor by color("PredictionLineColor", Color(255, 255, 0, 150)) { predictionLine }
    private val predictionLineWidth by float("PredictionLineWidth", 2.0f, 0.5f..5.0f) { predictionLine }
    private val showCurrentPos by boolean("ShowCurrentPos", true) { visualPrediction }
    private val currentPosColor by color("CurrentPosColor", Color(0, 255, 0, 100)) { showCurrentPos }
    private val predictionDuration by int("PredictionDuration", 200, 50..1000) { visualPrediction }

    private var isBoosting = false
    private var boostedTime = MSTimer()
    private var slowedTime = MSTimer()
    private var cooldownTimer = MSTimer()
    private var attackCounter = 0
    private var hasSlowed = false
    private var hasBoosted = false
    private var shouldBlink = false
    private var hasBlink = false
    private var predictedPlayerPosition: net.minecraft.util.Vec3? = null
    private var shouldShowPrediction = false

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler
        val timerChanged = mc.timer.timerSpeed == boostTimer || mc.timer.timerSpeed == slowTimer
        val target = KillAura.target ?: run {
            if (timerChanged) mc.timer.timerSpeed = 1.0f
            predictedPlayerPosition = null
            shouldShowPrediction = false
            return@handler
        }
        if (onlyForward && !mc.gameSettings.keyBindForward.isKeyDown) {
            predictedPlayerPosition = null
            shouldShowPrediction = false
            return@handler
        }

        if (stopBoostingWhenHurting && mc.thePlayer.hurtTime > 0 && mc.timer.timerSpeed == boostTimer) {
            mc.timer.timerSpeed = 1f
            if (debugMessage) chat("CancelledTimerChange")
            predictedPlayerPosition = null
            shouldShowPrediction = false
            return@handler
        }
        if (!KillAura.state) {
            if (timerChanged) mc.timer.timerSpeed = 1f
            predictedPlayerPosition = null
            shouldShowPrediction = false
            return@handler
        }

        val distance = player.getDistanceToEntityBox(target)

        if (distance !in minRange..maxRange) {
            if (timerChanged) mc.timer.timerSpeed = 1f
            predictedPlayerPosition = null
            shouldShowPrediction = false
            return@handler
        }

        // 检查是否开始BoostTimer
        val justStartedBoosting = !isBoosting && cooldownTimer.hasTimePassed(cooldownTime)

        if (justStartedBoosting) {
            isBoosting = true
            boostedTime.reset()
            slowedTime.reset()
            attackCounter = 0
            hasSlowed = false
            hasBoosted = false

            if (visualPrediction) {
                calculateBoostPrediction(player, target)
                shouldShowPrediction = true
            }
        }

        if (isBoosting) {
            when (workMode) {
                "BoostFirst" -> {
                    when {
                        !boostedTime.hasTimePassed(boostTime) -> {
                            if (blinkOnWorking) shouldBlink = true
                            mc.timer.timerSpeed = boostTimer
                            hasBoosted = true
                            slowedTime.reset()
                            debugMessage("Boosting")
                            if (attackWhenBoosting && "Boosting" in attackTiming) {
                                if (attackCounter < attackCount) {
                                    runAttack()
                                }
                            }
                            if (boostMotion && "Boosting" in boostTiming) {
                                player.motionX *= (boostBoostingFactor + 1.0)
                                player.motionZ *= (boostBoostingFactor + 1.0)
                            }
                        }
                        !slowedTime.hasTimePassed(slowTime) -> {
                            if (blinkOnWorking && shouldBlink) shouldBlink = false
                            mc.timer.timerSpeed = slowTimer
                            hasSlowed = true
                            debugMessage("Slowing")
                            if (attackWhenBoosting) {
                                if (attackCounter < attackCount && "Slowing" in attackTiming) {
                                    runAttack()
                                }
                            }
                            if (boostMotion && "Slowing" in boostTiming) {
                                player.motionX *= (boostSlowingFactor + 1.0)
                                player.motionZ *= (boostSlowingFactor + 1.0)
                            }
                            shouldShowPrediction = false
                        }
                        else -> {
                            if (safe && hasBoosted && !hasSlowed) {
                                if (blinkOnWorking && shouldBlink) shouldBlink = false
                                slowedTime.reset()
                                mc.timer.timerSpeed = slowTimer
                                hasSlowed = true
                                debugMessage("Safe mode: Forcing slow timer")
                            } else {
                                isBoosting = false
                                mc.timer.timerSpeed = 1f
                                cooldownTimer.reset()
                                attackCounter = 0
                                predictedPlayerPosition = null
                                shouldShowPrediction = false
                            }
                        }
                    }
                }
                "SlowFirst" -> {
                    when {
                        !slowedTime.hasTimePassed(slowTime) -> {
                            if (blinkOnWorking && shouldBlink) shouldBlink = false
                            mc.timer.timerSpeed = slowTimer
                            hasSlowed = true
                            boostedTime.reset()
                            debugMessage("Slowing")
                            if (attackWhenBoosting) {
                                if (attackCounter < attackCount && "Slowing" in attackTiming) {
                                    runAttack()
                                }
                            }
                            if (boostMotion && "Slowing" in boostTiming) {
                                player.motionX *= (boostSlowingFactor + 1.0)
                                player.motionZ *= (boostSlowingFactor + 1.0)
                            }
                        }
                        !boostedTime.hasTimePassed(boostTime) -> {
                            if (blinkOnWorking) shouldBlink = true
                            mc.timer.timerSpeed = boostTimer
                            hasBoosted = true
                            debugMessage("Boosting")

                            if (visualPrediction && !shouldShowPrediction) {
                                calculateBoostPrediction(player, target)
                                shouldShowPrediction = true
                            }

                            if (attackWhenBoosting) {
                                if (attackCounter < attackCount && "Boosting" in attackTiming) {
                                    runAttack()
                                }
                            }
                            if (boostMotion && "Boosting" in boostTiming) {
                                player.motionX *= (boostBoostingFactor + 1.0)
                                player.motionZ *= (boostBoostingFactor + 1.0)
                            }
                        }
                        else -> {
                            if (safe && hasBoosted && !hasSlowed) {
                                slowedTime.reset()
                                mc.timer.timerSpeed = slowTimer
                                hasSlowed = true
                                debugMessage("Safe mode: Forcing slow timer")
                            } else {
                                isBoosting = false
                                mc.timer.timerSpeed = 1f
                                cooldownTimer.reset()
                                attackCounter = 0
                                predictedPlayerPosition = null
                                shouldShowPrediction = false
                            }
                        }
                    }
                }
            }
        } else {
            if (mc.timer.timerSpeed != 1f) mc.timer.timerSpeed = 1f
            predictedPlayerPosition = null
            shouldShowPrediction = false
        }

        if (shouldShowPrediction && boostedTime.hasTimePassed(predictionDuration)) {
            shouldShowPrediction = false
        }
    }

    // 渲染预测可视化
    val onRender = handler<Render3DEvent> {
        if (!visualPrediction || !shouldShowPrediction) return@handler

        val player = mc.thePlayer ?: return@handler
        val predictedPos = predictedPlayerPosition ?: return@handler

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        val viewerX = mc.renderManager.viewerPosX
        val viewerY = mc.renderManager.viewerPosY
        val viewerZ = mc.renderManager.viewerPosZ

        if (showCurrentPos) {
            val currentPos = net.minecraft.util.Vec3(player.posX, player.posY, player.posZ)
            drawPlayerBox(currentPos, viewerX, viewerY, viewerZ, currentPosColor, "Current")
        }

        if (predictionLine) {
            drawPredictionLine(player, predictedPos, viewerX, viewerY, viewerZ)
        }

        if (predictionBox) {
            drawPlayerBox(predictedPos, viewerX, viewerY, viewerZ, predictionBoxColor, "Boosted")
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
    }

    val onPacket = handler<PacketEvent> { event ->
        when {
            event.packet is C03PacketPlayer && cancelC03 -> {
                if (cancelC03 && isBoosting) event.cancelEvent()
            }
            else -> if (blinkOnWorking) {
                if (shouldBlink && isBoosting && !hasBlink) {
                    BlinkUtils.blink(event.packet, event, receive = false)
                    if (!hasBlink) debugMessage("StartBlink")
                    hasBlink = true
                }
                else if (hasBlink && !isBoosting) {
                    debugMessage("StopBlink")
                    BlinkUtils.unblink()
                    hasBlink = false
                }
            }
        }
    }

    override fun onEnable() {
        mc.timer.timerSpeed = 1f
        isBoosting = false
        boostedTime.reset()
        slowedTime.reset()
        cooldownTimer.reset()
        hasSlowed = false
        hasBoosted = false
        predictedPlayerPosition = null
        shouldShowPrediction = false
    }

    override fun onDisable() {
        if (mc.timer.timerSpeed == boostTimer || mc.timer.timerSpeed == slowTimer) mc.timer.timerSpeed = 1f
        predictedPlayerPosition = null
        shouldShowPrediction = false
    }

    override val tag: String?
        get() = when (tagMode) {
            "WorkRange" -> "$minRange - $maxRange"
            "IsWorking" -> if (isBoosting) "Working" else "Idle"
            "MinSpeed-MaxSpeed" -> "${slowTimer}x - ${boostTimer}x"
            else -> customText
        }

    private fun debugMessage(string: Any) {
        if (debugMessage)
            chat(string.toString())
    }

    private fun runAttack() {
        val player = mc.thePlayer ?: return
        val aimedTarget = mc.objectMouseOver?.entityHit
        val target = aimedTarget ?: KillAura.target ?: return

        val dist = player.getDistanceToEntityBox(target)
        if (dist > attackMaxRange) return

        if (onlyAttackWhenNotReachedCPSLimit && CPSCounter.getCPS(CPSCounter.MouseButton.LEFT) >= CPSLimit) return
        if (attackCounter >= attackCount) return

        val shouldKeepSprint = keepSprint && player.hurtTime in allowKeepSprintHurtTime

        val swingHand = {
            if (swingMode.equals("packet", true)) sendPacket(C0APacketAnimation())
            else player.swingItem()
        }

        player.attackEntityWithModifiedSprint(target, !shouldKeepSprint) { swingHand() }

        CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)
        attackCounter++
        debugMessage("Attacked")
    }

    private fun calculateBoostPrediction(player: net.minecraft.entity.player.EntityPlayer, target: net.minecraft.entity.Entity) {
        val toTargetX = target.posX - player.posX
        val toTargetZ = target.posZ - player.posZ
        val distance = sqrt(toTargetX * toTargetX + toTargetZ * toTargetZ)

        val dirX = toTargetX / distance
        val dirZ = toTargetZ / distance

        val ticks = boostTime / 50.0
        val totalMoveDistance = 0.1 * boostTimer * ticks
        val maxMoveDistance = distance.coerceAtMost(totalMoveDistance)

        val predictedX = player.posX + dirX * maxMoveDistance
        val predictedY = player.posY
        val predictedZ = player.posZ + dirZ * maxMoveDistance

        predictedPlayerPosition = net.minecraft.util.Vec3(predictedX, predictedY, predictedZ)
    }

    private fun drawPredictionLine(player: net.minecraft.entity.player.EntityPlayer, predictedPos: net.minecraft.util.Vec3,
                                   viewerX: Double, viewerY: Double, viewerZ: Double) {
        val currentPos = net.minecraft.util.Vec3(player.posX, player.posY, player.posZ)

        GL11.glLineWidth(predictionLineWidth)
        GL11.glColor4f(
            predictionLineColor.red / 255f,
            predictionLineColor.green / 255f,
            predictionLineColor.blue / 255f,
            predictionLineColor.alpha / 255f
        )

        GL11.glBegin(GL11.GL_LINES)
        GL11.glVertex3d(currentPos.xCoord - viewerX, currentPos.yCoord - viewerY, currentPos.zCoord - viewerZ)
        GL11.glVertex3d(predictedPos.xCoord - viewerX, predictedPos.yCoord - viewerY, predictedPos.zCoord - viewerZ)
        GL11.glEnd()
    }

    private fun drawPlayerBox(position: net.minecraft.util.Vec3, viewerX: Double, viewerY: Double, viewerZ: Double,
                              color: Color, label: String) {
        RenderUtils.drawPosBox(position.xCoord, position.yCoord, position.zCoord, 0.6f, 1.8f, color, true)
    }
}
package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S12PacketEntityVelocity

class TimerGlideFly(parent: LoftilyFly) : LoftilyFlyMode("TimerGlide", parent) {

    val timerSpeed by float("TimerSpeed", 20f, 1f..100f)
    val speed by float("Speed", 0.02f, 0f..1f)
    val noSpeed by boolean("NoSpeed", false)
    val smartHurt by boolean("SmartHurt", false)
    val smartTicks by boolean("SmartTicks", false)
    val flyTicks by float("FlyTicks", 900f, 0f..1600f) { smartHurt }
    val jumpDamage by boolean("JumpDamage", false)
    val jumpCountValue by float("JumpCounts", 4f, 3f..8f) { jumpDamage }
    val jumpTimer by float("JumpTimer", 1f, 0.1f..5f) { jumpDamage }
    val customMotionY by choices("CustomMotionY", arrayOf("None", "Stable", "Multiply", "Optimal"), "None")
    val motionSpeed by float("MotionY", -0.01f, -0.3f..0.3f) { customMotionY != "None" }
    val autoToggle by boolean("AutoToggle", false)
    val autoAirDamage by boolean("AutoAirDamage", false)
    val spoofMode by choices("SpoofMode", arrayOf("Edit", "Packet", "1.8"), "Edit")
    val damageDistance by float("DamageDistance", 3.3f, 1f..5f) { autoAirDamage }
    val timerBeforeBoost by float("TimerBeforeBoost", 1f, 0.05f..1f)
    val stopMovement by boolean("NoMove", false) { autoAirDamage }

    private var elapsedTicks = 0
    private var jumpCounter = 0
    private var boostDurationTicks = 0
    private var boosting = false
    private var nextSpoof = false
    private var cancelTimer = 0L
    private var lastYaw = 0f
    private var lastPitch = 0f
    private var offGroundTicks = 0

    private fun fallDamage(): Boolean {
        val p = mc.thePlayer ?: return false
        return p.fallDistance > damageDistance
    }

    override fun onEnable() {
        val p = mc.thePlayer ?: return
        elapsedTicks = 0
        boosting = false
        lastYaw = p.rotationYaw
        lastPitch = p.rotationPitch
        offGroundTicks = 0
    }

    override fun onDisable() {
        boostDurationTicks = 0
        jumpCounter = 0
        elapsedTicks = 0
        boosting = false
        mc.timer.timerSpeed = 1f
        nextSpoof = false
        offGroundTicks = 0
    }

    private fun disable() {
        if (autoToggle) {
            parent.disableModule()
        } else {
            onDisable()
        }
    }

    override fun onPacketReceive(event: PacketEvent) {
        val packet = event.packet
        if (packet is S12PacketEntityVelocity) {
            if (autoAirDamage && packet.entityID == mc.thePlayer.entityId
                && System.currentTimeMillis() - cancelTimer < 2000L
            ) {
                event.cancelEvent()
            }
        }
    }

    override fun onPacketSend(event: PacketEvent) {
        val packet = event.packet
        if ((packet is C03PacketPlayer.C05PacketPlayerLook || packet is C03PacketPlayer.C06PacketPlayerPosLook)
            && System.currentTimeMillis() - cancelTimer < 3000L
            && autoAirDamage && !boosting && spoofMode != "1.8"
        ) {
            event.cancelEvent()
            if (packet is C03PacketPlayer.C06PacketPlayerPosLook) {
                PacketUtils.sendPacket(
                    C03PacketPlayer.C04PacketPlayerPosition(
                        packet.x, packet.y, packet.z, packet.onGround
                    ), false
                )
            }
        }
    }

    override fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.PRE) {
            if (autoAirDamage && nextSpoof) {
                when (spoofMode) {
                    "Edit" -> event.onGround = true
                    "Packet", "1.8" -> {
                        PacketUtils.sendPacket(
                            C03PacketPlayer.C04PacketPlayerPosition(
                                mc.thePlayer.posX, mc.thePlayer.posY,
                                mc.thePlayer.posZ, true
                            )
                        )
                    }
                }
                cancelTimer = System.currentTimeMillis()
                nextSpoof = false
                mc.timer.timerSpeed = 0.211f
            }
            if (jumpDamage && mc.thePlayer.hurtTime <= 0
                && jumpCounter.toDouble() < jumpCountValue && !boosting
            ) {
                event.onGround = false
                mc.thePlayer.rotationYaw = lastYaw
                mc.thePlayer.rotationPitch = lastPitch
            }
        }
    }

    override fun onMove(event: MoveEvent) {
        if (customMotionY == "Optimal"
            && (!smartHurt || (elapsedTicks.toDouble() <= flyTicks && boosting))
        ) {
            event.y = -1.0E-14
        }
    }

    override fun onMovementInput(event: MovementInputEvent) {
        if (jumpDamage && !boosting) {
            event.originalInput.moveStrafe = 0f
            event.originalInput.moveForward = 0f
        }
    }

    override fun onUpdate() {
        val p = mc.thePlayer ?: return

        offGroundTicks = if (p.onGround) 0 else offGroundTicks + 1

        if (!boosting) {
            if (fallDamage() && autoAirDamage) {
                if (spoofMode != "1.8") {
                    p.motionY *= 0.05
                    p.motionX = 0.0
                    p.motionZ = 0.0
                }
                p.fallDistance = 0f
                if (stopMovement) {
                    p.movementInput.moveForward = 0f
                    p.movementInput.moveStrafe = 0f
                }
                cancelTimer = System.currentTimeMillis()
                nextSpoof = true
            }
            mc.timer.timerSpeed = timerBeforeBoost
        }

        if (p.hurtTime > 0 && !boosting) {
            mc.timer.timerSpeed = timerSpeed
            if (p.onGround) {
                p.motionY = 0.42
            } else if (offGroundTicks >= 6) {
                boostDurationTicks = (20 * timerSpeed).toInt()
                boosting = true
            }
        }

        if (jumpDamage && p.hurtTime <= 0
            && jumpCounter.toDouble() < jumpCountValue
        ) {
            mc.timer.timerSpeed = jumpTimer
            if (p.onGround) {
                p.jump()
                jumpCounter++
            }
        }

        if (!smartHurt || (elapsedTicks.toDouble() <= flyTicks && boosting)) {
            if (!noSpeed) {
                setSpeed(speed.toDouble(), false)
            }
            mc.timer.timerSpeed = timerSpeed
            when (customMotionY) {
                "None" -> p.motionY *= 0.042
                "Stable" -> p.motionY = motionSpeed.toDouble()
                "Multiply" -> p.motionY *= motionSpeed.toDouble()
                "Optimal" -> {
                    if (!mc.thePlayer.isMoving) {
                        p.movementInput.moveForward = 1f
                        p.movementInput.moveStrafe = 0f
                    }
                }
            }
            if (boostDurationTicks > 0) {
                boostDurationTicks--
            }
            elapsedTicks++
        }

        if (boostDurationTicks == 0 && boosting) {
            disable()
        }
    }
}
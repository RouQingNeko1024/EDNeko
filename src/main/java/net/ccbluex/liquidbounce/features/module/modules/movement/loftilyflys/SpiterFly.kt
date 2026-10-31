package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity

class SpiterFly(parent: LoftilyFly) : LoftilyFlyMode("Spiter", parent) {

    private var canJump = false
    private var flag = false
    private var flags = 0
    private var times = 0
    private var ticks = 0
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0
    private var motionX = 0.0
    private var motionY = 0.0
    private var motionZ = 0.0
    private var freeze = false
    private var sent = false
    private var offGroundTicks = 0

    companion object {
        var ise = false
    }

    override fun onDisable() {
        mc.gameSettings.keyBindSneak.pressed = false
        ise = false
        super.onDisable()
    }

    override fun onEnable() {
        canJump = false
        flag = false
        mc.timer.timerSpeed = 1.0f
        times = 1
        ticks = 0
        freeze = false
        sent = false
        ise = true
        super.onEnable()
    }

    override fun onPacketReceive(event: PacketEvent) {
        val packet = event.packet
        when (packet) {
            is S08PacketPlayerPosLook -> {
                if (times == 1) {
                    flags++
                    if (flags.toFloat() >= 1.0f && !canJump && !flag) {
                        canJump = true
                        flag = true
                        flags = 0
                    }
                } else {
                    times = 1
                }
            }
            is S12PacketEntityVelocity -> {
                if (packet.entityID == mc.thePlayer.entityId) {
                    event.cancelEvent()
                }
            }
        }
    }

    override fun onMotion(event: MotionEvent) {
        mc.gameSettings.keyBindSneak.pressed = true
        if (times != 1) {
            return
        }
        if (!flag) {
            mc.thePlayer.setPosition(
                mc.thePlayer.lastTickPosX,
                mc.thePlayer.lastTickPosY,
                mc.thePlayer.lastTickPosZ
            )
            mc.timer.timerSpeed = 1.0f
            if (event.eventState == EventState.PRE && !sent) {
                event.x += 999999.0
                event.z += 999999.0
                event.y -= 999999.0
                sent = true
            }
        }
    }

    override fun onPacketSend(event: PacketEvent) {
        val packet = event.packet
        if (freeze && times != 1 && packet is C03PacketPlayer) {
            event.cancelEvent()
            return
        }
        // 1.8 wire has no ConfirmTeleport (it is captured at the ViaMCP/1.9+ layer), and the
        // original cancelled the bare CPacketPlayer base class to keep position packets strict.
        if (packet.javaClass == C03PacketPlayer::class.java) {
            event.cancelEvent()
        }
    }

    override fun onUpdate() {
        val player = mc.thePlayer
        player.isSprinting = false
        mc.gameSettings.keyBindSprint.pressed = false
        if (player.onGround) offGroundTicks = 0 else offGroundTicks++
        if (times == 0 && ticks >= 0) {
            if (player.onGround) {
                player.tryJump()
            }
            if (player.fallDistance >= 0.3f && !freeze) {
                x = player.posX
                y = player.posY
                z = player.posZ
                motionX = player.motionX
                motionY = player.motionY
                motionZ = player.motionZ
                freeze = true
            }
            if (ticks == 7) {
                player.motionX = motionX
                player.motionY = motionY
                player.motionZ = motionZ
                ticks = -4
                freeze = false
            }
            if (ticks != -4 && freeze) {
                player.motionX = 0.0
                player.motionY = 0.0
                player.motionZ = 0.0
                player.setPosition(x, y, z)
                ticks++
            }
        }
        if (times != 1) {
            return
        }
        if (canJump) {
            setSpeed(0.51, true)
            player.jump()
            player.motionY = 0.08
            offGroundTicks = 0
            mc.timer.timerSpeed = 1.0f
            canJump = false
            sent = false
        } else if (flag) {
            if (player.isMoving) {
                if (player.motionY <= 0.05 && !mc.gameSettings.keyBindJump.isKeyDown ||
                    offGroundTicks >= 1
                ) {
                    flag = false
                    mc.timer.timerSpeed = 0.3f
                }
            } else if (player.motionY < -0.1) {
                flag = false
                mc.timer.timerSpeed = 0.1f
            }
        }
    }
}
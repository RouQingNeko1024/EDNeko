package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.intave

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.client.settings.GameSettings
import net.minecraft.network.play.client.C03PacketPlayer
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object IntaveFly : FlyMode("Intave") {
    private var ticks = 0
    private var modifyTicks = 0
    private var stage = FlyStage.WAITING

    override fun onEnable() {
        ticks = 0
        modifyTicks = 0
        mc.thePlayer.setPosition(mc.thePlayer.posX, (mc.thePlayer.posY * 2).roundToInt().toDouble() / 2, mc.thePlayer.posZ)
        stage = FlyStage.WAITING
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1.0f
    }

    override fun onUpdate() {
        ticks++
        modifyTicks++
        mc.gameSettings.keyBindJump.pressed = false
        mc.gameSettings.keyBindSneak.pressed = false
        when (stage) {
            FlyStage.FLYING, FlyStage.WAITING -> {
                if (ticks == 2 && GameSettings.isKeyDown(mc.gameSettings.keyBindJump) && modifyTicks >= 6 && mc.theWorld.getCollisionBoxes(
                        mc.thePlayer.entityBoundingBox.offset(0.0, 0.5, 0.0)
                    ).isEmpty()
                ) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.5, mc.thePlayer.posZ)
                    modifyTicks = 0
                }
                if (mc.thePlayer.isMoving && ticks == 1 && (GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) || GameSettings.isKeyDown(
                        mc.gameSettings.keyBindJump
                    )) && modifyTicks >= 5
                ) {
                    val playerYaw = mc.thePlayer.rotationYaw * Math.PI / 180
                    mc.thePlayer.setPosition(
                        mc.thePlayer.posX + 0.05 * -sin(playerYaw),
                        mc.thePlayer.posY,
                        mc.thePlayer.posZ + 0.05 * cos(playerYaw)
                    )
                }
                if (ticks == 2 && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && modifyTicks >= 6 && mc.theWorld.getCollisionBoxes(
                        mc.thePlayer.entityBoundingBox.offset(0.0, -0.5, 0.0)
                    ).isEmpty()
                ) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - 0.5, mc.thePlayer.posZ)
                    modifyTicks = 0
                }
                mc.thePlayer.onGround = true
                mc.thePlayer.motionY = 0.0
            }

            FlyStage.WAIT_APPLY -> {
                mc.timer.timerSpeed = 1.0f
                mc.thePlayer.stopXZ()
                mc.thePlayer.motionY = 0.0
                mc.thePlayer.jumpMovementFactor = 0.0f
                if (modifyTicks >= 10) {
                    val playerYaw = mc.thePlayer.rotationYaw * Math.PI / 180
                    if (modifyTicks % 2 != 0) {
                        mc.thePlayer.setPosition(
                            mc.thePlayer.posX + 0.1 * -sin(playerYaw),
                            mc.thePlayer.posY,
                            mc.thePlayer.posZ + 0.1 * cos(playerYaw)
                        )
                    } else {
                        mc.thePlayer.setPosition(
                            mc.thePlayer.posX - 0.1 * -sin(playerYaw),
                            mc.thePlayer.posY,
                            mc.thePlayer.posZ - 0.1 * cos(playerYaw)
                        )
                        if (modifyTicks >= 16 && ticks == 2) {
                            modifyTicks = 16
                            mc.thePlayer.setPosition(
                                mc.thePlayer.posX, mc.thePlayer.posY + 0.5, mc.thePlayer.posZ
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer) {
            if (ticks > 2) {
                ticks = 0
                packet.y += 0.5
            }
            packet.onGround = true
        }
    }

    enum class FlyStage {
        WAITING,
        FLYING,
        WAIT_APPLY
    }
}
/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.block.Block
import net.minecraft.block.BlockPane
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

object HighJump : Module("HighJump", Category.MOVEMENT) {
    private val mode by choices("Mode", arrayOf("Vanilla", "Damage", "AACv3", "DAC", "Mineplex", "Matrix", "MatrixWater"), "Vanilla")
    private val height by float("Height", 2f, 1.1f..7f) { mode in arrayOf("Vanilla", "Damage", "MatrixWater") }
    private val glass by boolean("OnlyGlassPane", false)

    private var matrixStatus = 0
    private var matrixWasTimer = false
    private val timer = MSTimer()

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer

        if (glass && BlockPos(thePlayer).block !is BlockPane)
            return@handler

        when (mode.lowercase()) {
            "damage" -> if (thePlayer.hurtTime > 0 && thePlayer.onGround) thePlayer.motionY += 0.42f * height
            "aacv3" -> if (!thePlayer.onGround) thePlayer.motionY += 0.059
            "dac" -> if (!thePlayer.onGround) thePlayer.motionY += 0.049999
            "mineplex" -> if (!thePlayer.onGround) strafe(0.35f)
            "matrixwater" -> {
                if (thePlayer.isInWater) {
                    if (mc.theWorld.getBlockState(BlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ)).block == Block.getBlockById(9)) {
                        thePlayer.motionY = 0.18
                    } else if (mc.theWorld.getBlockState(BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)).block == Block.getBlockById(9)) {
                        thePlayer.motionY = height.toDouble()
                        thePlayer.onGround = true
                    }
                }
            }
            "matrix" -> {
                if (matrixWasTimer) {
                    mc.timer.timerSpeed = 1.00f
                    matrixWasTimer = false
                }
                if ((mc.theWorld.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(0.0, thePlayer.motionY, 0.0).expand(0.0, 0.0, 0.0)).isNotEmpty() ||
                            mc.theWorld.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(0.0, -4.0, 0.0).expand(0.0, 0.0, 0.0)).isNotEmpty()) &&
                    thePlayer.fallDistance > 10) {
                    if (!thePlayer.onGround) {
                        mc.timer.timerSpeed = 0.1f
                        matrixWasTimer = true
                    }
                }
                if (timer.hasTimePassed(1000) && matrixStatus == 1) {
                    mc.timer.timerSpeed = 1.0f
                    thePlayer.motionX = 0.0
                    thePlayer.motionZ = 0.0
                    matrixStatus = 0
                }
                if (matrixStatus == 1 && thePlayer.hurtTime > 0) {
                    mc.timer.timerSpeed = 1.0f
                    thePlayer.motionY = 3.0
                    thePlayer.motionX = 0.0
                    thePlayer.motionZ = 0.0
                    thePlayer.jumpMovementFactor = 0.00f
                    matrixStatus = 0
                }
                if (matrixStatus == 2) {
                    mc.netHandler.addToSendQueue(C0APacketAnimation())
                    mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, false))
                    repeat(8) {
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.3990, thePlayer.posZ, false))
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, false))
                    }
                    mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
                    mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
                    mc.timer.timerSpeed = 0.6f
                    matrixStatus = 1
                    timer.reset()
                    mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, BlockPos(thePlayer.posX, thePlayer.posY - 1, thePlayer.posZ), EnumFacing.UP))
                    mc.netHandler.addToSendQueue(C0APacketAnimation())
                }
                if (thePlayer.isCollidedHorizontally && matrixStatus == 0 && thePlayer.onGround) {
                    mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, BlockPos(thePlayer.posX, thePlayer.posY - 1, thePlayer.posZ), EnumFacing.UP))
                    mc.netHandler.addToSendQueue(C0APacketAnimation())
                    matrixStatus = 2
                    mc.timer.timerSpeed = 0.05f
                }
                if (thePlayer.isCollidedHorizontally && thePlayer.onGround) {
                    thePlayer.motionX = 0.0
                    thePlayer.motionZ = 0.0
                    thePlayer.onGround = false
                }
            }
        }
    }


    override fun onEnable() {
        matrixStatus = 0
        matrixWasTimer = false
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

    val onMove = handler<MoveEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (glass && BlockPos(thePlayer).block !is BlockPane)
            return@handler
        if (!thePlayer.onGround) {
            if ("mineplex" == mode.lowercase()) {
                thePlayer.motionY += if (thePlayer.fallDistance == 0f) 0.0499 else 0.05
            }
        }
    }

    val onJump = handler<JumpEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler

        if (glass && BlockPos(thePlayer).block !is BlockPane)
            return@handler
        when (mode.lowercase()) {
            "vanilla" -> event.motion *= height
            "mineplex" -> event.motion = 0.47f
        }
    }

    override val tag
        get() = mode
}
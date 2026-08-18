package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.MovementInputEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos

object GrimAC19 : NoFallMode("GrimAC1.9+") {

    private var grimACTriggered = false
    private var canJump = false
    private var jumpTick = 0
    private var fallStartY = 0.0
    private var wasInAir = false

    private fun getCalculatedFallDistance(): Float {
        val player = mc.thePlayer ?: return 0f
        if (!wasInAir) return 0f
        val distance = fallStartY - player.posY
        return maxOf(0f, distance.toFloat())
    }

    override fun onEnable() {
        grimACTriggered = false
        canJump = false
        jumpTick = 0
        fallStartY = 0.0
        wasInAir = false
    }

    override fun onDisable() {
        grimACTriggered = false
        canJump = false
        jumpTick = 0
        fallStartY = 0.0
        wasInAir = false
    }

    override fun onMovementInput(event: MovementInputEvent) {
        val player = mc.thePlayer ?: return
        if (NoFall.mode != modeName) return

        if (canJump) {
            event.originalInput.jump = true
            if (jumpTick == 0) jumpTick = 1
        }
        if (jumpTick != 0) jumpTick++
        if (jumpTick > 10) {
            event.originalInput.jump = false
            jumpTick = 0
            canJump = false
        }
    }

    override fun onMotion(event: MotionEvent) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return
        if (NoFall.mode != modeName) return
        if (event.eventState != EventState.PRE) return

        val isInAir = !player.onGround
            && !player.isInWater
            && !player.isOnLadder
            && !player.capabilities.isFlying

        if (isInAir) {
            if (!wasInAir) {
                fallStartY = player.posY
                wasInAir = true
            } else {
                if (player.posY > fallStartY) {
                    fallStartY = player.posY
                }
            }
        } else {
            wasInAir = false
        }

        if (grimACTriggered) {
            player.motionX = 0.0
            player.motionY = 0.0
            player.motionZ = 0.0
            return
        } else {
            val isFalling = player.motionY < -0.1 && getCalculatedFallDistance() > 3

            if (isFalling) {
                canJump = true
                val belowPos = BlockPos(
                    player.posX,
                    player.posY + player.motionY,
                    player.posZ
                )
                val state = world.getBlockState(belowPos)
                val block = state.block

                if (block.isBlockNormalCube || block.material.blocksMovement()) {
                    player.fallDistance = 0f
                    grimACTriggered = true
                }
            }
        }
    }

    override fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null) return
        if (NoFall.mode != modeName) return

        if (event.packet is S08PacketPlayerPosLook) {
            grimACTriggered = false
        }
    }
}
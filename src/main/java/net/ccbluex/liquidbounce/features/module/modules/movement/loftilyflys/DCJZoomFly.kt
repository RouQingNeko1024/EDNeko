package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.StepEvent
import net.minecraft.block.BlockAir
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos

class DCJZoomFly(parent: LoftilyFly) : LoftilyFlyMode("DCJZoom", parent) {

    private var ticks = 0

    override fun onBlockBBEvent(event: BlockBBEvent) {
        if (event.block is BlockAir && event.y.toDouble() <= mc.thePlayer.posY) {
            event.boundingBox = AxisAlignedBB.fromBounds(
                event.x.toDouble(),
                event.y.toDouble(),
                event.z.toDouble(),
                event.x + 1.0,
                mc.thePlayer.posY,
                event.z + 1.0
            )
        }
    }

    override fun onJump(event: JumpEvent) {
        event.cancelEvent()
    }

    override fun onStep(event: StepEvent) {
        event.stepHeight = 0.0f
    }

    override fun onUpdate() {
        ticks++
        if (ticks >= 2 && isBlastMove()) {
            mc.thePlayer.setPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY + 1.0E-5,
                mc.thePlayer.posZ
            )
            ticks = 0
        }
    }

    private fun isBlastMove(): Boolean {
        val pos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
        if (pos.down().y.toDouble() <= mc.thePlayer.posY) {
            return mc.theWorld.getBlockState(pos.down()).block is BlockAir
        }
        return false
    }
}
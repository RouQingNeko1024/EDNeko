package net.ccbluex.liquidbounce.utils.render

import net.minecraft.block.BlockAir
import net.minecraft.block.BlockBush
import net.minecraft.block.BlockLiquid
import net.minecraft.client.Minecraft
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3

class AttackParticle(
    val position: Vec3,
    private val speedMultiplier: Float = 1f,
    private val gravityMultiplier: Float = 1f
) {
    private var delta = Vec3(
        (Math.random() * 2.5 - 1.25) * 0.04 * speedMultiplier,
        (Math.random() * 0.5 - 0.2) * 0.04 * speedMultiplier,
        (Math.random() * 2.5 - 1.25) * 0.04 * speedMultiplier
    )
    
    var age = 0L
    var maxAge = 1000L

    fun update() {
        val block1 = getBlock(position.xCoord, position.yCoord, position.zCoord + delta.zCoord)
        if (block1 !is BlockAir && block1 !is BlockBush && block1 !is BlockLiquid) {
            delta = Vec3(delta.xCoord, delta.yCoord, -delta.zCoord * 0.8)
        }

        val block2 = getBlock(position.xCoord, position.yCoord + delta.yCoord, position.zCoord)
        if (block2 !is BlockAir && block2 !is BlockBush && block2 !is BlockLiquid) {
            delta = Vec3(delta.xCoord * 0.99, -delta.yCoord * 0.5, delta.zCoord * 0.99)
        }

        val block3 = getBlock(position.xCoord + delta.xCoord, position.yCoord, position.zCoord)
        if (block3 !is BlockAir && block3 !is BlockBush && block3 !is BlockLiquid) {
            delta = Vec3(-delta.xCoord * 0.8, delta.yCoord, delta.zCoord)
        }

        updateWithoutPhysics()
    }

    fun updateWithoutPhysics() {
        position.xCoord += delta.xCoord
        position.yCoord += delta.yCoord
        position.zCoord += delta.zCoord
        delta = Vec3(
            delta.xCoord * 0.998,
            delta.yCoord - 3.1E-5 * gravityMultiplier,
            delta.zCoord * 0.998
        )
        age++
    }
    
    fun isDead(): Boolean {
        return age >= maxAge
    }

    companion object {
        fun getBlock(offsetX: Double, offsetY: Double, offsetZ: Double) =
            Minecraft.getMinecraft().theWorld.getBlockState(BlockPos(offsetX, offsetY, offsetZ)).block
    }
}

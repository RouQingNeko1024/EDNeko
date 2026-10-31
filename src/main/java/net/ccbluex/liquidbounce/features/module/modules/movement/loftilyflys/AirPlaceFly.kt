package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.utils.extensions.onPlayerRightClick
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.minecraft.block.BlockAir
import net.minecraft.item.ItemBlock
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3

class AirPlaceFly(parent: LoftilyFly) : LoftilyFlyMode("AirPlace", parent) {

    val rotation by boolean("AirPlace-Rotation", false)
    val placePos by choices("AirPlace-PlacePos", arrayOf("Feet", "Body", "Funny"), "Feet")
    val jump by boolean("AirPlace-Jump", false)

    override fun onUpdate() {
        val player = mc.thePlayer
        if (player.getHeldItem()?.item !is ItemBlock) {
            val slot = InventoryUtils.findBlockInHotbar() ?: return
            player.inventory.currentItem = slot
        }
        if (rotation) {
            RotationUtils.currentRotation = Rotation(player.rotationYaw, 89f)
        }
        if (player.onGround && jump) {
            player.tryJump()
        }
        if (player.getHeldItem()?.item !is ItemBlock) {
            return
        }
        val condition = (player.motionY <= -0.05 && mc.gameSettings.keyBindJump.isKeyDown) ||
            player.motionY <= -0.38 || !jump
        if (!condition) {
            return
        }
        var pos = BlockPos(player.posX, player.posY, player.posZ).down()
        var facing = EnumFacing.UP
        when (placePos) {
            "Body" -> {
                pos = BlockPos(player.posX, player.posY - 1.5, player.posZ)
            }
            "Funny" -> {
                pos = BlockPos(player.posX, player.posY + 2.0, player.posZ)
                facing = EnumFacing.DOWN
            }
        }
        if (mc.theWorld.getBlockState(pos).block !is BlockAir) {
            return
        }
        val hitVec = Vec3(player.posX, pos.y.toDouble(), player.posZ)
        player.onPlayerRightClick(pos, facing, hitVec)
        player.swingItem()
    }
}
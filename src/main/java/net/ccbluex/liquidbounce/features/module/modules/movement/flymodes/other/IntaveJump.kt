/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.jumpY
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.block.BlockLadder
import net.minecraft.block.material.Material
import net.minecraft.client.settings.GameSettings
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.START_SNEAKING
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.STOP_SNEAKING
import net.minecraft.util.AxisAlignedBB

object IntaveJump : FlyMode("IntaveJump") {

    private var sneaking = false
    private var wasOnGround = true

    override fun onEnable() {
        sneaking = false
        wasOnGround = true
    }

    override fun onDisable() {
        stopSneaking()
    }

    private fun startSneaking() {
        if (sneaking) return
        val player = mc.thePlayer ?: return
        mc.gameSettings.keyBindSneak.pressed = true
        sendPacket(C0BPacketEntityAction(player, START_SNEAKING))
        sneaking = true
    }

    private fun stopSneaking() {
        if (!sneaking) return
        val player = mc.thePlayer ?: return
        mc.gameSettings.keyBindSneak.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
        sendPacket(C0BPacketEntityAction(player, STOP_SNEAKING))
        sneaking = false
    }

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (player.onGround && !player.isJumping)
            player.tryJump()

        if (wasOnGround && !player.onGround)
            startSneaking()

        if (!wasOnGround && player.onGround)
            stopSneaking()

        wasOnGround = player.onGround

        if ((mc.gameSettings.keyBindJump.isKeyDown && !mc.gameSettings.keyBindSneak.isKeyDown) || player.onGround)
            jumpY = player.posY
    }

    override fun onBB(event: BlockBBEvent) {
        val jumpYCondition =
            if (!mc.gameSettings.keyBindJump.isKeyDown && mc.gameSettings.keyBindSneak.isKeyDown) event.y.toDouble() < jumpY else event.y.toDouble() <= jumpY
        if ((!event.block.material.blocksMovement() && event.block.material != Material.carpet && event.block.material != Material.vine && event.block.material != Material.snow && event.block !is BlockLadder) && jumpYCondition) {
            event.boundingBox = AxisAlignedBB.fromBounds(
                event.x.toDouble(),
                event.y.toDouble(),
                event.z.toDouble(),
                event.x.toDouble() + 1,
                1.0,
                event.z.toDouble() + 1
            )
        }
    }
}
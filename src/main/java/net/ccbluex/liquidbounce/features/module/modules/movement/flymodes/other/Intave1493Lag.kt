/*
 * 猫娘伟大发明操船fly
 */
//By NekoBanka,NekoParua,AiNeko
//爱来自nyasoft,edneko使用此代码需要带上前面以及此行注释
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.item.EntityBoat
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.INTERACT
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.START_SNEAKING
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.STOP_SNEAKING

object Intave1493Lag : FlyMode("Intave14.9.3lag") {

    private var currentLag = 0
    private var sneaking = false
    private var walking = false
    private var mountedBoat = false
    private var done = false

    override fun onEnable() {
        currentLag = 0
        sneaking = false
        walking = false
        mountedBoat = false
        done = false

        mc.gameSettings.keyBindSneak.pressed = true
        sendPackets(C0BPacketEntityAction(mc.thePlayer, START_SNEAKING))
        sneaking = true

        if (Fly.intaveAutoWalk) {
            mc.gameSettings.keyBindForward.pressed = true
            walking = true
        }

        chat("[Intave14.9.3lag] Sneak started, searching for boat...")
    }

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (done) return

        if (!mountedBoat) {
            val boat = mc.theWorld.loadedEntityList
                .filterIsInstance<EntityBoat>()
                .filter { !it.isDead && player.getDistanceToEntity(it) <= 6f }
                .minByOrNull { player.getDistanceToEntity(it) }

            if (boat != null) {
                sendPackets(C02PacketUseEntity(boat, INTERACT))
                mountedBoat = true
                chat("[Intave14.9.3lag] Mounting boat at ${formatPos(boat)}")
            } else {
                chat("[Intave14.9.3lag] No boat found nearby!")
            }
            return
        }

        currentLag++
        chat("[Intave14.9.3lag] Lag #${currentLag} at ${formatPos(player)}")

        if (currentLag >= Fly.intaveLagCount) {
            mc.gameSettings.keyBindSneak.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
            sendPackets(C0BPacketEntityAction(player, STOP_SNEAKING))
            sneaking = false

            if (walking) {
                mc.gameSettings.keyBindForward.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
                walking = false
            }

            done = true

            chat("[Intave14.9.3lag] Done! Lag count: ${currentLag}, final pos: ${formatPos(player)}")
            Fly.state = false
        }
    }

    override fun onDisable() {
        if (sneaking) {
            mc.gameSettings.keyBindSneak.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
            val player = mc.thePlayer
            if (player != null) {
                sendPackets(C0BPacketEntityAction(player, STOP_SNEAKING))
            }
            sneaking = false
        }

        if (walking) {
            mc.gameSettings.keyBindForward.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
            walking = false
        }
    }

    private fun formatPos(entity: net.minecraft.entity.Entity): String {
        return String.format("%.2f, %.2f, %.2f", entity.posX, entity.posY, entity.posZ)
    }
}
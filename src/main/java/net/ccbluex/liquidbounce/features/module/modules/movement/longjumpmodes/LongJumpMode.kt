/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance

open class LongJumpMode(val modeName: String) : Configurable(modeName), MinecraftInstance {
    open fun onUpdate() {}
    open fun onMove(event: MoveEvent) {}
    open fun onJump(event: JumpEvent) {}
    open fun onMotion(event: MotionEvent) {}
    open fun onPacketSend(event: PacketEvent) {}
    open fun onPacketReceive(event: PacketEvent) {}

    open fun onEnable() {}
    open fun onDisable() {}
}
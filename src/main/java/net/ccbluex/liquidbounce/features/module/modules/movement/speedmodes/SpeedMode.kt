/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance

open class SpeedMode(val modeName: String) : MinecraftInstance {
    open fun onMotion() {}
    open fun onMotion(event: MotionEvent) = onMotion()
    open fun onMotion2(event: MotionEvent) {}
    open fun onPostMotion() {}
    open fun onUpdate() {}
    open fun onMove(event: MoveEvent) {}
    open fun onTick() {}
    open fun onStrafe() {}
    open fun onJump(event: JumpEvent) {}
    open fun onPacket(event: PacketEvent) {}
    open fun onMoveInput(event: MovementInputEvent) {}
    open fun onGameLoop(event: GameLoopEvent) {}
    open fun onBoundingBox() {}
    open fun onEnable() {}
    open fun onDisable() {}
}
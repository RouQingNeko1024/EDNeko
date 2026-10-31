package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance

open class LoftilyVelocityMode(
    val modeName: String,
    val parent: LoftilyVelocity
) : Configurable(modeName), MinecraftInstance {

    open fun onEnable() {}
    open fun onDisable() {}

    open fun onPacket(event: PacketEvent) {}
    open fun onMotion(event: MotionEvent) {}
    open fun onAttack(event: AttackEvent) {}
    open fun onStrafe(event: StrafeEvent) {}
    open fun onWorld(event: WorldEvent) {}
    open fun onJump(event: JumpEvent) {}
    open fun onUpdate() {}
}
package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.MovementInputEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.StepEvent
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.stop
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils

/**
 * 基础 Fly 模式基类（移植自 loftily 框架的 flys 模块）。
 *
 * 每个模式继承 Configurable，配置项由父模块 [LoftilyFly] 统一收集，
 * 并且仅在父模块选择了该模式时这些配置项才显示。
 *
 * 事件映射说明（与原框架对应关系）：
 *  - onPacketSend / onPacketReceive  对应原框架 PacketSendEvent / PacketReceiveEvent
 *  - onPreMotion                     对应原框架 PreUpdateEvent 之前的 MotionEvent PRE
 *  - onMotion                        对应原框架 MotionEvent（PRE 与 POST 均触发，由模式内部按 eventState 过滤）
 *  - onUpdate                        对应原框架 UpdateEvent / LivingUpdateEvent / PreUpdateEvent（位移控制）
 *  - onMove                          对应原框架 MoveEvent
 *  - onTick / onWorld                对应原框架 TickEvent / WorldLoadEvent
 */
open class LoftilyFlyMode(
    val modeName: String,
    val parent: LoftilyFly
) : Configurable(modeName), MinecraftInstance {

    /** 原框架 RotationHandler.getCurrentRotation() 的等价物 */
    protected val currentYaw: Float
        get() = RotationUtils.currentRotation?.yaw ?: mc.thePlayer.rotationYaw

    protected val currentPitch: Float
        get() = RotationUtils.currentRotation?.pitch ?: mc.thePlayer.rotationPitch

    /**
     * 原框架 MoveUtils.stop(movingCheck) 的等价实现。
     * movingCheck 为 true 时仅在玩家有输入时停止（否则忽略本次调用）。
     */
    protected fun stopMovement(movingCheck: Boolean) {
        val player = mc.thePlayer ?: return
        if (movingCheck && !player.isMoving) return
        player.stop()
    }

    /** 原框架 MoveUtils.setSpeed(speed, movingCheck) 的等价调用 */
    protected fun setSpeed(speed: Double, movingCheck: Boolean) = MovementUtils.setSpeed(speed, movingCheck)

    open fun onEnable() {
    }

    open fun onDisable() {
    }

    open fun onPacketSend(event: PacketEvent) {
    }

    open fun onPacketReceive(event: PacketEvent) {
    }

    open fun onPreMotion(event: MotionEvent) {
    }

    open fun onMotion(event: MotionEvent) {
    }

    open fun onUpdate() {
    }

    open fun onMove(event: MoveEvent) {
    }

    open fun onTick() {
    }

    open fun onWorld() {
    }

    open fun onBlockBBEvent(event: BlockBBEvent) {
    }

    open fun onJump(event: JumpEvent) {
    }

    open fun onStep(event: StepEvent) {
    }

    open fun onMovementInput(event: MovementInputEvent) {
    }
}
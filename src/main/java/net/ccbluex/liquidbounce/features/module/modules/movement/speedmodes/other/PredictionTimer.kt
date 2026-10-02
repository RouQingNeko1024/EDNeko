package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.predictionGroundTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object PredictionTimer : SpeedMode("PredictionTimer") {
    // 添加跳跃计数器，用于跟踪是第几次跳跃
    private var jumpCount = 0
    // 标记是否已经在这次跳跃中增加了计数器
    private var hasJumpedThisCycle = false

    // 重写onStrafe方法，处理移动时的跳跃逻辑
    override fun onStrafe() {
        val player = mc.thePlayer ?: return

        // 当玩家在地面且正在移动时尝试跳跃
        if (player.onGround && player.isMoving && !hasJumpedThisCycle) {
            player.tryJump()
            // 增加跳跃计数器
            jumpCount++
            hasJumpedThisCycle = true
        }
        // 当玩家离开地面时重置跳跃标记
        else if (!player.onGround) {
            hasJumpedThisCycle = false
        }
    }

    // 重写onUpdate方法，处理timer逻辑
    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        // 当玩家在地面时，根据跳跃次数决定是否加速
        if (player.onGround) {
            // 只在奇数次跳跃时设置timer为1.5倍速度
            if (jumpCount % 2 == 1) {
                mc.timer.timerSpeed = predictionGroundTimer
            } else {
                // 偶数次跳跃时使用正常速度
                mc.timer.timerSpeed = 1.0f
            }
        } else {
            // 不在地面时，重置timer为正常值
            mc.timer.timerSpeed = 1.0f
        }
    }

    // 确保在禁用时重置timer和计数器
    override fun onDisable() {
        mc.timer.timerSpeed = 1.0f
        jumpCount = 0
        hasJumpedThisCycle = false
    }
}
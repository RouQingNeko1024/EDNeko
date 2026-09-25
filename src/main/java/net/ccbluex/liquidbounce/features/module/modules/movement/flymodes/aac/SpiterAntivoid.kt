package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.spiterFallSpeedThreshold
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.spiterUpwardMotion
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.spiterTimerSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.spiterOnceTrigger
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.spiterForwardMotion
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import kotlin.math.cos
import kotlin.math.sin

object SpiterAntivoid : FlyMode("SpiterAntivoid") {
    private var triggeredOnce = false
    private var isRising = false

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        val isFalling = player.motionY < 0 && !player.onGround
        val fallSpeedBps = Math.abs(player.motionY) * 20.0

        if (isFalling && fallSpeedBps > spiterFallSpeedThreshold.toDouble()) {
            // 如果启用了"只触发一次"且已经触发过，跳过
            if (spiterOnceTrigger && triggeredOnce) {
                mc.timer.timerSpeed = 1f
                isRising = false
                return
            }

            triggeredOnce = true
            isRising = true

            // 上升 motion
            player.motionY = spiterUpwardMotion.toDouble()

            // 向前的 motion（朝向玩家视线方向）
            if (spiterForwardMotion > 0f) {
                val yaw = player.rotationYaw.toRadians()
                val forward = spiterForwardMotion.toDouble()
                player.motionX = -sin(yaw) * forward
                player.motionZ = cos(yaw) * forward
            }

            player.onGround = false
            mc.timer.timerSpeed = spiterTimerSpeed

        } else if (isRising) {
            // 上升阶段，持续应用 timer 加速
            mc.timer.timerSpeed = spiterTimerSpeed

            // 如果上升过程中还在给向前 motion
            if (spiterForwardMotion > 0f && player.motionY > 0) {
                val yaw = player.rotationYaw.toRadians()
                val forward = spiterForwardMotion.toDouble()
                player.motionX = -sin(yaw) * forward
                player.motionZ = cos(yaw) * forward
            }

            // 触顶或下落结束上升阶段
            if (player.motionY <= 0 || player.onGround) {
                isRising = false
                mc.timer.timerSpeed = 1f
            }
        } else {
            // 落地后重置触发标记
            if (player.onGround) {
                triggeredOnce = false
            }
            mc.timer.timerSpeed = 1f
        }
    }

    override fun onDisable() {
        triggeredOnce = false
        isRising = false
        mc.timer.timerSpeed = 1f
    }
}
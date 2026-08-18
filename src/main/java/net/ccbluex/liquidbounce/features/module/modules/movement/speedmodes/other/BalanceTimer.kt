package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.config.BoolValue
import net.ccbluex.liquidbounce.config.FloatValue
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object BalanceTimer : SpeedMode("BalanceTimer") {

    private val riseRatio by FloatValue("RiseRatio", 0.4f, 0.01f..0.99f)
    private val riseTimerMax by FloatValue("RiseTimerMax", 2.0f, 1.0f..10.0f)
    private val debug by BoolValue("Debug", false)

    private var jumpStartY = 0.0
    private var peakY = 0.0
    private var isRising = false
    private var wasOnGround = true

    private var riseRealTimeNanos = 0L
    private var fallRealTimeNanos = 0L
    private var riseStartNanos = 0L
    private var fallStartNanos = 0L

    private var riseTimerTicks = 0
    private var fallTimerTicks = 0
    private var currentRiseTimer = 1f
    private var currentFallTimer = 1f

    private var lastJumpPeakY = 0.0
    private var lastJumpStartY = 0.0
    private var lastFallEndY = 0.0
    private var lastRiseRealMs = 0.0
    private var lastFallRealMs = 0.0
    private var lastRiseTimerUsed = 0.0
    private var lastFallTimerUsed = 0.0
    private var lastRiseHeight = 0.0
    private var lastFallHeight = 0.0
    private var lastTotalTimer = 0.0

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (!player.isMoving) {
            mc.timer.timerSpeed = 1f
            return
        }

        if (player.onGround) {
            if (!wasOnGround) {
                onLand()
            }
            player.tryJump()
            jumpStartY = player.posY
            peakY = player.posY
            isRising = true
            wasOnGround = true
            riseStartNanos = System.nanoTime()
            riseTimerTicks = 0
            fallTimerTicks = 0
        } else {
            wasOnGround = false

            if (player.posY > peakY) {
                peakY = player.posY
            }

            val wasRising = isRising
            if (player.motionY <= 0 && isRising) {
                isRising = false
                fallStartNanos = System.nanoTime()
                riseRealTimeNanos = System.nanoTime() - riseStartNanos

                lastJumpPeakY = peakY
                lastJumpStartY = jumpStartY
                lastRiseHeight = peakY - jumpStartY

                if (wasRising && debug) {
                    val riseRealMs = riseRealTimeNanos / 1_000_000.0
                    val riseTimerUsed = riseTimerTicks * currentRiseTimer.toDouble()
                    chat("§a[Rise] §fHeight: §e${String.format("%.4f", lastRiseHeight)} §f| Timer: §b${String.format("%.3f", currentRiseTimer)} §f| RealMs: §d${String.format("%.2f", riseRealMs)} §f| TimerTicks: §c${riseTimerTicks} §f| TimerUsed: §c${String.format("%.2f", riseTimerUsed)}")
                }
            }

            val fallRatio = 1f - riseRatio

            if (isRising) {
                currentRiseTimer = riseTimerMax.coerceAtMost(1f / riseRatio)
                mc.timer.timerSpeed = currentRiseTimer
                riseTimerTicks++
            } else {
                val riseTimerUsed = riseTimerTicks * currentRiseTimer.toDouble()
                val fallTimerNeeded = if (fallTimerTicks > 0) {
                    (1.0 - riseTimerUsed) / fallTimerTicks
                } else {
                    val estimatedFallTicks = estimateFallTicks(peakY - player.posY, player.motionY)
                    if (estimatedFallTicks > 0) {
                        (1.0 - riseTimerUsed) / estimatedFallTicks
                    } else {
                        1f / fallRatio.toDouble()
                    }
                }
                currentFallTimer = fallTimerNeeded.coerceIn(0.01, 1.0 / fallRatio.toDouble()).toFloat()
                mc.timer.timerSpeed = currentFallTimer
                fallTimerTicks++
            }
        }
    }

    private fun onLand() {
        fallRealTimeNanos = System.nanoTime() - fallStartNanos

        lastFallEndY = mc.thePlayer.posY
        lastFallHeight = lastJumpPeakY - lastFallEndY
        lastRiseRealMs = riseRealTimeNanos / 1_000_000.0
        lastFallRealMs = fallRealTimeNanos / 1_000_000.0
        lastRiseTimerUsed = riseTimerTicks * currentRiseTimer.toDouble()
        lastFallTimerUsed = fallTimerTicks * currentFallTimer.toDouble()
        lastTotalTimer = lastRiseTimerUsed + lastFallTimerUsed

        if (debug) {
            val fallRealMs = lastFallRealMs
            chat("§c[Fall] §fHeight: §e${String.format("%.4f", lastFallHeight)} §f| Timer: §b${String.format("%.3f", currentFallTimer)} §f| RealMs: §d${String.format("%.2f", fallRealMs)} §f| TimerTicks: §c${fallTimerTicks} §f| TimerUsed: §c${String.format("%.2f", lastFallTimerUsed)}")
            chat("§9[Total] §fRiseTimerUsed: §a${String.format("%.2f", lastRiseTimerUsed)} §f+ FallTimerUsed: §c${String.format("%.2f", lastFallTimerUsed)} §f= §e${String.format("%.4f", lastTotalTimer)} §f| RiseHeight: §a${String.format("%.4f", lastRiseHeight)} §f| FallHeight: §c${String.format("%.4f", lastFallHeight)}")
            chat("§9[Time]  §fRiseRealMs: §a${String.format("%.2f", lastRiseRealMs)} §f| FallRealMs: §c${String.format("%.2f", lastFallRealMs)} §f| TotalRealMs: §e${String.format("%.2f", lastRiseRealMs + lastFallRealMs)}")
        }

        riseTimerTicks = 0
        fallTimerTicks = 0
    }

    private fun estimateFallTicks(remainingFall: Double, currentMotionY: Double): Int {
        var ticks = 0
        var y = 0.0
        var motionY = currentMotionY
        while (y > -remainingFall && ticks < 60) {
            motionY -= 0.08
            motionY *= 0.98
            y += motionY
            ticks++
        }
        return ticks
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        isRising = false
        wasOnGround = true
        riseTimerTicks = 0
        fallTimerTicks = 0
    }

    override fun onEnable() {
        mc.timer.timerSpeed = 1f
        isRising = false
        wasOnGround = true
        riseTimerTicks = 0
        fallTimerTicks = 0
        jumpStartY = 0.0
        peakY = 0.0
    }
}
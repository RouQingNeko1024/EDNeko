package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.network.play.client.C03PacketPlayer

object BalanceTimer : SpeedMode("BalanceTimer") {

    private var isRising = false
    private var wasOnGround = true
    private var jumpStartY = 0.0
    private var peakY = 0.0

    private var riseGameTicks = 0
    private var fallGameTicks = 0
    private var riseStartNanos = 0L
    private var fallStartNanos = 0L
    private var riseRealTimeNanos = 0L
    private var fallRealTimeNanos = 0L

    private var isBlinking = false
    private var blinkStartNanos = 0L
    private var lastPeakY = 0.0
    private var reachedNewPeak = false

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (!player.isMoving) {
            mc.timer.timerSpeed = 1f
            if (isBlinking) {
                stopBlink()
            }
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
            riseGameTicks = 0
            fallGameTicks = 0
            riseStartNanos = System.nanoTime()
            mc.timer.timerSpeed = Speed.balanceTimerRiseTimer
            riseGameTicks++
        } else {
            wasOnGround = false

            if (player.posY > peakY) {
                peakY = player.posY
            }

            if (player.motionY <= 0 && isRising) {
                isRising = false
                fallStartNanos = System.nanoTime()
                riseRealTimeNanos = System.nanoTime() - riseStartNanos

                if (Speed.balanceTimerAutoBlink) {
                    startBlink()
                }

                if (Speed.balanceTimerDebug) {
                    val riseRealMs = riseRealTimeNanos / 1_000_000.0
                    val riseTimerUsed = riseGameTicks * Speed.balanceTimerRiseTimer.toDouble()
                    val riseHeight = peakY - jumpStartY
                    chat("§a[Rise] §fHeight: §e${String.format("%.4f", riseHeight)} §f| Timer: §b${String.format("%.3f", Speed.balanceTimerRiseTimer)} §f| GameTicks: §c${riseGameTicks} §f| RealMs: §d${String.format("%.2f", riseRealMs)} §f| TimerUsed: §c${String.format("%.2f", riseTimerUsed)}")
                }
            }

            if (isBlinking) {
                val blinkElapsedMs = (System.nanoTime() - blinkStartNanos) / 1_000_000.0

                if (player.posY >= lastPeakY) {
                    reachedNewPeak = true
                    stopBlink()
                    if (Speed.balanceTimerDebug) {
                        chat("§d[Blink] §fReached new peak! §e${String.format("%.2f", blinkElapsedMs)}ms §f| PeakY: §a${String.format("%.4f", lastPeakY)} §f-> §b${String.format("%.4f", peakY)}")
                    }
                } else if (blinkElapsedMs >= Speed.balanceTimerBlinkThreshold) {
                    stopBlink()
                    if (Speed.balanceTimerDebug) {
                        chat("§d[Blink] §cThreshold exceeded! §e${String.format("%.2f", blinkElapsedMs)}ms §f> §c${Speed.balanceTimerBlinkThreshold}ms")
                    }
                }
            }

            if (isRising) {
                riseGameTicks++
                mc.timer.timerSpeed = Speed.balanceTimerRiseTimer
            } else {
                fallGameTicks++
                mc.timer.timerSpeed = Speed.balanceTimerFallTimer
            }
        }
    }

    override fun onPacket(event: PacketEvent) {
        if (!isBlinking) return
        if (event.eventType != net.ccbluex.liquidbounce.event.EventState.SEND) return

        val packet = event.packet
        if (packet is C03PacketPlayer) {
            event.cancelEvent()
            synchronized(BlinkUtils.packets) {
                BlinkUtils.packets += packet
            }
            if (packet.isMoving) {
                synchronized(BlinkUtils.positions) {
                    BlinkUtils.positions += net.minecraft.util.Vec3(packet.x, packet.y, packet.z)
                }
            }
        }
    }

    private fun startBlink() {
        if (isBlinking) return
        isBlinking = true
        blinkStartNanos = System.nanoTime()
        lastPeakY = peakY
        reachedNewPeak = false
        BlinkUtils.addFakePlayer()
        if (Speed.balanceTimerDebug) {
            chat("§d[Blink] §aStarted §fat peak §e${String.format("%.4f", peakY)}")
        }
    }

    private fun stopBlink() {
        if (!isBlinking) return
        isBlinking = false
        BlinkUtils.unblink()
        if (Speed.balanceTimerDebug) {
            val blinkElapsedMs = (System.nanoTime() - blinkStartNanos) / 1_000_000.0
            chat("§d[Blink] §cStopped §fafter §e${String.format("%.2f", blinkElapsedMs)}ms §f| ReachedPeak: ${if (reachedNewPeak) "§aYes" else "§cNo"}")
        }
    }

    private fun onLand() {
        fallRealTimeNanos = System.nanoTime() - fallStartNanos

        if (Speed.balanceTimerDebug) {
            val riseRealMs = riseRealTimeNanos / 1_000_000.0
            val fallRealMs = fallRealTimeNanos / 1_000_000.0
            val totalRealMs = riseRealMs + fallRealMs
            val riseTimerUsed = riseGameTicks * Speed.balanceTimerRiseTimer.toDouble()
            val fallTimerUsed = fallGameTicks * Speed.balanceTimerFallTimer.toDouble()
            val totalTimerUsed = riseTimerUsed + fallTimerUsed
            val totalGameTicks = riseGameTicks + fallGameTicks
            val fallHeight = peakY - mc.thePlayer.posY
            val riseHeight = peakY - jumpStartY

            chat("§c[Fall] §fHeight: §e${String.format("%.4f", fallHeight)} §f| Timer: §b${String.format("%.3f", Speed.balanceTimerFallTimer)} §f| GameTicks: §c${fallGameTicks} §f| RealMs: §d${String.format("%.2f", fallRealMs)} §f| TimerUsed: §c${String.format("%.2f", fallTimerUsed)}")
            chat("§9[Total] §fRiseTimerUsed: §a${String.format("%.2f", riseTimerUsed)} §f+ FallTimerUsed: §c${String.format("%.2f", fallTimerUsed)} §f= §e${String.format("%.2f", totalTimerUsed)} §f| TotalGameTicks: §e${totalGameTicks} §f| RiseHeight: §a${String.format("%.4f", riseHeight)} §f| FallHeight: §c${String.format("%.4f", fallHeight)}")
            chat("§9[Time]  §fRiseRealMs: §a${String.format("%.2f", riseRealMs)} §f| FallRealMs: §c${String.format("%.2f", fallRealMs)} §f| TotalRealMs: §e${String.format("%.2f", totalRealMs)}")
        }

        riseGameTicks = 0
        fallGameTicks = 0
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        isRising = false
        wasOnGround = true
        if (isBlinking) {
            stopBlink()
        }
    }

    override fun onEnable() {
        mc.timer.timerSpeed = 1f
        isRising = false
        wasOnGround = true
        jumpStartY = 0.0
        peakY = 0.0
        isBlinking = false
        reachedNewPeak = false
    }
}
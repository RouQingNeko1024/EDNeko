package net.ccbluex.liquidbounce.utils.timer

class TimerUtil {
    @JvmField
    var time: Long = System.currentTimeMillis()

    fun reset() {
        time = System.currentTimeMillis()
    }

    fun hasTimeElapsed(ms: Long): Boolean {
        return System.currentTimeMillis() - time >= ms
    }

    fun getTime(): Long {
        return System.currentTimeMillis() - time
    }
}

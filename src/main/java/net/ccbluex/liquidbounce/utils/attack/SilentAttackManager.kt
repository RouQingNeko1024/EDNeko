package net.ccbluex.liquidbounce.utils.attack

object SilentAttackManager {
    @Volatile
    private var silentAttack = false

    @JvmStatic
    fun isSilentAttack(): Boolean = silentAttack

    @JvmStatic
    fun <T> withSilentAttack(block: () -> T): T {
        silentAttack = true
        try {
            return block()
        } finally {
            silentAttack = false
        }
    }
}

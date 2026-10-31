package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
import org.lwjgl.input.Keyboard
import java.util.concurrent.CopyOnWriteArrayList
class WatchDogVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("WatchDog", parent) {

    val delay by float("Delay", 1f, 1f..50f)
    val noGround by boolean("ReleaseOnGround", true)
    val jumpReset by boolean("JumpReset", false)

    private data class DelayedPacket(
        val packet: net.minecraft.network.Packet<*>,
        val time: Long
    )

    private val packets = CopyOnWriteArrayList<DelayedPacket>()
    private var delaying = false
    private var conditionals = true
    private var attacked = false
    private var start = 0
    private var lastVelocity = 0.0
    private var offGroundTicks = 0

    override fun onEnable() {
        offGroundTicks = 0
    }

    override fun onDisable() {
        mc.gameSettings.keyBindJump.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindJump.keyCode)
        mc.gameSettings.keyBindForward.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.keyCode)
        start = 0
    }

    override fun onWorld(event: WorldEvent) {
        if (packets.isNotEmpty()) {
            flushAll()
            delaying = false
        }
    }

    override fun onAttack(event: AttackEvent) {
        attacked = true
    }

    override fun onMotion(event: MotionEvent) {
        if (!mc.thePlayer.onGround) offGroundTicks++ else offGroundTicks = 0

        if (event.eventState == EventState.POST) {
            conditionals = !mc.thePlayer.isInWeb && !mc.thePlayer.capabilities.isCreativeMode

            if (packets.isEmpty()) return

            val now = System.currentTimeMillis()
            val delayMs = (delay * 50).toLong()

            while (packets.isNotEmpty()) {
                val first = packets.first()
                val elapsed = now - first.time
                val shouldFlush = elapsed >= delayMs ||
                        offGroundTicks >= 13 ||
                        (noGround && mc.thePlayer.onGround)

                if (!shouldFlush) break
                flushAll()
            }

            if (!conditionals || !containsVelocity()) {
                flushAll()
            }
        }

        if (event.eventState == EventState.PRE && jumpReset) {
            if (lastVelocity > 0.4) {
                if (mc.thePlayer.hurtTime >= 8) {
                    mc.gameSettings.keyBindJump.pressed = true
                }
                if (mc.thePlayer.hurtTime >= 7 && !mc.gameSettings.keyBindForward.pressed) {
                    mc.gameSettings.keyBindForward.pressed = true
                    start = 1
                }
                if (mc.thePlayer.hurtTime < 7 && mc.thePlayer.hurtTime > 0) {
                    mc.gameSettings.keyBindJump.pressed = false
                    if (start == 1) {
                        mc.gameSettings.keyBindForward.pressed = false
                        start = 0
                    }
                }
            }
            if (mc.thePlayer.hurtTime == 0) {
                mc.gameSettings.keyBindForward.pressed = false
                mc.gameSettings.keyBindJump.pressed = false
            }
        }
    }

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        if (event.isCancelled || mc.thePlayer == null) return

        val packet = event.packet

        if (packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer.entityId) {
            if (conditionals && (!mc.thePlayer.onGround || !jumpReset) && lastVelocity > 0.4) {
                delaying = true
            }
        }

        if (!delaying) return

        packets.add(DelayedPacket(packet, System.currentTimeMillis()))
        event.cancelEvent()
    }

    private fun flushAll() {
        for (entry in packets) {
            PacketUtils.handlePackets(entry.packet)
        }
        packets.clear()
        delaying = false
    }

    private fun containsVelocity(): Boolean {
        val id = mc.thePlayer.entityId
        for (entry in packets) {
            if (entry.packet is S12PacketEntityVelocity &&
                (entry.packet as S12PacketEntityVelocity).entityID == id) {
                return true
            }
        }
        return false
    }
}
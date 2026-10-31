package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import kotlin.math.sqrt
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.server.S12PacketEntityVelocity
class AttackReduceVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("AttackReduce", parent) {

    val onlyPlayer by boolean("OnlyPlayer", true)
    val moveFix by boolean("MoveFix", true)
    val autoHold by boolean("AutoHold", true)
    val hurtTime by floatRange("HurtTime", 1f..10f, 1f..10f)
    val range by float("Range", 3f, 0f..6f)
    val cancelHit by boolean("CancelDoubleHit", true)
    val jumpReset by boolean("JumpReset", true)
    val onlyAir by boolean("OnlyAir", false)
    val swingMode by choices("SwingMode", arrayOf("Normal", "Packet"), "Normal")
    val debug by boolean("Debug", false)

    private var attacked = false

    private fun swing() {
        when (swingMode) {
            "Normal" -> mc.thePlayer.swingItem()
            "Packet" -> PacketUtils.sendPacket(C0APacketAnimation())
        }
    }

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        if (event.isCancelled || mc.thePlayer == null) return

        val packet = event.packet
        if (packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer.entityId) {
            val str = sqrt(
                packet.realMotionX * packet.realMotionX +
                packet.realMotionZ * packet.realMotionZ
            )

            parent.receivedVelocityMotionX = packet.realMotionX
            parent.receivedVelocityMotionY = packet.realMotionY
            parent.receivedVelocityMotionZ = packet.realMotionZ

            if (str >= 500.0) {
                attacked = false
            }
        }
    }

    override fun onAttack(event: AttackEvent) {
        if (!cancelHit) return
        if (!mc.thePlayer.isSprinting) return
        if (attacked) return

        val ht = mc.thePlayer.hurtTime.toFloat()
        if (ht >= hurtTime.start && ht <= hurtTime.endInclusive) {
            event.cancelEvent()
        }
    }

    override fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.PRE) return

        val hasVelocity = parent.receivedVelocityMotionX != 0.0 ||
                parent.receivedVelocityMotionY != 0.0 ||
                parent.receivedVelocityMotionZ != 0.0

        if (!hasVelocity) return

        val ht = mc.thePlayer.hurtTime.toFloat()
        if (ht < hurtTime.start || ht > hurtTime.endInclusive) {
            if (mc.thePlayer.hurtTime <= 0) {
                parent.receivedVelocityMotionX = 0.0
                parent.receivedVelocityMotionY = 0.0
                parent.receivedVelocityMotionZ = 0.0
                attacked = false
            }
            return
        }

        val target = KillAura.target
        if (target == null) return
        if (onlyPlayer && target !is EntityPlayer) return

        if (jumpReset && mc.thePlayer.isSprinting && mc.thePlayer.onGround) {
            mc.thePlayer.jump()
            if (debug) {
                // Jump reset applied
            }
        }

        if (mc.thePlayer.isSprinting && !(onlyAir && mc.thePlayer.onGround && attacked)) {
            if (cancelHit) {
                attacked = true
            }

            swing()
            PacketUtils.sendPacket(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
            mc.thePlayer.motionX *= 0.6
            mc.thePlayer.motionZ *= 0.6
            mc.thePlayer.isSprinting = false
            attacked = true
        }

        if (mc.thePlayer.hurtTime <= 0) {
            parent.receivedVelocityMotionX = 0.0
            parent.receivedVelocityMotionY = 0.0
            parent.receivedVelocityMotionZ = 0.0
            attacked = false
        }
    }
}
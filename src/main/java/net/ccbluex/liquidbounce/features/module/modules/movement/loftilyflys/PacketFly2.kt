package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.client.gui.GuiDownloadTerrain
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin

class PacketFly2(parent: LoftilyFly) : LoftilyFlyMode("Packet2", parent) {

    private val mode by choices("FlyMode", arrayOf("Factor", "Limit", "Rubber", "Pipeline"), "Factor")
    private val type by choices("RubberType", arrayOf("Empty", "Preserve", "Up", "Down", "Bounds", "Custom"), "Up")
    private val customX by float("CustomX", 0f, -1024f..1024f) { type == "Custom" }
    private val customY by float("CustomY", 0f, -1024f..1024f) { type == "Custom" }
    private val customZ by float("CustomZ", 290f, -1024f..1024f) { type == "Custom" }
    private val phase by choices("Phase", arrayOf("Full", "Semi", "Off"), "Full")
    private val limit by boolean("Limit", true)
    private val speed by float("PSpeed", 1f, 0f..10f)
    private val upSpeed by float("UpSpeed", 0.062f, 0.001f..1f)
    private val timer by float("PTimer", 1f, 0.1f..5f)
    private val increaseTicks by float("IncreaseTicks", 1f, 1f..20f)
    private val factor by float("Factor", 1f, 1f..10f)
    private val offset by float("POffset", 1337f, 0f..1337f)
    private val preset by boolean("MReset", false)
    private val rubberDelay by float("RubberBandDelay", 1f, 1f..10f)
    private val motionDelay by float("MotionPacketDelay", 1f, 1f..10f)
    private val rubberPackets by float("RubberBandPackets", 1f, 1f..10f)
    private val flagOnDisable by boolean("PacketOnDisable", false)
    private val cancelS08 by boolean("CancelS08", false)
    private val antiKick by boolean("AntiKick", false)
    private val antiKickOffset by float("AntiKickOffset", 0.04f, 0.01f..1f) { antiKick }
    private val upInterval by float("UpInterval", 20f, 1f..50f) { antiKick }
    private val interval by float("Interval", 4f, 1f..50f) { antiKick }

    private val pipeStep1Type by choices("Step1Type", arrayOf("Motion", "Rubber", "Empty", "MotionGround", "Confirm", "Skip"), "Motion") { mode == "Pipeline" }
    private val pipeStep1Delay by float("Step1Delay_ms", 0f, 0f..5000f) { mode == "Pipeline" }
    private val pipeStep2Type by choices("Step2Type", arrayOf("Motion", "Rubber", "Empty", "MotionGround", "Confirm", "Skip"), "Rubber") { mode == "Pipeline" }
    private val pipeStep2Delay by float("Step2Delay_ms", 0f, 0f..5000f) { mode == "Pipeline" }
    private val pipeStep3Type by choices("Step3Type", arrayOf("Motion", "Rubber", "Empty", "MotionGround", "Confirm", "Skip"), "Motion") { mode == "Pipeline" }
    private val pipeStep3Delay by float("Step3Delay_ms", 50f, 0f..5000f) { mode == "Pipeline" }
    private val pipeStep4Type by choices("Step4Type", arrayOf("Motion", "Rubber", "Empty", "MotionGround", "Confirm", "Skip"), "Rubber") { mode == "Pipeline" }
    private val pipeStep4Delay by float("Step4Delay_ms", 50f, 0f..5000f) { mode == "Pipeline" }
    private val pipeStep1Count by float("Step1Count", 1f, 1f..5f) { mode == "Pipeline" }
    private val pipeStep2Count by float("Step2Count", 1f, 1f..5f) { mode == "Pipeline" }
    private val pipeStep3Count by float("Step3Count", 1f, 1f..5f) { mode == "Pipeline" }
    private val pipeStep4Count by float("Step4Count", 1f, 1f..5f) { mode == "Pipeline" }

    private val teleports = ConcurrentHashMap<Int, Teleport>()
    private val pipelineQueue = ArrayDeque<LongArray>()
    private var ticks = 0
    private var factorTicks = 0
    private var teleportId = -1
    private var flip = false
    private var pipelineMotion = Vec3(0.0, 0.0, 0.0)
    private var pipelineCycleStart = 0L

    data class Teleport(val x: Double, val y: Double, val z: Double, val time: Long)

    override fun onEnable() {
        teleportId = -1
        mc.thePlayer?.let {
            ticks = 0
            teleportId = 0
        }
        factorTicks = 0
        pipelineQueue.clear()
        pipelineCycleStart = System.currentTimeMillis()
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1.0f
        if (flagOnDisable) {
            PacketUtils.sendPacket(C03PacketPlayer.C04PacketPlayerPosition())
        }
        pipelineQueue.clear()
        stopMovement(true)
    }

    private fun getTickCounter(n: Int): Boolean {
        ticks++
        if (ticks >= n) {
            ticks = 0
            return true
        }
        return false
    }

    private fun getWorldBorder(): Int {
        if (mc.isSingleplayer) return 1
        val n = (Math.random() * 29000000).toInt()
        return if (Math.random() > 0.5) n else -n
    }

    private fun getVectorByMode(delta: Vec3, current: Vec3): Vec3 {
        var result = current.add(delta)
        when (type) {
            "Preserve" -> result = Vec3(result.xCoord + getWorldBorder().toDouble(), result.yCoord, result.zCoord + getWorldBorder().toDouble())
            "Up" -> result = Vec3(result.xCoord, result.yCoord + offset.toDouble(), result.zCoord)
            "Down" -> result = Vec3(result.xCoord, result.yCoord - offset.toDouble(), result.zCoord)
            "Bounds" -> result = Vec3(result.xCoord, if (mc.thePlayer.posY <= 10.0) 255.0 else 1.0, result.zCoord)
            "Custom" -> result = Vec3(result.xCoord + customX.toDouble(), result.yCoord + customY.toDouble(), result.zCoord + customZ.toDouble())
        }
        return result
    }

    private fun sendPackets(delta: Vec3, needConfirm: Boolean) {
        val p = mc.thePlayer ?: return
        val current = p.positionVector.add(delta)
        val rubberPos = getVectorByMode(delta, current)

        if (p.ticksExisted % motionDelay.toInt() == 0) {
            PacketUtils.sendPacket(
                C03PacketPlayer.C04PacketPlayerPosition(current.xCoord, current.yCoord, current.zCoord, p.onGround),
                false
            )
        }

        if (p.ticksExisted % rubberDelay.toInt() == 0) {
            val rubberPacket = if (type == "Empty") {
                C03PacketPlayer.C04PacketPlayerPosition()
            } else {
                C03PacketPlayer.C04PacketPlayerPosition(rubberPos.xCoord, rubberPos.yCoord, rubberPos.zCoord, p.onGround)
            }
            repeat(rubberPackets.toInt()) {
                PacketUtils.sendPacket(rubberPacket, false)
            }
        }

        if (needConfirm) {
            teleportId++
            teleports[teleportId] = Teleport(current.xCoord, current.yCoord, current.zCoord, System.currentTimeMillis())
        }
    }

    private fun typeOrdinal(name: String): Long = when (name) {
        "Motion" -> 0
        "Rubber" -> 1
        "Empty" -> 2
        "MotionGround" -> 3
        "Confirm" -> 4
        else -> 5
    }

    private fun schedulePipelineCycle(delta: Vec3) {
        pipelineCycleStart = System.currentTimeMillis()
        pipelineMotion = delta
        val steps = arrayOf(
            longArrayOf(pipeStep1Delay.toDouble().toLong(), typeOrdinal(pipeStep1Type), pipeStep1Count.toDouble().toLong()),
            longArrayOf(pipeStep2Delay.toDouble().toLong(), typeOrdinal(pipeStep2Type), pipeStep2Count.toDouble().toLong()),
            longArrayOf(pipeStep3Delay.toDouble().toLong(), typeOrdinal(pipeStep3Type), pipeStep3Count.toDouble().toLong()),
            longArrayOf(pipeStep4Delay.toDouble().toLong(), typeOrdinal(pipeStep4Type), pipeStep4Count.toDouble().toLong())
        )
        for (step in steps) {
            if (step[1] == 5L) continue
            pipelineQueue.addLast(longArrayOf(pipelineCycleStart + step[0], step[1], step[2]))
        }
    }

    private fun flushPipelineQueue() {
        val p = mc.thePlayer ?: return
        val now = System.currentTimeMillis()
        val currentPos = p.positionVector
        val motionPos = currentPos.add(pipelineMotion)
        val rubberPos = getVectorByMode(pipelineMotion, motionPos)

        while (pipelineQueue.isNotEmpty() && pipelineQueue.peek() != null && pipelineQueue.peek()!![0] <= now) {
            val step = pipelineQueue.poll()
            val stepType = step[1].toInt()
            val count = step[2].toInt()

            repeat(count) {
                when (stepType) {
                    0 -> PacketUtils.sendPacket(
                        C03PacketPlayer.C04PacketPlayerPosition(motionPos.xCoord, motionPos.yCoord, motionPos.zCoord, false), false
                    )
                    1 -> {
                        val pos = if (type == "Empty") {
                            C03PacketPlayer.C04PacketPlayerPosition()
                        } else {
                            C03PacketPlayer.C04PacketPlayerPosition(rubberPos.xCoord, rubberPos.yCoord, rubberPos.zCoord, false)
                        }
                        PacketUtils.sendPacket(pos, false)
                    }
                    2 -> PacketUtils.sendPacket(C03PacketPlayer.C04PacketPlayerPosition(), false)
                    3 -> PacketUtils.sendPacket(
                        C03PacketPlayer.C04PacketPlayerPosition(motionPos.xCoord, motionPos.yCoord, motionPos.zCoord, true), false
                    )
                    4 -> {
                        teleportId++
                        teleports[teleportId] = Teleport(motionPos.xCoord, motionPos.yCoord, motionPos.zCoord, System.currentTimeMillis())
                    }
                }
            }
        }
    }

    override fun onTick() {
        val now = System.currentTimeMillis()
        teleports.entries.removeIf { now - it.value.time > 30000L }
        if (mode == "Pipeline") {
            flushPipelineQueue()
        }
    }

    override fun onPacketReceive(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        val p = mc.thePlayer ?: return
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook) {
            if (cancelS08) {
                val matched = teleports.entries.find { entry ->
                    val t = entry.value
                    Math.abs(t.x - packet.x) < 0.001 && Math.abs(t.y - packet.y) < 0.001 && Math.abs(t.z - packet.z) < 0.001
                }
                if (matched != null) {
                    teleports.remove(matched.key)
                }
                if (p.isEntityAlive && mc.theWorld?.isBlockLoaded(BlockPos(p.posX, p.posY, p.posZ)) == true
                    && mc.currentScreen !is GuiDownloadTerrain
                    && mode != "Rubber" && mode != "Pipeline"
                    && matched != null
                ) {
                    event.cancelEvent()
                    return
                }
                packet.yaw = p.rotationYaw
                packet.pitch = p.rotationPitch
                teleportId = 0
            }
        }
    }
    override fun onPacketSend(event: PacketEvent) {
        if (event.eventType != EventState.SEND) return
        if (event.packet is C03PacketPlayer) {
            event.cancelEvent()
        }
    }

    override fun onMove(event: MoveEvent) {
        val p = mc.thePlayer ?: return
        if (mode != "Rubber" && mode != "Pipeline" && teleportId == 0) return

        event.x = p.motionX
        event.y = p.motionY
        event.z = p.motionZ

        val boundingBox = p.entityBoundingBox.expand(-0.0625, -0.0625, -0.0625)
        val hasCollision = mc.theWorld?.getCollisionBoxes(boundingBox)?.isNotEmpty() == true
        if (phase != "Off" && (phase == "Semi" || hasCollision)) {
            p.noClip = true
        }
    }

    override fun onPreMotion(event: MotionEvent) {
        val p = mc.thePlayer ?: return

        if (timer != 1.0f) {
            mc.timer.timerSpeed = timer
        }
        p.motionY = 0.0
        if (preset) {
            p.motionX = 0.0
            p.motionZ = 0.0
        }

        if (mode == "Pipeline") {
            val boundingBox = p.entityBoundingBox.expand(-0.0625, -0.0625, -0.0625)
            val hasCollision = mc.theWorld?.getCollisionBoxes(boundingBox)?.isNotEmpty() == true
            var dy = 0.0
            if (mc.gameSettings.keyBindJump.pressed) {
                dy = upSpeed.toDouble()
            } else if (mc.gameSettings.keyBindSneak.pressed) {
                dy = -upSpeed.toDouble()
            }
            val fwd = forward(if (phase == "Full" && hasCollision) 0.034444444444444444 else speed.toDouble() * 0.26)
            p.motionY = dy
            p.motionX = fwd[0]
            p.motionZ = fwd[1]
            val pipelineDelta = Vec3(p.motionX, p.motionY, p.motionZ)
            if (pipelineQueue.isEmpty()) {
                schedulePipelineCycle(pipelineDelta)
            }
            flushPipelineQueue()
            return
        }

        if (mode != "Rubber" && teleportId == 0) {
            if (getTickCounter(4)) {
                sendPackets(Vec3(0.0, 0.0, 0.0), false)
            }
            return
        }

        val boundingBox = p.entityBoundingBox.expand(-0.0625, -0.0625, -0.0625)
        val hasCollision = mc.theWorld?.getCollisionBoxes(boundingBox)?.isNotEmpty() == true
        var dy = 0.0

        if (mc.gameSettings.keyBindJump.pressed && (hasCollision || !p.isMoving)) {
            dy = if (antiKick && !hasCollision) {
                if (getTickCounter(if (mode == "Rubber") (upInterval / 2).toInt() else upInterval.toInt())) {
                    -upSpeed.toDouble() / 2.0
                } else {
                    upSpeed.toDouble()
                }
            } else {
                upSpeed.toDouble()
            }
        } else if (mc.gameSettings.keyBindSneak.pressed) {
            dy = -upSpeed.toDouble()
        } else if (antiKick && !hasCollision) {
            if (getTickCounter(interval.toInt())) {
                dy = -antiKickOffset.toDouble()
            }
        }

        if (phase == "Full" && hasCollision && p.isMoving && dy != 0.0) {
            dy = if (mc.gameSettings.keyBindJump.pressed) dy / 2.5 else dy / 1.5
        }

        val fwd = forward(if (phase == "Full" && hasCollision) 0.034444444444444444 else speed.toDouble() * 0.26)
        var n = factor.toInt()

        if (mode == "Factor" && p.ticksExisted % increaseTicks.toDouble() == 0.0) {
            factorTicks++
            if (factorTicks > factor.toInt()) {
                n++
                factorTicks = 0
            }
        }

        for (i in 1..n) {
            if (mode == "Limit") {
                if (p.ticksExisted % 2 == 0) {
                    if (flip && dy >= 0.0) {
                        flip = false
                        dy = -upSpeed.toDouble() / 2.0
                    }
                    p.motionY = dy * i
                    p.motionX = fwd[0] * i
                    p.motionZ = fwd[1] * i
                    sendPackets(Vec3(p.motionX, p.motionY, p.motionZ), !limit)
                    continue
                }
                if (dy < 0.0) {
                    flip = true
                }
                continue
            }
            p.motionY = dy * i
            p.motionX = fwd[0] * i
            p.motionZ = fwd[1] * i
            sendPackets(Vec3(p.motionX, p.motionY, p.motionZ), mode != "Rubber")
        }
    }

    private fun forward(d: Double): DoubleArray {
        val forwardInput = mc.thePlayer?.movementInput?.moveForward ?: 0f
        val strafeInput = mc.thePlayer?.movementInput?.moveStrafe ?: 0f
        var yaw = mc.thePlayer?.rotationYaw ?: 0f

        var f = forwardInput
        var s = strafeInput
        if (f != 0f) {
            if (s > 0f) {
                yaw += if (f > 0f) -45f else 45f
            } else if (s < 0f) {
                yaw += if (f > 0f) 45f else -45f
            }
            s = 0f
            f = if (f > 0f) 1f else -1f
        }
        val rad = Math.toRadians((yaw + 90f).toDouble())
        val sinYaw = sin(rad)
        val cosYaw = cos(rad)
        return doubleArrayOf(f.toDouble() * d * cosYaw + s.toDouble() * d * sinYaw, f.toDouble() * d * sinYaw - s.toDouble() * d * cosYaw)
    }
}

package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.TimerUtil
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.util.Vec3
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class PacketFly3 : Module("PacketFly3", Category.MOVEMENT) {

    private val mode by choices("Mode", arrayOf("Matrix", "NCP"), "Matrix")
    private val horizontal by float("Horizontal", 2.0f, 0.1f..10.0f)
    private val vertical by float("Vertical", 4.0f, 0.1f..10.0f)
    private val speed by float("Speed", 1.0f, 0.1f..5.0f)
    private val boost by float("Boost", 1.5f, 1.0f..5.0f)
    private val boostDelay by float("BoostDelay", 400.0f, 100.0f..2000.0f)
    private val packetDelay by float("PacketDelay", 50.0f, 10.0f..2000.0f)
    private val teleportDelay by float("TeleportDelay", 500.0f, 100.0f..5000.0f)
    private val maxPackets by float("MaxPackets", 50.0f, 10.0f..200.0f)
    private val freeze by boolean("Freeze", false)
    private val noClip by boolean("NoClip", true)
    private val spoofGround by boolean("SpoofGround", true)
    private val antiKick by boolean("AntiKick", true)
    private val smartPackets by boolean("SmartPackets", true)
    private val resetOnLag by boolean("ResetOnLag", true)
    private val autoDisable by boolean("AutoDisable", true)

    private val packetTimer = TimerUtil()
    private val teleportTimer = TimerUtil()
    private val lagTimer = TimerUtil()
    private val boostTimer = TimerUtil()

    private val random = Random(System.currentTimeMillis())

    private val outgoingPackets = ConcurrentLinkedQueue<Packet<*>>()
    private val incomingPackets = ConcurrentLinkedQueue<Packet<*>>()
    private val delayedPackets = CopyOnWriteArrayList<Packet<*>>()
    private val positions = CopyOnWriteArrayList<Vec3>()

    private var initialized = false
    private var flying = false
    private var receivedCorrection = false
    private var waitingTeleport = false
    private var processingPacket = false
    private var shouldResync = false
    private var packetDesync = false
    private var matrixState = false
    private var ncpState = false

    private var packetCounter = 0
    private var teleportCounter = 0
    private var correctionCounter = 0
    private var failedPackets = 0
    private var ticksSinceEnable = 0
    private var ticksSinceTeleport = 0
    private var ticksSincePacket = 0
    private var groundTicks = 0
    private var airTicks = 0
    private var boostTicks = 0
    private var lagTicks = 0

    private var lastX = 0.0
    private var lastY = 0.0
    private var lastZ = 0.0
    private var lastYaw = 0f
    private var lastPitch = 0f
    private var serverX = 0.0
    private var serverY = 0.0
    private var serverZ = 0.0
    private var motionX = 0.0
    private var motionY = 0.0
    private var motionZ = 0.0
    private var lastMotionX = 0.0
    private var lastMotionY = 0.0
    private var lastMotionZ = 0.0
    private var rotationYaw = 0f
    private var rotationPitch = 0f

    private var lastPosition: Vec3? = null
    private var serverPosition: Vec3? = null
    private var teleportPosition: Vec3? = null
    private var serverGround = false
    private var lastServerGround = false

    override fun onEnable() {
        initialized = false
        flying = false
        receivedCorrection = false
        waitingTeleport = false
        processingPacket = false
        shouldResync = false
        packetDesync = false
        matrixState = false
        ncpState = false
        packetCounter = 0
        teleportCounter = 0
        correctionCounter = 0
        failedPackets = 0
        ticksSinceEnable = 0
        ticksSinceTeleport = 0
        ticksSincePacket = 0
        groundTicks = 0
        airTicks = 0
        boostTicks = 0
        lagTicks = 0
        outgoingPackets.clear()
        incomingPackets.clear()
        delayedPackets.clear()
        positions.clear()
        packetTimer.reset()
        teleportTimer.reset()
        lagTimer.reset()
        boostTimer.reset()
        mc.thePlayer?.let {
            lastX = it.posX
            lastY = it.posY
            lastZ = it.posZ
            serverX = lastX
            serverY = lastY
            serverZ = lastZ
            lastPosition = Vec3(lastX, lastY, lastZ)
            serverPosition = Vec3(serverX, serverY, serverZ)
            lastYaw = it.rotationYaw
            lastPitch = it.rotationPitch
            motionZ = 0.0
            motionY = 0.0
            motionX = 0.0
            serverGround = it.onGround
            lastServerGround = serverGround
        }
        initialized = true
    }

    override fun onDisable() {
        mc.thePlayer?.let {
            it.motionX = 0.0
            it.motionY = 0.0
            it.motionZ = 0.0
        }
        flushPackets()
        outgoingPackets.clear()
        incomingPackets.clear()
        delayedPackets.clear()
        positions.clear()
        initialized = false
        flying = false
        waitingTeleport = false
        receivedCorrection = false
        packetDesync = false
    }

    init {
        handler<GameTickEvent> {
            if (!initialized || mc.thePlayer == null) return@handler
            ticksSinceEnable++
            ticksSinceTeleport++
            ticksSincePacket++
            updateEnvironment()
            updateState()
            if (mode == "Matrix") {
                handleMatrix()
            } else {
                handleNCP()
            }
            handleAntiKick()
            handleBoost()
            handleResync()
            handlePacketQueue()
            if (freeze) freezePlayer()
            if (noClip) applyNoClip()
            updatePositionCache()
        }

        handler<MotionEvent> { event ->
            if (!initialized || mc.thePlayer == null) return@handler
            if (event.eventState != EventState.PRE) return@handler
            rotationYaw = mc.thePlayer.rotationYaw
            rotationPitch = mc.thePlayer.rotationPitch
            if (mode == "Matrix") {
                matrixMotion(event)
            } else {
                ncpMotion(event)
            }
            if (spoofGround) {
                event.onGround = getSpoofedGround()
            }
        }

        handler<StrafeEvent> { event ->
            if (!initialized || mc.thePlayer == null || !flying) return@handler
            var f = horizontal
            f = if (mode == "Matrix") f * 0.98f else f * 0.97f
            if (boostTicks > 0) f *= boost
            MovementUtils.strafe(f)
        }

        handler<PacketEvent> { event ->
            when (event.eventType) {
                EventState.RECEIVE -> {
                    if (!initialized || event.packet == null) return@handler
                    val packet = event.packet
                    when (packet) {
                        is S08PacketPlayerPosLook -> handleCorrection(packet)
                        is S12PacketEntityVelocity -> handleVelocity(packet)
                    }
                    if (isMovementPacket(packet)) {
                        handleMovementPacket(packet)
                    }
                    if (processingPacket) return@handler
                    if (smartPackets) analyzePacket(packet)
                }
                EventState.SEND -> {
                    val packet = event.packet
                    if (packet is C03PacketPlayer) {
                        event.cancelEvent()
                    }
                }
                else -> {}
            }
        }

        handler<WorldEvent> {
            onDisable()
        }
    }

    private fun updateEnvironment() {
        val p = mc.thePlayer ?: return
        lastServerGround = serverGround
        serverGround = p.onGround
        if (p.onGround) {
            groundTicks++
            airTicks = 0
        } else {
            airTicks++
            groundTicks = 0
        }
        if (lastPosition == null) {
            lastPosition = Vec3(p.posX, p.posY, p.posZ)
        }
        motionX = p.posX - lastPosition!!.xCoord
        motionY = p.posY - lastPosition!!.yCoord
        motionZ = p.posZ - lastPosition!!.zCoord
    }

    private fun updateState() {
        if (receivedCorrection) {
            flying = false
            waitingTeleport = true
        }
        if (ticksSinceEnable > 3 && !waitingTeleport) {
            flying = true
        }
        if (ticksSinceTeleport > 40) {
            waitingTeleport = false
        }
        if (failedPackets > 8) {
            shouldResync = true
        }
        if (lagTimer.hasTimeElapsed(1500L)) {
            lagTicks = 0
        }
    }

    private fun handleMatrix() {
        matrixState = true
        ncpState = false
        val mv = calculateMovement()
        if (freeze) return
        if (airTicks > 0) {
            applyMatrixMovement(mv[0], mv[1], mv[2])
        }
        if (packetTimer.hasTimeElapsed(packetDelay.toLong())) {
            sendMatrixPacket()
            packetTimer.reset()
        }
        if (ticksSinceEnable > 4) {
            matrixState = true
        }
    }

    private fun handleNCP() {
        ncpState = true
        matrixState = false
        val mv = calculateMovement()
        if (airTicks > 0) {
            applyNCPMovement(mv[0], mv[1], mv[2])
        }
        if (packetTimer.hasTimeElapsed(packetDelay.toLong())) {
            sendNCPPacket()
            packetTimer.reset()
        }
        if (airTicks > 5) {
            ncpState = true
        }
    }

    private fun matrixMotion(event: MotionEvent) {
        if (!flying) return
        val mv = calculateMovement()
        if (mv[0] != 0.0) {
            event.x += mv[0] * 0.1
        }
        if (mv[2] != 0.0) {
            event.z += mv[2] * 0.1
        }
        if (antiKick) {
            event.y += getMatrixVertical()
        }
    }

    private fun ncpMotion(event: MotionEvent) {
        if (!flying) return
        val mv = calculateMovement()
        event.x += mv[0] * 0.05
        event.z += mv[2] * 0.05
        event.y += getNCPVertical()
    }

    private fun calculateMovement(): DoubleArray {
        val p = mc.thePlayer ?: return doubleArrayOf(0.0, 0.0, 0.0)
        val f = p.movementInput.moveForward.toDouble()
        val f2 = p.movementInput.moveStrafe.toDouble()
        val f3 = p.rotationYaw.toDouble()
        if (f == 0.0 && f2 == 0.0) {
            return doubleArrayOf(0.0, 0.0, 0.0)
        }
        val d = kotlin.math.atan2(f2, f)
        val d2 = Math.toRadians(f3) + d
        var d3 = horizontal.toDouble()
        if (boostTicks > 0) {
            d3 *= boost.toDouble()
        }
        val d4 = -sin(d2) * d3
        val d5 = cos(d2) * d3
        return doubleArrayOf(d4, getVerticalMotion(), d5)
    }

    private fun getVerticalMotion(): Double {
        val p = mc.thePlayer ?: return 0.0
        return if (p.movementInput.jump) {
            vertical.toDouble()
        } else if (mc.gameSettings.keyBindSneak.pressed) {
            -vertical.toDouble()
        } else {
            0.0
        }
    }

    private fun getMatrixVertical(): Double {
        return if (ticksSinceEnable % 2 == 0) {
            -0.031
        } else {
            0.0
        }
    }

    private fun getNCPVertical(): Double {
        return if (ticksSinceEnable % 3 == 0) {
            -0.031
        } else {
            0.0
        }
    }

    private fun applyMatrixMovement(motionX2: Double, motionY2: Double, motionZ2: Double) {
        val p = mc.thePlayer ?: return
        lastMotionX = motionX
        lastMotionY = motionY
        lastMotionZ = motionZ
        motionX = motionX2
        motionY = motionY2
        motionZ = motionZ2
    }

    private fun applyNCPMovement(motionX2: Double, motionY2: Double, motionZ2: Double) {
        val p = mc.thePlayer ?: return
        lastMotionX = motionX
        lastMotionY = motionY
        lastMotionZ = motionZ
        motionX = motionX2
        motionY = motionY2
        motionZ = motionZ2
    }

    private fun sendMatrixPacket() {
        val p = mc.thePlayer ?: return
        val pos = serverPosition ?: p.positionVector
        packetCounter++
        outgoingPackets.add(
            C03PacketPlayer.C04PacketPlayerPosition(
                pos.xCoord + motionX,
                pos.yCoord + motionY,
                pos.zCoord + motionZ,
                getSpoofedGround()
            )
        )
    }

    private fun sendNCPPacket() {
        val p = mc.thePlayer ?: return
        packetCounter++
        outgoingPackets.add(
            C03PacketPlayer.C04PacketPlayerPosition(
                p.posX + motionX,
                p.posY + motionY,
                p.posZ + motionZ,
                p.onGround
            )
        )
    }

    private fun handleCorrection(packet: S08PacketPlayerPosLook) {
        val p = mc.thePlayer ?: return
        teleportPosition = Vec3(packet.x, packet.y, packet.z)
        serverPosition = Vec3(packet.x, packet.y, packet.z)
        serverX = packet.x
        serverY = packet.y
        serverZ = packet.z
        receivedCorrection = true
        correctionCounter++
        ticksSinceTeleport = 0
        lagTicks++
        if (resetOnLag) {
            failedPackets = 0
        }
        if (autoDisable && correctionCounter >= 3) {
            state = false
        }
    }

    private fun handleVelocity(packet: S12PacketEntityVelocity) {
        val p = mc.thePlayer ?: return
        if (packet.entityID != p.entityId) return
        if (!flying) return
        motionX *= 0.5
        motionY *= 0.5
        motionZ *= 0.5
        p.motionX *= 0.5
        p.motionY *= 0.5
        p.motionZ *= 0.5
    }

    private fun handleMovementPacket(packet: Packet<*>) {
        incomingPackets.offer(packet)
        if (incomingPackets.size > 50) {
            incomingPackets.poll()
        }
    }

    private fun analyzePacket(packet: Packet<*>) {
        when (packet) {
            is S08PacketPlayerPosLook -> {
                packetDesync = true
                lagTicks++
            }
            is S12PacketEntityVelocity -> {
                lagTicks++
            }
        }
    }

    private fun isMovementPacket(packet: Packet<*>): Boolean {
        return packet is C03PacketPlayer
    }

    private fun handleAntiKick() {
        if (!antiKick || !flying) return
        if (ticksSinceEnable % 20 != 0) return
        val p = mc.thePlayer ?: return
        if (abs(p.motionY) < 0.01) {
            p.motionY = -0.031
        }
    }

    private fun handleBoost() {
        if (!flying) {
            boostTicks = 0
            return
        }
        if (!boostTimer.hasTimeElapsed(boostDelay.toLong())) return
        if (boostTicks < 4) boostTicks++
        boostTimer.reset()
    }

    private fun handleResync() {
        if (!shouldResync) return
        if (!teleportTimer.hasTimeElapsed(teleportDelay.toLong())) return
        val p = mc.thePlayer ?: return
        val sp = serverPosition
        if (sp != null) {
            val d = sp.xCoord - p.posX
            val d2 = sp.yCoord - p.posY
            val d3 = sp.zCoord - p.posZ
            if (abs(d) > 3.0 || abs(d2) > 3.0 || abs(d3) > 3.0) {
                p.setPosition(sp.xCoord, sp.yCoord, sp.zCoord)
            }
        }
        receivedCorrection = false
        shouldResync = false
        packetDesync = false
        waitingTeleport = false
        teleportCounter++
        ticksSinceTeleport = 0
        teleportTimer.reset()
    }

    private fun handlePacketQueue() {
        if (outgoingPackets.isEmpty()) return
        if (checkPacketOverflow()) {
            trimQueues()
        }
        flushPackets()
    }

    private fun updatePositionCache() {
        val p = mc.thePlayer ?: return
        lastX = p.posX
        lastY = p.posY
        lastZ = p.posZ
        lastPosition = Vec3(lastX, lastY, lastZ)
        positions.add(lastPosition!!)
        if (positions.size > 40) {
            positions.removeAt(0)
        }
    }

    private fun getSpoofedGround(): Boolean {
        if (!spoofGround) return mc.thePlayer?.onGround ?: false
        return if (mode == "Matrix") {
            groundTicks > 0 || (packetCounter and 3) == 0
        } else {
            (packetCounter and 1) == 0
        }
    }

    private fun freezePlayer() {
        val p = mc.thePlayer ?: return
        p.motionX = 0.0
        p.motionY = 0.0
        p.motionZ = 0.0
    }

    private fun applyNoClip() {
        val p = mc.thePlayer ?: return
        p.noClip = true
    }

    private fun flushPackets() {
        var packet = outgoingPackets.poll()
        while (packet != null) {
            try {
                PacketUtils.sendPacket(packet, false)
            } catch (_: Throwable) {
            }
            packet = outgoingPackets.poll()
        }
        delayedPackets.clear()
    }

    private fun resetMotion() {
        motionZ = 0.0
        motionY = 0.0
        motionX = 0.0
        lastMotionZ = 0.0
        lastMotionY = 0.0
        lastMotionX = 0.0
    }

    private fun checkPacketOverflow(): Boolean {
        return outgoingPackets.size >= maxPackets.toInt()
    }

    private fun trimQueues() {
        while (outgoingPackets.size > maxPackets.toInt()) {
            outgoingPackets.poll()
        }
        while (incomingPackets.size > 50) {
            incomingPackets.poll()
        }
        while (delayedPackets.size > maxPackets.toInt() * 2) {
            delayedPackets.removeAt(0)
        }
    }

    private fun updatePacketState() {
        val dist = getDistanceFromServer()
        packetDesync = dist > 2.5
        if (packetDesync) {
            lagTicks++
        } else if (lagTicks > 0) {
            lagTicks--
        }
    }

    private fun updateTeleportState() {
        if (waitingTeleport && teleportPosition != null && getDistanceFromServer() < 1.0) {
            waitingTeleport = false
            receivedCorrection = false
        }
    }

    private fun updateFlightState() {
        val p = mc.thePlayer ?: return
        if (p.isDead) {
            flying = false
            return
        }
        if (waitingTeleport) {
            flying = false
            return
        }
        if (ticksSinceEnable < 3) {
            flying = false
            return
        }
        flying = true
    }

    private fun processInternalState() {
        updatePacketState()
        updateTeleportState()
        updateFlightState()
        trimQueues()
    }

    private fun normalizeState() {
        if (packetCounter < 0) packetCounter = 0
        if (failedPackets < 0) failedPackets = 0
        if (correctionCounter < 0) correctionCounter = 0
        if (ticksSinceEnable < 0) ticksSinceEnable = 0
    }

    private fun tickInternal() {
        processInternalState()
        normalizeState()
        if (!validateMovement()) {
            resetMotion()
        }
    }

    private fun getDistanceFromServer(): Double {
        val p = mc.thePlayer ?: return 0.0
        val sp = serverPosition ?: return 0.0
        val d = p.posX - sp.xCoord
        val d2 = p.posY - sp.yCoord
        val d3 = p.posZ - sp.zCoord
        return sqrt(d * d + d2 * d2 + d3 * d3)
    }

    private fun getHorizontalDistance(): Double {
        val lp = lastPosition ?: return 0.0
        val p = mc.thePlayer ?: return 0.0
        val d = p.posX - lp.xCoord
        val d2 = p.posZ - lp.zCoord
        return sqrt(d * d + d2 * d2)
    }

    private fun validateMovement(): Boolean {
        val mv = calculateMovement()
        val d = sqrt(mv[0] * mv[0] + mv[2] * mv[2])
        return d <= horizontal.toDouble() * 2.5
    }

    private fun randomOffset(): Double {
        return (random.nextDouble() - 0.5) * 1.0E-4
    }

    private fun stableOffset(d: Double): Double {
        val factor = Math.pow(10.0, 5.0)
        return kotlin.math.round(d * factor) / factor
    }

    private fun getMovementVector(): Vec3 {
        val mv = calculateMovement()
        return Vec3(mv[0], mv[1], mv[2])
    }

    private fun getServerDelta(): Vec3 {
        val p = mc.thePlayer ?: return Vec3(0.0, 0.0, 0.0)
        val sp = serverPosition ?: return Vec3(0.0, 0.0, 0.0)
        return Vec3(sp.xCoord - p.posX, sp.yCoord - p.posY, sp.zCoord - p.posZ)
    }

    private fun isNearCorrection(): Boolean {
        return getDistanceFromServer() < 1.5
    }

    private fun isPacketRateStable(): Boolean {
        return ticksSincePacket < 5 && packetCounter > 2
    }

    private fun shouldApplyBoost(): Boolean {
        return flying && boostTicks > 0 && !waitingTeleport && !packetDesync
    }

    private fun calculateServerOffset() {
        val sp = serverPosition ?: return
        val f4 = Math.pow(10.0, 4.0)
        serverX = kotlin.math.round(sp.xCoord * f4) / f4
        serverY = kotlin.math.round(sp.yCoord * f4) / f4
        serverZ = kotlin.math.round(sp.zCoord * f4) / f4
    }

    private fun updateServerPosition(x: Double, y: Double, z: Double) {
        serverX = x
        serverY = y
        serverZ = z
        serverPosition = Vec3(x, y, z)
    }
}
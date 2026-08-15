/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
//NyaSoft SKID NekoBounce
//NekoPaura
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.client.realX
import net.ccbluex.liquidbounce.utils.client.realY
import net.ccbluex.liquidbounce.utils.client.realZ
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.kotlin.StringUtils.contains
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBacktrackBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomDelay
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager.color
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.play.server.*
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.network.status.server.S01PacketPong
import net.minecraft.util.Vec3
import net.minecraft.world.WorldSettings
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.cos

@Suppress("unused")
object Backtrack : Module("Backtrack", Category.COMBAT) {

    private val nextBacktrackDelay by int("NextBacktrackDelay", 0, 0..10000) { mode == "Modern" }
    private val maxDelay: Value<Int> = int("MaxDelay", 80, 0..10000,"ms").onChange { _, new ->
        new.coerceAtLeast(minDelay.get())
    }
    private val minDelay: Value<Int> = int("MinDelay", 80, 0..10000,"ms") {
        mode == "Modern"
    }.onChange { _, new ->
        new.coerceAtMost(maxDelay.get())
    }

    val mode by choices("Mode", arrayOf("Legacy", "Modern"), "Modern").onChanged {
        clearPackets()
        backtrackedPlayer.clear()
        started = false
    }
    val extraMode by choices("ExtraMode",arrayOf("Polar","Intave","NoExtra"),"NoExtra") { mode == "Modern" }

    val startMode by choices("StartMode", arrayOf("InRange", "onAttack"), "InRange") { mode == "Modern" }

    private val legacyPos by choices(
        "Caching mode", arrayOf("ClientPos", "ServerPos"), "ClientPos"
    ) { mode == "Legacy" }

    private val style by choices("Style", arrayOf("Pulse", "Smooth", "Instant", "Wave", "Step", "Bounce"), "Smooth") { mode == "Modern" }

    private val waveFrequency by float("Wave-Frequency", 2f, 0.5f..5f) { style == "Wave" }
    private val stepCount by int("Step-Count", 3, 2..10) { style == "Step" }
    private val bounceIntensity by float("Bounce-Intensity", 0.3f, 0.1f..1f) { style == "Bounce" }
    private val instantThreshold by int("Instant-Threshold", 50, 0..200, "ms") { style == "Instant" }

    private val tagMode by choices("TagMode",arrayOf("Normal","WorkRange","PacketQueueSize","Custom","None"),"Normal") { mode == "Modern" }
    private val customTagText by text("TagText","") {tagMode == "Custom"}
    private val delayUpdateMode by choices("DelayUpdateMode", arrayOf("Normal", "Attack", "EveryTick","KillPlayer"), "Normal") {
        mode == "Modern"
    }
    private val attackCountToUpdate by int("AttackCountToUpdateDelay",1,0..10) { mode == "Modern" && delayUpdateMode == "Attack" }

    private val distanceMode by choices("DistanceMode", arrayOf("Custom", "Smart"), "Custom") { mode == "Modern" }
    private val distance by floatRange("Distance", 2f..3f, 0f..10f) { mode == "Modern" && distanceMode == "Custom" }
    private val expandRange by float("ExpandRange", 1f, 0f..5f) { mode == "Modern" && distanceMode == "Smart" }
    private val minExpandRange by float("MinExpandRange", 0f, 0f..5f) { mode == "Modern" && distanceMode == "Smart" }

    private val trackingBuffer by int("TrackingBuffer",0,0..10000,"ms") {mode == "Modern"}
    private val ownhurtTime by intRange("OwnHurtTime",0..10,0..10) {mode == "Modern"}
    private val enemyhurtTime by intRange("EnemyHurtTime",0..10,0..10) {mode == "Modern"}
    private val smart by boolean("Smart", true) { mode == "Modern" }
    private val autoRestart by boolean("AutoRestart", true) { mode == "Modern" }
    private val autoRestartDelayMode by choices("AutoRestartDelayMode",arrayOf("FollowTrueBacktrackDelay","Custom"),"FollowTrueBackTrackDelay") {mode == "Modern" && autoRestart}
    private val autoRestartDelay by int("AutoRestartDelay", 1000, 50..30000, "ms") {
        mode == "Modern" && autoRestart && autoRestartDelayMode == "Custom"
    }
    private val autoRestartDelayFactor by float("FollowFactor", 1.0f, 0.0f..10f,"x"){
        mode == "Modern" && autoRestart && autoRestartDelayMode == "FollowTrueBacktrackDelay"
    }
    private val onlyOnKillAura by boolean("OnlyOnKillAura",false) { mode == "Modern" }

    private val espMode by choices(
        "ESP-Mode",
        arrayOf("None", "Box", "Model", "Wireframe", "Outline", "Tracer", "2DBox", "HealthBar", "Arrow"),
        "Box"
    ) { mode == "Modern" }.subjective()

    private val wireframeWidth by float("WireFrame-Width", 1f, 0.5f..5f) { espMode == "Wireframe" }
    private val outlineWidth by float("Outline-Width", 2f, 0.5f..5f) { espMode == "Outline" }
    private val tracerWidth by float("Tracer-Width", 1.5f, 0.5f..5f) { espMode == "Tracer" }
    private val arrowSize by float("Arrow-Size", 15f, 5f..30f) { espMode == "Arrow" }
    private val showHealth by boolean("Show-Health", true) { espMode == "2DBox" || espMode == "HealthBar" }
    private val showDistance by boolean("Show-Distance", true) { espMode == "2DBox" || espMode == "Arrow" }

    private val espColor =
        ColorSettingsInteger(this, "ESPColor") { espMode != "Model" && mode == "Modern" }.with(0, 255, 0)

    private val packetQueue = ConcurrentLinkedQueue<QueueData>()
    private val positions = ConcurrentLinkedQueue<Pair<Vec3, Long>>()
    private val showProgressBar by boolean("ShowProgressBar", true) { mode == "Modern" }.subjective()
    private val progressBarPosition by choices("ProgressBarPosition", arrayOf("TopLeft", "TopRight", "BottomLeft", "BottomRight","BottomCenter"), "BottomCenter") {
        mode == "Modern" && showProgressBar
    }
    private val progressBarWidth by int("ProgressBarWidth", 150, 50..300) { mode == "Modern" && showProgressBar }
    private val progressBarHeight by int("ProgressBarHeight", 8, 4..20) { mode == "Modern" && showProgressBar }
    private val progressBarMargin by int("ProgressBarMargin", 10, 0..50) { mode == "Modern" && showProgressBar }
    private val progressBarTimer = MSTimer()
    private var progressBarActive = false
    private var progressBarStartTime = 0L
    private var progressBarCurrentTime = 0L
    var target: EntityLivingBase? = null

    private var globalTimer = MSTimer()

    var shouldRender = true

    private var ignoreWholeTick = false

    private var delayForNextBacktrack = 0L

    private var modernDelay = randomDelay(minDelay.get(), maxDelay.get()) to false

    private val supposedDelay
        get() = if (mode == "Modern") modernDelay.first else maxDelay.get()

    private val trackingBufferTimer = MSTimer()
    private val autoRestartTimer = MSTimer()

    private val maximumCachedPositions by int("MaxCachedPositions", 10, 1..20) { mode == "Legacy" }

    private val backtrackedPlayer = ConcurrentHashMap<UUID, MutableList<BacktrackData>>()

    private val nonDelayedSoundSubstrings = arrayOf("game.player.hurt", "game.player.die")

    private var targetSaver: EntityLivingBase? = null

    private var smartMinRange = 0f
    private var smartMaxRange = 0f
    private var smartDistanceInitialized = false
    private var isFirstAttack = true
    private var rangeBase = 0f

    private var started = false

    val isPacketQueueEmpty
        get() = packetQueue.isEmpty()

    val areQueuedPacketsEmpty
        get() = PacketUtils.isQueueEmpty()

    private var attackCounter = 0

    private var actualAutoRestartDelay = 0
    private val currentDistanceRange: ClosedFloatingPointRange<Float>
        get() = when {
            mode == "Modern" && distanceMode == "Smart" && smartDistanceInitialized -> smartMinRange..smartMaxRange
            mode == "Modern" -> distance
            else -> 0f..0f
        }

    // ========== 新增：假延迟配置（仅用于 Tag 显示） ==========
    private val fakeDelay by boolean("FakeDelay", false)
    // =======================================================

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (TickBase.duringTickModification && mode == "Modern") {
            clearPackets(stopRendering = false)
            return@handler
        }

        if (mode == "Modern") {
            if (packet is S13PacketDestroyEntities && target != null) {
                for (entityId in packet.entityIDs) {
                    if (entityId == target!!.entityId) {
                        clearPacketsImmediately()
                        reset()
                        break
                    }
                }
                return@handler
            }
        }

        if (Blink.blinkingReceive() || event.isCancelled) return@handler
        if (onlyOnKillAura && !KillAura.state && KillAura.target == null) return@handler

        if (mode == "Modern" && mc.thePlayer != null && target != null) {
            if (mc.thePlayer.hurtTime !in ownhurtTime || target!!.hurtTime !in enemyhurtTime) {
                return@handler
            }
        }

        when (mode.lowercase()) {
            "legacy" -> {
                when (packet) {
                    is S0CPacketSpawnPlayer -> {
                        addBacktrackData(
                            packet.player, packet.realX, packet.realY, packet.realZ, System.currentTimeMillis()
                        )
                    }
                    is S14PacketEntity, is S18PacketEntityTeleport -> if (legacyPos == "ServerPos") {
                        val id = if (packet is S14PacketEntity) packet.entityId else (packet as S18PacketEntityTeleport).entityId
                        val entity = mc.theWorld?.getEntityByID(id)
                        val entityMixin = entity as? IMixinEntity
                        if (entityMixin != null) {
                            addBacktrackData(
                                entity.uniqueID,
                                entityMixin.trueX,
                                entityMixin.trueY,
                                entityMixin.trueZ,
                                System.currentTimeMillis()
                            )
                        }
                    }
                }
            }
            "modern" -> {
                if (startMode == "onAttack" && !started) {
                    return@handler
                }

                if (packetQueue.isEmpty() && event.eventType == EventState.RECEIVE) {
                    autoRestartTimer.reset()
                }
                if (mc.isSingleplayer || mc.currentServerData == null) {
                    clearPackets()
                    return@handler
                }

                if (isPacketQueueEmpty && areQueuedPacketsEmpty && !shouldBacktrack()) return@handler

                when (packet) {
                    is C00Handshake, is C00PacketServerQuery, is S02PacketChat, is S01PacketPong -> return@handler
                    is S29PacketSoundEffect -> if (nonDelayedSoundSubstrings in packet.soundName) return@handler
                    is S06PacketUpdateHealth -> if (packet.health <= 0) {
                        clearPackets()
                        return@handler
                    }
                    is S13PacketDestroyEntities -> return@handler
                    is S00PacketKeepAlive -> if (extraMode == "Intave") {
                        return@handler
                    }
                    is S12PacketEntityVelocity, is S27PacketExplosion -> if (extraMode == "Polar") {
                        return@handler
                    }
                }

                if (extraMode == "Polar" && event.eventType == EventState.RECEIVE) {
                    when (packet) {
                        is S14PacketEntity, is S18PacketEntityTeleport -> {
                            val entityId = when (packet) {
                                is S14PacketEntity -> packet.entityId
                                is S18PacketEntityTeleport -> packet.entityId
                                else -> -1
                            }
                            if (target != null && entityId == target!!.entityId) {
                                event.cancelEvent()
                                packetQueue += QueueData(packet, System.currentTimeMillis())
                                return@handler
                            }
                        }
                        is S1CPacketEntityMetadata -> {
                            if (target != null && packet.entityId == target!!.entityId) {
                                event.cancelEvent()
                                packetQueue += QueueData(packet, System.currentTimeMillis())
                                return@handler
                            }
                        }
                        is S19PacketEntityStatus, is S0CPacketSpawnPlayer -> {
                            return@handler
                        }
                    }
                }

                if (event.eventType == EventState.RECEIVE) {
                    when (packet) {
                        is S14PacketEntity -> if (packet.entityId == target?.entityId) {
                            (target as? IMixinEntity)?.run {
                                positions += Pair(Vec3(trueX, trueY, trueZ), System.currentTimeMillis())
                            }
                        }
                        is S18PacketEntityTeleport -> if (packet.entityId == target?.entityId) {
                            (target as? IMixinEntity)?.run {
                                positions += Pair(Vec3(trueX, trueY, trueZ), System.currentTimeMillis())
                            }
                        }
                        is S1CPacketEntityMetadata -> if (target?.entityId == packet.entityId) {
                            packet.func_149376_c()?.forEach {
                                if (it.dataValueId == 6) {
                                    val objectValue = it.getObject().toString().toDoubleOrNull()
                                    if (objectValue != null && !objectValue.isNaN() && objectValue <= 0.0) {
                                        clearPackets()
                                        reset()
                                        return@handler
                                    }
                                }
                            }
                        }
                    }
                    event.cancelEvent()
                    packetQueue += QueueData(packet, System.currentTimeMillis())
                }
            }
        }
    }

    val onGameLoop = handler<GameLoopEvent> {
        if (mode == "Legacy") {
            backtrackedPlayer.forEach { (key, backtrackData) ->
                backtrackData.removeAll { it.time + supposedDelay < System.currentTimeMillis() }
                if (backtrackData.isEmpty()) removeBacktrackData(key)
            }
        }

        if (mode == "Modern") {
            if (target != null && !mc.theWorld.loadedEntityList.contains(target)) {
                clearPacketsImmediately()
                reset()
                return@handler
            }

            if (delayUpdateMode == "EveryTick") {
                modernDelay = randomDelay(minDelay.get(), maxDelay.get()) to true
            }
            if (autoRestart && autoRestartTimer.hasTimePassed(actualAutoRestartDelay)) {
                clearPackets()
                isFirstAttack = true
            }
            val target = target
            val targetMixin = target as? IMixinEntity
            if (onlyOnKillAura && !KillAura.state && KillAura.target == null) return@handler
            if (shouldBacktrack() && targetMixin != null && targetMixin.truePos) {
                if (!progressBarActive) {
                    progressBarActive = true
                    progressBarStartTime = System.currentTimeMillis()
                    progressBarTimer.reset()
                }
                progressBarCurrentTime = System.currentTimeMillis()
            } else {
                if (progressBarActive) {
                    progressBarActive = false
                    progressBarStartTime = 0L
                    progressBarCurrentTime = 0L
                }
            }
            if (shouldBacktrack() && targetMixin != null) {
                if (!Blink.blinkingReceive() && targetMixin.truePos) {
                    val trueDist = mc.thePlayer.getDistance(targetMixin.trueX, targetMixin.trueY, targetMixin.trueZ)
                    val dist = mc.thePlayer.getDistance(target.posX, target.posY, target.posZ)
                    val effectiveDelay = getEffectiveDelay()

                    if (trueDist <= 6f && (!smart || trueDist >= dist) && (style == "Smooth" || !globalTimer.hasTimePassed(effectiveDelay))) {
                        shouldRender = true
                        if (mc.thePlayer.getDistanceToEntityBox(target) in currentDistanceRange) {
                            handlePackets()
                        } else {
                            handlePacketsRange()
                        }
                    } else {
                        clear()
                    }
                }
            } else {
                clear()
            }
        }

        ignoreWholeTick = false
    }

    val onQueuePacketClear = handler<GameLoopEvent>(priority = -6) {
        if (delayUpdateMode != "Normal") return@handler
        val shouldChangeDelay = isPacketQueueEmpty && areQueuedPacketsEmpty
        if (!shouldChangeDelay) {
            modernDelay = modernDelay.first to false
        }
        if (shouldChangeDelay && !modernDelay.second && !shouldBacktrack()) {
            delayForNextBacktrack = System.currentTimeMillis() + nextBacktrackDelay
            modernDelay = randomDelay(minDelay.get(), maxDelay.get()) to true
        }
    }

    val onAttack = handler<AttackEvent> { event ->
        if (!isSelected(event.targetEntity, true)) return@handler

        when (delayUpdateMode) {
            "Attack" -> {
                if (attackCounter <= attackCountToUpdate) {
                    attackCounter++
                } else {
                    modernDelay = randomDelay(minDelay.get(), maxDelay.get()) to true
                    attackCounter = 0
                }
            }
            "KillPlayer" -> {
                if (event.targetEntity is EntityPlayer) targetSaver = event.targetEntity
            }
        }

        if (startMode == "onAttack") {
            started = true
        }

        if (onlyOnKillAura && !KillAura.state && KillAura.target == null) return@handler
        if (mode == "Modern" && distanceMode == "Smart" && event.targetEntity is EntityLivingBase) {
            val initialDistance = mc.thePlayer.getDistanceToEntityBox(event.targetEntity)

            if (isFirstAttack) {
                rangeBase = initialDistance.toFloat()
                smartMinRange = max((rangeBase - minExpandRange), 0f)
                smartMaxRange = rangeBase + expandRange
                smartDistanceInitialized = true
                isFirstAttack = false
            } else {
                smartMinRange = max((rangeBase - minExpandRange), 0f)
                smartMaxRange = rangeBase + expandRange
                smartDistanceInitialized = true
            }
        }
        if (target != event.targetEntity) {
            clearPackets()
            reset()
        }

        if (event.targetEntity is EntityLivingBase) {
            target = event.targetEntity
        }
    }
    val onUpdate = handler<UpdateEvent> {
        actualAutoRestartDelay = when (autoRestartDelayMode) {
            "FollowTrueBacktrackDelay" -> (getEffectiveDelay() * autoRestartDelayFactor).roundToInt()
            "Custom" -> autoRestartDelay
            else -> 0
        }
        val currentTarget = targetSaver ?: return@handler

        if ((currentTarget.health <= 0.0f || currentTarget.isDead) && !mc.theWorld.loadedEntityList.contains(currentTarget)) {
            modernDelay = randomDelay(minDelay.get(), maxDelay.get()) to true
            targetSaver = null
        }
    }
    val onRender3D = handler<Render3DEvent> { event ->
        val manager = mc.renderManager ?: return@handler
        if (onlyOnKillAura && !KillAura.state && KillAura.target == null) return@handler

        when (mode.lowercase()) {
            "legacy" -> {
                val color = Color.RED

                for (entity in mc.theWorld.loadedEntityList) {
                    if (entity is EntityPlayer) {
                        glPushMatrix()
                        glDisable(GL_TEXTURE_2D)
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                        glEnable(GL_LINE_SMOOTH)
                        glEnable(GL_BLEND)
                        glDisable(GL_DEPTH_TEST)

                        mc.entityRenderer.disableLightmap()

                        glBegin(GL_LINE_STRIP)
                        glColor(color)

                        loopThroughBacktrackData(entity) {
                            (entity.currPos - manager.renderPos).let { glVertex3d(it.xCoord, it.yCoord, it.zCoord) }
                            false
                        }

                        glColor4d(1.0, 1.0, 1.0, 1.0)
                        glEnd()
                        glEnable(GL_DEPTH_TEST)
                        glDisable(GL_LINE_SMOOTH)
                        glDisable(GL_BLEND)
                        glEnable(GL_TEXTURE_2D)
                        glPopMatrix()
                    }
                }
            }

            "modern" -> {
                if (!shouldBacktrack() || !shouldRender) return@handler

                target?.run {
                    val targetEntity = target as? IMixinEntity ?: return@run

                    val (x, y, z) = targetEntity.interpolatedPosition - manager.renderPos

                    if (targetEntity.truePos) {
                        when (espMode.lowercase()) {
                            "box" -> {
                                val axisAlignedBB = entityBoundingBox.offset(-currPos + Vec3(x, y, z))
                                drawBacktrackBox(axisAlignedBB, color)
                            }

                            "model" -> {
                                glPushMatrix()
                                glPushAttrib(GL_ALL_ATTRIB_BITS)

                                color(0.6f, 0.6f, 0.6f, 1f)
                                manager.doRenderEntity(
                                    this,
                                    x,
                                    y,
                                    z,
                                    prevRotationYaw + (rotationYaw - prevRotationYaw) * event.partialTicks,
                                    event.partialTicks,
                                    true
                                )

                                glPopAttrib()
                                glPopMatrix()
                            }

                            "wireframe" -> {
                                glPushMatrix()
                                glPushAttrib(GL_ALL_ATTRIB_BITS)

                                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                                glDisable(GL_TEXTURE_2D)
                                glDisable(GL_LIGHTING)
                                glDisable(GL_DEPTH_TEST)
                                glEnable(GL_LINE_SMOOTH)

                                glEnable(GL_BLEND)
                                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

                                glLineWidth(wireframeWidth)

                                glColor(color)
                                manager.doRenderEntity(
                                    this,
                                    x,
                                    y,
                                    z,
                                    prevRotationYaw + (rotationYaw - prevRotationYaw) * event.partialTicks,
                                    event.partialTicks,
                                    true
                                )

                                glPopAttrib()
                                glPopMatrix()
                            }

                            "outline" -> {
                                drawOutline(this, x, y, z, event.partialTicks)
                            }

                            "tracer" -> {
                                val distanceToTarget = mc.thePlayer.getDistanceToEntityBox(this)
                                if (distanceToTarget in currentDistanceRange) {
                                    drawTracer(this, x, y, z)
                                }
                            }

                            "2dbox" -> {
                                draw2DBox(this, x, y, z, event.partialTicks)
                            }

                            "healthbar" -> {
                                drawHealthBar(this, x, y, z)
                            }

                            "arrow" -> {
                                drawArrowIndicator(this, x, y, z)
                            }
                        }
                    }
                }
            }
        }
    }

    val onEntityMove = handler<EntityMovementEvent> { event ->
        if (mode == "Legacy" && legacyPos == "ClientPos") {
            val entity = event.movedEntity

            if (entity is EntityPlayer) {
                addBacktrackData(entity.uniqueID, entity.posX, entity.posY, entity.posZ, System.currentTimeMillis())
            }
        }
    }
    val onRender2D = handler<Render2DEvent> {
        if (mode != "Modern" || !showProgressBar || !progressBarActive) return@handler

        val progress = ((progressBarCurrentTime - progressBarStartTime).toFloat() / supposedDelay).coerceIn(0f, 1f)
        val scaledScreen = ScaledResolution(mc)
        val screenWidth = scaledScreen.scaledWidth
        val screenHeight = scaledScreen.scaledHeight

        val (x, y) = when (progressBarPosition) {
            "TopLeft" -> progressBarMargin to progressBarMargin
            "TopRight" -> screenWidth - progressBarWidth - progressBarMargin to progressBarMargin
            "BottomLeft" -> progressBarMargin to screenHeight - progressBarHeight - progressBarMargin
            "BottomRight" -> screenWidth - progressBarWidth - progressBarMargin to screenHeight - progressBarHeight - progressBarMargin
            "BottomCenter" -> (screenWidth - progressBarWidth) / 2 to screenHeight - progressBarHeight - progressBarMargin
            else -> progressBarMargin to progressBarMargin
        }

        drawProgressBar(x, y, progressBarWidth, progressBarHeight, progress)
    }
    val onWorld = handler<WorldEvent> { event ->
        if (mode == "Modern") {
            if (event.worldClient == null) clearPackets(false)
            target = null
            targetSaver = null
            modernDelay = randomDelay(minDelay.get(), maxDelay.get()) to true
            attackCounter = 0
            smartDistanceInitialized = false
            isFirstAttack = true
            started = false
        }
    }

    override fun onEnable() = reset()

    override fun onDisable() {
        clearPackets()
        backtrackedPlayer.clear()
        smartDistanceInitialized = false
        isFirstAttack = true
        started = false
    }

    private fun handlePackets() {
        val effectiveDelay = getEffectiveDelay()

        when (style) {
            "Pulse", "Smooth" -> {
                packetQueue.removeAll { (packet, timestamp) ->
                    if (timestamp <= System.currentTimeMillis() - effectiveDelay) {
                        PacketUtils.schedulePacketProcess(packet)
                        true
                    } else false
                }
            }
            "Instant" -> {
                packetQueue.removeAll { (packet, timestamp) ->
                    val shouldProcess = timestamp <= System.currentTimeMillis() - effectiveDelay ||
                            (System.currentTimeMillis() - timestamp) < instantThreshold
                    if (shouldProcess) {
                        PacketUtils.schedulePacketProcess(packet)
                        true
                    } else false
                }
            }
            "Wave" -> {
                val currentTime = System.currentTimeMillis()
                val baseFrequency = waveFrequency

                val adjustedFrequency = when {
                    target?.hurtTime in enemyhurtTime -> baseFrequency * 1.5f
                    mc.thePlayer.hurtTime in ownhurtTime -> baseFrequency * 0.7f
                    else -> baseFrequency
                }

                val waveOffset = (sin(currentTime * adjustedFrequency * 0.001) * 0.5 + 0.5).toLong() * effectiveDelay / 2

                packetQueue.removeAll { (packet, timestamp) ->
                    if (timestamp <= currentTime - effectiveDelay + waveOffset) {
                        PacketUtils.schedulePacketProcess(packet)
                        true
                    } else false
                }
            }
            "Step" -> {
                val inRange = target?.let { mc.thePlayer.getDistanceToEntityBox(it) in currentDistanceRange } ?: false
                val effectiveStepCount = if (inRange) stepCount else maxOf(2, stepCount / 2)

                val stepSize = effectiveDelay / effectiveStepCount
                val currentStep = (System.currentTimeMillis() / stepSize) % effectiveStepCount

                packetQueue.removeAll { (packet, timestamp) ->
                    val packetStep = (timestamp / stepSize) % effectiveStepCount
                    if (packetStep <= currentStep) {
                        PacketUtils.schedulePacketProcess(packet)
                        true
                    } else false
                }
            }
            "Bounce" -> {
                val currentTime = System.currentTimeMillis()
                var effectiveBounce = bounceIntensity

                if (autoRestart && autoRestartTimer.hasTimePassed(actualAutoRestartDelay * 0.8)) {
                    effectiveBounce *= 0.3f
                }

                val bounceFactor = (abs(sin(currentTime * 0.003)) * effectiveBounce + 1.0).toFloat()
                val adjustedDelay = (effectiveDelay * bounceFactor).toLong()

                packetQueue.removeAll { (packet, timestamp) ->
                    if (timestamp <= currentTime - adjustedDelay) {
                        PacketUtils.schedulePacketProcess(packet)
                        true
                    } else false
                }
            }
        }

        positions.removeAll { (_, timestamp) -> timestamp < System.currentTimeMillis() - effectiveDelay }
    }

    private fun getEffectiveDelay(): Long {
        var baseDelay = supposedDelay.toLong()

        val currentTarget = target
        val currentTargetMixin = currentTarget as? IMixinEntity

        if ((currentTarget?.hurtTime ?: 0) > 5) {
            baseDelay = (baseDelay * 0.8).toLong()
        }

        if (smart && currentTargetMixin != null && currentTargetMixin.truePos) {
            val trueDist = mc.thePlayer.getDistance(
                currentTargetMixin.trueX,
                currentTargetMixin.trueY,
                currentTargetMixin.trueZ
            )
            val dist = mc.thePlayer.getDistance(
                currentTarget.posX,
                currentTarget.posY,
                currentTarget.posZ
            )
            if (trueDist < dist) {
                baseDelay = (baseDelay * 1.2).toLong()
            }
        }

        return baseDelay
    }
    private fun handlePacketsRange() {
        val time = getRangeTime()
        if (time == -1L) {
            clearPackets()
            return
        }

        packetQueue.removeAll { (packet, timestamp) ->
            if (timestamp <= time) {
                PacketUtils.schedulePacketProcess(packet)
                true
            } else false
        }

        positions.removeAll { (_, timestamp) -> timestamp < time }
    }

    private fun getRangeTime(): Long {
        val target = this.target ?: return 0L

        var time = 0L
        var found = false

        for (data in positions) {
            time = data.second

            val targetPos = target.currPos

            val targetBox = target.hitBox.offset(data.first - targetPos)

            if (mc.thePlayer.getDistanceToBox(targetBox) in currentDistanceRange) {
                found = true
                break
            }
        }

        return if (found) time else -1L
    }

    private fun clearPacketsImmediately() {
        packetQueue.removeAll {
            PacketUtils.handlePackets(it.packet)
            true
        }
        positions.clear()
        autoRestartTimer.reset()
        shouldRender = false
        ignoreWholeTick = true
        progressBarActive = false
        progressBarStartTime = 0L
        progressBarCurrentTime = 0L
    }

    private fun clearPackets(handlePackets: Boolean = true, stopRendering: Boolean = true) {
        packetQueue.removeAll {
            if (handlePackets) {
                PacketUtils.schedulePacketProcess(it.packet)
            }

            true
        }

        positions.clear()
        autoRestartTimer.reset()

        if (stopRendering) {
            shouldRender = false
            ignoreWholeTick = true
        }
    }

    private fun addBacktrackData(id: UUID, x: Double, y: Double, z: Double, time: Long) {
        val backtrackData = getBacktrackData(id)

        if (backtrackData != null) {
            if (backtrackData.size >= maximumCachedPositions) {
                backtrackData.removeFirst()
            }

            backtrackData += BacktrackData(x, y, z, time)
        } else {
            backtrackedPlayer[id] = mutableListOf(BacktrackData(x, y, z, time))
        }
    }

    private fun getBacktrackData(id: UUID) = backtrackedPlayer[id]

    private fun removeBacktrackData(id: UUID) = backtrackedPlayer.remove(id)

    fun getNearestTrackedDistance(entity: Entity): Double {
        var nearestRange = 0.0

        loopThroughBacktrackData(entity) {
            val range = entity.getDistanceToEntityBox(mc.thePlayer)

            if (range < nearestRange || nearestRange == 0.0) {
                nearestRange = range
            }

            false
        }

        return nearestRange
    }

    fun loopThroughBacktrackData(entity: Entity, action: () -> Boolean) {
        if (!state || entity !is EntityPlayer || mode == "Modern") return

        val backtrackDataArray = getBacktrackData(entity.uniqueID) ?: return

        val currPos = entity.currPos
        val prevPos = entity.prevPos

        for ((x, y, z, _) in backtrackDataArray.reversed()) {
            entity.setPosAndPrevPos(Vec3(x, y, z))

            if (action()) break
        }

        entity.setPosAndPrevPos(currPos, prevPos)
    }

    fun <T> runWithNearestTrackedDistance(entity: Entity, f: () -> T): T {
        if (entity !is EntityPlayer || !handleEvents() || mode == "Modern") {
            return f()
        }

        var backtrackDataArray = getBacktrackData(entity.uniqueID)?.toMutableList() ?: return f()

        backtrackDataArray = backtrackDataArray.sortedBy { (x, y, z, _) ->
            runWithSimulatedPosition(entity, Vec3(x, y, z)) {
                mc.thePlayer.getDistanceToBox(entity.hitBox)
            }
        }.toMutableList()

        val (x, y, z, _) = backtrackDataArray.first()

        return runWithSimulatedPosition(entity, Vec3(x, y, z)) { f() } ?: f()
    }

    fun <T> runWithSimulatedPosition(entity: Entity, vec3: Vec3, f: () -> T?): T? {
        val currPos = entity.currPos
        val prevPos = entity.prevPos

        entity.setPosAndPrevPos(vec3)

        val result = f()

        entity.setPosAndPrevPos(currPos, prevPos)

        return result
    }

    fun <T> runWithModifiedRotation(
        entity: EntityPlayer, rotation: Rotation, body: Pair<Float, Float>? = null,
        f: (Rotation) -> T?
    ): T? {
        val currRotation = entity.rotation
        val prevRotation = entity.prevRotation
        val bodyYaw = entity.prevRenderYawOffset to entity.renderYawOffset
        val headRotation = entity.prevRotationYawHead to entity.rotationYawHead

        entity.prevRotation = rotation
        entity.rotation = rotation
        entity.prevRotationYawHead = rotation.yaw
        entity.rotationYawHead = rotation.yaw

        body?.let {
            entity.prevRenderYawOffset = it.first
            entity.renderYawOffset = it.second
        }

        val result = f(rotation)

        entity.rotation = currRotation
        entity.prevRotation = prevRotation
        entity.rotationYawHead = headRotation.second
        entity.prevRotationYawHead = headRotation.first

        body?.let {
            entity.prevRenderYawOffset = bodyYaw.first
            entity.renderYawOffset = bodyYaw.second
        }

        return result
    }

    private fun drawOutline(entity: EntityLivingBase, x: Double, y: Double, z: Double, partialTicks: Float) {
        glPushMatrix()
        glPushAttrib(GL_ALL_ATTRIB_BITS)

        glDisable(GL_TEXTURE_2D)
        glDisable(GL_LIGHTING)
        glEnable(GL_LINE_SMOOTH)
        glEnable(GL_BLEND)
        glDisable(GL_DEPTH_TEST)

        val highlightColor = if (entity.hurtTime in enemyhurtTime) Color.RED else color
        glLineWidth(if (entity.hurtTime in enemyhurtTime) outlineWidth * 1.5f else outlineWidth)

        glColor(highlightColor)

        val manager = mc.renderManager
        manager.doRenderEntity(
            entity,
            x, y, z,
            entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks,
            partialTicks,
            false
        )

        glPopAttrib()
        glPopMatrix()
    }

    private fun drawTracer(entity: EntityLivingBase, x: Double, y: Double, z: Double) {
        val playerPos = Vec3(0.0, mc.thePlayer.getEyeHeight().toDouble(), 0.0)
        val targetPos = Vec3(x, y + entity.height / 2, z)

        glPushMatrix()
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_LINE_SMOOTH)
        glEnable(GL_BLEND)
        glDisable(GL_DEPTH_TEST)

        glLineWidth(tracerWidth)
        glColor(color)

        glBegin(GL_LINES)
        glVertex3d(playerPos.xCoord, playerPos.yCoord, playerPos.zCoord)
        glVertex3d(targetPos.xCoord, targetPos.yCoord, targetPos.zCoord)
        glEnd()

        glPopMatrix()
    }

    private fun draw2DBox(entity: EntityLivingBase, x: Double, y: Double, z: Double, partialTicks: Float) {
        glPushMatrix()
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glDisable(GL_DEPTH_TEST)

        glColor(color)

        glBegin(GL_LINE_LOOP)
        glVertex3d(x - 0.3, y, z - 0.3)
        glVertex3d(x + 0.3, y, z - 0.3)
        glVertex3d(x + 0.3, y + entity.height, z - 0.3)
        glVertex3d(x - 0.3, y + entity.height, z - 0.3)
        glEnd()

        glPopMatrix()
    }

    private fun drawHealthBar(entity: EntityLivingBase, x: Double, y: Double, z: Double) {
        val health = entity.health.coerceAtLeast(0f)
        val maxHealth = entity.maxHealth
        val healthPercent = (health / maxHealth).coerceIn(0f, 1f)

        val barWidth = 0.8f
        val barHeight = 0.15f
        val cornerRadius = 0.05f
        val verticalOffset = 0.3f

        glPushMatrix()
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glDisable(GL_DEPTH_TEST)

        glTranslated(x, y + entity.height + verticalOffset, z)

        glColor4f(0.3f, 0.3f, 0.3f, 0.7f) // 灰色半透明背景
        drawRoundedRect(-barWidth/2, -barHeight/2, barWidth/2, barHeight/2, cornerRadius)

        val barLeft = (-barWidth/2)
        val barRight = (barLeft + barWidth * healthPercent)

        val barColor = when {
            healthPercent > 0.5f -> Color(0f, 1f, 0f, 0.7f) // 绿色
            healthPercent > 0.2f -> Color(1f, 1f, 0f, 0.7f) // 黄色
            else -> Color(1f, 0f, 0f, 0.7f)
        }

        glColor4f(barColor.red.toFloat(), barColor.green.toFloat(), barColor.blue.toFloat(), barColor.alpha.toFloat())
        glBegin(GL_QUADS)
        glVertex3d(barLeft.toDouble(), (-barHeight/2).toDouble(), 0.0)
        glVertex3d(barRight.toDouble(), (-barHeight/2).toDouble(), 0.0)
        glVertex3d(barRight.toDouble(), barHeight/2.0, 0.0)
        glVertex3d(barLeft.toDouble(), barHeight/2.0, 0.0)
        glEnd()

        if (healthPercent > 0) {
            glEnable(GL_TEXTURE_2D)
            glEnable(GL_DEPTH_TEST)
            glColor4f(1f, 1f, 1f, 1f)

            val healthText = "%.1f".format(health)
            val fontRenderer = mc.fontRendererObj

            val textWidth = fontRenderer.getStringWidth(healthText)
            val textHeight = 8
            val textX = -textWidth / 2.0
            val textY = -textHeight / 2.0

            glDepthFunc(GL_ALWAYS)

            glPushMatrix()
            glTranslated(textX + 0.5, textY + 0.5, 0.01)
            glScaled(0.02, 0.02, 0.02)
            fontRenderer.drawString(healthText, 0, 0, 0x000000)
            glPopMatrix()

            // 绘制主文本
            glPushMatrix()
            glTranslated(textX, textY, 0.01)
            glScaled(0.02, 0.02, 0.02)
            fontRenderer.drawString(healthText, 0, 0, 0xFFFFFF)
            glPopMatrix()

            glDepthFunc(GL_LEQUAL)
            glDisable(GL_TEXTURE_2D)
            glDisable(GL_DEPTH_TEST)
        }

        glPopMatrix()
    }

    private fun drawRoundedRect(left: Float, bottom: Float, right: Float, top: Float, radius: Float) {
        val segments = 8

        glBegin(GL_QUADS)

        glVertex3d((left + radius).toDouble(), top.toDouble(), 0.0)
        glVertex3d((right - radius).toDouble(), top.toDouble(), 0.0)
        glVertex3d((right - radius).toDouble(), bottom.toDouble(), 0.0)
        glVertex3d((left + radius).toDouble(), bottom.toDouble(), 0.0)

        glVertex3d(left.toDouble(), (bottom + radius).toDouble(), 0.0)
        glVertex3d((left + radius).toDouble(), (bottom + radius).toDouble(), 0.0)
        glVertex3d((left + radius).toDouble(), (top - radius).toDouble(), 0.0)
        glVertex3d(left.toDouble(), (top - radius).toDouble(), 0.0)

        glVertex3d((right - radius).toDouble(), (bottom + radius).toDouble(), 0.0)
        glVertex3d(right.toDouble(), (bottom + radius).toDouble(), 0.0)
        glVertex3d(right.toDouble(), (top - radius).toDouble(), 0.0)
        glVertex3d((right - radius).toDouble(), (top - radius).toDouble(), 0.0)

        glEnd()

        drawRoundedCorner(left + radius, top - radius, radius, 270f, segments) // 左上
        drawRoundedCorner(right - radius, top - radius, radius, 0f, segments) // 右上
        drawRoundedCorner(left + radius, bottom + radius, radius, 180f, segments) // 左下
        drawRoundedCorner(right - radius, bottom + radius, radius, 90f, segments) // 右下
    }

    private fun drawRoundedCorner(centerX: Float, centerY: Float, radius: Float, startAngle: Float, segments: Int) {
        glBegin(GL_TRIANGLE_FAN)
        glVertex3d(centerX.toDouble(), centerY.toDouble(), 0.0)

        val angleStep = 90f / segments
        for (i in 0..segments) {
            val angle = Math.toRadians(startAngle + i * angleStep.toDouble())
            val x = centerX + radius * cos(angle.toFloat())
            val y = centerY + radius * sin(angle).toFloat()
            glVertex3d(x.toDouble(), y.toDouble(), 0.0)
        }

        glEnd()
    }
    private fun drawArrowIndicator(entity: EntityLivingBase, x: Double, y: Double, z: Double) {
        glPushMatrix()
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_LINE_SMOOTH)

        glColor(color)
        glLineWidth(2f)

        glBegin(GL_LINES)
        glVertex3d(x, y + entity.height / 2, z)
        glVertex3d(x, y + entity.height / 2 + arrowSize / 10, z)
        glEnd()

        glPopMatrix()
    }

    val color
        get() = espColor.color()

    private fun shouldBacktrack(): Boolean {
        if (mc.thePlayer == null || mc.theWorld == null || target == null ||
            mc.thePlayer.health <= 0 || (target!!.health <= 0 && !target!!.health.isNaN()) ||
            mc.playerController.currentGameType == WorldSettings.GameType.SPECTATOR ||
            System.currentTimeMillis() < delayForNextBacktrack ||
            !isSelected(target!!, true) ||
            (mc.thePlayer?.ticksExisted ?: 0) <= 20 ||
            ignoreWholeTick) {
            if (!isFirstAttack) {
                isFirstAttack = true
            }
            return false
        }

        if (startMode == "onAttack" && !started) {
            return false
        }

        val inRange = mc.thePlayer.getDistanceToEntityBox(target!!) in currentDistanceRange
        if (inRange) {
            trackingBufferTimer.reset()
        }
        return inRange || !trackingBufferTimer.hasTimePassed(trackingBuffer.toLong())
    }


    private fun reset() {
        target = null
        globalTimer.reset()
        trackingBufferTimer.reset()
        attackCounter = 0
        smartDistanceInitialized = false
        isFirstAttack = true
        started = false
    }

    private fun clear() {
        clearPackets()
        globalTimer.reset()
        trackingBufferTimer.reset()
        isFirstAttack = true
        started = false
        progressBarActive = false
        progressBarStartTime = 0L
        progressBarCurrentTime = 0L
    }

    // ========== 修改 tag 显示，支持 FakeDelay ==========
    override val tag: String
        get() {
            // 如果 FakeDelay 开启，直接返回固定值（覆盖所有 tagMode）
            if (fakeDelay) {
                return "1.797693134862315907729305190789e+308ms"
            }

            return when (tagMode) {
                "Normal" -> {
                    if (extraMode == "NoExtra") {
                        "${supposedDelay}ms"
                    } else {
                        "${supposedDelay}ms | $extraMode"
                    }
                }
                "WorkRange" -> {
                    when {
                        mode == "Modern" && distanceMode == "Smart" && smartDistanceInitialized ->
                            "Smart: ${"%.1f".format(smartMinRange)}-${"%.1f".format(smartMaxRange)}"
                        mode == "Modern" -> "${distance.start} - ${distance.endInclusive}"
                        else -> ""
                    }
                }
                "PacketQueueSize" -> "${packetQueue.size}"
                "Custom" -> customTagText
                else -> ""
            }
        }
    // =================================================

    private fun drawProgressBar(x: Int, y: Int, width: Int, height: Int, progress: Float) {
        // 绘制背景
        drawRect(x, y, x + width, y + height, Color(0, 0, 0, 150).rgb)

        // 绘制进度条
        val progressWidth = (width * progress).toInt()

        if (progressWidth > 0) {
            // 创建蓝色渐变
            for (i in 0 until progressWidth) {
                val colorProgress = i.toFloat() / width
                val r = (0 * (1 - colorProgress) + 0 * colorProgress).toInt()
                val g = (0 * (1 - colorProgress) + 150 * colorProgress).toInt()
                val b = (255 * (1 - colorProgress) + 255 * colorProgress).toInt()
                val alpha = 200

                drawRect(x + i, y, x + i + 1, y + height, Color(r, g, b, alpha).rgb)
            }
        }

        // 绘制边框
        drawRect(x, y, x + width, y + 1, Color.WHITE.rgb) // 上边框
        drawRect(x, y + height - 1, x + width, y + height, Color.WHITE.rgb) // 下边框
        drawRect(x, y, x + 1, y + height, Color.WHITE.rgb) // 左边框
        drawRect(x + width - 1, y, x + width, y + height, Color.WHITE.rgb) // 右边框

        // 绘制进度文本
        val timeText = "${(progressBarCurrentTime - progressBarStartTime).coerceAtMost(supposedDelay.toLong())}ms / ${supposedDelay}ms"
        val textWidth = mc.fontRendererObj.getStringWidth(timeText)
        val textX = x + (width - textWidth) / 2
        val textY = y - mc.fontRendererObj.FONT_HEIGHT - 2

        mc.fontRendererObj.drawString(timeText, textX, textY, Color.WHITE.rgb)
    }

    private fun drawRect(x: Int, y: Int, x2: Int, y2: Int, color: Int) {
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glColor(color)

        glBegin(GL_QUADS)
        glVertex2i(x, y)
        glVertex2i(x2, y)
        glVertex2i(x2, y2)
        glVertex2i(x, y2)
        glEnd()

        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
    }

    private fun glColor(color: Int) {
        val a = (color shr 24 and 255).toFloat() / 255.0f
        val r = (color shr 16 and 255).toFloat() / 255.0f
        val g = (color shr 8 and 255).toFloat() / 255.0f
        val b = (color and 255).toFloat() / 255.0f
        glColor4f(r, g, b, a)
    }
}

data class QueueData(val packet: Packet<*>, val time: Long)
data class BacktrackData(val x: Double, val y: Double, val z: Double, val time: Long)
package net.ccbluex.liquidbounce.features.module.modules.rise

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemEgg
import net.minecraft.item.ItemFishingRod
import net.minecraft.item.ItemSnowball
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.util.MathHelper
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

object RiseComboOneHit : Module("ComboOneHit", Category.RISE, forcedDescription = "Rise - Sends multiple attack packets in one hit") {

    private val packets by int("Packets", 50, 1..1000)

    val onAttack = handler<AttackEvent> { event ->
        val entity = event.targetEntity as? EntityLivingBase ?: return@handler
        repeat(packets) {
            sendPacket(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
        }
    }
}

object RiseCriticals : Module("RiseCriticals", Category.RISE, forcedDescription = "Rise - Critical hit modifier") {

    private val mode by choices("Mode", arrayOf("Packet", "Edit", "NoGround", "Vulcan", "Watchdog", "Verus"), "Packet")
    private val delay by int("Delay", 0, 0..1000)
    private val hurtTime by int("HurtTime", 10, 0..10)

    private val msTimer = MSTimer()
    private var vulcanGroundSpoof = false

    val onAttack = handler<AttackEvent> { event ->
        if (event.targetEntity !is EntityLivingBase) return@handler
        val thePlayer = mc.thePlayer ?: return@handler
        val entity = event.targetEntity as EntityLivingBase

        if (thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWeb && !thePlayer.isInLiquid &&
            thePlayer.ridingEntity == null && entity.hurtTime <= hurtTime && msTimer.hasTimePassed(delay)
        ) {
            when (mode) {
                "Packet" -> {
                    val offsets = doubleArrayOf(0.0625, 0.0)
                    for (offset in offsets) {
                        sendPacket(C03PacketPlayer.C04PacketPlayerPosition(
                            thePlayer.posX, thePlayer.posY + offset, thePlayer.posZ, offset != 0.0
                        ))
                    }
                }
                "Edit" -> {
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.11, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.1100013579, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 1.3579E-6, thePlayer.posZ, false))
                }
                "NoGround" -> {
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, false))
                }
                "Vulcan" -> {
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.11, thePlayer.posZ, true))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.001, thePlayer.posZ, true))
                }
                "Watchdog" -> {
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.0625, thePlayer.posZ, true))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.0625, thePlayer.posZ, false))
                }
                "Verus" -> {
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.415, thePlayer.posZ, true))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.0, thePlayer.posZ, false))
                }
            }
            msTimer.reset()
        }
    }

    val onUpdate = handler<UpdateEvent> {
        if (mode == "Vulcan") {
            val thePlayer = mc.thePlayer ?: return@handler
            if (thePlayer.fallDistance > 0.0f && thePlayer.fallDistance < 1.8f) {
                vulcanGroundSpoof = true
            } else {
                vulcanGroundSpoof = false
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (mode == "Vulcan" && vulcanGroundSpoof) {
            val packet = event.packet
            if (packet is C03PacketPlayer) {
                packet.onGround = false
            }
        }
    }
}

object RiseVelocity : Module("RiseVelocity", Category.RISE, forcedDescription = "Rise - Velocity modifier with multiple bypass modes") {

    private val mode by choices(
        "Mode", arrayOf(
            "Standard", "BufferAbuse", "Delay", "Legit", "Ground", "Intave", "Matrix",
            "AAC", "Vulcan", "Tick", "Bounce", "Karhu", "MMC", "Grim", "Grim2",
            "Watchdog", "WatchdogPrediction", "WatchdogReduce"
        ), "Standard"
    )
    private val horizontal by float("Horizontal", 0f, 0f..100f)
    private val vertical by float("Vertical", 0f, 0f..100f)
    private val onSwing by boolean("OnSwing", false)
    private val bufferCount by int("Buffer", 1, 1..3) { mode == "BufferAbuse" }
    private val delayTicks by int("DelayTicks", 5, 0..40) { mode == "Delay" }
    private val explosionIgnore by boolean("ExplosionIgnore", false) { mode == "Standard" }
    private val delayExplosion by boolean("DelayExplosion", true) { mode == "Delay" }

    private var bufferAmount = 0
    private val velocityQueue = ArrayDeque<Pair<Any, Int>>()
    private var delayProcessing = false
    private var grimMovementFrozen = false
    private var grimMotionSaved = false
    private var grimSavedMotionX = 0.0
    private var grimSavedMotionY = 0.0
    private var grimSavedMotionZ = 0.0
    private var grimVelocityTicks = 0
    private var watchdogPendingVelocity = false
    private var watchdogJumpCancelled = false

    override fun onEnable() {
        bufferAmount = 0
        velocityQueue.clear()
        grimMovementFrozen = false
        grimMotionSaved = false
        grimVelocityTicks = 0
    }

    override fun onDisable() {
        bufferAmount = 0
        velocityQueue.clear()
        grimMovementFrozen = false
        grimMotionSaved = false
        mc.timer.timerSpeed = 1f
    }

    val onPacket = handler<PacketEvent> { event ->
        if (mc.thePlayer == null) return@handler
        if (delayProcessing) return@handler

        when (val p = event.packet) {
            is S12PacketEntityVelocity -> {
                if (p.entityID != mc.thePlayer.entityId) return@handler
                grimVelocityTicks = 0

                when (mode) {
                    "Standard" -> {
                        val h = horizontal.toDouble() / 100.0
                        val v = vertical.toDouble() / 100.0
                        if (h == 0.0) {
                            if (v != 0.0) {
                                mc.thePlayer.motionY = p.motionY / 8000.0
                            }
                            event.cancelEvent()
                            return@handler
                        }
                        p.motionX = (p.motionX * h).toInt()
                        p.motionY = (p.motionY * v).toInt()
                        p.motionZ = (p.motionZ * h).toInt()
                    }
                    "BufferAbuse" -> {
                        if (bufferAmount < bufferCount) {
                            event.cancelEvent()
                            bufferAmount++
                            return@handler
                        }
                        val h = horizontal.toDouble() / 100.0
                        val v = vertical.toDouble() / 100.0
                        p.motionX = (p.motionX * h).toInt()
                        p.motionY = (p.motionY * v).toInt()
                        p.motionZ = (p.motionZ * h).toInt()
                        bufferAmount = 0
                    }
                    "Delay" -> {
                        event.cancelEvent()
                        velocityQueue.addLast(p to (mc.thePlayer.ticksExisted + delayTicks))
                        return@handler
                    }
                    "Legit" -> {
                        if (mc.thePlayer.onGround) {
                            event.cancelEvent()
                        }
                    }
                    "Ground" -> {
                        if (mc.thePlayer.onGround) {
                            event.cancelEvent()
                        } else {
                            val h = horizontal.toDouble() / 100.0
                            val v = vertical.toDouble() / 100.0
                            p.motionX = (p.motionX * h).toInt()
                            p.motionY = (p.motionY * v).toInt()
                            p.motionZ = (p.motionZ * h).toInt()
                        }
                    }
                    "Intave" -> {
                        event.cancelEvent()
                    }
                    "Matrix" -> {
                        if (mc.thePlayer.hurtTime > 0) {
                            event.cancelEvent()
                        }
                    }
                    "AAC" -> {
                        val vx = p.motionX / 8000.0
                        val vz = p.motionZ / 8000.0
                        if (abs(vx) > 0.01 || abs(vz) > 0.01) {
                            event.cancelEvent()
                        }
                    }
                    "Vulcan" -> {
                        if (mc.thePlayer.hurtTime <= 3) {
                            event.cancelEvent()
                        }
                    }
                    "Tick" -> {
                        event.cancelEvent()
                        mc.thePlayer.motionX *= horizontal / 100f
                        mc.thePlayer.motionZ *= horizontal / 100f
                    }
                    "Bounce" -> {
                        if (p.motionY > 0) {
                            event.cancelEvent()
                        }
                    }
                    "Karhu" -> {
                        event.cancelEvent()
                        mc.thePlayer.motionX *= 0.0
                        mc.thePlayer.motionZ *= 0.0
                    }
                    "MMC" -> {
                        if (mc.thePlayer.onGround) {
                            event.cancelEvent()
                            mc.thePlayer.motionY = p.motionY / 8000.0
                        }
                    }
                    "Grim" -> {
                        if (mc.thePlayer.hurtTime >= 7 && !mc.thePlayer.isInWeb) {
                            grimMovementFrozen = true
                            if (!grimMotionSaved) {
                                grimSavedMotionX = mc.thePlayer.motionX
                                grimSavedMotionY = mc.thePlayer.motionY
                                grimSavedMotionZ = mc.thePlayer.motionZ
                                grimMotionSaved = true
                            }
                        }
                    }
                    "Grim2" -> {
                        event.cancelEvent()
                    }
                    "Watchdog" -> {
                        event.cancelEvent()
                    }
                    "WatchdogPrediction" -> {
                        if (mc.thePlayer.onGround) {
                            event.cancelEvent()
                        }
                    }
                    "WatchdogReduce" -> {
                        val h = horizontal.toDouble() / 100.0
                        val v = vertical.toDouble() / 100.0
                        p.motionX = (p.motionX * h).toInt()
                        p.motionY = (p.motionY * v).toInt()
                        p.motionZ = (p.motionZ * h).toInt()
                    }
                }
            }
            is S27PacketExplosion -> {
                when (mode) {
                    "Standard" -> {
                        if (explosionIgnore) {
                            event.cancelEvent()
                            return@handler
                        }
                        val h = horizontal.toDouble() / 100.0
                        val v = vertical.toDouble() / 100.0
                        mc.thePlayer.motionX += p.func_149144_d() * h
                        mc.thePlayer.motionY += p.func_149147_e() * v
                        mc.thePlayer.motionZ += p.field_149159_h * h
                        event.cancelEvent()
                    }
                    "BufferAbuse" -> {
                        if (bufferAmount < bufferCount) {
                            event.cancelEvent()
                            bufferAmount++
                            return@handler
                        }
                        val h = horizontal.toDouble() / 100.0
                        val v = vertical.toDouble() / 100.0
                        mc.thePlayer.motionX += p.func_149144_d() * h
                        mc.thePlayer.motionY += p.func_149147_e() * v
                        mc.thePlayer.motionZ += p.field_149159_h * h
                        event.cancelEvent()
                        bufferAmount = 0
                    }
                    "Delay" -> {
                        if (delayExplosion) {
                            event.cancelEvent()
                            velocityQueue.addLast(p to (mc.thePlayer.ticksExisted + delayTicks))
                            return@handler
                        }
                    }
                    else -> {
                        if (mode != "Ground" && mode != "Legit" && mode != "WatchdogPrediction" && mode != "WatchdogReduce") {
                            event.cancelEvent()
                        }
                    }
                }
            }
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        grimVelocityTicks++

        if (mode == "Delay" && velocityQueue.isNotEmpty()) {
            val tick = thePlayer.ticksExisted
            while (velocityQueue.isNotEmpty() && velocityQueue.first().second <= tick) {
                val (packet, _) = velocityQueue.removeFirst()
                delayProcessing = true
                try {
                    if (packet is S12PacketEntityVelocity) {
                        mc.thePlayer.motionX = packet.motionX / 8000.0
                        mc.thePlayer.motionY = packet.motionY / 8000.0
                        mc.thePlayer.motionZ = packet.motionZ / 8000.0
                    }
                } finally {
                    delayProcessing = false
                }
            }
        }

        if (mode == "Grim" && grimMovementFrozen && grimMotionSaved) {
            if (thePlayer.onGround && thePlayer.hurtTime <= 0) {
                grimMovementFrozen = false
                thePlayer.motionX = grimSavedMotionX
                thePlayer.motionY = grimSavedMotionY
                thePlayer.motionZ = grimSavedMotionZ
                grimMotionSaved = false
            }
        }

        if (mode == "Vulcan" && thePlayer.hurtTime > 0) {
            thePlayer.motionX *= 0.0
            thePlayer.motionZ *= 0.0
        }
    }
}

object RiseTickBase : Module("RiseTickBase", Category.RISE, forcedDescription = "Rise - Tick base movement for extended reach") {

    private val mode by choices("Mode", arrayOf("Post", "Legit"), "Legit")
    private val cooldownTicks by int("Cooldown", 10, 1..30)
    private val range by float("Range", 6f, 1f..8f)
    private val swing by boolean("Swing", true)

    private var ticksToSkip = 0
    private var skipDelayTicks = 0
    private var cooldown = 0
    private var target: EntityLivingBase? = null

    override fun onEnable() {
        ticksToSkip = 0
        skipDelayTicks = 0
        cooldown = 0
        target = null
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        target = null
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (cooldown > 0) {
            cooldown--
            return@handler
        }

        if (ticksToSkip > 0) {
            ticksToSkip--
            mc.timer.timerSpeed = 0.5f
            return@handler
        }

        mc.timer.timerSpeed = 1f

        if (skipDelayTicks > 0) {
            skipDelayTicks--
            return@handler
        }

        val currentTarget = target
        if (currentTarget != null && thePlayer.getDistanceToEntity(currentTarget) <= range) {
            if (thePlayer.onGround) {
                ticksToSkip = 2
                skipDelayTicks = cooldownTicks
                cooldown = cooldownTicks
            }
        }
    }
}

object RiseTeleportAura : Module("RiseTeleportAura", Category.RISE, forcedDescription = "Rise - Teleport-based aura attack") {

    private val mode by choices("Mode", arrayOf("Single", "Multiple"), "Single")
    private val range by float("Range", 6f, 1f..8f)
    private val cps by intRange("CPS", 8..14, 1..50)
    private val cooldown19 by boolean("1.9Cooldown", false)
    private val swing by boolean("Swing", true)
    private val throughWalls by boolean("ThroughWalls", true)
    private val players by boolean("Players", true)
    private val mobs by boolean("Mobs", false)

    private val clickStopWatch = MSTimer()
    private var nextSwing = 125L
    private var target: EntityLivingBase? = null
    private var attackedEntities = mutableListOf<EntityLivingBase>()

    override fun onEnable() {
        target = null
        attackedEntities.clear()
    }

    override fun onDisable() {
        target = null
        attackedEntities.clear()
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val theWorld = mc.theWorld ?: return@handler

        if (!clickStopWatch.hasTimePassed(nextSwing)) return@handler

        val targets = theWorld.loadedEntityList
            .filterIsInstance<EntityLivingBase>()
            .filter { it != thePlayer && it.health > 0 }
            .filter { if (players) it is EntityPlayer else true }
            .filter { thePlayer.getDistanceToEntity(it) <= range }
            .sortedBy { thePlayer.getDistanceToEntity(it) }

        if (targets.isEmpty()) {
            target = null
            return@handler
        }

        when (mode) {
            "Single" -> {
                val entity = targets.first()
                target = entity

                val posX = thePlayer.posX
                val posY = thePlayer.posY
                val posZ = thePlayer.posZ

                thePlayer.setPosition(entity.posX, entity.posY, entity.posZ)

                if (swing) thePlayer.swingItem()
                sendPacket(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))

                thePlayer.setPosition(posX, posY, posZ)
            }
            "Multiple" -> {
                attackedEntities.clear()
                for (entity in targets) {
                    val posX = thePlayer.posX
                    val posY = thePlayer.posY
                    val posZ = thePlayer.posZ

                    thePlayer.setPosition(entity.posX, entity.posY, entity.posZ)

                    if (swing) thePlayer.swingItem()
                    sendPacket(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))

                    thePlayer.setPosition(posX, posY, posZ)
                    attackedEntities.add(entity)
                }
                target = targets.first()
            }
        }

        val minCps = cps.first
        val maxCps = cps.last
        nextSwing = (1000.0 / (minCps + Math.random() * (maxCps - minCps))).toLong()
        clickStopWatch.reset()
    }
}

object RiseWatchdogTPAura : Module("RiseWatchdogTPAura", Category.RISE, forcedDescription = "Rise - Watchdog bypass teleport aura") {

    private val range by float("Range", 6f, 1f..8f)
    private val cps by intRange("CPS", 8..14, 1..50)
    private val swing by boolean("Swing", true)
    private val keepSprint by boolean("KeepSprint", true)
    private val failRate by float("FailRate", 0f, 0f..100f)

    private var target: EntityLivingBase? = null
    private val clickTimer = MSTimer()
    private var nextDelay = 125L

    override fun onEnable() { target = null }
    override fun onDisable() { target = null }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val theWorld = mc.theWorld ?: return@handler

        if (!clickTimer.hasTimePassed(nextDelay)) return@handler

        val targets = theWorld.loadedEntityList
            .filterIsInstance<EntityLivingBase>()
            .filter { it != thePlayer && it.health > 0 && it is EntityPlayer }
            .filter { thePlayer.getDistanceToEntity(it) <= range }
            .sortedBy { thePlayer.getDistanceToEntity(it) }

        if (targets.isEmpty()) return@handler

        val entity = targets.first()
        target = entity

        if (Math.random() * 100 < failRate) return@handler

        val prevX = thePlayer.posX
        val prevY = thePlayer.posY
        val prevZ = thePlayer.posZ

        sendPacket(C03PacketPlayer.C04PacketPlayerPosition(entity.posX, entity.posY, entity.posZ, true))
        if (swing) thePlayer.swingItem()
        sendPacket(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
        sendPacket(C03PacketPlayer.C04PacketPlayerPosition(prevX, prevY, prevZ, thePlayer.onGround))

        val minCps = cps.first
        val maxCps = cps.last
        nextDelay = (1000.0 / (minCps + Math.random() * (maxCps - minCps))).toLong()
        clickTimer.reset()
    }
}

object RiseLegitReach : Module("RiseLegitReach", Category.RISE, forcedDescription = "Rise - Legit reach with position tracking") {

    private val watchdogMode by boolean("WatchdogMode", false)
    private val maxPingSpoof by int("MaxPingSpoof", 200, 0..1000)
    private val renderRealLocation by boolean("RenderRealLocation", true)

    private var targetEntity: EntityLivingBase? = null
}

object RisePiercing : Module("RisePiercing", Category.RISE, forcedDescription = "Rise - Attack through blocks") {

    private val reachRange by float("ReachRange", 3f, 0.1f..6f)
    private val hitBoxExpand by float("HitBoxExpand", 0f, 0f..2f)
    private val swing by boolean("Swing", true)

    val onAttack = handler<AttackEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler
        val entity = event.targetEntity as? EntityLivingBase ?: return@handler

        if (thePlayer.getDistanceToEntity(entity) <= reachRange) {
            if (swing) thePlayer.swingItem()
            sendPacket(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
        }
    }
}

object RiseMimic : Module("RiseMimic", Category.RISE, forcedDescription = "Rise - Mimic target player movements") {

    private val range by float("Range", 6f, 1f..8f)
    private val mimicSprint by boolean("MimicSprint", true)
    private val mimicSneak by boolean("MimicSneak", false)

    private var target: EntityPlayer? = null

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val theWorld = mc.theWorld ?: return@handler

        target = theWorld.loadedEntityList
            .filterIsInstance<EntityPlayer>()
            .filter { it != thePlayer && thePlayer.getDistanceToEntity(it) <= range }
            .minByOrNull { thePlayer.getDistanceToEntity(it) }

        val t = target ?: return@handler
        if (mimicSprint && t.isSprinting) thePlayer.setSprinting(true)
        if (mimicSneak && t.isSneaking) thePlayer.setSneaking(true)
    }
}

object RiseFences : Module("RiseFences", Category.RISE, forcedDescription = "Rise - Attack through fence gaps") {

    private val range by float("Range", 3f, 0.1f..6f)
    private val swing by boolean("Swing", true)

    val onAttack = handler<AttackEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (swing) thePlayer.swingItem()
    }
}

object RiseKeepRange : Module("RiseKeepRange", Category.RISE, forcedDescription = "Rise - Keep target at optimal range") {

    private val minRange by float("MinRange", 2f, 0f..6f)
    private val maxRange by float("MaxRange", 3f, 0f..6f)
    private val speed by float("Speed", 0.2f, 0.01f..1f)

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val target = mc.objectMouseOver?.entityHit as? EntityLivingBase ?: return@handler
        val dist = thePlayer.getDistanceToEntity(target)

        if (dist < minRange || dist > maxRange) {
            val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
            val dir = if (dist < minRange) -1.0 else 1.0
            thePlayer.motionX += -Math.sin(yaw) * speed * dir
            thePlayer.motionZ += Math.cos(yaw) * speed * dir
        }
    }
}

object RiseKnockbackSample : Module("RiseKnockbackSample", Category.RISE, forcedDescription = "Rise - Sample and modify knockback values") {

    private val horizontal by float("Horizontal", 0f, 0f..100f)
    private val vertical by float("Vertical", 0f, 0f..100f)

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer?.entityId) {
            val h = horizontal.toDouble() / 100.0
            val v = vertical.toDouble() / 100.0
            packet.motionX = (packet.motionX * h).toInt()
            packet.motionY = (packet.motionY * v).toInt()
            packet.motionZ = (packet.motionZ * h).toInt()
        }
    }
}

object RiseLagBreak : Module("RiseLagBreak", Category.RISE, forcedDescription = "Rise - Lag-based block breaking") {

    private val range by float("Range", 5f, 0.1f..6f)

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is C07PacketPlayerDigging && packet.status == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
            event.cancelEvent()
            sendPacket(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, packet.position, packet.facing))
        }
    }
}

object RiseRegen : Module("RiseRegen", Category.RISE, forcedDescription = "Rise - Faster health regeneration") {

    private val mode by choices("Mode", arrayOf("Packet", "NCPPacket", "Vanilla"), "Packet")
    private val speed by int("Speed", 15, 1..100)

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (thePlayer.health < thePlayer.maxHealth && thePlayer.health > 0f && thePlayer.hurtTime <= 0) {
            when (mode) {
                "Packet" -> {
                    repeat(speed) {
                        sendPacket(C03PacketPlayer(thePlayer.onGround))
                    }
                }
                "NCPPacket" -> {
                    repeat(speed) {
                        sendPacket(C03PacketPlayer.C04PacketPlayerPosition(
                            thePlayer.posX, thePlayer.posY, thePlayer.posZ, thePlayer.onGround
                        ))
                    }
                }
                "Vanilla" -> {
                    repeat(speed - 1) {
                        thePlayer.heal(1f)
                    }
                }
            }
        }
    }
}

object RiseRotationSnapshot : Module("RiseRotationSnapshot", Category.RISE, forcedDescription = "Rise - Snapshot rotations for accurate attacks") {

    private val maxSnapshots by int("MaxSnapshots", 5, 1..20)

    private val rotationSnapshots = mutableListOf<Pair<Float, Float>>()

    val onAttack = handler<AttackEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        rotationSnapshots.add(thePlayer.rotationYaw to thePlayer.rotationPitch)
        if (rotationSnapshots.size > maxSnapshots) {
            rotationSnapshots.removeFirst()
        }
    }
}

object RiseAimAssist : Module("RiseAimAssist", Category.RISE, forcedDescription = "Rise - Aim assist for legitimate RISE") {

    private val mode by choices("Mode", arrayOf("Silent", "Blatant"), "Silent")
    private val range by float("Range", 4.5f, 1f..8f)
    private val fOV by float("FOV", 180f, 1f..360f)
    private val speed by float("Speed", 30f, 1f..180f)
    private val sticky by boolean("Sticky", true)
    private val silent by boolean("Silent", true)
    private val requireMouseMovement by boolean("RequireMouseMovement", false)
    private val requireSwinging by boolean("RequireSwinging", false)
    private val players by boolean("Players", true)
    private val mobs by boolean("Mobs", false)
    private val hitBoxExpand by float("HitBoxExpand", 0f, 0f..2f)

    private var target: EntityLivingBase? = null

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val theWorld = mc.theWorld ?: return@handler

        if (requireSwinging && thePlayer.swingProgress <= 0f) return@handler

        target = theWorld.loadedEntityList
            .filterIsInstance<EntityLivingBase>()
            .filter { it != thePlayer && it.health > 0 }
            .filter { if (players) it is EntityPlayer else true }
            .filter { thePlayer.getDistanceToEntity(it) <= range }
            .filter {
                val diff = abs(MathHelper.wrapAngleTo180_float(thePlayer.rotationYaw - getRotations(it).first))
                diff <= fOV / 2f
            }
            .minByOrNull { thePlayer.getDistanceToEntity(it) }

        val t = target ?: return@handler
        val (targetYaw, targetPitch) = getRotations(t)

        val yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - thePlayer.rotationYaw)
        val pitchDiff = targetPitch - thePlayer.rotationPitch

        val factor = speed / 180f

        if (mode == "Silent") {
            thePlayer.rotationYaw = thePlayer.rotationYaw + yawDiff * factor
            thePlayer.rotationPitch = thePlayer.rotationPitch + pitchDiff * factor
        } else {
            thePlayer.rotationYaw = thePlayer.rotationYaw + yawDiff * factor
            thePlayer.rotationPitch = thePlayer.rotationPitch + pitchDiff * factor
        }
    }

    private fun getRotations(entity: EntityLivingBase): Pair<Float, Float> {
        val thePlayer = mc.thePlayer ?: return 0f to 0f
        val diffX = entity.posX - thePlayer.posX
        val diffY = entity.posY + entity.eyeHeight / 2.0 - (thePlayer.posY + thePlayer.eyeHeight)
        val diffZ = entity.posZ - thePlayer.posZ
        val dist = sqrt(diffX * diffX + diffZ * diffZ)
        val yaw = (Math.toDegrees(kotlin.math.atan2(-diffX, diffZ))).toFloat()
        val pitch = (Math.toDegrees(-kotlin.math.atan2(diffY, dist))).toFloat()
        return yaw to pitch
    }
}

object RiseAimBacktrack : Module("RiseAimBacktrack", Category.RISE, forcedDescription = "Rise - Aim at backtrack positions") {

    private val range by float("Range", 3f, 0.1f..6f)
    private val ticks by int("Ticks", 2, 1..20)

    private val positionHistory = mutableMapOf<Int, ArrayDeque<Triple<Double, Double, Double>>>()

    val onUpdate = handler<UpdateEvent> {
        val theWorld = mc.theWorld ?: return@handler
        for (entity in theWorld.loadedEntityList.filterIsInstance<EntityLivingBase>()) {
            val history = positionHistory.getOrPut(entity.entityId) { ArrayDeque(ticks + 1) }
            history.addFirst(Triple(entity.posX, entity.posY, entity.posZ))
            while (history.size > ticks) history.removeLast()
        }
    }
}

object RiseAutoClicker : Module("RiseAutoClicker", Category.RISE, forcedDescription = "Rise - Auto clicker with jitter") {

    private val mode by choices("Mode", arrayOf("Normal", "DragClickSimulations"), "Normal")
    private val cps by intRange("CPS", 8..14, 1..50)
    private val jitter by boolean("Jitter", false)
    private val rightClick by boolean("RightClick", false)

    private val clickTimer = MSTimer()
    private var nextDelay = 125L

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (!clickTimer.hasTimePassed(nextDelay)) return@handler

        if (rightClick) {
            mc.rightClickMouse()
        } else {
            mc.clickMouse()
        }

        val minCps = cps.first
        val maxCps = cps.last
        var delay = (1000.0 / (minCps + Math.random() * (maxCps - minCps)))

        if (jitter) {
            delay += (Math.random() - 0.5) * 25.0
        }

        nextDelay = delay.toLong().coerceAtLeast(1)
        clickTimer.reset()
    }
}

object RiseClickAssist : Module("RiseClickAssist", Category.RISE, forcedDescription = "Rise - Click assist for RISE") {

    private val chance by float("Chance", 50f, 1f..100f)

    val onAttack = handler<AttackEvent> {
        if (Math.random() * 100 < chance) {
            mc.thePlayer?.swingItem()
        }
    }
}

object RiseDisplacementSample : Module("RiseDisplacementSample", Category.RISE, forcedDescription = "Rise - Sample displacement values") {

    private val sampleSize by int("SampleSize", 100, 10..1000)
    private val samples = mutableListOf<Double>()

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val expected = hypot(thePlayer.motionX, thePlayer.motionZ)
        val actual = hypot(thePlayer.posX - thePlayer.lastTickPosX, thePlayer.posZ - thePlayer.lastTickPosZ)
        samples.add(abs(expected - actual))
        if (samples.size > sampleSize) samples.removeFirst()
    }
}

object RiseFastPlace : Module("RiseFastPlace", Category.RISE, forcedDescription = "Rise - Fast block placement") {

    private val speed by int("Speed", 0, 0..4)

    val onUpdate = handler<UpdateEvent> {
        mc.rightClickDelayTimer = speed
    }
}

object RiseGuiClicker : Module("RiseGuiClicker", Category.RISE, forcedDescription = "Rise - Auto click in GUI") {

    private val delay by int("Delay", 50, 10..1000)
    private val clickTimer = MSTimer()

    val onUpdate = handler<UpdateEvent> {
        if (mc.currentScreen != null && clickTimer.hasTimePassed(delay.toLong())) {
            mc.clickMouse()
            clickTimer.reset()
        }
    }
}

object RiseKeepSprint : Module("RiseKeepSprint", Category.RISE, forcedDescription = "Rise - Keep sprinting while attacking") {

    private val mode by choices("Mode", arrayOf("Vanilla", "Watchdog", "Grim", "Matrix"), "Vanilla")

    val onAttack = handler<AttackEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        when (mode) {
            "Vanilla" -> thePlayer.setSprinting(true)
            "Watchdog" -> {
                thePlayer.setSprinting(true)
                sendPacket(C03PacketPlayer(thePlayer.onGround))
            }
            "Grim" -> thePlayer.setSprinting(true)
            "Matrix" -> {
                thePlayer.setSprinting(true)
                thePlayer.motionX *= 1.001
                thePlayer.motionZ *= 1.001
            }
        }
    }
}

object RiseLegitScaffold : Module("RiseLegitScaffold", Category.RISE, forcedDescription = "Rise - Legit scaffold placement") {

    private val expand by float("Expand", 1f, 0.1f..6f)
    private val delay by int("Delay", 0, 0..100)

    private val placeTimer = MSTimer()

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (!placeTimer.hasTimePassed(delay.toLong())) return@handler

        val movingObject = mc.objectMouseOver ?: return@handler
        if (movingObject.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            val pos = movingObject.blockPos
            val facing = movingObject.sideHit
            if (mc.playerController.onPlayerRightClick(thePlayer, mc.theWorld, thePlayer.heldItem, pos, facing, movingObject.hitVec)) {
                thePlayer.swingItem()
                placeTimer.reset()
            }
        }
    }
}

object RiseManualKBDisplacement : Module("RiseManualKBDisplacement", Category.RISE, forcedDescription = "Rise - Manual knockback displacement") {

    private val horizontal by float("Horizontal", 0f, -5f..5f)
    private val vertical by float("Vertical", 0f, -5f..5f)

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer?.entityId) {
            event.cancelEvent()
            val thePlayer = mc.thePlayer ?: return@handler
            thePlayer.motionX += horizontal.toDouble()
            thePlayer.motionY += vertical.toDouble()
            thePlayer.motionZ += horizontal.toDouble()
        }
    }
}

object RiseNoClickDelay : Module("RiseNoClickDelay", Category.RISE, forcedDescription = "Rise - Remove click delay") {

    private val mode by choices("Mode", arrayOf("Vanilla", "NCP"), "Vanilla")

    val onUpdate = handler<UpdateEvent> {
        when (mode) {
            "Vanilla" -> {
                mc.leftClickCounter = 0
            }
            "NCP" -> {
                mc.leftClickCounter = 0
                mc.rightClickDelayTimer = 0
            }
        }
    }
}

object RiseOldWTap : Module("RiseOldWTap", Category.RISE, forcedDescription = "Rise - Old W-Tap style sprint reset") {

    private val delay by int("Delay", 0, 0..100)
    private val resetTimer = MSTimer()

    val onAttack = handler<AttackEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (thePlayer.isSprinting && resetTimer.hasTimePassed(delay.toLong())) {
            thePlayer.setSprinting(false)
            thePlayer.setSprinting(true)
            resetTimer.reset()
        }
    }
}

object RiseWTap : Module("RiseWTap", Category.RISE, forcedDescription = "Rise - W-Tap sprint reset on attack") {

    private val delay by int("Delay", 0, 0..100)
    private val mode by choices("Mode", arrayOf("Stop", "Start"), "Stop")
    private val resetTimer = MSTimer()

    val onAttack = handler<AttackEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (thePlayer.isSprinting && resetTimer.hasTimePassed(delay.toLong())) {
            when (mode) {
                "Stop" -> {
                    thePlayer.setSprinting(false)
                }
                "Start" -> {
                    thePlayer.setSprinting(false)
                    thePlayer.setSprinting(true)
                }
            }
            resetTimer.reset()
        }
    }
}

object RiseThrowableAura : Module("RiseThrowableAura", Category.RISE, forcedDescription = "Rise - Auto throw projectiles at targets") {

    private val range by float("Range", 6f, 1f..10f)
    private val fOV by float("FOV", 90f, 30f..180f)
    private val predict by boolean("Predict", true)
    private val predictTicks by float("PredictTicks", 3f, 1f..10f)
    private val useBow by boolean("UseBow", true)
    private val autoShoot by boolean("AutoShoot", true)
    private val silent by boolean("Silent", true)

    private var target: EntityLivingBase? = null

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val theWorld = mc.theWorld ?: return@handler

        target = theWorld.loadedEntityList
            .filterIsInstance<EntityLivingBase>()
            .filter { it != thePlayer && it.health > 0 }
            .filter { it is EntityPlayer }
            .filter { thePlayer.getDistanceToEntity(it) <= range }
            .filter {
                val diff = abs(MathHelper.wrapAngleTo180_float(thePlayer.rotationYaw - getRotations(it).first))
                diff <= fOV / 2f
            }
            .minByOrNull { thePlayer.getDistanceToEntity(it) }

        val t = target ?: return@handler

        if (thePlayer.heldItem?.item is ItemBow && useBow) {
            if (thePlayer.itemInUseCount > 0 && autoShoot) {
                val (targetYaw, targetPitch) = getRotations(t)
                if (silent) {
                    thePlayer.rotationYaw = targetYaw
                    thePlayer.rotationPitch = targetPitch
                }
                mc.playerController.onStoppedUsingItem(thePlayer)
            }
            return@handler
        }

        if (thePlayer.heldItem?.item is ItemFishingRod) {
            val (targetYaw, targetPitch) = getRotations(t)
            if (silent) {
                thePlayer.rotationYaw = targetYaw
                thePlayer.rotationPitch = targetPitch
            }
            if (thePlayer.fishEntity == null) {
                mc.playerController.sendUseItem(thePlayer, theWorld, thePlayer.heldItem)
            } else {
                mc.playerController.sendUseItem(thePlayer, theWorld, thePlayer.heldItem)
            }
            return@handler
        }

        if (thePlayer.heldItem?.item is ItemSnowball || thePlayer.heldItem?.item is ItemEgg) {
            val (targetYaw, targetPitch) = getRotations(t)
            if (silent) {
                thePlayer.rotationYaw = targetYaw
                thePlayer.rotationPitch = targetPitch
            }
            mc.playerController.sendUseItem(thePlayer, theWorld, thePlayer.heldItem)
        }
    }

    private fun getRotations(entity: EntityLivingBase): Pair<Float, Float> {
        val thePlayer = mc.thePlayer ?: return 0f to 0f
        val predX = if (predict) entity.posX + (entity.posX - entity.prevPosX) * predictTicks else entity.posX
        val predY = if (predict) entity.posY + (entity.posY - entity.prevPosY) * predictTicks else entity.posY
        val predZ = if (predict) entity.posZ + (entity.posZ - entity.prevPosZ) * predictTicks else entity.posZ
        val diffX = predX - thePlayer.posX
        val diffY = predY + entity.eyeHeight / 2.0 - (thePlayer.posY + thePlayer.eyeHeight)
        val diffZ = predZ - thePlayer.posZ
        val dist = sqrt(diffX * diffX + diffZ * diffZ)
        val yaw = (Math.toDegrees(kotlin.math.atan2(-diffX, diffZ))).toFloat()
        val pitch = (Math.toDegrees(-kotlin.math.atan2(diffY, dist))).toFloat()
        return yaw to pitch
    }
}
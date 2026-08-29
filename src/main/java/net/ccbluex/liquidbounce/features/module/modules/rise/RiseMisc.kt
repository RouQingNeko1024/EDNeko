package net.ccbluex.liquidbounce.features.module.modules.rise

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S0CPacketSpawnPlayer
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Blocks
import java.util.UUID

object RiseAntiBot : Module("RiseAntiBot", Category.RISE, forcedDescription = "Rise - Advanced bot detection") {

    private val funcraftCheck by boolean("FuncraftCheck", false)
    private val tabCheck by boolean("TabCheck", false)
    private val npcDetection by boolean("NPCDetection", false)
    private val duplicateNameCheck by boolean("DuplicateNameCheck", false)
    private val noPingCheck by boolean("NoPingCheck", false)
    private val cubecraftBedrockCheck by boolean("CubecraftBedrockCheck", false)
    private val duplicateUniqueIDCheck by boolean("DuplicateUniqueIDCheck", false)
    private val colourCheck by boolean("ColourCheck", false)
    private val timeVisibleCheck by boolean("TimeVisibleCheck", false)
    private val middleClick by boolean("MiddleClickBot", false)
    private val advanced by boolean("Advanced", false)
    private val advanced2 by boolean("Advanced2", false)

    private val botList = mutableSetOf<UUID>()
    private val nameMap = mutableMapOf<String, MutableList<UUID>>()
    private val tickMap = mutableMapOf<UUID, Int>()

    fun isBot(entity: EntityLivingBase): Boolean {
        if (entity !is EntityPlayer) return false
        val uuid = entity.uniqueID
        if (botList.contains(uuid)) return true
        if (entity.isInvisible && advanced) return true
        return false
    }

    val onUpdate = handler<UpdateEvent> {
        val world = mc.theWorld ?: return@handler
        for (entity in world.loadedEntityList) {
            if (entity !is EntityPlayer) continue
            val uuid = entity.uniqueID

            if (tabCheck) {
                val networkPlayer = mc.netHandler.getPlayerInfo(uuid)
                if (networkPlayer == null && entity.ticksExisted > 200) {
                    botList.add(uuid)
                }
            }

            if (duplicateNameCheck) {
                val name = entity.name
                nameMap.getOrPut(name) { mutableListOf() }.let { list ->
                    if (!list.contains(uuid)) list.add(uuid)
                    if (list.size > 1) list.forEach { botList.add(it) }
                }
            }

            if (duplicateUniqueIDCheck) {
                val count = world.loadedEntityList.count {
                    it is EntityPlayer && it.uniqueID == uuid
                }
                if (count > 1) botList.add(uuid)
            }

            if (timeVisibleCheck) {
                tickMap[uuid] = (tickMap[uuid] ?: 0) + 1
                if ((tickMap[uuid] ?: 0) > 100 && entity.isInvisible) {
                    botList.add(uuid)
                }
            }
        }
    }
}

object RiseAntiAFK : Module("RiseAntiAFK", Category.RISE, forcedDescription = "Rise - Prevent AFK kick") {

    private val mode by choices("Mode", arrayOf("Vanilla", "NCP", "Hypixel"), "Vanilla")
    private val delay by int("Delay", 5000, 1000..30000)

    private val timerUtil = MSTimer()

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (!timerUtil.hasTimePassed(delay.toLong())) return@handler

        when (mode) {
            "Vanilla" -> {
                sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY - 0.01, thePlayer.posZ, true))
                sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
            }
            "NCP" -> {
                thePlayer.rotationYaw += 1f
                sendPacket(C03PacketPlayer.C05PacketPlayerLook(thePlayer.rotationYaw, thePlayer.rotationPitch, thePlayer.onGround))
            }
            "Hypixel" -> {
                sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY - 0.01, thePlayer.posZ, false))
                sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
            }
        }
        timerUtil.reset()
    }
}

object RiseAntiCrash : Module("RiseAntiCrash", Category.RISE, forcedDescription = "Rise - Prevent client crashes") {

    private val maxEntities by int("MaxEntities", 500, 100..5000)
    private val maxParticles by int("MaxParticles", 1000, 100..10000)

    override fun onEnable() {
        mc.gameSettings.limitFramerate = mc.gameSettings.limitFramerate
    }

    val onUpdate = handler<UpdateEvent> {
        val world = mc.theWorld ?: return@handler
        val entityList = world.loadedEntityList
        if (entityList.size > maxEntities) {
            val toRemove = entityList.size - maxEntities
            repeat(toRemove) {
                val entity = entityList.lastOrNull { !it.isEntityAlive }
                entity?.let { world.removeEntity(it) }
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is net.minecraft.network.play.server.S2APacketParticles) {
            val packet = event.packet
            if (packet.particleCount > maxParticles) {
                event.cancelEvent()
            }
        }
    }
}

object RiseAutoGG : Module("RiseAutoGG", Category.RISE, forcedDescription = "Rise - Auto say GG after game") {

    private val message by text("Message", "gg")
    private val delay by int("Delay", 1000, 0..5000)

    private var triggered = false
    private val timerUtil = MSTimer()

    override fun onEnable() {
        triggered = false
    }

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is S02PacketChat) {
            val text = event.packet.chatComponent.unformattedText
            if (text.contains("1st Killer") || text.contains("Winner") || text.contains("VICTORY") ||
                text.contains("won the game") || text.contains("1st Place")) {
                triggered = true
            }
        }
    }

    val onUpdate = handler<UpdateEvent> {
        if (triggered && timerUtil.hasTimePassed(delay.toLong())) {
            mc.thePlayer?.sendChatMessage(message)
            triggered = false
            timerUtil.reset()
        }
    }
}

object RiseNoGuiClose : Module("RiseNoGuiClose", Category.RISE, forcedDescription = "Rise - Prevent GUI closing") {

    private val mode by choices("Mode", arrayOf("Vanilla", "NCP"), "Vanilla")

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is net.minecraft.network.play.server.S2EPacketCloseWindow) {
            event.cancelEvent()
        }
    }
}

object RiseNoPitchLimit : Module("RiseNoPitchLimit", Category.RISE, forcedDescription = "Rise - Remove pitch rotation limit") {

    private val mode by choices("Mode", arrayOf("Vanilla", "NCP"), "Vanilla")

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (thePlayer.rotationPitch > 90f) {
            thePlayer.rotationPitch = 90f
        }
        if (thePlayer.rotationPitch < -90f) {
            thePlayer.rotationPitch = -90f
        }
    }
}

object RiseNuker : Module("RiseNuker", Category.RISE, forcedDescription = "Rise - Auto nuke blocks") {

    private val range by int("Range", 5, 1..10)
    private val mode by choices("Mode", arrayOf("All", "Flat", "Smash"), "All")
    private val delay by int("Delay", 50, 0..500)

    private val timerUtil = MSTimer()

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler
        if (!timerUtil.hasTimePassed(delay.toLong())) return@handler

        var bestBlock: BlockPos? = null
        var bestDist = Double.MAX_VALUE

        val px = thePlayer.posX.toInt()
        val py = when (mode) {
            "Flat" -> thePlayer.posY.toInt() - 1
            "Smash" -> thePlayer.posY.toInt() + 1
            else -> thePlayer.posY.toInt()
        }
        val pz = thePlayer.posZ.toInt()

        val yRange = when (mode) {
            "All" -> (-range..range)
            "Flat" -> (py..py)
            "Smash" -> (py - range..py)
            else -> (-range..range)
        }

        for (x in -range..range) {
            for (y in yRange) {
                for (z in -range..range) {
                    val pos = BlockPos(px + x, py + y, pz + z)
                    val block = world.getBlockState(pos).block
                    if (block == Blocks.air || block == Blocks.bedrock) continue
                    val dist = thePlayer.getDistanceSq(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
                    if (dist < bestDist) {
                        bestDist = dist
                        bestBlock = pos
                    }
                }
            }
        }

        if (bestBlock != null) {
            sendPacket(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, bestBlock, EnumFacing.UP))
            sendPacket(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, bestBlock, EnumFacing.UP))
            timerUtil.reset()
        }
    }
}

object RisePlayerNotifier : Module("RisePlayerNotifier", Category.RISE, forcedDescription = "Rise - Notify when players join/leave") {

    private val ignoreVanish by boolean("IgnoreVanish", true)
    private val message by text("Message", "Player {name} joined!")

    private val knownPlayers = mutableSetOf<UUID>()

    val onUpdate = handler<UpdateEvent> {
        val world = mc.theWorld ?: return@handler
        val currentPlayers = mutableSetOf<UUID>()
        for (entity in world.loadedEntityList) {
            if (entity is EntityPlayer) {
                currentPlayers.add(entity.uniqueID)
                if (!knownPlayers.contains(entity.uniqueID)) {
                    mc.thePlayer?.addChatMessage(
                        net.minecraft.util.ChatComponentText(message.replace("{name}", entity.name))
                    )
                }
            }
        }
        knownPlayers.clear()
        knownPlayers.addAll(currentPlayers)
    }
}

object RiseSpammer : Module("RiseSpammer", Category.RISE, forcedDescription = "Rise - Auto spam messages") {

    private val message by text("Message", "Buy Rise at riseclient.com!")
    private val delay by int("Delay", 3000, 0..20000)

    private val timerUtil = MSTimer()

    val onUpdate = handler<UpdateEvent> {
        if (!timerUtil.hasTimePassed(delay.toLong())) return@handler
        mc.thePlayer?.sendChatMessage(message)
        timerUtil.reset()
    }
}

object RiseChatBypass : Module("RiseChatBypass", Category.RISE, forcedDescription = "Rise - Bypass chat filters") {

    private val mode by choices("Mode", arrayOf("Unicode", "Reverse", "Spacing"), "Unicode")

    fun bypassMessage(msg: String): String = when (mode) {
        "Unicode" -> msg.map { "${it}\u200B" }.joinToString("")
        "Reverse" -> msg.reversed()
        "Spacing" -> msg.map { "$it " }.joinToString("")
        else -> msg
    }
}

object RiseClientSpoofer : Module("RiseClientSpoofer", Category.RISE, forcedDescription = "Rise - Spoof client brand") {

    private val brand by text("Brand", "vanilla")

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is net.minecraft.network.play.client.C17PacketCustomPayload) {
            if (event.packet.channelName == "MC|Brand") {
                event.cancelEvent()
                sendPacket(net.minecraft.network.play.client.C17PacketCustomPayload("MC|Brand",
                    net.minecraft.network.PacketBuffer(net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer()).writeString(brand))))
            }
        }
    }
}

object RiseInsults : Module("RiseInsults", Category.RISE, forcedDescription = "Rise - Auto insult killed players") {

    private val message by text("Message", "EZ")
    private val delay by int("Delay", 1000, 0..5000)

    private val timerUtil = MSTimer()
    private var dead = false

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (thePlayer.health <= 0f) {
            dead = true
        }
        if (thePlayer.health > 0f && dead) {
            dead = false
        }
    }

    val onAttack = handler<AttackEvent> {
        if (timerUtil.hasTimePassed(delay.toLong())) {
            mc.thePlayer?.sendChatMessage(message)
            timerUtil.reset()
        }
    }
}

object RiseHypixelAutoPlay : Module("RiseHypixelAutoPlay", Category.RISE, forcedDescription = "Rise - Auto play on Hypixel") {

    private val mode by choices("Mode", arrayOf("Bedwars", "Skywars", "Duels"), "Bedwars")
    private val delay by int("Delay", 5000, 1000..30000)

    private val timerUtil = MSTimer()
    private var gameEnded = false

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is S02PacketChat) {
            val text = event.packet.chatComponent.unformattedText
            if (text.contains("1st Killer") || text.contains("Winner") || text.contains("won the game")) {
                gameEnded = true
            }
        }
    }

    val onUpdate = handler<UpdateEvent> {
        if (gameEnded && timerUtil.hasTimePassed(delay.toLong())) {
            when (mode) {
                "Bedwars" -> mc.thePlayer?.sendChatMessage("/play bedwars_eight_one")
                "Skywars" -> mc.thePlayer?.sendChatMessage("/play solo_insane")
                "Duels" -> mc.thePlayer?.sendChatMessage("/play duels_classic")
            }
            gameEnded = false
            timerUtil.reset()
        }
    }
}

object RiseTimer : Module("RiseTimer", Category.RISE, forcedDescription = "Rise - Game timer modifier") {

    private val speed by float("Speed", 1.5f, 0.1f..10f)

    override fun onEnable() {
        mc.timer.timerSpeed = speed
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

    val onUpdate = handler<UpdateEvent> {
        mc.timer.timerSpeed = speed
    }
}
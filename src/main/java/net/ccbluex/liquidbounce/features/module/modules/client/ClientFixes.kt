//skid firebounce
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.entity.item.EntityItem
import net.minecraft.network.play.client.C19PacketResourcePackStatus
import net.minecraft.network.play.server.*
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumChatFormatting
import java.net.URI
import java.net.URISyntaxException
import java.util.*

object ClientFixes : Module("ClientFixes", Category.CLIENT) {
    val noException by boolean("NoPacketException", false)
    val dataWatcherFix by boolean("DataWatcherFix", false)
    private val blockPacket by boolean("S0APacketUseBed", true)
    private val chatPacket by boolean("S02PacketChat", true)
    private val multiBlockPacket by boolean("S22PacketMultiBlockChange", true)
    private val spawnObjectPacket by boolean("S0EPacketSpawnObject", true)
    private val spawnPlayerPacket by boolean("S0CPacketSpawnPlayer", true)
    private val entityStatusPacket by boolean("S19PacketEntityStatus", true)
    private val updateSignPacket by boolean("S33PacketUpdateSign", true)
    private val destroyEntitiesPacket by boolean("S13PacketDestroyEntities", true)
    private val collectItemPacket by boolean("S0DPacketCollectItem", true)
    private val cameraPacket by boolean("S43PacketCamera", true)
    private val entityPacket by boolean("S14PacketEntity", true)
    private val spawnExpOrbPacket by boolean("S11PacketSpawnExperienceOrb", true)
    private val changeGameStatePacket by boolean("S2BPacketChangeGameState", true)
    private val heldItemPacket by boolean("S09PacketHeldItemChange", true)
    private val playerAbilitiesPacket by boolean("S39PacketPlayerAbilities", true)
    private val updateHealthPacket by boolean("S06PacketUpdateHealth", true)
    private val playerPosLookPacket by boolean("S08PacketPlayerPosLook", true)
    private val entityVelocityPacket by boolean("S12PacketEntityVelocity", true)
    private val particlesPacket by boolean("S2APacketParticles", true)
    private val explosionPacket by boolean("S27PacketExplosion", true)
    private val timePacket by boolean("S03PacketTimeUpdate", true)
    private val openWindowPacket by boolean("S2DPacketOpenWindow", true)
    private val resourcePackSendPacket by boolean("S48PacketResourcePackSend", true)
    private val debug by boolean("DebugMessage",true)
    var lastExperience: Long = 0
    var lastExplosion: Long = 0
    var lastGuardian: Long = 0
    var lastArrow: Long = 0
    var invalidExperience: Int = 0
    val CONST_VALID_SLOT_COUNT = arrayOf(0, 1, 3, 5, 9, 27, 54)
    override fun onEnable() {
        lastExperience = 0
        lastExplosion = 0
        lastGuardian = 0
        lastArrow = 0
        invalidExperience = 0
    }
    val onPacket = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is S48PacketResourcePackSend -> {
                if(!resourcePackSendPacket)return@handler
                var shouldCancel = false
                try {
                    val uri = URI(packet.url)
                    val scheme = uri.scheme

                    val isLevelProtocol = scheme.equals("level", ignoreCase = true)
                    val isHttpProtocol = scheme.equals("http", ignoreCase = true)
                    val isHttpsProtocol = scheme.equals("https", ignoreCase = true)

                    if (!isHttpProtocol && !isHttpsProtocol && !isLevelProtocol) {
                        shouldCancel = true
                    } else if (isLevelProtocol && (packet.url.contains("..") || !packet.url
                            .endsWith("/resources.zip"))
                    ) {
                        shouldCancel = true
                        mc.netHandler.addToSendQueue(
                            C19PacketResourcePackStatus(
                                packet.hash,
                                C19PacketResourcePackStatus.Action.FAILED_DOWNLOAD
                            )
                        )
                    }
                } catch (_: URISyntaxException) {
                    shouldCancel = true
                    mc.netHandler.addToSendQueue(
                        C19PacketResourcePackStatus(
                            packet.hash,
                            C19PacketResourcePackStatus.Action.FAILED_DOWNLOAD
                        )
                    )
                }
                if(shouldCancel){
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid resource pack packet.")
                }
            }
            is S2DPacketOpenWindow -> {
                if(!openWindowPacket) return@handler
                if(!CONST_VALID_SLOT_COUNT.contains(packet.slotCount)){
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid open window packet.")
                }
            }
            is S12PacketEntityVelocity ->{
                if(!entityVelocityPacket)return@handler
                if(packet.entityID!=mc.thePlayer.entityId)return@handler
                if(isNotValid(packet.motionX, 31201)||isNotValid(packet.motionY, 31201)||isNotValid(packet.motionZ, 31201)){
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid entity velocity packet.")
                }

            }
            is S2APacketParticles ->{
                if(!particlesPacket)return@handler
                if(isNotValidPosition(BlockPos(packet.xOffset.toDouble(), packet.yOffset.toDouble(), packet.zOffset.toDouble()))||isNotValidPosition(BlockPos(packet.xCoordinate, packet.yCoordinate, packet.zCoordinate))||isNotValid(packet.particleSpeed.toDouble(), 3000)||isNotValid(packet.particleCount, 1200)||isNotValid(packet.particleArgs.size, 30000000)){
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid particles packet.")
                }
            }
            is S08PacketPlayerPosLook ->{
                if(!playerPosLookPacket)return@handler
                if(isNotValidPosition(BlockPos(packet.x, packet.y, packet.z))||isNotValid(packet.yaw.toDouble(), 10800)||isNotValid(packet.pitch.toDouble(), 360)){
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid player pos look packet.")
                }
            }
            is S06PacketUpdateHealth -> {
                if(!updateHealthPacket)return@handler
                if (isNotValid(packet.health.toDouble(), mc.thePlayer.maxHealth + 1E3)||isNotValid(packet.foodLevel, 1E5)||isNotValid(packet.saturationLevel.toDouble(), 512)) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid update health packet.")
                }
            }
            is S39PacketPlayerAbilities -> {
                if(!playerAbilitiesPacket)return@handler
                if (isNotValid(packet.flySpeed.toDouble(), 1)||isNotValid(packet.walkSpeed.toDouble(), 2)) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid player abilities packet.")
                }
            }
            is S2BPacketChangeGameState -> {
                if(!changeGameStatePacket)return@handler
                if (packet.gameState == 5) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid change game state packet.")
                }
                if (packet.gameState == 7 || packet.gameState == 8) {
                    if (packet.func_149137_d() >= 100) {
                        event.cancelEvent()
                        debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid change game state packet.")
                    }
                }
                if (packet.gameState == 10) {
                    if (System.currentTimeMillis() - lastGuardian < 5) {
                        event.cancelEvent()
                        debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid change game state packet.")
                    }
                    lastGuardian = System.currentTimeMillis()
                }
            }
            is S09PacketHeldItemChange -> {
                if(!heldItemPacket)return@handler
                if (isNotValid(packet.heldItemHotbarIndex, 8) || packet.heldItemHotbarIndex < 0) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]"+  EnumChatFormatting.YELLOW + " Invalid held item change packet.")
                }
            }
            is S11PacketSpawnExperienceOrb -> {
                if(!spawnExpOrbPacket)return@handler
                if (System.currentTimeMillis() - lastExperience <= 1) {
                    invalidExperience++

                    if (invalidExperience >= 2) {
                        event.cancelEvent()
                    }
                } else {
                    invalidExperience = 0
                }
                lastExperience = System.currentTimeMillis()
            }
            is S43PacketCamera -> {
                if(!cameraPacket)return@handler
                if ((packet.getEntity(mc.theWorld) == mc.thePlayer && !mc.thePlayer.isSpectator)) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid camera packet.")
                }
            }
            is S33PacketUpdateSign -> {
                if(!updateSignPacket)return@handler
                if (isNotValid(
                        packet.lines[0].unformattedText.length + packet.lines[1].unformattedText.length + packet.lines[2].unformattedText.length + packet.lines[3].unformattedText.length,
                        2048
                    )
                ) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid sign packet.")
                }
            }
            is S0DPacketCollectItem -> {
                if(!collectItemPacket)return@handler
                if (mc.theWorld.getEntityByID(packet.collectedItemEntityID) is EntityItem && isNotValid(
                        mc.theWorld.getEntityByID(
                            packet.collectedItemEntityID
                        ).name.length, 512
                    )
                ) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid collect item packet.")
                }
            }
            is S14PacketEntity -> {
                if(!entityPacket)return@handler
                val entity = packet.getEntity(mc.theWorld) ?: return@handler
                val box = entity.entityBoundingBox ?: return@handler
                if (isNotValid(
                        (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ), 1000
                    )
                ) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid entity packet.")
                }
            }
            is S0APacketUseBed -> {
                if(!blockPacket)return@handler
                if (isNotValidPosition(packet.bedPosition)) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid bed position.")
                }
            }

            is S27PacketExplosion -> {
                if(!explosionPacket)return@handler
                if (isNotValid(packet.x, 30000000) || isNotValid(packet.y, 30000000) || isNotValid(
                        packet.z,
                        30000000
                    )
                    || isNotValid(packet.strength.toDouble(), 100)
                    || isNotValid(packet.field_149152_f.toDouble(), 8)
                    || isNotValid(packet.field_149153_g.toDouble(), 8)
                    || isNotValid(packet.field_149159_h.toDouble(), 8)
                    || isNotValid(
                        packet.affectedBlockPositions.size,
                        3000
                    ) || System.currentTimeMillis() - lastExplosion <= 1
                ) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid explosion packet.")
                } else lastExplosion = System.currentTimeMillis()
            }
            is S22PacketMultiBlockChange -> {
                if(!multiBlockPacket)return@handler
                if (isNotValid(packet.changedBlocks.size, 300000000)) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid multi block change packet.")
                }
            }
            is S02PacketChat -> {
                if(!chatPacket)return@handler
                if (isNotValid(packet.chatComponent.formattedText.length, 2048)) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid chat packet.")
                }
            }
            is S0EPacketSpawnObject -> {
                if(!spawnObjectPacket)return@handler
                if (isNotValid(packet.x, 2.9999997E7) || isNotValid(packet.y, 2.9999997E7) || isNotValid(
                        packet.z,
                        2.9999997E7
                    ) || isNotValid(packet.yaw, 10800) || isNotValid(packet.pitch, 10800) || isNotValid(
                        packet.speedX,
                        32767
                    ) || isNotValid(packet.speedY, 32767) || isNotValid(packet.speedZ, 32767)
                ) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid spawn object packet.")
                }
                if(packet.type == 60){
                    if(System.currentTimeMillis()-lastArrow<20){
                        event.cancelEvent()
                        debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid arrow packet.")
                    }
                    lastArrow=System.currentTimeMillis()
                }
            }
            is S0CPacketSpawnPlayer -> {
                if(!spawnPlayerPacket)return@handler
                if (isNotValid(packet.x, 2.9999997E7) || isNotValid(packet.y, 2.9999997E7) || isNotValid(
                        packet.z, 2.9999997E7
                    ) || isNotValid(packet.yaw.toDouble(), 10800) || isNotValid(packet.pitch.toDouble(), 10800)
                ) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid spawn player packet.")
                }
            }
            is S19PacketEntityStatus -> {
                if(!entityStatusPacket)return@handler
                if (packet.opCode == 3.toByte() && packet.getEntity(mc.theWorld) == mc.thePlayer) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid entity status packet.")
                }
            }
            is S13PacketDestroyEntities -> {
                if(!destroyEntitiesPacket)return@handler
                if (mc.thePlayer != null && Arrays.stream(packet.entityIDs)
                        .anyMatch { i -> i == mc.thePlayer.entityId }
                ) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid destroy entities packet.")
                }
            }
            is S03PacketTimeUpdate -> {
                if(!timePacket)return@handler
                if (isNotValid(packet.totalWorldTime, Long.MAX_VALUE - 1) || isNotValid(packet.worldTime, 100000)) {
                    event.cancelEvent()
                    debugMessage(EnumChatFormatting.DARK_RED.toString() + "[ClientFixes]" + EnumChatFormatting.YELLOW + " Invalid time update packet.")
                }
            }

        }
    }
    fun isNotValid(va: Double, mx: Double): Boolean{
        return va > mx || va < -mx
    }
    fun isNotValid(va: Double, mx: Int): Boolean{
        return va > mx || va < -mx
    }
    fun isNotValid(va: Int, mx: Int): Boolean{
        return va > mx || va < -mx
    }
    fun isNotValid(va: Int, mx: Double): Boolean{
        return va > mx || va < -mx
    }
    fun isNotValid(va: Long, mx: Long): Boolean{
        return va > mx || va < -mx
    }
    fun isNotValidPosition(p: BlockPos): Boolean{
        return p.x > 30000000 || p.x < -30000000 || p.y > 30000000 || p.y < -30000000 || p.z > 30000000 || p.z < -30000000
    }
    private fun debugMessage(msg: String) {
        if (!debug) return
        chat(msg)
    }
}

package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S19PacketEntityStatus
import net.minecraft.util.EnumParticleTypes

object BlockNotify : Module("BlockNotify", Category.COMBAT) {
    private val checkMode by choices("CheckMode", arrayOf("Packet", "HurtTime"), "HurtTime")
    
    private val playSound by boolean("PlaySound", true)
    private val showVisual by boolean("ShowVisual", true)
    private val showChat by boolean("ShowChat", true)
    private val onlySelf by boolean("OnlySelf", true)
    
    private val soundMode by choices("SoundMode", arrayOf(
        "Anvil", "Exp", "LevelUp", "Pling", "Orb", "BlazeHit", "NoteHarp"
    ), "Anvil")
    
    private val visualMode by choices("VisualMode", arrayOf(
        "Redstone", "Smoke", "Heart", "EnchantmentTable", "FireworksSpark", "VillagerHappy", "MagicCrit"
    ), "Redstone")
    
    private val particleIntensity by int("ParticleIntensity", 10, 1..50)

    private var prevHurtTime = 0

    val onPacket = handler<PacketEvent> { event ->
        if (checkMode != "Packet") return@handler
        
        val packet = event.packet
        if (packet is S19PacketEntityStatus) {
            val entity = packet.getEntity(mc.theWorld)
            if (entity is EntityPlayer) {
                if (onlySelf && entity != mc.thePlayer) return@handler
                
                val opCode = packet.opCode.toInt() and 0xFF
                if (opCode == 2) {
                    val isBlocking = entity.isBlocking || 
                        (entity == mc.thePlayer && KillAura.blockStatus)
                    if (isBlocking) {
                        onBlockSuccess(entity)
                    }
                }
            }
        }
    }

    val onUpdate = handler<UpdateEvent> {
        if (checkMode != "HurtTime") return@handler
        
        val player = mc.thePlayer ?: return@handler
        
        val isBlocking = player.isBlocking || KillAura.blockStatus
        
        if (onlySelf) {
            if (player.hurtTime > 0 && prevHurtTime == 0 && isBlocking) {
                onBlockSuccess(player)
            }
            prevHurtTime = player.hurtTime
        } else {
            val world = mc.theWorld ?: return@handler
            for (entityPlayer in world.playerEntities) {
                if (entityPlayer.hurtTime > 0 && entityPlayer != player) {
                    if (entityPlayer.isBlocking) {
                        onBlockSuccess(entityPlayer)
                    }
                }
            }
        }
    }

    private fun onBlockSuccess(player: EntityPlayer) {
        if (playSound) {
            playBlockSound()
        }
        
        if (showVisual) {
            spawnParticles(player)
        }
        
        if (showChat) {
            chat("§a§l格挡成功")
        }
    }

    private fun playBlockSound() {
        val thePlayer = mc.thePlayer ?: return
        
        val soundName = when (soundMode) {
            "Anvil" -> "random.anvil_land"
            "Exp" -> "random.orb"
            "LevelUp" -> "random.levelup"
            "Pling" -> "note.pling"
            "Orb" -> "random.orb"
            "BlazeHit" -> "mob.blaze.hit"
            "NoteHarp" -> "note.harp"
            else -> "random.anvil_land"
        }
        
        thePlayer.playSound(soundName, 1.0f, 1.0f)
    }

    private fun spawnParticles(player: EntityPlayer) {
        val world = mc.theWorld ?: return
        
        val particleType = when (visualMode) {
            "Redstone" -> EnumParticleTypes.REDSTONE
            "Smoke" -> EnumParticleTypes.SMOKE_LARGE
            "Heart" -> EnumParticleTypes.HEART
            "EnchantmentTable" -> EnumParticleTypes.ENCHANTMENT_TABLE
            "FireworksSpark" -> EnumParticleTypes.FIREWORKS_SPARK
            "VillagerHappy" -> EnumParticleTypes.VILLAGER_HAPPY
            "MagicCrit" -> EnumParticleTypes.CRIT_MAGIC 
            else -> EnumParticleTypes.REDSTONE
        }
        
        val posX = player.posX
        val posY = player.posY + player.eyeHeight
        val posZ = player.posZ
        
        for (i in 0 until particleIntensity) {
            val offsetX = (Math.random() - 0.5) * 0.5
            val offsetY = (Math.random() - 0.5) * 0.5
            val offsetZ = (Math.random() - 0.5) * 0.5
            
            world.spawnParticle(
                particleType,
                posX + offsetX,
                posY + offsetY,
                posZ + offsetZ,
                0.0, 0.1, 0.0
            )
        }
    }
}

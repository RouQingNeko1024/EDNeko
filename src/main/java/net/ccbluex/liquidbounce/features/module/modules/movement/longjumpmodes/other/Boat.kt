/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump.boatBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump.boatMode
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.entity.item.EntityBoat
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK
import net.minecraft.network.play.client.C0APacketAnimation

object Boat : LongJumpMode("Boat") {

    private var attacked = false
    private var boatBroken = false
    private var targetBoat: EntityBoat? = null

    override fun onEnable() {
        attacked = false
        boatBroken = false
        targetBoat = null
    }

    override fun onDisable() {
        attacked = false
        boatBroken = false
        targetBoat = null
    }

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (!boatBroken) {
            val riding = player.ridingEntity

            if (riding is EntityBoat && !riding.isDead) {
                targetBoat = riding
            } else if (targetBoat == null || targetBoat!!.isDead) {
                targetBoat = mc.theWorld.loadedEntityList
                    .filterIsInstance<EntityBoat>()
                    .filter { !it.isDead && player.getDistanceToEntity(it) <= 6f }
                    .minByOrNull { player.getDistanceToEntity(it) }
            }

            val boat = targetBoat

            if (boat == null || boat.isDead) {
                boatBroken = true
            } else if (boatMode == "PacketAttack" && !attacked) {
                repeat(4) {
                    sendPackets(
                        C0APacketAnimation(),
                        C02PacketUseEntity(boat, ATTACK)
                    )
                }
                attacked = true
            }

            if (boatBroken && player.isMoving) {
                player.motionY = 0.42
                strafe(boatBoost)
                LongJump.jumped = true
            }
            return
        }

        if (player.onGround || player.capabilities.isFlying) {
            LongJump.jumped = false
            player.motionX = 0.0
            player.motionZ = 0.0
            return
        }

        if (player.isMoving) {
            strafe(boatBoost)
        }
    }
}
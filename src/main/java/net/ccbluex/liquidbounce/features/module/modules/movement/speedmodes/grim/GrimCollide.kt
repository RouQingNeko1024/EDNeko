package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.grim

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.boostSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import kotlin.math.cos
import kotlin.math.sin

object GrimCollide : SpeedMode("GrimCollide") {

    override fun onUpdate() {
        val player = mc.thePlayer
        val world = mc.theWorld

        // 没有移动就不触发
        if (player.moveForward == 0f && player.moveStrafing == 0f) {
            return
        }

        var collisions = 0
        val box = player.entityBoundingBox.expand(1.0, 0.0, 1.0)

        for (entity in world.loadedEntityList) {
            if (entity is Entity && canCauseSpeed(entity)) {
                if (box.intersectsWith(entity.entityBoundingBox)) {
                    collisions++
                }
            }
        }

        if (collisions > 0) {
            val yaw = Math.toRadians(player.rotationYaw.toDouble())
            val boost = boostSpeed * collisions
            player.motionX -= sin(yaw) * boost
            player.motionZ += cos(yaw) * boost
        }
    }

    private fun canCauseSpeed(entity: Entity) =
        entity != mc.thePlayer && entity is EntityLivingBase && entity !is EntityArmorStand
}
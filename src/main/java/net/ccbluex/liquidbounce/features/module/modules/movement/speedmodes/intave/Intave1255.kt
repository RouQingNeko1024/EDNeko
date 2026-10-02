package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.hasDamaged
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.intave1255Ticks
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.stage
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.getBaseMoveSpeed

object Intave1255 : SpeedMode("Intave12.5.5") {


    override fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.PRE) {
            if (stage < 3) {
                mc.thePlayer.stopXZ()
                event.onGround = false
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                    stage++
                    return
                }
            } else {
                if (mc.thePlayer.hurtTime > 0 && hasDamaged) {
                    hasDamaged = true
                }

                if (hasDamaged) {
                    intave1255Ticks++
                    if (mc.thePlayer.onGround) {
                        MovementUtils.setSpeed(
                            getBaseMoveSpeed() * 3.2,
                            movingCheck = false
                        )
                    } else {
                        mc.thePlayer.stopXZ()
                    }

                    if (intave1255Ticks > 16) {
                        mc.thePlayer.stopXZ()
                        Speed.state = false
                    }

                    return
                }
            }

            return
        }
    }
}
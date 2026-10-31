package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.loftily

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object MatrixLongJump : LongJumpMode("Matrix") {

    val boostSpeed by float("Matrix-BoostSpeed", 1.97f, -3f..3f)
    val noGround by boolean("Matrix-NoGround", false)

    private var receivedFlag = false
    private var canBoost = false
    private var boosted = false

    override fun onDisable() {
        receivedFlag = false
        canBoost = false
    }

    override fun onEnable() {
        if (!mc.thePlayer.onGround && !boosted) {
            canBoost = true
        }
        boosted = false
    }

    override fun onMotion(event: MotionEvent) {
        if (noGround) {
            event.onGround = false
        }
    }

    override fun onUpdate() {
        if (mc.thePlayer.onGround) {
            mc.thePlayer.tryJump()
            canBoost = true
            return
        }
        if (canBoost) {
            MovementUtils.setSpeed(boostSpeed.toDouble(), false)
            mc.thePlayer.motionY = 0.42
            boosted = true
        }
        if (receivedFlag && boosted) {
            LongJump.state = false
            if (!LongJump.autoDisable) {
                onDisable()
            }
        }
    }

    override fun onPacketReceive(event: PacketEvent) {
        if (event.packet is S08PacketPlayerPosLook) {
            receivedFlag = true
        }
    }
}
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.loftily

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object MatrixNewLongJump : LongJumpMode("MatrixNew") {

    val motionMode by choices("MotionMode", arrayOf("Stable", "Last"), "Stable")
    val modeValue by choices("BypassMethod", arrayOf("Fall", "NoGround"), "Fall")
    val boostSpeed by float("MatrixNew-BoostSpeed", 2.1f, -3f..8f)
    val test by boolean("Test", false)

    private var x = 0.0
    private var y = 0.0
    private var z = 0.0
    private var receivedFlag = false
    private var canBoost = false
    private var boosted = false
    private var touchGround = false

    override fun onEnable() {
        boosted = false
        canBoost = false
        receivedFlag = false
        touchGround = false
        if (modeValue == "NoGround" && mc.thePlayer.onGround) {
            mc.thePlayer.tryJump()
            touchGround = true
        }
    }

    override fun onPacketReceive(event: PacketEvent) {
        if (event.packet is S08PacketPlayerPosLook) {
            receivedFlag = true
            val s08 = event.packet as S08PacketPlayerPosLook
            if (motionMode == "Last") {
                canBoost = false
                x = mc.thePlayer.motionX
                y = mc.thePlayer.motionY
                z = mc.thePlayer.motionZ
            }
            if (test) {
                // FlagHandler.flagSilentMotion
            }
        }
    }

    override fun onPacketSend(event: PacketEvent) {
        if (event.packet is C03PacketPlayer.C06PacketPlayerPosLook && motionMode == "Last" && receivedFlag) {
            mc.thePlayer.motionX = x
            mc.thePlayer.motionY = y
            mc.thePlayer.motionZ = z
            LongJump.state = false
            canBoost = false
            receivedFlag = false
        }
    }

    override fun onMotion(event: MotionEvent) {
        if (modeValue == "NoGround" && !canBoost) {
            event.onGround = false
        }
    }

    override fun onUpdate() {
        if (!mc.thePlayer.onGround && touchGround) {
            touchGround = false
        }
        if (mc.thePlayer.onGround && !touchGround) {
            mc.thePlayer.tryJump()
            boosted = false
            if (modeValue == "NoGround" && !boosted) {
                canBoost = true
            }
        }
        if (mc.thePlayer.fallDistance >= 0.25f && !boosted && modeValue == "Fall") {
            canBoost = true
        }
        if (canBoost) {
            MovementUtils.setSpeed(boostSpeed.toDouble(), false)
            mc.thePlayer.motionY = 0.42
            boosted = true
        }
        if (receivedFlag && boosted) {
            LongJump.state = false
            canBoost = false
            receivedFlag = false
        }
    }
}
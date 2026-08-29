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
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import kotlin.math.cos
import kotlin.math.sin

object RiseFlight : Module("RiseFlight", Category.RISE, forcedDescription = "Rise - Flight with multiple bypass modes") {

    private val mode by choices(
        "Mode", arrayOf(
            "Vanilla", "Matrix", "MatrixHighJump", "MatrixDamage", "Grim191181", "Grim2", "Grim3",
            "WatchdogPrediction", "Watchdog2", "MiniBlox", "Vulcan2", "AirWalk", "OldNCP",
            "Funcraft", "SkyCave", "LatestNCP", "Verus", "Block", "MMC", "BufferAbuse",
            "ZoneCraft", "SlimeNCP", "AirJump", "CubeCraft", "MineLand", "VulcanDeprecated",
            "Bloxd", "VerusDamageNew", "Watchdog", "DamageDeprecated", "MMCFireball",
            "VulcanDamage"
        ), "Vanilla"
    )
    private val disableOnTeleport by boolean("DisableOnTeleport", false)
    private val viewBobbing by boolean("ViewBobbing", false)
    private val fakeDamage by boolean("FakeDamage", false)
    private val smoothCamera by boolean("SmoothCamera", false)
    private val visualDragon by boolean("VisualDragon", false)
    private val speed by float("Speed", 2f, 0.1f..10f)
    private val verticalSpeed by float("VerticalSpeed", 1f, 0.1f..10f)
    private val timer by float("Timer", 1f, 0.1f..5f)
    private val motionY by float("MotionY", 0.42f, 0.1f..2f)
    private val motionYOnDamage by float("MotionYOnDamage", 3.5f, 0.1f..10f)
    private val clip by boolean("Clip", false)
    private val offset by float("Offset", 0f, 0f..3f)
    private val counter by int("Counter", 12, 1..50)
    private val boostDelay by int("BoostDelay", 12, 1..50)

    private var teleported = false
    private var ticks = 0
    private var damaged = false
    private val fakeDamageTimer = MSTimer()
    private var lastY = 0.0
    private var wasOnGround = false

    override fun onEnable() {
        teleported = false
        ticks = 0
        damaged = false
        lastY = mc.thePlayer?.posY ?: 0.0
        wasOnGround = mc.thePlayer?.onGround ?: false
        if (mode == "VulcanDamage" || mode == "MatrixDamage" || mode == "VerusDamageNew" || mode == "MMCFireball" || mode == "DamageDeprecated") {
            mc.thePlayer?.jump()
            fakeDamageTimer.reset()
        }
        if (clip) {
            mc.thePlayer?.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + offset, mc.thePlayer.posZ)
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        mc.thePlayer?.capabilities?.isFlying = false
        mc.thePlayer?.capabilities?.flySpeed = 0.05f
        damaged = false
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        ticks++

        when (mode) {
            "Vanilla" -> {
                thePlayer.capabilities.isFlying = true
                thePlayer.capabilities.flySpeed = speed
                thePlayer.motionY = 0.0
                if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY = verticalSpeed.toDouble()
                if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -verticalSpeed.toDouble()
            }
            "AirWalk" -> {
                thePlayer.motionY = 0.0
                if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY = verticalSpeed.toDouble()
                if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -verticalSpeed.toDouble()
                MovementUtils.strafe(speed)
            }
            "OldNCP" -> {
                thePlayer.motionY = 0.0
                if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY = 0.5
                if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -0.5
                MovementUtils.strafe(speed)
            }
            "Verus" -> {
                thePlayer.motionY = 0.0
                if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY = motionY.toDouble()
                if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -motionY.toDouble()
                MovementUtils.strafe(speed)
            }
            "Matrix" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                    MovementUtils.strafe(speed)
                } else {
                    thePlayer.motionY = -0.01
                    if (thePlayer.motionY < -0.3) thePlayer.motionY = -0.3
                    MovementUtils.strafe(speed)
                }
            }
            "MatrixHighJump" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble() * 2
                    MovementUtils.strafe(speed)
                } else {
                    thePlayer.motionY = -0.01
                    MovementUtils.strafe(speed)
                }
            }
            "MatrixDamage" -> {
                if (!damaged && fakeDamageTimer.hasTimePassed(500)) {
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 3, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
                    damaged = true
                }
                if (thePlayer.hurtTime > 0) {
                    thePlayer.motionY = motionYOnDamage.toDouble()
                    damaged = true
                }
                if (damaged) {
                    if (thePlayer.onGround) {
                        thePlayer.motionY = motionY.toDouble()
                    }
                    MovementUtils.strafe(speed)
                    thePlayer.motionY -= 0.01
                }
            }
            "Grim191181", "Grim2" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                }
                MovementUtils.strafe(speed)
                mc.timer.timerSpeed = timer
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.0784
                }
            }
            "Grim3" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                }
                MovementUtils.strafe(speed)
                if (thePlayer.fallDistance > 0.5f) {
                    thePlayer.motionY = -0.0784
                }
            }
            "WatchdogPrediction", "Watchdog2" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                }
                MovementUtils.strafe(speed)
                if (thePlayer.fallDistance > 1.5f) {
                    thePlayer.motionY = -0.0784
                }
            }
            "MiniBlox" -> {
                thePlayer.motionY = 0.0
                if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY = motionY.toDouble()
                MovementUtils.strafe(speed)
                mc.timer.timerSpeed = timer
            }
            "Vulcan2" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                }
                MovementUtils.strafe(speed)
                if (ticks % 20 == 0) {
                    thePlayer.motionY = -0.0784
                }
            }
            "Funcraft" -> {
                thePlayer.motionY = 0.0
                if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY = motionY.toDouble()
                if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -motionY.toDouble()
                MovementUtils.strafe(speed)
                mc.timer.timerSpeed = 0.5f
            }
            "SkyCave" -> {
                thePlayer.motionY = 0.0
                if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY = motionY.toDouble()
                MovementUtils.strafe(speed)
                if (thePlayer.fallDistance > 0.5f) {
                    thePlayer.motionY = -0.1
                }
            }
            "LatestNCP" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = 0.42
                }
                MovementUtils.strafe(speed)
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY *= 0.98
                }
            }
            "Block" -> {
                thePlayer.motionY = 0.0
                if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY = motionY.toDouble()
                if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -motionY.toDouble()
                MovementUtils.strafe(speed)
            }
            "MMC" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                }
                MovementUtils.strafe(speed)
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.098
                }
            }
            "BufferAbuse" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                }
                MovementUtils.strafe(speed)
                if (ticks % 2 == 0) {
                    thePlayer.motionY = -0.0784
                }
            }
            "ZoneCraft" -> {
                thePlayer.motionY = 0.0
                if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY = motionY.toDouble()
                MovementUtils.strafe(speed)
                mc.timer.timerSpeed = 0.8f
            }
            "SlimeNCP" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                }
                MovementUtils.strafe(speed)
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY *= 0.98
                }
            }
            "AirJump" -> {
                if (mc.gameSettings.keyBindJump.isKeyDown) {
                    thePlayer.motionY = motionY.toDouble()
                }
                MovementUtils.strafe(speed)
                thePlayer.motionY -= 0.01
            }
            "CubeCraft" -> {
                thePlayer.motionY = 0.0
                if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY = motionY.toDouble()
                MovementUtils.strafe(speed)
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.0784
                }
            }
            "MineLand" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                }
                MovementUtils.strafe(speed)
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.098
                }
            }
            "VulcanDeprecated" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                }
                MovementUtils.strafe(speed)
                if (ticks % 3 == 0) {
                    thePlayer.motionY = -0.0784
                }
            }
            "Bloxd" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                }
                MovementUtils.strafe(speed)
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.1
                }
            }
            "VerusDamageNew" -> {
                if (!damaged && fakeDamageTimer.hasTimePassed(500)) {
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 3.1, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
                    damaged = true
                }
                if (damaged) {
                    if (thePlayer.onGround) {
                        thePlayer.motionY = motionY.toDouble()
                    }
                    MovementUtils.strafe(speed)
                    thePlayer.motionY -= 0.0098
                }
            }
            "Watchdog" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                }
                MovementUtils.strafe(speed)
                if (thePlayer.fallDistance > 1.5f) {
                    thePlayer.motionY = -0.08
                }
            }
            "DamageDeprecated" -> {
                if (!damaged && fakeDamageTimer.hasTimePassed(500)) {
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 3.01, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
                    damaged = true
                }
                if (damaged) {
                    if (thePlayer.onGround) {
                        thePlayer.motionY = motionY.toDouble()
                    }
                    MovementUtils.strafe(speed)
                    thePlayer.motionY -= 0.005
                }
            }
            "MMCFireball" -> {
                if (!damaged && fakeDamageTimer.hasTimePassed(300)) {
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 4.0, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
                    damaged = true
                }
                if (damaged && thePlayer.hurtTime > 0) {
                    thePlayer.motionY = motionYOnDamage.toDouble()
                }
                if (damaged) {
                    if (thePlayer.onGround) {
                        thePlayer.motionY = motionY.toDouble()
                    }
                    MovementUtils.strafe(speed)
                    thePlayer.motionY -= 0.01
                }
            }
            "VulcanDamage" -> {
                if (!damaged && fakeDamageTimer.hasTimePassed(500)) {
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 3.25, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
                    damaged = true
                }
                if (damaged && thePlayer.hurtTime > 0) {
                    thePlayer.motionY = motionYOnDamage.toDouble()
                }
                if (damaged) {
                    if (thePlayer.onGround) {
                        thePlayer.motionY = motionY.toDouble()
                    }
                    MovementUtils.strafe(speed)
                    thePlayer.motionY -= 0.008
                }
            }
        }

        if (viewBobbing) {
            thePlayer.cameraYaw = 0.1f
        }

        if (smoothCamera) {
            thePlayer.cameraYaw = thePlayer.cameraYaw * 0.8f
        }

        if (disableOnTeleport) {
            val dist = MathHelper.sqrt_double(
                (thePlayer.posX - thePlayer.prevPosX) * (thePlayer.posX - thePlayer.prevPosX) +
                (thePlayer.posZ - thePlayer.prevPosZ) * (thePlayer.posZ - thePlayer.prevPosZ)
            )
            if (dist > 5.0) {
                teleported = true
                state = false
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (visualDragon && event.packet is C03PacketPlayer) {
            val p = event.packet
            p.yaw = (mc.thePlayer?.rotationYaw ?: 0f) + (Math.random() * 360f).toFloat() - 180f
        }
    }
}

object RiseSpeed : Module("RiseSpeed", Category.RISE, forcedDescription = "Rise - Speed with multiple bypass modes") {

    private val mode by choices(
        "Mode", arrayOf(
            "Vanilla", "Strafe", "WatchdogPrediction", "Vulcan", "Watchdog", "NCP",
            "MiniBlox", "Verus", "Matrix", "BlocksMC", "MineMenClub", "Watchdog6Tick",
            "KoksCraft", "Legit", "Polar", "Tatako", "Grim", "Grim2", "OldNCPYPort"
        ), "Vanilla"
    )
    private val disableOnTeleport by boolean("DisableOnTeleport", false)
    private val stopOnDisable by boolean("StopOnDisable", false)
    private val speedValue by float("Speed", 1.6f, 0.1f..10f)
    private val timer by float("Timer", 1f, 0.1f..5f)
    private val hurtTime by boolean("HurtTime", false)
    private val lowHop by boolean("LowHop", false)
    private val groundSpoof by boolean("GroundSpoof", false)
    private val airStrafe by boolean("AirStrafe", false)
    private val smooth by boolean("Smooth", false)

    private var ticks = 0
    private var lastY = 0.0
    private var wasOnGround = false
    private var teleported = false

    override fun onEnable() {
        ticks = 0
        lastY = mc.thePlayer?.posY ?: 0.0
        wasOnGround = mc.thePlayer?.onGround ?: false
        teleported = false
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        if (stopOnDisable) {
            mc.thePlayer?.stop()
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (!thePlayer.isMoving && !airStrafe) return@handler
        ticks++

        when (mode) {
            "Vanilla" -> {
                if (thePlayer.isMoving) {
                    MovementUtils.strafe(speedValue)
                }
                mc.timer.timerSpeed = timer
            }
            "Strafe" -> {
                if (thePlayer.onGround) {
                    if (lowHop) {
                        thePlayer.motionY = 0.28
                    } else {
                        thePlayer.jump()
                    }
                    if (smooth && wasOnGround) {
                        thePlayer.motionX *= 0.6
                        thePlayer.motionZ *= 0.6
                    }
                }
                MovementUtils.strafe(MovementUtils.speed * speedValue)
                if (airStrafe && !thePlayer.onGround) {
                    MovementUtils.strafe(MovementUtils.speed * speedValue)
                }
            }
            "NCP" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    if (smooth && wasOnGround) {
                        thePlayer.motionX *= 0.65
                        thePlayer.motionZ *= 0.65
                    }
                }
                MovementUtils.strafe(MovementUtils.speed * speedValue)
            }
            "WatchdogPrediction", "Watchdog" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = 0.42
                }
                MovementUtils.strafe(MovementUtils.speed * speedValue)
                if (thePlayer.fallDistance > 0.5f && thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.0784
                }
            }
            "Watchdog6Tick" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = 0.42
                }
                if (ticks % 6 == 0) {
                    MovementUtils.strafe(MovementUtils.speed * speedValue)
                }
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.0784
                }
            }
            "Vulcan" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = 0.42
                }
                MovementUtils.strafe(MovementUtils.speed * speedValue)
                if (thePlayer.motionY < 0 && ticks % 3 == 0) {
                    thePlayer.motionY = -0.0784
                }
            }
            "MiniBlox" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = 0.42
                }
                MovementUtils.strafe(MovementUtils.speed * speedValue)
                mc.timer.timerSpeed = timer
            }
            "Verus" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = 0.42
                }
                MovementUtils.strafe(MovementUtils.speed * speedValue)
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY *= 0.98
                }
            }
            "Matrix" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = 0.42
                }
                MovementUtils.strafe(MovementUtils.speed * speedValue)
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.02
                }
            }
            "BlocksMC" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = 0.42
                }
                MovementUtils.strafe(MovementUtils.speed * speedValue)
                if (thePlayer.fallDistance > 1f) {
                    thePlayer.motionY = -0.0784
                }
            }
            "MineMenClub" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = 0.42
                }
                MovementUtils.strafe(MovementUtils.speed * speedValue)
                if (thePlayer.fallDistance > 0.5f) {
                    thePlayer.motionY = -0.098
                }
            }
            "KoksCraft" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = 0.42
                }
                MovementUtils.strafe(MovementUtils.speed * speedValue)
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.098
                }
            }
            "Legit" -> {
                if (thePlayer.onGround && thePlayer.isMoving) {
                    thePlayer.jump()
                }
            }
            "Polar" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = 0.42
                }
                MovementUtils.strafe(MovementUtils.speed * speedValue)
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.0784
                }
            }
            "Tatako" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = 0.42
                }
                MovementUtils.strafe(MovementUtils.speed * speedValue)
                if (ticks % 2 == 0 && thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.0784
                }
            }
            "Grim", "Grim2" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = 0.42
                }
                MovementUtils.strafe(MovementUtils.speed * speedValue)
                mc.timer.timerSpeed = timer
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.0784
                }
            }
            "OldNCPYPort" -> {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionY = 0.42
                }
                MovementUtils.strafe(MovementUtils.speed * speedValue)
                if (thePlayer.motionY < 0 && ticks % 2 == 0) {
                    thePlayer.motionY = -0.098
                }
            }
        }

        if (hurtTime && thePlayer.hurtTime > 0) {
            MovementUtils.strafe(MovementUtils.speed * speedValue)
        }

        if (groundSpoof && !thePlayer.onGround) {
            sendPacket(C03PacketPlayer(true))
        }

        wasOnGround = thePlayer.onGround
        lastY = thePlayer.posY

        if (disableOnTeleport) {
            val dist = MathHelper.sqrt_double(
                (thePlayer.posX - thePlayer.prevPosX) * (thePlayer.posX - thePlayer.prevPosX) +
                (thePlayer.posZ - thePlayer.prevPosZ) * (thePlayer.posZ - thePlayer.prevPosZ)
            )
            if (dist > 5.0) {
                teleported = true
                state = false
            }
        }
    }
}

object RiseLongJump : Module("RiseLongJump", Category.RISE, forcedDescription = "Rise - Long jump with multiple bypass modes") {

    private val mode by choices(
        "Mode", arrayOf(
            "Vanilla", "Matrix2", "NCP", "Vulcan", "WatchdogWater118", "Watchdog2",
            "FireBall", "Watchdog", "WatchdogGlide", "WatchdogFireBall", "WatchdogFireBall2",
            "Grim", "Bloxd", "Vulcan2"
        ), "Vanilla"
    )
    private val autoDisable by boolean("AutoDisable", true)
    private val fakeDamage by boolean("FakeDamage", false)
    private val boost by float("Boost", 2f, 1f..10f)
    private val timer by float("Timer", 1f, 0.1f..5f)
    private val motionY by float("MotionY", 0.42f, 0.1f..2f)

    private var inAir = false
    private var ticks = 0
    private var damaged = false
    private val fakeDamageTimer = MSTimer()

    override fun onEnable() {
        inAir = false
        ticks = 0
        damaged = false
        if (fakeDamage) {
            mc.thePlayer?.jump()
            fakeDamageTimer.reset()
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        inAir = false
        damaged = false
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        ticks++

        if (autoDisable && inAir && thePlayer.onGround && mode != "Watchdog" && mode != "Watchdog2" && mode != "WatchdogWater118") {
            state = false
        }

        inAir = !thePlayer.onGround

        when (mode) {
            "Vanilla" -> {
                if (thePlayer.onGround && thePlayer.isMoving) {
                    thePlayer.jump()
                    MovementUtils.strafe(boost)
                }
            }
            "NCP" -> {
                if (thePlayer.onGround && thePlayer.isMoving) {
                    thePlayer.jump()
                    MovementUtils.strafe(boost)
                } else if (thePlayer.isMoving) {
                    MovementUtils.strafe(MovementUtils.speed * boost)
                }
            }
            "Matrix2" -> {
                if (thePlayer.onGround && thePlayer.isMoving) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                    MovementUtils.strafe(boost)
                } else {
                    thePlayer.motionY = -0.01
                    MovementUtils.strafe(boost)
                }
            }
            "Vulcan" -> {
                if (thePlayer.onGround && thePlayer.isMoving) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                    MovementUtils.strafe(boost)
                } else if (thePlayer.isMoving) {
                    MovementUtils.strafe(MovementUtils.speed * boost)
                    if (ticks % 3 == 0) {
                        thePlayer.motionY = -0.0784
                    }
                }
            }
            "WatchdogWater118" -> {
                if (thePlayer.isInWater) {
                    thePlayer.motionY = 0.42
                    MovementUtils.strafe(boost)
                }
                if (thePlayer.onGround && thePlayer.isMoving) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                    MovementUtils.strafe(boost)
                }
                if (thePlayer.fallDistance > 1.5f) {
                    thePlayer.motionY = -0.0784
                    MovementUtils.strafe(boost)
                }
            }
            "Watchdog2" -> {
                if (thePlayer.onGround && thePlayer.isMoving) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                    MovementUtils.strafe(boost)
                }
                if (thePlayer.fallDistance > 1.5f) {
                    thePlayer.motionY = -0.0784
                    MovementUtils.strafe(boost)
                }
            }
            "FireBall" -> {
                if (!damaged && fakeDamageTimer.hasTimePassed(500)) {
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 3.1, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
                    damaged = true
                }
                if (damaged && thePlayer.hurtTime > 0) {
                    thePlayer.motionY = motionY.toDouble() * 2
                    MovementUtils.strafe(boost)
                }
                if (damaged && thePlayer.isMoving) {
                    MovementUtils.strafe(boost)
                }
            }
            "Watchdog" -> {
                if (thePlayer.onGround && thePlayer.isMoving) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                    MovementUtils.strafe(boost)
                }
                if (thePlayer.fallDistance > 1.5f) {
                    thePlayer.motionY = -0.0784
                }
            }
            "WatchdogGlide" -> {
                if (thePlayer.onGround && thePlayer.isMoving) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                    MovementUtils.strafe(boost)
                }
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.0784
                    MovementUtils.strafe(boost)
                }
            }
            "WatchdogFireBall" -> {
                if (!damaged && fakeDamageTimer.hasTimePassed(500)) {
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 3.1, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
                    damaged = true
                }
                if (damaged && thePlayer.hurtTime > 0) {
                    thePlayer.motionY = motionY.toDouble() * 2.5
                    MovementUtils.strafe(boost)
                }
                if (damaged) {
                    MovementUtils.strafe(boost)
                }
            }
            "WatchdogFireBall2" -> {
                if (!damaged && fakeDamageTimer.hasTimePassed(500)) {
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 4.0, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
                    damaged = true
                }
                if (damaged && thePlayer.hurtTime > 0) {
                    thePlayer.motionY = motionY.toDouble() * 3
                    MovementUtils.strafe(boost * 1.5f)
                }
                if (damaged) {
                    MovementUtils.strafe(boost)
                }
            }
            "Grim" -> {
                if (thePlayer.onGround && thePlayer.isMoving) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                    MovementUtils.strafe(boost)
                }
                mc.timer.timerSpeed = timer
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.0784
                }
            }
            "Bloxd" -> {
                if (thePlayer.onGround && thePlayer.isMoving) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                    MovementUtils.strafe(boost)
                }
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY = -0.1
                }
            }
            "Vulcan2" -> {
                if (thePlayer.onGround && thePlayer.isMoving) {
                    thePlayer.jump()
                    thePlayer.motionY = motionY.toDouble()
                    MovementUtils.strafe(boost)
                } else if (thePlayer.isMoving) {
                    MovementUtils.strafe(MovementUtils.speed * boost)
                    if (ticks % 5 == 0) {
                        thePlayer.motionY = -0.0784
                    }
                }
            }
        }
    }
}

object RiseNoSlow : Module("RiseNoSlow", Category.RISE, forcedDescription = "Rise - No slow with multiple bypass modes") {

    private val mode by choices(
        "Mode", arrayOf(
            "Vanilla", "NCP", "NewNCP", "Intave", "Legit", "WatchdogPrediction",
            "Variable", "Prediction", "Watchdog", "Grim19", "Grim", "Grim30", "Matrix"
        ), "Vanilla"
    )
    private val food by boolean("Food", false)
    private val potion by boolean("Potion", false)
    private val sword by boolean("Sword", false)
    private val bow by boolean("Bow", false)
    private val forward by float("Forward", 1f, 0f..2f)
    private val strafe by float("Strafe", 1f, 0f..2f)
    private val sprint by boolean("Sprint", true)
    private val onlyMove by boolean("OnlyMove", false)
    private val grimKeepSprint by boolean("GrimKeepSprint", false)

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (!thePlayer.isUsingItem) return@handler

        val item = thePlayer.heldItem?.item
        if (item is ItemFood && !food) return@handler
        if (item is ItemPotion && !potion) return@handler
        if (item is ItemSword && !sword) return@handler
        if (item is ItemBow && !bow) return@handler

        if (onlyMove && !thePlayer.isMoving) return@handler

        when (mode) {
            "Vanilla" -> {
                if (thePlayer.isMoving) {
                    MovementUtils.strafe(MovementUtils.speed * forward)
                    thePlayer.motionX *= strafe.toDouble()
                    thePlayer.motionZ *= strafe.toDouble()
                }
            }
            "NCP" -> {
                if (thePlayer.isMoving) {
                    MovementUtils.strafe(MovementUtils.speed * forward)
                    thePlayer.motionX *= strafe.toDouble()
                    thePlayer.motionZ *= strafe.toDouble()
                }
                if (thePlayer.onGround && thePlayer.isMoving) {
                    val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
                    thePlayer.motionX -= sin(yaw) * 0.2
                    thePlayer.motionZ += cos(yaw) * 0.2
                }
            }
            "NewNCP" -> {
                if (thePlayer.isMoving) {
                    MovementUtils.strafe(MovementUtils.speed * forward)
                }
            }
            "Intave" -> {
                if (thePlayer.isMoving && thePlayer.onGround) {
                    thePlayer.jump()
                    MovementUtils.strafe(MovementUtils.speed * forward)
                }
            }
            "Legit" -> {
                if (thePlayer.isMoving) {
                    thePlayer.motionX *= forward.toDouble()
                    thePlayer.motionZ *= forward.toDouble()
                }
            }
            "WatchdogPrediction", "Watchdog" -> {
                if (thePlayer.isMoving) {
                    MovementUtils.strafe(MovementUtils.speed * forward)
                }
            }
            "Variable" -> {
                if (thePlayer.isMoving) {
                    val factor = if (thePlayer.onGround) forward else forward * 0.8f
                    MovementUtils.strafe(MovementUtils.speed * factor)
                }
            }
            "Prediction" -> {
                if (thePlayer.isMoving) {
                    MovementUtils.strafe(MovementUtils.speed * forward)
                }
            }
            "Grim19", "Grim", "Grim30" -> {
                if (thePlayer.isMoving) {
                    MovementUtils.strafe(MovementUtils.speed * forward)
                }
                if (sprint) {
                    thePlayer.setSprinting(true)
                }
            }
            "Matrix" -> {
                if (thePlayer.isMoving) {
                    MovementUtils.strafe(MovementUtils.speed * forward)
                    thePlayer.motionX *= strafe.toDouble()
                    thePlayer.motionZ *= strafe.toDouble()
                }
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (mode == "Grim" || mode == "Grim19" || mode == "Grim30") {
            if (event.packet is C0BPacketEntityAction) {
                val p = event.packet
                if (p.action == C0BPacketEntityAction.Action.STOP_SPRINTING && grimKeepSprint) {
                    event.cancelEvent()
                }
            }
        }
    }
}

object RiseStep : Module("RiseStep", Category.RISE, forcedDescription = "Rise - Step with multiple bypass modes") {

    private val mode by choices(
        "Mode", arrayOf("Vanilla", "NCP", "NewNCP", "NCPPacketless", "Vulcan", "Matrix", "Jump", "Watchdog"), "Vanilla"
    )
    private val height by float("Height", 1f, 0.5f..10f)
    private val timer by float("Timer", 1f, 0.1f..5f)
    private val delay by int("Delay", 0, 0..500)
    private val offGroundTicks by boolean("OffGroundTicks", false)

    private var ticks = 0
    private var wasOnGround = false
    private val timerUtil = MSTimer()
    private var stepped = false

    override fun onEnable() {
        mc.thePlayer?.stepHeight = height
        ticks = 0
        stepped = false
    }

    override fun onDisable() {
        mc.thePlayer?.stepHeight = 0.6f
        mc.timer.timerSpeed = 1f
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        ticks++

        when (mode) {
            "Vanilla" -> {
                thePlayer.stepHeight = height
            }
            "NCP" -> {
                thePlayer.stepHeight = height
                if (thePlayer.onGround && !wasOnGround && timerUtil.hasTimePassed(delay.toLong())) {
                    thePlayer.stepHeight = 0.6f
                    timerUtil.reset()
                }
                if (wasOnGround && !thePlayer.onGround) {
                    thePlayer.stepHeight = height
                }
            }
            "NewNCP" -> {
                thePlayer.stepHeight = height
                if (thePlayer.onGround) {
                    thePlayer.motionY = 0.42
                    thePlayer.stepHeight = 0.6f
                }
            }
            "NCPPacketless" -> {
                thePlayer.stepHeight = height
                if (thePlayer.onGround) {
                    thePlayer.stepHeight = 0.6f
                }
            }
            "Vulcan" -> {
                thePlayer.stepHeight = height
                if (thePlayer.onGround && !wasOnGround) {
                    thePlayer.stepHeight = 0.6f
                    thePlayer.motionY = 0.42
                }
            }
            "Matrix" -> {
                thePlayer.stepHeight = height
                if (thePlayer.onGround && !wasOnGround) {
                    thePlayer.stepHeight = 0.6f
                    thePlayer.motionY = 0.0
                }
            }
            "Jump" -> {
                if (thePlayer.onGround && thePlayer.isCollidedHorizontally) {
                    thePlayer.jump()
                    thePlayer.motionY = height.toDouble() * 0.42
                    stepped = true
                }
                if (stepped && thePlayer.onGround) {
                    stepped = false
                }
            }
            "Watchdog" -> {
                thePlayer.stepHeight = height
                if (thePlayer.onGround && !wasOnGround) {
                    thePlayer.stepHeight = 0.6f
                    thePlayer.motionY = -0.0784
                }
            }
        }

        if (offGroundTicks && !thePlayer.onGround && timer > 1f) {
            mc.timer.timerSpeed = timer
        } else {
            mc.timer.timerSpeed = 1f
        }

        wasOnGround = thePlayer.onGround
    }
}

object RiseStrafe : Module("RiseStrafe", Category.RISE, forcedDescription = "Rise - RISE strafe modifier") {

    private val strength by float("Strength", 100f, 1f..100f)
    private val hypixelFlyDisabler by boolean("HypixelFlyDisabler", false)
    private val onlyOnGround by boolean("OnlyOnGround", false)
    private val airStrafe by boolean("AirStrafe", false)

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (!thePlayer.isMoving) return@handler
        if (onlyOnGround && !thePlayer.onGround) return@handler

        if (thePlayer.onGround || airStrafe) {
            MovementUtils.strafe(MovementUtils.speed * strength / 100f)
        }

        if (hypixelFlyDisabler && thePlayer.onGround) {
            thePlayer.motionY = 0.42
        }
    }
}

object RiseTargetStrafe : Module("RiseTargetStrafe", Category.RISE, forcedDescription = "Rise - Strafe around target") {

    private val range by float("Range", 2.5f, 0.5f..6f)
    private val behind by boolean("Behind", false)
    private val circle by boolean("Circle", true)
    private val glow by boolean("Glow", false)
    private val dots by int("Dots", 5, 1..30)
    private val thickness by float("Thickness", 2f, 1f..5f)
    private val autoThirdPersonCamera by boolean("AutoThirdPersonCamera", false)
    private val speed by float("Speed", 1f, 0.1f..5f)
    private val onlyInCombat by boolean("OnlyInCombat", false)

    private var target: EntityLivingBase? = null
    private var left = false
    private var active = false
    private var forcedThirdPerson = false
    private var direction = 1

    override fun onEnable() {
        forcedThirdPerson = false
        direction = if (Math.random() > 0.5) 1 else -1
    }

    override fun onDisable() {
        if (forcedThirdPerson && autoThirdPersonCamera) {
            mc.gameSettings.thirdPersonView = 0
            forcedThirdPerson = false
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val theWorld = mc.theWorld ?: return@handler

        if (onlyInCombat && mc.objectMouseOver?.entityHit !is EntityLivingBase) {
            active = false
            return@handler
        }

        target = theWorld.playerEntities
            .filter { it != thePlayer && it.health > 0 }
            .minByOrNull { thePlayer.getDistanceToEntity(it) }

        val t = target ?: return@handler
        val dist = thePlayer.getDistanceToEntity(t)

        if (dist > range * 2) {
            active = false
            return@handler
        }

        active = true

        if (autoThirdPersonCamera && !forcedThirdPerson) {
            mc.gameSettings.thirdPersonView = 1
            forcedThirdPerson = true
        }

        if (!thePlayer.isMoving) return@handler

        if (circle) {
            val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
            val targetYaw = MathHelper.wrapAngleTo180_float(
                Math.toDegrees(Math.atan2(t.posZ - thePlayer.posZ, t.posX - thePlayer.posX)).toFloat() - 90f
            )

            if (behind) {
                val behindYaw = Math.toRadians((targetYaw + 180f).toDouble())
                thePlayer.motionX = -sin(behindYaw) * speed * MovementUtils.speed
                thePlayer.motionZ = cos(behindYaw) * speed * MovementUtils.speed
            } else {
                if (dist < range) {
                    left = true
                } else if (dist > range + 1) {
                    left = false
                }

                if (left) {
                    thePlayer.motionX = -sin(yaw + direction * Math.PI / 2) * speed * MovementUtils.speed
                    thePlayer.motionZ = cos(yaw + direction * Math.PI / 2) * speed * MovementUtils.speed
                } else {
                    val moveYaw = Math.atan2(t.posZ - thePlayer.posZ, t.posX - thePlayer.posX)
                    thePlayer.motionX = -sin(moveYaw) * speed * MovementUtils.speed * 0.5
                    thePlayer.motionZ = cos(moveYaw) * speed * MovementUtils.speed * 0.5
                }
            }
        }
    }
}

object RiseInventoryMove : Module("RiseInventoryMove", Category.RISE, forcedDescription = "Rise - Move while inventory is open") {

    private val bypassMode by choices(
        "BypassMode", arrayOf("Normal", "BufferAbuse", "Cancel", "Grim", "Grim2", "Watchdog"), "Normal"
    )
    private val noSlow by boolean("NoSlow", true)
    private val jump by boolean("Jump", false)

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (mc.currentScreen == null) return@handler

        when (bypassMode) {
            "Normal" -> {
                if (mc.gameSettings.keyBindForward.isKeyDown) {
                    MovementUtils.strafe(MovementUtils.speed)
                }
                if (jump && mc.gameSettings.keyBindJump.isKeyDown) {
                    thePlayer.jump()
                }
            }
            "BufferAbuse" -> {
                if (mc.gameSettings.keyBindForward.isKeyDown) {
                    MovementUtils.strafe(MovementUtils.speed)
                }
                if (mc.gameSettings.keyBindJump.isKeyDown && thePlayer.onGround) {
                    thePlayer.jump()
                }
            }
            "Cancel" -> {
                if (mc.gameSettings.keyBindForward.isKeyDown) {
                    MovementUtils.strafe(MovementUtils.speed)
                }
            }
            "Grim", "Grim2", "Watchdog" -> {
                if (mc.gameSettings.keyBindForward.isKeyDown) {
                    MovementUtils.strafe(MovementUtils.speed)
                }
                if (mc.gameSettings.keyBindJump.isKeyDown && thePlayer.onGround) {
                    thePlayer.jump()
                }
            }
        }
    }

    fun isGrimBypass(): Boolean = bypassMode.equals("Grim", ignoreCase = true)
}

object RiseNoClip : Module("RiseNoClip", Category.RISE, forcedDescription = "Rise - Clip through blocks") {

    private val speed by float("Speed", 1f, 0.1f..5f)

    override fun onEnable() {
        mc.thePlayer?.noClip = true
    }

    override fun onDisable() {
        mc.thePlayer?.noClip = false
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        thePlayer.noClip = true
        thePlayer.motionY = 0.0
        if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY = speed.toDouble()
        if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -speed.toDouble()
        if (thePlayer.isMoving) {
            MovementUtils.strafe(speed)
        }
    }
}

object RisePhase : Module("RisePhase", Category.RISE, forcedDescription = "Rise - Phase through blocks") {

    private val mode by choices("Mode", arrayOf("Vanilla", "NCP", "AAC", "Smart"), "Vanilla")
    private val speed by float("Speed", 0.5f, 0.1f..5f)
    private val clip by boolean("Clip", false)
    private val offset by float("Offset", 0f, -3f..3f)

    private var ticks = 0

    override fun onEnable() {
        ticks = 0
        if (clip) {
            mc.thePlayer?.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + offset, mc.thePlayer.posZ)
        }
    }

    override fun onDisable() {
        mc.thePlayer?.noClip = false
        mc.timer.timerSpeed = 1f
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        ticks++

        when (mode) {
            "Vanilla" -> {
                thePlayer.noClip = true
                if (thePlayer.isMoving) {
                    MovementUtils.strafe(speed)
                }
                thePlayer.motionY = 0.0
                if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY = speed.toDouble()
                if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -speed.toDouble()
            }
            "NCP" -> {
                if (thePlayer.isMoving && thePlayer.isCollidedHorizontally) {
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY - 0.0625, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, false))
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX + thePlayer.motionX * 3, thePlayer.posY, thePlayer.posZ + thePlayer.motionZ * 3, false))
                    thePlayer.setPosition(thePlayer.posX + thePlayer.motionX * 3, thePlayer.posY, thePlayer.posZ + thePlayer.motionZ * 3)
                }
            }
            "AAC" -> {
                if (thePlayer.isMoving && thePlayer.isCollidedHorizontally) {
                    thePlayer.setPosition(thePlayer.posX + thePlayer.motionX * 0.3, thePlayer.posY, thePlayer.posZ + thePlayer.motionZ * 0.3)
                }
            }
            "Smart" -> {
                if (thePlayer.isCollidedHorizontally) {
                    val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
                    thePlayer.setPosition(
                        thePlayer.posX - sin(yaw) * speed * 0.5,
                        thePlayer.posY,
                        thePlayer.posZ + cos(yaw) * speed * 0.5
                    )
                }
            }
        }
    }
}

object RiseSneak : Module("RiseSneak", Category.RISE, forcedDescription = "Rise - Sneak modifier") {

    private val mode by choices("Mode", arrayOf("Vanilla", "Switch", "Hypixel"), "Vanilla")
    private val packet by boolean("Packet", false)
    private val delay by int("Delay", 0, 0..500)

    private val timerUtil = MSTimer()
    private var sneaking = false

    override fun onDisable() {
        if (sneaking && packet) {
            sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING))
            sneaking = false
        } else if (sneaking) {
            mc.gameSettings.keyBindSneak.pressed = false
            sneaking = false
        }
    }

    val onUpdate = handler<UpdateEvent> {
        if (!timerUtil.hasTimePassed(delay.toLong())) return@handler

        when (mode) {
            "Vanilla" -> {
                mc.gameSettings.keyBindSneak.pressed = true
                sneaking = true
            }
            "Switch" -> {
                sneaking = !sneaking
                if (packet) {
                    sendPacket(
                        if (sneaking) C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING)
                        else C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING)
                    )
                } else {
                    mc.gameSettings.keyBindSneak.pressed = sneaking
                }
                timerUtil.reset()
            }
            "Hypixel" -> {
                if (mc.thePlayer?.onGround == true) {
                    if (packet) {
                        sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING))
                        sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING))
                    } else {
                        mc.gameSettings.keyBindSneak.pressed = true
                        mc.gameSettings.keyBindSneak.pressed = false
                    }
                    timerUtil.reset()
                }
            }
        }
    }
}

object RiseSprint : Module("RiseSprint", Category.RISE, forcedDescription = "Rise - Sprint modifier") {

    private val mode by choices("Mode", arrayOf("Vanilla", "Legit", "Omni", "Back"), "Vanilla")
    private val omniSprint by boolean("OmniSprint", false)
    private val onlyOnGround by boolean("OnlyOnGround", true)
    private val onlyInCombat by boolean("OnlyInCombat", false)

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (onlyOnGround && !thePlayer.onGround) return@handler
        if (onlyInCombat && mc.objectMouseOver?.entityHit !is EntityLivingBase) return@handler

        when (mode) {
            "Vanilla" -> {
                thePlayer.setSprinting(true)
            }
            "Legit" -> {
                if (thePlayer.isMoving && thePlayer.moveForward > 0) {
                    thePlayer.setSprinting(true)
                }
            }
            "Omni" -> {
                if (thePlayer.isMoving || omniSprint) {
                    thePlayer.setSprinting(true)
                }
            }
            "Back" -> {
                if (thePlayer.isMoving) {
                    thePlayer.setSprinting(true)
                }
            }
        }
    }
}

object RiseSnapTap : Module("RiseSnapTap", Category.RISE, forcedDescription = "Rise - Snap tap RISE") {

    private val snapTime by int("SnapTime", 50, 1..200)
    private val range by float("Range", 3f, 1f..10f)

    private var lastKeyTime = 0L
    private var lastKey = 0

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (!thePlayer.isMoving) return@handler

        val currentTime = System.currentTimeMillis()

        if (mc.gameSettings.keyBindLeft.isKeyDown) {
            if (lastKey == 1 && currentTime - lastKeyTime < snapTime) {
                thePlayer.motionX = 0.0
                thePlayer.motionZ = 0.0
            }
            lastKey = 0
            lastKeyTime = currentTime
        } else if (mc.gameSettings.keyBindRight.isKeyDown) {
            if (lastKey == 0 && currentTime - lastKeyTime < snapTime) {
                thePlayer.motionX = 0.0
                thePlayer.motionZ = 0.0
            }
            lastKey = 1
            lastKeyTime = currentTime
        }
    }
}

object RiseWallClimb : Module("RiseWallClimb", Category.RISE, forcedDescription = "Rise - Climb walls") {

    private val mode by choices("Mode", arrayOf("Vanilla", "NCP", "AAC", "Grim"), "Vanilla")
    private val speed by float("Speed", 0.5f, 0.1f..5f)
    private val onlyOnGround by boolean("OnlyOnGround", true)

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (!thePlayer.isCollidedHorizontally) return@handler
        if (onlyOnGround && !thePlayer.onGround) return@handler

        when (mode) {
            "Vanilla" -> {
                thePlayer.motionY = speed.toDouble()
            }
            "NCP" -> {
                if (thePlayer.ticksExisted % 2 == 0) {
                    thePlayer.motionY = speed.toDouble()
                } else {
                    thePlayer.motionY = 0.0
                }
            }
            "AAC" -> {
                thePlayer.motionY = speed.toDouble()
                thePlayer.motionX *= 0.6
                thePlayer.motionZ *= 0.6
            }
            "Grim" -> {
                if (thePlayer.ticksExisted % 3 == 0) {
                    thePlayer.motionY = speed.toDouble()
                }
            }
        }
    }
}

object RiseJesus : Module("RiseJesus", Category.RISE, forcedDescription = "Rise - Walk on water") {

    private val mode by choices("Mode", arrayOf("Vanilla", "NCP", "AAC", "Intave"), "Vanilla")
    private val speed by float("Speed", 1f, 0.1f..5f)
    private val motionY by float("MotionY", 0.1f, 0f..1f)

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (!thePlayer.isInWater) return@handler

        when (mode) {
            "Vanilla" -> {
                thePlayer.motionY = motionY.toDouble()
                if (thePlayer.isMoving) {
                    MovementUtils.strafe(speed)
                }
            }
            "NCP" -> {
                if (thePlayer.ticksExisted % 2 == 0) {
                    thePlayer.motionY = motionY.toDouble()
                }
                if (thePlayer.isMoving && thePlayer.motionY > 0) {
                    MovementUtils.strafe(speed)
                }
            }
            "AAC" -> {
                thePlayer.motionY = motionY.toDouble()
                if (thePlayer.isMoving) {
                    MovementUtils.strafe(speed * 0.8f)
                }
            }
            "Intave" -> {
                thePlayer.motionY = 0.05
                if (thePlayer.isMoving) {
                    MovementUtils.strafe(speed * 0.6f)
                }
            }
        }
    }
}

object RiseClipper : Module("RiseClipper", Category.RISE, forcedDescription = "Rise - Clip through blocks") {

    private val clip by float("Clip", 0.5f, 0.1f..10f)
    private val delay by int("Delay", 1000, 100..5000)
    private val mode by choices("Mode", arrayOf("Vanilla", "NCP"), "Vanilla")

    private val timerUtil = MSTimer()

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (!thePlayer.isMoving) return@handler
        if (!timerUtil.hasTimePassed(delay.toLong())) return@handler

        when (mode) {
            "Vanilla" -> {
                val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
                thePlayer.setPosition(
                    thePlayer.posX - sin(yaw) * clip,
                    thePlayer.posY,
                    thePlayer.posZ + cos(yaw) * clip
                )
            }
            "NCP" -> {
                val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
                sendPacket(C03PacketPlayer.C04PacketPlayerPosition(
                    thePlayer.posX - sin(yaw) * clip,
                    thePlayer.posY,
                    thePlayer.posZ + cos(yaw) * clip,
                    thePlayer.onGround
                ))
                thePlayer.setPosition(
                    thePlayer.posX - sin(yaw) * clip,
                    thePlayer.posY,
                    thePlayer.posZ + cos(yaw) * clip
                )
            }
        }
        timerUtil.reset()
    }
}

object RiseAutoStuck : Module("RiseAutoStuck", Category.RISE, forcedDescription = "Rise - Auto stuck detection and prevention") {

    private val threshold by int("Threshold", 5, 1..30)
    private val mode by choices("Mode", arrayOf("Normal", "TeleportBack"), "Normal")

    private var stuckTicks = 0
    private var lastX = 0.0
    private var lastZ = 0.0

    override fun onEnable() {
        stuckTicks = 0
        lastX = mc.thePlayer?.posX ?: 0.0
        lastZ = mc.thePlayer?.posZ ?: 0.0
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        val moved = Math.abs(thePlayer.posX - lastX) + Math.abs(thePlayer.posZ - lastZ)
        if (moved < 0.01) {
            stuckTicks++
        } else {
            stuckTicks = 0
        }

        if (stuckTicks > threshold) {
            when (mode) {
                "Normal" -> {
                    thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 0.5, thePlayer.posZ)
                }
                "TeleportBack" -> {
                    thePlayer.setPosition(lastX, thePlayer.posY, lastZ)
                }
            }
            stuckTicks = 0
        }

        lastX = thePlayer.posX
        lastZ = thePlayer.posZ
    }
}

object RiseStuck : Module("RiseStuck", Category.RISE, forcedDescription = "Rise - Stuck detection and prevention") {

    private val threshold by int("Threshold", 5, 1..30)

    private var stuckTicks = 0
    private var lastX = 0.0
    private var lastZ = 0.0

    override fun onEnable() {
        stuckTicks = 0
        lastX = mc.thePlayer?.posX ?: 0.0
        lastZ = mc.thePlayer?.posZ ?: 0.0
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        val moved = Math.abs(thePlayer.posX - lastX) + Math.abs(thePlayer.posZ - lastZ)
        if (moved < 0.01) {
            stuckTicks++
        } else {
            stuckTicks = 0
        }

        if (stuckTicks > threshold) {
            thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 0.1, thePlayer.posZ)
            thePlayer.motionX = 0.0
            thePlayer.motionZ = 0.0
            stuckTicks = 0
        }

        lastX = thePlayer.posX
        lastZ = thePlayer.posZ
    }
}

object RisePotionExtender : Module("RisePotionExtender", Category.RISE, forcedDescription = "Rise - Extend potion effects") {

    private val extendTime by int("ExtendTime", 200, 0..1000)
    private val mode by choices("Mode", arrayOf("Normal", "Hypixel"), "Normal")

    private var extended = false

    override fun onEnable() {
        extended = false
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (extended) return@handler

        val activeEffects = thePlayer.activePotionEffects
        if (activeEffects.isNotEmpty()) {
            extended = true
        }
    }
}

object RiseTerrainSpeed : Module("RiseTerrainSpeed", Category.RISE, forcedDescription = "Rise - Speed on different terrain types") {

    private val mode by choices("Mode", arrayOf("Vanilla", "Bloxd", "Grim", "Watchdog"), "Vanilla")
    private val soulsandSpeed by float("SoulSandSpeed", 1f, 0.1f..5f)
    private val iceSpeed by float("IceSpeed", 1.5f, 0.1f..5f)
    private val webSpeed by float("WebSpeed", 1f, 0.1f..5f)

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (!thePlayer.isMoving) return@handler

        val blockBelow = BlockPos(thePlayer.posX, thePlayer.posY - 0.5, thePlayer.posZ)
        val block = thePlayer.worldObj.getBlockState(blockBelow).block

        when (mode) {
            "Vanilla" -> {
                if (block == Blocks.soul_sand) {
                    MovementUtils.strafe(MovementUtils.speed * soulsandSpeed)
                } else if (block == Blocks.ice || block == Blocks.packed_ice) {
                    MovementUtils.strafe(MovementUtils.speed * iceSpeed)
                } else if (block == Blocks.web) {
                    thePlayer.motionX *= webSpeed.toDouble()
                    thePlayer.motionZ *= webSpeed.toDouble()
                }
            }
            else -> {
                if (block == Blocks.soul_sand) {
                    MovementUtils.strafe(MovementUtils.speed * soulsandSpeed)
                } else if (block == Blocks.ice || block == Blocks.packed_ice) {
                    MovementUtils.strafe(MovementUtils.speed * iceSpeed)
                }
            }
        }
    }
}

object RiseTeleport : Module("RiseTeleport", Category.RISE, forcedDescription = "Rise - Teleport RISE") {

    private val distance by float("Distance", 5f, 1f..50f)
    private val delay by int("Delay", 1000, 100..10000)
    private val mode by choices("Mode", arrayOf("Vanilla", "NCP"), "Vanilla")

    private val timerUtil = MSTimer()

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (!thePlayer.isMoving) return@handler
        if (!timerUtil.hasTimePassed(delay.toLong())) return@handler

        val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
        val newX = thePlayer.posX - sin(yaw) * distance
        val newZ = thePlayer.posZ + cos(yaw) * distance

        when (mode) {
            "Vanilla" -> {
                thePlayer.setPosition(newX, thePlayer.posY, newZ)
            }
            "NCP" -> {
                sendPacket(C03PacketPlayer.C04PacketPlayerPosition(newX, thePlayer.posY, newZ, thePlayer.onGround))
                thePlayer.setPosition(newX, thePlayer.posY, newZ)
            }
        }
        timerUtil.reset()
    }
}

object RiseAutoMLG : Module("RiseAutoMLG", Category.RISE, forcedDescription = "Rise - Auto MLG water bucket") {

    private val mode by choices("Mode", arrayOf("Normal", "Hypixel", "NCP"), "Normal")
    private val fallDistance by float("FallDistance", 5f, 3f..30f)
    private val delay by int("Delay", 0, 0..500)
    private val autoSwitch by boolean("AutoSwitch", true)

    private var mlgPerformed = false
    private val timerUtil = MSTimer()

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (thePlayer.onGround || thePlayer.isInWater) {
            mlgPerformed = false
            return@handler
        }

        if (thePlayer.fallDistance >= fallDistance && !mlgPerformed && timerUtil.hasTimePassed(delay.toLong())) {
            val waterBucketSlot = (0..8).firstOrNull {
                thePlayer.inventory.getStackInSlot(it)?.item == net.minecraft.init.Items.water_bucket
            } ?: return@handler

            val prevSlot = thePlayer.inventory.currentItem

            when (mode) {
                "Normal" -> {
                    if (autoSwitch) thePlayer.inventory.currentItem = waterBucketSlot
                    mc.playerController.sendUseItem(thePlayer, thePlayer.worldObj, thePlayer.inventory.getStackInSlot(waterBucketSlot))
                    if (autoSwitch) thePlayer.inventory.currentItem = prevSlot
                }
                "Hypixel" -> {
                    if (autoSwitch) thePlayer.inventory.currentItem = waterBucketSlot
                    val blockPos = BlockPos(thePlayer.posX, thePlayer.posY - 1, thePlayer.posZ)
                    if (thePlayer.worldObj.isAirBlock(blockPos)) {
                        mc.playerController.sendUseItem(thePlayer, thePlayer.worldObj, thePlayer.inventory.getStackInSlot(waterBucketSlot))
                    }
                    if (autoSwitch) thePlayer.inventory.currentItem = prevSlot
                }
                "NCP" -> {
                    if (autoSwitch) thePlayer.inventory.currentItem = waterBucketSlot
                    mc.playerController.sendUseItem(thePlayer, thePlayer.worldObj, thePlayer.inventory.getStackInSlot(waterBucketSlot))
                    if (autoSwitch) thePlayer.inventory.currentItem = prevSlot
                }
            }
            mlgPerformed = true
            timerUtil.reset()
        }
    }
}

object RiseNoJumpDelay : Module("RiseNoJumpDelay", Category.RISE, forcedDescription = "Rise - Remove jump delay") {

    private val mode by choices("Mode", arrayOf("Vanilla", "NCP"), "Vanilla")

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (thePlayer.jumpTicks > 10) {
            thePlayer.jumpTicks = 0
        }
    }
}

object RiseResourcePackSpoof : Module("RiseResourcePackSpoof", Category.RISE, forcedDescription = "Rise - Spoof resource pack acceptance") {

    private val mode by choices("Mode", arrayOf("Vanilla", "Hypixel"), "Vanilla")

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is net.minecraft.network.play.server.S48PacketResourcePackSend) {
            when (mode) {
                "Vanilla" -> {
                    event.cancelEvent()
                    sendPacket(net.minecraft.network.play.client.C19PacketResourcePackStatus("success", net.minecraft.network.play.client.C19PacketResourcePackStatus.Action.SUCCESSFULLY_LOADED))
                }
                "Hypixel" -> {
                    event.cancelEvent()
                    sendPacket(net.minecraft.network.play.client.C19PacketResourcePackStatus("success", net.minecraft.network.play.client.C19PacketResourcePackStatus.Action.ACCEPTED))
                    sendPacket(net.minecraft.network.play.client.C19PacketResourcePackStatus("success", net.minecraft.network.play.client.C19PacketResourcePackStatus.Action.SUCCESSFULLY_LOADED))
                }
            }
        }
    }
}
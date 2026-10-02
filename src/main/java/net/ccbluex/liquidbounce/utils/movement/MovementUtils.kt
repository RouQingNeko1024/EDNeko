/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.utils.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.potion.Potion
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MovementUtils : MinecraftInstance, Listenable {

    var affectSprintOnAttack: Boolean? = null

    var speed
        get() = mc.thePlayer?.run { sqrt(motionX * motionX + motionZ * motionZ).toFloat() } ?: .0f
        set(value) {
            strafe(value)
        }

    val hasMotion
        get() = mc.thePlayer?.run { motionX != .0 || motionY != .0 || motionZ != .0 } == true

    var airTicks = 0
    var groundTicks = 0

    @JvmOverloads
    fun strafe(
        speed: Float = MovementUtils.speed, stopWhenNoInput: Boolean = false, moveEvent: MoveEvent? = null,
        strength: Double = 1.0,
    ) =
        mc.thePlayer?.run {
            if (!mc.thePlayer.isMoving) {
                if (stopWhenNoInput) {
                    moveEvent?.zeroXZ()
                    stopXZ()
                }

                return@run
            }

            val prevX = motionX * (1.0 - strength)
            val prevZ = motionZ * (1.0 - strength)
            val useSpeed = speed * strength

            val yaw = direction
            val x = (-sin(yaw) * useSpeed) + prevX
            val z = (cos(yaw) * useSpeed) + prevZ

            if (moveEvent != null) {
                moveEvent.x = x
                moveEvent.z = z
            }

            motionX = x
            motionZ = z
        }

    fun Vec3.strafe(
        yaw: Float = direction.toDegreesF(), speed: Double = sqrt(xCoord * xCoord + zCoord * zCoord),
        strength: Double = 1.0,
        moveCheck: Boolean = false,
    ): Vec3 {
        if (moveCheck) {
            xCoord = 0.0
            zCoord = 0.0
            return this
        }

        val prevX = xCoord * (1.0 - strength)
        val prevZ = zCoord * (1.0 - strength)
        val useSpeed = speed * strength

        val angle = Math.toRadians(yaw.toDouble())
        xCoord = (-sin(angle) * useSpeed) + prevX
        zCoord = (cos(angle) * useSpeed) + prevZ
        return this
    }

    fun forward(distance: Double) =
        mc.thePlayer?.run {
            val yaw = rotationYaw.toRadiansD()
            setPosition(posX - sin(yaw) * distance, posY, posZ + cos(yaw) * distance)
        }

    fun getBaseMoveSpeed(): Double {
        var baseSpeed = mc.thePlayer.capabilities.walkSpeed * 2.873

        mc.thePlayer.getActivePotionEffect(Potion.moveSlowdown)?.let { effect ->
            baseSpeed /= 1.0 + 0.2 * (effect.amplifier + 1)
        }

        mc.thePlayer.getActivePotionEffect(Potion.moveSpeed)?.let { effect ->
            baseSpeed *= 1.0 + 0.2 * (effect.amplifier + 1)
        }

        return baseSpeed
    }

    fun setSpeed(speed: Double, movingCheck: Boolean) {
        if (!mc.thePlayer.isMoving && movingCheck) return

        val yaw = direction
        mc.thePlayer.motionX = -sin(yaw) * speed
        mc.thePlayer.motionZ = cos(yaw) * speed
    }

    val direction
        get() = mc.thePlayer?.run {
            var yaw = rotationYaw
            var forward = 1f

            if (movementInput.moveForward < 0f) {
                yaw += 180f
                forward = -0.5f
            } else if (movementInput.moveForward > 0f)
                forward = 0.5f

            if (movementInput.moveStrafe < 0f) yaw += 90f * forward
            else if (movementInput.moveStrafe > 0f) yaw -= 90f * forward

            yaw.toRadiansD()
        } ?: 0.0

    fun isOnGround(height: Double) =
        mc.theWorld != null && mc.thePlayer != null &&
            mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer,
                mc.thePlayer.entityBoundingBox.offset(Vec3_ZERO.withY(-height))
            ).isNotEmpty()

    var serverOnGround = false

    var serverX = .0
    var serverY = .0
    var serverZ = .0

    val onPacket = handler<PacketEvent> { event ->
        if (event.isCancelled)
            return@handler

        val packet = event.packet

        if (packet is C03PacketPlayer) {
            serverOnGround = packet.onGround

            if (packet.isMoving) {
                serverX = packet.x
                serverY = packet.y
                serverZ = packet.z
            }
        }
    }

    fun isBlockUnder(): Boolean {
        val player = mc.thePlayer ?: return false
        for (i in 0..4) {
            val pos = BlockPos(player.posX, player.posY - i, player.posZ)
            if (mc.theWorld.getBlockState(pos).block != net.minecraft.init.Blocks.air) {
                return true
            }
        }
        return false
    }

    fun doTargetStrafe(
        curTarget: EntityLivingBase,
        direction_: Double,
        radius: Double,
        moveEvent: MoveEvent,
        mathRadius: Int
    ) {
        var forward_ = 1.0
        var strafe_ = 0.0
        var speed_ = sqrt(
            moveEvent.x * moveEvent.x + moveEvent.z * moveEvent.z
        )
        var _direction = 0.0
        if (direction_ > 0.001) {
            _direction = 1.0
        } else if (direction_ < -0.001) {
            _direction = -1.0
        }
        var curDistance: Float
        if (mathRadius == 1) {
            curDistance = mc.thePlayer.getDistanceToEntity(curTarget)
        } else {
            curDistance =
                sqrt(
                    (mc.thePlayer.posX - curTarget.posX) * (mc.thePlayer.posX - curTarget.posX) +
                    (mc.thePlayer.posZ - curTarget.posZ) * (mc.thePlayer.posZ - curTarget.posZ)
                ).toFloat()
        }
        if (curDistance < radius - speed_) {
            forward_ = -1.0
        } else if (curDistance > radius + speed_) {
            forward_ = 1.0
        } else {
            forward_ = (curDistance - radius) / speed_
        }
        if (curDistance < radius + speed_ * 2 && curDistance > radius - speed_ * 2) {
            strafe_ = 1.0
        }
        strafe_ *= _direction
        var strafeYaw = RotationUtils.getRotationsEntity(curTarget).yaw.toDouble()
        val covert_ = sqrt(forward_ * forward_ + strafe_ * strafe_)

        forward_ /= covert_
        strafe_ /= covert_
        var turnAngle = Math.toDegrees(asin(strafe_))
        if (turnAngle > 0) {
            if (forward_ < 0)
                turnAngle = 180.0 - turnAngle
        } else {
            if (forward_ < 0)
                turnAngle = -180.0 - turnAngle
        }
        strafeYaw = Math.toRadians(strafeYaw + turnAngle)
        moveEvent.x = -sin(strafeYaw) * speed_
        moveEvent.z = cos(strafeYaw) * speed_
        mc.thePlayer.motionX = moveEvent.x
        mc.thePlayer.motionZ = moveEvent.z
    }
}
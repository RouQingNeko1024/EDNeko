/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
@file:Suppress("MayBeConstant")

package net.ccbluex.liquidbounce.utils.rotation

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.combat.FastBow
import net.ccbluex.liquidbounce.features.module.modules.misc.NoRotateSet
import net.ccbluex.liquidbounce.features.module.modules.render.Rotations
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.rotation
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextDouble
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.MAX_CAPTURE_TICKS
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getFixedAngleDelta
import net.ccbluex.liquidbounce.utils.rotation.rotationAdditions.BezierRotation
import net.ccbluex.liquidbounce.utils.rotation.rotationAdditions.SmoothRotation
import net.ccbluex.liquidbounce.utils.rotation.rotations.*
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.*
import org.lwjgl.input.Mouse
import javax.vecmath.Vector2f
import kotlin.math.*
import kotlin.random.Random


object RotationUtils : MinecraftInstance, Listenable {
    /**
     * Our final rotation point, which [currentRotation] follows.
     */
    var targetRotation: Rotation? = null

    /**
     * The current rotation that is responsible for aiming at objects, synchronizing movement, etc.
     */
    var currentRotation: Rotation? = null

    // import classes
    private val patternPrediction = PatternPrediction()
    private val smoothRotation = SmoothRotation()
    private val entropyRotation = EntropyRotation()
    private val accelerationRotation = AccelerationRotation()
    private val dynamicRotation = DynamicRotation()
    private val bezierRotation = BezierRotation()



    init {
        Thread {
            try {
                patternPrediction.loadCollectedPatterns()

            } catch (e: Exception) {
                println("Failed to load collected patterns: ${e.message}")
            }
        }.start()
    }

    /**
     * The last rotation that the server has received.
     */
    var serverRotation: Rotation
        get() = lastRotations[0]
        set(value) {
            lastRotations = lastRotations.toMutableList().apply { set(0, value) }
        }

    private const val MAX_CAPTURE_TICKS = 3

    var modifiedInput = MovementInput()

    /**
     * A list that stores the last rotations captured from 0 up to [MAX_CAPTURE_TICKS] previous ticks.
     */
    var lastRotations = MutableList(MAX_CAPTURE_TICKS) { Rotation.ZERO }
        set(value) {
            val updatedList = MutableList(lastRotations.size) { Rotation.ZERO }

            for (tick in 0 until MAX_CAPTURE_TICKS) {
                updatedList[tick] = if (tick == 0) value[0] else field[tick - 1]
            }

            field = updatedList
        }
    private object CancelRotationTicker
    private object FailRotationCancelTicker

    private var lastValidRotation: Rotation? = null
    private var lastValidRotationTime: Long = 0L
    /**
     * The currently in-use rotation settings, which are used to determine how the rotations will move.
     */
    var activeSettings: RotationSettings? = null
    var randomizationSettings: RandomizationSettings? = null

    var resetTicks = 0

    fun getRotationDifference(entity: Entity): Double {
        val rotation = toRotation(getCenter(entity.entityBoundingBox), true)
        return getRotationDifference(rotation, Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch))
    }
    fun getRotationDifference(a: Rotation, b: Rotation?): Double {
        return hypot(getAngleDifference(a.yaw, b!!.yaw).toDouble(), (a.pitch - b.pitch).toDouble())
    }
    fun getCenter(bb: AxisAlignedBB): Vec3 {
        return Vec3(
            bb.minX + (bb.maxX - bb.minX) * 0.5,
            bb.minY + (bb.maxY - bb.minY) * 0.5,
            bb.minZ + (bb.maxZ - bb.minZ) * 0.5
        )
    }
    /**
     * Face block
     *
     * @param blockPos target block
     */
    fun faceBlock(
        blockPos: BlockPos?,
        throughWalls: Boolean = true,
        targetUpperFace: Boolean = false,
        hRange: ClosedFloatingPointRange<Double> = 0.0..1.0
    ): VecRotation? {
        val world = mc.theWorld ?: return null
        val player = mc.thePlayer ?: return null

        if (blockPos == null) return null

        val block = blockPos.block ?: return null

        val eyesPos = player.eyes
        val startPos = Vec3(blockPos)

        var visibleVec: VecRotation? = null
        var invisibleVec: VecRotation? = null

        val yRange = if (targetUpperFace) 0.0..0.01 else 0.0..1.0

        for (x in hRange) {
            for (y in yRange) {
                for (z in hRange) {
                    val posVec = startPos.add(block.lerpWith(x, y, z))

                    val dist = eyesPos.distanceTo(posVec)

                    val (diffX, diffY, diffZ) = posVec - eyesPos
                    val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)

                    val rotation = Rotation(
                        MathHelper.wrapAngleTo180_float(atan2(diffZ, diffX).toDegreesF() - 90f),
                        MathHelper.wrapAngleTo180_float(-atan2(diffY, diffXZ).toDegreesF())
                    ).fixedSensitivity()

                    val rotationVector = getVectorForRotation(rotation)
                    val vector = eyesPos + (rotationVector * dist)

                    val currentVec = VecRotation(posVec, rotation)
                    val raycast = world.rayTraceBlocks(eyesPos, vector, false, true, false)

                    val currentRotation = currentRotation ?: player.rotation

                    if (raycast != null && raycast.blockPos == blockPos && (!targetUpperFace || raycast.sideHit == EnumFacing.UP)) {
                        if (visibleVec == null || rotationDifference(
                                currentVec.rotation, currentRotation
                            ) < rotationDifference(visibleVec.rotation, currentRotation)
                        ) {
                            visibleVec = currentVec
                        }
                    } else if (throughWalls) {
                        val invisibleRaycast = performRaytrace(blockPos, rotation) ?: continue

                        if (invisibleRaycast.blockPos != blockPos) {
                            continue
                        }

                        if (invisibleVec == null || rotationDifference(
                                currentVec.rotation, currentRotation
                            ) < rotationDifference(invisibleVec.rotation, currentRotation)
                        ) {
                            invisibleVec = currentVec
                        }
                    }
                }
            }
        }

        return visibleVec ?: invisibleVec
    }

    /**
     * Face trajectory of arrow by default, can be used for calculating other trajectories (eggs, snowballs)
     * by specifying `gravity` and `velocity` parameters
     *
     * @param target      your enemy
     * @param predict     predict new enemy position
     * @param predictSize predict size of predict
     * @param gravity     how much gravity does the projectile have, arrow by default
     * @param velocity    with what velocity will the projectile be released, velocity for arrow is calculated when null
     */
    fun faceTrajectory(
        target: Entity,
        predict: Boolean,
        predictSize: Float,
        gravity: Float = 0.05f,
        velocity: Float? = null,
    ): Rotation {

        val player = mc.thePlayer

        val posX =
            target.posX + (if (predict) (target.posX - target.prevPosX) * predictSize else .0) - (player.posX + if (predict) player.posX - player.prevPosX else .0)
        val posY =
            target.entityBoundingBox.minY + (if (predict) (target.entityBoundingBox.minY - target.prevPosY) * predictSize else .0) + target.eyeHeight - 0.15 - (player.entityBoundingBox.minY + (if (predict) player.posY - player.prevPosY else .0)) - player.getEyeHeight()
        val posZ =
            target.posZ + (if (predict) (target.posZ - target.prevPosZ) * predictSize else .0) - (player.posZ + if (predict) player.posZ - player.prevPosZ else .0)
        val posSqrt = sqrt(posX * posX + posZ * posZ)

        var finalVelocity = velocity

        if (finalVelocity == null) {
            finalVelocity = if (FastBow.handleEvents()) 1f else player.itemInUseDuration / 20f
            finalVelocity = ((finalVelocity * finalVelocity + finalVelocity * 2) / 3).coerceAtMost(1f)
        }

        val gravityModifier = 0.12f * gravity

        return Rotation(
            atan2(posZ, posX).toDegreesF() - 90f, -atan(
                (finalVelocity * finalVelocity - sqrt(
                    finalVelocity * finalVelocity * finalVelocity * finalVelocity - gravityModifier * (gravityModifier * posSqrt * posSqrt + 2 * posY * finalVelocity * finalVelocity)
                )) / (gravityModifier * posSqrt)
            ).toDegreesF()
        )
    }

    /**
     * Translate vec to rotation
     *
     * @param vec     target vec
     * @param predict predict new location of your body
     * @return rotation
     */
    fun toRotation(vec: Vec3, predict: Boolean = false, fromEntity: Entity = mc.thePlayer): Rotation {
        val eyesPos = fromEntity.eyes
        if (predict) eyesPos.addVector(fromEntity.motionX, fromEntity.motionY, fromEntity.motionZ)

        val (diffX, diffY, diffZ) = vec - eyesPos
        return Rotation(
            MathHelper.wrapAngleTo180_float(
                atan2(diffZ, diffX).toDegreesF() - 90f
            ), MathHelper.wrapAngleTo180_float(
                -atan2(diffY, sqrt(diffX * diffX + diffZ * diffZ)).toDegreesF()
            )
        )
    }

    /**
     * Search good center
     *
     * @param bb                entity box to search rotation for
     * @param outborder         outborder option
     * @param random            random option
     * @param predict           predict, offsets rotation by player's motion
     * @param lookRange         look range
     * @param attackRange       attack range, rotations in attack range will be prioritized
     * @param throughWallsRange through walls range,
     * @return center
     */
    fun searchCenter(
        bb: AxisAlignedBB,
        distanceBasedSpot: Boolean = false,
        outborder: Boolean,
        randomization: RandomizationSettings? = null,
        predict: Boolean,
        lookRange: Float,
        attackRange: Float,
        throughWallsRange: Float = 0f,
        bodyPoints: List<String> = listOf("Head", "Feet"),
        horizontalSearch: ClosedFloatingPointRange<Float> = 0f..1f,
    ): Rotation? {
        activeSettings?.rotationMode?.let {
            if ("Center" in it) {
                val center = getCenter(bb)
                return toRotation(center, predict).fixedSensitivity()
            }
        }
        val scanRange = lookRange.coerceAtLeast(attackRange)

        val max = BodyPoint.fromString(bodyPoints[0]).range.endInclusive
        val min = BodyPoint.fromString(bodyPoints[1]).range.start

        if (outborder) {
            val vec3 = bb.lerpWith(nextDouble(0.5, 1.3), nextDouble(0.9, 1.3), nextDouble(0.5, 1.3))

            return toRotation(vec3, predict).fixedSensitivity()
        }

        val eyes = mc.thePlayer.eyes

        val preferredRotation = toRotation(getNearestPointBB(eyes, bb), predict).takeIf {
            distanceBasedSpot
        } ?: currentRotation ?: mc.thePlayer.rotation

        val currRotation = Rotation.ZERO.plus(preferredRotation)

        var attackRotation: Pair<Rotation, Float>? = null
        var lookRotation: Pair<Rotation, Float>? = null


        randomization?.takeIf { it.randomizationChosen }?.run {
            activeSettings?.let { processNextSpot(bb, currRotation, eyes, scanRange.toDouble(),it) }
        }

        val (hMin, hMax) = horizontalSearch.start.toDouble() to min(horizontalSearch.endInclusive + 0.01, 1.0)

        for (x in hMin..hMax) {
            for (y in min..max) {
                for (z in hMin..hMax) {
                    val vec = bb.lerpWith(x, y, z)

                    val rotation = toRotation(vec, predict).fixedSensitivity()

                    // Calculate actual hit vec after applying fixed sensitivity to rotation
                    val gcdVec = bb.calculateIntercept(
                        eyes, eyes + getVectorForRotation(rotation) * scanRange.toDouble()
                    )?.hitVec ?: continue

                    val distance = eyes.distanceTo(gcdVec)

                    // Check if vec is in range
                    // Skip if a rotation that is in attack range was already found and the vec is out of attack range
                    if (distance > scanRange || (attackRotation != null && distance > attackRange)) continue

                    // Check if vec is reachable through walls
                    if (!isVisible(gcdVec) && distance > throughWallsRange) continue

                    val rotationWithDiff = rotation to rotationDifference(rotation, currRotation)

                    if (distance <= attackRange) {
                        if (attackRotation == null || rotationWithDiff.second < attackRotation.second) attackRotation =
                            rotationWithDiff
                    } else {
                        if (lookRotation == null || rotationWithDiff.second < lookRotation.second) lookRotation =
                            rotationWithDiff
                    }
                }
            }
        }

        return attackRotation?.first ?: lookRotation?.first ?: run {
            val vec = getNearestPointBB(eyes, bb)
            val dist = eyes.distanceTo(vec)

            if (dist <= scanRange && (dist <= throughWallsRange || isVisible(vec))) toRotation(vec, predict)
            else null
        }
    }

    /**
     * Calculate difference between the client rotation and your entity
     *
     * @param entity your entity
     * @return difference between rotation
     */
    fun rotationDifference(entity: Entity) =
        rotationDifference(toRotation(entity.hitBox.center, true), mc.thePlayer.rotation)

    /**
     * Calculate difference between two rotations
     *
     * @param a rotation
     * @param b rotation
     * @return difference between rotation
     */
    fun rotationDifference(a: Rotation, b: Rotation = serverRotation) =
        hypot(angleDifference(a.yaw, b.yaw), a.pitch - b.pitch)


    /**
     * 应用疲劳效果到旋转
     */
    fun applyFatigueEffect(
        currentRotation: Rotation,
        targetRotation: Rotation,
        settings: RotationSettings
    ): Rotation {
        if (!settings.fatigueEnabled.get()) {
            return targetRotation
        }

        val now = System.currentTimeMillis()
        val timeDiff = (now - settings.lastRotationTime).coerceAtLeast(0)
        settings.lastRotationTime = now

        // 计算旋转差异
        val diff = rotationDifference(targetRotation, currentRotation)
        val diffChange = abs(diff - settings.lastRotationDiff)
        settings.lastRotationDiff = diff

        // 根据时间恢复疲劳度
        settings.fatigueLevel = max(0f, settings.fatigueLevel - (timeDiff / 1000f) * settings.fatigueRecoveryRate.get())

        // 根据旋转差异增加疲劳度
        when (settings.fatigueMode) {
            "Linear" -> {
                if (diff > settings.fatigueStartThreshold.get()) {
                    val fatigueIncrease = (diff - settings.fatigueStartThreshold.get()) /
                            (settings.fatigueMaxThreshold.get() - settings.fatigueStartThreshold.get())
                    settings.fatigueLevel = min(1f, settings.fatigueLevel + fatigueIncrease * 0.1f)
                }
            }
            "Exponential" -> {
                if (diff > settings.fatigueStartThreshold.get()) {
                    val normalizedDiff = (diff - settings.fatigueStartThreshold.get()) /
                            (settings.fatigueMaxThreshold.get() - settings.fatigueStartThreshold.get())
                    settings.fatigueLevel = min(1f, settings.fatigueLevel + normalizedDiff * normalizedDiff * 0.2f)
                }
            }
            "CustomCurve" -> {
                val curveFactor = settings.fatigueCustomCurve.get().random()
                settings.fatigueLevel = min(1f, settings.fatigueLevel + diffChange * curveFactor * 0.01f)
            }
        }

        // 应用疲劳效果
        return when (settings.fatigueMode) {
            "Linear" -> {
                val fatigueFactor = 1f - settings.fatigueLevel
                Rotation(
                    currentRotation.yaw + angleDifference(targetRotation.yaw, currentRotation.yaw) * fatigueFactor,
                    currentRotation.pitch + (targetRotation.pitch - currentRotation.pitch) * fatigueFactor
                )
            }
            "Exponential" -> {
                val fatigueFactor = 1f - settings.fatigueLevel * settings.fatigueLevel
                Rotation(
                    currentRotation.yaw + angleDifference(targetRotation.yaw, currentRotation.yaw) * fatigueFactor,
                    currentRotation.pitch + (targetRotation.pitch - currentRotation.pitch) * fatigueFactor
                )
            }
            "CustomCurve" -> {
                val yawFactor = 1f - settings.fatigueLevel * settings.fatigueYawFactor.get()
                val pitchFactor = 1f - settings.fatigueLevel * settings.fatiguePitchFactor.get()
                Rotation(
                    currentRotation.yaw + angleDifference(targetRotation.yaw, currentRotation.yaw) * yawFactor,
                    currentRotation.pitch + (targetRotation.pitch - currentRotation.pitch) * pitchFactor
                )
            }
            else -> targetRotation
        }
    }

    private fun limitAngleChange(
        currentRotation: Rotation,
        targetRotation: Rotation,
        settings: RotationSettings,
        randomization: RandomizationSettings? = null,
    ): Rotation {
        // 获取基础目标旋转
        var baseTarget = targetRotation

        // 在所有模式之前应用手动转头混合（如果启用）
        val player = mc.thePlayer
        if (player != null) {
            baseTarget = handleSmartManualRotationMix(baseTarget, settings)
        }

        // 获取基础速度设置
        var (hSpeed, vSpeed) = if (settings.instantRotation) {
            180f to 180f
        } else settings.horizontalSpeed to settings.verticalSpeed

        if (settings.damageBoostEnabled.get() && (mc.thePlayer?.hurtTime
                ?: 0) > settings.damageBoostHurtTimeThreshold.get()
        ) {
            val multiplier = settings.damageBoostMultiplier.get().random()
            hSpeed *= multiplier
            vSpeed *= multiplier
        }

        // 应用启发式调整（如果启用）
        if (settings.heuristicRotationEnabled.get()) {
            val adjustedSpeeds = applyHeuristicAdjustment(
                currentRotation,
                baseTarget,
                hSpeed,
                vSpeed,
                mc.thePlayer,
                mc.pointedEntity
            )
            hSpeed = adjustedSpeeds.first
            vSpeed = adjustedSpeeds.second
        }

        // 使用平滑旋转计算中间旋转
        val smoothed = if (settings.useSmoothRotation) {
            // 获取平滑参数
            val tension = when (settings.smoothRotationTypeValue.get()) {
                "HERMITE" -> settings.hermiteTension.get().random()
                else -> 0.5f
            }

            val bias = if (settings.smoothRotationTypeValue.get() == "HERMITE") {
                settings.hermiteBias.get().random()
            } else {
                0.0f
            }

            smoothRotation.smoothRotation(
                currentRotation,
                baseTarget,
                settings.smoothRotationAlpha,
                settings.smoothRotationType,
                tension,
                bias,
                settings.bounceAmplitude.get(),
                settings.elasticPeriod.get()
            )
        } else {
            baseTarget
        }

        val randomizedTarget = if (randomization?.randomizationChosen == true && randomization.allowWorkWhenRotating) {
            val tempRotation = smoothed.copy()
            randomization.processNextSpot(
                mc.pointedEntity?.entityBoundingBox ?: mc.thePlayer.entityBoundingBox,
                tempRotation,
                mc.thePlayer.eyes,
                mc.playerController.blockReachDistance.toDouble(),
                settings
            )
            tempRotation
        } else {
            smoothed
        }

        val result = when {
            settings.pidEnabled.get() -> smoothRotationPID(randomizedTarget, currentRotation, settings)
            settings.bezierCurveEnabled.get() -> {
                if (settings.dynamicBezierEnabled.get()) {
                    val player = mc.thePlayer ?: return randomizedTarget
                    val target = mc.pointedEntity ?: return randomizedTarget
                    dynamicBezierRotationTo(
                        randomizedTarget,
                        currentRotation,
                        settings,
                        player,
                        target
                    )
                } else {
                    bezierRotationTo(
                        randomizedTarget,
                        currentRotation,
                        settings,
                        settings.minYawStep.get(),
                        settings.minPitchStep.get()
                    )
                }
            }
            else -> performAngleChange(
                currentRotation,
                randomizedTarget,
                hSpeed,
                vSpeed,
                !settings.instant && settings.legitimize,
                settings.legitimizeFactor.range,
                settings.minRotationDifference,
                settings.minRotationDifferenceResetTiming
            )
        }

        // 应用疲劳效果
        val fatiguedResult = if (settings.fatigueEnabled.get()) {
            applyFatigueEffect(currentRotation, result, settings)
        } else {
            result
        }
        val rotationMode = settings.rotationMode
        val finalResult = if ("Center" !in rotationMode) {
            val handle1 = if (settings.allowExcessiveRotation.get()) handleAllowExcessiveRotation(
                currentRotation,
                fatiguedResult,
                settings
            ) else fatiguedResult
            val handle2 = if ("EntropyControlled" in rotationMode) entropyRotation.handleEntropyControlledMode(
                currentRotation,
                handle1,
                settings
            ) else handle1
            val handle3 =
                if ("AdvancedRotationSpeed" in rotationMode) AdvancedRotation(option = settings).handleAdvancedRotationSpeedMode(
                    currentRotation,
                    handle2
                ) else handle2
            val handle4 = if ("Acceleration" in rotationMode) accelerationRotation.handleAccelerationMode(
                currentRotation,
                handle3,
                settings
            ) else handle3
            val handle5 =
                if ("SlowPursuit" in settings.rotationMode) handleSlowPursuit(
                    currentRotation,
                    handle4,
                    settings
                )
            else handle4
            val handle6 = if ("DynamicRotation" in settings.rotationMode) dynamicRotation.handleDynamicRotationMode(
                currentRotation,
                handle5,
                settings
            ) else handle5
            val handle7 = if ("PatternPrediction" in rotationMode) patternPrediction.handlePatternPredictionMode(
                currentRotation,
                handle6,
                settings
            ) else handle6

            // return
            handle7
        } else handleCenterMode(currentRotation, fatiguedResult, settings)


        if ("PatternPrediction" in rotationMode && settings.neuralNetworkEnabled.get() && settings.nnTrainingEnabled.get()) {
            patternPrediction.collectTrainingDataForNN(currentRotation, finalResult, settings)
        }

        return finalResult
    }

    private fun applyHeuristicAdjustment(
        currentRotation: Rotation,
        targetRotation: Rotation,
        baseHSpeed: Float,
        baseVSpeed: Float,
        player: EntityPlayer?,
        target: Entity?
    ): Pair<Float, Float> {
        if (player == null) return baseHSpeed to baseVSpeed

        // 计算旋转差异
        val (yawDiff, pitchDiff) = angleDifferences(targetRotation, currentRotation)
        val distance = if (target != null) player.getDistanceToEntity(target) else 4f

        var hSpeed = baseHSpeed
        var vSpeed = baseVSpeed

        // 简单：根据差值大小调整
        if (abs(yawDiff) > 45f || abs(pitchDiff) > 20f) {
            hSpeed *= 1.1f
            vSpeed *= 1.05f
        } else {
            hSpeed *= 0.95f
            vSpeed *= 0.95f
        }

        // 简单：根据移动状态调整
        if (player.isSprinting) {
            hSpeed *= 1.15f
        } else if (player.isSneaking) {
            hSpeed *= 0.85f
            vSpeed *= 0.85f
        }

        // 简单：根据目标距离调整
        if (distance < 3.0f) {
            hSpeed *= 1.1f
            vSpeed *= 1.05f
        } else if (distance > 8.0f) {
            hSpeed *= 0.9f
            vSpeed *= 0.9f
        }

        return hSpeed to vSpeed
    }

    fun handleSlowPursuit(current: Rotation, target: Rotation, settings: RotationSettings): Rotation {
        val now = System.currentTimeMillis()
        val lastAttackTime = settings.slowPursuitLastAttackTime

        // 检查是否在攻击后的 500ms 窗口内
        val isInAttackWindow = lastAttackTime != null && now - lastAttackTime <= settings.slowPursuitDurationTime

        // 如果不在攻击窗口内，则回退到 Normal 模式
        if (!isInAttackWindow) {
            settings.slowPursuitActive = false
            return performAngleChange(
                current,
                target,
                settings.horizontalSpeed,
                settings.verticalSpeed,
                settings.legitimize,
                settings.legitimizeFactor.range,
                settings.minRotationDifference,
                settings.minRotationDifferenceResetTiming
            )
        }

        // 在攻击窗口内，激活 SlowPursuit 行为
        settings.slowPursuitActive = true

        val speed = settings.slowPursuitSpeed.random()
        val delay = settings.slowPursuitDelay.random()
        val randomness = settings.slowPursuitRandomness.random()

        val yawDiff = angleDifference(target.yaw, current.yaw)
        val pitchDiff = target.pitch - current.pitch

        val currentTick = (now / 50) % delay

        return if (currentTick == 0L) {
            val yawChange = yawDiff.coerceIn(-speed, speed) * (1 + randomness * (Random.nextFloat() * 2 - 1))
            val pitchChange = pitchDiff.coerceIn(-speed * 0.5f, speed * 0.5f) * (1 + randomness * (Random.nextFloat() * 2 - 1))

            Rotation(
                current.yaw + yawChange,
                current.pitch + pitchChange
            ).fixedSensitivity()
        } else {
            current
        }
    }


    fun performAngleChange(
        currentRotation: Rotation,
        targetRotation: Rotation,
        hSpeed: Float,
        vSpeed: Float = hSpeed,
        legitimize: Boolean,
        legitimizeFactor: ClosedFloatingPointRange<Float>,
        minRotationDiff: Float,
        minRotationDiffResetTiming: String,
    ): Rotation {
        var (yawDiff, pitchDiff) = angleDifferences(targetRotation, currentRotation)

        val rotationDifference = hypot(yawDiff, pitchDiff)

        val isShortStopActive = WaitTickUtils.hasScheduled(this)
        val isNoRotateSetActive = WaitTickUtils.hasScheduled(NoRotateSet)
        val isCancelRotationActive = WaitTickUtils.hasScheduled(CancelRotationTicker)
        val isFailCancelActive = WaitTickUtils.hasScheduled(FailRotationCancelTicker)

        if (isNoRotateSetActive) {
            yawDiff = 0F
            pitchDiff = 0F
        } else if (isShortStopActive || isCancelRotationActive || isFailCancelActive || activeSettings?.shouldPerformShortStop() == true) {
            if (!isShortStopActive && !isCancelRotationActive && !isFailCancelActive) {
                val duration = activeSettings?.shortStopDuration?.random()?.plus(1) ?: 0
                WaitTickUtils.schedule(duration, this)
            }

            activeSettings?.resetSimulateShortStopData()

            val yawSlowdown = (0F..0.1F).random()
            val pitchSlowdown = (0F..0.1F).random()

            yawDiff = (yawDiff * yawSlowdown).withGCD()
            pitchDiff = (pitchDiff * pitchSlowdown).withGCD()
        }

        var (straightLineYaw, straightLinePitch) = run {
            var baseYawSpeed = abs(yawDiff safeDiv rotationDifference) * hSpeed
            var basePitchSpeed = abs(pitchDiff safeDiv rotationDifference) * vSpeed

            // Apply imperfect correlation
            if (legitimize) {
                baseYawSpeed *= legitimizeFactor.random()
                basePitchSpeed *= legitimizeFactor.random()
            }

            baseYawSpeed to basePitchSpeed
        }

        straightLineYaw = yawDiff.coerceIn(-straightLineYaw, straightLineYaw)
        straightLinePitch = pitchDiff.coerceIn(-straightLinePitch, straightLinePitch)

        // Humans usually have some small jitter when moving their mouse from point A to point B.
        // Usually when a rotation axis' difference is prioritized.
        if (rotationDifference > 0F) {
            val yawJitter = (-0.03F..0.03F).random() * straightLineYaw
            val pitchJitter = (-0.02F..0.02F).random() * straightLinePitch

            straightLineYaw += yawJitter
            straightLinePitch += pitchJitter
        }

        val minYaw = nextFloat(min(minRotationDiff, getFixedAngleDelta()), minRotationDiff).withGCD()
        val minPitch = nextFloat(min(minRotationDiff, getFixedAngleDelta()), minRotationDiff).withGCD()

        applySlowDown(straightLineYaw, minYaw, minRotationDiffResetTiming, true, legitimize) {
            straightLineYaw = it
        }

        applySlowDown(straightLinePitch, minPitch, minRotationDiffResetTiming, false, legitimize) {
            straightLinePitch = it
        }

        return currentRotation.plus(Rotation(straightLineYaw, straightLinePitch))
    }

    private fun applySlowDown(
        diff: Float, min: Float, timing: String, yaw: Boolean, applyRealism: Boolean, action: (Float) -> Unit
    ) {
        if (diff == 0f) {
            action(diff)
            return
        }

        val lastTick1 = angleDifferences(serverRotation, lastRotations[1]).let { diffs ->
            if (yaw) diffs.x else diffs.y
        }

        val diffAbs = abs(diff)
        val isSlowingDown = diffAbs <= abs(lastTick1)

        if (diffAbs.withGCD() <= min && (timing == "Always" || timing == "OnSlowDown" && isSlowingDown || timing == "OnStart" && lastTick1 == 0F)) {
            action(0f)
            return
        }

        if (!applyRealism) {
            action(diff)
            return
        }

        val range = when {
            lastTick1 == 0f -> {
                val inc = 0.2f * (diffAbs / 50f).coerceIn(0f, 1f)

                0.1F + inc..0.5F + inc
            }

            else -> 0.3f..0.7f
        }

        val new = (lastTick1..diff).lerpWith(range.random())

        if (abs(new.withGCD()) <= min && isSlowingDown) {
            action(diff)
        } else {
            action(new)
        }
    }

    /**
     * Calculate difference between two angle points
     *
     * @param a angle point
     * @param b angle point
     * @return difference between angle points
     */
    fun angleDifference(a: Float, b: Float) = MathHelper.wrapAngleTo180_float(a - b)

    /**
     * Returns a 2-parameter vector with the calculated angle differences between [target] and [current] rotations
     */
    fun angleDifferences(target: Rotation, current: Rotation) =
        Vector2f(angleDifference(target.yaw, current.yaw), target.pitch - current.pitch)

    /**
     * Calculate rotation to vector
     *
     * @param [yaw] [pitch] your rotation
     * @return target vector
     */
    fun getVectorForRotation(yaw: Float, pitch: Float): Vec3 {
        val yawRad = yaw.toRadians()
        val pitchRad = pitch.toRadians()

        val f = MathHelper.cos(-yawRad - PI.toFloat())
        val f1 = MathHelper.sin(-yawRad - PI.toFloat())
        val f2 = -MathHelper.cos(-pitchRad)
        val f3 = MathHelper.sin(-pitchRad)

        return Vec3((f1 * f2).toDouble(), f3.toDouble(), (f * f2).toDouble())
    }

    fun getVectorForRotation(rotation: Rotation) = getVectorForRotation(rotation.yaw, rotation.pitch)

    /**
     * Returns the inverted yaw angle.
     *
     * @param yaw The original yaw angle in degrees.
     * @return The yaw angle inverted by 180 degrees.
     */
    fun invertYaw(yaw: Float): Float {
        return (yaw + 180) % 360
    }

    /**
     * Allows you to check if your crosshair is over your target entity
     *
     * @param targetEntity       your target entity
     * @param blockReachDistance your reach
     * @return if crosshair is over target
     */
    fun isFaced(targetEntity: Entity, blockReachDistance: Double) =
        raycastEntity(blockReachDistance) { entity: Entity -> targetEntity == entity } != null

    /**
     * Allows you to check if your crosshair is over your target entity
     *
     * @param targetEntity       your target entity
     * @param blockReachDistance your reach
     * @return if crosshair is over target
     */
    fun isRotationFaced(targetEntity: Entity, blockReachDistance: Double, rotation: Rotation) = raycastEntity(
        blockReachDistance, rotation.yaw, rotation.pitch
    ) { entity: Entity -> targetEntity == entity } != null

    /**
     * Allows you to check if your enemy is behind a wall
     */
    fun isVisible(vec3: Vec3) = mc.theWorld.rayTraceBlocks(mc.thePlayer.eyes, vec3) == null

    fun isEntityHeightVisible(entity: Entity) = arrayOf(
        entity.hitBox.center.withY(entity.hitBox.maxY), entity.hitBox.center.withY(entity.hitBox.minY)
    ).any { isVisible(it) }

    fun isEntityHeightVisible(entity: TileEntity) = arrayOf(
        entity.renderBoundingBox.center.withY(entity.renderBoundingBox.maxY),
        entity.renderBoundingBox.center.withY(entity.renderBoundingBox.minY)
    ).any { isVisible(it) }

    /**
     * Set your target rotation
     *
     * @param rotation your target rotation
     */
    fun setTargetRotation(rotation: Rotation, options: RotationSettings, ticks: Int = options.resetTicks) {
        if (options.failRotationEnabled.get() &&
            options.failRotationMode == "Cancel" &&
            options.currentFailedRotation != null) {
            options.currentFailedRotation = null
            return
        }

        if (rotation.yaw.isNaN() || rotation.pitch.isNaN() || rotation.pitch > 90 || rotation.pitch < -90) {
            return
        }

        if (!options.prioritizeRequest && activeSettings?.prioritizeRequest == true) {
            return
        }

        if (!options.applyServerSide) {
            currentRotation?.let {
                mc.thePlayer.rotationYaw = it.yaw
                mc.thePlayer.rotationPitch = it.pitch
            }
            resetRotation()
        }

        val shouldCancelInBorder = options.cancelRotationInBorder.get() && mc.pointedEntity != null
        val isCancelScheduled = WaitTickUtils.hasScheduled(CancelRotationTicker)

        if (shouldCancelInBorder && !isCancelScheduled) {
            WaitTickUtils.schedule(1, CancelRotationTicker)
        }

        val isFailCancelScheduled = WaitTickUtils.hasScheduled(FailRotationCancelTicker)

        if (!isCancelScheduled && !isFailCancelScheduled) {
            targetRotation = options.maybeApplyFailRotation(rotation, mc.thePlayer)
            lastValidRotation = targetRotation
            lastValidRotationTime = System.currentTimeMillis()
        } else {
            targetRotation = lastValidRotation ?: currentRotation ?: serverRotation
        }

        resetTicks = if (!options.applyServerSide || !options.resetTicksValue.isSupported()) 1 else ticks

        activeSettings = options

        if (options.immediate) {
            update()
        }
    }
    private fun resetRotation() {
        resetTicks = 0
        currentRotation?.let { (yaw, _) ->
            mc.thePlayer?.let {
                it.rotationYaw = yaw + angleDifference(it.rotationYaw, yaw)
                syncRotations()
            }
        }
        targetRotation = null
        currentRotation = null
        activeSettings = null
    }

    /**
     * Returns the smallest angle difference possible with a specific sensitivity ("gcd")
     */
    fun getFixedAngleDelta(sensitivity: Float = mc.gameSettings.mouseSensitivity) =
        (sensitivity * 0.6f + 0.2f).pow(3) * 1.2f

    /**
     * Returns angle that is legitimately accomplishable with player's current sensitivity
     */
    fun getFixedSensitivityAngle(targetAngle: Float, startAngle: Float = 0f, gcd: Float = getFixedAngleDelta()) =
        startAngle + ((targetAngle - startAngle) / gcd).roundToInt() * gcd

    /**
     * Creates a raytrace even when the target [blockPos] is not visible
     */
    fun performRaytrace(
        blockPos: BlockPos,
        rotation: Rotation,
        reach: Float = mc.playerController.blockReachDistance,
    ): MovingObjectPosition? {
        val world = mc.theWorld ?: return null
        val player = mc.thePlayer ?: return null

        val eyes = player.eyes

        return blockPos.block?.collisionRayTrace(
            world, blockPos, eyes, eyes + (getVectorForRotation(rotation) * reach.toDouble())
        )
    }

    fun performRayTrace(blockPos: BlockPos, vec: Vec3, eyes: Vec3 = mc.thePlayer.eyes) =
        mc.theWorld?.let { blockPos.block?.collisionRayTrace(it, blockPos, eyes, vec) }

    fun syncRotations() {
        val player = mc.thePlayer ?: return

        player.prevRotationYaw = player.rotationYaw
        player.prevRotationPitch = player.rotationPitch
        player.renderArmYaw = player.rotationYaw
        player.renderArmPitch = player.rotationPitch
        player.prevRenderArmYaw = player.rotationYaw
        player.prevRotationPitch = player.rotationPitch
    }

    private fun update() {
        val settings = activeSettings ?: return
        val player = mc.thePlayer ?: return

        val playerRotation = player.rotation

        val shouldUpdate = !InventoryUtils.serverOpenContainer && !InventoryUtils.serverOpenInventory

        if (!shouldUpdate) {
            return
        }

        val serverRotation = currentRotation ?: serverRotation

        currentRotation?.let { patternPrediction.addRotationData(it.yaw, it.pitch) }

        if (resetTicks == 0) {
            if (isDifferenceAcceptableForReset(serverRotation, playerRotation, settings)) {
                resetRotation()
                return
            }

            currentRotation = limitAngleChange(
                serverRotation, playerRotation, settings
            ).fixedSensitivity()
            return
        }

        targetRotation?.let {
            limitAngleChange(serverRotation, it, settings,randomizationSettings).let { rotation ->
                if (!settings.applyServerSide) {
                    rotation.toPlayer(player)
                } else {
                    currentRotation = rotation.fixedSensitivity()
                }
            }
        }

        if (resetTicks > 0) {
            resetTicks--
        }
    }
    private fun isDifferenceAcceptableForReset(
        curr: Rotation, target: Rotation, options: RotationSettings
    ): Boolean {
        if (!options.applyServerSide) return true

        if (rotationDifference(target, curr) > options.angleResetDifference) return false

        // We use the last rotation saved 2 ticks ago because we have not updated the currentRotation yet.
        val diffs = angleDifferences(target, curr).abs
        val lastTickDiffs = angleDifferences(curr, lastRotations[1]).abs

        return diffs.x <= lastTickDiffs.x && diffs.y <= lastTickDiffs.y || !options.legitimize
    }

    /**
     * Any module that modifies the server packets without using the [currentRotation] should use on module disable.
     */
    fun syncSpecialModuleRotations() {
        serverRotation.let { (yaw, _) ->
            mc.thePlayer?.let {
                it.rotationYaw = yaw + angleDifference(it.rotationYaw, yaw)
                syncRotations()
            }
        }
    }

    /**
     * Checks if the rotation difference is not the same as the smallest GCD angle possible.
     */
    fun canUpdateRotation(current: Rotation, target: Rotation, multiplier: Int = 1): Boolean {
        if (current == target) return true

        val smallestAnglePossible = getFixedAngleDelta()

        return rotationDifference(target, current).withGCD() > smallestAnglePossible * multiplier
    }

    /**
     * Handle rotation update
     */
    val onRotationUpdate = handler<RotationUpdateEvent>(priority = -1) {
        activeSettings?.let {
            // Was the rotation update immediate? Allow updates the next tick.
            if (it.immediate) {
                it.immediate = false
                return@handler
            }
        }

        update()
    }

    /**
     * Handle strafing
     */
    val onStrafe = handler<StrafeEvent> { event ->
        val data = activeSettings ?: return@handler

        if (!data.strafe) {
            return@handler
        }

        currentRotation?.let {
            it.applyStrafeToPlayer(event, data.strict)
            event.cancelEvent()
        }
    }

    /**
     * Handle rotation-packet modification
     */
    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet !is C03PacketPlayer) {
            return@handler
        }

        if (!packet.rotating) {
            activeSettings?.resetSimulateShortStopData()
            return@handler
        }

        currentRotation?.let { packet.rotation = it }

        val diffs = angleDifferences(packet.rotation, serverRotation)

        if (Rotations.debugRotations && currentRotation != null) {
            chat("PREV YAW: ${diffs.x}, PREV PITCH: ${diffs.y}")
        }

        activeSettings?.updateSimulateShortStopData(diffs.x)
    }

    enum class BodyPoint(val rank: Int, val range: ClosedFloatingPointRange<Double>, val displayName: String) {
        HEAD(1, 0.75..0.9, "Head"), BODY(0, 0.5..0.75, "Body"), FEET(-1, 0.1..0.4, "Feet"), UNKNOWN(
            -2, 0.0..0.0, "Unknown"
        );

        companion object {
            fun fromString(point: String): BodyPoint {
                return entries.find { it.name.equals(point, ignoreCase = true) } ?: UNKNOWN
            }
        }
    }

    fun coerceBodyPoint(point: BodyPoint, minPoint: BodyPoint, maxPoint: BodyPoint): BodyPoint {
        return when {
            point.rank < minPoint.rank -> minPoint
            point.rank > maxPoint.rank -> maxPoint
            else -> point
        }
    }
    private fun getAngleDifference(a: Float, b: Float): Float {
        return ((a - b) % 360f + 540f) % 360f - 180f
    }

    fun bezierRotationTo(
        targetRotation: Rotation,
        currentRotation: Rotation,
        settings: RotationSettings,
        yawStep: Float,
        pitchStep: Float
    ): Rotation {
        return bezierRotation.bezierRotationTo(targetRotation, currentRotation, settings, yawStep, pitchStep)
    }

    /**
     * Bezier blending function
     */
    fun dynamicBezierRotationTo(
        targetRotation: Rotation,
        currentRotation: Rotation,
        settings: RotationSettings,
        player: EntityPlayer,
        target: Entity
    ): Rotation {
        return bezierRotation.dynamicBezierRotationTo(targetRotation, currentRotation, settings, player, target)
    }

    /**
     * Fatigue effect calculation
     */
    private fun fatigueRotation(
        currentYaw: Float,
        yawOutput: Float,
        currentPitch: Float,
        pitchOutput: Float,
        yawMaxSpeed: Float,
        pitchMaxSpeed: Float
    ): Rotation {
        val targetYaw = currentYaw + yawOutput.coerceIn(-yawMaxSpeed, yawMaxSpeed)
        var yawDiff = targetYaw - currentYaw

        val targetPitch = currentPitch + pitchOutput.coerceIn(-pitchMaxSpeed, pitchMaxSpeed)
        var pitchDiff = targetPitch - currentPitch

        var fatigueFactorPitch = 0f
        var fatigueFactorYaw = 0f

        if (abs(yawDiff) >= yawMaxSpeed) {
            val t = abs(yawOutput % yawMaxSpeed) / abs(yawMaxSpeed)
            val fatigueFactor = 1 - t
            yawDiff *= fatigueFactor
            fatigueFactorPitch = abs(yawDiff / yawMaxSpeed)
        }

        if (abs(pitchDiff) >= pitchMaxSpeed) {
            val t = abs(pitchOutput % pitchMaxSpeed) / abs(pitchMaxSpeed)
            val fatigueFactor = 1 - t
            pitchDiff *= fatigueFactor
            fatigueFactorYaw = abs(pitchDiff / pitchMaxSpeed)
        }

        return Rotation(yawDiff + fatigueFactorYaw, pitchDiff + fatigueFactorPitch)
    }
    /**
     * PID控制平滑旋转
     */
    fun smoothRotationPID(
        targetRotation: Rotation,
        currentRotation: Rotation,
        settings: RotationSettings
    ): Rotation {
        if (!settings.pidEnabled.get()) {
            return targetRotation
        }

        val proportionalGain = settings.pidProportionalGain.get() / 1000f
        val integralGain = settings.pidIntegralGain.get() / 1000f
        val derivativeGain = settings.pidDerivativeGain.get() / 1000f

        // 计算当前误差
        val yawError = angleDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchError = targetRotation.pitch - currentRotation.pitch

        // 比例项
        var yawOutput = proportionalGain * yawError
        var pitchOutput = proportionalGain * pitchError

        val deltaTime = 0.05f
        // 积分项
        settings.pidYawIntegral += yawError * deltaTime
        settings.pidPitchIntegral += pitchError * deltaTime
        yawOutput += integralGain * settings.pidYawIntegral
        pitchOutput += integralGain * settings.pidPitchIntegral

        // 微分项
        val yawDerivative = (yawError - settings.pidPrevYawError) / deltaTime
        val pitchDerivative = (pitchError - settings.pidPrevPitchError) / deltaTime
        yawOutput += derivativeGain * yawDerivative
        pitchOutput += derivativeGain * pitchDerivative

        // 保存当前误差用于下次计算
        settings.pidPrevYawError = yawError
        settings.pidPrevPitchError = pitchError

        // 平滑因子
        val pidSMYawF = 1f - settings.pidSmoothYaw.get()
        val pidSMPitchF = 1f - settings.pidSmoothPitch.get()

        // 应用平滑
        yawOutput = yawOutput * pidSMYawF + (yawOutput + settings.pidPrevYawError) * pidSMYawF
        pitchOutput = pitchOutput * pidSMPitchF + (pitchOutput + settings.pidPrevPitchError) * pidSMPitchF

        // 应用速度限制和疲劳效果
        val newRotationDiff = fatigueRotation(
            currentRotation.yaw, yawOutput,
            currentRotation.pitch, pitchOutput,
            settings.horizontalAngleChange.random(),
            settings.verticalAngleChange.random()
        )

        return Rotation(
            currentRotation.yaw + newRotationDiff.yaw,
            currentRotation.pitch + newRotationDiff.pitch
        )
    }
    fun getRotations(posX: Double, posY: Double, posZ: Double): Rotation {
        val player = mc.thePlayer
        val x = posX - player.posX
        val y = posY - (player.posY + player.getEyeHeight())
        val z = posZ - player.posZ
        val dist = MathHelper.sqrt_double(x * x + z * z)
        val yaw = (atan2(z, x) * 180.0 / Math.PI - 90).toFloat()
        val pitch = (-(atan2(y, dist.toDouble()) * 180.0 / Math.PI)).toFloat()
        return Rotation(yaw, pitch)
    }
    fun getRotationsEntity(entity: EntityLivingBase): Rotation {
        return getRotations(entity.posX, entity.posY + entity.eyeHeight - 0.4, entity.posZ)
    }
    private fun handleCenterMode(
        currentRotation: Rotation,
        targetRotation: Rotation,
        settings: RotationSettings
    ): Rotation {

        if (settings.instant) {
            return targetRotation
        }

        return performAngleChange(
            currentRotation,
            targetRotation,
            settings.horizontalSpeed,
            settings.verticalSpeed,
            settings.legitimize,
            settings.legitimizeFactor.range,
            settings.minRotationDifference,
            settings.minRotationDifferenceResetTiming
        )
    }
    /**
     * 计算智能手动转头乘数 - 使用数学函数
     */
    private fun calculateSmartManualMultiplier(
        manualDiff: Vector2f,
        target: Rotation,
        settings: RotationSettings,
        targetEntity: Entity? = null
    ): Float {
        if (targetEntity == null) return 0f

        val manualMagnitude = hypot(abs(manualDiff.x), abs(manualDiff.y))
        val maxDeviation = settings.manualMixMaxDeviation.get()
        val baseStrength = settings.manualMixStrength.get().random()

        // 1. 基于转头大小的衰减函数（数学曲线）
        val magnitudeFactor = when (settings.manualMixCurve.get()) {
            "Linear" -> 1f - (manualMagnitude / maxDeviation).coerceIn(0f, 1f)
            "Quadratic" -> 1f - (manualMagnitude / maxDeviation).pow(2).coerceIn(0f, 1f)
            "Exponential" -> exp(-manualMagnitude / (maxDeviation * 0.5f)).coerceIn(0f, 1f)
            "Sigmoid" -> 1f / (1f + exp((manualMagnitude - maxDeviation * 0.5f) / 10f))
            else -> 1f - (manualMagnitude / maxDeviation).coerceIn(0f, 1f)
        }

        // 2. Hitbox安全系数 - 现在传入正确的参数
        val hitboxSafetyFactor = if (settings.manualMixRequireHitbox.get()) {
            calculateContinuousHitboxSafety(target, targetEntity, manualDiff)
        } else {
            // 如果不要求必须在碰撞箱内，则返回一个基于距离的安全系数
            calculateDistanceBasedSafety(targetEntity)
        }

        // 3. 距离衰减函数
        val distance = mc.thePlayer.getDistanceToEntity(targetEntity)
        val distanceFactor = exp(-distance / 8f).coerceIn(0.3f, 1f)

        return baseStrength * magnitudeFactor * hitboxSafetyFactor * distanceFactor
    }

    /**
     * 连续化的hitbox安全系数计算
     */
    private fun calculateContinuousHitboxSafety(
        target: Rotation,  // 使用 target 作为基准
        targetEntity: Entity,
        manualDiff: Vector2f  // manualDiff 是相对于 target 的差异
    ): Float {
        val bb = targetEntity.entityBoundingBox
        val eyes = mc.thePlayer.eyes

        // 测试多个应用比例来找到最大安全比例
        val testFactors = listOf(1.0f, 0.7f, 0.5f, 0.3f, 0.1f)

        for (factor in testFactors) {
            val testRot = Rotation(
                // 以 target 为基准，加上手动偏移
                target.yaw + manualDiff.x * factor,
                target.pitch + manualDiff.y * factor
            )
            val testVec = eyes + getVectorForRotation(testRot) * 6.0
            val intercept = bb.calculateIntercept(eyes, testVec)

            if (intercept != null) {
                return factor
            }
        }

        return 0f
    }

    /**
     * 智能手动转头混合
     */
    private fun handleSmartManualRotationMix(
        target: Rotation,
        settings: RotationSettings,
    ): Rotation {
        if (!settings.manualRotationMixEnabled.get()) {
            return target
        }

        val targetEntity = mc.pointedEntity ?: return target

        // 获取鼠标原始输入
        val mouseDeltaX = Mouse.getDX().toFloat()
        val mouseDeltaY = Mouse.getDY().toFloat()

        // 如果鼠标没有移动，直接返回目标旋转
        if (mouseDeltaX == 0f && mouseDeltaY == 0f) return target

        // 将鼠标增量转换为角度变化（考虑灵敏度）
        val sensitivity = mc.gameSettings.mouseSensitivity
        val gcd = getFixedAngleDelta(sensitivity)
        val mouseYawDelta = (mouseDeltaX * sensitivity * 0.6f).withGCD(gcd)
        val mousePitchDelta = (-mouseDeltaY * sensitivity * 0.6f).withGCD(gcd)

        val manualDiff = Vector2f(mouseYawDelta, mousePitchDelta)

        // 计算智能乘数
        val smartMultiplier = calculateSmartManualMultiplier(manualDiff, target, settings, targetEntity)

        // 如果乘数为0，直接返回目标旋转
        if (smartMultiplier <= 0.01f) {
            return target
        }

        // 应用有限的手动转头影响
        val influencedYaw = target.yaw + mouseYawDelta * smartMultiplier
        val influencedPitch = (target.pitch + mousePitchDelta * smartMultiplier).coerceIn(-90f, 90f)

        return Rotation(influencedYaw, influencedPitch).fixedSensitivity()
    }
    private fun calculateDistanceBasedSafety(targetEntity: Entity): Float {
        val distance = mc.thePlayer.getDistanceToEntity(targetEntity)
        return exp(-distance / 12f).coerceIn(0.2f, 1f)
    }
    private fun handleAllowExcessiveRotation(
        currentRotation: Rotation,
        targetRotation: Rotation,
        settings: RotationSettings
    ): Rotation {
        if (!settings.allowExcessiveRotation.get()) {
            return targetRotation
        }

        val (yawDiff, pitchDiff) = angleDifferences(targetRotation, currentRotation)

        val overshootFactor = settings.excessiveRotationCorrectionSpeed.random
        val maxAngle = settings.excessiveRotationMaxAngle.get()

        val overshootYaw = yawDiff * overshootFactor
        val overshootPitch = pitchDiff * overshootFactor

        val limitedYaw = overshootYaw.coerceIn(-maxAngle, maxAngle)
        val limitedPitch = overshootPitch.coerceIn(-maxAngle, maxAngle)

        return Rotation(
            currentRotation.yaw + limitedYaw,
            (currentRotation.pitch + limitedPitch).coerceIn(-90f, 90f)
        )
    }


}
private fun Float.withGCD(gcd: Float = getFixedAngleDelta()): Float {
    return if (abs(this) < gcd) 0f else (this / gcd).roundToInt() * gcd
}

fun Double.toDegreesF(): Float = Math.toDegrees(this).toFloat()
fun Float.toDegreesF(): Float = Math.toDegrees(this.toDouble()).toFloat()
fun Float.toRadians(): Float = Math.toRadians(this.toDouble()).toFloat()
fun Double.toRadians(): Double = Math.toRadians(this)

fun Float.pow(n: Int): Float = this.toDouble().pow(n).toFloat()
fun Float.pow(x: Float): Float = this.toDouble().pow(x.toDouble()).toFloat()
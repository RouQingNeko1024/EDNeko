package net.ccbluex.liquidbounce.utils.rotation

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import kotlin.math.*
import kotlin.random.Random

// ==================== 数据模型 ====================

data class RotationPoint(val yaw: Float, val pitch: Float, val time: Float = 0f)
data class RotationCurve(val points: MutableList<RotationPoint> = mutableListOf())
data class PlayerState(val isMoving: Boolean, val isClicking: Boolean, val isStationary: Boolean)
data class StateParams(
    val amplification: Float, val reduction: Float, val multiplier: Float,
    val decay: Float, val seed: Float, val amplificationMax: Float,
    val amplificationMin: Float, val decayMin: Float, val decayMax: Float,
    val multiplierMax: Float
)
data class Layer2Params(
    val fovSlowdown: Float, val fovRestoreSpeed: Float, val curveRestoreSpeed: Float,
    val inertiaDecay: Float, val noiseAmplitude: Float, val noiseSeed: Int,
    val noiseMax: Float, val noiseDecayRate: Float
)
data class SmoothParams(
    val smoothAmplifyMultiplier: Float, val smoothDecayMultiplier: Float,
    val movingAmplifyMultiplier: Float, val movingDecayMultiplier: Float,
    val stationaryAmplifyMultiplier: Float, val stationaryDecayMultiplier: Float
)
data class SimulationParams(
    val timeDecay: Float, val decayMin: Float, val decayMax: Float,
    val decayLowest: Float, val decayHighest: Float,
    val idleBoost: Float, val idleBoostMax: Float, val idleBoostMin: Float,
    val idleBoostHighest: Float, val amplifyType: String, val decayType: String
)

private fun wrapAngleDegrees(angle: Float): Float {
    var result = angle % 360f
    if (result < -180f) result += 360f
    if (result > 180f) result -= 360f
    return result
}

// ==================== 策略接口 ====================

interface RandomizationMode {
    val name: String
    fun process(
        current: Rotation,
        target: Rotation,
        config: NekoNoiseConfig
    ): Rotation
}

class NoneRandomization : RandomizationMode {
    override val name = "None"
    override fun process(current: Rotation, target: Rotation, config: NekoNoiseConfig): Rotation = target
}

class LegitimizeRandomization : RandomizationMode {
    override val name = "Legitimize"
    override fun process(current: Rotation, target: Rotation, config: NekoNoiseConfig): Rotation {
        val factor = config.legitimizeFactor
        val yawDiff = target.yaw - current.yaw
        val pitchDiff = target.pitch - current.pitch
        return Rotation(
            current.yaw + yawDiff * factor,
            current.pitch + pitchDiff * factor
        )
    }
}

// ==================== NekoNoise 完整流水线 ====================

class NekoNoiseRandomization : RandomizationMode {
    override val name = "NekoNoise"
    private val rng = Random(System.currentTimeMillis())
    private var tickCounter = 0L
    private var lastAttackTick = 0L
    private var predictionCurve = RotationCurve()
    private var rotationCurve = RotationCurve()
    private var previousRotation = Rotation(0f, 0f)
    private var inertiaAccumulator = 0f
    private var lazinessAccumulator = 0f
    private var noisePhase = 0f
    private var fovSlowdownActive = false
    private var fovSlowdownAmount = 0f
    private var targetEntity: Entity? = null
    private var eyePos: Vec3 = Vec3(0.0, 0.0, 0.0)

    fun getStateParams(config: NekoNoiseConfig, isMoving: Boolean, isClicking: Boolean, isStationary: Boolean): StateParams {
        return when {
            isClicking -> StateParams(
                config.clickAmplification, config.clickReduction, config.clickMultiplier,
                config.clickDecay, config.clickSeed, config.clickAmplificationMax,
                config.clickAmplificationMin, config.clickDecayMin, config.clickDecayMax,
                config.clickMultiplierMax
            )
            isMoving -> StateParams(
                config.movingAmplification, config.movingReduction, config.movingMultiplier,
                config.movingDecay, config.movingSeed, config.movingAmplificationMax,
                config.movingAmplificationMin, config.movingDecayMin, config.movingDecayMax,
                config.movingMultiplierMax
            )
            isStationary -> StateParams(
                config.stationaryAmplification, config.stationaryReduction, config.stationaryMultiplier,
                config.stationaryDecay, config.stationarySeed, config.stationaryAmplificationMax,
                config.stationaryAmplificationMin, config.stationaryDecayMin, config.stationaryDecayMax,
                config.stationaryMultiplierMax
            )
            else -> StateParams(
                config.movingAmplification, config.movingReduction, config.movingMultiplier,
                config.movingDecay, config.movingSeed, config.movingAmplificationMax,
                config.movingAmplificationMin, config.movingDecayMin, config.movingDecayMax,
                config.movingMultiplierMax
            )
        }
    }

    override fun process(current: Rotation, target: Rotation, config: NekoNoiseConfig): Rotation {
        tickCounter++
        val player = RotationUtils.mc.thePlayer
        val isMoving = player?.isMoving ?: false
        val isStationary = !isMoving
        val isClicking = false
        val state = PlayerState(isMoving, isClicking, isStationary)

        // 更新视线起点
        eyePos = if (player != null) {
            Vec3(player.posX, player.posY + player.getEyeHeight().toDouble(), player.posZ)
        } else {
            Vec3(0.0, 0.0, 0.0)
        }

        // 获取当前瞄准目标实体
        targetEntity = RotationUtils.targetRotation?.let {
            RotationUtils.mc.theWorld?.playerEntities?.firstOrNull { e ->
                val rot = RotationUtils.toRotation(RotationUtils.getCenter(e.entityBoundingBox), true)
                abs(wrapAngleDegrees(rot.yaw - target.yaw)) < 1f &&
                        abs(rot.pitch - target.pitch) < 1f
            }
        }

        // ===== 层 1：计算转头 =====
        val rotationDiff = layer1ComputeRotation(current, target)

        // ===== 层 2：计算预判位置 =====
        val predictedTarget = layer2Prediction(target, config)

        // ===== 层 3：计算出旋转曲线与预判初始旋转曲线 =====
        val initCurves = layer3InitialCurves(rotationDiff, predictedTarget, config)

        // ===== 层 4：第一层随机化预判曲线 =====
        val randomizedPrediction = layer4RandomizePrediction(initCurves.second, state, config)

        // ===== 层 5：旋转曲线按照预判曲线拟合 =====
        val fittedCurve = layer5FitToPrediction(initCurves.first, randomizedPrediction, config)

        // ===== 层 6：计算出距离目标优势攻击点 =====
        val bestPoint = layer6AdvantagePoints(fittedCurve, target, config)

        // ===== 层 7：旋转曲线拟合最近点 =====
        val refittedCurve = layer7FitToBestPoint(fittedCurve, bestPoint, config)

        // ===== 层 8：第二层随机化旋转曲线 =====
        val layer2Randomized = layer8SecondRandomize(refittedCurve, state, config)

        // ===== 层 9：第二次平滑旋转曲线 =====
        val smoothedCurve = layer9SecondSmooth(layer2Randomized, state, config)

        // ===== 层 10：第一次仿真化 =====
        val simulatedCurve = layer10Simulate(smoothedCurve, config)

        // ===== 层 11：随机化旋转曲线路径 =====
        val candidates = layer11MultiCurve(simulatedCurve, config)

        // ===== 层 12：选择曲线路径 =====
        val selectedCurve = layer12SelectCurve(candidates, target, config)

        // ===== 层 13：真正转头 =====
        val finalRotation = layer13ApplyRotation(selectedCurve, current, config)

        previousRotation = finalRotation
        return finalRotation
    }

    // ─── 层 1：计算转头 ───
    private fun layer1ComputeRotation(current: Rotation, target: Rotation): RotationDiff {
        val yawDiff = wrapAngleDegrees(target.yaw - current.yaw)
        val pitchDiff = (target.pitch - current.pitch).coerceIn(-90f, 90f)
        return RotationDiff(yawDiff, pitchDiff)
    }

    // ─── 层 2：计算预判位置 ───
    private fun layer2Prediction(target: Rotation, config: NekoNoiseConfig): Rotation {
        if (!config.predictionEnabled) return target

        val conservative = config.predictionConservative.coerceIn(0f, 1f)
        val multiplier = config.predictionMultiplier

        val entity = targetEntity ?: return target

        // 根据目标移动速度和方向计算预判位置
        val velocityX = (entity.posX - entity.prevPosX)
        val velocityY = (entity.posY - entity.prevPosY)
        val velocityZ = (entity.posZ - entity.prevPosZ)

        val speed = sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ).toFloat()

        // 保守程度影响预判：保守程度越高，预判越小
        val predictionAmount = speed * multiplier * (1f - conservative)

        if (predictionAmount < 0.001f) return target

        // 计算预判后的位置偏移（转为旋转角度偏移）
        val player = RotationUtils.mc.thePlayer ?: return target
        val distX = entity.posX - player.posX
        val distZ = entity.posZ - player.posZ
        val distY = (entity.posY + entity.eyeHeight) - (player.posY + player.getEyeHeight())
        val distXZ = sqrt(distX * distX + distZ * distZ).toFloat()

        // 将速度转为角度预判
        val yawPrediction = if (distXZ > 0.001f) {
            val perpSpeed = (velocityX * (-distZ / distXZ) + velocityZ * (distX / distXZ)).toFloat()
            atan2(perpSpeed * predictionAmount.toDouble(), distXZ.toDouble()).toDegreesF()
        } else 0f

        val pitchPrediction = if (distXZ > 0.001f) {
            val radialSpeed = (velocityX * (distX / distXZ) + velocityZ * (distZ / distXZ)).toFloat()
            atan2((velocityY * predictionAmount).toDouble(), distXZ.toDouble()).toDegreesF()
        } else 0f

        return Rotation(
            target.yaw + yawPrediction,
            (target.pitch + pitchPrediction).coerceIn(-90f, 90f)
        )
    }

    // ─── 层 3：初始曲线 ───
    private fun layer3InitialCurves(
        diff: RotationDiff,
        predicted: Rotation,
        config: NekoNoiseConfig
    ): Pair<RotationCurve, RotationCurve> {
        val rotCurve = RotationCurve()
        val predCurve = RotationCurve()
        val steps = 3
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            rotCurve.points.add(RotationPoint(diff.yaw * t, diff.pitch * t, t))
            predCurve.points.add(RotationPoint(predicted.yaw * t, predicted.pitch * t, t))
        }
        return Pair(rotCurve, predCurve)
    }

    // ─── 层 4：第一层随机化预判曲线 ───
    private fun layer4RandomizePrediction(
        curve: RotationCurve,
        state: PlayerState,
        config: NekoNoiseConfig
    ): RotationCurve {
        val result = RotationCurve()
        val sp = getStateParams(config, state.isMoving, state.isClicking, state.isStationary)
        val noiseSeed = (sp.seed.toInt() + tickCounter.toInt() + config.noiseSeed.toInt()).toInt()
        val localRng = Random(noiseSeed)

        // 惰性计算：惰性百分数 * 基准转头速度 = 转头速度01
        val lazinessFactor = (config.laziness / 100f).coerceIn(0f, 1f)
        lazinessAccumulator = lazinessFactor

        // 惯性计算：惯性百分数 * 转头速度01 = 转头速度02
        val rawInertiaPct = (config.inertia / 100f).coerceIn(0f, 1f)
        val maxInertiaPct = config.maxInertia / 100f
        val inertiaPct = if (rawInertiaPct > maxInertiaPct) maxInertiaPct else rawInertiaPct
        inertiaAccumulator += (inertiaPct - inertiaAccumulator) * 0.1f

        // 状态参数应用
        val stateAmplification = sp.amplification.coerceIn(sp.amplificationMin, sp.amplificationMax)
        val stateDecay = sp.decay.coerceIn(sp.decayMin, sp.decayMax)
        val stateMultiplier = sp.multiplier.coerceAtMost(sp.multiplierMax)
        val stateReduction = sp.reduction

        for (point in curve.points) {
            // 噪声抖动：基础抖动 * 状态增幅 * 状态倍率
            val baseNoiseJitter = if (config.noiseJitter > 0f) {
                (localRng.nextFloat() - 0.5f) * config.noiseJitter * 2f
            } else 0f
            val noiseJitter = baseNoiseJitter * stateAmplification * stateMultiplier

            // 攻击时噪声抖动增幅
            val attackBoost = if (state.isClicking) config.attackNoiseJitterAmplification else 1f
            val attackLazyBoost = if (state.isClicking) config.attackLazinessAmplification else 1f
            val attackLazyReduce = if (state.isClicking) config.attackLazinessReduction else 1f
            val nonAttackLazyReduce = if (!state.isClicking) config.nonAttackLazinessReduction else 1f

            val baseSpeed = 1f
            val speed01 = baseSpeed * (1f - lazinessFactor * attackLazyBoost * attackLazyReduce * nonAttackLazyReduce)
            val speed02 = speed01 * (1f + inertiaAccumulator)

            // 应用状态衰减到速度
            val speedWithDecay = speed02 * (1f - stateDecay) * (1f / stateReduction.coerceAtLeast(0.001f))

            val randomizedYaw = point.yaw + noiseJitter * attackBoost * speedWithDecay
            val randomizedPitch = point.pitch + noiseJitter * attackBoost * speedWithDecay

            result.points.add(RotationPoint(randomizedYaw, randomizedPitch, point.time))
        }
        return result
    }

    // ─── 层 5：旋转曲线按照预判曲线拟合 ───
    private fun layer5FitToPrediction(
        rotCurve: RotationCurve,
        predCurve: RotationCurve,
        config: NekoNoiseConfig
    ): RotationCurve {
        val result = RotationCurve()
        val maxPoints = maxOf(rotCurve.points.size, predCurve.points.size)
        for (i in 0 until maxPoints) {
            val rot = rotCurve.points.getOrElse(i) { rotCurve.points.last() }
            val pred = predCurve.points.getOrElse(i) { predCurve.points.last() }
            val blend = config.predictionBlend.coerceIn(0f, 1f)
            result.points.add(RotationPoint(
                rot.yaw * (1f - blend) + pred.yaw * blend,
                rot.pitch * (1f - blend) + pred.pitch * blend,
                rot.time
            ))
        }
        return result
    }

    // ─── 层 6：计算优势攻击点 A/B/C ───
    private fun layer6AdvantagePoints(
        curve: RotationCurve,
        target: Rotation,
        config: NekoNoiseConfig
    ): RotationPoint {
        val entity = targetEntity
        val totalYaw = curve.points.lastOrNull()?.yaw ?: 0f
        val absYaw = abs(totalYaw)

        val threshold90 = config.threshold90
        val threshold20 = config.threshold20
        val hitboxScale = config.hitboxScale.coerceIn(0.1f, 1f)

        if (entity == null) {
            return RotationPoint(target.yaw, target.pitch)
        }

        return when {
            absYaw >= threshold90 -> {
                // 点 C：视线射线与目标碰撞体的最近命中点
                computePointC(entity, hitboxScale)
            }
            absYaw < threshold20 -> {
                // 点 A：离视点最近的点（优先）
                computePointA(entity, hitboxScale)
            }
            else -> {
                // 点 B：转头代价最小的点
                computePointB(entity, hitboxScale)
            }
        }
    }

    private fun computePointA(entity: Entity, hitboxScale: Float): RotationPoint {
        // A. 离视点最近的点：AABB/Capsule 上到 eyePos 的最近点
        val bb = entity.entityBoundingBox
        val scaledBb = scaleBoundingBox(bb, hitboxScale)

        // 计算 AABB 上离 eyePos 最近的点
        val closestX = eyePos.xCoord.coerceIn(scaledBb.minX, scaledBb.maxX)
        val closestY = eyePos.yCoord.coerceIn(scaledBb.minY, scaledBb.maxY)
        val closestZ = eyePos.zCoord.coerceIn(scaledBb.minZ, scaledBb.maxZ)

        val closestPoint = Vec3(closestX, closestY, closestZ)
        val diffX = closestPoint.xCoord - eyePos.xCoord
        val diffY = closestPoint.yCoord - eyePos.yCoord
        val diffZ = closestPoint.zCoord - eyePos.zCoord
        val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)

        val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f
        val pitch = -Math.toDegrees(atan2(diffY, diffXZ)).toFloat()

        return RotationPoint(wrapAngleDegrees(yaw), pitch.coerceIn(-90f, 90f))
    }

    private fun computePointB(entity: Entity, hitboxScale: Float): RotationPoint {
        // B. 转头代价最小的点：目标表面采样点中，使当前视线转到它所需角度最小的那个
        val bb = entity.entityBoundingBox
        val scaledBb = scaleBoundingBox(bb, hitboxScale)

        val player = RotationUtils.mc.thePlayer
        val currentYaw = player?.rotationYaw ?: 0f
        val currentPitch = player?.rotationPitch ?: 0f

        // 采样 AABB 上的关键点（8个角 + 6个面中心）
        val samplePoints = mutableListOf<Vec3>()
        val minX = scaledBb.minX; val maxX = scaledBb.maxX
        val minY = scaledBb.minY; val maxY = scaledBb.maxY
        val minZ = scaledBb.minZ; val maxZ = scaledBb.maxZ
        val midX = (minX + maxX) / 2.0; val midY = (minY + maxY) / 2.0; val midZ = (minZ + maxZ) / 2.0

        // 8 corners
        for (x in listOf(minX, maxX))
            for (y in listOf(minY, maxY))
                for (z in listOf(minZ, maxZ))
                    samplePoints.add(Vec3(x, y, z))

        // 6 face centers
        samplePoints.add(Vec3(midX, midY, minZ))
        samplePoints.add(Vec3(midX, midY, maxZ))
        samplePoints.add(Vec3(midX, minY, midZ))
        samplePoints.add(Vec3(midX, maxY, midZ))
        samplePoints.add(Vec3(minX, midY, midZ))
        samplePoints.add(Vec3(maxX, midY, midZ))

        var bestPoint = samplePoints[0]
        var bestAngle = Float.MAX_VALUE

        for (sp in samplePoints) {
            val diffX = sp.xCoord - eyePos.xCoord
            val diffY = sp.yCoord - eyePos.yCoord
            val diffZ = sp.zCoord - eyePos.zCoord
            val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)

            val spYaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f
            val spPitch = -Math.toDegrees(atan2(diffY, diffXZ)).toFloat()

            val yawDiff = abs(wrapAngleDegrees(spYaw - currentYaw))
            val pitchDiff = abs(spPitch - currentPitch)
            val totalAngle = yawDiff + pitchDiff

            if (totalAngle < bestAngle) {
                bestAngle = totalAngle
                bestPoint = sp
            }
        }

        val diffX = bestPoint.xCoord - eyePos.xCoord
        val diffY = bestPoint.yCoord - eyePos.yCoord
        val diffZ = bestPoint.zCoord - eyePos.zCoord
        val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)

        val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f
        val pitch = -Math.toDegrees(atan2(diffY, diffXZ)).toFloat()

        return RotationPoint(wrapAngleDegrees(yaw), pitch.coerceIn(-90f, 90f))
    }

    private fun computePointC(entity: Entity, hitboxScale: Float): RotationPoint {
        // C. 视线射线与目标碰撞体的最近命中点：沿当前视线方向最近的命中点
        val bb = entity.entityBoundingBox
        val scaledBb = scaleBoundingBox(bb, hitboxScale)

        val player = RotationUtils.mc.thePlayer ?: return RotationPoint(0f, 0f)
        val lookVec = player.getLookVec()

        // 沿着视线方向找与 AABB 的交点
        var tMin = 0.0
        var tMax = Double.MAX_VALUE

        // 获取碰撞箱沿视线的进入/离开距离
        val invDirX = if (abs(lookVec.xCoord) < 1e-7) Double.MAX_VALUE else 1.0 / lookVec.xCoord
        val invDirY = if (abs(lookVec.yCoord) < 1e-7) Double.MAX_VALUE else 1.0 / lookVec.yCoord
        val invDirZ = if (abs(lookVec.zCoord) < 1e-7) Double.MAX_VALUE else 1.0 / lookVec.zCoord

        val t1x = (scaledBb.minX - eyePos.xCoord) * invDirX
        val t2x = (scaledBb.maxX - eyePos.xCoord) * invDirX
        val t1y = (scaledBb.minY - eyePos.yCoord) * invDirY
        val t2y = (scaledBb.maxY - eyePos.yCoord) * invDirY
        val t1z = (scaledBb.minZ - eyePos.zCoord) * invDirZ
        val t2z = (scaledBb.maxZ - eyePos.zCoord) * invDirZ

        val txMin = minOf(t1x, t2x); val txMax = maxOf(t1x, t2x)
        val tyMin = minOf(t1y, t2y); val tyMax = maxOf(t1y, t2y)
        val tzMin = minOf(t1z, t2z); val tzMax = maxOf(t1z, t2z)

        tMin = maxOf(maxOf(txMin, tyMin), tzMin)
        tMax = minOf(minOf(txMax, tyMax), tzMax)

        if (tMin <= tMax && tMin > 0) {
            val hitPoint = eyePos.addVector(lookVec.xCoord * tMin, lookVec.yCoord * tMin, lookVec.zCoord * tMin)
            val diffX = hitPoint.xCoord - eyePos.xCoord
            val diffY = hitPoint.yCoord - eyePos.yCoord
            val diffZ = hitPoint.zCoord - eyePos.zCoord
            val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)

            val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f
            val pitch = -Math.toDegrees(atan2(diffY, diffXZ)).toFloat()
            return RotationPoint(wrapAngleDegrees(yaw), pitch.coerceIn(-90f, 90f))
        }

        // Fallback: use bounding box center
        val center = RotationUtils.getCenter(scaledBb)
        val diffX = center.xCoord - eyePos.xCoord
        val diffY = center.yCoord - eyePos.yCoord
        val diffZ = center.zCoord - eyePos.zCoord
        val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)

        val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f
        val pitch = -Math.toDegrees(atan2(diffY, diffXZ)).toFloat()
        return RotationPoint(wrapAngleDegrees(yaw), pitch.coerceIn(-90f, 90f))
    }

    private fun scaleBoundingBox(bb: AxisAlignedBB, scale: Float): AxisAlignedBB {
        val centerX = (bb.minX + bb.maxX) / 2.0
        val centerY = (bb.minY + bb.maxY) / 2.0
        val centerZ = (bb.minZ + bb.maxZ) / 2.0
        val halfW = (bb.maxX - bb.minX) / 2.0 * scale
        val halfH = (bb.maxY - bb.minY) / 2.0 * scale
        val halfD = (bb.maxZ - bb.minZ) / 2.0 * scale
        return AxisAlignedBB(
            centerX - halfW, centerY - halfH, centerZ - halfD,
            centerX + halfW, centerY + halfH, centerZ + halfD
        )
    }

    // ─── 层 7：旋转曲线拟合最近点 ───
    private fun layer7FitToBestPoint(
        curve: RotationCurve,
        bestPoint: RotationPoint,
        config: NekoNoiseConfig
    ): RotationCurve {
        val result = RotationCurve()
        val lastPoint = curve.points.lastOrNull() ?: return curve
        val n = curve.points.size
        for ((idx, point) in curve.points.withIndex()) {
            val t = idx.toFloat() / maxOf(n - 1, 1)
            val adjustedYaw = point.yaw + (bestPoint.yaw - lastPoint.yaw) * t
            val adjustedPitch = point.pitch + (bestPoint.pitch - lastPoint.pitch) * t
            result.points.add(RotationPoint(adjustedYaw, adjustedPitch, point.time))
        }
        return result
    }

    // ─── 层 8：第二层随机化旋转曲线 ───
    private fun layer8SecondRandomize(
        curve: RotationCurve,
        state: PlayerState,
        config: NekoNoiseConfig
    ): RotationCurve {
        val result = RotationCurve()
        val sp = getStateParams(config, state.isMoving, state.isClicking, state.isStationary)

        // FOV 离开范围减缓转头速度01并恢复
        val fovFactor = if (fovSlowdownActive) {
            (1f - config.fovSlowdownAmount).coerceIn(0f, 1f)
        } else 1f

        // 恢复 FOV 减缓
        if (!fovSlowdownActive) {
            fovSlowdownAmount = (fovSlowdownAmount * 0.8f).coerceAtLeast(0f)
        }

        // 状态参数应用 - 第二层
        val stateAmplification = sp.amplification.coerceIn(sp.amplificationMin, sp.amplificationMax)
        val stateDecay = sp.decay.coerceIn(sp.decayMin, sp.decayMax)
        val stateMultiplier = sp.multiplier.coerceAtMost(sp.multiplierMax)
        val stateReduction = sp.reduction

        for (point in curve.points) {
            // 噪声抖动叠加 - 各状态独立叠加
            val movingNoise = if (state.isMoving) config.movingNoiseJitter * stateAmplification else 0f
            val clickingNoise = if (state.isClicking) config.clickingNoiseJitter * stateAmplification else 0f
            val rotatingNoise = config.rotatingNoiseJitter
            val jitterNoise = if (abs(point.yaw) > 0.01f || abs(point.pitch) > 0.01f) {
                config.jitterNoiseIncrease * stateAmplification
            } else 0f

            noisePhase += 0.1f * stateMultiplier
            val totalNoise = (sin(noisePhase) * rotatingNoise + movingNoise + clickingNoise + jitterNoise)
                .coerceIn(-config.noiseMaxValue, config.noiseMaxValue)

            // 噪声衰减曲线
            val noiseDecay = totalNoise * exp(-config.noiseDecayCurve * point.time * (1f + stateDecay))

            // 转弯方向约束：仅正 XZ 轴惯性
            // 根据需转头角度计算应有惯性
            val neededAngle = abs(point.yaw)
            val inertiaBase = inertiaAccumulator * config.inertiaDecayRate
            val inertiaEffect = point.yaw * inertiaBase * stateMultiplier

            // 应用状态缩小
            val speedFactor = fovFactor * (1f / stateReduction.coerceAtLeast(0.001f))
            val randomizedYaw = (point.yaw + noiseDecay) * speedFactor + inertiaEffect
            val randomizedPitch = (point.pitch + noiseDecay * 0.5f) * speedFactor

            result.points.add(RotationPoint(randomizedYaw, randomizedPitch, point.time))
        }
        return result
    }

    // ─── 层 9：第二次平滑旋转曲线 ───
    private fun layer9SecondSmooth(
        curve: RotationCurve,
        state: PlayerState,
        config: NekoNoiseConfig
    ): RotationCurve {
        val result = RotationCurve()
        val amplifyBase = config.smoothAmplifyMultiplier
        val decayBase = config.smoothDecayMultiplier
        val movingAmplify = if (state.isMoving) config.movingSmoothAmplifyMultiplier else 1f
        val movingDecay = if (state.isMoving) config.movingSmoothDecayMultiplier else 1f
        val stationaryAmplify = if (state.isStationary) config.stationarySmoothAmplifyMultiplier else 1f
        val stationaryDecay = if (state.isStationary) config.stationarySmoothDecayMultiplier else 1f

        val totalAmplify = amplifyBase * movingAmplify * stationaryAmplify
        val totalDecay = decayBase * movingDecay * stationaryDecay

        var smoothedYaw = 0f
        var smoothedPitch = 0f
        for (point in curve.points) {
            val alpha = 1f - totalDecay
            smoothedYaw = smoothedYaw * alpha + point.yaw * totalAmplify * (1f - alpha)
            smoothedPitch = smoothedPitch * alpha + point.pitch * totalAmplify * (1f - alpha)
            result.points.add(RotationPoint(smoothedYaw, smoothedPitch, point.time))
        }
        return result
    }

    // ─── 层 10：第一次仿真化 ───
    private fun layer10Simulate(
        curve: RotationCurve,
        config: NekoNoiseConfig
    ): RotationCurve {
        val result = RotationCurve()

        // 时间推移转头速度衰减
        val timeSinceStart = tickCounter * 0.05f
        val timeDecayFactor = (config.simTimeDecay * timeSinceStart)
            .coerceIn(config.simDecayMin, config.simDecayMax)
        val decaySpeed = config.simDecayLowest +
                (config.simDecayHighest - config.simDecayLowest) * timeDecayFactor

        // 未攻击/远离目标的增幅
        val ticksSinceAttack = (tickCounter - lastAttackTick).coerceAtLeast(0)
        val idleFactor = if (ticksSinceAttack > 20) {
            val boost = config.simIdleBoost.coerceIn(config.simIdleBoostMin, config.simIdleBoostMax)
            boost.coerceAtMost(config.simIdleBoostHighest)
        } else 0f

        for (point in curve.points) {
            val decayedYaw: Float
            val decayedPitch: Float

            when (config.simDecayType) {
                "divide" -> {
                    decayedYaw = point.yaw / (1f + decaySpeed)
                    decayedPitch = point.pitch / (1f + decaySpeed)
                }
                else -> {
                    decayedYaw = point.yaw - decaySpeed * sign(point.yaw)
                    decayedPitch = point.pitch - decaySpeed * sign(point.pitch)
                }
            }

            val boostedYaw: Float
            val boostedPitch: Float
            when (config.simAmplifyType) {
                "multiply" -> {
                    boostedYaw = decayedYaw * (1f + idleFactor)
                    boostedPitch = decayedPitch * (1f + idleFactor)
                }
                else -> {
                    boostedYaw = decayedYaw + idleFactor * sign(decayedYaw)
                    boostedPitch = decayedPitch + idleFactor * sign(decayedPitch)
                }
            }

            result.points.add(RotationPoint(boostedYaw, boostedPitch, point.time))
        }
        return result
    }

    // ─── 层 11：随机化旋转曲线路径 ───
    private fun layer11MultiCurve(
        curve: RotationCurve,
        config: NekoNoiseConfig
    ): List<RotationCurve> {
        val candidates = mutableListOf<RotationCurve>()
        val count = config.curveCount.coerceIn(1, 10)

        for (c in 0 until count) {
            val variant = RotationCurve()
            val localSeed = config.curveSeed + c * 9973
            val localRng = Random(localSeed)

            for (point in curve.points) {
                val noiseYaw = (localRng.nextFloat() - 0.5f) * config.curveNoiseAmplitude
                val noisePitch = (localRng.nextFloat() - 0.5f) * config.curveNoiseAmplitude
                variant.points.add(RotationPoint(
                    point.yaw + noiseYaw,
                    point.pitch + noisePitch,
                    point.time
                ))
            }
            candidates.add(variant)
        }
        return candidates
    }

    // ─── 层 12：选择曲线路径 ───
    private fun layer12SelectCurve(
        candidates: List<RotationCurve>,
        target: Rotation,
        config: NekoNoiseConfig
    ): RotationCurve {
        if (candidates.isEmpty()) return RotationCurve()
        if (candidates.size == 1) return candidates[0]

        return when (config.curveSelectStrategy) {
            "closest" -> candidates.minByOrNull { curve ->
                val last = curve.points.lastOrNull() ?: return@minByOrNull Float.MAX_VALUE
                val dy = last.yaw - target.yaw
                val dp = last.pitch - target.pitch
                dy * dy + dp * dp
            } ?: candidates[0]
            "smoothest" -> candidates.minByOrNull { curve ->
                var roughness = 0f
                var prev = curve.points.firstOrNull() ?: return@minByOrNull Float.MAX_VALUE
                for (i in 1 until curve.points.size) {
                    val curr = curve.points[i]
                    roughness += abs(curr.yaw - prev.yaw) + abs(curr.pitch - prev.pitch)
                    prev = curr
                }
                roughness
            } ?: candidates[0]
            "first" -> candidates[0]
            else -> candidates[candidates.size / 2]
        }
    }

    // ─── 层 13：真正转头 ───
    private fun layer13ApplyRotation(
        curve: RotationCurve,
        current: Rotation,
        config: NekoNoiseConfig
    ): Rotation {
        val point = curve.points.firstOrNull() ?: return current
        val newYaw = current.yaw + point.yaw
        val newPitch = (current.pitch + point.pitch).coerceIn(-90f, 90f)
        return Rotation(newYaw, newPitch)
    }

    fun notifyAttack() { lastAttackTick = tickCounter }
    fun notifyFovLeave() { fovSlowdownActive = true; fovSlowdownAmount = 1f }
    fun notifyFovEnter() { fovSlowdownActive = false }

    data class RotationDiff(val yaw: Float, val pitch: Float)
}

// ==================== 完整配置 ====================

class NekoNoiseConfig(owner: Module, val generalApply: () -> Boolean = { true }) : Configurable("NekoNoise") {

    // ─── 模式选择 ───
    val randomizationModeValue = choices(
        "Mode", arrayOf("None", "Legitimize", "NekoNoise"), "NekoNoise"
    ) { generalApply() }
    val randomizationMode: String get() = randomizationModeValue.get()

    // ─── Legitimize 参数 ───
    val legitimizeFactor by float("Legitimize/Factor", 1f, 0.1f..2f) {
        randomizationMode == "Legitimize" && generalApply()
    }

    // ==================== 状态矩阵：移动时 ====================
    val movingAmplification by float("Moving/Amplification", 1f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val movingReduction by float("Moving/Reduction", 1f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val movingMultiplier by float("Moving/Multiplier", 1f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val movingDecay by float("Moving/Decay", 0.1f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val movingSeed by float("Moving/Seed", 42f, 0f..100000f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val movingAmplificationMax by float("Moving/AmplificationMax", 5f, 0f..20f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val movingAmplificationMin by float("Moving/AmplificationMin", 0.1f, 0f..5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val movingDecayMin by float("Moving/DecayMin", 0.01f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val movingDecayMax by float("Moving/DecayMax", 0.5f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val movingMultiplierMax by float("Moving/MultiplierMax", 5f, 0f..20f) {
        randomizationMode == "NekoNoise" && generalApply()
    }

    // ==================== 状态矩阵：静止时 ====================
    val stationaryAmplification by float("Stationary/Amplification", 1f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val stationaryReduction by float("Stationary/Reduction", 1f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val stationaryMultiplier by float("Stationary/Multiplier", 1f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val stationaryDecay by float("Stationary/Decay", 0.1f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val stationarySeed by float("Stationary/Seed", 42f, 0f..100000f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val stationaryAmplificationMax by float("Stationary/AmplificationMax", 5f, 0f..20f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val stationaryAmplificationMin by float("Stationary/AmplificationMin", 0.1f, 0f..5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val stationaryDecayMin by float("Stationary/DecayMin", 0.01f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val stationaryDecayMax by float("Stationary/DecayMax", 0.5f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val stationaryMultiplierMax by float("Stationary/MultiplierMax", 5f, 0f..20f) {
        randomizationMode == "NekoNoise" && generalApply()
    }

    // ==================== 状态矩阵：点击时 ====================
    val clickAmplification by float("Clicking/Amplification", 1.5f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val clickReduction by float("Clicking/Reduction", 0.8f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val clickMultiplier by float("Clicking/Multiplier", 1.2f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val clickDecay by float("Clicking/Decay", 0.05f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val clickSeed by float("Clicking/Seed", 42f, 0f..100000f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val clickAmplificationMax by float("Clicking/AmplificationMax", 8f, 0f..20f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val clickAmplificationMin by float("Clicking/AmplificationMin", 0.5f, 0f..5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val clickDecayMin by float("Clicking/DecayMin", 0.01f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val clickDecayMax by float("Clicking/DecayMax", 0.3f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val clickMultiplierMax by float("Clicking/MultiplierMax", 8f, 0f..20f) {
        randomizationMode == "NekoNoise" && generalApply()
    }

    // ==================== 预判参数 ====================
    val predictionEnabled by boolean("Prediction/Enabled", true) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val predictionConservative by float("Prediction/Conservative", 0.5f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val predictionMultiplier by float("Prediction/Multiplier", 1f, 0f..5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val predictionBlend by float("Prediction/Blend", 0.5f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }

    // ==================== 第一层随机化参数 ====================
    val noiseJitter by float("Layer1/NoiseJitter", 0.5f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val noiseSeed by float("Layer1/NoiseSeed", 12345f, 0f..100000f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val inertia by float("Layer1/Inertia", 0f, 0f..100f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val laziness by float("Layer1/Laziness", 0f, 0f..100f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val attackNoiseJitterAmplification by float("Layer1/AttackNoiseJitterAmplification", 2f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val attackLazinessAmplification by float("Layer1/AttackLazinessAmplification", 1.5f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val attackLazinessReduction by float("Layer1/AttackLazinessReduction", 0.5f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val nonAttackLazinessReduction by float("Layer1/NonAttackLazinessReduction", 1f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val maxInertia by float("Layer1/MaxInertia", 80f, 0f..100f) {
        randomizationMode == "NekoNoise" && generalApply()
    }

    // ==================== 优势攻击点参数 ====================
    val threshold90 by float("Advantage/Threshold90", 90f, 0f..180f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val threshold20 by float("Advantage/Threshold20", 20f, 0f..180f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val hitboxScale by float("Advantage/HitboxScale", 0.8f, 0.1f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }

    // ==================== 第二层随机化参数 ====================
    val fovSlowdownAmount by float("Layer2/FovSlowdownAmount", 0.5f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val movingNoiseJitter by float("Layer2/MovingNoiseJitter", 0.2f, 0f..5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val clickingNoiseJitter by float("Layer2/ClickingNoiseJitter", 0.5f, 0f..5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val rotatingNoiseJitter by float("Layer2/RotatingNoiseJitter", 0.3f, 0f..5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val jitterNoiseIncrease by float("Layer2/JitterNoiseIncrease", 0.1f, 0f..5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val noiseMaxValue by float("Layer2/NoiseMaxValue", 5f, 0f..20f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val noiseDecayCurve by float("Layer2/NoiseDecayCurve", 0.5f, 0f..5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val inertiaDecayRate by float("Layer2/InertiaDecayRate", 0.1f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }

    // ==================== 平滑层参数 ====================
    val smoothAmplifyMultiplier by float("Smooth/AmplifyMultiplier", 1.2f, 0f..5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val smoothDecayMultiplier by float("Smooth/DecayMultiplier", 0.8f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val movingSmoothAmplifyMultiplier by float("Smooth/MovingAmplifyMultiplier", 1f, 0f..5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val movingSmoothDecayMultiplier by float("Smooth/MovingDecayMultiplier", 1f, 0f..5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val stationarySmoothAmplifyMultiplier by float("Smooth/StationaryAmplifyMultiplier", 1f, 0f..5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val stationarySmoothDecayMultiplier by float("Smooth/StationaryDecayMultiplier", 1f, 0f..5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }

    // ==================== 仿真化参数 ====================
    val simTimeDecay by float("Simulation/TimeDecay", 0.01f, 0f..0.1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val simDecayMin by float("Simulation/DecayMin", 0f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val simDecayMax by float("Simulation/DecayMax", 0.5f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val simDecayLowest by float("Simulation/DecayLowest", 0f, 0f..0.5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val simDecayHighest by float("Simulation/DecayHighest", 0.3f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val simIdleBoost by float("Simulation/IdleBoost", 0.1f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val simIdleBoostMax by float("Simulation/IdleBoostMax", 0.5f, 0f..1f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val simIdleBoostMin by float("Simulation/IdleBoostMin", 0f, 0f..0.5f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val simIdleBoostHighest by float("Simulation/IdleBoostHighest", 0.8f, 0f..2f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val simAmplifyType by choices("Simulation/AmplifyType", arrayOf("multiply", "add"), "multiply") {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val simDecayType by choices("Simulation/DecayType", arrayOf("divide", "subtract"), "subtract") {
        randomizationMode == "NekoNoise" && generalApply()
    }

    // ==================== 路径层参数 ====================
    val curveCount by int("Curve/Count", 3, 1..10) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val curveSeed by int("Curve/Seed", 42, 0..100000) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val curveNoiseAmplitude by float("Curve/NoiseAmplitude", 0.5f, 0f..10f) {
        randomizationMode == "NekoNoise" && generalApply()
    }
    val curveSelectStrategy by choices("Curve/SelectStrategy", arrayOf("closest", "smoothest", "first", "random"), "closest") {
        randomizationMode == "NekoNoise" && generalApply()
    }
}

// ==================== 工厂 ====================

object RandomizationModeFactory {
    private val modes = mutableMapOf<String, RandomizationMode>()

    init {
        register(NoneRandomization())
        register(LegitimizeRandomization())
        register(NekoNoiseRandomization())
    }

    fun register(mode: RandomizationMode) { modes[mode.name] = mode }
    fun get(name: String): RandomizationMode = modes[name] ?: NoneRandomization()
    fun getNekoNoise(): NekoNoiseRandomization = get("NekoNoise") as NekoNoiseRandomization
}
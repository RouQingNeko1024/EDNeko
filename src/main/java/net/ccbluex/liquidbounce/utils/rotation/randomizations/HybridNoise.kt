package net.ccbluex.liquidbounce.utils.rotation.randomizations

import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.random
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.math.FastMathUtil.cos
import net.ccbluex.liquidbounce.utils.rotation.RandomizationSettings
import net.minecraft.client.Minecraft
import java.util.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class HybridNoise(private val settings: RandomizationSettings) {
    private val mc = Minecraft.getMinecraft()!!
    private var lastJitterTime = 0L
    private var lastAimStartTime = System.currentTimeMillis()
    private val noiseRandom = Random()
    private val lastHybridNoise = floatArrayOf(0f, 0f)

    fun getHybridNoiseOffset(axis: Int, distanceToTarget: Float = 0f): Float {
        val timeBase = System.currentTimeMillis() / 1000f

        val fatigueFactor = if (System.currentTimeMillis() - lastAimStartTime > 10000) {
            1.8f
        } else if (System.currentTimeMillis() - lastAimStartTime > 5000) {
            1.4f
        } else {
            1f
        }

        var combinedNoise = 0f
        repeat(settings.hybridNoiseLayerCount.random()) { layer ->
            val layerWeight = 1f / (layer + 1)

            val mixRatio = settings.hybridNoiseMixRatio.random().coerceIn(0.001f, 0.999f)
            val brownian = brownianMotionNoise(
                mean = settings.hybridNoiseMean.random() * fatigueFactor,
                sigma = settings.hybridNoiseSigma.random() * (layer + 1),
                dt = settings.hybridNoiseDt.random()
            ) * (1 - mixRatio)

            val ou = ouProcessNoise(
                current = if (layer == 0) lastHybridNoise[axis] else combinedNoise,
                mean = settings.hybridNoiseMean.random() * fatigueFactor,
                theta = settings.hybridNoiseTheta.random(),
                sigma = settings.hybridNoiseSigma.random(),
                dt = settings.hybridNoiseDt.random()
            ) * mixRatio

            combinedNoise += (brownian + ou) * layerWeight
        }

        val modulated = when (settings.hybridDynamicModulation) {
            "Sine" -> {
                val phase = timeBase * settings.hybridModulationFrequency.random() * PI.toFloat() * 2
                combinedNoise * (1 + settings.hybridModulationAmplitude.random() * sin(phase))
            }
            "Sawtooth" -> {
                val phase = (timeBase * settings.hybridModulationFrequency.random()) % 1f
                combinedNoise * (1 + settings.hybridModulationAmplitude.random() * (phase * 2 - 1))
            }
            "Square" -> {
                val phase = ((timeBase * settings.hybridModulationFrequency.random()) % 1f) > 0.5f
                combinedNoise * (1 + settings.hybridModulationAmplitude.random() * if (phase) 1f else -1f)
            }
            else -> {
                if ((timeBase * settings.hybridModulationFrequency.random() % 1f) < 0.1f) {
                    combinedNoise * (1 + settings.hybridModulationAmplitude.random() * (noiseRandom.nextFloat() * 2 - 1))
                } else {
                    combinedNoise
                }
            }
        } * fatigueFactor

        val spectralNoise = settings.applySpectralProcessing(modulated, axis, timeBase)

        val coupledNoise = when {
            axis == 0 && nextFloat() < settings.hybridAxisCoupling.random() ->
                spectralNoise * (1 + noiseRandom.nextFloat() * settings.hybridAxisCoupling.random())
            axis == 1 && nextFloat() < settings.hybridAxisCoupling.random() * 0.5f ->
                spectralNoise * (1 - noiseRandom.nextFloat() * settings.hybridAxisCoupling.random() * 0.3f)
            else -> spectralNoise
        }

        val scaledNoise = settings.hybridNoiseScale.random().let { scale ->
            if (scale > 5f) scale * 1.2f else scale * 0.8f
        }
        var result = coupledNoise * scaledNoise

        val distanceFactor = if (distanceToTarget > 0f) {
            val minDist = settings.hybridDistanceRangeMin.random()
            val maxDist = settings.hybridDistanceRangeMax.random()
            val minMulti = settings.hybridDistanceFactorMin.random()
            val maxMulti = settings.hybridDistanceFactorMax.random()

            when {
                distanceToTarget <= minDist -> minMulti
                distanceToTarget >= maxDist -> maxMulti
                else -> {
                    val t = (distanceToTarget - minDist) / (maxDist - minDist)
                    settings.lerp(minMulti, maxMulti, t)
                }
            }
        } else {
            1.0f
        }

        result *= distanceFactor
        result = when (axis) {
            0 -> result
            1 -> result / settings.hybridYawPitchRatio.random().coerceAtLeast(0.1f)
            else -> 0f
        }

        result += (noiseRandom.nextGaussian().toFloat() * settings.hybridMicroJitter.random())

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastJitterTime > 1000 &&
            nextFloat() < settings.hybridJitterProbability.random()) {
            lastJitterTime = currentTime
            result += (noiseRandom.nextGaussian().toFloat() * settings.hybridJitterStrength.random())
        }

        if (!mc.thePlayer.isMoving) {
            result *= 1.2f.coerceAtMost(1f + settings.hybridMicroJitter.random())
        }

        val smoothFactor = (1f / (settings.hybridSmoothness.random() * fatigueFactor)).coerceIn(0.05f, 0.95f)
        lastHybridNoise[axis] = settings.lerp(
            lastHybridNoise[axis],
            result,
            smoothFactor
        )

        if (mc.thePlayer.isMoving) {
            lastAimStartTime = System.currentTimeMillis()
        }

        return lastHybridNoise[axis].coerceIn(-30f, 30f)
    }

    private fun brownianMotionNoise(mean: Float, sigma: Float, dt: Float): Float {
        val u = nextFloat()
        val z = sqrt(dt.toDouble()) * sqrt(-2.0 * kotlin.math.ln(u.toDouble())) *
                cos((2.0 * PI * nextFloat().toDouble()).toFloat())
        return (z * sigma.toDouble() + mean.toDouble()).toFloat()
    }

    private fun ouProcessNoise(current: Float, mean: Float, theta: Float, sigma: Float, dt: Float): Float {
        val drift = theta * (mean - current) * dt
        val diffusion = sigma * sqrt(dt.toDouble()).toFloat() * gaussianRandom(0f, 1f)
        return drift + diffusion
    }

    private fun gaussianRandom(mean: Float, sigma: Float): Float {
        val u1 = nextFloat()
        val u2 = nextFloat()
        val z0 = sqrt(-2.0 * kotlin.math.ln(u1.toDouble())) * cos((2.0 * PI * u2.toDouble()).toFloat())
        return (z0 * sigma.toDouble() + mean.toDouble()).toFloat()
    }
}

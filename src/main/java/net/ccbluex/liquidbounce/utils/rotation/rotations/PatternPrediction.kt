package net.ccbluex.liquidbounce.utils.rotation.rotations

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.activeSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.angleDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.minecraft.client.Minecraft
import java.io.*
import kotlin.math.abs
import kotlin.math.tanh
import kotlin.random.Random

class PatternPrediction {

    private val mc = Minecraft.getMinecraft()

    val loadedPatterns = mutableListOf<RotationPattern>()
    val rotationHistory = mutableListOf<RotationData>()
    val patternMatchThreshold = 0.15f
    var lastPrediction: Rotation? = null
    var lastPredictionTime: Long = 0
    val predictionCacheDuration: Long = 50

    // 平滑过渡相关变量
    private var transitionProgress: Float = 0f
    private var predictionActive: Boolean = false
    private val transitionDuration: Long = 300 // 300ms过渡时间
    private var transitionStartTime: Long = 0
    private var lastSmoothRotation: Rotation? = null
    private var predictionStartRotation: Rotation? = null

    @Volatile
    private var patternsLoaded = false
    private var lastModified: Long = 0
    private var fileSize: Long = 0

    private val neuralNetwork = SimpleAimNeuralNetwork()
    private var nnTrained = false
    private var nnInitialized = false

    private val trainingData = mutableListOf<TrainingSample>()
    private var lastTrainingTime = 0L
    private var totalSamplesCollected = 0

    data class TrainingSample(
        val inputs: FloatArray,
        val outputs: FloatArray
    ) : Serializable {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as TrainingSample
            if (!inputs.contentEquals(other.inputs)) return false
            if (!outputs.contentEquals(other.outputs)) return false
            return true
        }
        override fun hashCode(): Int {
            var result = inputs.contentHashCode()
            result = 31 * result + outputs.contentHashCode()
            return result
        }
    }

    data class NeuralNetworkModel(
        val weights1: Array<FloatArray>,
        val bias1: FloatArray,
        val weights2: Array<FloatArray>,
        val bias2: FloatArray,
        val trainingSamples: Int
    ) : Serializable {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as NeuralNetworkModel
            if (!weights1.contentDeepEquals(other.weights1)) return false
            if (!bias1.contentEquals(other.bias1)) return false
            if (!weights2.contentDeepEquals(other.weights2)) return false
            if (!bias2.contentEquals(other.bias2)) return false
            if (trainingSamples != other.trainingSamples) return false
            return true
        }
        override fun hashCode(): Int {
            var result = weights1.contentDeepHashCode()
            result = 31 * result + bias1.contentHashCode()
            result = 31 * result + weights2.contentDeepHashCode()
            result = 31 * result + bias2.contentHashCode()
            result = 31 * result + trainingSamples
            return result
        }
    }

    inner class SimpleAimNeuralNetwork {
        private var inputSize = 7
        private var hiddenSize = 8
        private var outputSize = 2
        private var learningRate = 0.01f

        private lateinit var weights1: Array<FloatArray>
        private lateinit var bias1: FloatArray
        private lateinit var weights2: Array<FloatArray>
        private lateinit var bias2: FloatArray

        fun initialize(hiddenSize: Int, learningRate: Float) {
            this.hiddenSize = hiddenSize
            this.learningRate = learningRate

            weights1 = Array(hiddenSize) { FloatArray(inputSize) { Random.nextFloat() * 2f - 1f } }
            bias1 = FloatArray(hiddenSize) { Random.nextFloat() * 0.1f }
            weights2 = Array(outputSize) { FloatArray(hiddenSize) { Random.nextFloat() * 2f - 1f } }
            bias2 = FloatArray(outputSize) { Random.nextFloat() * 0.1f }

            nnInitialized = true
        }

        private fun relu(x: Float): Float = if (x > 0) x else 0.01f * x
        private fun reluDerivative(x: Float): Float = if (x > 0) 1f else 0.01f

        fun forward(inputs: FloatArray): FloatArray {
            if (!nnInitialized || !nnTrained) return FloatArray(outputSize)

            val hidden = FloatArray(hiddenSize)
            for (i in 0 until hiddenSize) {
                var sum = bias1[i]
                for (j in 0 until inputSize) {
                    sum += inputs[j] * weights1[i][j]
                }
                hidden[i] = relu(sum)
            }

            val outputs = FloatArray(outputSize)
            for (i in 0 until outputSize) {
                var sum = bias2[i]
                for (j in 0 until hiddenSize) {
                    sum += hidden[j] * weights2[i][j]
                }
                outputs[i] = tanh(sum)
            }

            return outputs
        }

        fun trainSingle(inputs: FloatArray, targets: FloatArray) {
            if (!nnInitialized) return

            val hidden = FloatArray(hiddenSize)
            val hiddenInputs = FloatArray(hiddenSize)
            for (i in 0 until hiddenSize) {
                var sum = bias1[i]
                for (j in 0 until inputSize) {
                    sum += inputs[j] * weights1[i][j]
                }
                hiddenInputs[i] = sum
                hidden[i] = relu(sum)
            }

            val outputs = FloatArray(outputSize)
            val outputInputs = FloatArray(outputSize)
            for (i in 0 until outputSize) {
                var sum = bias2[i]
                for (j in 0 until hiddenSize) {
                    sum += hidden[j] * weights2[i][j]
                }
                outputInputs[i] = sum
                outputs[i] = tanh(sum)
            }

            val outputErrors = FloatArray(outputSize)
            val outputDeltas = FloatArray(outputSize)
            for (i in 0 until outputSize) {
                outputErrors[i] = targets[i] - outputs[i]
                outputDeltas[i] = outputErrors[i] * (1 - outputs[i] * outputs[i])
            }

            val hiddenErrors = FloatArray(hiddenSize)
            val hiddenDeltas = FloatArray(hiddenSize)
            for (i in 0 until hiddenSize) {
                var error = 0f
                for (j in 0 until outputSize) {
                    error += outputDeltas[j] * weights2[j][i]
                }
                hiddenErrors[i] = error
                hiddenDeltas[i] = error * reluDerivative(hidden[i])
            }

            for (i in 0 until outputSize) {
                for (j in 0 until hiddenSize) {
                    weights2[i][j] += learningRate * outputDeltas[i] * hidden[j]
                }
                bias2[i] += learningRate * outputDeltas[i]
            }

            for (i in 0 until hiddenSize) {
                for (j in 0 until inputSize) {
                    weights1[i][j] += learningRate * hiddenDeltas[i] * inputs[j]
                }
                bias1[i] += learningRate * hiddenDeltas[i]
            }

            nnTrained = true
        }

        fun trainBatch(samples: List<TrainingSample>, epochs: Int = 10) {
            if (!nnInitialized || samples.isEmpty()) return

            repeat(epochs) { epoch ->
                samples.shuffled().forEach { sample ->
                    trainSingle(sample.inputs, sample.outputs)
                }

                val settings = activeSettings
                if (settings?.nnEnableLogging?.get() == true && (epoch + 1) % 5 == 0) {
                    val loss = calculateLoss(samples)
                    println("神经网络训练 - Epoch ${epoch + 1}, Loss: $loss")
                }
            }
        }

        fun calculateLoss(samples: List<TrainingSample>): Float {
            var totalLoss = 0f
            samples.forEach { sample ->
                val prediction = forward(sample.inputs)
                for (i in prediction.indices) {
                    totalLoss += abs(prediction[i] - sample.outputs[i])
                }
            }
            return totalLoss / (samples.size * outputSize)
        }

        fun saveModel(file: File) {
            try {
                val model = NeuralNetworkModel(weights1, bias1, weights2, bias2, totalSamplesCollected)
                ObjectOutputStream(FileOutputStream(file)).use { oos ->
                    oos.writeObject(model)
                }
                val settings = activeSettings
                if (settings?.nnEnableLogging?.get() == true) {
                    println("神经网络模型已保存: $file, 训练样本数: $totalSamplesCollected")
                }
            } catch (e: Exception) {
                println("保存神经网络模型失败: ${e.message}")
            }
        }

        fun loadModel(file: File): Boolean {
            return try {
                ObjectInputStream(FileInputStream(file)).use { ois ->
                    val model = ois.readObject() as NeuralNetworkModel
                    weights1 = model.weights1
                    bias1 = model.bias1
                    weights2 = model.weights2
                    bias2 = model.bias2
                    totalSamplesCollected = model.trainingSamples
                }
                nnInitialized = true
                nnTrained = true
                val settings = activeSettings
                if (settings?.nnEnableLogging?.get() == true) {
                    println("神经网络模型已加载: $file, 训练样本数: $totalSamplesCollected")
                }
                true
            } catch (e: Exception) {
                println("加载神经网络模型失败: ${e.message}")
                false
            }
        }

        fun getModelInfo(): String {
            return "神经网络[隐藏层:$hiddenSize, 学习率:$learningRate, 训练样本:$totalSamplesCollected]"
        }
    }

    private fun initNeuralNetwork(settings: RotationSettings) {
        if (!settings.neuralNetworkEnabled.get() || nnInitialized) return

        val hiddenSize = settings.nnHiddenSize.get()
        val learningRate = settings.nnLearningRate.get()

        neuralNetwork.initialize(hiddenSize, learningRate)

        if (settings.nnLoadOnStart.get()) {
            val modelFile = File(FileManager.dir, "aim_nn_model.dat")
            if (modelFile.exists()) {
                neuralNetwork.loadModel(modelFile)
            }
        }
    }

    fun collectTrainingData(
        current: Rotation,
        target: Rotation,
        distance: Float,
        relativeSpeedX: Float,
        relativeSpeedZ: Float,
        actualDeltaYaw: Float,
        actualDeltaPitch: Float,
        settings: RotationSettings
    ) {
        if (!settings.neuralNetworkEnabled.get() || !settings.nnTrainingEnabled.get()) return

        initNeuralNetwork(settings)

        val maxSamples = settings.nnMaxTrainingSamples.get()
        if (trainingData.size >= maxSamples) {
            trainingData.removeAt(0)
        }

        val normalizedInputs = floatArrayOf(
            normalizeAngle(current.yaw, -180f, 180f),
            normalizeAngle(current.pitch, -90f, 90f),
            normalizeAngle(target.yaw, -180f, 180f),
            normalizeAngle(target.pitch, -90f, 90f),
            distance / 20f,
            relativeSpeedX / 5f,
            relativeSpeedZ / 5f
        )

        val normalizedOutputs = floatArrayOf(
            actualDeltaYaw / 30f,
            actualDeltaPitch / 20f
        ).map { it.coerceIn(-1f, 1f) }.toFloatArray()

        trainingData.add(TrainingSample(normalizedInputs, normalizedOutputs))
        totalSamplesCollected++

        val now = System.currentTimeMillis()
        val trainingInterval = settings.nnTrainingInterval.get()

        if (now - lastTrainingTime > trainingInterval && trainingData.size >= 50) {
            trainNeuralNetwork(settings)
            lastTrainingTime = now
        }
    }

    private fun trainNeuralNetwork(settings: RotationSettings) {
        if (trainingData.size < 10) return

        if (settings.nnEnableLogging.get()) {
            println("开始训练神经网络，样本数: ${trainingData.size}")
        }

        neuralNetwork.trainBatch(trainingData, epochs = 20)

        if (settings.nnAutoSave.get()) {
            val modelFile = File(FileManager.dir, "aim_nn_model.dat")
            neuralNetwork.saveModel(modelFile)
        }

        val keepSize = minOf(trainingData.size, settings.nnMaxTrainingSamples.get() / 2)
        if (trainingData.size > keepSize) {
            trainingData.subList(0, trainingData.size - keepSize).clear()
        }
    }

    fun neuralNetworkPrediction(
        current: Rotation,
        target: Rotation,
        distance: Float = 4f,
        relativeSpeedX: Float = 0f,
        relativeSpeedZ: Float = 0f,
        settings: RotationSettings
    ): Rotation? {
        if (!settings.neuralNetworkEnabled.get() || !nnTrained) return null

        initNeuralNetwork(settings)

        val inputs = floatArrayOf(
            normalizeAngle(current.yaw, -180f, 180f),
            normalizeAngle(current.pitch, -90f, 90f),
            normalizeAngle(target.yaw, -180f, 180f),
            normalizeAngle(target.pitch, -90f, 90f),
            distance / 20f,
            relativeSpeedX / 5f,
            relativeSpeedZ / 5f
        )

        val outputs = neuralNetwork.forward(inputs)

        val predictedDeltaYaw = outputs[0] * 30f
        val predictedDeltaPitch = outputs[1] * 20f

        var result = Rotation(
            current.yaw + predictedDeltaYaw,
            current.pitch + predictedDeltaPitch
        )

        if (settings.nnUseSpeedLimit.get()) {
            result = applySpeedLimit(current, result, settings)
        }

        return result.fixedSensitivity()
    }

    private fun applySpeedLimit(current: Rotation, target: Rotation, settings: RotationSettings): Rotation {
        val multiplier = settings.nnSpeedMultiplier.get()
        val maxYawSpeed = settings.horizontalSpeed * multiplier
        val maxPitchSpeed = settings.verticalSpeed * multiplier

        val yawDiff = angleDifference(target.yaw, current.yaw)
        val pitchDiff = target.pitch - current.pitch

        val limitedYawDiff = yawDiff.coerceIn(-maxYawSpeed, maxYawSpeed)
        val limitedPitchDiff = pitchDiff.coerceIn(-maxPitchSpeed, maxPitchSpeed)

        return Rotation(
            current.yaw + limitedYawDiff,
            current.pitch + limitedPitchDiff
        )
    }

    fun handlePatternPredictionMode(
        current: Rotation,
        target: Rotation,
        settings: RotationSettings
    ): Rotation {
        // 检查是否启用模式预测
        val patternPredictionEnabled = ("PatternPrediction" in settings.rotationMode) &&
                settings.mlPredictionWeight.get() > 0f

        val neuralNetworkEnabled = settings.neuralNetworkEnabled.get() &&
                settings.nnPredictionWeight.get() > 0f

        // 如果没有启用任何预测模式，直接返回目标
        if (!patternPredictionEnabled && !neuralNetworkEnabled) {
            resetPredictionState()
            return target
        }

        val now = System.currentTimeMillis()

        // 获取预测结果
        val patternPrediction = if (patternPredictionEnabled) {
            predictNextRotation()
        } else {
            null
        }

        val nnPrediction = if (neuralNetworkEnabled) {
            val player = mc.thePlayer
            val pointedEntity = mc.pointedEntity

            val distance = if (pointedEntity != null) {
                player.getDistanceToEntity(pointedEntity)
            } else {
                4f
            }

            neuralNetworkPrediction(current, target, distance, 0f, 0f, settings)
        } else {
            null
        }

        // 如果没有有效的预测结果，直接返回目标
        if (patternPrediction == null && nnPrediction == null) {
            resetPredictionState()
            return target
        }

        // 开始或继续预测过渡
        if (!predictionActive) {
            predictionActive = true
            transitionStartTime = now
            transitionProgress = 0f
            predictionStartRotation = current
            lastSmoothRotation = current
        }

        // 更新过渡进度
        val elapsed = (now - transitionStartTime).toFloat()
        transitionProgress = (elapsed / transitionDuration).coerceIn(0f, 1f)

        // 计算基础预测旋转（不使用平滑，仅用于计算）
        val basePredictedRotation = calculateBasePrediction(
            current,
            target,
            patternPrediction,
            nnPrediction,
            settings
        )

        // 应用平滑过渡到预测结果
        val finalRotation = applySmoothTransition(
            current,
            basePredictedRotation,
            target,
            transitionProgress,
            settings
        )

        // 保存最后平滑旋转用于下一帧
        lastSmoothRotation = finalRotation

        return finalRotation.fixedSensitivity()
    }

    // 计算基础预测（不考虑平滑）
    private fun calculateBasePrediction(
        current: Rotation,
        target: Rotation,
        patternPrediction: Rotation?,
        nnPrediction: Rotation?,
        settings: RotationSettings
    ): Rotation {
        return when {
            patternPrediction != null && nnPrediction != null -> {
                // 两者都可用，加权混合
                val patternWeight = settings.mlPredictionWeight.get().coerceIn(0f, 1f)
                val nnWeight = settings.nnPredictionWeight.get().coerceIn(0f, 1f)
                val currentWeight = 0.1f.coerceAtMost(1f - patternWeight - nnWeight)
                val targetWeight = 1f - currentWeight - patternWeight - nnWeight

                val yaw = current.yaw * currentWeight +
                        patternPrediction.yaw * patternWeight +
                        nnPrediction.yaw * nnWeight +
                        target.yaw * targetWeight

                val pitch = current.pitch * currentWeight +
                        patternPrediction.pitch * patternWeight +
                        nnPrediction.pitch * nnWeight +
                        target.pitch * targetWeight

                Rotation(yaw, pitch)
            }

            patternPrediction != null -> {
                // 仅模式预测可用
                val patternWeight = settings.mlPredictionWeight.get().coerceIn(0f, 1f)
                val currentWeight = 0.1f.coerceAtMost(1f - patternWeight)
                val targetWeight = 1f - currentWeight - patternWeight

                val yaw = current.yaw * currentWeight +
                        patternPrediction.yaw * patternWeight +
                        target.yaw * targetWeight

                val pitch = current.pitch * currentWeight +
                        patternPrediction.pitch * patternWeight +
                        target.pitch * targetWeight

                Rotation(yaw, pitch)
            }

            nnPrediction != null -> {
                // 仅神经网络预测可用
                val nnWeight = settings.nnPredictionWeight.get().coerceIn(0f, 1f)
                val currentWeight = 0.1f.coerceAtMost(1f - nnWeight)
                val targetWeight = 1f - currentWeight - nnWeight

                val yaw = current.yaw * currentWeight +
                        nnPrediction.yaw * nnWeight +
                        target.yaw * targetWeight

                val pitch = current.pitch * currentWeight +
                        nnPrediction.pitch * nnWeight +
                        target.pitch * targetWeight

                Rotation(yaw, pitch)
            }

            else -> {
                // 没有预测可用，返回目标
                target
            }
        }
    }

    // 应用平滑过渡
    private fun applySmoothTransition(
        current: Rotation,
        predicted: Rotation,
        target: Rotation,
        progress: Float,
        settings: RotationSettings
    ): Rotation {
        // 计算平滑因子：开始时更接近目标，逐渐过渡到预测
        val smoothFactor = progress

        // 混合目标旋转和预测旋转
        val mixedYaw = target.yaw * (1f - smoothFactor) + predicted.yaw * smoothFactor
        val mixedPitch = target.pitch * (1f - smoothFactor) + predicted.pitch * smoothFactor

        val mixedRotation = Rotation(mixedYaw, mixedPitch)

        // 应用帧间平滑（防止突然变化）
        return applyFrameSmoothing(current, mixedRotation, settings)
    }

    // 应用帧间平滑
    private fun applyFrameSmoothing(current: Rotation, target: Rotation, settings: RotationSettings): Rotation {
        val lastRotation = lastSmoothRotation ?: current

        // 限制单帧最大变化
        val maxYawChange = settings.horizontalSpeed * 2f
        val maxPitchChange = settings.verticalSpeed * 2f

        val yawDiff = angleDifference(target.yaw, lastRotation.yaw)
        val pitchDiff = target.pitch - lastRotation.pitch

        val limitedYawDiff = yawDiff.coerceIn(-maxYawChange, maxYawChange)
        val limitedPitchDiff = pitchDiff.coerceIn(-maxPitchChange, maxPitchChange)

        return Rotation(
            lastRotation.yaw + limitedYawDiff,
            lastRotation.pitch + limitedPitchDiff
        )
    }

    // 重置预测状态
    private fun resetPredictionState() {
        if (predictionActive) {
            predictionActive = false
            transitionProgress = 0f
            transitionStartTime = 0
            predictionStartRotation = null
            lastSmoothRotation = null
            lastPrediction = null
        }
    }

    private fun normalizeAngle(angle: Float, min: Float, max: Float): Float {
        val normalized = (angle - min) / (max - min)
        return normalized.coerceIn(0f, 1f)
    }

    fun getNeuralNetworkInfo(): String {
        return if (nnInitialized) {
            neuralNetwork.getModelInfo()
        } else {
            "神经网络未初始化"
        }
    }

    data class RotationData(
        val yaw: Float,
        val pitch: Float,
        val timestamp: Long
    )

    data class RotationPattern(
        val yawDeltas: List<Float>,
        val pitchDeltas: List<Float>,
        val duration: Long
    ) {
        val size: Int = minOf(yawDeltas.size, pitchDeltas.size)
    }

    fun loadCollectedPatterns() {
        if (patternsLoaded) return

        val dataFile = File(FileManager.dir, "aim_path_data.jsonl")

        if (!dataFile.exists()) {
            chat("No collected aim data found.Please Enable AimDataCollector Module to collect data.")
            patternsLoaded = true
            return
        }

        val currentModified = dataFile.lastModified()
        val currentSize = dataFile.length()

        if (patternsLoaded && currentModified == lastModified && currentSize == fileSize) {
            return
        }

        val tempPatterns = mutableListOf<RotationPattern>()

        try {
            dataFile.forEachLine { line ->
                try {
                    val jsonObject = JsonParser().parse(line).asJsonObject
                    val pointsArray = jsonObject.getAsJsonArray("points")

                    if (pointsArray.size() >= 3) {
                        val yawDeltas = mutableListOf<Float>()
                        val pitchDeltas = mutableListOf<Float>()

                        for (i in 1 until pointsArray.size()) {
                            val currentPoint = pointsArray[i].asJsonObject
                            val prevPoint = pointsArray[i-1].asJsonObject

                            val currentYaw = currentPoint.get("currentYaw").asFloat
                            val prevYaw = prevPoint.get("currentYaw").asFloat
                            val deltaYaw = angleDifference(currentYaw, prevYaw)

                            val currentPitch = currentPoint.get("currentPitch").asFloat
                            val prevPitch = prevPoint.get("currentPitch").asFloat
                            val deltaPitch = currentPitch - prevPitch

                            yawDeltas.add(deltaYaw)
                            pitchDeltas.add(deltaPitch)
                        }

                        val duration = jsonObject.get("duration").asLong

                        tempPatterns.add(RotationPattern(yawDeltas, pitchDeltas, duration))
                    }
                } catch (_: Exception) {
                }
            }

            loadedPatterns.addAll(
                tempPatterns.sortedByDescending { it.yawDeltas.size }
                    .take(100)
            )

            println("Loaded ${loadedPatterns.size} rotation patterns from collected data (${tempPatterns.size} total).")

            lastModified = currentModified
            fileSize = currentSize
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            patternsLoaded = true
        }
    }

    fun addRotationData(yaw: Float, pitch: Float) {
        val settings = activeSettings ?: return

        if ("PatternPrediction" !in settings.rotationMode) return

        rotationHistory.add(RotationData(yaw, pitch, System.currentTimeMillis()))

        if (rotationHistory.size > 50) {
            rotationHistory.removeAt(0)
        }
    }

    fun predictNextRotation(): Rotation? {
        val settings = activeSettings ?: return null

        if ("PatternPrediction" !in settings.rotationMode || rotationHistory.size < 5) {
            return null
        }

        val now = System.currentTimeMillis()
        if (lastPrediction != null && (now - lastPredictionTime) < predictionCacheDuration) {
            return lastPrediction
        }

        val currentRotation = currentRotation ?: return null

        if (!patternsLoaded) {
            val result = simpleLinearPrediction(currentRotation)
            lastPrediction = result
            lastPredictionTime = now
            return result
        }

        val result = if (loadedPatterns.isEmpty()) {
            simpleLinearPrediction(currentRotation)
        } else {
            patternBasedPrediction(currentRotation)
        }

        lastPrediction = result
        lastPredictionTime = now

        return result
    }

    fun simpleLinearPrediction(current: Rotation): Rotation? {
        if (rotationHistory.size < 3) return null

        val lastIndex = rotationHistory.size - 1
        val prevIndex = rotationHistory.size - 2
        val prevPrevIndex = rotationHistory.size - 3

        val lastYaw = rotationHistory[lastIndex].yaw
        val prevYaw = rotationHistory[prevIndex].yaw
        val prevPrevYaw = rotationHistory[prevPrevIndex].yaw

        val lastPitch = rotationHistory[lastIndex].pitch
        val prevPitch = rotationHistory[prevIndex].pitch
        val prevPrevPitch = rotationHistory[prevPrevIndex].pitch

        val deltaYaw1 = angleDifference(lastYaw, prevYaw)
        val deltaYaw2 = angleDifference(prevYaw, prevPrevYaw)
        val deltaPitch1 = lastPitch - prevPitch
        val deltaPitch2 = prevPitch - prevPrevPitch

        // 使用更保守的线性预测，避免突然变化
        val predictedDeltaYaw = deltaYaw1 * 0.7f + deltaYaw2 * 0.3f
        val predictedDeltaPitch = deltaPitch1 * 0.7f + deltaPitch2 * 0.3f

        // 限制预测变化范围
        val maxDelta = 30f
        val limitedDeltaYaw = predictedDeltaYaw.coerceIn(-maxDelta, maxDelta)
        val limitedDeltaPitch = predictedDeltaPitch.coerceIn(-maxDelta, maxDelta)

        return Rotation(
            current.yaw + limitedDeltaYaw,
            current.pitch + limitedDeltaPitch
        ).fixedSensitivity()
    }

    fun patternBasedPrediction(current: Rotation): Rotation? {
        if (rotationHistory.size < 5) return null

        val currentSequenceLength = minOf(10, rotationHistory.size - 1)
        val currentYawDeltas = mutableListOf<Float>()
        val currentPitchDeltas = mutableListOf<Float>()

        for (i in rotationHistory.size - currentSequenceLength until rotationHistory.size) {
            if (i > 0) {
                val deltaYaw = angleDifference(rotationHistory[i].yaw, rotationHistory[i-1].yaw)
                val deltaPitch = rotationHistory[i].pitch - rotationHistory[i-1].pitch

                currentYawDeltas.add(deltaYaw)
                currentPitchDeltas.add(deltaPitch)
            }
        }

        var bestMatchScore = Float.MAX_VALUE
        var bestPattern: RotationPattern? = null

        val patternsToCheck = minOf(20, loadedPatterns.size)
        for (i in 0 until patternsToCheck) {
            val pattern = loadedPatterns[i]
            val matchScore = calculatePatternMatchScore(currentYawDeltas, currentPitchDeltas, pattern)
            if (matchScore < bestMatchScore) {
                bestMatchScore = matchScore
                bestPattern = pattern
            }
        }

        if (bestPattern != null && bestMatchScore < patternMatchThreshold) {
            val lastDeltaYaw = bestPattern.yawDeltas.lastOrNull() ?: 0f
            val lastDeltaPitch = bestPattern.pitchDeltas.lastOrNull() ?: 0f

            // 限制模式预测的变化
            val maxDelta = 45f
            val limitedDeltaYaw = lastDeltaYaw.coerceIn(-maxDelta, maxDelta)
            val limitedDeltaPitch = lastDeltaPitch.coerceIn(-maxDelta, maxDelta)

            return Rotation(
                current.yaw + limitedDeltaYaw,
                current.pitch + limitedDeltaPitch
            ).fixedSensitivity()
        }

        return simpleLinearPrediction(current)
    }

    fun calculatePatternMatchScore(
        currentYawDeltas: List<Float>,
        currentPitchDeltas: List<Float>,
        pattern: RotationPattern
    ): Float {
        val patternYawDeltas = pattern.yawDeltas
        val patternPitchDeltas = pattern.pitchDeltas

        val compareLength = minOf(currentYawDeltas.size, patternYawDeltas.size, pattern.size)
        if (compareLength == 0) return Float.MAX_VALUE

        var totalDiff = 0f
        for (i in 0 until compareLength) {
            val currentIndex = currentYawDeltas.size - compareLength + i
            val patternIndex = patternYawDeltas.size - compareLength + i

            totalDiff += abs(currentYawDeltas[currentIndex] - patternYawDeltas[patternIndex])
            totalDiff += abs(currentPitchDeltas[currentIndex] - patternPitchDeltas[patternIndex])
        }

        return totalDiff / (compareLength * 2)
    }

    fun collectTrainingDataForNN(current: Rotation, final: Rotation, settings: RotationSettings) {
        val player = mc.thePlayer ?: return
        val targetEntity = mc.pointedEntity ?: return

        val distance = player.getDistanceToEntity(targetEntity)

        val actualDeltaYaw = angleDifference(final.yaw, current.yaw)
        val actualDeltaPitch = final.pitch - current.pitch

        collectTrainingData(
            current,
            final,  // 使用最终结果作为目标
            distance,
            (targetEntity.posX - targetEntity.prevPosX).toFloat(),
            (targetEntity.posZ - targetEntity.prevPosZ).toFloat(),
            actualDeltaYaw,
            actualDeltaPitch,
            settings
        )
    }

}
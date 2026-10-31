package net.ccbluex.liquidbounce.utils.rotation.noise3

import net.ccbluex.liquidbounce.utils.rotation.Rotation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class Noise3Test {

    /**
     * 验证物理模拟能否精确到达目标位移
     */
    @Test
    fun physicsReachesTarget() {
        val planner = C40N()
        // 参数顺序: (accelFactor, totalRemaining, acceleration, baseVelocity, timeStep, currentVelocity)
        // maxVelocity 默认 = INITIAL_SPEED (3.0)
        val s = AxisState(
            accelFactor = 3.0,
            totalRemaining = 45.0,
            acceleration = 3.0,
            baseVelocity = 0.0,
            timeStep = DT,
            currentVelocity = 0.0
        )
        val path = planner.simulate(s)
        assertFalse(path.isEmpty(), "Path should not be empty")
        val finalPosition = path.last().position
        assertEquals(45.0, finalPosition, 1e-6,
            "Final position should match target displacement within tolerance")
    }

    /**
     * 验证二分调参后路径步数与目标步数匹配（±1）
     */
    @Test
    fun tuneStepsMatchesTarget() {
        val planner = C40N()
        val s = AxisState(
            accelFactor = 3.0,
            totalRemaining = 45.0,
            acceleration = 3.0,
            baseVelocity = 0.0,
            timeStep = DT,
            currentVelocity = 0.0
        ).also {
            it.maxVelocity = 30.0  // 给一个足够大的初始上限
        }
        val tuned = planner.tuneSteps(s, 3)
        val count = planner.simulate(tuned).size
        assertTrue(count in 2..4,
            "Tuned step count should be 2-4 (±1 from target 3), but was $count")
    }

    /**
     * 验证主流程 10 tick 内能接近目标 rotation（|Δyaw| < 10°）
     */
    @Test
    fun noise3ApproachesTarget() {
        val calc = Noise3Calculator()
        calc.aimSpeed = 30f  // 加速收敛
        calc.speedAtLargeRot = 30f
        calc.speedAtInitialRot = 30f

        var currentYaw: Float = 0f
        var currentPitch: Float = 0f

        val player = object : PlayerHandle {
            override fun currentRotation() = Rotation(currentYaw, currentPitch)
            override fun getHurtTime() = 0
        }

        for (i in 0 until 10) {
            val r = calc.compute(player, null, Rotation(45f, 0f), Rotation(45f, 0f))
            currentYaw = r.yaw
            currentPitch = r.pitch
        }

        val diff = kotlin.math.abs(currentYaw - 45f)
        assertTrue(diff < 10f,
            "After 10 ticks, yaw should be within 10° of target 45°, but diff was $diff")
    }
}
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.STOP_SPRINTING
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import java.awt.Color
import kotlin.math.*

object CSGOFight : Module("CSGOFight", Category.COMBAT) {

    // ---------- 基本设置 ----------
    private val silent by boolean("Silent", true)
    private val targetHead by boolean("TargetHead", false)
    private val throughWalls by boolean("ThroughWalls", false)
    private val infiniteRange by boolean("InfiniteRange", false)

    private val range by float("Range", 6f, 1f..12f)
    private val fov by float("FOV", 90f, 30f..180f)

    // 目标优先级
    private val targetPriority by choices("TargetPriority", arrayOf("Fov", "Range", "Health", "Speed"), "Fov")

    // 预测
    private val predict by boolean("Predict", true)
    private val minPredictTick by int("MinPredictTick", 1, 0..10) { predict }
    private val maxPredictTick by int("MaxPredictTick", 4, 0..10) { predict }
    private val aimFix by boolean("AimFix", true)

    // 瞄准点采样
    private val multiRay by boolean("MultiRay", true)
    private val pointPriority by choices("PointPriority", arrayOf("Head", "Body", "Smart", "Auto"), "Smart")

    // 无散射
    private val noSpread by boolean("NoSpread", true)
    private val spreadSilent by boolean("SpreadSilent", true) { noSpread }
    private val spreadDelay by int("SpreadDelay", 0, 0..200) { noSpread }
    private val attackAfterSpreadDelay by int("AttackAfterSpreadDelay", 0, 0..200) { noSpread }

    // 非静默平滑转头
    private val turnSpeed by float("TurnSpeed", 1.0f, 0.1f..1.0f) { !silent }

    // 定身与急停
    private val freezeOnAir by boolean("FreezeOnAir", false)
    private val freezeOnGround by boolean("FreezeOnGround", false)
    private val stopMove by boolean("StopMove", false)

    // 渲染
    private val trajectory by boolean("Trajectory", true)
    private val trajectoryColor by color("TrajectoryColor", Color(0, 255, 255, 200)) { trajectory }
    private val hitIndicator by boolean("HitIndicator", true)

    // ---------- 攻击循环延迟 ----------
    private val aimDelay by int("AimDelay", 0, 0..200)
    private val attackDelay by int("AttackDelay", 50, 0..200)
    private val recoverDelay by int("RecoverDelay", 0, 0..200)

    private val rotateOptions = RotationSettings(this).withoutKeepRotation()

    private enum class CSGOState {
        IDLE, AIM, SPREAD, WAIT_AFTER_SPREAD, ATTACK, RECOVER
    }

    private var csgoState = CSGOState.IDLE
    private var target: EntityLivingBase? = null
    private val stateTimer = MSTimer()
    private var originalRotation: Rotation? = null
    private var originalYaw = 0f

    private var freezePos: Vec3? = null

    private var spreadSlot = -1
    private var originalSlot = -1

    // 轨迹线存储
    private var lastTrajectoryStart: Vec3? = null
    private var lastTrajectoryEnd: Vec3? = null

    // 命中目标
    private var lastHitTarget: EntityLivingBase? = null

    // 目标历史记录
    private data class MotionSample(
        val tick: Int,
        val x: Double, val y: Double, val z: Double,
        val mx: Double, val my: Double, val mz: Double,
        val onGround: Boolean
    )
    private val moveHistory = HashMap<Int, ArrayDeque<MotionSample>>()

    override fun onDisable() {
        target = null
        csgoState = CSGOState.IDLE
        originalRotation = null
        freezePos = null
        moveHistory.clear()
        lastTrajectoryStart = null
        lastTrajectoryEnd = null
        lastHitTarget = null
        if (spreadSlot != -1) {
            if (spreadSilent) {
                SilentHotbar.resetSlot(this)
            } else {
                sendPacket(C09PacketHeldItemChange(originalSlot))
                mc.thePlayer?.inventory?.currentItem = originalSlot
            }
            spreadSlot = -1
        }
        RotationUtils.setTargetRotation(
            Rotation(mc.thePlayer?.rotationYaw ?: 0f, mc.thePlayer?.rotationPitch ?: 0f),
            rotateOptions
        )
    }

    // 移动修正
    val onMove = handler<MoveEvent> {
        if (freezePos != null) {
            it.cancelEvent()
            mc.thePlayer?.setPosition(freezePos!!.xCoord, freezePos!!.yCoord, freezePos!!.zCoord)
            return@handler
        }

        if (csgoState != CSGOState.IDLE) {
            val player = mc.thePlayer ?: return@handler
            val speed = sqrt((it.x * it.x + it.z * it.z).toDouble()).toFloat()
            if (speed > 0f && (player.moveForward != 0f || player.moveStrafing != 0f)) {
                val forward = player.moveForward
                val strafing = player.moveStrafing
                var moveYaw = originalYaw

                if (forward != 0f) {
                    if (strafing > 0f) moveYaw -= 45f
                    else if (strafing < 0f) moveYaw += 45f
                } else if (strafing != 0f) {
                    moveYaw += if (strafing > 0f) -90f else 90f
                }
                if (forward < 0f) moveYaw += 180f

                val radians = Math.toRadians(moveYaw.toDouble())
                it.x = -sin(radians) * speed
                it.z = cos(radians) * speed
            }
        }
    }

    val onTick = handler<GameTickEvent> {
        val player = mc.thePlayer ?: return@handler
        if (player.isDead) return@handler

        when (csgoState) {
            CSGOState.IDLE -> {
                freezePos = null
                target = findTarget()
                if (target != null) {
                    csgoState = CSGOState.AIM
                    stateTimer.reset()
                    originalRotation = currentRotation ?: Rotation(player.rotationYaw, player.rotationPitch)
                    originalYaw = player.rotationYaw
                }
            }

            CSGOState.AIM -> {
                val currentTarget = target ?: run { csgoState = CSGOState.IDLE; return@handler }
                val aimPoint = getBestAimPoint(currentTarget)
                if (aimPoint != null) {
                    lastTrajectoryStart = getAimFrom()
                    lastTrajectoryEnd = aimPoint

                    val targetRotation = RotationUtils.toRotation(aimPoint, true)
                    if (silent) {
                        RotationUtils.setTargetRotation(targetRotation, rotateOptions)
                    } else {
                        val curYaw = player.rotationYaw
                        val curPitch = player.rotationPitch
                        val yawDiff = wrapAngleTo180(targetRotation.yaw - curYaw)
                        val pitchDiff = targetRotation.pitch - curPitch
                        val step = turnSpeed.coerceAtLeast(0.01f)
                        player.rotationYaw = curYaw + yawDiff * step
                        player.rotationPitch = curPitch + pitchDiff * step
                    }
                }

                if (stateTimer.hasTimePassed(aimDelay)) {
                    val shouldFreeze = stopMove || (freezeOnAir && !player.onGround) || (freezeOnGround && player.onGround)
                    if (shouldFreeze) {
                        player.motionX = 0.0
                        player.motionZ = 0.0
                        if (player.isSprinting) sendPacket(C0BPacketEntityAction(player, STOP_SPRINTING))
                        freezePos = Vec3(player.posX, player.posY, player.posZ)
                    }

                    if (noSpread) {
                        spreadSlot = (0..8).firstOrNull { player.inventory.getStackInSlot(it) == null } ?: -1
                        if (spreadSlot != -1) {
                            originalSlot = player.inventory.currentItem
                            if (spreadSilent) {
                                SilentHotbar.selectSlotSilently(this, spreadSlot, render = false, resetManually = true)
                            } else {
                                sendPacket(C09PacketHeldItemChange(spreadSlot))
                                player.inventory.currentItem = spreadSlot
                            }
                            csgoState = CSGOState.SPREAD
                            stateTimer.reset()
                        } else {
                            csgoState = CSGOState.ATTACK
                            stateTimer.reset()
                        }
                    } else {
                        csgoState = CSGOState.ATTACK
                        stateTimer.reset()
                    }
                }
            }

            CSGOState.SPREAD -> {
                if (stateTimer.hasTimePassed(spreadDelay)) {
                    if (spreadSilent) {
                        SilentHotbar.selectSlotSilently(this, originalSlot, render = false, resetManually = true)
                    } else {
                        sendPacket(C09PacketHeldItemChange(originalSlot))
                        mc.thePlayer?.inventory?.currentItem = originalSlot
                    }
                    csgoState = CSGOState.WAIT_AFTER_SPREAD
                    stateTimer.reset()
                }
            }

            CSGOState.WAIT_AFTER_SPREAD -> {
                if (stateTimer.hasTimePassed(attackAfterSpreadDelay)) {
                    csgoState = CSGOState.ATTACK
                    stateTimer.reset()
                }
            }

            CSGOState.ATTACK -> {
                sendPacket(C08PacketPlayerBlockPlacement(player.heldItem))
                if (stateTimer.hasTimePassed(attackDelay)) {
                    if (target?.hurtTime ?: 0 > 0) {
                        lastHitTarget = target
                    }
                    csgoState = CSGOState.RECOVER
                    stateTimer.reset()
                }
            }

            CSGOState.RECOVER -> {
                freezePos = null
                val backRotation = originalRotation ?: Rotation(player.rotationYaw, player.rotationPitch)
                if (silent) {
                    RotationUtils.setTargetRotation(backRotation, rotateOptions)
                } else {
                    val curYaw = player.rotationYaw
                    val curPitch = player.rotationPitch
                    val yawDiff = wrapAngleTo180(backRotation.yaw - curYaw)
                    val pitchDiff = backRotation.pitch - curPitch
                    val step = turnSpeed.coerceAtLeast(0.01f)
                    player.rotationYaw = curYaw + yawDiff * step
                    player.rotationPitch = curPitch + pitchDiff * step
                }

                if (stateTimer.hasTimePassed(recoverDelay)) {
                    csgoState = CSGOState.IDLE
                    target = null
                }
            }
        }
    }

    private fun wrapAngleTo180(angle: Float): Float {
        var a = angle % 360f
        if (a >= 180f) a -= 360f
        if (a < -180f) a += 360f
        return a
    }

    private data class TargetInfo(
        val entity: EntityLivingBase,
        val distance: Double,
        val angle: Float,
        val health: Float,
        val speed: Double
    )

    private fun findTarget(): EntityLivingBase? {
        val player = mc.thePlayer ?: return null
        val world = mc.theWorld ?: return null

        val candidates = world.loadedEntityList
            .filterIsInstance<EntityLivingBase>()
            .filter { isSelected(it, true) && it != player }
            .mapNotNull { entity ->
                val distance = player.getDistanceToEntity(entity).toDouble()
                if (!infiniteRange && distance > range) return@mapNotNull null
                val angle = RotationUtils.rotationDifference(entity)
                if (angle > fov) return@mapNotNull null
                if (!throughWalls && !isVisible(getAimFrom(), entity)) return@mapNotNull null
                val health = entity.health ?: 20f
                val speed = sqrt(entity.motionX * entity.motionX + entity.motionZ * entity.motionZ)
                TargetInfo(entity, distance, angle, health, speed)
            }

        return when (targetPriority) {
            "Fov" -> candidates.minByOrNull { it.angle }?.entity
            "Range" -> candidates.minByOrNull { it.distance }?.entity
            "Health" -> candidates.minByOrNull { it.health }?.entity
            "Speed" -> candidates.maxByOrNull { it.speed }?.entity
            else -> candidates.minByOrNull { it.angle }?.entity
        }
    }

    private fun getAimFrom(): Vec3 {
        val player = mc.thePlayer ?: return Vec3(0.0, 0.0, 0.0)
        if (!aimFix) return player.positionVector.addVector(0.0, player.getEyeHeight().toDouble(), 0.0)
        val dx = player.posX - player.prevPosX
        val dy = player.posY - player.prevPosY
        val dz = player.posZ - player.prevPosZ
        return Vec3(player.posX + dx * 0.5, player.posY + dy * 0.5 + player.getEyeHeight(), player.posZ + dz * 0.5)
    }

    private fun isVisible(from: Vec3, target: EntityLivingBase): Boolean {
        val world = mc.theWorld ?: return false
        val box = getPredictBox(target) ?: target.entityBoundingBox ?: return false
        val points = getAimPoints(box, target)
        for (point in points) {
            if (world.rayTraceBlocks(from, point, false, true, false) == null) return true
        }
        return false
    }

    private fun getPredictBox(entity: EntityLivingBase): AxisAlignedBB? {
        val box = entity.entityBoundingBox ?: return null
        if (!predict) return box

        val motion = getPredictedMotion(entity)
        val ticks = dynamicPredictTicks(entity)
        var newBox = AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)
        for (i in 0 until ticks) {
            newBox = newBox.offset(motion.x, motion.y, motion.z)
        }

        val speed = sqrt(motion.x * motion.x + motion.z * motion.z)
        if (speed > 0.1) {
            val expand = min(0.2, speed * 0.3)
            newBox = newBox.expand(expand, 0.0, expand)
        }
        return newBox
    }

    private data class PredictedMotion(val x: Double, val y: Double, val z: Double)

    private fun getPredictedMotion(entity: EntityLivingBase): PredictedMotion {
        val history = moveHistory.getOrPut(entity.entityId) { ArrayDeque() }
        val nowTick = mc.thePlayer?.ticksExisted ?: 0
        history.addLast(
            MotionSample(nowTick, entity.posX, entity.posY, entity.posZ,
                entity.motionX, entity.motionY, entity.motionZ, entity.onGround)
        )
        if (history.size > 10) history.removeFirst()

        if (history.size < 2) {
            return PredictedMotion(entity.motionX, entity.motionY, entity.motionZ)
        }

        var weightedX = 0.0; var weightedY = 0.0; var weightedZ = 0.0
        var totalWeight = 0.0
        var prev = history.first()
        for (i in 1 until history.size) {
            val cur = history[i]
            val dt = (cur.tick - prev.tick).coerceAtLeast(1)
            val wx = (cur.x - prev.x) / dt
            val wy = (cur.y - prev.y) / dt
            val wz = (cur.z - prev.z) / dt
            val weight = (i + 1).toDouble()
            weightedX += wx * weight; weightedY += wy * weight; weightedZ += wz * weight
            totalWeight += weight
            prev = cur
        }

        val histX = weightedX / totalWeight
        val histY = weightedY / totalWeight
        val histZ = weightedZ / totalWeight

        val mix = 0.6
        return PredictedMotion(
            histX * (1 - mix) + entity.motionX * mix,
            histY * (1 - mix) + entity.motionY * mix,
            histZ * (1 - mix) + entity.motionZ * mix
        )
    }

    private fun dynamicPredictTicks(entity: EntityLivingBase): Int {
        val dist = mc.thePlayer?.getDistanceToEntity(entity) ?: 0f
        val speed = sqrt(entity.motionX * entity.motionX + entity.motionZ * entity.motionZ)
        val distFactor = exp(-dist * 0.15)
        val speedFactor = min(1.0, speed / 0.2)
        val factor = distFactor * 0.55 + speedFactor * 0.45
        return (minPredictTick + (maxPredictTick - minPredictTick) * factor).roundToInt()
    }

    private fun getAimPoints(box: AxisAlignedBB, entity: EntityLivingBase): List<Vec3> {
        val points = ArrayList<Vec3>()
        val h = box.maxY - box.minY
        val cx = (box.minX + box.maxX) * 0.5
        val cz = (box.minZ + box.maxZ) * 0.5
        val ox = max(0.03, (box.maxX - box.minX) * 0.35)
        val oz = max(0.03, (box.maxZ - box.minZ) * 0.35)

        fun addBand(ratio: Double) {
            val y = clamp(box.minY + h * ratio, box.minY + 0.05, box.maxY - 0.01)
            points.add(Vec3(cx, y, cz))
            if (multiRay) {
                points.add(Vec3(cx + ox, y, cz))
                points.add(Vec3(cx - ox, y, cz))
                points.add(Vec3(cx, y, cz + oz))
                points.add(Vec3(cx, y, cz - oz))
                points.add(Vec3(cx + ox, y, cz + oz))
                points.add(Vec3(cx + ox, y, cz - oz))
                points.add(Vec3(cx - ox, y, cz + oz))
                points.add(Vec3(cx - ox, y, cz - oz))
            }
        }

        when (pointPriority) {
            "Head" -> { addBand(0.93); addBand(0.86); addBand(0.78) }
            "Body" -> { addBand(0.62); addBand(0.52); addBand(0.42); addBand(0.32) }
            "Auto" -> {
                addBand(0.94); addBand(0.88); addBand(0.80); addBand(0.70)
                addBand(0.58); addBand(0.45); addBand(0.30); addBand(0.12)
            }
            "Smart" -> {
                if (entity.isSprinting) {
                    addBand(0.65); addBand(0.52); addBand(0.40)
                } else if (!entity.onGround) {
                    addBand(0.60); addBand(0.48); addBand(0.36)
                } else {
                    addBand(0.93); addBand(0.86); addBand(0.72); addBand(0.52)
                }
            }
        }
        return points
    }

    private fun getBestAimPoint(entity: EntityLivingBase): Vec3? {
        val player = mc.thePlayer ?: return null
        val box = getPredictBox(entity) ?: return null
        val from = getAimFrom()
        val points = getAimPoints(box, entity)

        for (point in points) {
            if (isVisible(from, point)) return point
        }
        if (throughWalls && points.isNotEmpty()) return points.first()
        return null
    }

    private fun isVisible(from: Vec3, to: Vec3): Boolean {
        val world = mc.theWorld ?: return false
        return world.rayTraceBlocks(from, to, false, true, false) == null
    }

    // ---------- 渲染 ----------
    val onRender2D = handler<Render2DEvent> {
        if (trajectory && lastTrajectoryStart != null && lastTrajectoryEnd != null) {
            drawTrajectory(lastTrajectoryStart!!, lastTrajectoryEnd!!)
        }

        if (hitIndicator && lastHitTarget != null) {
            val entity = lastHitTarget!!
            if (entity.isEntityAlive && entity.hurtTime > 0) {
                drawHitIndicator(entity)
            } else {
                lastHitTarget = null
            }
        }
    }

    private fun drawTrajectory(start: Vec3, end: Vec3) {
        val startScreen = projectToScreen(start) ?: return
        val endScreen = projectToScreen(end) ?: return

        val color = trajectoryColor
        val steps = 20
        for (i in 0 until steps) {
            val t1 = i / steps.toFloat()
            val t2 = (i + 1) / steps.toFloat()
            val p1x = startScreen.first + (endScreen.first - startScreen.first) * t1
            val p1y = startScreen.second + (endScreen.second - startScreen.second) * t1
            val p2x = startScreen.first + (endScreen.first - startScreen.first) * t2
            val p2y = startScreen.second + (endScreen.second - startScreen.second) * t2
            val cx = (p1x + p2x) / 2f
            val cy = (p1y + p2y) / 2f
            val length = sqrt((p2x - p1x).pow(2).toDouble() + (p2y - p1y).pow(2).toDouble()).toFloat()
            val angle = atan2((p2y - p1y).toDouble(), (p2x - p1x).toDouble())
            val thickness = 2.0f

            GlStateManager.pushMatrix()
            GlStateManager.translate(cx, cy, 0f)
            GlStateManager.rotate(Math.toDegrees(angle).toFloat(), 0f, 0f, 1f)
            GlowUtils.drawGlow(-length / 2f, -thickness / 2f, length, thickness, 3, color)
            GlStateManager.popMatrix()
        }
    }

    private fun drawHitIndicator(entity: EntityLivingBase) {
        val pos = Vec3(entity.posX, entity.posY + entity.height + 0.5, entity.posZ)
        val screen = projectToScreen(pos) ?: return
        Fonts.font35.drawString("hit", screen.first, screen.second, Color.RED.rgb, true)
    }

    private fun projectToScreen(vec: Vec3): Pair<Float, Float>? {
        val renderManager = mc.renderManager
        val viewerX = renderManager.viewerPosX
        val viewerY = renderManager.viewerPosY
        val viewerZ = renderManager.viewerPosZ
        val dx = vec.xCoord - viewerX
        val dy = vec.yCoord - viewerY
        val dz = vec.zCoord - viewerZ

        val yaw = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
        val pitch = Math.toRadians(mc.thePlayer.rotationPitch.toDouble())
        val cosYaw = cos(yaw); val sinYaw = sin(yaw)
        val cosPitch = cos(pitch); val sinPitch = sin(pitch)

        // 旋转到视角空间
        val x1 = dx * cosYaw - dz * sinYaw
        val z1 = dx * sinYaw + dz * cosYaw
        val y1 = dy * cosPitch - z1 * sinPitch
        val z2 = dy * sinPitch + z1 * cosPitch

        if (z2 <= 0.05) return null

        val sr = ScaledResolution(mc)
        val screenWidth = sr.scaledWidth.toDouble()
        val screenHeight = sr.scaledHeight.toDouble()
        val fov = 70.0
        val scale = (screenHeight / 2.0) / (tan(Math.toRadians(fov / 2.0)) * z2)
        val screenX = screenWidth / 2.0 + x1 * scale
        val screenY = screenHeight / 2.0 - y1 * scale
        return Pair(screenX.toFloat(), screenY.toFloat())
    }

    private fun clamp(value: Double, min: Double, max: Double): Double = value.coerceIn(min, max)
}
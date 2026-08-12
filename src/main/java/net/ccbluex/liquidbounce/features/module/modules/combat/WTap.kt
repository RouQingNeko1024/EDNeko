//skid firebounce wtap
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.angleDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase
import kotlin.math.abs

object WTap : Module("WTap", Category.COMBAT) {
    private val mode by choices("Mode",arrayOf("Normal","Simple"),"Normal")
    private val chance by int("Chance", 100, 0..100,"%"){mode == "Normal"}
    private val delay by intRange("Delay", 0..0, 0..500,"ms"){mode == "Normal"}
    private val targetHurtTime by intRange("TargetHurtTime", 0..10, 0..10){mode == "Normal"}
    private val ownHurtTime by intRange("OwnHurtTime",0..10,0..10){mode == "Normal"}
    private val ticksUntilBlock by intRange("TicksUntilBlock", 0..2, 0..10) {mode == "Normal"}
    private val reSprintTicks by intRange("ReSprintTicks", 1..2, 1..10){mode == "Normal"}

    private val targetDistance by int("TargetDistance", 3, 0..5){mode == "Normal"}
    private val AllowJump by boolean("AllowJump",false){mode == "Normal"}
    private val ADStrafe by boolean("ADStrafe", false){mode == "Normal"}
    private val DurationTime by int("ADStrafeDurationTick", 3,1..10) {ADStrafe && mode == "Normal"}

    private val restartForwardWhenBlockStop by boolean("RestartForwardWhenBlockStop",false)
    private val forwardTick by int("ForwardTick",1,0..10,"Ticks") {restartForwardWhenBlockStop}
    private val minEnemyRotDiffToIgnore by float("MinRotationDiffFromEnemyToIgnore", 180f, 0f..180f){mode == "Normal"}

    private val stopDuration by int("StopDuration",1,0..10,"Tick") { mode == "Simple" }
    private val onlyGround by boolean("OnlyGround", false)
    val onlyMove by boolean("OnlyMove", true)
    val onlyMoveForward by boolean("OnlyMoveForward", true)
    private val onlyWhenTargetGoesBack by boolean("OnlyWhenTargetGoesBack", false)
    private val onlyWhenNotBlocking by boolean("OnlyWhenNotBlocking",false)

    var forwardTicks = 0
    private var blockInputTicks = ticksUntilBlock.random()
    private var blockTicksElapsed = 0
    private var startWaiting = false
    private var blockInput = false
    private var allowInputTicks = reSprintTicks.random()
    private var ticksElapsed = 0
    private var strafeTimer = MSTimer()
    private var strafeDuration = 0
    private var randomSide = RandomUtils.nextBoolean()
    private var simpleModeTicks = 0
    private var wasBlockingInput = false

    override fun onToggle(state: Boolean) {
        blockInput = false
        startWaiting = false
        blockTicksElapsed = 0
        ticksElapsed = 0
        forwardTicks = 0
        wasBlockingInput = false
    }

    val onAttack = handler<AttackEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val target = event.targetEntity as? EntityLivingBase ?: return@handler
        val distance = player.getDistanceToEntityBox(target)

        if (!shouldActivateWTap(player, target, this.mode)) return@handler

        if (player.isSprinting && player.serverSprintState && !blockInput && !startWaiting) {
            val delayMultiplier = 1.0 / (abs(targetDistance - distance) + 1)
            randomSide = RandomUtils.nextBoolean()
            blockInputTicks = (ticksUntilBlock.random() * delayMultiplier).toInt()

            blockInput = blockInputTicks == 0

            if (!blockInput) {
                startWaiting = true
            }

            allowInputTicks = (reSprintTicks.random() * delayMultiplier).toInt()
        }
    }

    val onUpdate = handler<UpdateEvent> {
        if (mc.thePlayer?.hurtTime !in ownHurtTime) return@handler
        val targetHurtTime = KillAura.target?.hurtTime
        val pointedEntity = mc.pointedEntity as? EntityLivingBase

        if (wasBlockingInput && !blockInput && restartForwardWhenBlockStop) {
            forwardTicks = forwardTick
        }
        wasBlockingInput = blockInput

        if (forwardTicks > 0) {
            forwardTicks--
        }

        if (blockInput) {
            if (ticksElapsed++ >= allowInputTicks) {
                blockInput = false
                ticksElapsed = 0
            }
        } else {
            if (startWaiting) {
                blockInput = blockTicksElapsed++ >= blockInputTicks
                if (blockInput) {
                    startWaiting = false
                    blockTicksElapsed = 0
                }
            }
        }
        if ((targetHurtTime ?: (pointedEntity?.hurtTime ?: return@handler)) == 10) {
            simpleModeTicks = stopDuration
        }
    }

    private fun shouldActivateWTap(player: net.minecraft.client.entity.EntityPlayerSP, target: EntityLivingBase, mode: String): Boolean {
        if (onlyGround && !player.onGround) return false

        if (onlyMove && (!player.isMoving || onlyMoveForward && player.movementInput.moveStrafe != 0f)) return false

        if (mode == "Normal") {
            if (target.hurtTime !in targetHurtTime ||
                player.hurtTime !in ownHurtTime ||
                !MSTimer().hasTimePassed(delay.random().toLong()) ||
                RandomUtils.nextInt(endExclusive = 100) > chance
            ) return false

            val rotationToPlayer = toRotation(player.hitBox.center, false, target).fixedSensitivity().yaw
            val angleDifferenceToPlayer = abs(angleDifference(rotationToPlayer, target.rotationYaw))
            if (angleDifferenceToPlayer > minEnemyRotDiffToIgnore && !target.hitBox.isVecInside(player.eyes)) return false

            if (onlyWhenTargetGoesBack) {
                val pos = target.currPos - target.lastTickPos
                val distanceBasedOnMotion = player.getDistanceToBox(target.hitBox.offset(pos))
                if (distanceBasedOnMotion >= player.getDistanceToEntityBox(target)) return false
            }
        }

        return true
    }

    fun shouldBlockInput(): Boolean {
        if (onlyWhenNotBlocking && mc.thePlayer.isBlocking) return false
        when (mode) {
            "Normal" -> {
                if (handleEvents() && blockInput) {
                    val player = mc.thePlayer ?: return false
                    if (strafeDuration == 0 && ADStrafe) {
                        strafeDuration = DurationTime
                        strafeTimer.reset()
                        player.movementInput.moveStrafe = 0f
                    }
                    if (strafeTimer.hasTimePassed(strafeDuration.toLong()) && ADStrafe) {
                        player.movementInput.moveStrafe = 0f
                        strafeDuration = 0
                    } else if (mc.gameSettings.keyBindLeft.pressed || mc.gameSettings.keyBindRight.pressed && ADStrafe) {
                        if (mc.gameSettings.keyBindLeft.pressed && ADStrafe) {
                            mc.thePlayer.movementInput.moveStrafe = -1F
                        } else if (mc.gameSettings.keyBindRight.pressed && ADStrafe) {
                            mc.thePlayer.movementInput.moveStrafe = 1F
                        }
                    } else if (randomSide && ADStrafe) {
                        player.movementInput.moveStrafe = -1f
                    } else if (ADStrafe) {
                        player.movementInput.moveStrafe = 1f
                    }
                    if (AllowJump && player.onGround) {
                        player.jump()
                    }
                    return true
                }
            }
            "Simple" -> {
                if (simpleModeTicks != 0) {
                    simpleModeTicks--
                    return true
                }
            }
        }
        return false
    }
}

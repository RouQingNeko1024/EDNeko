//skid firebounce morekb
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.angleDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.*
import kotlin.math.abs

object MoreKB : Module("MoreKB", Category.COMBAT) {

    private val chance by int("Chance", 100, 0..100,"%")
    private val delay by intRange("Delay", 0..0, 0..500,"ms")
    private val targetHurtTime by intRange("TargetHurtTime", 0..10, 0..10)
    private val ownHurtTime by intRange("OwnHurtTime",0..10,0..10)

    private val mode by choices(
        "Mode",
        arrayOf("Legit", "Legit2", "SprintTap", "SprintTap2", "Old", "Silent", "Packet","FullPacket", "SneakPacket"),
        "Legit"
    )

    private val ticksUntilBlock by intRange("TicksUntilBlock", 0..2, 0..10) { mode == "Legit" }
    private val reSprintTicks by intRange("ReSprintTicks", 1..2, 1..10) { mode == "Legit" }

    private val targetDistance by int("TargetDistance", 3, 0..5) { mode == "Legit" }
    private val AllowJump by boolean("AllowJump",false) {mode == "Legit"}
    private val ADStrafe by boolean("ADStrafe", false) {mode == "Legit"}
    private val DurationTime by int("ADStrafeDurationTick", 3,1..10) {mode == "Legit" && ADStrafe}

    private val stopTicks: Value<Int> = int("PressBackTicks", 1, 1..5) {
        mode == "SprintTap2"
    }.onChange { _, new ->
        new.coerceAtMost(unSprintTicks.get())
    }
    private val unSprintTicks: Value<Int> = int("ReleaseBackTicks", 2, 1..5) {
        mode == "SprintTap2"
    }.onChange { _, new ->
        new.coerceAtLeast(stopTicks.get())
    }

    private val minEnemyRotDiffToIgnore by float("MinRotationDiffFromEnemyToIgnore", 180f, 0f..180f)

    private val onlyGround by boolean("OnlyGround", false)
    val onlyMove by boolean("OnlyMove", true)
    val onlyMoveForward by boolean("OnlyMoveForward", true) { onlyMove }
    private val onlyWhenTargetGoesBack by boolean("OnlyWhenTargetGoesBack", false)

    private var ticks = 0
    private var attackTicks = 0
    private var forceSprintState = 0
    private val timer = MSTimer()
    private var randomSide = RandomUtils.nextBoolean()

    private var blockInputTicks = ticksUntilBlock.random()
    private var blockTicksElapsed = 0
    private var startWaiting = false
    private var blockInput = false
    private var allowInputTicks = reSprintTicks.random()
    private var ticksElapsed = 0
    private var strafeTimer = MSTimer()
    private var strafeDuration = 0

    private var sprintTicks = 0

    override fun onToggle(state: Boolean) {
        blockInput = false
        startWaiting = false
        blockTicksElapsed = 0
        ticksElapsed = 0
        sprintTicks = 0
    }

    val onAttack = handler<AttackEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val target = event.targetEntity as? EntityLivingBase ?: return@handler
        val distance = player.getDistanceToEntityBox(target)

        val rotationToPlayer = toRotation(player.hitBox.center, false, target).fixedSensitivity().yaw
        val angleDifferenceToPlayer = abs(angleDifference(rotationToPlayer, target.rotationYaw))

        if (event.targetEntity.hurtTime !in targetHurtTime ||
            mc.thePlayer.hurtTime !in ownHurtTime ||
            !timer.hasTimePassed(delay.random()) ||
            onlyGround && !player.onGround ||
            RandomUtils.nextInt(
                endExclusive = 100
            ) > chance
        ) return@handler
        if (onlyMove && (!player.isMoving || onlyMoveForward && player.movementInput.moveStrafe != 0f)) return@handler

        if (angleDifferenceToPlayer > minEnemyRotDiffToIgnore && !target.hitBox.isVecInside(player.eyes)) return@handler

        val pos = target.currPos - target.lastTickPos

        val distanceBasedOnMotion = player.getDistanceToBox(target.hitBox.offset(pos))

        if (onlyWhenTargetGoesBack && distanceBasedOnMotion >= player.getDistanceToEntityBox(target)) return@handler

        when (mode) {
            "FullPacket" -> {
                mc.netHandler.addToSendQueue(
                    C0BPacketEntityAction(
                        mc.thePlayer,
                        START_SPRINTING
                    )
                )
                if (mc.thePlayer.isSprinting)
                    mc.thePlayer.isSprinting = true
                mc.thePlayer.serverSprintState = true
            }
            "Old" -> {
                if (player.isSprinting) {
                    sendPacket(C0BPacketEntityAction(player, STOP_SPRINTING))
                }

                sendPackets(
                    C0BPacketEntityAction(player, START_SPRINTING),
                    C0BPacketEntityAction(player, STOP_SPRINTING),
                    C0BPacketEntityAction(player, START_SPRINTING)
                )
                player.isSprinting = true
                player.serverSprintState = true
            }

            "SprintTap", "Silent" -> if (player.isSprinting && player.serverSprintState) ticks = 2

            "Packet" -> {
                sendPackets(
                    C0BPacketEntityAction(player, STOP_SPRINTING),
                    C0BPacketEntityAction(player, START_SPRINTING)
                )
            }

            "SneakPacket" -> {
                sendPackets(
                    C0BPacketEntityAction(player, STOP_SPRINTING),
                    C0BPacketEntityAction(player, START_SNEAKING),
                    C0BPacketEntityAction(player, START_SPRINTING),
                    C0BPacketEntityAction(player, STOP_SNEAKING)
                )
            }

            "Legit" -> {
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
            "Legit2" -> {
                attackTicks = 2
            }

            "SprintTap2" -> {
                if (++sprintTicks == stopTicks.get()) {

                    if (player.isSprinting && player.serverSprintState) {
                        player.isSprinting = false
                        player.serverSprintState = false
                    } else {
                        player.isSprinting = true
                        player.serverSprintState = true
                    }

                    mc.thePlayer.stopXZ()

                } else if (sprintTicks >= unSprintTicks.get()) {

                    player.isSprinting = false
                    player.serverSprintState = false

                    sprintTicks = 0
                }
            }
        }

        timer.reset()
    }

    @Suppress("unused")
    val onPostSprintUpdate = handler<PostSprintUpdateEvent> {
        if (mc.thePlayer.hurtTime !in ownHurtTime) return@handler
        val player = mc.thePlayer ?: return@handler
        if (mode == "SprintTap") {
            when (ticks) {
                2 -> {
                    player.isSprinting = false
                    forceSprintState = 2
                    ticks--
                }

                1 -> {
                    if (player.movementInput.moveForward > 0.8) {
                        player.isSprinting = true
                    }
                    forceSprintState = 1
                    ticks--
                }

                else -> {
                    forceSprintState = 0
                }
            }
        }
    }

    val onUpdate = handler<UpdateEvent> {
        if (mc.thePlayer.hurtTime !in ownHurtTime) return@handler
        when (mode) {
            "Legit" -> {
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
            }
            "Legit2" -> {
                if (mc.thePlayer.isMoving) {
                    if (attackTicks == 2) {
                        mc.thePlayer.isSprinting = false
                        attackTicks = 1
                    } else if (attackTicks == 1) {
                        mc.thePlayer.isSprinting = true
                        attackTicks = 0
                    }
                } else if (attackTicks != 0) attackTicks = 0
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (mc.thePlayer?.hurtTime !in ownHurtTime) return@handler
        val player = mc.thePlayer ?: return@handler
        val packet = event.packet
        if (packet is C03PacketPlayer && mode == "Silent") {
            if (ticks == 2) {
                sendPacket(C0BPacketEntityAction(player, STOP_SPRINTING))
                ticks--
            } else if (ticks == 1 && player.isSprinting) {
                sendPacket(C0BPacketEntityAction(player, START_SPRINTING))
                ticks--
            }
        }

    }

    fun shouldBlockInput(): Boolean {

        if (handleEvents() && mode == "Legit" && blockInput) {
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
        return false
    }

    override val tag
        get() = mode

    fun breakSprint() = handleEvents() && forceSprintState == 2 && mode == "SprintTap"
    fun startSprint() = handleEvents() && forceSprintState == 1 && mode == "SprintTap"
}

package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.util.MathHelper
import java.util.*

object AntiKnockBack : Module("AntiKnockBack", Category.COMBAT) {

    // 全局模式设置
    private val globalMode by choices(
        "GlobalMode",
        arrayOf("Custom", "Intave"),
        "Custom"
    )

    // 标签设置
    private val tagState by boolean("ModuleTag", true)
    private val tagMode by choices("TagMode", arrayOf("Default", "Custom"), "Default")
    private val customText by text("CustomTagText", "")
    private val authorName by boolean("AuthorName", true)
    private val sprintAfterReduce by boolean("SprintAfterReduce", false)

    // Modify模式设置
    private val modifyMode by boolean("Modify", false)
    private val modifyMinHurtTime by int("ModifyMinHurtTime", 10, 0..10)
    private val modifyMaxHurtTime by int("ModifyMaxHurtTime", 10, 0..10)
    private val modifyHorizonFactor by float("ModifyHorizonFactor", 0.0f, -2.0f..2.0f)
    private val modifyVerticalFactor by float("ModifyVerticalFactor", 0.0f, -2.0f..2.0f)
    private val modifyDebug by boolean("ModifyDebug", false)

    // ClickReduce设置
    private val clickReduce by boolean("ClickReduce", false)
    private val clickMode by choices("ClickMode", arrayOf("Packet", "Simulation"), "Packet")
    private val clicksPerTime by int("ClicksPerTime", 1, 1..10)
    private val crTicks by int("ClickReduceWorkTicks", 1, 1..10)
    private val workTicks by int("WorkTicks", 10, 3..10)
    private val extraClickWhenFirstClickReduce by boolean("ExtraClickWhenFirstClickReduce", false)
    private val extraCount by int("ExtraCount", 1, 1..10)
    private val clickExtraReduce by boolean("ClickExtraReduce", false)
    private val clickExtraTime by choices("ClickExtraReduceTime", arrayOf("BeforeClick", "AfterClick"), "BeforeClick")
    private val maxClickExtraReduceHurtTime by int("MaxClickExtraReduceHurtTime", 10, 0..10)
    private val minClickExtraReduceHurtTime by int("MinClickExtraReduceHurtTime", 0, 0..10)
    private val clickExtraReduceFactorOnGround by float("ClickExtraReduceFactorOnGround", 1.0f, 0.0f..1.0f)
    private val clickExtraReduceFactorInAir by float("ClickExtraReduceFactorInAir", 1.0f, 0.0f..1.0f)
    private val clickNoHurtTrigger by boolean("WhenNoHurtBecomeAttackExtraClick", false)
    private val clickNoHurtCooldown by int("ClickNoHurtDelay", 1000, 100..2000)
    private val clickExtraReduceDebug by boolean("ClickReduceDebug", false)

    // SmartBlocking设置
    private val smartBlocking by boolean("SmartBlockingTest", false)
    private val stbForwardTicks by int("AllowSmartBlockingAfterForwardTicks", 40, 1..60)
    private val stbTicks by int("SmartBlockingWorkTicks", 3, 1..10)
    private val stbMinDistance by float("SmartBlockingMinDistance", 1.0f, 0.0f..5.0f)

    // KeepRange设置
    private val keepRange by boolean("KeepRange", false)
    private val krAllowJump by boolean("KeepRangeAllowJump", false)
    private val krAllowJumpLevel by float("KeepRangeAllowJumpLevel", 0.5f, 0.0f..1.0f)
    private val krDuration by int("KeepRangeDuration", 3, 1..10)
    private val krMaxDistance by float("KeepRangeMaxDistance", 3.0f, 1.0f..7.0f)
    private val krMinDistance by float("KeepRangeMinDistance", 2.5f, 1.0f..7.0f)

    // JumpReset设置
    private val jumpReset by boolean("JumpReset", false)
    private val badEnvironmentCheck by boolean("BadEnvironmentCheck", false)
    private val jumpResetMinRange by float("JumpResetMinRange", 0.0f, 0.0f..10.0f)
    private val doubleJump by boolean("DoubleJump", false)
    private val onlyWhenSprint by boolean("OnlyWhenSprint", false)
    private val jumpSprintControl by choices("JumpSprintControl", arrayOf("None", "Sprint", "Stop"), "None")
    private val jumpMode by choices("JumpMode", arrayOf("PacketJump", "VanillaJump"), "VanillaJump")
    private val ticksUntilJump by int("TicksUntilJump", 0, 0..100)
    private val jumpProbability by int("JumpChance", 100, 0..100)
    private val jumpMaxHurtTime by int("JumpMaxHurtTime", 9, 0..10)
    private val jumpMinHurtTime by int("JumpMinHurtTime", 9, 0..10)
    private val jumpExtraReduce by boolean("JumpExtraReduce", false)
    private val jumpExtraReduceTime by choices("JumpExtraReduceTime", arrayOf("BeforeJump", "AfterJump"), "BeforeJump")
    private val jumpExtraReduceFactor by float("JumpExtraReduceFactor", 1.0f, 0.0f..1.0f)
    private val jumpExtraReduceDebug by boolean("JumpResetDebug", false)

    // AttackReduce设置
    private val attackReduce by boolean("AttackReduce", false)
    private val tryForwardAfterReduce by boolean("TryForwardAfterReduce", false)
    private val attackFactorOnGround by float("AttackFactorOnGround", 60.0f, 0.0f..100.0f)
    private val attackFactorInAir by float("AttackFactorInAir", 60.0f, 0.0f..100.0f)
    private val attackReduceYFactor by float("AttackReduceYFactor", 1.0f, 0.0f..1.0f)
    private val maxReduceHurtTime by int("MaxReduceHurtTime", 9, 1..10)
    private val minReduceHurtTime by int("MinReduceHurtTime", 9, 1..10)
    private val firstReduceDelay by int("FirstReduceDelay", 0, 0..200)
    private val multiReduce by boolean("MultiReducePerHurtTime", false)
    private val reduceTriggerHurtTimeDelay by int("ReduceTriggerHurtTimeDelay", 0, 0..10)
    private val maxMultiReduceTimes by int("MaxMultiReduceTimes", 3, 1..10)
    private val firstReduceExtra by boolean("FirstReduceExtra", false)
    private val firstReduceExtraFactor by float("FirstReduceExtraFactor", 0.0f, -100.0f..100.0f)
    private val progressiveFactor by boolean("ProgressiveFactor", false)
    private val progressiveMode by choices("ProgressiveMode", arrayOf("Decrease", "Increase"), "Decrease")
    private val progressiveStep by float("ProgressiveStep", 5.0f, 0.0f..200.0f)
    private val progressiveMaxFactor by float("ProgressiveMaxFactor", 100.0f, -200.0f..200.0f)
    private val progressiveMinFactor by float("ProgressiveMinFactor", 0.0f, -200.0f..200.0f)
    private val reversalMode by boolean("AttackReversalMode", false)
    private val reversalDelay by int("ReversalTriggerDelay", 2, 0..5)
    private val reversalMaxHurtTime by int("ReversalMaxHurtTime", 9, 1..10)
    private val reversalMinHurtTime by int("ReversalMinHurtTime", 9, 1..10)
    private val reversalXModifier by float("ReversalXFactor", 0.5f, -1.0f..1.0f)
    private val reversalZModifier by float("ReversalZFactor", 0.5f, -1.0f..1.0f)
    private val onlyMovingReversal by boolean("ReversalOnlyMoving", false)
    private val onlyInAir by boolean("ReversalOnlyInAir", false)
    private val attackReduceDebug by boolean("AttackReduceDebug", false)

    // MatrixReduce设置
    private val matrixEnabled by boolean("MatrixReduce", false)
    private val matrixHReduce by float("MatrixReduceFactor", 0.6f, 0.0f..1.0f)
    private val matrixDebug by boolean("MatrixReduceDebug", false)

    // 变量声明
    private var attackTicks = 0
    private var hurtTimeCounter = 0
    private var checkTicks = 0
    private var tx = 0.0
    private var tz = 0.0
    private var x = 0.0
    private var z = 0.0
    private var timerA = 0
    private var timerB = 0
    private var timerC = 0
    private var hasHurt = 0
    private var tHurtTime = 0
    private var generalSpeed = 0
    private var allowTimerA = false
    private var allowTimerB = false
    private var allowS = false
    private var allowJump = false
    private var allowTimerC = false
    private var ticksUntilJumpCounter = 0
    private var hasReceivedVelocity = false
    private var hasReceivedVelocity2 = false
    private var hasReceivedVelocity3 = false
    private var hasReceivedVelocity4 = false
    private var jumpCooldownTicks = 0
    private var damageXmotion = 0.0
    private var damageZmotion = 0.0
    private var damageXmotion2 = 0.0
    private var damageZmotion2 = 0.0
    private var fakeSwingMode = false
    private var jumpHurtTime = 9
    private var isAttacking = false
    private var isAttackingTicks = 0
    private var dexlandCount = 0
    private var dexlandLastAttackTime = 0L
    private var lastHurtTime = 0L
    private var lastReduceHurtTime = 0
    private var isFirstReduce = true
    private var hasTriggeredExtraClick = false
    private var multiReduceCount = 0
    private var firstReduceStartTime = 0L
    private var firstReduceTimer = 0
    private var canTriggerFirstReduce = false
    private var reversalTicks = 0
    private var isReversalActive = false
    private var triggeredReversal = false

    private var target: net.minecraft.entity.Entity? = null

    // 获取KillAura模块
    private val killAuraModule: net.ccbluex.liquidbounce.features.module.Module?
        get() = net.ccbluex.liquidbounce.features.module.ModuleManager.getModule("KillAura")

    // 获取KillAura的Range值
    private val range: Float
        get() {
            val killAura = killAuraModule ?: return 3.0f
            try {
                val field = killAura.javaClass.getDeclaredField("range")
                field.isAccessible = true
                return (field.get(killAura) as? Float) ?: 3.0f
            } catch (e: Exception) {
                return 3.0f
            }
        }

    // 获取KillAura的HurtTime值
    private val clickHurtTime: Int
        get() {
            val killAura = killAuraModule ?: return 10
            try {
                val field = killAura.javaClass.getDeclaredField("hurtTime")
                field.isAccessible = true
                return (field.get(killAura) as? Int) ?: 10
            } catch (e: Exception) {
                return 10
            }
        }

    private fun mol(x: Double, y: Double, z: Double): Double {
        return MathHelper.sqrt_double(x * x + y * y + z * z).toDouble()
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        val player = mc.thePlayer ?: return@handler

        if (packet is S12PacketEntityVelocity && packet.entityID == player.entityId) {
            hasReceivedVelocity = true
            hasReceivedVelocity2 = true
            hasReceivedVelocity4 = true
            lastHurtTime = System.currentTimeMillis()
            isFirstReduce = true
            hasTriggeredExtraClick = false
            firstReduceTimer = 0
            firstReduceStartTime = 0
            canTriggerFirstReduce = false
            triggeredReversal = false
        } else if (packet is S27PacketExplosion) {
            hasReceivedVelocity = true
            hasReceivedVelocity2 = false
            hasReceivedVelocity4 = false
            lastHurtTime = System.currentTimeMillis()
            isFirstReduce = true
            hasTriggeredExtraClick = false
            firstReduceTimer = 0
            firstReduceStartTime = 0
            canTriggerFirstReduce = false
            triggeredReversal = false
        }
    }

    override fun onEnable() {
        attackTicks = 0
        checkTicks = 0
        hasReceivedVelocity = false
        hasReceivedVelocity2 = false
        ticksUntilJumpCounter = 0
        allowJump = false
        jumpCooldownTicks = 0
        fakeSwingMode = false
        isAttacking = false
        isAttackingTicks = 0
        isFirstReduce = true
        multiReduceCount = 0
    }

    override fun onDisable() {
        attackTicks = 0
        checkTicks = 0
        hasReceivedVelocity = false
        hasReceivedVelocity2 = false
        ticksUntilJumpCounter = 0
        allowJump = false
        jumpCooldownTicks = 0
        fakeSwingMode = false
        isAttacking = false
        isAttackingTicks = 0
        isFirstReduce = true
        multiReduceCount = 0
    }

    val onAttack = handler<AttackEvent> { event ->
        target = event.targetEntity
        attackTicks++
        isAttacking = true
        isAttackingTicks = 0

        // AttackReduce部分逻辑
        if (attackReduce && hasReceivedVelocity2 && globalMode == "Custom") {
            val player = mc.thePlayer ?: return@handler
            val currentHurtTime = player.hurtTime
            var canTriggerReduce = true

            // 重置计数器当不在有效的HurtTime范围内时
            if (currentHurtTime < minReduceHurtTime || currentHurtTime > maxReduceHurtTime) {
                multiReduceCount = 0
                firstReduceStartTime = 0
                canTriggerFirstReduce = false
            }

            // 检查是否需要延迟第一次Reduce
            if (currentHurtTime >= minReduceHurtTime &&
                currentHurtTime <= maxReduceHurtTime &&
                !canTriggerFirstReduce) {
                if (firstReduceStartTime == 0L) {
                    firstReduceStartTime = System.currentTimeMillis()
                }

                val currentTime = System.currentTimeMillis()
                if (currentTime - firstReduceStartTime >= firstReduceDelay) {
                    canTriggerFirstReduce = true
                    firstReduceStartTime = 0
                    if (attackReduceDebug) {
                        mc.thePlayer.addChatMessage(
                            net.minecraft.util.ChatComponentText(
                                "[AttackReduce] First reduce delay ended | Delay: $firstReduceDelay ms"
                            )
                        )
                    }
                }
            }

            if (reduceTriggerHurtTimeDelay >= 1 &&
                multiReduce &&
                lastReduceHurtTime > 0) {
                val requiredDelay = reduceTriggerHurtTimeDelay
                canTriggerReduce = (currentHurtTime <= lastReduceHurtTime - requiredDelay) ||
                        (currentHurtTime >= lastReduceHurtTime + requiredDelay)
            }

            // 添加最大次数限制检查
            if (multiReduce && multiReduceCount >= maxMultiReduceTimes) {
                canTriggerReduce = false
                if (attackReduceDebug) {
                    mc.thePlayer.addChatMessage(
                        net.minecraft.util.ChatComponentText(
                            "Max reduce times reached: $multiReduceCount"
                        )
                    )
                }
            }

            if (canTriggerReduce &&
                minReduceHurtTime <= currentHurtTime &&
                currentHurtTime <= maxReduceHurtTime &&
                (multiReduceCount > 0 || canTriggerFirstReduce)) {
                var currentFactorOnGround = attackFactorOnGround
                var currentFactorInAir = attackFactorInAir

                // 如果启用渐进式因子
                if (progressiveFactor && multiReduce) {
                    val step = progressiveStep
                    val minFactor = progressiveMinFactor
                    val maxFactor = progressiveMaxFactor

                    if (progressiveMode == "Decrease") {
                        // 递减模式（确保不低于最小值）
                        currentFactorOnGround = kotlin.math.max(
                            minFactor,
                            attackFactorOnGround - (step * multiReduceCount)
                        )
                        currentFactorInAir = kotlin.math.max(
                            minFactor,
                            attackFactorInAir - (step * multiReduceCount)
                        )
                    } else {
                        // 递增模式（确保不超过最大值）
                        currentFactorOnGround = kotlin.math.min(
                            maxFactor,
                            attackFactorOnGround + (step * multiReduceCount)
                        )
                        currentFactorInAir = kotlin.math.min(
                            maxFactor,
                            attackFactorInAir + (step * multiReduceCount)
                        )
                    }

                    if (attackReduceDebug) {
                        mc.thePlayer.addChatMessage(
                            net.minecraft.util.ChatComponentText(
                                "Progressive factor | Mode: $progressiveMode | Step: $step% | Range: [${minFactor}% ~ ${maxFactor}%] | Current OnGround: ${currentFactorOnGround}% | Current InAir: ${currentFactorInAir}%"
                            )
                        )
                    }

                    if (tryForwardAfterReduce) {
                        mc.gameSettings.keyBindForward.pressed = true
                    }
                }

                // 首次额外减少逻辑
                if (firstReduceExtra && isFirstReduce) {
                    val groundFactor = (currentFactorOnGround / 100f) - (firstReduceExtraFactor / 100f)
                    val airFactor = (currentFactorInAir / 100f) - (firstReduceExtraFactor / 100f)

                    if (player.onGround) {
                        player.motionX *= groundFactor.toDouble()
                        player.motionZ *= groundFactor.toDouble()
                    } else {
                        player.motionX *= airFactor.toDouble()
                        player.motionZ *= airFactor.toDouble()
                    }

                    if (attackReduceDebug) {
                        mc.thePlayer.addChatMessage(
                            net.minecraft.util.ChatComponentText(
                                "First reduce factor | OnGround: ${groundFactor * 100}% | InAir: ${airFactor * 100}% | HurtTime: $currentHurtTime"
                            )
                        )
                    }

                    if (tryForwardAfterReduce) {
                        mc.gameSettings.keyBindForward.pressed = true
                    }

                    if (sprintAfterReduce) {
                        player.isSprinting = true
                    }
                    isFirstReduce = false
                }
                // 正常减少逻辑（仅在未启用首次额外减少或不是首次时执行）
                else if (!firstReduceExtra || !isFirstReduce) {
                    if (player.onGround) {
                        player.motionX *= (currentFactorOnGround / 100f).toDouble()
                        player.motionZ *= (currentFactorOnGround / 100f).toDouble()
                        if (attackReduceDebug) {
                            mc.thePlayer.addChatMessage(
                                net.minecraft.util.ChatComponentText(
                                    "OnGroundReduce | HurtTime: $currentHurtTime | Reduce factor: $currentFactorOnGround% | Count: ${multiReduceCount + 1}/$maxMultiReduceTimes"
                                )
                            )
                        }
                    } else {
                        player.motionX *= (currentFactorInAir / 100f).toDouble()
                        player.motionZ *= (currentFactorInAir / 100f).toDouble()
                        if (attackReduceDebug) {
                            mc.thePlayer.addChatMessage(
                                net.minecraft.util.ChatComponentText(
                                    "InAirReduce | HurtTime: $currentHurtTime | Reduce factor: $currentFactorInAir% | Count: ${multiReduceCount + 1}/$maxMultiReduceTimes"
                                )
                            )
                        }
                        if (tryForwardAfterReduce) {
                            mc.gameSettings.keyBindForward.pressed = true
                        }
                    }

                    if (sprintAfterReduce) {
                        player.isSprinting = true
                    }
                }

                player.motionY *= attackReduceYFactor.toDouble()
                lastReduceHurtTime = currentHurtTime
                multiReduceCount++

                if (!multiReduce) {
                    hasReceivedVelocity2 = false
                }
            }
        }

        // Reversal模式逻辑
        val player = mc.thePlayer ?: return@handler
        if (reversalMode && hasReceivedVelocity4 && globalMode == "Custom") {
            if (!isReversalActive && player.hurtTime >= reversalMinHurtTime &&
                reversalMaxHurtTime >= player.hurtTime && !triggeredReversal) {
                reversalTicks = 0
                triggeredReversal = true
                isReversalActive = true
            }

            if (isReversalActive) {
                reversalTicks++

                if (reversalTicks >= reversalDelay) {
                    // 反转运动速度
                    if (onlyInAir && player.onGround) {
                        isReversalActive = false
                        return@handler
                    }
                    if (onlyMovingReversal && !player.isMoving) {
                        isReversalActive = false
                        return@handler
                    }
                    player.motionX *= (-reversalXModifier).toDouble()
                    player.motionZ *= (-reversalZModifier).toDouble()
                    if (attackReduceDebug) {
                        mc.thePlayer.addChatMessage(
                            net.minecraft.util.ChatComponentText(
                                "[Reversal] KB Blocked! XFactor: ${reversalXModifier * 100}% | ZFactor: ${reversalZModifier * 100}% | HurtTime: ${player.hurtTime}"
                            )
                        )
                    }
                    if (tryForwardAfterReduce) {
                        mc.gameSettings.keyBindForward.pressed = true
                    }

                    isReversalActive = false
                }
            }
        }

        // Intave模式专用逻辑
        if (globalMode == "Intave" && hasReceivedVelocity2) {
            val currentHurtTime = player.hurtTime

            // Intave模式只在hurtTime=9时生效
            if (currentHurtTime == 9) {
                val intaveFactor = 0.6f

                player.motionX *= intaveFactor.toDouble()
                player.motionZ *= intaveFactor.toDouble()

                if (sprintAfterReduce) {
                    player.isSprinting = true
                }
            }
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        val target = this.target ?: return@handler

        tx = target.posX
        tz = target.posZ
        x = player.posX
        z = player.posZ
        val dx = tx - x
        val dz = tz - z
        val ax = target.motionX
        val az = target.motionZ
        val distanceToTarget = MathHelper.sqrt_double(dx * dx + dz * dz).toDouble()

        if (player.hurtTime != 0) {
            hurtTimeCounter++
        } else {
            hurtTimeCounter = 0
        }
        if ((target as? net.minecraft.entity.EntityLivingBase)?.hurtTime != 0) {
            tHurtTime++
        } else {
            tHurtTime = 0
        }

        if (isAttacking) {
            isAttackingTicks++
            if (isAttackingTicks >= 2) {
                isAttacking = false
                isAttackingTicks = 0
            }
        }

        if (jumpCooldownTicks > 0) {
            jumpCooldownTicks--
        }

        // AttackReduce部分计算第一次Reduce延迟
        if (globalMode == "Custom" &&
            player.hurtTime >= minReduceHurtTime &&
            player.hurtTime <= maxReduceHurtTime &&
            !canTriggerFirstReduce) {
            // 使用系统时间计算延迟
            if (firstReduceStartTime == 0L) {
                firstReduceStartTime = System.currentTimeMillis()
            }

            val currentTime = System.currentTimeMillis()
            if (currentTime - firstReduceStartTime >= firstReduceDelay) {
                canTriggerFirstReduce = true
                firstReduceStartTime = 0 // 重置计时器
            }
        }

        // Modify模式逻辑
        if (modifyMode && player.hurtTime > 0 && globalMode == "Custom") {
            if (player.hurtTime >= modifyMinHurtTime &&
                player.hurtTime <= modifyMaxHurtTime) {
                player.motionX *= modifyHorizonFactor.toDouble()
                player.motionZ *= modifyHorizonFactor.toDouble()
                player.motionY *= modifyVerticalFactor.toDouble()

                if (modifyDebug) {
                    mc.thePlayer.addChatMessage(
                        net.minecraft.util.ChatComponentText(
                            "[Modify] Applied | Horizon: ${(modifyHorizonFactor * 100).toInt()}% | Vertical: ${(modifyVerticalFactor * 100).toInt()}%"
                        )
                    )
                }
                if (sprintAfterReduce) {
                    player.isSprinting = true
                }
            }
        }

        // MatrixReduce 逻辑
        if (matrixEnabled && player.hurtTime > 0 && globalMode == "Custom") {
            // 检测玩家是否正在移动
            if (player.moveForward != 0f || player.moveStrafing != 0f) {
                // 应用 MatrixAttackReduceFactor
                player.motionX *= matrixHReduce.toDouble()
                player.motionZ *= matrixHReduce.toDouble()
                if (matrixDebug) {
                    mc.thePlayer.addChatMessage(
                        net.minecraft.util.ChatComponentText(
                            "Matrix Reduced - HurtTime: ${player.hurtTime}"
                        )
                    )
                }
            }
        }

        // IntaveJumpReset 逻辑
        if (hasReceivedVelocity && globalMode == "Intave") {
            // 环境检查
            if (player.isInWater || player.isInLava ||
                player.isRiding || player.isBurning) {
                return@handler
            }

            // 触发条件
            if (player.onGround &&
                player.hurtTime == 9 &&
                player.isSprinting) {
                if (Math.random() * 100 <= 20) {  // 20%概率
                    player.jump()
                    hasReceivedVelocity = false
                }
            }
        }

        // JumpReset 逻辑
        if (jumpReset && hasReceivedVelocity && globalMode == "Custom") {
            var canJump = true
            if (badEnvironmentCheck) {
                // 检查是否在水中、岩浆中、载具中或燃烧中
                if (player.isInWater ||
                    player.isInLava ||
                    player.isRiding ||
                    player.isBurning) {
                    canJump = false
                    if (jumpExtraReduceDebug) {
                        mc.thePlayer.addChatMessage(
                            net.minecraft.util.ChatComponentText(
                                "[Jump] Canceled due to bad environment"
                            )
                        )
                    }
                }
            }
            if (canJump &&
                player.onGround &&
                player.hurtTime <= jumpMaxHurtTime &&
                player.hurtTime >= jumpMinHurtTime &&
                jumpCooldownTicks <= 0 &&
                distanceToTarget >= jumpResetMinRange.toDouble()) {

                // 新增 OnlyWhenSprint 判断
                if (onlyWhenSprint && !player.isSprinting) {
                    // 如果 OnlyWhenSprint 开启，且玩家没有疾跑，则不触发 JumpReset
                    return@handler
                }

                val jumpChance = Math.random() * 100
                if (jumpChance <= jumpProbability) {
                    // ExtraReduce 跳跃前执行
                    if (jumpExtraReduce && jumpExtraReduceTime == "BeforeJump") {
                        player.motionX *= jumpExtraReduceFactor.toDouble()
                        player.motionZ *= jumpExtraReduceFactor.toDouble()
                        if (jumpExtraReduceDebug) {
                            mc.thePlayer.addChatMessage(
                                net.minecraft.util.ChatComponentText(
                                    "[JumpExtra] Pre-Reduce | Factor: ${(jumpExtraReduceFactor * 100).toInt()}%"
                                )
                            )
                        }
                    }

                    // 执行跳跃
                    if (jumpMode == "VanillaJump") {
                        player.jump()
                        if (doubleJump) {
                            player.jump()
                            if (jumpExtraReduceDebug) {
                                mc.thePlayer.addChatMessage(
                                    net.minecraft.util.ChatComponentText(
                                        "[Jump] DoubleJump | HurtTime: ${player.hurtTime}"
                                    )
                                )
                            }
                        }
                        if (jumpExtraReduceDebug) {
                            mc.thePlayer.addChatMessage(
                                net.minecraft.util.ChatComponentText(
                                    "[Jump] VanillaJump | HurtTime: ${player.hurtTime}"
                                )
                            )
                        }
                    }
                    else if (jumpMode == "PacketJump") {
                        player.motionY = 0.42
                        if (jumpExtraReduceDebug) {
                            mc.thePlayer.addChatMessage(
                                net.minecraft.util.ChatComponentText(
                                    "[Jump] PacketJump | HurtTime: ${player.hurtTime}"
                                )
                            )
                        }
                    }
                    allowJump = true

                    // ExtraReduce 跳跃后执行
                    if (jumpExtraReduce && jumpExtraReduceTime == "AfterJump") {
                        player.motionX *= jumpExtraReduceFactor.toDouble()
                        player.motionZ *= jumpExtraReduceFactor.toDouble()
                        if (jumpExtraReduceDebug) {
                            mc.thePlayer.addChatMessage(
                                net.minecraft.util.ChatComponentText(
                                    "[JumpExtra] Post-Reduce | Factor: ${(jumpExtraReduceFactor * 100).toInt()}%"
                                )
                            )
                        }
                    }

                    // 疾跑控制
                    if (jumpSprintControl == "Stop") {
                        player.isSprinting = false
                    }
                    if (jumpSprintControl == "Sprint") {
                        player.isSprinting = true
                    }
                    if (jumpExtraReduceDebug) {
                        mc.thePlayer.addChatMessage(
                            net.minecraft.util.ChatComponentText(
                                "[Jump] Sprint Control: ${player.isSprinting}"
                            )
                        )
                    }

                    // 重置状态
                    hasReceivedVelocity = false
                    jumpCooldownTicks = ticksUntilJump
                }
            }
        }

        // ClickReduce 逻辑
        if (clickReduce && globalMode == "Custom") {
            val currentTime = System.currentTimeMillis()

            // NoHurtClick 逻辑
            if (clickNoHurtTrigger && player.hurtTime == 0 && currentTime - lastHurtTime >= clickNoHurtCooldown) {
                if (isAttacking) {
                    val distance = player.getDistanceToEntity(target)
                    if (distance <= range) {
                        // 按照 crTicks 设置触发点击
                        for (tick in 0 until crTicks) {
                            // 按照 ClicksPerTime 设置触发点击
                            for (num in 0 until clicksPerTime) {
                                if (clickMode == "Packet") {
                                    player.swingItem()
                                    mc.playerController.attackEntity(player, target)
                                } else { // Simulation mode
                                    mc.gameSettings.keyBindAttack.pressed = true
                                    mc.gameSettings.keyBindAttack.pressed = false
                                }
                                isAttacking = true
                                if (clickExtraReduceDebug) {
                                    mc.thePlayer.addChatMessage(
                                        net.minecraft.util.ChatComponentText(
                                            "[NoHurtClick] Clicked | No Hurt Time: ${currentTime - lastHurtTime} | Tick: $tick | Click: $num"
                                        )
                                    )
                                }

                                // 如果 ExtraClickWhenFirstClickReduce 开启，并且是首次点击减少，触发额外点击
                                if (extraClickWhenFirstClickReduce && !hasTriggeredExtraClick) {
                                    for (i in 0 until extraCount) {
                                        if (clickMode == "Packet") {
                                            player.swingItem()
                                            mc.playerController.attackEntity(player, target)
                                        } else { // Simulation mode
                                            mc.gameSettings.keyBindAttack.pressed = true
                                            mc.playerController.sendUseItem(player, mc.theWorld, player.heldItem)
                                            mc.gameSettings.keyBindAttack.pressed = false
                                        }
                                        if (sprintAfterReduce) {
                                            player.isSprinting = true
                                        }
                                        if (clickExtraReduceDebug) {
                                            mc.thePlayer.addChatMessage(
                                                net.minecraft.util.ChatComponentText(
                                                    "[NoHurtClick] Extra Click | No Hurt Time: ${currentTime - lastHurtTime} | Tick: $tick | Extra Click: $i"
                                                )
                                            )
                                        }
                                    }
                                    hasTriggeredExtraClick = true
                                }
                            }
                        }
                        lastHurtTime = currentTime
                    }
                }
            }

            // 正常 ClickReduce 逻辑
            if (player.hurtTime != 0 && hurtTimeCounter <= crTicks) {
                val distance = player.getDistanceToEntity(target)
                if (distance <= range) {
                    for (num in 0 until clicksPerTime) {
                        if (player.hurtTime >= minClickExtraReduceHurtTime &&
                            player.hurtTime <= maxClickExtraReduceHurtTime) {
                            if (clickExtraReduce && clickExtraTime == "BeforeClick") {
                                if (player.onGround) {
                                    player.motionX *= clickExtraReduceFactorOnGround.toDouble()
                                    player.motionZ *= clickExtraReduceFactorOnGround.toDouble()
                                } else {
                                    player.motionX *= clickExtraReduceFactorInAir.toDouble()
                                    player.motionZ *= clickExtraReduceFactorInAir.toDouble()
                                }
                                if (clickExtraReduceDebug) {
                                    mc.thePlayer.addChatMessage(
                                        net.minecraft.util.ChatComponentText(
                                            "[ExtraReduce] BeforeClick-motionXZReduced | HurtTime: ${player.hurtTime}"
                                        )
                                    )
                                }
                            }
                        }

                        if (clickMode == "Packet") {
                            player.swingItem()
                            mc.playerController.attackEntity(player, target)
                        } else { // Simulation mode
                            mc.gameSettings.keyBindAttack.pressed = true
                            mc.gameSettings.keyBindAttack.pressed = false
                        }
                        if (sprintAfterReduce) {
                            player.isSprinting = true
                        }
                        isAttacking = true
                        if (clickExtraReduceDebug) {
                            mc.thePlayer.addChatMessage(
                                net.minecraft.util.ChatComponentText(
                                    "[NormalClick] Clicked | HurtTime: ${player.hurtTime}"
                                )
                            )
                        }

                        // 如果 ExtraClickWhenFirstClickReduce 开启，并且是首次点击减少，触发额外点击
                        if (extraClickWhenFirstClickReduce && !hasTriggeredExtraClick) {
                            for (i in 0 until extraCount) {
                                if (clickMode == "Packet") {
                                    player.swingItem()
                                    mc.playerController.attackEntity(player, target)
                                } else { // Simulation mode
                                    mc.gameSettings.keyBindAttack.pressed = true
                                    mc.gameSettings.keyBindAttack.pressed = false
                                }
                                if (sprintAfterReduce) {
                                    player.isSprinting = true
                                }
                                if (clickExtraReduceDebug) {
                                    mc.thePlayer.addChatMessage(
                                        net.minecraft.util.ChatComponentText(
                                            "[NormalClick] Extra Click | HurtTime: ${player.hurtTime} | Extra Click: $i"
                                        )
                                    )
                                }
                            }
                            hasTriggeredExtraClick = true
                        }

                        if (player.hurtTime >= minClickExtraReduceHurtTime &&
                            player.hurtTime <= maxClickExtraReduceHurtTime) {
                            if (clickExtraReduce && clickExtraTime == "AfterClick") {
                                if (player.onGround) {
                                    player.motionX *= clickExtraReduceFactorOnGround.toDouble()
                                    player.motionZ *= clickExtraReduceFactorOnGround.toDouble()
                                } else {
                                    player.motionX *= clickExtraReduceFactorInAir.toDouble()
                                    player.motionZ *= clickExtraReduceFactorInAir.toDouble()
                                }
                                if (clickExtraReduceDebug) {
                                    mc.thePlayer.addChatMessage(
                                        net.minecraft.util.ChatComponentText(
                                            "[ExtraReduce] AfterClick-motionXZReduced | HurtTime: ${player.hurtTime}"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SmartBlocking逻辑
        if (smartBlocking && globalMode == "Custom") {
            val distance = mol(dx, dz, 0.0)
            if (distance <= stbMinDistance && attackTicks > 0 && hurtTimeCounter > 7 && hurtTimeCounter < 9 && timerA > stbForwardTicks) {
                mc.gameSettings.keyBindUseItem.pressed = true
                hasHurt = 1
            }
            if (timerA > stbForwardTicks && hasHurt > 0) {
                allowTimerB = true
            }
            if (allowTimerB) {
                timerB++
            }
            if (timerB > stbTicks) {
                mc.gameSettings.keyBindUseItem.pressed = false
                timerA = 0
                hasHurt = 0
                timerB = 0
                allowTimerB = false
            }
            if (player.moveForward > 0f && hurtTimeCounter == 0 && player.onGround) {
                timerA++
            }
            if (player.moveForward <= 0f) {
                timerA = 0
            }
        }

        // KeepRange逻辑
        if (keepRange) {
            val killAuraState = killAuraModule?.state ?: false
            val distance = mol(dx, dz, 0.0)
            val targetSpeed = mol(ax, az, 0.0)

            if (killAuraState && distance > krMinDistance && distance <= krMaxDistance &&
                player.moveForward > 0f && !mc.gameSettings.keyBindBack.isKeyDown && generalSpeed > 0) {
                mc.gameSettings.keyBindBack.pressed = true
                allowS = true
                allowTimerC = true
                generalSpeed = 0
            }
            if (allowTimerC) {
                timerC++
            }
            if (allowS && timerC > krDuration && (player.moveForward <= 0f || !killAuraState)) {
                mc.gameSettings.keyBindBack.pressed = false
                allowS = false
                timerC = 0
                allowTimerC = false
            }
            if (allowS && (distance > (0.1 + range) || hurtTimeCounter > 0)) {
                mc.gameSettings.keyBindBack.pressed = false
                allowS = false
                timerC = 0
                allowTimerC = false
            }
            if (krAllowJump && player.moveForward > 0f && player.onGround && !mc.gameSettings.keyBindJump.isKeyDown &&
                targetSpeed > krAllowJumpLevel && distance > krMaxDistance &&
                distance <= 1 + krMaxDistance && tHurtTime > 0 && !allowJump) {
                player.jump()
                allowJump = true
            }
            if (allowJump && !player.onGround) {
                mc.gameSettings.keyBindJump.pressed = false
                allowJump = false
            }
        }

        if (attackTicks > 0) {
            if (attackTicks > 1) {
                checkTicks = 0
                attackTicks = 1
            } else {
                checkTicks++
                if (checkTicks > workTicks) {
                    checkTicks = 0
                    attackTicks = 0
                }
            }
        }
    }

    override val tag: String
        get() {
            val tags = mutableListOf<String>()

            if (tagMode == "Custom") {
                tags.add(customText)
            }

            if (authorName && tagState && tagMode == "Default" && globalMode == "Custom") {
                tags.add("BusyFixer/FireFly")
            }

            if (modifyMode && tagState && tagMode == "Default" && globalMode == "Custom") {
                tags.add("Modify H:${(modifyHorizonFactor * 100).toInt()}% V:${(modifyVerticalFactor * 100).toInt()}%")
            }

            if (keepRange && tagState && tagMode == "Default" && globalMode == "Custom") {
                tags.add("AutoWtap ${krDuration}ticks")
            }

            if (clickReduce && tagState && tagMode == "Default" && globalMode == "Custom") {
                // 计算点击速率，包括 ExtraCount 的数值
                val clickRate = if (extraClickWhenFirstClickReduce) {
                    (clicksPerTime * 2 * crTicks) + (extraCount * 2)
                } else {
                    clicksPerTime * 2 * crTicks
                }
                tags.add("ClickReduce ${clickRate}/s")
            }

            if (attackReduce && tagState && tagMode == "Default" && globalMode == "Custom") {
                tags.add("AttackReduce G:${attackFactorOnGround.toInt()}%/A:${attackFactorInAir.toInt()}%")
            }

            if (jumpReset && tagState && tagMode == "Default" && globalMode == "Custom") {
                tags.add("JumpReset ${jumpProbability}%")
            }

            if (matrixEnabled && tagState && tagMode == "Default" && globalMode == "Custom") {
                tags.add("MatrixReduce ${(matrixHReduce * 100).toInt()}%")
            }

            if (tagMode == "Default" && globalMode == "Intave") {
                tags.add("Intave")
            }

            return tags.joinToString(" | ")
        }
}
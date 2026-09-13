/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.potion.Potion

object Criticals : Module("Criticals", Category.COMBAT) {

    private val tagMode by choices("TagMode", arrayOf("ModuleMode", "Delay", "Custom"), "ModuleMode")
    private val customText by text("CustomTagText", "") { tagMode == "Custom" }

    val mode by choices(
        "Mode",
        arrayOf(
            "Packet",
            "NCPPacket",
            "AACPacket",
            "BlocksMC",
            "BlocksMC2",
            "BlocksMC3",
            "BlocksMC4",
            "BlocksMC5",
            "BlocksMC6",
            "BlocksMC7",
            "NoGround",
            "ForceNoGround",
            "Hop",
            "TPHop",
            "Jump",
            "LowJump",
            "CustomMotion",
            "Visual",
            "Grim",
            "LegitGrim",
            "NewPacket",
            "NewNCP",
            "Invalid",
            "NonCalculable",
            "MiniPhase",
            "DCJCombo",
            "LegitHVH",
            "MatrixSemi",
            "MatrixSmart",
            "SemiPos",
            "Mid",
            "Timer",
            "Edit"
        ),
        "Packet"
    )

    private val allowWorkTime by multiChoices(
        "AllowCriticalTime",
        arrayOf("OnGround", "InAir"),
        setOf("OnGround", "InAir")
    )

    val delay by int("Delay", 0, 0..500)
    private val hurtTime by int("HurtTime", 10, 0..10)
    private val useC06 by boolean("UseC06Packet", false) { mode == "MatrixSemi" }
    private val customMotionY by float("Custom-Y", 0.2f, 0.01f..0.42f) { mode == "CustomMotion" }
    private val autoJump by boolean("AutoJump", true) { mode == "Grim" }
    private val visual by boolean("Visual", true) { mode != "Visual" }
    private val debugger by boolean("Debugger", false)

    private val timerChange by float("Timer", 0.5f, 0.01f..2.0f) { mode == "Timer" }
    private val resetTimerUntil by int("ResetTimerUntilHitCount", 1, 0..10) { mode == "Timer" }
    private var hitCounter = 0
    private var changedTimer = false

    val msTimer = MSTimer()
    private var attacked = 0
    private var matrixAttacks = 0

    override fun onEnable() {
        matrixAttacks = 0
        attacked = 0
        hitCounter = 0
        changedTimer = false
        if (mode == "NoGround" || mode == "ForceNoGround")
            mc.thePlayer.tryJump()
    }

    override fun onDisable() {
        if (changedTimer) {
            mc.timer.timerSpeed = 1f
            changedTimer = false
        }
    }

    val onAttack = handler<AttackEvent> { event ->
        matrixAttacks++
        if (event.targetEntity is EntityLivingBase) {
            val thePlayer = mc.thePlayer ?: return@handler
            val entity = event.targetEntity

            if (thePlayer.isOnLadder || thePlayer.isInWeb || thePlayer.isInLiquid ||
                thePlayer.ridingEntity != null || entity.hurtTime > hurtTime ||
                Fly.handleEvents() || !msTimer.hasTimePassed(delay)
            )
                return@handler

            val (x, y, z) = thePlayer

            if ("InAir" !in allowWorkTime && "OnGround" !in allowWorkTime) return@handler
            if (mc.thePlayer.onGround && "InAir" in allowWorkTime && "OnGround" !in allowWorkTime) return@handler
            if (!mc.thePlayer.onGround && "OnGround" in allowWorkTime && "InAir" !in allowWorkTime) return@handler

            when (mode.lowercase()) {
                "edit" -> {
                    if (mc.thePlayer.onGround && !mc.thePlayer.isMoving) {
                        mc.thePlayer.onGround = false
                        PacketUtils.sendPacket(
                            C04PacketPlayerPosition(
                                x + mc.thePlayer.motionX,
                                y + mc.thePlayer.motionY,
                                z + mc.thePlayer.motionZ,
                                false
                            )
                        )
                    }
                }

                "timer" -> {
                    hitCounter++
                    if (mc.thePlayer.motionY < 0 && !mc.thePlayer.onGround && hitCounter < resetTimerUntil) {
                        if (!changedTimer) {
                            mc.timer.timerSpeed = timerChange
                            changedTimer = true
                            if (debugger) chat("Timer changed to $timerChange")
                        }
                    } else if (changedTimer) {
                        if (mc.thePlayer.onGround || hitCounter >= resetTimerUntil || mc.thePlayer.motionY >= 0) {
                            mc.timer.timerSpeed = 1f
                            changedTimer = false
                            if (debugger) chat("Timer reset to 1.0")
                            if (hitCounter >= resetTimerUntil) {
                                hitCounter = 0
                            }
                        }
                    }
                }

                "semipos" -> {
                    if (!thePlayer.isJumping) {
                        thePlayer.setPosition(x, y + 0.234, z)
                    }
                    if (entity.health < 0.3f) {
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 2.43192168e-14, z, true),
                            C04PacketPlayerPosition(x, y - 1.265e-256, z, false)
                        )
                    }
                }

                "mid" -> {
                    val hurtTimeVal = entity.hurtTime
                    val middle = hurtTimeVal == 0 || hurtTimeVal in 6..7
                    if (middle && !thePlayer.isJumping) {
                        thePlayer.setPosition(x, y + 0.234, z)
                    }
                    if (entity.health < 0.3f) {
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 2.43192168e-14, z, true),
                            C04PacketPlayerPosition(x, y - 1.265e-256, z, false)
                        )
                    }
                }

                "matrixsemi" -> {
                    if (matrixAttacks > 3) {
                        sendCriticalPacket(yOffset = 0.0825080378093, ground = false)
                        sendCriticalPacket(yOffset = 0.023243243674, ground = false)
                        sendCriticalPacket(yOffset = 0.0215634532004, ground = false)
                        sendCriticalPacket(yOffset = 0.00150000001304, ground = false)
                        matrixAttacks = 0
                        if (debugger) chat("Critical")
                    }
                }

                "matrixsmart" -> {
                    if (matrixAttacks > 3) {
                        sendCriticalPacket(yOffset = 0.110314, ground = false)
                        sendCriticalPacket(yOffset = 0.0200081, ground = false)
                        sendCriticalPacket(yOffset = 0.00000001300009, ground = false)
                        sendCriticalPacket(yOffset = 0.000000000022, ground = false)
                        sendCriticalPacket(ground = true)
                        matrixAttacks = 0
                        if (debugger) chat("Critical")
                    }
                }

                "packet" -> {
                    if (mc.thePlayer.onGround) {
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 0.0625, z, true),
                            C04PacketPlayerPosition(x, y, z, false)
                        )
                    }
                    if (visual) thePlayer.onCriticalHit(entity)
                    if (debugger) chat("Critical")
                }

                "ncppacket" -> {
                    if (mc.thePlayer.onGround) {
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 0.11, z, false),
                            C04PacketPlayerPosition(x, y + 0.1100013579, z, false),
                            C04PacketPlayerPosition(x, y + 0.0000013579, z, false)
                        )
                    }
                    if (visual) thePlayer.onCriticalHit(entity)
                    if (debugger) chat("Critical")
                }

                "aacpacket" -> {
                    sendCriticalPacket(yOffset = 0.05250000001304, ground = false)
                    sendCriticalPacket(yOffset = 0.00150000001304, ground = false)
                    sendCriticalPacket(yOffset = 0.01400000001304, ground = false)
                    sendCriticalPacket(yOffset = 0.00150000001304, ground = false)
                }

                "invalid" -> {
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 1E+27, z, false))
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y - 1E+68, z, false))
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 1E+41, z, false))
                    if (debugger) chat("Critical")
                    if (visual) thePlayer.onCriticalHit(entity)
                }

                "newpacket" -> {
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.05250000001304, z, true))
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.00150000001304, z, false))
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.01400000001304, z, false))
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.00150000001304, z, false))
                    if (debugger) chat("Critical")
                    if (visual) thePlayer.onCriticalHit(entity)
                }

                "newncp" -> {
                    attacked++
                    if (attacked >= 5) {
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.00001058293536, z, false))
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.00000916580235, z, false))
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.00000010371854, z, false))
                        attacked = 0
                        if (debugger) chat("Critical")
                        if (visual) thePlayer.onCriticalHit(entity)
                    }
                }

                "noncalculable" -> {
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 1E-5, z, false))
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 1E-7, z, false))
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y - 1E-6, z, false))
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y - 1E-4, z, false))
                    if (visual) thePlayer.onCriticalHit(entity)
                    if (debugger) chat("Critical")
                }

                "blocksmc" -> {
                    if (mc.thePlayer.onGround) {
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 0.001091981, z, true),
                            C04PacketPlayerPosition(x, y, z, false)
                        )
                    }
                    if (visual) thePlayer.onCriticalHit(entity)
                    if (debugger) chat("Critical")
                }

                "blocksmc2" -> {
                    if (mc.thePlayer.onGround) {
                        if (thePlayer.ticksExisted % 4 == 0) {
                            sendPackets(
                                C04PacketPlayerPosition(x, y + 0.0011, z, true),
                                C04PacketPlayerPosition(x, y, z, false)
                            )
                        }
                    }
                    if (visual) thePlayer.onCriticalHit(entity)
                    if (debugger) chat("Critical")
                }

                "blocksmc3" -> {
                    sendPacket1(0.0825080378093, true)
                    sendPacket1(0.0215634532004, false)
                    sendPacket1(0.1040220332227, false)
                    crit(entity)
                }

                "blocksmc4" -> {
                    sendPacket1(0.001, false)
                    sendPacket1(0.0010353, false)
                    sendPacket1(0.0, false)
                    crit(entity)
                }

                "blocksmc5" -> {
                    sendPacket1(0.001, false)
                    sendPacket1(0.0010153, false)
                    sendPacket1(0.0, false)
                    crit(entity)
                }

                "blocksmc6" -> {
                    sendPacket1(0.002, true)
                    sendPacket1(-0.000001, false)
                    sendPacket1(0.0, false)
                    crit(entity)
                }

                "blocksmc7" -> {
                    sendPacket1(0.0011, true)
                    sendPacket1(0.0, false)
                    crit(entity)
                }

                "miniphase" -> {
                    sendPackets(
                        C04PacketPlayerPosition(x, y + 0.00000001, z, false),
                        C04PacketPlayerPosition(x, y, z, false)
                    )
                    if (visual) thePlayer.onCriticalHit(entity)
                    if (debugger) chat("Critical")
                }

                "dcjcombo" -> {
                    if (mc.thePlayer.onGround) {
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 0.42, z, false),
                            C04PacketPlayerPosition(x, y + 0.42, z, false)
                        )
                    }
                    if (visual) thePlayer.onCriticalHit(entity)
                    if (debugger) chat("Critical")
                }

                "hop" -> {
                    if (mc.thePlayer.onGround) {
                        thePlayer.motionY = 0.1
                        thePlayer.fallDistance = 0.1f
                        thePlayer.onGround = false
                    }
                    if (visual) thePlayer.onCriticalHit(entity)
                    if (debugger) chat("Critical")
                }

                "tphop" -> {
                    if (mc.thePlayer.onGround) {
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 0.02, z, false),
                            C04PacketPlayerPosition(x, y + 0.01, z, false)
                        )
                        thePlayer.setPosition(x, y + 0.01, z)
                    }
                    if (visual) thePlayer.onCriticalHit(entity)
                    if (debugger) chat("Critical")
                }

                "jump" -> {
                    if (mc.thePlayer.onGround) {
                        thePlayer.motionY = 0.42
                    }
                    if (visual) thePlayer.onCriticalHit(entity)
                    if (debugger) chat("Critical")
                }

                "lowjump" -> {
                    if (mc.thePlayer.onGround) {
                        thePlayer.motionY = 0.3425
                    }
                    if (visual) thePlayer.onCriticalHit(entity)
                    if (debugger) chat("Critical")
                }

                "custommotion" -> {
                    if (mc.thePlayer.onGround) {
                        thePlayer.motionY = customMotionY.toDouble()
                    }
                    if (visual) thePlayer.onCriticalHit(entity)
                    if (debugger) chat("Critical")
                }

                "visual" -> {
                    thePlayer.onCriticalHit(entity)
                    if (debugger) chat("Critical")
                }

                "grim" -> {
                    if (!mc.thePlayer.onGround) {
                        val gx = mc.thePlayer.posX
                        val gy = mc.thePlayer.posY - 0.00001
                        val gz = mc.thePlayer.posZ
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(gx, gy, gz, false))
                        if (visual) thePlayer.onCriticalHit(entity)
                        if (debugger) chat("Critical")
                    } else if (autoJump && mc.thePlayer.onGround) {
                        mc.thePlayer.jump()
                        if (debugger) chat("Jump")
                    }
                }

                "legitgrim" -> {
                    if (mc.thePlayer.ticksExisted % 2 == 1) {
                        mc.thePlayer.motionY += 1.0E-7
                    }
                }

                "forcenoground" -> {
                    mc.thePlayer.onGround = false
                    if (mc.thePlayer.motionY > 0) {
                        mc.thePlayer.motionY *= -1.0E7
                    } else {
                        mc.thePlayer.motionY = -0.1
                    }
                }

                "legithvh" -> {
                    val offsets = doubleArrayOf(0.06253453, 0.02253453, 0.001253453, 1.135346E-4)
                    for (offset in offsets) {
                        mc.thePlayer.sendQueue.addToSendQueue(C04PacketPlayerPosition(x, y + offset, z, false))
                    }
                }
            }

            msTimer.reset()
        }
    }

    val onUpdate = handler<UpdateEvent> {
        if ("InAir" !in allowWorkTime && "OnGround" !in allowWorkTime) return@handler
        if (mc.thePlayer.onGround && "InAir" in allowWorkTime && "OnGround" !in allowWorkTime) return@handler
        if (!mc.thePlayer.onGround && "OnGround" in allowWorkTime && "InAir" !in allowWorkTime) return@handler
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is C03PacketPlayer && (mode == "NoGround" || mode == "ForceNoGround"))
            packet.onGround = false
    }

    private fun sendPacket(x: Double, y: Double, z: Double, ground: Boolean) {
        val (pX, pY, pZ) = mc.thePlayer
        PacketUtils.sendPacket(C04PacketPlayerPosition(pX + x, pY + y, pZ + z, ground))
    }

    private fun sendPacket1(y: Double, ground: Boolean) = sendPacket(0.0, y, 0.0, ground)

    private fun crit(entity: Entity) {
        mc.thePlayer.run {
            if (fallDistance > 0.0f
                && !onGround
                && !isOnLadder
                && !isInWater
                && !isPotionActive(Potion.blindness)
                && ridingEntity == null
                && entity is EntityLivingBase
            ) return

            onCriticalHit(entity)
        }
    }

    private fun sendCriticalPacket(
        xOffset: Double = 0.0,
        yOffset: Double = 0.0,
        zOffset: Double = 0.0,
        ground: Boolean
    ) {
        val px = mc.thePlayer.posX + xOffset
        val py = mc.thePlayer.posY + yOffset
        val pz = mc.thePlayer.posZ + zOffset
        if (useC06) {
            mc.netHandler.addToSendQueue(
                C03PacketPlayer.C06PacketPlayerPosLook(
                    px,
                    py,
                    pz,
                    mc.thePlayer.rotationYaw,
                    mc.thePlayer.rotationPitch,
                    ground
                )
            )
        } else {
            mc.netHandler.addToSendQueue(C04PacketPlayerPosition(px, py, pz, ground))
        }
    }

    override val tag
        get() = when (tagMode) {
            "ModuleMode" -> mode
            "Delay" -> "${delay}ms"
            else -> customText
        }
}
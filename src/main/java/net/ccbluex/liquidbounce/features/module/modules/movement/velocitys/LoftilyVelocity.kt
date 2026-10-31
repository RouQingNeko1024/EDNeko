package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys

import net.ccbluex.liquidbounce.config.choices
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.play.server.S12PacketEntityVelocity

object LoftilyVelocity : Module("LoftilyVelocity", Category.MOVEMENT) {

    private val velocityModes = arrayOf<LoftilyVelocityMode>(
        NormalVelocity(this),
        CancelVelocity(this),
        BoostVelocity(this),
        AdjustVelocity(this),
        LegitVelocity(this),
        StrafeVelocity(this),
        IntaveVelocity(this),
        PolarVelocity(this),
        PolarFlagVelocity(this),
        ReverseVelocity(this),
        ReverseVelocity2(this),
        BoostVelocity2(this),
        DelayJumpVelocity(this),
        GrimVerticalVelocity(this),
        MatrixVelocity(this),
        MotMatrixVelocity(this),
        MotMatrix2Velocity(this),
        AttackReduceVelocity(this),
        SmoothVelocity(this),
        RiseMatrixVelocity(this),
        WatchDogVelocity(this)
    )

    private val modeValue = choices(
        "Mode",
        velocityModes.map { it.modeName }.toTypedArray(),
        "Cancel"
    )

    val mode get() = modeValue.get()

    var receivedVelocity: S12PacketEntityVelocity? = null
    var receivedVelocityMotionX = .0
    var receivedVelocityMotionY = .0
    var receivedVelocityMotionZ = .0

    private var currentMode: LoftilyVelocityMode? = null
    var ticks = 0

    init {
        for (vm in velocityModes) {
            for (value in vm.values) {
                value.setSupport { prev -> prev && mode == vm.modeName }
            }
            addValues(vm.values)
        }

        handler<PacketEvent> { event ->
            val modeObj = velocityModes.firstOrNull { it.modeName == mode }
            if (modeObj != null && modeObj != currentMode) {
                currentMode?.onDisable()
                currentMode = modeObj
                currentMode?.onEnable()
            }
            currentMode?.onPacket(event)
        }

        handler<MotionEvent> { event ->
            if (event.eventState == EventState.PRE) {
                currentMode?.onMotion(event)
                receivedVelocity = null
            } else {
                ticks++
                currentMode?.onUpdate()
            }
        }

        handler<AttackEvent> { event -> currentMode?.onAttack(event) }
        handler<StrafeEvent> { event -> currentMode?.onStrafe(event) }
        handler<WorldEvent> { event -> currentMode?.onWorld(event) }
        handler<JumpEvent> { event -> currentMode?.onJump(event) }
    }

    override fun onEnable() {
        currentMode = velocityModes.firstOrNull { it.modeName == mode }
        currentMode?.onEnable()
    }

    override fun onDisable() {
        currentMode?.onDisable()
        currentMode = null
        receivedVelocity = null
    }
}
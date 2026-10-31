package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

/**
 * Loftily 框架 Fly 模块主类（移植自 loftily framework 的 flys 模块）。
 *
 * 统一收集所有 Fly 模式，通过 Mode 选项切换当前激活的模式，
 * 事件按规则分发到当前模式的对应回调，配置项仅在对应模式选中时显示。
 */
object LoftilyFly : Module("LoftilyFly", Category.MOVEMENT) {

    private val flyModes = arrayOf<LoftilyFlyMode>(
        VanillaFly(this),
        ClipFly(this),
        Matrix7158(this),
        HighViaFly(this),
        IntaveFly(this),
        TeleportFly(this),
        MatrixTestFly(this),
        MatrixBoostFly(this),
        MatrixFallFly(this),
        DCJZoomFly(this),
        PacketFly(this),
        AirPlaceFly(this),
        SpiterFly(this),
        TatakoFly(this),
        TimerGlideFly(this),
        PacketFly2(this)
    )

    private val modeValue = choices(
        "Mode",
        flyModes.map { it.modeName }.toTypedArray(),
        "Vanilla"
    )

    private val mode get() = modeValue.get()

    private var currentMode: LoftilyFlyMode? = null

    init {
        for (fm in flyModes) {
            for (value in fm.values) {
                value.setSupport { prev -> prev && mode == fm.modeName }
            }
            addValues(fm.values)
        }

        handler<PacketEvent> { event ->
            syncMode()
            when (event.eventType) {
                EventState.SEND -> currentMode?.onPacketSend(event)
                EventState.RECEIVE -> currentMode?.onPacketReceive(event)
                else -> {
                }
            }
        }

        handler<MotionEvent> { event ->
            syncMode()
            if (event.eventState == EventState.PRE) {
                currentMode?.onPreMotion(event)
                currentMode?.onMotion(event)
            } else {
                currentMode?.onUpdate()
            }
        }

        handler<MoveEvent> { event ->
            currentMode?.onMove(event)
        }

        handler<GameTickEvent> {
            currentMode?.onTick()
        }

        handler<WorldEvent> {
            currentMode?.onWorld()
        }

        handler<BlockBBEvent> {
            currentMode?.onBlockBBEvent(it)
        }

        handler<JumpEvent> {
            currentMode?.onJump(it)
        }

        handler<StepEvent> {
            currentMode?.onStep(it)
        }

        handler<MovementInputEvent> {
            currentMode?.onMovementInput(it)
        }
    }

    private fun syncMode() {
        val modeObj = flyModes.firstOrNull { it.modeName == mode }
        if (modeObj != null && modeObj != currentMode) {
            currentMode?.onDisable()
            currentMode = modeObj
            currentMode?.onEnable()
        }
    }

    override fun onEnable() {
        currentMode = flyModes.firstOrNull { it.modeName == mode }
        currentMode?.onEnable()
    }

    override fun onDisable() {
        currentMode?.onDisable()
        currentMode = null
    }

    /** 供模式内部调用：关闭父模块（对应原框架 (Mode)getParent().toggle()） */
    fun disableModule() {
        if (state) {
            toggle()
        }
    }
}
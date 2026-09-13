/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.event.*
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object FreezeUtil : Listenable, MinecraftInstance {
    // 冻结状态变量
    var frozen = false
        private set

    // 冻结位置数据
    private var frozenX = 0.0
    private var frozenY = 0.0
    private var frozenZ = 0.0

    // 冻结时的运动数据
    private var frozenMotionX = 0.0
    private var frozenMotionY = 0.0
    private var frozenMotionZ = 0.0

    // 是否允许转头发包
    private var keepRotationChange = true

    /**
     * 冻结玩家位置
     * @param allowRotation 是否允许转头发包
     */
    fun freeze(allowRotation: Boolean = true) {
        val player = mc.thePlayer ?: return

        if (frozen) return

        frozen = true
        keepRotationChange = allowRotation

        frozenX = player.posX
        frozenY = player.posY
        frozenZ = player.posZ
        frozenMotionX = player.motionX
        frozenMotionY = player.motionY
        frozenMotionZ = player.motionZ
    }

    fun unfreeze() {
        if (!frozen) return

        val player = mc.thePlayer ?: return

        // 恢复运动数据
        player.motionX = frozenMotionX
        player.motionY = frozenMotionY
        player.motionZ = frozenMotionZ
        player.setPositionAndRotation(
            frozenX,
            frozenY,
            frozenZ,
            player.rotationYaw,
            player.rotationPitch
        )

        frozen = false

        frozenX = 0.0
        frozenY = 0.0
        frozenZ = 0.0
        frozenMotionX = 0.0
        frozenMotionY = 0.0
        frozenMotionZ = 0.0
        keepRotationChange = true
    }
    val onUpdate = handler<UpdateEvent> {
        if (!frozen) return@handler

        val player = mc.thePlayer ?: return@handler

        // 冻结玩家位置
        player.motionX = 0.0
        player.motionY = 0.0
        player.motionZ = 0.0
        player.setPositionAndRotation(
            frozenX,
            frozenY,
            frozenZ,
            player.rotationYaw,
            player.rotationPitch
        )
    }
    val onMove = handler<MoveEvent> { e->
        if (frozen) e.cancelEvent()
    }

    val onPacket = handler<PacketEvent> { event ->
        if (!frozen) return@handler

        val packet = event.packet

        if (packet.isMovePacket) {
            if (!keepRotationChange) {
                event.cancelEvent()
            } else if (packet !is C03PacketPlayer.C05PacketPlayerLook) {
                event.cancelEvent()
            }
        }

        if (packet is S08PacketPlayerPosLook) {
            frozenX = packet.x
            frozenY = packet.y
            frozenZ = packet.z
            frozenMotionX = 0.0
            frozenMotionY = 0.0
            frozenMotionZ = 0.0
        }
    }

    /**
     * 获取当前冻结位置
     */
    fun getFrozenPosition(): Triple<Double, Double, Double> {
        return Triple(frozenX, frozenY, frozenZ)
    }

    /**
     * 更新冻结位置
     */
    fun updateFrozenPosition(x: Double, y: Double, z: Double) {
        frozenX = x
        frozenY = y
        frozenZ = z
    }

    /**
     * 重置所有数据
     */
    fun reset() {
        frozen = false
        frozenX = 0.0
        frozenY = 0.0
        frozenZ = 0.0
        frozenMotionX = 0.0
        frozenMotionY = 0.0
        frozenMotionZ = 0.0
        keepRotationChange = true
    }

    /**
     * 启用事件监听
     */
    override fun handleEvents(): Boolean {
        return true
    }
}
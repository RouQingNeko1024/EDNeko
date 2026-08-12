//skid firebounce tpaura
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.utils.attack.CooldownHelper
import net.ccbluex.liquidbounce.utils.attack.EntityUtils
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.ccbluex.liquidbounce.utils.pathfinder.CustomPathHelper
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.rotation.AimAssistRotationUtil
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*


object TPAura : Module("TPAura", Category.COMBAT) {
    private val tips by boolean("-----If you don't know how to configure it, the default settings are the best.-----", true)
    private val tips2 by boolean("----------Settings:----------", true)
    private val clickDelayValue by choices("AttackCalculation", arrayOf("APS", "1.9Simulation"), "APS")
    private val apsValue by int("AttackPerSecond", 20, 1..40){clickDelayValue=="APS"}
    private val maxTargetsValue by int("MaxTarget", 5, 1..50)
    private val rangeValue by int("SelectRange", 512, 10..512, "b")
    private val fovValue by float("Fov", 180F, 0F..180F, "°")
    private val pathType by choices("PathType", arrayOf("Smart", "Direct", "Immediate", "Flux"), "Smart")
    private val legit by boolean("ForceOnGroundPath", false) {pathType=="Smart"}
    private val moveDistance by float("MoveDistance", 9.0F, 0.1F..10.0F){pathType!="Flux"}
    private val sortBy by choices("SortBy", arrayOf("Health", "DistanceFarToClose", "DistanceCloseToFar"), "DistanceCloseToFar")
    private val sortDuringAttacking by boolean("SortAgainDuringAttack", true)
    private val swingValue by choices("SwingSide", arrayOf("Client", "Silent", "None"), "Client")
    private val swingOrderValue by choices("SwingOrder", arrayOf("Before", "After"), "Before")
    private val packetMode by choices("PacketMode", arrayOf("C04", "C06"), "C04")
    private val finalPosMode by choices("StopAt", arrayOf("RemoveUseless", "TpEntity", "Default"), "RemoveUseless")
    private val noLagMode by choices("NoLagMode", arrayOf("Smart", "Direct", "Immediate", "Flux", "None"), "None")
    private val noLagTryDelay by int("NoLag-TryDelay", 1000, 0..3000){noLagMode!="None"}
    private val loop by int("SearchBlocks", 1500, 500..32767){pathType=="Smart"}
    private val depth by int("SearchRange", 5, 1..32){pathType=="Smart"}
    private val positionPredict by boolean("PredictionEngine", true)
    private val antiFlyHack by boolean("AntiFlyHack(100%HitRate)", false)
    private val ignoreHurtTime by boolean("IgnoreTargetsInHurtDelay", true)
    private val rotationValue by boolean("Rotations", false)
    private val autoBlock by boolean("FullAutoBlock", true)
    private val critical by boolean("PacketCritical", true)
    private val noMovePacket by boolean("NoMovePacket", false)
    private val attackEventCall by boolean("CallAttackEvent", false)
    private val spoofGround by boolean("SpoofGround", true)
    private val ignoreUnreachableTarget by boolean("IgnoreUnreachableTargets", true)
    private val preCheck by boolean("Pre-check(UseMoreCPU?)", true){ignoreUnreachableTarget}

    private val showPathValue by boolean("ShowPath", true)
    private val pathColorValue by color("PathColor", Color(255, 0, 0, 150))
    private val debug by boolean("Debug", true)


    private val clickTimer = MSTimer()
    private val noLagTryTimer = MSTimer()
    var isBlocking = false
    private var thread: Thread? = null

    private var currentPathRendering: List<Vec3> = emptyList()
    private val _targetsCount = Collections.synchronizedList(arrayListOf<EntityLivingBase>())

    private val attackDelay: Long get() = 1000L / apsValue

    override val tag: String
        get() = "${_targetsCount.size}"

    fun removeUseless(positions: List<Vec3>, final: Vec3): List<Vec3>{
        val result = mutableListOf<Vec3>()
        for (pos in positions){
            result+=pos
            if(pos.distanceTo(final)<5.9)break
        }
        return result
    }
    fun debug(s: String) {
        Chat.print(EnumChatFormatting.RED.toString()+"[TPAura]"+EnumChatFormatting.GREEN.toString()+" -> "+ EnumChatFormatting.WHITE.toString()+s)
    }
    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        if((packet is C03PacketPlayer)&&noMovePacket){
            if(currentPathRendering.isNotEmpty()) {
                event.cancelEvent()
                if(debug) debug("Canceled a movement packet")
            }
        }
        if (packet is S08PacketPlayerPosLook){
            if(noLagMode=="None")return@handler
            if(!noLagTryTimer.hasTimePassed(noLagTryDelay)) return@handler
            noLagTryTimer.reset()
            event.cancelEvent()
            var pathToClientPosition = emptyList<Vec3>().toMutableList()
            sendPacket(C04PacketPlayerPosition(packet.x, packet.y, packet.z, spoofGround),false)
            when(noLagMode){
                "Smart"->{
                    pathToClientPosition = CustomPathHelper.findTeleportPathPointToPoint(packet.x, packet.y, packet.z, mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, moveDistance.toDouble(), loop, depth, legit)

                }
                "Direct"->{
                    pathToClientPosition = CustomPathHelper.findPathDirectly(packet.x, packet.y, packet.z, mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, moveDistance.toDouble())

                }
                "Immediate" -> {
                    pathToClientPosition = CustomPathHelper.findPathImmediately(packet.x, packet.y, packet.z, mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, moveDistance.toDouble())

                }
                "Flux" -> {
                    for (pos in CustomPathHelper.fluxGetPath(
                        Vec3(packet.x, packet.y, packet.z),
                        mc.thePlayer.positionVector
                    )) {
                        pathToClientPosition+=(Vec3(pos.x + 0.5, pos.y + 0.0, pos.z + 0.5))
                    }
                }
            }
            if(pathToClientPosition.isEmpty())return@handler
            if(debug) {
                if(pathToClientPosition.last().distanceTo(mc.thePlayer.positionVector) > 4) debug("No lag: Unreachable[P"+pathToClientPosition.size+"]")
                else debug("No lag: Synchronized client position[Sent "+pathToClientPosition.size+"x packets]")
            }
            for( vec in pathToClientPosition) sendPosition(vec)

        }
    }
    override fun onDisable() {
        if(debug) {
            if(tips||tips2) debug("Disabled, reset data.")
            else debug("Disabled?, reset data.")
        }
        if(thread!=null&&thread!!.isAlive){
            thread!!.interrupt()
            thread=null
        }
        isBlocking = false
        clickTimer.reset()
        _targetsCount.clear()
    }


    @Suppress("unused")
    val onRender = handler<Render3DEvent> {
        if (showPathValue && currentPathRendering.isNotEmpty()) {
            renderPath(currentPathRendering)
        }
    }

    private fun renderPath(path: List<Vec3>) {
        val color = pathColorValue
        mc.timer.renderPartialTicks

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        GL11.glLineWidth(2.0f)

        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        for (vec in path) {
            GL11.glVertex3d(
                vec.xCoord - mc.renderManager.viewerPosX,
                vec.yCoord - mc.renderManager.viewerPosY,
                vec.zCoord - mc.renderManager.viewerPosZ
            )
        }
        GL11.glEnd()

        GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f * 0.3f)

        for (vec in path) {
            val x = vec.xCoord - mc.renderManager.viewerPosX
            val y = vec.yCoord - mc.renderManager.viewerPosY
            val z = vec.zCoord - mc.renderManager.viewerPosZ
            val width = 0.3
            val height = 1.8
            val box = net.minecraft.util.AxisAlignedBB(
                x - width, y, z - width,
                x + width, y + height, z + width
            )
            RenderUtils.drawSelectionBoundingBox(box)
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
    }
    @Suppress("unused")
    val onWorldEvent = handler<WorldEvent> {
        state = false
        debug("Disabled cuz world change")
    }

    val onUpdate = handler<UpdateEvent> {
        if (isBlocking && mc.thePlayer.heldItem?.item is ItemSword) {
            sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
        }
        if (clickDelayValue=="APS"&&!clickTimer.hasTimePassed(attackDelay)) return@handler
        else if(clickDelayValue=="1.9Simulation"&& CooldownHelper.getAttackCooldownProgress()<1f) return@handler

        try {
            if (thread == null || !thread!!.isAlive) {
                thread = Thread { runAttack() }
                thread!!.start()
                clickTimer.reset()
            } else clickTimer.reset()
        } catch (_: Exception) {
        }
    }

    private fun calcPath(fromPos: Vec3, finalItPosition: Vec3): List<Vec3>{
        var path = CustomPathHelper.findPathDirectly(fromPos.xCoord, fromPos.yCoord, fromPos.zCoord,
            finalItPosition.xCoord, finalItPosition.yCoord, finalItPosition.zCoord,
            moveDistance.toDouble())
        when(pathType){
            "Immediate" -> {
                path = CustomPathHelper.findPathImmediately(fromPos.xCoord, fromPos.yCoord, fromPos.zCoord,
                    finalItPosition.xCoord, finalItPosition.yCoord, finalItPosition.zCoord,

                    moveDistance.toDouble())
            }
            "Smart" -> {
                path = CustomPathHelper.findTeleportPathPointToPoint(
                    fromPos.xCoord, fromPos.yCoord, fromPos.zCoord,
                    finalItPosition.xCoord, finalItPosition.yCoord, finalItPosition.zCoord,
                    moveDistance.toDouble(), loop, depth, legit)
            }
            "Flux" -> {
                val pathFlux1337 = CustomPathHelper.fluxGetPath(fromPos, finalItPosition)
                pathFlux1337.forEach {
                    path.add(Vec3(it.x+0.5, it.y + 0.0, it.z+0.5))
                }
            }
        }
        return path
    }
    private fun runAttack() {
        if (mc.thePlayer == null || mc.theWorld == null) return

        var targets = mutableListOf<EntityLivingBase>()

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityLivingBase && EntityUtils.isSelected(entity, true) && mc.thePlayer.getDistanceToEntity(
                    entity
                ) <= rangeValue
            ) {

                if (fovValue < 180F && RotationUtils.rotationDifference(entity) > fovValue)
                    continue

                if (autoBlock)
                    isBlocking = true
                if(ignoreHurtTime&& entity.hurtTime>1) continue

                targets.add(entity)
            }
        }
        if(ignoreUnreachableTarget&&preCheck)targets.removeIf {
            calcPath(mc.thePlayer.positionVector, it.positionVector).last().distanceTo(it.positionVector)>5.9F
        }
        when(sortBy){
            "Health" -> targets.sortBy { it.health }
            "DistanceCloseToFar" -> targets.sortBy { mc.thePlayer.getDistanceToEntity(it) }
            "DistanceFarToClose" -> targets.sortByDescending { mc.thePlayer.getDistanceToEntity(it) }
        }

        targets = targets.subList(0, maxTargetsValue.coerceAtMost(targets.size))


        _targetsCount.clear()
        _targetsCount.addAll(targets)

        if (targets.isEmpty()) {
            isBlocking = false
            currentPathRendering = emptyList()
            return
        }



        var fromPos = Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
        currentPathRendering = emptyList()
        var packetCount=0
        var unreachable=0
        val targetCountTotal = targets.size
        while(targets.isNotEmpty()){

            val it = targets.first()
            targets.removeAt(0)
            if (sortDuringAttacking){
                when(sortBy){
                    "Health" -> targets.sortBy { it.health }

                    "DistanceCloseToFar" -> targets.sortBy { mc.thePlayer.getDistanceToEntity(it) }
                    "DistanceFarToClose" -> targets.sortByDescending { mc.thePlayer.getDistanceToEntity(it) }
                }
            }
            val pt = (mc.thePlayer.getPing()/50)+1
            val finalItPosition = Vec3(it.posX + (if(positionPredict)(it.posX-it.lastTickPosX)*pt else 0.0),
                it.posY+(if(positionPredict)(it.posY-it.lastTickPosY)*pt else 0.0),
                it.posZ+(if(positionPredict)(it.posZ-it.lastTickPosZ)*pt else 0.0))
            var path = calcPath(fromPos, finalItPosition).toMutableList()
            if(path.isEmpty()) continue

            currentPathRendering+=path
            currentPathRendering.distinct()

            if (ignoreUnreachableTarget&&!preCheck){
                if(path.last().distanceTo(it.positionVector)>5.9F) {
                    unreachable++
                    continue
                }
            }
            when(finalPosMode){
                "RemoveUseless"-> path = removeUseless(path, it.positionVector).toMutableList()
                "TpEntity" -> path.add(it.positionVector)
            }
            val lastVec = path.last()

            for (vec in path) {
                sendPosition(vec)
                packetCount++
            }

            fromPos = lastVec

            if(lastVec.distanceTo(it.positionVector)>5.9F) unreachable++
            if(rotationValue){
                val rot = AimAssistRotationUtil.getRotations(lastVec, it.positionVector).withLimitedPitch(90F)
                sendPacket(C03PacketPlayer.C05PacketPlayerLook(rot.yaw, rot.pitch, true))
                packetCount++
            }
            if (critical){
                packetCount+=3
            }

            if(!antiFlyHack) applyAttack(it, lastVec)
            else{
                for( fx in -2..2){
                    for( fz in -2..2){
                        val pathAntiHack = calcPath(fromPos, Vec3(finalItPosition.xCoord+fx*12, finalItPosition.yCoord, finalItPosition.zCoord+fz*12)).toMutableList()
                        if(pathAntiHack.isNotEmpty()){
                            currentPathRendering+=pathAntiHack
                            pathAntiHack.forEach { sendPosition(it) }
                            fromPos = pathAntiHack.last()
                            packetCount+=pathAntiHack.size
                        }
                        applyAttack(it, fromPos)
                    }
                }
                currentPathRendering.distinct()
            }
        }
        var path123 = CustomPathHelper.findPathDirectly(fromPos.xCoord, fromPos.yCoord, fromPos.zCoord,
            mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
            moveDistance.toDouble())
        when(pathType){
            "Immediate" ->
                path123 = CustomPathHelper.findPathImmediately(fromPos.xCoord, fromPos.yCoord, fromPos.zCoord,
                    mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                    moveDistance.toDouble())
            "Smart" ->
                path123 = CustomPathHelper.findTeleportPathPointToPoint(
                    fromPos.xCoord, fromPos.yCoord, fromPos.zCoord,
                    mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                    moveDistance.toDouble(), loop, depth, legit)
            "Flux" -> {
                val pathFlux1337 = CustomPathHelper.fluxGetPath(fromPos, mc.thePlayer.positionVector)
                pathFlux1337.forEach {
                    path123.add(Vec3(it.x+0.5, it.y + 0.0, it.z+0.5))
                }
            }
        }

        for (vec in path123) {
            sendPosition(vec)
            packetCount++
        }
        if(debug) debug("Attack finished, sent $packetCount packets in total, attacked $targetCountTotal targets["+(if(ignoreUnreachableTarget) "Ignored " else "")+"$unreachable"+"x unreachable]")

    }
    fun applyAttack(e: Entity, lastVec: Vec3){
        if(critical) {
            sendPacket(C04PacketPlayerPosition(lastVec.xCoord,lastVec.yCoord+1E-14, lastVec.zCoord, false),false)
            sendPacket(C04PacketPlayerPosition(lastVec.xCoord,lastVec.yCoord+1E-15, lastVec.zCoord, false),false)
        }
        if (swingOrderValue=="After") {
            if(attackEventCall)EventManager.call(AttackEvent(e))

            mc.netHandler.addToSendQueue(C02PacketUseEntity(e, C02PacketUseEntity.Action.ATTACK))
        }
        when (swingValue) {
            "Client" -> mc.thePlayer.swingItem()
            "Silent" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
        }

        if (swingOrderValue=="Before") {
            if(attackEventCall)EventManager.call(AttackEvent(e))

            mc.netHandler.addToSendQueue(C02PacketUseEntity(e, C02PacketUseEntity.Action.ATTACK))
        }
        if(critical)
            sendPacket(C04PacketPlayerPosition(lastVec.xCoord,lastVec.yCoord, lastVec.zCoord, true),false)

    }
    fun sendPosition(pos: Vec3){
        if(packetMode=="C04") sendPacket(C04PacketPlayerPosition(pos.xCoord,pos.yCoord, pos.zCoord, spoofGround),false)
        else sendPacket(
            C03PacketPlayer.C06PacketPlayerPosLook(
                pos.xCoord,
                pos.yCoord,
                pos.zCoord,
                mc.thePlayer.rotationYaw,
                mc.thePlayer.rotationPitch,
                spoofGround
            ),false
        )
    }
}

/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.AxisAlignedBB
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object SmartBlink : Module("SmartBlink", Category.COMBAT) {

    private val startDistance by float("StartDistance", 5f, 1f..20f)
    private val stopDistance by float("StopDistance", 2.5f, 1f..20f)
    
    private val maxBlinkTime by float("MaxBlinkTime", 1f, 1f..30f)
    private val cooldownTime by float("CooldownTime", 3f, 1f..10f)
    private val markStopDistance by float("MarkStopDistance", 2f, 0.5f..10f)
    
    //这个pulse很傻逼，我就不用了
    //private val pulse by boolean("Pulse", false)
    //private val pulseDelay by int("PulseDelay", 1000, 100..5000) { pulse }
    
    private val pulseOnDamage by boolean("PulseOnDamage", true)
    private val pulseOnHit by boolean("PulseOnHit", true)
    
    private val markPosition by boolean("MarkPosition", true)
    private val markFilled by boolean("MarkFilled", true) { markPosition }
    private val markColor by color("MarkColor", Color(255, 0, 0, 100)) { markPosition }
    private val markOutlineColor by color("MarkOutlineColor", Color(255, 0, 0, 255)) { markPosition }
    
    private val showProgressBar by boolean("ShowProgressBar", true)
    private val progressBarX by float("ProgressBarX", 50f, 0f..100f) { showProgressBar }
    private val progressBarY by float("ProgressBarY", 80f, 0f..100f) { showProgressBar }
    private val progressBarWidth by float("ProgressBarWidth", 150f, 50f..300f) { showProgressBar }
    private val progressBarHeight by float("ProgressBarHeight", 10f, 5f..30f) { showProgressBar }
    private val progressBarBackground by color("ProgressBarBackground", Color(0, 0, 0, 150)) { showProgressBar }
    private val progressBarFill by color("ProgressBarFill", Color(0, 200, 255, 200)) { showProgressBar }
    private val progressBarBorder by color("ProgressBarBorder", Color(255, 255, 255, 200)) { showProgressBar }
    private val progressBarRounded by float("ProgressBarRounded", 3f, 0f..10f) { showProgressBar }
    private val progressBarText by boolean("ProgressBarText", true) { showProgressBar }
    private val progressBarTextColor by color("ProgressBarTextColor", Color(255, 255, 255)) { showProgressBar && progressBarText }
    
    private val chatNotify by boolean("ChatNotify", true)
    
    private val flagPause by boolean("FlagPause", true)
    private val flagPauseTime by int("FlagPauseTime", 5, 1..60) { flagPause }
    
    private var isBlinking = false
    private var startPosition: AxisAlignedBB? = null
    private var blinkStartTime = 0L
    
    private val pulseTimer = MSTimer()
    private val flagTimer = MSTimer()
    
    private var target: EntityLivingBase? = null
    private var isPausedByFlag = false
    private var isInCooldown = false
    private var cooldownStartTime = 0L
    private var lastHurtTime = 0

    override fun onDisable() {
        if (isBlinking) {
            BlinkUtils.unblink()
            isBlinking = false
            if (chatNotify) {
                chat("§7[§cSmartBlink§7] §fStopped blinking")
            }
        }
        startPosition = null
        target = null
        isPausedByFlag = false
        isInCooldown = false
        blinkStartTime = 0L
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        
        if (pulseOnDamage && isBlinking && player.hurtTime > 0 && lastHurtTime == 0) {
            BlinkUtils.unblink()
            isBlinking = false
            startPosition = null
            blinkStartTime = 0L
            isInCooldown = true
            cooldownStartTime = System.currentTimeMillis()
            if (chatNotify) {
                chat("§7[§cSmartBlink§7] §fPulse on damage")
            }
        }
        lastHurtTime = player.hurtTime
        
        if (isPausedByFlag) {
            if (!flagTimer.hasTimePassed(flagPauseTime * 1000L)) {
                return@handler
            }
            isPausedByFlag = false
            if (chatNotify) {
                chat("§7[§cSmartBlink§7] §fResumed after flag cooldown")
            }
        }
        
        if (isInCooldown) {
            if (System.currentTimeMillis() - cooldownStartTime < (cooldownTime * 1000L).toLong()) {
                return@handler
            }
            isInCooldown = false
            if (chatNotify) {
                chat("§7[§cSmartBlink§7] §aCooldown finished, ready to blink")
            }
        }
        
        target = mc.theWorld?.loadedEntityList
            ?.filterIsInstance<EntityPlayer>()
            ?.filter { it != player && !it.isDead }
            ?.minByOrNull { player.getDistanceToEntityBox(it) }
        
        val currentTarget = target
        
        if (currentTarget == null) {
            if (isBlinking) {
                BlinkUtils.unblink()
                isBlinking = false
                startPosition = null
                blinkStartTime = 0L
                if (chatNotify) {
                    chat("§7[§cSmartBlink§7] §fStopped blinking (no target)")
                }
            }
            return@handler
        }
        
        val distance = player.getDistanceToEntityBox(currentTarget)
        
        if (isBlinking) {
            val elapsedSeconds = (System.currentTimeMillis() - blinkStartTime) / 1000f
            
            if (elapsedSeconds >= maxBlinkTime) {
                BlinkUtils.unblink()
                isBlinking = false
                startPosition = null
                blinkStartTime = 0L
                isInCooldown = true
                cooldownStartTime = System.currentTimeMillis()
                if (chatNotify) {
                    chat("§7[§cSmartBlink§7] §fStopped blinking (max time reached: ${maxBlinkTime}s)")
                }
                return@handler
            }
            
            val box = startPosition
            if (box != null) {
                val targetPos = currentTarget.positionVector
                val boxCenterX = (box.minX + box.maxX) / 2
                val boxCenterY = (box.minY + box.maxY) / 2
                val boxCenterZ = (box.minZ + box.maxZ) / 2
                
                val dx = targetPos.xCoord - boxCenterX
                val dy = targetPos.yCoord - boxCenterY
                val dz = targetPos.zCoord - boxCenterZ
                val distanceToMark = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
                
                if (distanceToMark <= markStopDistance) {
                    BlinkUtils.unblink()
                    isBlinking = false
                    startPosition = null
                    blinkStartTime = 0L
                    isInCooldown = true
                    cooldownStartTime = System.currentTimeMillis()
                    if (chatNotify) {
                        chat("§7[§cSmartBlink§7] §fStopped blinking (enemy near mark: ${String.format("%.1f", distanceToMark)}m)")
                    }
                    return@handler
                }
            }
        }
        
        if (!isBlinking && distance <= startDistance && !isPausedByFlag && !isInCooldown) {
            isBlinking = true
            startPosition = AxisAlignedBB(
                player.posX - 0.3, player.posY, player.posZ - 0.3,
                player.posX + 0.3, player.posY + 1.8, player.posZ + 0.3
            )
            blinkStartTime = System.currentTimeMillis()
            pulseTimer.reset()
            if (chatNotify) {
                chat("§7[§cSmartBlink§7] §aStarted blinking")
            }
        }
        
        if (isBlinking && distance <= stopDistance) {
            BlinkUtils.unblink()
            isBlinking = false
            startPosition = null
            blinkStartTime = 0L
            isInCooldown = true
            cooldownStartTime = System.currentTimeMillis()
            if (chatNotify) {
                chat("§7[§cSmartBlink§7] §fStopped blinking (reached target)")
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (!isBlinking) return@handler
        
        val packet = event.packet
        val player = mc.thePlayer ?: return@handler
        
        if (player.isDead || player.ticksExisted <= 10) {
            BlinkUtils.unblink()
            isBlinking = false
            startPosition = null
            blinkStartTime = 0L
            isInCooldown = true
            cooldownStartTime = System.currentTimeMillis()
            if (chatNotify) {
                chat("§7[§cSmartBlink§7] §fStopped blinking (player dead/respawn)")
            }
            return@handler
        }
        
        BlinkUtils.blink(packet, event, sent = true, receive = false)
    }
    
    val onReceivePacket = handler<PacketEvent> { event ->
        if (!flagPause) return@handler
        
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook) {
            if (isBlinking) {
                BlinkUtils.unblink()
                isBlinking = false
                startPosition = null
                blinkStartTime = 0L
                isPausedByFlag = true
                flagTimer.reset()
                if (chatNotify) {
                    chat("§7[§cSmartBlink§7] §cFlag detected! Pausing for ${flagPauseTime}s")
                }
            }
        }
    }

    val onMotion = handler<MotionEvent> { event ->
        if (event.eventState == EventState.POST && isBlinking) {
            BlinkUtils.syncSent()
        }
    }

    val onRender3D = handler<Render3DEvent> {
        if (!markPosition || !isBlinking) return@handler
        
        val box = startPosition ?: return@handler
        val renderManager = mc.renderManager ?: return@handler
        val renderPosX = renderManager.viewerPosX
        val renderPosY = renderManager.viewerPosY
        val renderPosZ = renderManager.viewerPosZ
        
        glPushMatrix()
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glEnable(GL_BLEND)
        glDisable(GL_DEPTH_TEST)
        try {
            mc.entityRenderer?.disableLightmap()
        } catch (e: Exception) {
        }
        
        if (markFilled) {
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glColor4f(markColor.red / 255f, markColor.green / 255f, markColor.blue / 255f, markColor.alpha / 255f)
            glBegin(GL_QUADS)
            glVertex3d(box.minX - renderPosX, box.minY - renderPosY, box.minZ - renderPosZ)
            glVertex3d(box.maxX - renderPosX, box.minY - renderPosY, box.minZ - renderPosZ)
            glVertex3d(box.maxX - renderPosX, box.minY - renderPosY, box.maxZ - renderPosZ)
            glVertex3d(box.minX - renderPosX, box.minY - renderPosY, box.maxZ - renderPosZ)
            
            glVertex3d(box.minX - renderPosX, box.maxY - renderPosY, box.minZ - renderPosZ)
            glVertex3d(box.maxX - renderPosX, box.maxY - renderPosY, box.minZ - renderPosZ)
            glVertex3d(box.maxX - renderPosX, box.maxY - renderPosY, box.maxZ - renderPosZ)
            glVertex3d(box.minX - renderPosX, box.maxY - renderPosY, box.maxZ - renderPosZ)
            
            glVertex3d(box.minX - renderPosX, box.minY - renderPosY, box.minZ - renderPosZ)
            glVertex3d(box.minX - renderPosX, box.maxY - renderPosY, box.minZ - renderPosZ)
            glVertex3d(box.maxX - renderPosX, box.maxY - renderPosY, box.minZ - renderPosZ)
            glVertex3d(box.maxX - renderPosX, box.minY - renderPosY, box.minZ - renderPosZ)
            
            glVertex3d(box.minX - renderPosX, box.minY - renderPosY, box.maxZ - renderPosZ)
            glVertex3d(box.minX - renderPosX, box.maxY - renderPosY, box.maxZ - renderPosZ)
            glVertex3d(box.maxX - renderPosX, box.maxY - renderPosY, box.maxZ - renderPosZ)
            glVertex3d(box.maxX - renderPosX, box.minY - renderPosY, box.maxZ - renderPosZ)
            
            glVertex3d(box.minX - renderPosX, box.minY - renderPosY, box.minZ - renderPosZ)
            glVertex3d(box.minX - renderPosX, box.minY - renderPosY, box.maxZ - renderPosZ)
            glVertex3d(box.minX - renderPosX, box.maxY - renderPosY, box.maxZ - renderPosZ)
            glVertex3d(box.minX - renderPosX, box.maxY - renderPosY, box.minZ - renderPosZ)
            
            glVertex3d(box.maxX - renderPosX, box.minY - renderPosY, box.minZ - renderPosZ)
            glVertex3d(box.maxX - renderPosX, box.minY - renderPosY, box.maxZ - renderPosZ)
            glVertex3d(box.maxX - renderPosX, box.maxY - renderPosY, box.maxZ - renderPosZ)
            glVertex3d(box.maxX - renderPosX, box.maxY - renderPosY, box.minZ - renderPosZ)
            glEnd()
        }
        
        glColor4f(markOutlineColor.red / 255f, markOutlineColor.green / 255f, markOutlineColor.blue / 255f, markOutlineColor.alpha / 255f)
        glLineWidth(2f)
        glBegin(GL_LINE_STRIP)
        
        glVertex3d(box.minX - renderPosX, box.minY - renderPosY, box.minZ - renderPosZ)
        glVertex3d(box.maxX - renderPosX, box.minY - renderPosY, box.minZ - renderPosZ)
        glVertex3d(box.maxX - renderPosX, box.minY - renderPosY, box.maxZ - renderPosZ)
        glVertex3d(box.minX - renderPosX, box.minY - renderPosY, box.maxZ - renderPosZ)
        glVertex3d(box.minX - renderPosX, box.minY - renderPosY, box.minZ - renderPosZ)
        
        glVertex3d(box.minX - renderPosX, box.maxY - renderPosY, box.minZ - renderPosZ)
        glVertex3d(box.maxX - renderPosX, box.maxY - renderPosY, box.minZ - renderPosZ)
        glVertex3d(box.maxX - renderPosX, box.maxY - renderPosY, box.maxZ - renderPosZ)
        glVertex3d(box.minX - renderPosX, box.maxY - renderPosY, box.maxZ - renderPosZ)
        glVertex3d(box.minX - renderPosX, box.maxY - renderPosY, box.minZ - renderPosZ)
        
        glEnd()
        
        glBegin(GL_LINES)
        glVertex3d(box.minX - renderPosX, box.minY - renderPosY, box.minZ - renderPosZ)
        glVertex3d(box.minX - renderPosX, box.maxY - renderPosY, box.minZ - renderPosZ)
        glVertex3d(box.maxX - renderPosX, box.minY - renderPosY, box.minZ - renderPosZ)
        glVertex3d(box.maxX - renderPosX, box.maxY - renderPosY, box.minZ - renderPosZ)
        glVertex3d(box.maxX - renderPosX, box.minY - renderPosY, box.maxZ - renderPosZ)
        glVertex3d(box.maxX - renderPosX, box.maxY - renderPosY, box.maxZ - renderPosZ)
        glVertex3d(box.minX - renderPosX, box.minY - renderPosY, box.maxZ - renderPosZ)
        glVertex3d(box.minX - renderPosX, box.maxY - renderPosY, box.maxZ - renderPosZ)
        glEnd()
        
        glEnable(GL_DEPTH_TEST)
        glDisable(GL_LINE_SMOOTH)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glPopMatrix()
    }
    
    val onRender2D = handler<Render2DEvent> {
        if (!showProgressBar || !isBlinking) return@handler
        
        val sr = net.minecraft.client.gui.ScaledResolution(mc)
        val screenWidth = sr.scaledWidth.toFloat()
        val screenHeight = sr.scaledHeight.toFloat()
        
        val x = screenWidth * (progressBarX / 100f) - progressBarWidth / 2
        val y = screenHeight * (progressBarY / 100f)
        
        val elapsedSeconds = (System.currentTimeMillis() - blinkStartTime) / 1000f
        val progress = (elapsedSeconds / maxBlinkTime).coerceIn(0f, 1f)
        
        net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect(
            x, y, x + progressBarWidth, y + progressBarHeight,
            progressBarBackground.rgb, progressBarRounded
        )
        
        if (progress > 0) {
            val fillWidth = progressBarWidth * progress
            net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect(
                x, y, x + fillWidth, y + progressBarHeight,
                progressBarFill.rgb, progressBarRounded
            )
        }
        
        net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorder(
            x, y, x + progressBarWidth, y + progressBarHeight,
            1f, progressBarBorder.rgb, progressBarRounded
        )
        
        if (progressBarText) {
            val text = String.format("%.1fs / %.1fs", elapsedSeconds, maxBlinkTime)
            val font = Fonts.fontSemibold35
            val textWidth = font.getStringWidth(text)
            font.drawString(
                text,
                x + (progressBarWidth - textWidth) / 2,
                y + (progressBarHeight - font.FONT_HEIGHT) / 2 + 1,
                progressBarTextColor.rgb,
                true
            )
        }
    }

    val onAttack = handler<AttackEvent> {
        if (!pulseOnHit || !isBlinking) return@handler
        
        BlinkUtils.unblink()
        isBlinking = false
        startPosition = null
        blinkStartTime = 0L
        isInCooldown = true
        cooldownStartTime = System.currentTimeMillis()
        if (chatNotify) {
            chat("§7[§cSmartBlink§7] §fPulse on hit")
        }
    }

    override val tag: String
        get() = when {
            isPausedByFlag -> "Paused(${flagPauseTime}s)"
            isInCooldown -> {
                val remaining = (cooldownTime * 1000L - (System.currentTimeMillis() - cooldownStartTime)) / 1000f
                "Cooldown(${String.format("%.1f", remaining.coerceAtLeast(0f))}s)"
            }
            isBlinking -> {
                val elapsed = (System.currentTimeMillis() - blinkStartTime) / 1000f
                String.format("%.1fs", elapsed)
            }
            else -> "Waiting"
        }
}

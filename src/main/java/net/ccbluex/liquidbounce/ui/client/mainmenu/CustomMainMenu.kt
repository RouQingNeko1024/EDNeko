/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.mainmenu

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration
import net.ccbluex.liquidbounce.ui.client.*
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.fontmanager.GuiFontManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.animation.Animation
import net.ccbluex.liquidbounce.utils.render.animation.AnimationType
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import net.ccbluex.liquidbounce.utils.ui.AbstractScreen
import net.ccbluex.liquidbounce.utils.render.shader.Background
import net.ccbluex.liquidbounce.utils.render.shader.BuiltinShaderBackground
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.GuiOptions
import net.minecraft.client.gui.GuiSelectWorld
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Mouse
import java.awt.Color

class CustomMainMenu : AbstractScreen() {
    private var particleEngine: ParticleEngine? = null
    
    private val buttons = mutableListOf<MenuButton>()
    private val textButtons = mutableListOf<TextButton>()
    
    private var firstInit = false
    private var selectionAnimY = 0f
    private var i1 = 0
    private var cur = ""
    private val timer = net.ccbluex.liquidbounce.utils.timing.MSTimer()
    private val animation = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
    
    init {
        // Initialize menu buttons
        buttons.add(MenuButton("Singleplayer"))
        buttons.add(MenuButton("Multiplayer"))
        buttons.add(MenuButton("Alt Manager"))
        buttons.add(MenuButton("Settings"))
        buttons.add(MenuButton("Exit"))
        
        // Initialize text buttons
     //   textButtons.add(TextButton("Scripting"))
      //  textButtons.add(TextButton("Discord"))
    }
    
    override fun initGui() {
        if (!firstInit) {
            firstInit = true
        }
        
        if (particleEngine == null) particleEngine = ParticleEngine()
        
        buttons.forEach { it.initGui() }
        timer.reset()
    }
    
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val sr = ScaledResolution(mc)
        width = sr.scaledWidth
        height = sr.scaledHeight
        
        drawDefaultBackground()
        
        RenderUtils.drawImage(
            ResourceLocation("edneko/MCDOG.png"),
            0, 0, width, height
        )
        
        // Render particles
        particleEngine?.render()
        
        // Draw logo
      //  RenderUtils.drawImage(ResourceLocation("edneko/logo1.png"), width / 2f - 100 / 2f, height / 2f - 110, 100, 37)
        
        // Draw main panel
        val x = (width - 269 / 2) / 2
        val y = (height - 200 / 2) / 2
        val width1 = 269 / 2
        val height1 = 244 / 2
        RenderUtils.drawImage(ResourceLocation("edneko/guis/mainmenu/selectBG.png"), x.toInt(), y.toInt(), width1.toInt(), height1.toInt(), Color(250, 250, 250))
        
        // Draw buttons with selection indicator
        val height2 = 92 / 2
        val strs = arrayOf("Single Player", "Multi Player", "Settings", "Alts Manager")
        
        RenderUtils.drawImage(ResourceLocation("edneko/guis/mainmenu/selection.png"), (x - 10).toInt(), selectionAnimY.toInt(), (309 / 2f).toInt(), height2.toInt())
        
        if (selectionAnimY == 0f) {
            selectionAnimY = y - 10f
            i1 = (y - 10f).toInt()
        }
        selectionAnimY = animation.base(i1.toDouble(), selectionAnimY.toDouble(), 0.4).toFloat()
        
        var buttonY = y.toInt()
        for (i in 0 until 4) {
            val str = strs[i]
            val font = Fonts.fontSemibold35
            val hoveringAppend = isHoveringAppend(mouseX, mouseY, x.toInt(), buttonY, width1.toInt(), 32)
            
            if (hoveringAppend || cur == str) {
                i1 = buttonY - 10
                cur = str
                RenderUtils.drawImage(
                    ResourceLocation("edneko/icons/mainmenu/${str.lowercase().replace(" ", "")}.png"),
                    (x + 30).toInt(),
                    (buttonY + 8),
                    13,
                    13,
                    Color(90, 90, 90)
                )
                font.drawString(str, (x + 50).toInt(), (buttonY + 10), Color(90, 90, 90).rgb)
                
                if (hoveringAppend && Mouse.isButtonDown(0) && timer.hasTimePassed(200)) {
                    when (i) {
                        0 -> mc.displayGuiScreen(GuiSelectWorld(this))
                        1 -> mc.displayGuiScreen(GuiMultiplayer(this))
                        2 -> mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
                        3 -> mc.displayGuiScreen(GuiAltManager(this))
                    }
                    timer.reset()
                }
            } else {
                font.drawString(str, (x + 50).toInt(), (buttonY + 10), Color(197, 197, 197).rgb)
                RenderUtils.drawImage(
                    ResourceLocation("edneko/icons/mainmenu/${str.lowercase().replace(" ", "")}.png"),
                    (x + 30).toInt(),
                    (buttonY + 8),
                    13,
                    13,
                    Color(197, 197, 197)
                )
            }
            buttonY += 32
        }
        
        // Draw text buttons
        val buttonsWidth = textButtons.sumOf { it.getWidth().toDouble() }.toFloat() + 
                          Fonts.fontSemibold35.getStringWidth(" | ") * (textButtons.size - 1)
        
        var buttonCount = 0f
        var buttonIncrement = 0
        for (button in textButtons) {
            button.x = width / 2f - buttonsWidth / 2f + buttonCount
            button.y = height / 2f + 120
            
            button.addToEnd = buttonIncrement != (textButtons.size - 1)
            button.drawScreen(mouseX, mouseY)
            
            buttonCount += button.getWidth() + Fonts.fontSemibold35.getStringWidth(" | ")
            buttonIncrement++
        }
        
        val bgBtnX = 5
        val bgBtnY = height - 50
        val bgBtnWidth = 80
        val bgBtnHeight = 20
        val isHoveringBg = mouseX >= bgBtnX && mouseX <= bgBtnX + bgBtnWidth && 
                           mouseY >= bgBtnY && mouseY <= bgBtnY + bgBtnHeight
        val bgBtnColor = if (isHoveringBg) Color(60, 60, 60, 200) else Color(40, 40, 40, 200)
        val bgTextColor = if (isHoveringBg) Color(200, 200, 200) else Color(150, 150, 150)
        
        RenderUtils.drawRoundedRectInt(bgBtnX, bgBtnY, bgBtnX + bgBtnWidth, bgBtnY + bgBtnHeight, bgBtnColor.rgb, 3f)
        
        val currentBgName = Background.BUILTIN_BACKGROUND_NAMES[ClientConfiguration.customMenuBackgroundIndex] ?: "Aurora"
        Fonts.fontSemibold35.drawCenteredString(
            currentBgName, 
            bgBtnX + bgBtnWidth / 2f, 
            bgBtnY + (bgBtnHeight - Fonts.fontSemibold35.FONT_HEIGHT) / 2f,
            bgTextColor.rgb
        )

        val switchBtnX = 5
        val switchBtnY = height - 25
        val switchBtnWidth = 80
        val switchBtnHeight = 20
        val isHoveringSwitch = mouseX >= switchBtnX && mouseX <= switchBtnX + switchBtnWidth && 
                               mouseY >= switchBtnY && mouseY <= switchBtnY + switchBtnHeight
        val switchBtnColor = if (isHoveringSwitch) Color(60, 60, 60, 200) else Color(40, 40, 40, 200)
        val switchTextColor = if (isHoveringSwitch) Color(200, 200, 200) else Color(150, 150, 150)
        
        RenderUtils.drawRoundedRectInt(switchBtnX, switchBtnY, switchBtnX + switchBtnWidth, switchBtnY + switchBtnHeight, switchBtnColor.rgb, 3f)
        Fonts.fontSemibold35.drawCenteredString(
            "Switch Style", 
            switchBtnX + switchBtnWidth / 2f, 
            switchBtnY + (switchBtnHeight - Fonts.fontSemibold35.FONT_HEIGHT) / 2f,
            switchTextColor.rgb
        )
        
        super.drawScreen(mouseX, mouseY, partialTicks)
    }
    
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        buttons.forEach { it.mouseClicked(mouseX, mouseY, mouseButton) }
        textButtons.forEach { it.mouseClicked(mouseX, mouseY, mouseButton) }
        
        val bgBtnX = 5
        val bgBtnY = height - 50
        val bgBtnWidth = 80
        val bgBtnHeight = 20
        val isHoveringBg = mouseX >= bgBtnX && mouseX <= bgBtnX + bgBtnWidth && 
                           mouseY >= bgBtnY && mouseY <= bgBtnY + bgBtnHeight
        
        if (isHoveringBg && mouseButton == 0 && timer.hasTimePassed(200)) {
            val currentIndex = ClientConfiguration.customMenuBackgroundIndex
            val newIndex = (currentIndex + 1) % Background.BUILTIN_BACKGROUNDS.size
            ClientConfiguration.customMenuBackgroundIndex = newIndex
            LiquidBounce.customMenuBackground = Background.fromBuiltin(newIndex)
            FileManager.saveConfig(valuesConfig)
            timer.reset()
            return
        }

        val switchBtnX = 5
        val switchBtnY = height - 25
        val switchBtnWidth = 80
        val switchBtnHeight = 20
        val isHoveringSwitch = mouseX >= switchBtnX && mouseX <= switchBtnX + switchBtnWidth && 
                               mouseY >= switchBtnY && mouseY <= switchBtnY + switchBtnHeight
        
        if (isHoveringSwitch && mouseButton == 0 && timer.hasTimePassed(200)) {
            ClientConfiguration.mainMenuStyle = "Default"
            FileManager.saveConfig(valuesConfig)
            mc.displayGuiScreen(GuiMainMenu())
            timer.reset()
            return
        }
        
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }
    
    inner class MenuButton(val text: String) : MinecraftInstance {
        var x = 0f
        var y = 0f
        var width = 0f
        var height = 0f
        var clickAction: () -> Unit = {}
        private val hoverAnimation = Animation()
        private var isHovered = false
        
        fun initGui() {
            // Initialization code if needed
        }
        
        fun drawScreen(mouseX: Int, mouseY: Int) {
            isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
            hoverAnimation.start(0.0, if (isHovered) 1.0 else 0.0, 0.15f, AnimationType.LINEAR)
            hoverAnimation.update()
            
            val animationOutput = hoverAnimation.value
            val buttonColor = if (isHovered) Color(60, 60, 60, 200) else Color(40, 40, 40, 200)
            val textColor = Color(255, 255, 255, (255 * animationOutput).toInt())
            
            RenderUtils.drawRoundedRectInt(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), buttonColor.rgb, 3f)
            Fonts.fontSemibold35.drawCenteredString(text, x + width / 2f, y + height / 2f - Fonts.fontSemibold35.FONT_HEIGHT / 2f, textColor.rgb)
        }
        
        fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
            if (mouseButton == 0 && isHovered) {
                clickAction()
            }
        }
    }
    
    inner class TextButton(val text: String) : MinecraftInstance {
        var x = 0f
        var y = 0f
        var clickAction: () -> Unit = {}
        var addToEnd = false
        private val hoverAnimation = Animation()
        private var isHovered = false
        
        fun getWidth(): Int {
            return Fonts.fontSemibold35.getStringWidth(text).toInt()
        }
        
        fun drawScreen(mouseX: Int, mouseY: Int) {
            isHovered = mouseX >= x && mouseX <= x + getWidth() && mouseY >= y - Fonts.fontSemibold35.FONT_HEIGHT / 2 && mouseY <= y + Fonts.fontSemibold35.FONT_HEIGHT / 2
            hoverAnimation.start(0.0, if (isHovered) 1.0 else 0.0, 0.15f, AnimationType.LINEAR)
            hoverAnimation.update()
            
            val animationOutput = hoverAnimation.value
            val textColor = if (isHovered) Color(200, 200, 200, (255 * animationOutput).toInt()) else Color.WHITE
            
            Fonts.fontSemibold35.drawString(text, x, y - (Fonts.fontSemibold35.FONT_HEIGHT / 2f * animationOutput.toFloat()), textColor.rgb)
            
            if (addToEnd) {
                Fonts.fontSemibold35.drawString(" | ", x + getWidth(), y, Color.WHITE.rgb)
            }
        }
        
        fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
            if (mouseButton == 0 && isHovered) {
                clickAction()
            }
        }
    }

    /**
     * Check if mouse is hovering over a region
     */
    private fun isHoveringAppend(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int): Boolean {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
    }
}
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.features.special.ClientFixes
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockFML
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockPayloadPackets
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockProxyPacket
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockResourcePackExploit
import net.ccbluex.liquidbounce.features.special.ClientFixes.clientBrand
import net.ccbluex.liquidbounce.features.special.ClientFixes.fmlFixesEnabled
import net.ccbluex.liquidbounce.features.special.ClientFixes.eaglercraftEnabled
import net.ccbluex.liquidbounce.features.special.ClientFixes.eaglercraftUrl
import net.ccbluex.liquidbounce.features.special.ClientFixes.eaglercraftDebug
import net.ccbluex.liquidbounce.features.special.eagler.EaglercraftManager
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ui.AbstractScreen
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import java.io.IOException
import java.util.*

class GuiClientFixes(private val prevGui: GuiScreen) : AbstractScreen() {

    private lateinit var enabledButton: GuiButton
    private lateinit var fmlButton: GuiButton
    private lateinit var proxyButton: GuiButton
    private lateinit var payloadButton: GuiButton
    private lateinit var customBrandButton: GuiButton
    private lateinit var resourcePackButton: GuiButton
    private lateinit var eaglercraftButton: GuiButton
    private lateinit var eaglercraftDebugButton: GuiButton
    private lateinit var eaglercraftConnectButton: GuiButton
    private lateinit var eaglercraftAutoConnectButton: GuiButton
    private lateinit var eaglercraftUrlField: GuiTextField

    override fun initGui() {
        val startX = width / 2 - 100
        var yPos = height / 4 + 10

        enabledButton = +GuiButton(1, startX, yPos, "AntiForge (" + (if (fmlFixesEnabled) "On" else "Off") + ")")
        yPos += 22
        fmlButton = +GuiButton(2, startX, yPos, "Block FML (" + (if (blockFML) "On" else "Off") + ")")
        yPos += 22
        proxyButton = +GuiButton(3, startX, yPos, "Block FML Proxy (" + (if (blockProxyPacket) "On" else "Off") + ")")
        yPos += 22
        payloadButton = +GuiButton(4, startX, yPos, "Block Non-MC Payloads (" + (if (blockPayloadPackets) "On" else "Off") + ")")
        yPos += 22
        customBrandButton = +GuiButton(5, startX, yPos, "Brand ($clientBrand)")
        yPos += 22
        resourcePackButton = +GuiButton(6, startX, yPos, "Block ResPack Exploit (" + (if (blockResourcePackExploit) "On" else "Off") + ")")
        yPos += 28

        Fonts.fontSemibold40.drawCenteredString("Eaglercraft Protocol", width / 2f, yPos.toFloat(), 0xFFAA00, true)
        yPos += 18

        eaglercraftButton = +GuiButton(10, startX, yPos, "Eaglercraft (" + (if (eaglercraftEnabled) "On" else "Off") + ")")
        yPos += 22
        eaglercraftDebugButton = +GuiButton(11, startX, yPos, "Debug (" + (if (eaglercraftDebug) "On" else "Off") + ")")
        yPos += 22

        eaglercraftUrlField = GuiTextField(12, mc.fontRendererObj, startX, yPos, 170, 20)
        eaglercraftUrlField.text = eaglercraftUrl
        eaglercraftUrlField.maxStringLength = 256
        eaglercraftUrlField.setCanLoseFocus(true)
        eaglercraftUrlField.isFocused = false
        yPos += 24

        eaglercraftConnectButton = +GuiButton(13, startX, yPos, 84, 20, getConnectButtonText())
        eaglercraftAutoConnectButton = +GuiButton(14, startX + 86, yPos, 84, 20, "Auto Join")
        yPos += 28

        +GuiButton(0, startX, yPos, "Back")
    }

    private fun getConnectButtonText(): String {
        return if (EaglercraftManager.isProxyRunning()) "Stop Proxy" else "Start Proxy"
    }

    public override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            1 -> {
                fmlFixesEnabled = !fmlFixesEnabled
                enabledButton.displayString = "AntiForge (${if (fmlFixesEnabled) "On" else "Off"})"
            }

            2 -> {
                blockFML = !blockFML
                fmlButton.displayString = "Block FML (${if (blockFML) "On" else "Off"})"
            }

            3 -> {
                blockProxyPacket = !blockProxyPacket
                proxyButton.displayString = "Block FML Proxy (${if (blockProxyPacket) "On" else "Off"})"
            }

            4 -> {
                blockPayloadPackets = !blockPayloadPackets
                payloadButton.displayString = "Block Non-MC Payloads (${if (blockPayloadPackets) "On" else "Off"})"
            }

            5 -> {
                val brands = listOf(*ClientFixes.possibleBrands)
                clientBrand = brands[(brands.indexOf(clientBrand) + 1) % brands.size]
                customBrandButton.displayString = "Brand ($clientBrand)"
            }

            6 -> {
                blockResourcePackExploit = !blockResourcePackExploit
                resourcePackButton.displayString = "Block ResPack Exploit (${if (blockResourcePackExploit) "On" else "Off"})"
            }

            10 -> {
                eaglercraftEnabled = !eaglercraftEnabled
                EaglercraftManager.enabled = eaglercraftEnabled
                eaglercraftButton.displayString = "Eaglercraft (${if (eaglercraftEnabled) "On" else "Off"})"
                if (!eaglercraftEnabled) {
                    EaglercraftManager.disconnect()
                }
            }

            11 -> {
                eaglercraftDebug = !eaglercraftDebug
                EaglercraftManager.debug = eaglercraftDebug
                eaglercraftDebugButton.displayString = "Debug (${if (eaglercraftDebug) "On" else "Off"})"
            }

            13 -> {
                if (EaglercraftManager.isProxyRunning()) {
                    EaglercraftManager.disconnect()
                } else {
                    startProxy()
                }
            }

            14 -> {
                if (!EaglercraftManager.isProxyRunning()) {
                    startProxy()
                }
                if (EaglercraftManager.isProxyRunning()) {
                    EaglercraftManager.autoConnectToProxy()
                }
            }

            0 -> {
                syncAndSave()
                mc.displayGuiScreen(prevGui)
            }
        }
    }

    private fun startProxy() {
        val url = eaglercraftUrlField.text.trim()
        if (url.isEmpty()) return

        eaglercraftUrl = url
        EaglercraftManager.debug = eaglercraftDebug
        val eaglerUrl = EaglercraftManager.buildEaglerUrl(url)
        val username = mc.session.username

        Thread {
            EaglercraftManager.connectToEaglerServer(eaglerUrl, username)
        }.start()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)
        Fonts.fontBold180.drawCenteredString("Fixes", width / 2f, height / 8f + 5f, 4673984, true)

        eaglercraftUrlField.drawTextBox()

        if (eaglercraftUrlField.text.isEmpty() && !eaglercraftUrlField.isFocused) {
            Fonts.fontRegular35.drawString(
                "wss://host:port or ws://host:port",
                eaglercraftUrlField.xPosition + 4f,
                eaglercraftUrlField.yPosition + 6f,
                0x808080,
                false
            )
        }

        if (eaglercraftEnabled) {
            val status = EaglercraftManager.getStatusText()
            val color = when {
                status.contains("Connected") -> 0x55FF55
                status.contains("ready") -> 0x55FFFF
                status.contains("Starting") -> 0xFFFF55
                else -> 0xFF5555
            }
            Fonts.fontRegular35.drawString(
                "Status: $status",
                eaglercraftUrlField.xPosition.toFloat(),
                eaglercraftUrlField.yPosition + 24f,
                color,
                false
            )

            val hint = EaglercraftManager.getConnectHint()
            if (hint.isNotEmpty()) {
                Fonts.fontRegular35.drawString(
                    hint,
                    eaglercraftUrlField.xPosition.toFloat(),
                    eaglercraftUrlField.yPosition + 36f,
                    0xAAAAFF,
                    false
                )
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun updateScreen() {
        eaglercraftUrlField.updateCursorCounter()
        eaglercraftConnectButton.displayString = getConnectButtonText()
    }

    @Throws(IOException::class)
    public override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            syncAndSave()
            mc.displayGuiScreen(prevGui)
            return
        }

        if (eaglercraftUrlField.isFocused) {
            if (!eaglercraftUrlField.textboxKeyTyped(typedChar, keyCode)) {
                return
            }
        }

        super.keyTyped(typedChar, keyCode)
    }

    public override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        eaglercraftUrlField.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    private fun syncAndSave() {
        eaglercraftUrl = eaglercraftUrlField.text
        EaglercraftManager.enabled = eaglercraftEnabled
        EaglercraftManager.debug = eaglercraftDebug
        saveConfig(valuesConfig)
    }

    override fun onGuiClosed() {
        syncAndSave()
        super.onGuiClosed()
    }
}
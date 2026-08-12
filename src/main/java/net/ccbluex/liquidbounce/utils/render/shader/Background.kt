/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.utils.render.shader

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.render.drawWithTessellatorWorldRenderer
import net.ccbluex.liquidbounce.utils.render.shader.shaders.BackgroundShader
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager.color
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import java.io.File
import java.util.concurrent.CountDownLatch
import javax.imageio.ImageIO

sealed class Background(val backgroundFile: File) {
    companion object {
        val BUILTIN_BACKGROUNDS = listOf(
            "aurora",
            "matrix",
            "starfield",
            "plasma",
            "waves",
            "fire",
            "neongrid",
            "rain",
            "clouds",
            "vortex",
            "fractal",
            "gradient"
        )

        val BUILTIN_BACKGROUND_NAMES = mapOf(
            0 to "Aurora",
            1 to "Matrix",
            2 to "Starfield",
            3 to "Plasma",
            4 to "Waves",
            5 to "Fire",
            6 to "Neon Grid",
            7 to "Rain",
            8 to "Clouds",
            9 to "Vortex",
            10 to "Fractal",
            11 to "Gradient"
        )

        fun fromFile(backgroundFile: File): Background {
            return when (backgroundFile.extension) {
                "png" -> ImageBackground(backgroundFile)
                "frag", "glsl", "shader" -> ShaderBackground(backgroundFile)
                else -> throw IllegalArgumentException("Invalid background file extension")
            }.also {
                it.initBackground()
            }
        }

        fun fromBuiltin(index: Int): BuiltinShaderBackground {
            val safeIndex = index.coerceIn(0, BUILTIN_BACKGROUNDS.size - 1)
            return BuiltinShaderBackground(safeIndex).also {
                it.initBackground()
            }
        }
    }

    protected abstract fun initBackground()

    abstract fun drawBackground(width: Int, height: Int)
}

private class ImageBackground(backgroundFile: File) : Background(backgroundFile) {

    private val resourceLocation = ResourceLocation("${CLIENT_NAME.lowercase()}/background.png")

    override fun initBackground() {
        try {
            val image = ImageIO.read(backgroundFile.inputStream())
            mc.textureManager.loadTexture(resourceLocation, DynamicTexture(image))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun drawBackground(width: Int, height: Int) {
        mc.textureManager.bindTexture(resourceLocation)
        color(1f, 1f, 1f, 1f)
        Gui.drawScaledCustomSizeModalRect(0, 0, 0f, 0f, width, height, width, height, width.toFloat(), height.toFloat())
    }
}

private class ShaderBackground(backgroundFile: File) : Background(backgroundFile) {

    private var shaderInitialized = false
    private lateinit var shader: Shader
    private val initializationLatch = CountDownLatch(1)

    override fun initBackground() {
        runCatching {
            shader = BackgroundShader(backgroundFile)
        }.onFailure {
            LOGGER.error("Failed to load background.", it)
        }.onSuccess {
            initializationLatch.countDown()
            shaderInitialized = true
            LOGGER.info("Successfully loaded background.")
        }
    }

    override fun drawBackground(width: Int, height: Int) {
        if (!shaderInitialized) {
            try {
                initializationLatch.await()
            } catch (e: Exception) {
                LOGGER.error(e.message)
                return
            }
        }

        if (shaderInitialized) {
            shader.startShader()

            drawWithTessellatorWorldRenderer {
                begin(7, DefaultVertexFormats.POSITION)
                pos(0.0, height.toDouble(), 0.0).endVertex()
                pos(width.toDouble(), height.toDouble(), 0.0).endVertex()
                pos(width.toDouble(), 0.0, 0.0).endVertex()
                pos(0.0, 0.0, 0.0).endVertex()
            }

            shader.stopShader()
        }
    }
}

class BuiltinShaderBackground(val index: Int) : Background(File("builtin")) {

    private var shaderInitialized = false
    private lateinit var shader: Shader
    private val initializationLatch = CountDownLatch(1)

    public override fun initBackground() {
        val safeIndex = index.coerceIn(0, BUILTIN_BACKGROUNDS.size - 1)
        val backgroundName = BUILTIN_BACKGROUNDS[safeIndex]
        val resourceLocation = ResourceLocation("edneko/shader/backgrounds/$backgroundName.frag")

        runCatching {
            shader = BackgroundShader(resourceLocation)
        }.onFailure {
            LOGGER.error("Failed to load builtin background: $backgroundName", it)
        }.onSuccess {
            initializationLatch.countDown()
            shaderInitialized = true
            LOGGER.info("Successfully loaded builtin background: $backgroundName")
        }
    }

    override fun drawBackground(width: Int, height: Int) {
        if (!shaderInitialized) {
            try {
                initializationLatch.await()
            } catch (e: Exception) {
                LOGGER.error(e.message)
                return
            }
        }

        if (shaderInitialized) {
            shader.startShader()

            drawWithTessellatorWorldRenderer {
                begin(7, DefaultVertexFormats.POSITION)
                pos(0.0, height.toDouble(), 0.0).endVertex()
                pos(width.toDouble(), height.toDouble(), 0.0).endVertex()
                pos(width.toDouble(), 0.0, 0.0).endVertex()
                pos(0.0, 0.0, 0.0).endVertex()
            }

            shader.stopShader()
        }
    }

    fun getName(): String {
        return BUILTIN_BACKGROUND_NAMES[index] ?: "Unknown"
    }
}

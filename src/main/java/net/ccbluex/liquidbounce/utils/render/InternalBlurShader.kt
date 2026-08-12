package net.ccbluex.liquidbounce.utils.render

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.shader.Framebuffer
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL11.*

object InternalBlurShader {
    private val mc = Minecraft.getMinecraft()
    private var blurOutputFramebuffer: Framebuffer? = null
    private var shaderProgramID: Int = -1
    private var uniformTextureLocation = -1
    private var uniformTexelSizeLocation = -1
    private var uniformDirectionLocation = -1
    private var uniformRadiusLocation = -1

    fun blurArea(x: Float, y: Float, width: Float, height: Float, radius: Float) {
        val sr = ScaledResolution(mc)
        val factor = sr.scaleFactor
        ensureShaderInitialized()
        ensureFramebuffer(mc.displayWidth, mc.displayHeight)

        val sX = (x * factor).toInt()
        val sY = (mc.displayHeight - (y * factor).toInt() - (height * factor).toInt())
        val sW = (width * factor).toInt()
        val sH = (height * factor).toInt()

        glEnable(GL_SCISSOR_TEST)
        val pad = (radius * factor).toInt()
        glScissor(sX - pad, sY - pad, sW + pad * 2, sH + pad * 2)

        val buffer = blurOutputFramebuffer ?: return
        val mainBuffer = mc.framebuffer

        buffer.framebufferClear()
        buffer.bindFramebuffer(true)
        mainBuffer.bindFramebufferTexture()
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 33071)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 33071)

        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        GL11.glOrtho(0.0, sr.scaledWidth_double, sr.scaledHeight_double, 0.0, 1000.0, 3000.0)
        GL11.glTranslated(0.0, 0.0, -2000.0)

        GL20.glUseProgram(shaderProgramID)
        GL20.glUniform2f(uniformTexelSizeLocation, 1.0f / mc.displayWidth, 1.0f / mc.displayHeight)
        GL20.glUniform1i(uniformTextureLocation, 0)
        GL20.glUniform1f(uniformRadiusLocation, radius)
        GL20.glUniform2f(uniformDirectionLocation, 1.0f, 0.0f)
        drawQuads()

        mainBuffer.bindFramebuffer(true)
        buffer.bindFramebufferTexture()
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 33071)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 33071)

        GL20.glUniform2f(uniformDirectionLocation, 0.0f, 1.0f)
        drawQuads()

        GL20.glUseProgram(0)

        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPopMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPopMatrix()

        glDisable(GL_SCISSOR_TEST)
    }

    private fun ensureShaderInitialized() {
        if (shaderProgramID != -1) return
        val vertexShaderSrc = "#version 120\nvoid main() { gl_TexCoord[0] = gl_MultiTexCoord0; gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex; }"
        val fragmentShaderSrc = "#version 120\nuniform sampler2D textureIn; uniform vec2 texelSize; uniform vec2 direction; uniform float radius;\nfloat gaussian(float x, float sigma) { return exp(-(x*x) / (2.0 * sigma * sigma)); }\nvoid main() { vec2 coord = gl_TexCoord[0].xy; vec4 sum = vec4(0.0); float totalWeight = 0.0; int range = int(min(radius, 50.0)); float sigma = radius / 2.0; for (int i = -range; i <= range; i++) { float weight = gaussian(float(i), sigma); vec2 offset = float(i) * texelSize * direction; sum += texture2D(textureIn, coord + offset) * weight; totalWeight += weight; } gl_FragColor = sum / totalWeight; }"
        val vID = createShader(vertexShaderSrc, GL20.GL_VERTEX_SHADER)
        val fID = createShader(fragmentShaderSrc, GL20.GL_FRAGMENT_SHADER)
        shaderProgramID = GL20.glCreateProgram()
        GL20.glAttachShader(shaderProgramID, vID)
        GL20.glAttachShader(shaderProgramID, fID)
        GL20.glLinkProgram(shaderProgramID)
        GL20.glUseProgram(shaderProgramID)
        uniformTextureLocation = GL20.glGetUniformLocation(shaderProgramID, "textureIn")
        uniformTexelSizeLocation = GL20.glGetUniformLocation(shaderProgramID, "texelSize")
        uniformDirectionLocation = GL20.glGetUniformLocation(shaderProgramID, "direction")
        uniformRadiusLocation = GL20.glGetUniformLocation(shaderProgramID, "radius")
        GL20.glUseProgram(0)
    }

    private fun ensureFramebuffer(w: Int, h: Int) {
        if (blurOutputFramebuffer == null || blurOutputFramebuffer!!.framebufferWidth != w || blurOutputFramebuffer!!.framebufferHeight != h) {
            blurOutputFramebuffer?.deleteFramebuffer()
            blurOutputFramebuffer = Framebuffer(w, h, true)
            blurOutputFramebuffer!!.setFramebufferFilter(9729)
        }
    }

    private fun createShader(src: String, type: Int): Int {
        val id = GL20.glCreateShader(type)
        GL20.glShaderSource(id, src)
        GL20.glCompileShader(id)
        return id
    }

    private fun drawQuads() {
        val sr = ScaledResolution(mc)
        val w = sr.scaledWidth_double
        val h = sr.scaledHeight_double
        glBegin(GL_QUADS)
        glTexCoord2f(0f, 1f); glVertex2d(0.0, 0.0)
        glTexCoord2f(0f, 0f); glVertex2d(0.0, h)
        glTexCoord2f(1f, 0f); glVertex2d(w, h)
        glTexCoord2f(1f, 1f); glVertex2d(w, 0.0)
        glEnd()
    }
}

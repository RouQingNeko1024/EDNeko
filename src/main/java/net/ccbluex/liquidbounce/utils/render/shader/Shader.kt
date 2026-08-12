/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.utils.render.shader

import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import org.apache.commons.io.IOUtils
import org.lwjgl.opengl.*
import org.lwjgl.opengl.ARBShaderObjects.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUseProgram
import java.io.File
import java.io.IOException
import java.nio.file.Files
import net.minecraft.util.ResourceLocation

abstract class Shader : MinecraftInstance {
    var programId = 0
        private set
    
    private val uniformsMap = mutableMapOf<String, Int>()

    constructor(fragmentShader: String) {
        val vertexShaderID: Int
        val fragmentShaderID: Int
        
        try {
            val vertexStream = javaClass.getResourceAsStream("/assets/minecraft/edneko/shader/vertex.vert")
            vertexShaderID = createShader(IOUtils.toString(vertexStream), ARBVertexShader.GL_VERTEX_SHADER_ARB)
            IOUtils.closeQuietly(vertexStream)
            
            val fragmentStream = javaClass.getResourceAsStream("/assets/minecraft/edneko/shader/$fragmentShader")
            fragmentShaderID = createShader(IOUtils.toString(fragmentStream), ARBFragmentShader.GL_FRAGMENT_SHADER_ARB)
            IOUtils.closeQuietly(fragmentStream)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        
        if (vertexShaderID == 0 || fragmentShaderID == 0)
            return
        
        programId = glCreateProgramObjectARB()
        
        if (programId == 0)
            return
        
        glAttachObjectARB(programId, vertexShaderID)
        glAttachObjectARB(programId, fragmentShaderID)
        
        glLinkProgramARB(programId)
        glValidateProgramARB(programId)
        
        LOGGER.info("[Shader] Successfully loaded: $fragmentShader")
    }

    @Throws(IOException::class)
    constructor(fragmentShader: File) {
        val vertexShaderID: Int
        val fragmentShaderID: Int
        
        val vertexStream = javaClass.getResourceAsStream("/assets/minecraft/edneko/shader/vertex.vert")
        vertexShaderID = createShader(IOUtils.toString(vertexStream), ARBVertexShader.GL_VERTEX_SHADER_ARB)
        IOUtils.closeQuietly(vertexStream)
        
        val fragmentStream = Files.newInputStream(fragmentShader.toPath())
        fragmentShaderID = createShader(IOUtils.toString(fragmentStream), ARBFragmentShader.GL_FRAGMENT_SHADER_ARB)
        IOUtils.closeQuietly(fragmentStream)
        
        if (vertexShaderID == 0 || fragmentShaderID == 0)
            return
        
        programId = glCreateProgramObjectARB()
        
        if (programId == 0)
            return
        
        glAttachObjectARB(programId, vertexShaderID)
        glAttachObjectARB(programId, fragmentShaderID)
        
        glLinkProgramARB(programId)
        glValidateProgramARB(programId)
        
        LOGGER.info("[Shader] Successfully loaded: " + fragmentShader.name)
    }

    @Throws(IOException::class)
    constructor(fragmentShader: ResourceLocation) {
        val vertexShaderID: Int
        val fragmentShaderID: Int
        
        try {
            val vertexStream = javaClass.getResourceAsStream("/assets/minecraft/edneko/shader/vertex.vert")
            vertexShaderID = createShader(IOUtils.toString(vertexStream), ARBVertexShader.GL_VERTEX_SHADER_ARB)
            IOUtils.closeQuietly(vertexStream)
            
            val fragmentStream = mc.resourceManager.getResource(fragmentShader).inputStream
            fragmentShaderID = createShader(IOUtils.toString(fragmentStream), ARBFragmentShader.GL_FRAGMENT_SHADER_ARB)
            IOUtils.closeQuietly(fragmentStream)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        
        if (vertexShaderID == 0 || fragmentShaderID == 0)
            return
        
        programId = glCreateProgramObjectARB()
        
        if (programId == 0)
            return
        
        glAttachObjectARB(programId, vertexShaderID)
        glAttachObjectARB(programId, fragmentShaderID)
        
        glLinkProgramARB(programId)
        glValidateProgramARB(programId)
        
        LOGGER.info("[Shader] Successfully loaded: ${fragmentShader.resourcePath}")
    }

    open fun startShader() {
        if (programId == 0) return
        
        glUseProgram(programId)

        if (uniformsMap.isEmpty())
            setupUniforms()

        updateUniforms()
    }

    open fun stopShader() {
        if (programId == 0) return
        
        glUseProgram(0)
    }

    abstract fun setupUniforms()
    abstract fun updateUniforms()
    private fun createShader(shaderSource: String, shaderType: Int): Int {
        var shader = 0

        return try {
            shader = glCreateShaderObjectARB(shaderType)

            if (shader == 0)
                return 0

            glShaderSourceARB(shader, shaderSource)
            glCompileShaderARB(shader)

            if (glGetObjectParameteriARB(shader, GL_OBJECT_COMPILE_STATUS_ARB) == GL_FALSE)
                throw RuntimeException("Error creating shader: " + getLogInfo(shader))

            shader
        } catch (e: Exception) {
            glDeleteObjectARB(shader)
            throw e
        }
    }

    private fun getLogInfo(i: Int) = glGetInfoLogARB(i, glGetObjectParameteriARB(i, GL_OBJECT_INFO_LOG_LENGTH_ARB))

    fun setUniform(uniformName: String, location: Int) {
        uniformsMap[uniformName] = location
    }

    fun setupUniform(uniformName: String) = setUniform(uniformName, glGetUniformLocation(programId, uniformName))

    fun getUniform(uniformName: String) = uniformsMap[uniformName]!!

    fun setUniformf(name: String, vararg args: Float) {
        val loc = glGetUniformLocation(programId, name)
        when (args.size) {
            1 -> org.lwjgl.opengl.GL20.glUniform1f(loc, args[0])
            2 -> org.lwjgl.opengl.GL20.glUniform2f(loc, args[0], args[1])
            3 -> org.lwjgl.opengl.GL20.glUniform3f(loc, args[0], args[1], args[2])
            4 -> org.lwjgl.opengl.GL20.glUniform4f(loc, args[0], args[1], args[2], args[3])
        }
    }

    companion object {
        fun drawQuad(x: Float, y: Float, width: Float, height: Float) {
            glBegin(GL_QUADS)
            glTexCoord2f(0.0f, 0.0f)
            glVertex2d(x.toDouble(), (y + height).toDouble())
            glTexCoord2f(1.0f, 0.0f)
            glVertex2d((x + width).toDouble(), (y + height).toDouble())
            glTexCoord2f(1.0f, 1.0f)
            glVertex2d((x + width).toDouble(), y.toDouble())
            glTexCoord2f(0.0f, 1.0f)
            glVertex2d(x.toDouble(), y.toDouble())
            glEnd()
        }
    }
}

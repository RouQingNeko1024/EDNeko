package net.ccbluex.liquidbounce.utils.render

import net.minecraft.client.Minecraft
import net.minecraft.client.shader.Framebuffer
import org.lwjgl.opengl.EXTFramebufferObject
import org.lwjgl.opengl.EXTPackedDepthStencil
import org.lwjgl.opengl.GL11.*

object EmbeddedStencil {
    fun checkSetupFBO(framebuffer: Framebuffer?) {
        if (framebuffer != null && framebuffer.depthBuffer > -1) {
            setupFBO(framebuffer)
            framebuffer.depthBuffer = -1
        }
    }

    fun setupFBO(framebuffer: Framebuffer) {
        EXTFramebufferObject.glDeleteRenderbuffersEXT(framebuffer.depthBuffer)
        val stencilDepthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT()
        EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID)
        EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, EXTPackedDepthStencil.GL_DEPTH_STENCIL_EXT, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight)
        EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID)
        EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID)
    }

    fun write(invert: Boolean) {
        checkSetupFBO(Minecraft.getMinecraft().framebuffer)
        glClearStencil(0)
        glClear(GL_STENCIL_BUFFER_BIT)
        glEnable(GL_STENCIL_TEST)
        glStencilFunc(GL_ALWAYS, 1, 65535)
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
        if (!invert) {
            glColorMask(false, false, false, false)
            glDepthMask(false)
            glStencilFunc(GL_ALWAYS, 1, 65535)
            glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
        }
    }

    fun erase(invert: Boolean) {
        glStencilFunc(if (invert) GL_EQUAL else GL_NOTEQUAL, 1, 65535)
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
        if (invert) {
            glColorMask(true, true, true, true)
            glDepthMask(true)
            glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
        } else {
            glColorMask(true, true, true, true)
            glDepthMask(true)
            glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
        }
    }

    fun dispose() {
        glDisable(GL_STENCIL_TEST)
    }
}

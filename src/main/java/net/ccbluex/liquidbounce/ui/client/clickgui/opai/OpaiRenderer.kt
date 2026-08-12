package net.ccbluex.liquidbounce.ui.client.clickgui.opai

import net.minecraft.client.gui.ScaledResolution
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.Constants.mc
import kotlin.math.roundToInt

class OpaiRenderer {

    companion object {
        private const val MAX_SLOTS = 32
    }

    private val slots = Array(MAX_SLOTS) { Slot() }
    private var slotIndex = -1
    private var slotCount = 0

    private fun current(): Slot = slots[slotIndex]

    fun shadow(): ShadowRenderer {
        val slot = current()
        if (slot.shadow == null) slot.shadow = ShadowRenderer.create()
        return slot.shadow!!
    }

    fun roundRect(): RoundRectRenderer {
        val slot = current()
        if (slot.roundRect == null) slot.roundRect = RoundRectRenderer.create()
        return slot.roundRect!!
    }

    fun outline(): RoundRectOutlineRenderer {
        val slot = current()
        if (slot.outline == null) slot.outline = RoundRectOutlineRenderer.create()
        return slot.outline!!
    }

    fun rect(): RectRenderer {
        val slot = current()
        if (slot.rect == null) slot.rect = RectRenderer.create()
        return slot.rect!!
    }

    fun triangle(): TriangleRenderer {
        val slot = current()
        if (slot.triangle == null) slot.triangle = TriangleRenderer.create()
        return slot.triangle!!
    }

    fun text(): TextRenderer {
        val slot = current()
        if (slot.text == null) slot.text = TextRenderer.create()
        return slot.text!!
    }

    fun setScissor(guiX: Float, guiY: Float, guiW: Float, guiH: Float, guiHeight: Int) {
        val sr = ScaledResolution(mc)
        val scale = sr.scaleFactor
        val x = (guiX * scale).roundToInt()
        val y = ((guiHeight - guiY - guiH) * scale).roundToInt()
        val w = (guiW * scale).roundToInt()
        val h = (guiH * scale).roundToInt()
        val slot = current()
        setScissorOn(slot.shadow, x, y, w, h)
        setScissorOn(slot.roundRect, x, y, w, h)
        setScissorOn(slot.outline, x, y, w, h)
        setScissorOn(slot.rect, x, y, w, h)
        setScissorOn(slot.triangle, x, y, w, h)
        setScissorOn(slot.text, x, y, w, h)
    }

    private fun setScissorOn(renderer: Any?, x: Int, y: Int, w: Int, h: Int) {
        when (renderer) {
            is ShadowRenderer -> renderer.setScissor(x, y, w, h)
            is RoundRectRenderer -> renderer.setScissor(x, y, w, h)
            is RoundRectOutlineRenderer -> renderer.setScissor(x, y, w, h)
            is RectRenderer -> renderer.setScissor(x, y, w, h)
            is TriangleRenderer -> renderer.setScissor(x, y, w, h)
            is TextRenderer -> renderer.setScissor(x, y, w, h)
        }
    }

    fun clearScissor() {
        val slot = current()
        clearScissorOn(slot.shadow)
        clearScissorOn(slot.roundRect)
        clearScissorOn(slot.outline)
        clearScissorOn(slot.rect)
        clearScissorOn(slot.triangle)
        clearScissorOn(slot.text)
    }

    private fun clearScissorOn(renderer: Any?) {
        when (renderer) {
            is ShadowRenderer -> renderer.clearScissor()
            is RoundRectRenderer -> renderer.clearScissor()
            is RoundRectOutlineRenderer -> renderer.clearScissor()
            is RectRenderer -> renderer.clearScissor()
            is TriangleRenderer -> renderer.clearScissor()
            is TextRenderer -> renderer.clearScissor()
        }
    }

    fun beginFrame() {
        slotIndex = -1
    }

    fun beginPass() {
        slotIndex++
        if (slotIndex >= slotCount) {
            if (slotCount >= MAX_SLOTS) {
                throw IllegalStateException("exceeded max renderer slots: $MAX_SLOTS")
            }
            slotCount++
        }
        slots[slotIndex].flushed = false
    }

    fun flush() {
        val slot = current()
        if (slot.flushed) return
        slot.flushed = true
        slot.shadow?.drawAndClear()
        slot.roundRect?.drawAndClear()
        slot.outline?.drawAndClear()
        slot.rect?.drawAndClear()
        slot.triangle?.drawAndClear()
        slot.text?.drawAndClear()
    }

    fun close() {
        for (i in 0 until slotCount) {
            val slot = slots[i]
            slot.shadow?.close()
            slot.roundRect?.close()
            slot.outline?.close()
            slot.rect?.close()
            slot.triangle?.close()
            slot.text?.close()
        }
    }

    private class Slot {
        var flushed = false
        var shadow: ShadowRenderer? = null
        var roundRect: RoundRectRenderer? = null
        var outline: RoundRectOutlineRenderer? = null
        var rect: RectRenderer? = null
        var triangle: TriangleRenderer? = null
        var text: TextRenderer? = null
    }

}

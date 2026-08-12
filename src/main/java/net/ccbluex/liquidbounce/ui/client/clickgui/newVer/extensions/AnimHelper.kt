/*
 * LiquidBounce++ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/PlusPlusMC/LiquidBouncePlusPlus/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.extensions

import net.ccbluex.liquidbounce.features.module.modules.client.NewGUI
import net.ccbluex.liquidbounce.utils.render.LBPPAnimationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils

fun Float.animSmooth(target: Float, speed: Float): Float {
    if (NewGUI.fastRenderValue) return target
    val delta = RenderUtils.deltaTime.coerceIn(1, 50)
    return LBPPAnimationUtils.animate(target, this, speed * delta * 0.01F)
}

fun Float.animLinear(speed: Float, min: Float, max: Float) = if (NewGUI.fastRenderValue) { if (speed < 0F) min else max } else (this + speed).coerceIn(min, max)
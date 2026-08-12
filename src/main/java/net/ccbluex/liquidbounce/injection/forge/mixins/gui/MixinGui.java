/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinGui {
    @Shadow
    public abstract void drawTexturedModalRect(float xCoord, float yCoord, int minU, int minV, int maxU, int maxV);

    @Shadow protected float zLevel;

    @Inject(method = "drawTexturedModalRect(IIIIII)V", at = @At("HEAD"), cancellable = true)
    private void injectForgeButtonRenderPrevention(int p_drawTexturedModalRect_1_, int p_drawTexturedModalRect_2_, int p_drawTexturedModalRect_3_, int p_drawTexturedModalRect_4_, int p_drawTexturedModalRect_5_, int p_drawTexturedModalRect_6_, CallbackInfo ci) {
        if ((Object) this instanceof GuiSlider) {
            ci.cancel();
        }
    }
}

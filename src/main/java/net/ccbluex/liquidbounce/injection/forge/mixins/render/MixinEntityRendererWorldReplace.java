/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.features.module.modules.render.WorldReplace;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

@Mixin(EntityRenderer.class)
@SideOnly(Side.CLIENT)
public class MixinEntityRendererWorldReplace {

    @Inject(method = "updateFogColor", at = @At("TAIL"))
    private void updateFogColor(float partialTicks, CallbackInfo ci) {
        WorldReplace module = WorldReplace.INSTANCE;
        if (module.handleEvents()) {
            Color color = module.getSkyColorValue();
            try {
                net.minecraft.client.renderer.GlStateManager.clearColor(
                    color.getRed() / 255.0F,
                    color.getGreen() / 255.0F,
                    color.getBlue() / 255.0F,
                    1.0F
                );
            } catch (Exception ignored) {}
        }
    }
}

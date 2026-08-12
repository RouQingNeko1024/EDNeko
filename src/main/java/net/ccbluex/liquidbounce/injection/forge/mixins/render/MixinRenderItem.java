/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.features.module.modules.render.Glint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(RenderItem.class)
public abstract class MixinRenderItem {

    @Shadow
    protected abstract void renderModel(IBakedModel model, int color);

    @Inject(method = "renderItemIntoGUI", at = @At("RETURN"))
    private void onRenderItemReturn(ItemStack stack, int x, int y, CallbackInfo ci) {
        if (!Glint.INSTANCE.handleEvents()) return;
        if (stack == null) return;
        
        Item item = stack.getItem();
        if (item == null) return;
        
        String targetItems = Glint.INSTANCE.getTargetItems();
        boolean isSword = item instanceof ItemSword;
        boolean isEnchanted = stack.hasEffect();
        
        boolean shouldRender = false;
        if ("All".equals(targetItems)) {
            shouldRender = true;
        } else if ("Swords".equals(targetItems)) {
            shouldRender = isSword;
        } else if ("Enchanted".equals(targetItems)) {
            shouldRender = isEnchanted;
        }
        
        if (!shouldRender) return;
        
        float intensity = Glint.INSTANCE.getIntensity();
        
        Color glintColor = Glint.INSTANCE.getColor();
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getRenderItem() == null) return;
        
        IBakedModel model;
        try {
            model = mc.getRenderItem().getItemModelMesher().getItemModel(stack);
        } catch (Exception e) {
            return;
        }
        
        if (model == null) return;
        
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        
        int r = glintColor.getRed();
        int g = glintColor.getGreen();
        int b = glintColor.getBlue();
        
        int passes = (int) (intensity * 10);
        for (int i = 0; i < passes; i++) {
            int a = (int) (255 * (intensity / passes) * (1 + i * 0.1f));
            a = Math.min(a, 255);
            int customColor = (a << 24) | (r << 16) | (g << 8) | b;
            this.renderModel(model, customColor);
        }
        
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    @Redirect(method = "renderEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderModel(Lnet/minecraft/client/resources/model/IBakedModel;I)V"))
    private void renderEffectModel(RenderItem renderItem, IBakedModel model, int color) {
        if (!Glint.INSTANCE.handleEvents()) {
            this.renderModel(model, -8372020);
            return;
        }

        String targetItems = Glint.INSTANCE.getTargetItems();
        if (!"Enchanted".equals(targetItems)) {
            this.renderModel(model, -8372020);
            return;
        }

        Color glintColor = Glint.INSTANCE.getColor();
        float intensity = Glint.INSTANCE.getIntensity();
        
        int r = glintColor.getRed();
        int g = glintColor.getGreen();
        int b = glintColor.getBlue();
        
        int passes = (int) (intensity * 10);
        for (int i = 0; i < passes; i++) {
            int a = (int) (255 * (intensity / passes) * (1 + i * 0.1f));
            a = Math.min(a, 255);
            int customColor = (a << 24) | (r << 16) | (g << 8) | b;
            this.renderModel(model, customColor);
        }
    }
}

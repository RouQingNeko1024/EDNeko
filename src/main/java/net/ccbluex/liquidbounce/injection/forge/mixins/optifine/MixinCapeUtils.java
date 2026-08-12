/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.optifine;

import net.ccbluex.liquidbounce.utils.client.WeakCapeImageBuffer;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "CapeUtils", remap = false)
public abstract class MixinCapeUtils {

    @Redirect(method = "downloadCape(Lnet/minecraft/client/entity/AbstractClientPlayer;)V", at = @At(value = "NEW", target = "CapeUtils$1"), remap = false, require = 0)
    private static IImageBuffer createWeakCapeImageBuffer(AbstractClientPlayer player, ResourceLocation resourceLocation) {
        return new WeakCapeImageBuffer(player, resourceLocation);
    }
}

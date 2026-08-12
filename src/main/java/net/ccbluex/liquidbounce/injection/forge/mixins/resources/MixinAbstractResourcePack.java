/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.resources;

import net.ccbluex.liquidbounce.utils.client.ResourcePackIconScaler;
import net.minecraft.client.resources.AbstractResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.image.BufferedImage;

@Mixin(AbstractResourcePack.class)
public abstract class MixinAbstractResourcePack {

    @Inject(method = "getPackImage", at = @At("RETURN"), cancellable = true)
    private void scalePackImage(CallbackInfoReturnable<BufferedImage> callbackInfo) {
        callbackInfo.setReturnValue(ResourcePackIconScaler.scale(callbackInfo.getReturnValue()));
    }
}

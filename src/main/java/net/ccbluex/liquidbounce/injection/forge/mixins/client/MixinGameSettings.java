package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EnumPlayerModelParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameSettings.class)
public class MixinGameSettings {

    @Shadow public int guiScale;

    @Inject(method = "<init>()V", at = @At("RETURN"))
    private void injectDefaults(final CallbackInfo callbackInfo) {
        this.guiScale = 2;

        for (EnumPlayerModelParts part : EnumPlayerModelParts.values()) {
            ((GameSettings)(Object)this).setModelPartEnabled(part, true);
        }
    }

}
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.utils.inputfix.GuiScreenFix;
import net.ccbluex.liquidbounce.utils.inputfix.InputFixInit;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiScreen.class)
public class MixinGuiScreenInputFix
{
    @Shadow
    public void keyTyped(char typedChar, int keyCode) {}

    @Overwrite
    public void handleKeyboardInput()
    {
        if (InputFixInit.impl != null)
        {
            GuiScreenFix.handleKeyboardInput((GuiScreen) (Object) this);
        }
        else
        {
            char c = Keyboard.getEventCharacter();
            int k = Keyboard.getEventKey();
            if (Keyboard.getEventKeyState() || (k == 0 && Character.isDefined(c)))
            {
                this.keyTyped(c, k);
            }
        }
    }
}

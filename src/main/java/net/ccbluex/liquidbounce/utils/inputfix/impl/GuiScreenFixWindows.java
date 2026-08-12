package net.ccbluex.liquidbounce.utils.inputfix.impl;

import net.ccbluex.liquidbounce.utils.inputfix.IGuiScreen;
import net.ccbluex.liquidbounce.utils.inputfix.IGuiScreenFix;
import org.lwjgl.input.Keyboard;

public class GuiScreenFixWindows implements IGuiScreenFix
{
    @Override
    public void handleKeyboardInput(IGuiScreen gui)
    {
        char c = Keyboard.getEventCharacter();
        int k = Keyboard.getEventKey();
        if (Keyboard.getEventKeyState() || (k == 0 && Character.isDefined(c)))
        {
            gui.keyTyped(c, k);
        }
    }
}

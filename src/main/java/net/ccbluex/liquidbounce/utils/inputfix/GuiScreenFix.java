package net.ccbluex.liquidbounce.utils.inputfix;

import java.lang.reflect.Method;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

public class GuiScreenFix
{
    private static class Proxy implements IGuiScreen
    {
        private GuiScreen gui;

        @Override
        public void keyTyped(char c, int k)
        {
            try
            {
                if (gui != null)
                    keyTyped.invoke(gui, c, k);
            }
            catch (Throwable t)
            {
                throw new RuntimeException(t);
            }
        }

        public Proxy setGui(GuiScreen gui)
        {
            this.gui = gui;
            return this;
        }
    }

    private static final ThreadLocal<Proxy> proxies = new ThreadLocal<Proxy>()
    {
        @Override
        protected Proxy initialValue()
        {
            return new Proxy();
        }
    };

    private static final Method keyTyped = ReflectionHelper.findMethod(GuiScreen.class, new String[] { "func_73869_a", "keyTyped" }, new Class<?>[] { char.class, int.class });

    public static void handleKeyboardInput(GuiScreen gui)
    {
        Proxy p = proxies.get().setGui(gui);
        if (InputFixInit.impl != null)
            InputFixInit.impl.handleKeyboardInput(p);
        else if (Keyboard.getEventKeyState())
            p.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
        gui.mc.dispatchKeypresses();
    }
}

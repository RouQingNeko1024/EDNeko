/*
 * LiquidBounce++ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/PlusPlusMC/LiquidBouncePlusPlus/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.Category;
import net.ccbluex.liquidbounce.features.module.modules.client.NewGUI;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.ccbluex.liquidbounce.utils.MouseUtils;
import net.ccbluex.liquidbounce.utils.render.*;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MaterialClickGUI extends GuiScreen {
    private static MaterialClickGUI instance;

    public static MaterialClickGUI getInstance() {
        return instance == null ? instance = new MaterialClickGUI() : instance;
    }

    private static final Map<Category, Boolean> catPanels = new HashMap<>();
    private static final Map<Category, Float> catPanelsF = new HashMap<>();

    private static final Map<Module, Boolean> expandedMods = new HashMap<>();
    private static final Map<Module, Float> expandedModsF = new HashMap<>();

    private static final Map<Module, Boolean> keybindListen = new HashMap<>();

    private final Map<Category, Float> catX = new HashMap<>();
    private final Map<Category, Float> catY = new HashMap<>();

    private int scroll = 0;

    private static final int PANEL_WIDTH = 120;
    private static final int PANEL_HEIGHT = 24;

    private static final int MODULE_HEIGHT = 20;
    private static final int VALUE_HEIGHT = 18;

    public MaterialClickGUI() {
        for (Category category : Category.values()) {
            if (category.shouldShow()) {
                catPanels.put(category, false);
                catPanelsF.put(category, 0f);
                catX.put(category, 0f);
                catY.put(category, 0f);
            }
        }
        for (Module module : LiquidBounce.INSTANCE.getModuleManager().getModules()) {
            expandedMods.put(module, false);
            expandedModsF.put(module, 0f);
            keybindListen.put(module, false);
        }
    }

    private static final ResourceLocation background = new ResourceLocation("edneko/background.png");

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        Color accentColor = NewGUI.INSTANCE.getAccentColor();

        // Background
        if (!NewGUI.INSTANCE.getFastRenderValue()) {
            mc.getTextureManager().bindTexture(background);
            GlStateManager.color(1f, 1f, 1f, 1f);
            drawScaledCustomSizeModalRect(0, 0, 0, 0, 64, 64, sr.getScaledWidth(), sr.getScaledHeight(), 64f, 64f);
        }

        // Panels
        int startX = 20;
        int startY = 20 + scroll;
        int gap = 5;

        for (Category category : Category.values()) {
            if (!category.shouldShow()) continue;

            float animF = catPanelsF.get(category);
            animF = LBPPAnimationUtils.animate(catPanels.get(category) ? 1f : 0f, animF, 0.1f * RenderUtils.INSTANCE.getDeltaTime() * 0.025F);
            catPanelsF.put(category, animF);

            List<Module> modules = LiquidBounce.INSTANCE.getModuleManager().getModules()
                    .stream()
                    .filter(m -> m.getCategory() == category)
                    .collect(Collectors.toList());

            float totalModuleHeight = (modules.size() * MODULE_HEIGHT) + (modules.stream()
                    .mapToInt(m -> {
                        float e = expandedModsF.get(m);
                        int count = (int) (e * m.getValues().size());
                        return count * VALUE_HEIGHT;
                    }).sum());

            float realTotalH = PANEL_HEIGHT + animF * totalModuleHeight;

            // Panel title
            int bgColor = Color.BLACK.getRGB();
            int bgAlpha = (bgColor >> 24) & 0xFF;
            int bgRed = (bgColor >> 16) & 0xFF;
            int bgGreen = (bgColor >> 8) & 0xFF;
            int bgBlue = bgColor & 0xFF;
            Color bgWithAlpha = new Color(bgRed, bgGreen, bgBlue, 180);
            RenderUtils.INSTANCE.drawRoundedRect(startX, startY, startX + PANEL_WIDTH, startY + realTotalH, bgWithAlpha.getRGB(), 5f, net.ccbluex.liquidbounce.utils.render.RenderUtils.RoundedCorners.ALL);

            // Title text
            Fonts.INSTANCE.getFont40().drawString(category.getDisplayName(), startX + 10, startY + PANEL_HEIGHT / 2f - Fonts.INSTANCE.getFont40().getHeight() / 2f + 2, -1);

            if (animF > 0.05f) {
                float currentY = startY + PANEL_HEIGHT;
                for (Module module : modules) {
                    float e = expandedModsF.get(module);
                    e = LBPPAnimationUtils.animate(expandedMods.get(module) ? 1f : 0f, e, 0.1f * RenderUtils.INSTANCE.getDeltaTime() * 0.025F);
                    expandedModsF.put(module, e);

                    int valuesHeight = (int) (e * module.getValues().size() * VALUE_HEIGHT);

                    // Module background
                    Color moduleBgColor = module.getState() ? new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 200) : new Color(40, 40, 40, 200);
                    RenderUtils.INSTANCE.drawRoundedRect(startX + 2, currentY, startX + PANEL_WIDTH - 2, currentY + MODULE_HEIGHT + valuesHeight, moduleBgColor.getRGB(), 3f, net.ccbluex.liquidbounce.utils.render.RenderUtils.RoundedCorners.ALL);

                    // Module name
                    String displayName = module.getName();
                    Fonts.INSTANCE.getFont35().drawString(displayName, startX + 10, currentY + MODULE_HEIGHT / 2f - Fonts.INSTANCE.getFont35().getHeight() / 2f + 1.5f, -1);

                    // Keybind
                    String keyName = keybindListen.get(module) ? "Listening" : Keyboard.getKeyName(module.getKeyBind());
                    Fonts.INSTANCE.getFont30().drawString("[" + keyName + "]", startX + PANEL_WIDTH - 4 - Fonts.INSTANCE.getFont30().getStringWidth("[" + keyName + "]"), currentY + MODULE_HEIGHT / 2f - Fonts.INSTANCE.getFont30().getHeight() / 2f + 1.5f, new Color(180, 180, 180).getRGB());

                    // Values
                    if (e > 0.05f) {
                        float vY = currentY + MODULE_HEIGHT;
                        for (int i = 0; i < module.getValues().size(); i++) {
                            if (i >= e * module.getValues().size()) break;

                            net.ccbluex.liquidbounce.config.Value<?> value = module.getValues().get(i);
                            if (!value.shouldRender()) continue;

                            // Value name
                            Fonts.INSTANCE.getFont30().drawString(value.getName(), startX + 15, vY + VALUE_HEIGHT / 2f - Fonts.INSTANCE.getFont30().getHeight() / 2f + 1f, new Color(200, 200, 200).getRGB());

                            vY += VALUE_HEIGHT;
                        }
                    }

                    currentY += MODULE_HEIGHT + valuesHeight;
                }
            }

            startX += PANEL_WIDTH + gap;
            if (startX + PANEL_WIDTH > sr.getScaledWidth() - 20) {
                startX = 20;
                startY += realTotalH + gap;
            }
        }

        // Scroll
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            if (wheel > 0) scroll += 20;
            else scroll -= 20;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        ScaledResolution sr = new ScaledResolution(mc);
        int startX = 20;
        int startY = 20 + scroll;
        int gap = 5;

        for (Category category : Category.values()) {
            if (!category.shouldShow()) continue;

            float animF = catPanelsF.get(category);
            List<Module> modules = LiquidBounce.INSTANCE.getModuleManager().getModules()
                    .stream()
                    .filter(m -> m.getCategory() == category)
                    .collect(Collectors.toList());

            float totalModuleHeight = (modules.size() * MODULE_HEIGHT) + (modules.stream()
                    .mapToInt(m -> {
                        float e = expandedModsF.get(m);
                        int count = (int) (e * m.getValues().size());
                        return count * VALUE_HEIGHT;
                    }).sum());

            float realTotalH = PANEL_HEIGHT + animF * totalModuleHeight;

            if (MouseUtils.INSTANCE.mouseWithinBounds(mouseX, mouseY, startX, startY, startX + PANEL_WIDTH, startY + PANEL_HEIGHT)) {
                catPanels.put(category, !catPanels.get(category));
            } else if (MouseUtils.INSTANCE.mouseWithinBounds(mouseX, mouseY, startX, startY + PANEL_HEIGHT, startX + PANEL_WIDTH, startY + realTotalH) && animF > 0.05f) {
                float currentY = startY + PANEL_HEIGHT;
                for (Module module : modules) {
                    float e = expandedModsF.get(module);
                    int valuesHeight = (int) (e * module.getValues().size() * VALUE_HEIGHT);
                    if (MouseUtils.INSTANCE.mouseWithinBounds(mouseX, mouseY, startX + 2, currentY, startX + PANEL_WIDTH - 2, currentY + MODULE_HEIGHT + valuesHeight)) {
                        if (MouseUtils.INSTANCE.mouseWithinBounds(mouseX, mouseY, startX + 2, currentY, startX + PANEL_WIDTH - 2, currentY + MODULE_HEIGHT)) {
                            if (keybindListen.get(module)) {
                                keybindListen.put(module, false);
                            } else if (mouseButton == 0) {
                                module.toggle();
                            } else if (mouseButton == 1) {
                                expandedMods.put(module, !expandedMods.get(module));
                            } else if (mouseButton == 2) {
                                keybindListen.put(module, true);
                            }
                        }
                        break;
                    }
                    currentY += MODULE_HEIGHT + valuesHeight;
                }
            }

            startX += PANEL_WIDTH + gap;
            if (startX + PANEL_WIDTH > sr.getScaledWidth() - 20) {
                startX = 20;
                startY += realTotalH + gap;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            for (Module module : keybindListen.keySet()) {
                keybindListen.put(module, false);
            }
            mc.displayGuiScreen(null);
        } else {
            for (Module module : keybindListen.keySet()) {
                if (keybindListen.get(module)) {
                    if (keyCode == Keyboard.KEY_ESCAPE) {
                        module.setKeyBind(0);
                        keybindListen.put(module, false);
                    } else {
                        module.setKeyBind(keyCode);
                        keybindListen.put(module, false);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
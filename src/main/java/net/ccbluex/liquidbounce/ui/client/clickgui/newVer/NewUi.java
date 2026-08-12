/*
 * LiquidBounce++ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/PlusPlusMC/LiquidBouncePlusPlus/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer;

import net.ccbluex.liquidbounce.features.module.Category;
import net.ccbluex.liquidbounce.features.module.modules.client.NewGUI;
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.element.CategoryElement;
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.element.SearchElement;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NewUi extends GuiScreen {
    private static NewUi instance;
    public static NewUi getInstance() {
        return instance == null ? instance = new NewUi() : instance;
    }

    private static final ResourceLocation background = new ResourceLocation("edneko/background.png");

    public final List<CategoryElement> categoryElements = new ArrayList<>();

    private final SearchElement searchElement = new SearchElement();

    public int scrollAmt = 0;
    private float targetScrollAmt = 0F;
    private float animScrollAmt = 0F;
    private float maxScroll = 0F;
    
    private boolean isDragging = false;
    private float dragOffsetX = 0F;
    private float dragOffsetY = 0F;
    private float customPanelX = 0F;
    private float customPanelY = 30F;
    private boolean hasCustomPosition = false;

    private int lastScaledWidth = 0;
    private int lastScaledHeight = 0;
    private float lastScaleWidth = 0F;
    private float lastScaleHeight = 0F;
    private float cachedPanelX = 0F;
    private float cachedPanelY = 30F;
    private float cachedPanelWidth = 0F;
    private float cachedPanelHeight = 0F;

    public NewUi() {
        for (Category c : Category.values()) {
            if (c.shouldShow()) {
                CategoryElement e = new CategoryElement(c);
                if (categoryElements.isEmpty()) e.setFocused(true);
                categoryElements.add(e);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        final Color accentColor = NewGUI.INSTANCE.getAccentColor();

        final ScaledResolution sr = new ScaledResolution(mc);
        int scrollWheel = Mouse.getDWheel();

        final float scaleWidth = NewGUI.INSTANCE.getScaleWidth();
        final float scaleHeight = NewGUI.INSTANCE.getScaleHeight();
        final float panelWidth = sr.getScaledWidth() * scaleWidth;
        final float panelHeight = sr.getScaledHeight() * scaleHeight;

        if (sr.getScaledWidth() != lastScaledWidth || sr.getScaledHeight() != lastScaledHeight ||
            scaleWidth != lastScaleWidth || scaleHeight != lastScaleHeight) {
            lastScaledWidth = sr.getScaledWidth();
            lastScaledHeight = sr.getScaledHeight();
            lastScaleWidth = scaleWidth;
            lastScaleHeight = scaleHeight;
            cachedPanelX = (sr.getScaledWidth() - panelWidth) / 2;
            cachedPanelY = (sr.getScaledHeight() - panelHeight) / 2;
            cachedPanelWidth = panelWidth;
            cachedPanelHeight = panelHeight;
        }

        final float panelX = hasCustomPosition ? customPanelX : cachedPanelX;
        final float panelY = hasCustomPosition ? customPanelY : cachedPanelY;

        if (scrollWheel != 0) {
            if (mouseX >= panelX + 5 && mouseX <= panelX + 195 &&
                mouseY >= panelY + 5 && mouseY <= panelY + panelHeight - 5) {
                targetScrollAmt -= scrollWheel * 0.5F;
                targetScrollAmt = Math.max(0, Math.min(targetScrollAmt, maxScroll));
            }
        }

        animScrollAmt += (targetScrollAmt - animScrollAmt) * 0.15F;

        int bgColor = ColorManager.INSTANCE.getBackground().getRGB();
        int bgAlpha = (bgColor >> 24) & 0xFF;
        int bgRed = (bgColor >> 16) & 0xFF;
        int bgGreen = (bgColor >> 8) & 0xFF;
        int bgBlue = bgColor & 0xFF;
        Color bgWithAlpha = new Color(bgRed, bgGreen, bgBlue, 200);

        RenderUtils.INSTANCE.originalRoundedRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 10, bgWithAlpha.getRGB());

        // left sidebar
        RenderUtils.INSTANCE.drawRoundedRect(panelX + 5, panelY + 5, panelX + 195, panelY + panelHeight - 5, ColorManager.INSTANCE.getDropDown().getRGB(), 5f, RenderUtils.RoundedCorners.ALL);
        
        float totalCategoryHeight = categoryElements.size() * 37F;
        maxScroll = Math.max(0, totalCategoryHeight - (panelHeight - 60F));
        
        float categoryY = panelY + 50F - animScrollAmt;
        for (CategoryElement c : categoryElements) {
            if (categoryY + 35F >= panelY + 5 && categoryY <= panelY + panelHeight - 5) {
                c.drawLabel(mouseX, mouseY, panelX, categoryY, 200, 35);
            }
            categoryY += 37F;
        }

        // Search box
        float searchX = panelX + 205;
        float searchY = panelY + 10;
        float searchW = panelWidth - 210;
        float searchH = 30;

        boolean usingSearch = searchElement.drawBox(mouseX, mouseY, searchX, searchY, searchW, searchH, accentColor);

        int moduleScrollWheel = 0;
        if (mouseX >= panelX + 205 && mouseX <= panelX + panelWidth - 5 &&
            mouseY >= panelY + 45 && mouseY <= panelY + panelHeight - 5) {
            moduleScrollWheel = scrollWheel;
        }

        if (usingSearch) {
            searchElement.drawPanel(mouseX, mouseY, panelX + 205, panelY + 45, panelWidth - 210, panelHeight - 50, moduleScrollWheel, categoryElements, accentColor);
        } else {
            CategoryElement ce = categoryElements.stream().filter(CategoryElement::getFocused).findFirst().orElse(categoryElements.get(0));
            ce.drawPanel(mouseX, mouseY, panelX + 205, panelY + 45, panelWidth - 210, panelHeight - 50, moduleScrollWheel, accentColor);
        }

        // title bar
        Fonts.INSTANCE.getFontSF35().drawString("edneko", panelX + 20, panelY + 10, Color.WHITE.getRGB());

        // right sidebar top
        Fonts.INSTANCE.getFont35().drawString("ClickGUI", panelX + 210, panelY + 15, Color.WHITE.getRGB());
        
        handleDrag(mouseX, mouseY);
    }
    
    private void handleDrag(int mouseX, int mouseY) {
        if (isDragging && Mouse.isButtonDown(0)) {
            hasCustomPosition = true;
            customPanelX = mouseX - dragOffsetX;
            customPanelY = mouseY - dragOffsetY;
        }
        if (!Mouse.isButtonDown(0)) {
            isDragging = false;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        final ScaledResolution sr = new ScaledResolution(mc);
        final float scaleWidth = NewGUI.INSTANCE.getScaleWidth();
        final float scaleHeight = NewGUI.INSTANCE.getScaleHeight();
        final float panelWidth = sr.getScaledWidth() * scaleWidth;
        final float panelHeight = sr.getScaledHeight() * scaleHeight;
        final float panelX = hasCustomPosition ? customPanelX : (sr.getScaledWidth() - panelWidth) / 2;
        final float panelY = hasCustomPosition ? customPanelY : (sr.getScaledHeight() - panelHeight) / 2;

        if (mouseButton == 0) {
            if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
                mouseY >= panelY && mouseY <= panelY + 40) {
                isDragging = true;
                dragOffsetX = mouseX - panelX;
                dragOffsetY = mouseY - panelY;
                return;
            }
        }

        float categoryY = panelY + 50F - animScrollAmt;

        for (CategoryElement c : categoryElements) {
            if (mouseX >= panelX && mouseX <= panelX + 200 && mouseY >= categoryY && mouseY <= categoryY + 35) {
                categoryElements.forEach(x -> x.setFocused(false));
                c.setFocused(true);
            }
            categoryY += 37F;
        }

        float searchX = panelX + 205;
        float searchY = panelY + 10;
        float searchW = panelWidth - 210;
        float searchH = 30;
        
        if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
            searchElement.handleMouseClick(mouseX, mouseY, mouseButton, searchX, searchY, searchW, searchH, categoryElements);
            return;
        }
        
        searchElement.clearFocus();

        boolean hasSearchContent = searchElement.hasSearchContent();
        if (hasSearchContent) {
            searchElement.handleMouseClick(mouseX, mouseY, mouseButton, searchX, panelY + 45, searchW, panelHeight - 50, categoryElements);
        } else {
            CategoryElement ce = categoryElements.stream().filter(CategoryElement::getFocused).findFirst().orElse(categoryElements.get(0));
            ce.handleMouseClick(mouseX, mouseY, mouseButton, searchX, panelY + 45, searchW, panelHeight - 50);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        isDragging = false;
        
        final ScaledResolution sr = new ScaledResolution(mc);
        final float scaleWidth = NewGUI.INSTANCE.getScaleWidth();
        final float scaleHeight = NewGUI.INSTANCE.getScaleHeight();
        final float panelWidth = sr.getScaledWidth() * scaleWidth;
        final float panelHeight = sr.getScaledHeight() * scaleHeight;
        final float panelX = hasCustomPosition ? customPanelX : (sr.getScaledWidth() - panelWidth) / 2;
        final float panelY = hasCustomPosition ? customPanelY : (sr.getScaledHeight() - panelHeight) / 2;

        float searchX = panelX + 205;
        float searchY = panelY + 10;
        float searchW = panelWidth - 210;
        float searchH = 30;
        
        boolean hasSearchContent = searchElement.hasSearchContent();
        if (hasSearchContent) {
            searchElement.handleMouseRelease(mouseX, mouseY, state, searchX, panelY + 45, searchW, panelHeight - 50, categoryElements);
        } else {
            CategoryElement ce = categoryElements.stream().filter(CategoryElement::getFocused).findFirst().orElse(categoryElements.get(0));
            ce.handleMouseRelease(mouseX, mouseY, state, searchX, panelY + 45, searchW, panelHeight - 50);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isDragging && clickedMouseButton == 0) {
            hasCustomPosition = true;
            customPanelX = mouseX - dragOffsetX;
            customPanelY = mouseY - dragOffsetY;
            return;
        }

        final ScaledResolution sr = new ScaledResolution(mc);
        final float scaleWidth = NewGUI.INSTANCE.getScaleWidth();
        final float scaleHeight = NewGUI.INSTANCE.getScaleHeight();
        final float panelWidth = sr.getScaledWidth() * scaleWidth;
        final float panelHeight = sr.getScaledHeight() * scaleHeight;
        final float panelX = hasCustomPosition ? customPanelX : (sr.getScaledWidth() - panelWidth) / 2;
        final float panelY = hasCustomPosition ? customPanelY : (sr.getScaledHeight() - panelHeight) / 2;

        float searchX = panelX + 205;
        float searchW = panelWidth - 210;
        
        boolean hasSearchContent = searchElement.hasSearchContent();
        if (hasSearchContent) {
            searchElement.handleMouseDrag(mouseX, mouseY, clickedMouseButton, searchX, panelY + 45, searchW, panelHeight - 50, categoryElements);
        } else {
            CategoryElement ce = categoryElements.stream().filter(CategoryElement::getFocused).findFirst().orElse(categoryElements.get(0));
            ce.handleMouseDrag(mouseX, mouseY, clickedMouseButton, searchX, panelY + 45, searchW, panelHeight - 50);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        CategoryElement ce = categoryElements.stream().filter(CategoryElement::getFocused).findFirst().orElse(categoryElements.get(0));
        
        boolean hasKeybindListening = ce.isAnyKeybindListening() || searchElement.isAnyKeybindListening(categoryElements);
        
        if (keyCode == 1 && !hasKeybindListening) {
            mc.displayGuiScreen(null);
            return;
        }
        
        final ScaledResolution sr = new ScaledResolution(mc);
        final float scaleWidth = NewGUI.INSTANCE.getScaleWidth();
        final float scaleHeight = NewGUI.INSTANCE.getScaleHeight();
        final float panelWidth = sr.getScaledWidth() * scaleWidth;
        final float panelHeight = sr.getScaledHeight() * scaleHeight;
        final float panelX = (sr.getScaledWidth() - panelWidth) / 2;
        final float panelY = (sr.getScaledHeight() - panelHeight) / 2;
        float searchX = panelX + 205;
        float searchW = panelWidth - 210;
        float searchH = panelHeight - 50;
        
        boolean hasSearchContent = searchElement.hasSearchContent();
        boolean isSearchTyping = searchElement.isTyping();
        boolean isSearchModuleTyping = searchElement.isAnyModuleTyping(categoryElements);
        boolean isSearchKeybindListening = searchElement.isAnyKeybindListening(categoryElements);
        boolean isCategoryKeybindListening = ce.isAnyKeybindListening();
        
        if (hasSearchContent || isSearchTyping || isSearchModuleTyping) {
            searchElement.handleTyping(typedChar, keyCode, searchX, panelY + 45, searchW, searchH, categoryElements);
        } else if (isCategoryKeybindListening) {
            ce.handleKeyTyped(typedChar, keyCode);
        } else if (isSearchKeybindListening) {
            searchElement.handleTyping(typedChar, keyCode, searchX, panelY + 45, searchW, searchH, categoryElements);
        } else {
            ce.handleKeyTyped(typedChar, keyCode);
        }
        
        try {
            super.keyTyped(typedChar, keyCode);
        } catch (Exception ignored) {
        }
    }

    private int getScrollHeight(ScaledResolution sr) {
        float panelHeight = sr.getScaledHeight() * NewGUI.INSTANCE.getScaleHeight();
        int baseHeight = (int)(panelHeight - 30);
        int categoryHeight = 37 * categoryElements.size() + 50;
        return Math.max(0, categoryHeight - baseHeight);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
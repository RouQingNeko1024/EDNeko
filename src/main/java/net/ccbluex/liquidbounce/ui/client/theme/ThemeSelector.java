/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.ui.client.theme;

import net.ccbluex.liquidbounce.features.module.modules.client.ThemeManager;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ThemeSelector extends GuiScreen {
    private static ThemeSelector instance;
    private int scrollOffset = 0;
    private final int visibleRows = 8;
    private final int itemHeight = 70;
    private final int itemsPerRow = 4;
    private final List<ThemeColor> themeColors = new ArrayList<>();

    public static ThemeSelector getInstance() {
        return instance == null ? instance = new ThemeSelector() : instance;
    }

    public ThemeSelector() {
        initThemeColors();
    }

    private void initThemeColors() {
        themeColors.add(new ThemeColor("MoonPurple", "moonpurple"));
        themeColors.add(new ThemeColor("Astolfo", "astolfo"));
        themeColors.add(new ThemeColor("Rainbow", "rainbow"));
        themeColors.add(new ThemeColor("Water", "water"));
        themeColors.add(new ThemeColor("Fire", "fire"));
        themeColors.add(new ThemeColor("Aqua", "aqua"));
        themeColors.add(new ThemeColor("Mint", "mint"));
        themeColors.add(new ThemeColor("FDP", "fdp"));
        themeColors.add(new ThemeColor("Magic", "magic"));
        themeColors.add(new ThemeColor("Tree", "tree"));
        themeColors.add(new ThemeColor("Sun", "sun"));
        themeColors.add(new ThemeColor("Flower", "flower"));
        themeColors.add(new ThemeColor("Loyoi", "loyoi"));
        themeColors.add(new ThemeColor("May", "may"));
        themeColors.add(new ThemeColor("Cero", "cero"));
        themeColors.add(new ThemeColor("Azure", "azure"));
        themeColors.add(new ThemeColor("Pumpkin", "pumpkin"));
        themeColors.add(new ThemeColor("Polarized", "polarized"));
        themeColors.add(new ThemeColor("Sundae", "sundae"));
        themeColors.add(new ThemeColor("Terminal", "terminal"));
        themeColors.add(new ThemeColor("Coral", "coral"));
        themeColors.add(new ThemeColor("Peony", "peony"));
        themeColors.add(new ThemeColor("VerGreen", "vergren"));
        themeColors.add(new ThemeColor("EveningSunshine", "eveningsunshine"));
        themeColors.add(new ThemeColor("LightOrange", "lightorange"));
        themeColors.add(new ThemeColor("Reef", "reef"));
        themeColors.add(new ThemeColor("Amin", "amin"));
        themeColors.add(new ThemeColor("MagicS", "magics"));
        themeColors.add(new ThemeColor("MangoPulp", "mangopulp"));
        themeColors.add(new ThemeColor("Aqualicious", "aqualicious"));
        themeColors.add(new ThemeColor("Stripe", "stripe"));
        themeColors.add(new ThemeColor("Shifter", "shifter"));
        themeColors.add(new ThemeColor("QuePal", "quepal"));
        themeColors.add(new ThemeColor("Orca", "orca"));
        themeColors.add(new ThemeColor("SublimeVivid", "sublimevivid"));
        themeColors.add(new ThemeColor("MoonAsteroid", "moonasteroid"));
        themeColors.add(new ThemeColor("SummerDog", "summerdog"));
        themeColors.add(new ThemeColor("PinkFlavour", "pinkflavour"));
        themeColors.add(new ThemeColor("SinCityRed", "sincityred"));
        themeColors.add(new ThemeColor("Timber", "timber"));
        themeColors.add(new ThemeColor("PinotNoir", "pinotnoir"));
        themeColors.add(new ThemeColor("DirtyFog", "dirtyfog"));
        themeColors.add(new ThemeColor("Piglet", "piglet"));
        themeColors.add(new ThemeColor("LittleLeaf", "littleleaf"));
        themeColors.add(new ThemeColor("Nelson", "nelson"));
        themeColors.add(new ThemeColor("TurquoiseFlow", "turquoiseflow"));
        themeColors.add(new ThemeColor("Purplin", "purplin"));
        themeColors.add(new ThemeColor("Martini", "martini"));
        themeColors.add(new ThemeColor("SoundCloud", "soundcloud"));
        themeColors.add(new ThemeColor("Inbox", "inbox"));
        themeColors.add(new ThemeColor("Amethyst", "amethyst"));
        themeColors.add(new ThemeColor("Blush", "blush"));
        themeColors.add(new ThemeColor("MochaRose", "mocharose"));
        themeColors.add(new ThemeColor("NeonCrimson", "neoncrimson"));
        themeColors.add(new ThemeColor("AcidGreen", "acidgreen"));
        themeColors.add(new ThemeColor("VaporWave", "vaporwave"));
        themeColors.add(new ThemeColor("Noir", "noir"));
        themeColors.add(new ThemeColor("Obsidian", "obsidian"));
        themeColors.add(new ThemeColor("Champagne", "champagne"));
        themeColors.add(new ThemeColor("RoseGold", "rosegold"));
        themeColors.add(new ThemeColor("Arctic", "arctic"));
        themeColors.add(new ThemeColor("Frost", "frost"));
        themeColors.add(new ThemeColor("Glacier", "glacier"));
        themeColors.add(new ThemeColor("Slate", "slate"));
        themeColors.add(new ThemeColor("Abyss", "abyss"));
        themeColors.add(new ThemeColor("BioLum", "biolum"));
        themeColors.add(new ThemeColor("EverGreen", "evergreen"));
        themeColors.add(new ThemeColor("Dusk", "dusk"));
        themeColors.add(new ThemeColor("Aurora", "aurora"));
        themeColors.add(new ThemeColor("RetroWave", "retrowave"));
        themeColors.add(new ThemeColor("Y2K", "y2k"));
        themeColors.add(new ThemeColor("DustyRose", "dustyrose"));
        themeColors.add(new ThemeColor("Sage", "sage"));
        themeColors.add(new ThemeColor("CloudBurst", "cloudburst"));
        themeColors.add(new ThemeColor("Monolith", "monolith"));
        themeColors.add(new ThemeColor("Bloodline", "bloodline"));
        themeColors.add(new ThemeColor("Lavender", "lavender"));
        themeColors.add(new ThemeColor("Butter", "butter"));
        themeColors.add(new ThemeColor("Gothic", "gothic"));
        themeColors.add(new ThemeColor("Phantom", "phantom"));
        themeColors.add(new ThemeColor("QuickSilver", "quicksilver"));
        themeColors.add(new ThemeColor("Mercury", "mercury"));
        themeColors.add(new ThemeColor("Tropical", "tropical"));
        themeColors.add(new ThemeColor("Mango", "mango"));
        themeColors.add(new ThemeColor("Rust", "rust"));
        themeColors.add(new ThemeColor("Concrete", "concrete"));
        themeColors.add(new ThemeColor("Nebula", "nebula"));
        themeColors.add(new ThemeColor("SuperNova", "supernova"));
        themeColors.add(new ThemeColor("Eclipse", "eclipse"));
        themeColors.add(new ThemeColor("Iceberg", "iceberg"));
        themeColors.add(new ThemeColor("Scarlet", "scarlet"));
        themeColors.add(new ThemeColor("CyberPink", "cyberpink"));
        themeColors.add(new ThemeColor("Matrix", "matrix"));
        themeColors.add(new ThemeColor("SolarGlare", "solarglare"));
        themeColors.add(new ThemeColor("Zywl", "zywl"));
        themeColors.add(new ThemeColor("DarkNight", "darknight"));
        themeColors.add(new ThemeColor("Emerald", "emerald"));
        themeColors.add(new ThemeColor("Sapphire", "sapphire"));
        themeColors.add(new ThemeColor("Ruby", "ruby"));
        themeColors.add(new ThemeColor("Topaz", "topaz"));
        themeColors.add(new ThemeColor("Amethyst2", "amethyst2"));
        themeColors.add(new ThemeColor("Jade", "jade"));
        themeColors.add(new ThemeColor("Opal", "opal"));
        themeColors.add(new ThemeColor("Garnet", "garnet"));
        themeColors.add(new ThemeColor("Turquoise", "turquoise"));
        themeColors.add(new ThemeColor("Citrine", "citrine"));
        themeColors.add(new ThemeColor("Peridot", "peridot"));
        themeColors.add(new ThemeColor("Aquamarine", "aquamarine"));
        themeColors.add(new ThemeColor("Tanzanite", "tanzanite"));
        themeColors.add(new ThemeColor("Morganite", "morganite"));
        themeColors.add(new ThemeColor("Kunzite", "kunzite"));
        themeColors.add(new ThemeColor("Spinel", "spinel"));
        themeColors.add(new ThemeColor("Zircon", "zircon"));
        themeColors.add(new ThemeColor("Tourmaline", "tourmaline"));
        themeColors.add(new ThemeColor("Alexandrite", "alexandrite"));
        themeColors.add(new ThemeColor("Iolite", "iolite"));
        themeColors.add(new ThemeColor("Chrysoberyl", "chrysoberyl"));
        themeColors.add(new ThemeColor("Beryl", "beryl"));
        themeColors.add(new ThemeColor("Corundum", "corundum"));
        themeColors.add(new ThemeColor("Quartz", "quartz"));
        themeColors.add(new ThemeColor("Moonstone", "moonstone"));
        themeColors.add(new ThemeColor("Sunstone", "sunstone"));
        themeColors.add(new ThemeColor("Labradorite", "labradorite"));
        themeColors.add(new ThemeColor("Spectrolite", "spectrolite"));
        themeColors.add(new ThemeColor("Apatite", "apatite"));
        themeColors.add(new ThemeColor("Fluorite", "fluorite"));
        themeColors.add(new ThemeColor("Calcite", "calcite"));
        themeColors.add(new ThemeColor("Sodalite", "sodalite"));
        themeColors.add(new ThemeColor("Lapis", "lapis"));
        themeColors.add(new ThemeColor("Malachite", "malachite"));
        themeColors.add(new ThemeColor("Azurite", "azurite"));
        themeColors.add(new ThemeColor("Rhodochrosite", "rhodochrosite"));
        themeColors.add(new ThemeColor("Rhodonite", "rhodonite"));
        themeColors.add(new ThemeColor("Serpentine", "serpentine"));
        themeColors.add(new ThemeColor("Howlite", "howlite"));
        themeColors.add(new ThemeColor("Onyx", "onyx"));
        themeColors.add(new ThemeColor("Jasper", "jasper"));
        themeColors.add(new ThemeColor("Agate", "agate"));
        themeColors.add(new ThemeColor("Basalt", "basalt"));
        themeColors.add(new ThemeColor("Granite", "granite"));
        themeColors.add(new ThemeColor("Marble", "marble"));
        themeColors.add(new ThemeColor("Sandstone", "sandstone"));
        themeColors.add(new ThemeColor("Ocean", "ocean"));
        themeColors.add(new ThemeColor("Sunset", "sunset"));
        themeColors.add(new ThemeColor("Forest", "forest"));
        themeColors.add(new ThemeColor("Midnight", "midnight"));
        themeColors.add(new ThemeColor("Cherry", "cherry"));
        themeColors.add(new ThemeColor("Minty", "minty"));
        themeColors.add(new ThemeColor("Thunder", "thunder"));
        themeColors.add(new ThemeColor("Honey", "honey"));
        themeColors.add(new ThemeColor("Ice", "ice"));
        themeColors.add(new ThemeColor("Velvet", "velvet"));
        themeColors.add(new ThemeColor("Plum", "plum"));
        themeColors.add(new ThemeColor("Storm", "storm"));
        themeColors.add(new ThemeColor("Peach", "peach"));
        themeColors.add(new ThemeColor("Denim", "denim"));
        themeColors.add(new ThemeColor("Wine", "wine"));
        themeColors.add(new ThemeColor("Sky", "sky"));
        themeColors.add(new ThemeColor("Amber", "amber"));
        themeColors.add(new ThemeColor("Fern", "fern"));
        themeColors.add(new ThemeColor("Iris", "iris"));
        themeColors.add(new ThemeColor("Crimson", "crimson"));
        themeColors.add(new ThemeColor("Indigo", "indigo"));
        themeColors.add(new ThemeColor("Magenta", "magenta"));
        themeColors.add(new ThemeColor("Violet", "violet"));
        themeColors.add(new ThemeColor("Chartreuse", "chartreuse"));
        themeColors.add(new ThemeColor("Fuchsia", "fuchsia"));
        themeColors.add(new ThemeColor("Lime", "lime"));
        themeColors.add(new ThemeColor("Navy", "navy"));
        themeColors.add(new ThemeColor("Teal", "teal"));
        themeColors.add(new ThemeColor("Cyan", "cyan"));
        themeColors.add(new ThemeColor("Bronze", "bronze"));
        themeColors.add(new ThemeColor("Pearl", "pearl"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        final ScaledResolution sr = new ScaledResolution(mc);
        final int screenWidth = sr.getScaledWidth();
        final int screenHeight = sr.getScaledHeight();

        final float panelWidth = Math.min(600, screenWidth - 80);
        final float panelHeight = Math.min(620, screenHeight - 80);
        final float panelX = (screenWidth - panelWidth) / 2;
        final float panelY = (screenHeight - panelHeight) / 2;

        int scrollWheel = Mouse.getDWheel();
        if (scrollWheel != 0) {
            int maxScroll = Math.max(0, (int) (themeColors.size() / itemsPerRow - visibleRows) * itemHeight);
            scrollOffset -= scrollWheel / 5;
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        }

        Color panelColor = ThemeManager.INSTANCE.getPanelColor();
        RenderUtils.INSTANCE.drawRoundedRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, panelColor.getRGB(), 10f, RenderUtils.RoundedCorners.ALL);

        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();

        Fonts.INSTANCE.getFontSF35().drawString("Select Theme Color", panelX + 20, panelY + 20, Color.WHITE.getRGB());

        float contentY = panelY + 55 - scrollOffset;
        float contentX = panelX + 15;
        float boxWidth = (panelWidth - 50) / itemsPerRow;
        float boxHeight = 55;
        float spacing = 10;

        int itemIndex = 0;
        for (int row = 0; row < Math.ceil((double) themeColors.size() / itemsPerRow); row++) {
            for (int col = 0; col < itemsPerRow; col++) {
                int index = row * itemsPerRow + col;
                if (index >= themeColors.size()) break;

                ThemeColor theme = themeColors.get(index);
                float boxX = contentX + col * (boxWidth + spacing);
                float boxY = contentY + row * (itemHeight + spacing);

                if (boxY + boxHeight < panelY + 50 || boxY > panelY + panelHeight - 15) {
                    itemIndex++;
                    continue;
                }

                Color borderColor = theme.getMode().equalsIgnoreCase(net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.INSTANCE.getClientColorMode())
                    ? new Color(255, 255, 255, 255) : new Color(80, 80, 80, 100);

                Color[] gradientColors = getThemePreviewColors(theme.getMode());
                drawGradientRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, gradientColors[0].getRGB(), gradientColors[1].getRGB());

                RenderUtils.INSTANCE.drawRoundedRect(boxX - 1, boxY - 1, boxX + boxWidth + 1, boxY + boxHeight + 1, borderColor.getRGB(), 6f, RenderUtils.RoundedCorners.ALL);

                float textY = boxY + boxHeight / 2 - 4;
                String displayName = theme.getDisplayName();
                if (Fonts.INSTANCE.getFont35().getStringWidth(displayName) > boxWidth - 10) {
                    displayName = displayName.substring(0, Math.min(3, displayName.length())) + "...";
                }
                Fonts.INSTANCE.getFont35().drawString(displayName, boxX + (boxWidth - Fonts.INSTANCE.getFont35().getStringWidth(displayName)) / 2, textY + 15, Color.WHITE.getRGB());

                itemIndex++;
            }
        }

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private Color[] getThemePreviewColors(String mode) {
        try {
            kotlin.Pair<Color, Color> colorPair = net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.INSTANCE.getThemeColorPair(mode);
            if (colorPair != null) {
                return new Color[] { colorPair.getFirst(), colorPair.getSecond() };
            }
            Color color = net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.INSTANCE.getColorForMode(mode, 0);
            return new Color[] { color, color };
        } catch (Exception e) {
            return new Color[] { Color.GRAY, Color.GRAY };
        }
    }

    private void drawGradientRect(float left, float top, float right, float bottom, int startColor, int endColor) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(right, top, 0.0).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, top, 0.0).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos(left, bottom, 0.0).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos(right, bottom, 0.0).color(f1, f2, f3, f).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            final ScaledResolution sr = new ScaledResolution(mc);
            final int screenWidth = sr.getScaledWidth();
            final int screenHeight = sr.getScaledHeight();

            final float panelWidth = Math.min(600, screenWidth - 80);
            final float panelHeight = Math.min(620, screenHeight - 80);
            final float panelX = (screenWidth - panelWidth) / 2;
            final float panelY = (screenHeight - panelHeight) / 2;

            if (mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY && mouseY <= panelY + panelHeight) {
                float contentY = panelY + 55 - scrollOffset;
                float contentX = panelX + 15;
                float boxWidth = (panelWidth - 50) / itemsPerRow;
                float boxHeight = 55;
                float spacing = 10;

                for (int row = 0; row < Math.ceil((double) themeColors.size() / itemsPerRow); row++) {
                    for (int col = 0; col < itemsPerRow; col++) {
                        int index = row * itemsPerRow + col;
                        if (index >= themeColors.size()) break;

                        ThemeColor theme = themeColors.get(index);
                        float boxX = contentX + col * (boxWidth + spacing);
                        float boxY = contentY + row * (itemHeight + spacing);

                        if (mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= boxY && mouseY <= boxY + boxHeight) {
                            net.ccbluex.liquidbounce.features.module.modules.client.ThemeManager.INSTANCE.setTheme(theme.getMode());
                            return;
                        }
                    }
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.displayGuiScreen(null);
            ThemeManager.INSTANCE.setState(false);
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public static class ThemeColor {
        private final String displayName;
        private final String mode;

        public ThemeColor(String displayName, String mode) {
            this.displayName = displayName;
            this.mode = mode;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getMode() {
            return mode;
        }
    }
}
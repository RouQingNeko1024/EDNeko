/*
 * edneko Hacked Client
 * A free open source mixin-based injection hacked client built on Liquidbounce legacy codebase.
 * https://github.com/lmx0721/edneko
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.augustus;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.config.BoolValue;
import net.ccbluex.liquidbounce.config.ColorValue;
import net.ccbluex.liquidbounce.config.FloatValue;
import net.ccbluex.liquidbounce.config.IntValue;
import net.ccbluex.liquidbounce.config.ListValue;
import net.ccbluex.liquidbounce.config.TextValue;
import net.ccbluex.liquidbounce.config.Value;
import net.ccbluex.liquidbounce.features.module.Category;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Vector2f;
import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Augustus-styled ClickGUI ported from NightSky to edneko's module/value APIs.
 */
public class AugustusClickGui extends GuiScreen {

    private enum GuiEvent {
        DRAW,
        CLICK,
        RELEASE
    }

    private static final class ColorPickerState {
        float pickerX;
        float pickerY;
        float hueY;
        float opacityY;
        boolean draggingColor;
        boolean draggingHue;
        boolean draggingOpacity;
    }

    public static float lastPosX = -1337F;
    public static float lastPosY = -1337F;
    public static float lastWidth = 500F;
    public static float lastHeight = 250F;

    private float posX;
    private float posY;
    private float width = 500F;
    private float height = 250F;
    private float dragX;
    private float dragY;
    private float moduleScroll;
    private float valueScroll;
    private float categoryLineX;
    private float targetCategoryLineX;
    private long lastAnimationTime = System.currentTimeMillis();

    private boolean dragging;
    private boolean resizing;
    private boolean waitingForKey;

    private Category selectedCategory = Category.COMBAT;
    private Module selectedModule;

    private FloatValue draggingFloat;
    private IntValue draggingInt;

    private final Map<TextValue, GuiTextField> textFields = new HashMap<>();
    private final Map<Value<?>, Float> animatedSliders = new HashMap<>();
    private final Map<ColorValue, ColorPickerState> colorPickerStates = new HashMap<>();

    public AugustusClickGui() {
        if (lastPosX == -1337F || lastPosY == -1337F) {
            posX = 150F;
            posY = 80F;
        } else {
            posX = lastPosX;
            posY = lastPosY;
            width = lastWidth;
            height = lastHeight;
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        if (lastPosX == -1337F || lastPosY == -1337F) {
            posX = this.width / 2F - this.width() / 2F;
            posY = this.height / 2F - this.height() / 2F;
            if (posX <= 0F) posX = 150F;
            if (posY <= 0F) posY = 80F;
        }
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateAnimations();
        handle(mouseX, mouseY, -1, GuiEvent.DRAW);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        handle(mouseX, mouseY, mouseButton, GuiEvent.CLICK);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        handle(mouseX, mouseY, state, GuiEvent.RELEASE);
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (waitingForKey && selectedModule != null) {
            selectedModule.setKeyBind(keyCode == Keyboard.KEY_ESCAPE ? Keyboard.KEY_NONE : keyCode);
            waitingForKey = false;
            return;
        }

        boolean textFocused = false;
        for (GuiTextField field : textFields.values()) {
            if (field.isFocused()) {
                field.textboxKeyTyped(typedChar, keyCode);
                textFocused = true;
            }
        }

        if (textFocused) {
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        lastPosX = posX;
        lastPosY = posY;
        lastWidth = width;
        lastHeight = height;
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void handle(int mouseX, int mouseY, int mouseButton, GuiEvent event) {
        if (event == GuiEvent.RELEASE) {
            dragging = false;
            resizing = false;
            draggingFloat = null;
            draggingInt = null;
            for (ColorPickerState state : colorPickerStates.values()) {
                state.draggingColor = false;
                state.draggingHue = false;
                state.draggingOpacity = false;
            }
        }

        if (event == GuiEvent.CLICK) {
            if (hovered(mouseX, mouseY, posX, posY, width, 18F) && mouseButton == 0) {
                dragging = true;
                dragX = mouseX - posX;
                dragY = mouseY - posY;
            }

            if (hovered(mouseX, mouseY, posX + width - 10F, posY + height - 10F, 12F, 12F) && mouseButton == 0) {
                resizing = true;
            }
        }

        if (event == GuiEvent.DRAW) {
            if (dragging && Mouse.isButtonDown(0)) {
                posX = mouseX - dragX;
                posY = mouseY - dragY;
            }
            if (resizing && Mouse.isButtonDown(0)) {
                width = Math.max(400F, mouseX - posX);
                height = Math.max(200F, mouseY - posY);
            }
        }

        drawMainWindow(mouseX, mouseY, mouseButton, event);
    }

    private void drawMainWindow(int mouseX, int mouseY, int mouseButton, GuiEvent event) {
        if (event == GuiEvent.DRAW) {
            drawRounded(posX, posY, width, height, 12F, new Color(25, 25, 25, 190).getRGB());
            drawRect(posX, posY, width, 18F, new Color(34, 34, 34, 230).getRGB());
            drawTitle("CLICKGUI", posX + 6F, posY + 6F, new Color(220, 220, 220).getRGB());
            drawRect(posX + 90F, posY, 2F, height, new Color(34, 34, 34, 230).getRGB());
            drawRect(posX + 90F, posY + 40F, width - 90F, 2F, new Color(34, 34, 34, 230).getRGB());
            drawRect(posX + width - 9F, posY + height - 9F, 8F, 8F, new Color(70, 70, 70, 160).getRGB());
        }

        drawCategories(mouseX, mouseY, mouseButton, event);
        drawModules(mouseX, mouseY, mouseButton, event);
        drawValues(mouseX, mouseY, mouseButton, event);
        updateDraggingValues(mouseX, mouseY);
    }

    private void drawCategories(int mouseX, int mouseY, int mouseButton, GuiEvent event) {
        float categoryX = posX + 105F;
        float clipX = posX + 92F;
        float clipY = posY + 18F;
        float clipW = width - 94F;
        float clipH = 24F;

        if (event == GuiEvent.DRAW) scissor(clipX, clipY, clipW, clipH, true);

        for (Category category : Category.values()) {
            if (!category.shouldShow()) continue;

            String name = category.name();
            float textWidth = normalWidth(name);
            boolean selected = category == selectedCategory;
            boolean inBounds = categoryX + textWidth >= clipX && categoryX <= clipX + clipW;
            boolean categoryHovered = inBounds && hovered(mouseX, mouseY, categoryX, posY + 20F, textWidth, normalHeight())
                    && hovered(mouseX, mouseY, clipX, clipY, clipW, clipH);

            if (event == GuiEvent.DRAW) {
                int color = selected ? accent().getRGB() : categoryHovered ? new Color(230, 230, 230).getRGB() : new Color(180, 180, 180).getRGB();
                drawNormal(name, categoryX, posY + 25F, color);
                if (selected) {
                    targetCategoryLineX = categoryX;
                    if (categoryLineX == 0F) categoryLineX = targetCategoryLineX;
                }
            } else if (event == GuiEvent.CLICK && categoryHovered && mouseButton == 0) {
                selectedCategory = category;
                selectedModule = null;
                moduleScroll = 0F;
                valueScroll = 0F;
                colorPickerStates.clear();
                targetCategoryLineX = categoryX;
            }

            categoryX += textWidth + 16F;
        }

        if (event == GuiEvent.DRAW && selectedCategory != null) {
            drawRect(categoryLineX, posY + 22F + normalHeight(), normalWidth(selectedCategory.name()), 2F, accent().getRGB());
        }

        if (event == GuiEvent.DRAW) scissor(0F, 0F, 0F, 0F, false);
    }

    private void drawModules(int mouseX, int mouseY, int mouseButton, GuiEvent event) {
        if (selectedCategory == null) return;

        float listX = posX;
        float listY = posY + 18F;
        float listW = 90F;
        float listH = height - 18F;
        float contentHeight = modulesInCategory(selectedCategory).size() * (normalHeight() + 5F);
        moduleScroll = clampScroll(moduleScroll, contentHeight, listH - 8F);

        if (event == GuiEvent.DRAW && hovered(mouseX, mouseY, listX, listY, listW, listH)) {
            moduleScroll = clampScroll(moduleScroll + Mouse.getDWheel() / 10F, contentHeight, listH - 8F);
        }

        if (event == GuiEvent.DRAW) scissor(listX, listY, listW, listH, true);

        float moduleY = posY + 26F + moduleScroll;
        for (Module module : modulesInCategory(selectedCategory)) {
            boolean inBounds = moduleY >= listY && moduleY + normalHeight() <= listY + listH;
            boolean moduleHovered = inBounds && hovered(mouseX, mouseY, posX + 8F, moduleY - 3F, 74F, normalHeight() + 4F);
            if (event == GuiEvent.DRAW) {
                float drawX = posX + 8F;
                if (module == selectedModule) {
                    drawNormal(">", drawX, moduleY, module.getState() ? accent().getRGB() : new Color(210, 210, 210).getRGB());
                    drawX += normalWidth("> ");
                }
                int color = module.getState() ? accent().getRGB() : moduleHovered ? Color.WHITE.getRGB() : new Color(200, 200, 200).getRGB();
                drawNormal(module.getName(), drawX, moduleY, color);
            } else if (event == GuiEvent.CLICK && moduleHovered) {
                if (mouseButton == 0) {
                    module.toggle();
                } else if (mouseButton == 1) {
                    selectedModule = module;
                    valueScroll = 0F;
                    colorPickerStates.clear();
                }
            }
            moduleY += normalHeight() + 5F;
        }

        if (event == GuiEvent.DRAW) scissor(0F, 0F, 0F, 0F, false);
    }

    private void drawValues(int mouseX, int mouseY, int mouseButton, GuiEvent event) {
        if (selectedModule == null) return;

        float valuesX = posX + 100F;
        float currentY = posY + 48F;

        if (event == GuiEvent.DRAW) {
            drawNormal(selectedModule.getName() + ":", valuesX, currentY, new Color(210, 210, 210).getRGB());
        }
        currentY += normalHeight() + 5F;

        String keyName = selectedModule.getKeyBind() == Keyboard.KEY_NONE ? "None" : Keyboard.getKeyName(selectedModule.getKeyBind());
        boolean keyHovered = hovered(mouseX, mouseY, valuesX, currentY, normalWidth("Key: " + keyName), normalHeight());
        boolean hideHovered = hovered(mouseX, mouseY, valuesX + 72F, currentY, normalWidth("Hide: " + selectedModule.isHidden()), normalHeight());

        if (event == GuiEvent.DRAW) {
            drawNormal(waitingForKey ? "Key: ..." : "Key: " + keyName, valuesX, currentY, waitingForKey ? accent().getRGB() : new Color(150, 150, 150).getRGB());
            drawNormal("Hide: ", valuesX + 72F, currentY, new Color(150, 150, 150).getRGB());
            drawNormal(String.valueOf(selectedModule.isHidden()), valuesX + 72F + normalWidth("Hide: "), currentY, selectedModule.isHidden() ? new Color(0, 180, 0).getRGB() : new Color(180, 0, 0).getRGB());
        } else if (event == GuiEvent.CLICK && mouseButton == 0) {
            if (keyHovered) waitingForKey = true;
            if (hideHovered) selectedModule.setHidden(!selectedModule.isHidden());
        }

        currentY += normalHeight() + 10F;

        if (event == GuiEvent.DRAW && hovered(mouseX, mouseY, posX + 92F, posY + 42F, width - 94F, height - 44F)) {
            valueScroll = clampScroll(valueScroll + Mouse.getDWheel() / 10F, valuesContentHeight(), height - (currentY - posY) - 2F);
        }

        if (event == GuiEvent.DRAW) scissor(posX + 92F, currentY - 2F, width - 94F, height - (currentY - posY) - 2F, true);

        float clipY = currentY - 2F;
        float clipH = height - (currentY - posY) - 2F;
        currentY += valueScroll;
        for (Value<?> value : selectedModule.getValues()) {
            if (!value.shouldRender()) continue;
            float oldY = currentY;
            currentY = drawValue(value, currentY, mouseX, mouseY, mouseButton, event, clipY, clipH);
            if (currentY == oldY) currentY += normalHeight() + 5F;
        }

        valueScroll = clampScroll(valueScroll, valuesContentHeight(), clipH);

        if (event == GuiEvent.DRAW) scissor(0F, 0F, 0F, 0F, false);
    }

    private float drawValue(Value<?> value, float y, int mouseX, int mouseY, int mouseButton, GuiEvent event, float clipY, float clipH) {
        float height = valueHeight(value);
        boolean inBounds = y + height >= clipY && y <= clipY + clipH;
        if (!inBounds) return y + height;
        if (event != GuiEvent.DRAW && !hovered(mouseX, mouseY, posX + 92F, clipY, width - 94F, clipH)) return y + height;

        float x = posX + 100F;
        if (value instanceof BoolValue) {
            BoolValue boolValue = (BoolValue) value;
            String text = value.getName() + ": " + boolValue.get();
            if (event == GuiEvent.DRAW) {
                drawNormal(value.getName() + ": ", x, y, new Color(200, 200, 200).getRGB());
                drawNormal(String.valueOf(boolValue.get()), x + normalWidth(value.getName() + ": "), y, boolValue.get() ? new Color(0, 180, 0).getRGB() : new Color(180, 0, 0).getRGB());
            } else if (event == GuiEvent.CLICK && mouseButton == 0 && hovered(mouseX, mouseY, x, y, normalWidth(text), normalHeight())) {
                boolValue.set(!boolValue.get(), true);
            }
            return y + normalHeight() + 5F;
        }

        if (value instanceof TextValue) {
            TextValue textValue = (TextValue) value;
            float labelWidth = normalWidth(value.getName() + ": ");
            GuiTextField field = textFields.computeIfAbsent(textValue, key -> {
                GuiTextField textField = new GuiTextField(0, mc.fontRendererObj, 0, 0, 100, 14);
                textField.setMaxStringLength(256);
                textField.setText(textValue.get());
                textField.setEnableBackgroundDrawing(false);
                return textField;
            });
            field.xPosition = (int) (x + labelWidth + 4F);
            field.yPosition = (int) (y - 2F);
            field.width = 110;
            field.height = 14;

            if (event == GuiEvent.DRAW) {
                drawNormal(value.getName() + ": ", x, y, new Color(200, 200, 200).getRGB());
                drawRect(field.xPosition - 2F, field.yPosition - 1F, field.width + 4F, field.height + 2F, new Color(34, 34, 34).getRGB());
                drawRect(field.xPosition - 1F, field.yPosition, field.width + 2F, field.height, new Color(45, 45, 45, 220).getRGB());
                field.updateCursorCounter();
                if (field.getText().isEmpty() && !field.isFocused()) {
                    drawNormal("Enter text...", field.xPosition + 3F, y, new Color(120, 120, 120).getRGB());
                } else {
                    field.drawTextBox();
                }
                textValue.set(field.getText(), true);
            } else if (event == GuiEvent.CLICK) {
                field.mouseClicked(mouseX, mouseY, mouseButton);
            }
            return y + normalHeight() + 6F;
        }

        if (value instanceof FloatValue) {
            FloatValue floatValue = (FloatValue) value;
            float sliderX = x + normalWidth(value.getName() + ": ") + 4F;
            drawSlider(value, y, sliderX, 98F, floatValue.get(), floatValue.getMinimum(), floatValue.getMaximum(), event);
            if (event == GuiEvent.CLICK && mouseButton == 0 && hovered(mouseX, mouseY, sliderX, y - 4F, 98F, 10F)) {
                draggingFloat = floatValue;
                updateFloat(mouseX, sliderX, 98F, floatValue);
            }
            return y + normalHeight() + 6F;
        }

        if (value instanceof IntValue) {
            IntValue intValue = (IntValue) value;
            float sliderX = x + normalWidth(value.getName() + ": ") + 4F;
            drawSlider(value, y, sliderX, 98F, intValue.get(), intValue.getMinimum(), intValue.getMaximum(), event);
            if (event == GuiEvent.CLICK && mouseButton == 0 && hovered(mouseX, mouseY, sliderX, y - 4F, 98F, 10F)) {
                draggingInt = intValue;
                updateInt(mouseX, sliderX, 98F, intValue);
            }
            return y + normalHeight() + 6F;
        }

        if (value instanceof ListValue) {
            ListValue listValue = (ListValue) value;
            float modeX = x + normalWidth(value.getName() + ": ");
            float modeY = y;
            if (event == GuiEvent.DRAW) {
                drawNormal(value.getName() + ": ", x, y, new Color(200, 200, 200).getRGB());
            }
            String[] values = listValue.getValues();
            for (int i = 0; i < values.length; i++) {
                String mode = values[i];
                if (modeX > posX + width - 55F) {
                    modeX = x + normalWidth(value.getName() + ": ");
                    modeY += normalHeight() + 2F;
                }
                boolean selected = mode.equalsIgnoreCase(listValue.get());
                if (event == GuiEvent.DRAW) {
                    drawNormal(mode, modeX, modeY, selected ? accent().getRGB() : new Color(200, 200, 200).getRGB());
                    if (i < values.length - 1) drawNormal(", ", modeX + normalWidth(mode), modeY, new Color(200, 200, 200).getRGB());
                } else if (event == GuiEvent.CLICK && mouseButton == 0 && hovered(mouseX, mouseY, modeX, modeY, normalWidth(mode), normalHeight())) {
                    listValue.set(mode, true);
                }
                modeX += normalWidth(mode + (i < values.length - 1 ? ", " : ""));
            }
            return modeY + normalHeight() + 6F;
        }

        if (value instanceof ColorValue) {
            return drawColorValue((ColorValue) value, y, mouseX, mouseY, mouseButton, event);
        }

        if (event == GuiEvent.DRAW) {
            drawNormal(value.getName() + ": " + value.get(), x, y, new Color(200, 200, 200).getRGB());
        }
        return y + normalHeight() + 5F;
    }

    private float drawColorValue(ColorValue colorValue, float y, int mouseX, int mouseY, int mouseButton, GuiEvent event) {
        float x = posX + 100F;
        ColorPickerState state = colorPickerStates.computeIfAbsent(colorValue, key -> new ColorPickerState());
        float previewX = x + normalWidth(colorValue.getName() + ": ") + 4F;
        state.pickerX = x;
        state.pickerY = y + normalHeight() + 4F;
        state.hueY = state.pickerY + 54F;
        state.opacityY = state.hueY + 9F;

        if (event == GuiEvent.DRAW) {
            Color selected = colorValue.selectedColor();
            drawNormal(colorValue.getName() + ": ", x, y, new Color(200, 200, 200).getRGB());
            drawRect(previewX, y - 1F, 22F, 10F, selected.getRGB());

            if (colorValue.getShowPicker()) {
                Color hueColor = Color.getHSBColor(colorValue.getHueSliderY(), 1F, 1F);
                drawRect(state.pickerX, state.pickerY, 100F, 50F, hueColor.getRGB());
                for (int sx = 0; sx < 100; sx++) {
                    float saturation = sx / 100F;
                    drawRect(state.pickerX + sx, state.pickerY, 1F, 50F, new Color(255, 255, 255, (int) (255F * (1F - saturation))).getRGB());
                }
                for (int sy = 0; sy < 50; sy++) {
                    float brightness = 1F - sy / 50F;
                    drawRect(state.pickerX, state.pickerY + sy, 100F, 1F, new Color(0, 0, 0, (int) (255F * (1F - brightness))).getRGB());
                }
                for (int sx = 0; sx < 100; sx++) {
                    drawRect(state.pickerX + sx, state.hueY, 1F, 5F, Color.getHSBColor(sx / 100F, 1F, 1F).getRGB());
                }
                for (int sx = 0; sx < 100; sx++) {
                    int alpha = MathHelper.clamp_int((int) (sx / 99F * 255F), 0, 255);
                    Color alphaColor = new Color(selected.getRed(), selected.getGreen(), selected.getBlue(), alpha);
                    drawRect(state.pickerX + sx, state.opacityY, 1F, 5F, alphaColor.getRGB());
                }

                Vector2f pos = colorValue.getColorPickerPos();
                float markerX = state.pickerX + pos.x * 100F;
                float markerY = state.pickerY + pos.y * 50F;
                drawRect(markerX - 2F, markerY - 2F, 4F, 4F, Color.WHITE.getRGB());
                float hueMarker = state.pickerX + colorValue.getHueSliderY() * 100F;
                drawRect(hueMarker - 1F, state.hueY - 1F, 2F, 7F, Color.WHITE.getRGB());
                float opacityMarker = state.pickerX + colorValue.getOpacitySliderY() * 100F;
                drawRect(opacityMarker - 1F, state.opacityY - 1F, 2F, 7F, Color.WHITE.getRGB());
            }
        } else if (event == GuiEvent.CLICK && mouseButton == 0) {
            if (hovered(mouseX, mouseY, previewX, y - 1F, 22F, 10F)) {
                colorValue.setShowPicker(!colorValue.getShowPicker());
            } else if (colorValue.getShowPicker()) {
                if (hovered(mouseX, mouseY, state.pickerX, state.pickerY, 100F, 50F)) {
                    state.draggingColor = true;
                    updateColor(mouseX, mouseY, colorValue, state);
                } else if (hovered(mouseX, mouseY, state.pickerX, state.hueY, 100F, 5F)) {
                    state.draggingHue = true;
                    updateHue(mouseX, colorValue, state);
                } else if (hovered(mouseX, mouseY, state.pickerX, state.opacityY, 100F, 5F)) {
                    state.draggingOpacity = true;
                    updateOpacity(mouseX, colorValue, state);
                }
            }
        }
        return y + valueHeight(colorValue);
    }

    private void drawSlider(Value<?> value, float y, float sliderX, float sliderWidth, double current, double min, double max, GuiEvent event) {
        if (event != GuiEvent.DRAW) return;

        float x = posX + 100F;
        drawNormal(value.getName() + ": ", x, y, new Color(200, 200, 200).getRGB());
        double target = Math.max(0D, Math.min(sliderWidth, (current - min) / (max - min) * sliderWidth));
        float animated = animatedSliders.getOrDefault(value, (float) target);
        animated += ((float) target - animated) * 0.35F;
        animatedSliders.put(value, animated);

        drawRect(sliderX, y - 3F, sliderWidth + 1F, 10F, new Color(34, 34, 34).getRGB());
        drawRect(sliderX + 1F, y - 2F, Math.max(0F, animated - 1F), 8F, accent().getRGB());
        String valueText = current == Math.rint(current) ? String.valueOf((int) current) : String.valueOf(Math.round(current * 100D) / 100D);
        drawNormal(valueText, sliderX + sliderWidth / 2F - normalWidth(valueText) / 2F, y - 1F, new Color(230, 230, 230).getRGB());
    }

    private void updateDraggingValues(int mouseX, int mouseY) {
        if (draggingFloat != null && Mouse.isButtonDown(0)) {
            float sliderX = posX + 100F + normalWidth(draggingFloat.getName() + ": ") + 4F;
            updateFloat(mouseX, sliderX, 98F, draggingFloat);
        }
        if (draggingInt != null && Mouse.isButtonDown(0)) {
            float sliderX = posX + 100F + normalWidth(draggingInt.getName() + ": ") + 4F;
            updateInt(mouseX, sliderX, 98F, draggingInt);
        }
        for (Map.Entry<ColorValue, ColorPickerState> entry : colorPickerStates.entrySet()) {
            ColorValue value = entry.getKey();
            ColorPickerState state = entry.getValue();
            if (state.draggingColor && Mouse.isButtonDown(0)) {
                updateColor(mouseX, mouseY, value, state);
            } else if (state.draggingHue && Mouse.isButtonDown(0)) {
                updateHue(mouseX, value, state);
            } else if (state.draggingOpacity && Mouse.isButtonDown(0)) {
                updateOpacity(mouseX, value, state);
            }
        }
    }

    private void updateFloat(int mouseX, float sliderX, float sliderWidth, FloatValue value) {
        float min = value.getMinimum();
        float max = value.getMaximum();
        double raw = (mouseX - sliderX) * (max - min) / sliderWidth + min;
        double stepped = Math.round(raw * 100D) / 100D;
        value.set((float) MathHelper.clamp_double(stepped, min, max), true);
    }

    private void updateInt(int mouseX, float sliderX, float sliderWidth, IntValue value) {
        int min = value.getMinimum();
        int max = value.getMaximum();
        double raw = (mouseX - sliderX) * (max - min) / sliderWidth + min;
        value.set((int) MathHelper.clamp_double(Math.round(raw), min, max), true);
    }

    private void updateHue(int mouseX, ColorValue value, ColorPickerState state) {
        value.setHueSliderY(MathHelper.clamp_float((mouseX - state.pickerX) / 100F, 0F, 1F));
        updateColorFromPicker(value);
    }

    private void updateOpacity(int mouseX, ColorValue value, ColorPickerState state) {
        value.setOpacitySliderY(MathHelper.clamp_float((mouseX - state.pickerX) / 100F, 0F, 1F));
        updateColorFromPicker(value);
    }

    private void updateColor(int mouseX, int mouseY, ColorValue value, ColorPickerState state) {
        float saturation = MathHelper.clamp_float((mouseX - state.pickerX) / 100F, 0F, 1F);
        float brightnessPicker = MathHelper.clamp_float((mouseY - state.pickerY) / 50F, 0F, 1F);
        value.getColorPickerPos().set(saturation, brightnessPicker);
        updateColorFromPicker(value);
    }

    private void updateColorFromPicker(ColorValue value) {
        Vector2f picker = value.getColorPickerPos();
        Color color = new Color(Color.HSBtoRGB(value.getHueSliderY(), picker.x, 1F - picker.y));
        value.set(new Color(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp_int((int) (value.getOpacitySliderY() * 255F), 0, 255)), true);
    }

    private List<Module> modulesInCategory(Category category) {
        return LiquidBounce.INSTANCE.getModuleManager().getModules().stream()
                .filter(module -> module.getCategory() == category)
                .collect(Collectors.toList());
    }

    private float valuesContentHeight() {
        if (selectedModule == null) return 0F;

        float contentHeight = 0F;
        for (Value<?> value : selectedModule.getValues()) {
            if (value.shouldRender()) contentHeight += valueHeight(value);
        }
        return contentHeight;
    }

    private float valueHeight(Value<?> value) {
        if (value instanceof ColorValue) {
            ColorValue colorValue = (ColorValue) value;
            return normalHeight() + (colorValue.getShowPicker() ? 77F : 5F);
        }
        if (value instanceof ListValue) {
            ListValue listValue = (ListValue) value;
            float modeX = posX + 100F + normalWidth(value.getName() + ": ");
            float rows = 1F;
            for (String mode : listValue.getValues()) {
                if (modeX > posX + width - 55F) {
                    modeX = posX + 100F + normalWidth(value.getName() + ": ");
                    rows += 1F;
                }
                modeX += normalWidth(mode + ", ");
            }
            return rows * normalHeight() + Math.max(0F, rows - 1F) * 2F + 6F;
        }
        return normalHeight() + 6F;
    }

    private float clampScroll(float scroll, float contentHeight, float visibleHeight) {
        float min = Math.min(0F, visibleHeight - contentHeight);
        return MathHelper.clamp_float(scroll, min, 0F);
    }

    private void updateAnimations() {
        long now = System.currentTimeMillis();
        float delta = Math.max(0F, Math.min(0.1F, (now - lastAnimationTime) / 1000F));
        lastAnimationTime = now;
        categoryLineX += (targetCategoryLineX - categoryLineX) * Math.min(1F, delta * 12F);
    }

    private Color accent() {
        return new Color(81, 149, 219);
    }

    private float width() {
        return width;
    }

    private float height() {
        return height;
    }

    private boolean hovered(int mouseX, int mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private void drawTitle(String text, float x, float y, int color) {
        Fonts.INSTANCE.getFont40().drawString(text, x, y, color);
    }

    private void drawNormal(String text, float x, float y, int color) {
        Fonts.INSTANCE.getFont35().drawString(text, x, y, color);
    }

    private float normalWidth(String text) {
        return Fonts.INSTANCE.getFont35().getStringWidth(text);
    }

    private float normalHeight() {
        return Fonts.INSTANCE.getFont35().getHeight();
    }

    private void drawRect(float x, float y, float w, float h, int color) {
        RenderUtils.INSTANCE.drawRect(x, y, x + w, y + h, color);
    }

    private void drawRounded(float x, float y, float w, float h, float radius, int color) {
        RenderUtils.INSTANCE.drawRoundedRect(x, y, x + w, y + h, color, radius, RenderUtils.RoundedCorners.ALL);
    }

    private void scissor(float x, float y, float w, float h, boolean start) {
        if (!start) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int factor = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * factor), (int) ((sr.getScaledHeight() - y - h) * factor), (int) (w * factor), (int) (h * factor));
    }
}

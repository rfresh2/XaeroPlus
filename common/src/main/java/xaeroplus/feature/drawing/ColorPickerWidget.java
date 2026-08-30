package xaeroplus.feature.drawing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import xaeroplus.util.Color;
import xaeroplus.util.ColorHelper;

import java.util.Objects;
import java.util.function.Consumer;

// with inspiration from future client's lovely design
public class ColorPickerWidget extends AbstractWidget {
    private static final float HEIGHT_PROPORTION = 8.0f / 9.0f;
    private static final int ENTRY_LABEL_HEIGHT = 9;
    private static final int ENTRY_HEIGHT = 18;
    private static final int ENTRY_GAP = 1;
    private static final int ENTRY_SECTION_GAP = 3;
    private static final int PRESET_COLUMNS = 7;
    private static final int PRESET_ROWS = 2;
    private static final int PRESET_HEIGHT = 10;
    private static final int PRESET_GAP = 1;
    private static final int PRESET_SECTION_GAP = 3;
    private static final int PRESET_GRID_HEIGHT = PRESET_ROWS * PRESET_HEIGHT + (PRESET_ROWS - 1) * PRESET_GAP;
    private static final float PADDING_PROPORTION = 1.0f / 90.0f;
    private static final float SELECTOR_GAP_PROPORTION = 1.0f / 45.0f;
    private static final float HUE_BAR_PROPORTION = 1.0f / 15.0f;
    private static final float ALPHA_BAR_PROPORTION = 1.0f / 15.0f;
    private static final float CHECKER_PROPORTION = 1.0f / 30.0f;
    private static final float COLOR_SELECTOR_OUTER_PROPORTION = 1.0f / 18.0f;
    private static final float COLOR_SELECTOR_INNER_PROPORTION = 1.0f / 30.0f;
    private static final float BAR_SELECTOR_THICKNESS_PROPORTION = 1.0f / 30.0f;
    private static final float SELECTOR_EXTENSION_PROPORTION = 1.0f / 90.0f;

    private final Consumer<Color> onColorChanged;
    private final int pickerHeight;
    private final int padding;
    private final int selectorGap;
    private final int hueBarHeight;
    private final int alphaBarWidth;
    private final int checkerSize;
    private final int colorSelectorOuterSize;
    private final int colorSelectorInnerSize;
    private final int barSelectorThickness;
    private final int selectorExtension;
    private final EditBox[] colorEntryBoxes;
    private float hue;
    private float saturation;
    private float brightness;
    private int colorAlpha;
    private boolean updatingEntries;
    private Selector activeSelector = Selector.NONE;

    public ColorPickerWidget(final int x, final int y, final int size, final Color color, final Consumer<Color> onColorChanged) {
        super(
            x,
            y,
            size,
            Math.round(size * HEIGHT_PROPORTION)
                + PRESET_SECTION_GAP
                + PRESET_GRID_HEIGHT
                + ENTRY_SECTION_GAP
                + ENTRY_LABEL_HEIGHT
                + ENTRY_HEIGHT,
            Component.translatable("xaeroplus.gui.world_map.draw_color")
        );
        this.onColorChanged = Objects.requireNonNull(onColorChanged);
        pickerHeight = Math.round(size * HEIGHT_PROPORTION);
        padding = scaled(size, PADDING_PROPORTION);
        selectorGap = scaled(size, SELECTOR_GAP_PROPORTION);
        hueBarHeight = scaled(size, HUE_BAR_PROPORTION);
        alphaBarWidth = scaled(size, ALPHA_BAR_PROPORTION);
        checkerSize = scaled(size, CHECKER_PROPORTION);
        colorSelectorOuterSize = Math.max(3, scaled(size, COLOR_SELECTOR_OUTER_PROPORTION));
        colorSelectorInnerSize = Math.min(colorSelectorOuterSize - 2, scaled(size, COLOR_SELECTOR_INNER_PROPORTION));
        barSelectorThickness = Math.max(3, scaled(size, BAR_SELECTOR_THICKNESS_PROPORTION));
        selectorExtension = scaled(size, SELECTOR_EXTENSION_PROPORTION);
        colorEntryBoxes = createColorEntryBoxes();
        setColor(color, false);
    }

    private EditBox[] createColorEntryBoxes() {
        var entries = new EditBox[ColorChannel.values().length];
        var availableWidth = width - ENTRY_GAP * (entries.length - 1);
        var entryY = presetY() + PRESET_GRID_HEIGHT + ENTRY_SECTION_GAP + ENTRY_LABEL_HEIGHT;
        var consumedWidth = 0;
        for (var channel : ColorChannel.values()) {
            var remainingEntries = entries.length - channel.ordinal();
            var entryWidth = (availableWidth - consumedWidth) / remainingEntries;
            var entryX = getX() + consumedWidth + ENTRY_GAP * channel.ordinal();
            var entry = new EditBox(
                Minecraft.getInstance().font,
                entryX,
                entryY,
                entryWidth,
                ENTRY_HEIGHT,
                Component.literal(channel.label)
            );
            entry.setMaxLength(3);
            entry.setFilter(ColorPickerWidget::isValidEntryText);
            entry.setResponder(value -> updateColorFromEntry(channel, value));
            entries[channel.ordinal()] = entry;
            consumedWidth += entryWidth;
        }
        return entries;
    }

    private static boolean isValidEntryText(final String value) {
        if (value.isEmpty()) return true;
        if (!value.chars().allMatch(Character::isDigit)) return false;
        try {
            return Integer.parseInt(value) <= 255;
        } catch (final NumberFormatException ignored) {
            return false;
        }
    }

    private void updateColorFromEntry(final ColorChannel channel, final String value) {
        if (updatingEntries || value.isEmpty()) return;
        var channelValue = Integer.parseInt(value);
        var color = getColor();
        setColor(switch (channel) {
            case RED -> new Color(channelValue, color.g(), color.b(), color.a());
            case GREEN -> new Color(color.r(), channelValue, color.b(), color.a());
            case BLUE -> new Color(color.r(), color.g(), channelValue, color.a());
            case ALPHA -> new Color(color.r(), color.g(), color.b(), channelValue);
        }, true);
    }

    private static int scaled(final int size, final float proportion) {
        return Math.max(1, Math.round(size * proportion));
    }

    public Color getColor() {
        var rgb = java.awt.Color.getHSBColor(hue, saturation, brightness);
        return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), colorAlpha);
    }

    void setColor(final Color color, final boolean notify) {
        Objects.requireNonNull(color);
        var hsb = java.awt.Color.RGBtoHSB(color.r(), color.g(), color.b(), null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
        colorAlpha = color.a();
        updateEntryValues(color);
        if (notify) {
            onColorChanged.accept(getColor());
        }
    }

    private void updateEntryValues(final Color color) {
        updatingEntries = true;
        colorEntryBoxes[ColorChannel.RED.ordinal()].setValue(Integer.toString(color.r()));
        colorEntryBoxes[ColorChannel.GREEN.ordinal()].setValue(Integer.toString(color.g()));
        colorEntryBoxes[ColorChannel.BLUE.ordinal()].setValue(Integer.toString(color.b()));
        colorEntryBoxes[ColorChannel.ALPHA.ordinal()].setValue(Integer.toString(color.a()));
        updatingEntries = false;
    }

    @Override
    protected void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, ColorHelper.getColor(0, 0, 0, 255));
        renderColorGradient(guiGraphics);
        renderHueBar(guiGraphics);
        renderAlphaBar(guiGraphics);
        renderSelectors(guiGraphics);
        renderColorPresets(guiGraphics, mouseX, mouseY);
        renderColorEntries(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderColorPresets(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        var currentColor = getColor();
        for (var i = 0; i < ColorHelper.HighlightColor.VALUES.length; i++) {
            var preset = ColorHelper.HighlightColor.VALUES[i];
            var presetColor = preset.getColor();
            var x = presetX(i);
            var y = presetY(i);
            var presetWidth = presetWidth(i);
            guiGraphics.fill(
                x,
                y,
                x + presetWidth,
                y + PRESET_HEIGHT,
                ColorHelper.getColorWithAlpha(presetColor, 255)
            );
            var selected = currentColor.r() == ColorHelper.getIntR(presetColor)
                && currentColor.g() == ColorHelper.getIntG(presetColor)
                && currentColor.b() == ColorHelper.getIntB(presetColor);
            if (selected || contains(mouseX, mouseY, x, y, presetWidth, PRESET_HEIGHT)) {
                guiGraphics.renderOutline(x, y, presetWidth, PRESET_HEIGHT, ColorHelper.getColor(255, 255, 255, 255));
            } else if (preset == ColorHelper.HighlightColor.BLACK) {
                guiGraphics.renderOutline(x, y, presetWidth, PRESET_HEIGHT, ColorHelper.getColor(128, 128, 128, 255));
            }
        }
    }

    private void renderColorEntries(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        var font = Minecraft.getInstance().font;
        var labelY = presetY() + PRESET_GRID_HEIGHT + ENTRY_SECTION_GAP;
        for (var channel : ColorChannel.values()) {
            var entry = colorEntryBoxes[channel.ordinal()];
            guiGraphics.drawCenteredString(
                font,
                channel.label,
                entry.getX() + entry.getWidth() / 2,
                labelY,
                channel.labelColor
            );
            entry.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    void renderColorGradient(final GuiGraphics guiGraphics) {
        var gradientWidth = gradientWidth();
        for (var xOffset = 0; xOffset < gradientWidth; xOffset++) {
            var columnSaturation = (float) xOffset / (gradientWidth - 1);
            var topColor = java.awt.Color.HSBtoRGB(hue, columnSaturation, 1.0f);
            guiGraphics.fillGradient(
                gradientX() + xOffset,
                gradientY(),
                gradientX() + xOffset + 1,
                gradientY() + gradientHeight(),
                topColor,
                ColorHelper.getColor(0, 0, 0, 255)
            );
        }
    }

    void renderHueBar(final GuiGraphics guiGraphics) {
        var hueX = gradientX();
        var hueY = hueY();
        var hueWidth = gradientWidth();
        for (var xOffset = 0; xOffset < hueWidth; xOffset++) {
            var columnHue = (float) xOffset / (hueWidth - 1);
            guiGraphics.fill(
                hueX + xOffset,
                hueY,
                hueX + xOffset + 1,
                hueY + hueBarHeight,
                java.awt.Color.HSBtoRGB(columnHue, 1.0f, 1.0f)
            );
        }
    }

    void renderAlphaBar(final GuiGraphics guiGraphics) {
        var alphaX = alphaX();
        var alphaY = gradientY();
        var alphaHeight = gradientHeight();
        for (var yOffset = 0; yOffset < alphaHeight; yOffset += checkerSize) {
            for (var xOffset = 0; xOffset < alphaBarWidth; xOffset += checkerSize) {
                var checkerColor = ((xOffset / checkerSize) + (yOffset / checkerSize)) % 2 == 0
                    ? ColorHelper.getColor(255, 255, 255, 255)
                    : ColorHelper.getColor(119, 119, 199, 255);
                guiGraphics.fill(
                    alphaX + xOffset,
                    alphaY + yOffset,
                    Math.min(alphaX + xOffset + checkerSize, alphaX + alphaBarWidth),
                    Math.min(alphaY + yOffset + checkerSize, alphaY + alphaHeight),
                    checkerColor
                );
            }
        }

        var rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness) & ColorHelper.getColor(255, 255, 255, 0);
        guiGraphics.fillGradient(
            alphaX,
            alphaY,
            alphaX + alphaBarWidth,
            alphaY + alphaHeight,
            ColorHelper.getColorWithAlpha(rgb, 255),
            rgb
        );
    }

    void renderSelectors(final GuiGraphics guiGraphics) {
        var selectedX = gradientX() + Math.round(saturation * (gradientWidth() - 1));
        var selectedY = gradientY() + Math.round((1.0f - brightness) * (gradientHeight() - 1));
        guiGraphics.renderOutline(
            selectedX - colorSelectorOuterSize / 2,
            selectedY - colorSelectorOuterSize / 2,
            colorSelectorOuterSize,
            colorSelectorOuterSize,
            ColorHelper.getColor(0, 0, 0, 255)
        );
        guiGraphics.renderOutline(
            selectedX - colorSelectorInnerSize / 2,
            selectedY - colorSelectorInnerSize / 2,
            colorSelectorInnerSize,
            colorSelectorInnerSize,
            ColorHelper.getColor(255, 255, 255, 255)
        );

        var hueSelectorX = gradientX() + Math.round(hue * (gradientWidth() - 1));
        guiGraphics.renderOutline(
            hueSelectorX - barSelectorThickness / 2,
            hueY() - selectorExtension,
            barSelectorThickness,
            hueBarHeight + selectorExtension * 2,
            ColorHelper.getColor(255, 255, 255, 255)
        );
        guiGraphics.vLine(
            hueSelectorX,
            hueY() - selectorExtension,
            hueY() + hueBarHeight + selectorExtension - 1,
            ColorHelper.getColor(0, 0, 0, 255)
        );

        var alphaSelectorY = gradientY() + Math.round((1.0f - colorAlpha / 255.0f) * (gradientHeight() - 1));
        guiGraphics.renderOutline(
            alphaX() - selectorExtension,
            alphaSelectorY - barSelectorThickness / 2,
            alphaBarWidth + selectorExtension * 2,
            barSelectorThickness,
            ColorHelper.getColor(255, 255, 255, 255)
        );
        guiGraphics.hLine(
            alphaX() - selectorExtension,
            alphaX() + alphaBarWidth + selectorExtension - 1,
            alphaSelectorY,
            ColorHelper.getColor(0, 0, 0, 255)
        );
    }

    @Override
    public void onClick(final double mouseX, final double mouseY) {
        var preset = presetAt(mouseX, mouseY);
        if (preset != null) {
            activeSelector = Selector.NONE;
            clearEntryFocus();
            var presetColor = preset.getColor();
            var currentColor = getColor();
            setColor(new Color(
                ColorHelper.getIntR(presetColor),
                ColorHelper.getIntG(presetColor),
                ColorHelper.getIntB(presetColor),
                currentColor.a()
            ), true);
            return;
        }
        for (int i = 0; i < colorEntryBoxes.length; i++) {
            final var entry = colorEntryBoxes[i];
            if (entry.isMouseOver(mouseX, mouseY)) {
                activeSelector = Selector.NONE;
                focusEntry(entry);
                entry.mouseClicked(mouseX, mouseY, 0);
                entry.setCursorPosition(entry.getValue().length());
                entry.setHighlightPos(0);
                return;
            }
        }
        clearEntryFocus();
        activeSelector = selectorAt(mouseX, mouseY);
        updateSelection(mouseX, mouseY);
    }

    private ColorHelper.HighlightColor presetAt(final double mouseX, final double mouseY) {
        for (var i = 0; i < ColorHelper.HighlightColor.VALUES.length; i++) {
            if (contains(mouseX, mouseY, presetX(i), presetY(i), presetWidth(i), PRESET_HEIGHT)) {
                return ColorHelper.HighlightColor.VALUES[i];
            }
        }
        return null;
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        var inWidget = super.clicked(mouseX, mouseY);
        if (!inWidget) clearEntryFocus();
        return inWidget;
    }

    @Override
    protected void onDrag(final double mouseX, final double mouseY, final double dragX, final double dragY) {
        updateSelection(mouseX, mouseY);
    }

    @Override
    public void onRelease(final double mouseX, final double mouseY) {
        activeSelector = Selector.NONE;
    }

    Selector selectorAt(final double mouseX, final double mouseY) {
        if (contains(mouseX, mouseY, gradientX(), gradientY(), gradientWidth(), gradientHeight())) {
            return Selector.COLOR;
        }
        if (contains(mouseX, mouseY, gradientX(), hueY(), gradientWidth(), hueBarHeight)) {
            return Selector.HUE;
        }
        if (contains(mouseX, mouseY, alphaX(), gradientY(), alphaBarWidth, gradientHeight())) {
            return Selector.ALPHA;
        }
        return Selector.NONE;
    }

    boolean contains(final double mouseX, final double mouseY, final int x, final int y, final int areaWidth, final int areaHeight) {
        return mouseX >= x && mouseX < x + areaWidth && mouseY >= y && mouseY < y + areaHeight;
    }

    void updateSelection(final double mouseX, final double mouseY) {
        switch (activeSelector) {
            case COLOR -> {
                saturation = normalized(mouseX, gradientX(), gradientWidth());
                brightness = 1.0f - normalized(mouseY, gradientY(), gradientHeight());
            }
            case HUE -> hue = normalized(mouseX, gradientX(), gradientWidth());
            case ALPHA -> colorAlpha = Math.round((1.0f - normalized(mouseY, gradientY(), gradientHeight())) * 255.0f);
        }
        if (activeSelector != Selector.NONE) {
            var color = getColor();
            updateEntryValues(color);
            onColorChanged.accept(color);
        }
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        var focusedEntry = focusedEntry();
        if (focusedEntry == null) return false;
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            var direction = Screen.hasShiftDown() ? -1 : 1;
            var nextIndex = Math.floorMod(indexOf(focusedEntry) + direction, colorEntryBoxes.length);
            focusEntry(colorEntryBoxes[nextIndex]);
            return true;
        }
        return focusedEntry.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char codePoint, final int modifiers) {
        var focusedEntry = focusedEntry();
        return focusedEntry != null && focusedEntry.charTyped(codePoint, modifiers);
    }

    private void focusEntry(final EditBox selectedEntry) {
        for (int i = 0; i < colorEntryBoxes.length; i++) {
            final var entry = colorEntryBoxes[i];
            if (entry == selectedEntry) {
                entry.setFocused(true);
            } else {
                entry.setFocused(false);
                entry.setHighlightPos(entry.getValue().length());
            }
        }
    }

    private void clearEntryFocus() {
        for (int i = 0; i < colorEntryBoxes.length; i++) {
            final var entry = colorEntryBoxes[i];
            if (entry.getValue().isEmpty()) updateEntryValues(getColor());
            entry.setFocused(false);
            entry.setHighlightPos(entry.getValue().length());
        }
    }

    private EditBox focusedEntry() {
        for (int i = 0; i < colorEntryBoxes.length; i++) {
            final var entry = colorEntryBoxes[i];
            if (entry.isFocused()) return entry;
        }
        return null;
    }

    private int indexOf(final EditBox selectedEntry) {
        for (var i = 0; i < colorEntryBoxes.length; i++) {
            var entry = colorEntryBoxes[i];
            if (entry == selectedEntry) return i;
        }
        return 0;
    }

    float normalized(final double position, final int start, final int length) {
        return Mth.clamp((float) ((position - start) / (length - 1)), 0.0f, 1.0f);
    }

    int gradientX() {
        return getX() + padding;
    }

    int gradientY() {
        return getY() + padding;
    }

    int gradientWidth() {
        return width - (padding * 2) - selectorGap - alphaBarWidth;
    }

    int gradientHeight() {
        return pickerHeight - (padding * 2) - selectorGap - hueBarHeight;
    }

    int hueY() {
        return gradientY() + gradientHeight() + selectorGap;
    }

    int alphaX() {
        return gradientX() + gradientWidth() + selectorGap;
    }

    int presetX(final int index) {
        var column = index % PRESET_COLUMNS;
        var availableWidth = width - (PRESET_COLUMNS - 1) * PRESET_GAP;
        var baseWidth = availableWidth / PRESET_COLUMNS;
        var widerPresetCount = availableWidth % PRESET_COLUMNS;
        return getX() + column * (baseWidth + PRESET_GAP) + Math.min(column, widerPresetCount);
    }

    int presetY() {
        return getY() + pickerHeight + PRESET_SECTION_GAP;
    }

    int presetY(final int index) {
        return presetY() + (index / PRESET_COLUMNS) * (PRESET_HEIGHT + PRESET_GAP);
    }

    int presetWidth(final int index) {
        var column = index % PRESET_COLUMNS;
        var availableWidth = width - (PRESET_COLUMNS - 1) * PRESET_GAP;
        var baseWidth = availableWidth / PRESET_COLUMNS;
        return baseWidth + (column < availableWidth % PRESET_COLUMNS ? 1 : 0);
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {
        var color = getColor();
        narrationElementOutput.add(
            NarratedElementType.TITLE,
            Component.translatable("xaeroplus.gui.world_map.draw_color")
                .append(Component.literal(" RGBA " + color.r() + ", " + color.g() + ", " + color.b() + ", " + color.a()))
        );
    }

    enum Selector {
        NONE,
        COLOR,
        HUE,
        ALPHA
    }

    enum ColorChannel {
        RED("R", ColorHelper.getColor(255, 0, 0, 255)),
        GREEN("G", ColorHelper.getColor(0, 255, 0, 255)),
        BLUE("B", ColorHelper.getColor(0, 0, 255, 255)),
        ALPHA("A", ColorHelper.getColor(255, 255, 255, 255));

        private final String label;
        private final int labelColor;

        ColorChannel(final String label, final int labelColor) {
            this.label = label;
            this.labelColor = labelColor;
        }
    }
}

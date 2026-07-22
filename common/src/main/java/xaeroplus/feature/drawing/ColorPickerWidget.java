package xaeroplus.feature.drawing;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import xaeroplus.util.Color;
import xaeroplus.util.ColorHelper;

import java.util.Objects;
import java.util.function.Consumer;

// with inspiration from future client's lovely design
public class ColorPickerWidget extends AbstractWidget {
    private static final float HEIGHT_PROPORTION = 8.0f / 9.0f;
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
    private final int padding;
    private final int selectorGap;
    private final int hueBarHeight;
    private final int alphaBarWidth;
    private final int checkerSize;
    private final int colorSelectorOuterSize;
    private final int colorSelectorInnerSize;
    private final int barSelectorThickness;
    private final int selectorExtension;
    private float hue;
    private float saturation;
    private float brightness;
    private int colorAlpha;
    private Selector activeSelector = Selector.NONE;

    public ColorPickerWidget(final int x, final int y, final int size, final Color color, final Consumer<Color> onColorChanged) {
        super(
            x,
            y,
            size,
            Math.round(size * HEIGHT_PROPORTION),
            Component.translatable("xaeroplus.gui.world_map.draw_color")
        );
        this.onColorChanged = Objects.requireNonNull(onColorChanged);
        padding = scaled(size, PADDING_PROPORTION);
        selectorGap = scaled(size, SELECTOR_GAP_PROPORTION);
        hueBarHeight = scaled(size, HUE_BAR_PROPORTION);
        alphaBarWidth = scaled(size, ALPHA_BAR_PROPORTION);
        checkerSize = scaled(size, CHECKER_PROPORTION);
        colorSelectorOuterSize = Math.max(3, scaled(size, COLOR_SELECTOR_OUTER_PROPORTION));
        colorSelectorInnerSize = Math.min(colorSelectorOuterSize - 2, scaled(size, COLOR_SELECTOR_INNER_PROPORTION));
        barSelectorThickness = Math.max(3, scaled(size, BAR_SELECTOR_THICKNESS_PROPORTION));
        selectorExtension = scaled(size, SELECTOR_EXTENSION_PROPORTION);
        setColor(color, false);
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
        if (notify) {
            onColorChanged.accept(getColor());
        }
    }

    @Override
    protected void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, ColorHelper.getColor(0, 0, 0, 255));
        renderColorGradient(guiGraphics);
        renderHueBar(guiGraphics);
        renderAlphaBar(guiGraphics);
        renderSelectors(guiGraphics);
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
        renderOutline(guiGraphics,
            selectedX - colorSelectorOuterSize / 2,
            selectedY - colorSelectorOuterSize / 2,
            colorSelectorOuterSize,
            colorSelectorOuterSize,
            ColorHelper.getColor(0, 0, 0, 255)
        );
        renderOutline(guiGraphics,
            selectedX - colorSelectorInnerSize / 2,
            selectedY - colorSelectorInnerSize / 2,
            colorSelectorInnerSize,
            colorSelectorInnerSize,
            ColorHelper.getColor(255, 255, 255, 255)
        );

        var hueSelectorX = gradientX() + Math.round(hue * (gradientWidth() - 1));
        renderOutline(guiGraphics,
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
        renderOutline(guiGraphics,
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

    private static void renderOutline(
        final GuiGraphics guiGraphics,
        final int x,
        final int y,
        final int width,
        final int height,
        final int color
    ) {
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y + 1, x + 1, y + height - 1, color);
        guiGraphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    @Override
    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        activeSelector = selectorAt(event.x(), event.y());
        updateSelection(event.x(), event.y());
    }

    @Override
    protected void onDrag(final MouseButtonEvent event, final double dragX, final double dragY) {
        updateSelection(event.x(), event.y());
    }

    @Override
    public void onRelease(final MouseButtonEvent event) {
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
            onColorChanged.accept(getColor());
        }
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
        return height - (padding * 2) - selectorGap - hueBarHeight;
    }

    int hueY() {
        return gradientY() + gradientHeight() + selectorGap;
    }

    int alphaX() {
        return gradientX() + gradientWidth() + selectorGap;
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
}

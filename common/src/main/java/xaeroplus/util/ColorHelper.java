package xaeroplus.util;

import xaeroplus.settings.TranslatableSettingEnum;

public class ColorHelper {
    public static int getColor(final int r, final int g, final int b, final int a) {
        return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }

    public static int getColorWithAlpha(final int colorInt, final int a) {
        return ((a & 255) << 24) | (colorInt & 0x00FFFFFF);
    }

    public static float[] getColorRGBA(final int colorInt) {
        return new float[] {
            getR(colorInt),
            getG(colorInt),
            getB(colorInt),
            getA(colorInt)
        };
    }

    public static float getR(final int color) {
        return ((color >> 16) & 255) / 255.0f;
    }

    public static float getG(final int color) {
        return ((color >> 8) & 255) / 255.0f;
    }

    public static float getB(final int color) {
        return (color & 255) / 255.0f;
    }

    public static float getA(final int color) {
        return ((color >> 24) & 255) / 255.0f;
    }

    public enum HighlightColor implements TranslatableSettingEnum {
        RED(ColorHelper.getColor(255, 0, 0, 100), "gui.xaero_red"),
        GREEN(ColorHelper.getColor(0, 255, 0, 100), "gui.xaero_green"),
        BLUE(ColorHelper.getColor(0, 0, 255, 100), "gui.xaero_blue"),
        YELLOW(ColorHelper.getColor(255, 255, 0, 100), "gui.xaero_yellow"),
        CYAN(ColorHelper.getColor(0, 255, 255, 100), "gui.xaero_aqua"),
        MAGENTA(ColorHelper.getColor(255, 0, 255, 100), "gui.xaero_purple"),
        WHITE(ColorHelper.getColor(255, 255, 255, 100), "gui.xaero_white"),
        BLACK(ColorHelper.getColor(0, 0, 0, 100), "gui.xaero_black");

        private final int color;
        private final String translationKey;
        HighlightColor(final int color, final String translationKey) {
            this.color = color;
            this.translationKey = translationKey;
        }

        public int getColor() {
            return color;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }

    public enum WaystoneColor implements TranslatableSettingEnum {

        BLACK(0, "gui.xaero_black"),
        DARK_BLUE(1, "gui.xaero_dark_blue"),
        DARK_GREEN(2, "gui.xaero_dark_green"),
        DARK_AQUA(3, "gui.xaero_dark_aqua"),
        DARK_RED(4, "gui.xaero_dark_red"),
        DARK_PURPLE(5, "gui.xaero_dark_purple"),
        GOLD(6, "gui.xaero_gold"),
        GRAY(7, "gui.xaero_gray"),
        DARK_GRAY(8, "gui.xaero_dark_gray"),
        BLUE(9, "gui.xaero_blue"),
        GREEN(10, "gui.xaero_green"),
        AQUA(11, "gui.xaero_aqua"),
        RED(12, "gui.xaero_red"),
        LIGHT_PURPLE(13, "gui.xaero_purple"),
        YELLOW(14, "gui.xaero_yellow"),
        WHITE(15, "gui.xaero_white"),
        RANDOM(16, "xaeroplus.gui.random");

        private final int colorIndex;
        private final String translationKey;

        WaystoneColor(final int colorIndex, final String translationKey) {
            this.colorIndex = colorIndex;
            this.translationKey = translationKey;
        }

        public int getColorIndex() {
            return colorIndex;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }
}

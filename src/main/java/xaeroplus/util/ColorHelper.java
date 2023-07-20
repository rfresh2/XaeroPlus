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
                ((colorInt >> 16) & 255) / 255.0f,
                ((colorInt >> 8) & 255) / 255.0f,
                (colorInt & 255) / 255.0f,
                ((colorInt >> 24) & 255) / 255.0f };
    }

    public static int getColorAlpha(final int colorInt) {
        return (colorInt >> 24) & 255;
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
}

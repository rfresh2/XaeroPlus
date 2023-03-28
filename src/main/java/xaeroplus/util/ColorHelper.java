package xaeroplus.util;

public class ColorHelper {
    public static int getColor(final int r, final int g, final int b, final int a) {
        return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }

    public static int getColorWithAlpha(final int colorInt, final int a) {
        return ((a & 255) << 24) | (colorInt & 0x00FFFFFF);
    }

    public static int getColorAlpha(final int colorInt) {
        return (colorInt >> 24) & 255;
    }

    public enum HighlightColor {
        RED(ColorHelper.getColor(255, 0, 0, 100)), GREEN(ColorHelper.getColor(0, 255, 0, 100)),
        BLUE(ColorHelper.getColor(0, 0, 255, 100)), YELLOW(ColorHelper.getColor(255, 255, 0, 100)),
        CYAN(ColorHelper.getColor(0, 255, 255, 100)), MAGENTA(ColorHelper.getColor(255, 0, 255, 100)),
        WHITE(ColorHelper.getColor(255, 255, 255, 100)), BLACK(ColorHelper.getColor(0, 0, 0, 100));

        private final int color;
        HighlightColor(final int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }
}

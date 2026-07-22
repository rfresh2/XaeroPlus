package xaeroplus.util;

public record Color(int r, int g, int b, int a) {
    public Color(int colorInt) {
        this(ColorHelper.getIntR(colorInt), ColorHelper.getIntG(colorInt), ColorHelper.getIntB(colorInt), ColorHelper.getIntA(colorInt));
    }

    public int getInt() {
        return ColorHelper.getColor(r, g, b, a);
    }

    public Color withAlpha(int a) {
        return new Color(r, g, b, a);
    }

}

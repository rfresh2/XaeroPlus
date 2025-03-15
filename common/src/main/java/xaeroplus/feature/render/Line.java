package xaeroplus.feature.render;

public record Line(int x1, int z1, int x2, int z2) {
    public double length() {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(z2 - z1, 2));
    }
}


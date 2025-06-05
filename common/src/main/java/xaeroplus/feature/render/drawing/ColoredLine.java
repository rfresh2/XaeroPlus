package xaeroplus.feature.render.drawing;

public record ColoredLine(int x1, int z1, int x2, int z2, int color) {
    public double length() {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(z2 - z1, 2));
    }

    public ColoredLine extrapolateToWorldBorder() {
        return extrapolateToMaxCoord(30_000_000);
    }

    public ColoredLine extrapolateToMaxCoord(int coord) {
        // extrapolate the line and find its intersections with the world border rect
        int dx = x2() - x1();
        if (dx == 0) { // vertical line
            return new ColoredLine(x1(), -coord, x2(), coord, color());
        }
        int dz = z2() - z1();
        if (dz == 0) { // horizontal line
            return new ColoredLine(-coord, z1(), coord, z2(), color());
        }
        double slope = (double) dz / dx;
        double intercept = z1() - slope * x1();
        double x1 = -coord;
        double z1 = slope * x1 + intercept;
        if (z1 < -coord) {
            z1 = -coord;
            x1 = (z1 - intercept) / slope;
        } else if (z1 > coord) {
            z1 = coord;
            x1 = (z1 - intercept) / slope;
        }
        double x2 = coord;
        double z2 = slope * x2 + intercept;
        if (z2 < -coord) {
            z2 = -coord;
            x2 = (z2 - intercept) / slope;
        } else if (z2 > coord) {
            z2 = coord;
            x2 = (z2 - intercept) / slope;
        }
        return new ColoredLine((int) Math.round(x1), (int) Math.round(z1), (int) Math.round(x2), (int) Math.round(z2), color());
    }
}


package xaeroplus.feature.render.line;

import net.minecraft.util.Mth;

public record Line(int x1, int z1, int x2, int z2) {
    public double length() {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(z2 - z1, 2));
    }

    public Line extrapolateToWorldBorder() {
        return extrapolateToMaxCoord(30_000_000);
    }

    public Line extrapolateToMaxCoord(int coord) {
        // extrapolate the line and find its intersections with the world border rect
        int dx = x2() - x1();
        if (dx == 0) { // vertical line
            return new Line(x1(), -coord, x2(), coord);
        }
        int dz = z2() - z1();
        if (dz == 0) { // horizontal line
            return new Line(-coord, z1(), coord, z2());
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
        return new Line((int) Math.round(x1), (int) Math.round(z1), (int) Math.round(x2), (int) Math.round(z2));
    }

    // https://arxiv.org/pdf/1908.01350
    public boolean lineClip(int rxMin, int rxMax, int rzMin, int rzMax) {
        if (!(x1() < rxMin && x2() < rxMin) && !(x1() > rxMax && x2() > rxMax)) {
            if (!(z1() < rzMin && z2() < rzMin) && !(z1() > rzMax && z2() > rzMax)) {
                double xx1 = x1();
                double xx2 = x2();
                double zz1 = z1();
                double zz2 = z2();
                double m = (zz2 - zz1) / (xx2 - xx1);
                double n = 1.0 / m;

                if (xx1 < rxMin) {
                    xx1 = rxMin;
                    zz1 = m * (rxMin - x1()) + z1();
                } else if (xx1 > rxMax) {
                    xx1 = rxMax;
                    zz1 = m * (rxMax - x1()) + z1();
                }
                if (zz1 < rzMin) {
                    zz1 = rzMin;
                    xx1 = n * (rzMin - z1()) + x1();
                } else if (zz1 > rzMax) {
                    zz1 = rzMax;
                    xx1 = n * (rzMax - z1()) + x1();
                }

                if (xx2 < rxMin) {
                    xx2 = rxMin;
                    zz2 = m * (rxMin - x1()) + z1();
                } else if (xx2 > rxMax) {
                    xx2 = rxMax;
                    zz2 = m * (rxMax - x1()) + z1();
                }
                if (zz2 < rzMin) {
                    zz2 = rzMin;
                    xx2 = n * (rzMin - z1()) + x1();
                } else if (zz2 > rzMax) {
                    zz2 = rzMax;
                    xx2 = n * (rzMax - z1()) + x1();
                }

                if (!(xx1 < rxMin && xx2 < rxMin) && !(xx1 > rxMax && xx2 > rxMax)) {
                    return true;
                }
            }
        }
        return false;
    }

    public double angle() {
        int dx = x2() - x1();
        int dz = z2() - z1();
        double angleRadians = Math.atan2(dz, dx);
        double degrees = Math.toDegrees(angleRadians) - 90;
        return Mth.wrapDegrees(degrees);
    }
}


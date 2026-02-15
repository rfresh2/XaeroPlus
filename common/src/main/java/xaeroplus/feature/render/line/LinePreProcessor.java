package xaeroplus.feature.render.line;

import net.minecraft.util.Mth;
import xaeroplus.util.ChunkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LinePreProcessor {
    public record WindowBounds(int minX, int maxX, int minZ, int maxZ) {
        public boolean contains(final Line line) {
            return line.lineClip(minX, maxX, minZ, maxZ);
        }
    }

    public static WindowBounds windowBounds(final int windowX, final int windowZ, final int windowSize) {
        int minX = ChunkUtils.regionCoordToCoord(windowX - windowSize);
        int minZ = ChunkUtils.regionCoordToCoord(windowZ - windowSize);
        int maxX = ChunkUtils.regionCoordToCoord(windowX + windowSize);
        int maxZ = ChunkUtils.regionCoordToCoord(windowZ + windowSize);
        return new WindowBounds(minX, maxX, minZ, maxZ);
    }

    public static List<Line> clippedSplitOriented(final Line line, final WindowBounds windowBounds) {
        if (!windowBounds.contains(line)) return Collections.emptyList();
        var splitLines = ensureLength(line);
        if (splitLines.isEmpty()) {
            return List.of(ensureOrientation(line));
        }
        splitLines.replaceAll(LinePreProcessor::ensureOrientation);
        return splitLines;
    }

    public static Line ensureOrientation(Line line) {
        // z1 must always be less than or equal to z2
        if (line.z1() > line.z2()) {
            return new Line(line.x2(), line.z2(), line.x1(), line.z1());
        } else {
            return line;
        }
    }

    public static final int MAX_LINE_LENGTH = 500_000;

    // ensure no line is longer than MAX_LINE_LENGTH
    // if a line is longer than MAX_LINE_LENGTH, split it into multiple lines each with a maximum length of MAX_LINE_LENGTH
    // else return an empty list indicating no change is needed
    // this is needed as otherwise we start hitting floating point precision issues as the line is rendered
    public static List<Line> ensureLength(Line line) {
        double len = line.length();
        if (len <= MAX_LINE_LENGTH) {
            return Collections.emptyList();
        }
        List<Line> lines = new ArrayList<>((int) (len / MAX_LINE_LENGTH) + 1);
        int dx = line.x2() - line.x1();
        int dz = line.z2() - line.z1();
        if (dx == 0) { // vertical line
            int x = line.x1();
            int minZ = Math.min(line.z1(), line.z2());
            int maxZ = Math.max(line.z1(), line.z2());
            int z1 = minZ;
            while (z1 < maxZ) {
                int z2 = Mth.clamp(z1 + MAX_LINE_LENGTH, minZ, maxZ);
                Line l = new Line(x, z1, x, z2);
                lines.add(l);
                z1 = z2;
            }
        } else if (dz == 0) { // horizontal line
            int z = line.z1();
            int minX = Math.min(line.x1(), line.x2());
            int maxX = Math.max(line.x1(), line.x2());
            int x1 = minX;
            while (x1 < maxX) {
                int x2 = Mth.clamp(x1 + MAX_LINE_LENGTH, minX, maxX);
                Line l = new Line(x1, z, x2, z);
                lines.add(l);
                x1 = x2;
            }
        } else {
            double slope = (double) dz / dx;
            double intercept = line.z1() - slope * line.x1();
            // if positive slope, we are increasing z faster than x
            // and therefore need to increment z by 500k
            boolean positiveSlope = Math.abs(slope) > 1;
            if (positiveSlope) {
                int minZ = Math.min(line.z1(), line.z2());
                int maxZ = Math.max(line.z1(), line.z2());
                int z1 = minZ;
                while (z1 < maxZ) {
                    double x1 = (z1 - intercept) / slope;
                    int z2 = Mth.clamp(z1 + MAX_LINE_LENGTH, minZ, maxZ);
                    double x2 = (z2 - intercept) / slope;
                    int xx1 = (int) Math.round(x1);
                    int xx2 = (int) Math.round(x2);
                    Line l = new Line(xx1, z1, xx2, z2);
                    lines.add(l);
                    z1 = z2;
                }
            } else {
                int minX = Math.min(line.x1(), line.x2());
                int maxX = Math.max(line.x1(), line.x2());
                int x1 = minX;
                while (x1 < maxX) {
                    double z1 = slope * x1 + intercept;
                    int x2 = Mth.clamp(x1 + MAX_LINE_LENGTH, minX, maxX);
                    double z2 = slope * x2 + intercept;
                    int zz1 = (int) Math.round(z1);
                    int zz2 = (int) Math.round(z2);
                    Line l = new Line(x1, zz1, x2, zz2);
                    lines.add(l);
                    x1 = x2;
                }
            }
        }
        return lines;
    }
}

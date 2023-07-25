package xaeroplus.util.highlights;

import java.util.Objects;

public class RegionRenderPos {
    public final int leafRegionX;
    public final int leafRegionZ;
    public final int level;

    public RegionRenderPos(final int leafRegionX, final int leafRegionZ, final int level) {
        this.leafRegionX = leafRegionX;
        this.leafRegionZ = leafRegionZ;
        this.level = level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegionRenderPos that = (RegionRenderPos) o;
        return leafRegionX == that.leafRegionX && leafRegionZ == that.leafRegionZ && level == that.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(leafRegionX, leafRegionZ, level);
    }
}

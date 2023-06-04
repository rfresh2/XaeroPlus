package xaeroplus.util;

import java.util.Objects;

public class HighlightAtChunkPos {
    public final int x;
    public final int z;

    public HighlightAtChunkPos(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final HighlightAtChunkPos that = (HighlightAtChunkPos) o;
        return x == that.x && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}

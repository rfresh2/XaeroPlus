package xaeroplus.util;

import net.minecraft.core.BlockPos;

public class MutableBlockPos extends BlockPos {
    public MutableBlockPos(final int i, final int j, final int k) {
        super(i, j, k);
    }

    public void mutateX(final int x) {
        this.setX(x);
    }

    public void mutateY(final int y) {
        this.setY(y);
    }

    public void mutateZ(final int z) {
        this.setZ(z);
    }

    public void setPos(final int x, final int y, final int z) {
        this.mutateX(x);
        this.mutateY(y);
        this.mutateZ(z);
    }
}

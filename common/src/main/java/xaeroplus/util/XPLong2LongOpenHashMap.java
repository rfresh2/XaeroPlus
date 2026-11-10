package xaeroplus.util;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

public class XPLong2LongOpenHashMap extends Long2LongOpenHashMap {

    public XPLong2LongOpenHashMap(final int expected, final float f) {
        super(expected, f);
    }

    public XPLong2LongOpenHashMap(final int expected) {
        super(expected);
    }

    public XPLong2LongOpenHashMap() {
        super();
    }

    public int n() {
        return this.n;
    }

    public float f() {
        return this.f;
    }

}

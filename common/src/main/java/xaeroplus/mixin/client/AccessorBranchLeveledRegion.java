package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;

@Mixin(value = BranchLeveledRegion.class, remap = false)
public interface AccessorBranchLeveledRegion {
    @Invoker("putLeaf")
    void invokePutLeaf(int X, int Z, MapRegion leaf);

    @Invoker("get")
    LeveledRegion<?> invokeGet(int leveledX, int leveledZ, int level);

    @Invoker("remove")
    boolean invokeRemove(int leveledX, int leveledZ, int level);
}

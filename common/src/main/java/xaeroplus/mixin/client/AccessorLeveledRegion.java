package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import xaero.map.region.LeveledRegion;

/**
 * Exposes load state field on {@link LeveledRegion} for hot-reload.
 *
 * <p>Load state machine: 0 = not loaded, 1 = loading, 2 = loaded, 3 = cleaned (GPU-only).</p>
 */
@Mixin(value = LeveledRegion.class, remap = false)
public interface AccessorLeveledRegion {
    @Accessor
    byte getLoadState();

    @Accessor
    void setLoadState(byte state);

    @Invoker
    boolean invokeIsBeingWritten();
}

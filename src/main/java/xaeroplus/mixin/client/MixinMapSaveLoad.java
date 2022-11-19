package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import xaero.map.WorldMap;
import xaero.map.file.MapSaveLoad;

import java.nio.file.Path;
import java.util.Objects;

@Mixin(value = MapSaveLoad.class, remap = false)
public abstract class MixinMapSaveLoad {

    /**
     * @author rfresh2
     * @reason Use DIM0 as overworld dimension directory instead of "null"
     */
    @Overwrite
    public Path getOldFolder(String oldUnfixedMainId, String dim) {
        if (oldUnfixedMainId == null) {
            return null;
        }
        String dimIdFixed = Objects.equals(dim, "null") ? "0" : dim;
        return WorldMap.saveFolder.toPath().resolve(oldUnfixedMainId + "_" + dimIdFixed);
    }
}

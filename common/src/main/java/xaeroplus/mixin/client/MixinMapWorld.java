package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

@Mixin(value = MapWorld.class, remap = false)
public abstract class MixinMapWorld {
    @Unique MapDimension xaeroPlus$currentDimensionRef = null;

    @Shadow public abstract MapDimension getDimension(final ResourceKey<Level> dimId);
    @Shadow private ResourceKey<Level> currentDimensionId;

    @WrapOperation(method = "switchToFutureUnsynced",
        at = @At(
            value = "FIELD",
            opcode = Opcodes.PUTFIELD,
            target = "Lxaero/map/world/MapWorld;currentDimensionId:Lnet/minecraft/resources/ResourceKey;"),
        remap = true) // $REMAP
    public void setCurrentDimensionRef(final MapWorld instance, final ResourceKey<Level> value, final Operation<Void> original) {
        original.call(instance, value);
        this.xaeroPlus$currentDimensionRef = getDimension(value);
    }

    /**
     * @author rfresh2
     * @reason skip hot hashtable lookup with cached reference
     */
    @Overwrite
    public MapDimension getCurrentDimension() {
        ResourceKey<Level> dimId = this.currentDimensionId;
        MapDimension ref = xaeroPlus$currentDimensionRef;
        if (dimId == null) return null;
        if (ref != null) return ref;
        return this.getDimension(dimId);
    }
}

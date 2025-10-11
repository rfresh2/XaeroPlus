package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.cache.BlockStateShortShapeCache;

import java.util.function.Supplier;

@Mixin(value = BlockStateShortShapeCache.class, remap = false)
public class MixinBlockStateShortShapeCache {

    @WrapOperation(method = "supplyForIOThread", at = @At(
        value = "FIELD",
        opcode = Opcodes.GETFIELD,
        target = "Lxaero/map/cache/BlockStateShortShapeCache;ioThreadWaitingForSupplier:Ljava/util/function/Supplier;"
    ))
    public Supplier<Boolean> patchConcurrencyCrash(final BlockStateShortShapeCache instance, final Operation<Supplier<Boolean>> original) {
        // this field can sometimes be null, which is not checked by xaero's code
        // there is concurrent unsynced writes to the field which would preferably be solved with locks
        // but this is the least invasive for xaeroplus to do
        var supplier = original.call(instance);
        if (supplier != null) return supplier;
        return () -> false;
    }
}

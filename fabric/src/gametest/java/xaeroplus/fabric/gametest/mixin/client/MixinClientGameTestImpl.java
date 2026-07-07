package xaeroplus.fabric.gametest.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.fabric.impl.client.gametest.util.ClientGameTestImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientGameTestImpl.class)
public class MixinClientGameTestImpl {

    @ModifyExpressionValue(method = "waitForWorldLoad",
        at = @At(
            value = "CONSTANT",
            args = "intValue=1200"))
    private static int customMaxWaitTime(final int original) {
        return 20 * 600;
    }
}

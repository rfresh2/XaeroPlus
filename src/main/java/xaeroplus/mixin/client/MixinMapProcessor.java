package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;

import static java.util.Objects.nonNull;

@Mixin(value = MapProcessor.class, remap = false)
public class MixinMapProcessor {

    @Inject(method = "getMainId", at = @At("HEAD"), cancellable = true)
    private void getMainId(boolean rootFolderFormat, CallbackInfoReturnable<String> cir) {
        Minecraft mc = Minecraft.getMinecraft();
        if (nonNull(mc.getCurrentServerData())) {
            // use common directories based on server list name instead of IP
            // good for proxies
            cir.setReturnValue("Multiplayer_" + mc.getCurrentServerData().serverName);
            cir.cancel();
        }
    }

    /**
     * @author
     */
    @Overwrite
    public String getDimensionName(int id) {
        return "DIM" + id; // remove backwards compatibility for "null" overworld dimension id
    }
}

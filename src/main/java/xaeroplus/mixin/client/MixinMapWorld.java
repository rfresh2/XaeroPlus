package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.world.MapWorld;

@Mixin(value = MapWorld.class, remap = false)
public class MixinMapWorld {

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(String mainId, String oldUnfixedMainId, MapProcessor mapProcessor, final CallbackInfo ci) {
    }

    /**
     * @author rfresh2
     * @reason fix cross dim teleports command in forge 1.12 (will only work on forge servers)
     */
    @Overwrite
    public String getDimensionTeleportCommandFormat() {
        // xaero uses the wrong command for 1.12 cross dim teleports lol
        return "/forge setdim " + Minecraft.getMinecraft().getSession().getUsername() + " {d} {x} {y} {z}";
    }
}

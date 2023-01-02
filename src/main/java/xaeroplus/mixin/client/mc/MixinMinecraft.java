package xaeroplus.mixin.client.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.NewChunks;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("TAIL"))
    public void loadWorld(WorldClient p_71353_1_, String p_71353_2_, CallbackInfo ci) {
        NewChunks.reset();
    }
}

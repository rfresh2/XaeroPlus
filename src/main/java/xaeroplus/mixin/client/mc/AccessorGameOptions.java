package xaeroplus.mixin.client.mc;

import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Options.class)
public interface AccessorGameOptions {
    @Accessor
    int getServerRenderDistance();
}

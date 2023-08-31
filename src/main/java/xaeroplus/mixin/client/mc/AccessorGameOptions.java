package xaeroplus.mixin.client.mc;

import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameOptions.class)
public interface AccessorGameOptions {
    @Accessor
    int getServerViewDistance();
}

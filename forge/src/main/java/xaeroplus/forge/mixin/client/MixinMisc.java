package xaeroplus.forge.mixin.client;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import xaero.map.misc.Misc;

@Mixin(value = Misc.class, remap = false)
public class MixinMisc {

    /**
     * @author rfresh2
     * @reason hide unset keybind text
     */
    @Overwrite
    public static String getKeyName(KeyMapping kb) {
        return kb != null && kb.getKey().getValue() != -1 ? kb.getTranslatedKeyMessage().getString().toUpperCase() : "";
    }
}

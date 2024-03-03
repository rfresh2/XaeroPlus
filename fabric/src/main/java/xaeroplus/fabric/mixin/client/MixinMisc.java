package xaeroplus.fabric.mixin.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
        return kb != null && KeyBindingHelper.getBoundKeyOf(kb).getValue() != -1 ? kb.getTranslatedKeyMessage().getString().toUpperCase() : "";
    }
}

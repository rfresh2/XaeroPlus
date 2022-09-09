package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import xaero.map.misc.Misc;

@Mixin(value = Misc.class)
public class MixinMisc {

    /**
     * @author rfresh2
     * @reason skipping world render messes up some events on hack clients
     */
    @Overwrite
    public static boolean screenShouldSkipWorldRender(GuiScreen screen, boolean checkOtherMod) {
        return false;
    }
}

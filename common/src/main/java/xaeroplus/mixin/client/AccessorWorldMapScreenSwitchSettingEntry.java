package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.gui.ScreenSwitchSettingEntry;

@Mixin(value = ScreenSwitchSettingEntry.class, remap = false)
public interface AccessorWorldMapScreenSwitchSettingEntry {
    @Accessor
    String getName();
}

package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

@Mixin(value = RightClickOption.class, remap = false)
public interface AccessorRightClickOption {
    @Accessor
    String getName();
}

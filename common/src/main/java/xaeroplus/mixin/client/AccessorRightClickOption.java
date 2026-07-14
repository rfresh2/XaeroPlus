package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

@Mixin(value = RightClickOption.class, remap = false)
public interface AccessorRightClickOption {
    @Invoker("getName")
    String invokeGetName();
}

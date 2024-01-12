package xaeroplus.mixin.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import xaero.map.gui.GuiCaveModeOptions;
import xaero.map.world.MapDimension;

@Mixin(value = GuiCaveModeOptions.class, remap = false)
public interface AccessorGuiCaveModeOptions {

    @Accessor
    void setDimension(MapDimension dimension);

    @Accessor
    AbstractWidget getCaveModeStartSlider();

    @Accessor
    EditBox getCaveModeStartField();

    @Invoker("getCaveModeTypeButtonMessage")
    Component invokeCaveModeTypeButtonMessage();

}

package xaeroplus.mixin.client;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
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
    ClickableWidget getCaveModeStartSlider();

    @Accessor
    TextFieldWidget getCaveModeStartField();

    @Invoker("getCaveModeTypeButtonMessage")
    Text invokeCaveModeTypeButtonMessage();

}

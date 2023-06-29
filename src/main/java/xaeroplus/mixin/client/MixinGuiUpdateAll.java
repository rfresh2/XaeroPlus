package xaeroplus.mixin.client;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.patreon.GuiUpdateAll;

import java.net.URI;

@Mixin(value = GuiUpdateAll.class, remap = false)
public abstract class MixinGuiUpdateAll extends ConfirmScreen {

    public MixinGuiUpdateAll(final BooleanConsumer callback, final Text title, final Text message) {
        super(callback, title, message);
    }

    @Inject(method = "init", at = @At("TAIL"), remap = true)
    public void initGui(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(Text.of("Go to XaeroPlus Github for updates"), (button -> {
            try {
                 Util.getOperatingSystem().open(new URI("https://github.com/rfresh2/XaeroPlus"));
            } catch (Exception e) {
                 // ???
            }
        }))
        .dimensions(this.width / 2 - 100,
            this.height / 6 + 168,
            200,
            20)
        .build());
    }
}

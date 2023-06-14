package xaeroplus.mixin.client;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.MySmallButton;
import xaero.common.patreon.GuiUpdateAll;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Mixin(value = GuiUpdateAll.class, remap = false)
public abstract class MixinGuiUpdateAll extends ConfirmScreen {

    public MixinGuiUpdateAll(final BooleanConsumer callback, final Text title, final Text message) {
        super(callback, title, message);
    }

    @Inject(method = "init", at = @At("TAIL"), remap = true)
    public void initGui(CallbackInfo ci) {
        addButton(new MySmallButton(999,
                                    this.width / 2 - 100,
                                    this.height / 6 + 168,
                                    Text.of("Go to XaeroPlus Github for updates"),
                                    (button -> {
                                        Desktop d = Desktop.getDesktop();
                                        try {
                                            d.browse(new URI("https://github.com/rfresh2/XaeroPlus"));
                                        } catch (IOException | URISyntaxException e) {
                                            // ???
                                        }
                                   })));
    }
}

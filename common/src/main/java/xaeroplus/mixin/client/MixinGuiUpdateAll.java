package xaeroplus.mixin.client;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.patreon.GuiUpdateAll;

import java.net.URI;

@Mixin(value = GuiUpdateAll.class, remap = false)
public abstract class MixinGuiUpdateAll extends ConfirmScreen {

    public MixinGuiUpdateAll(final BooleanConsumer callback, final Component title, final Component message) {
        super(callback, title, message);
    }

    @Inject(method = "init", at = @At("RETURN"), remap = true)
    public void initGui(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.translatable("xaeroplus.gui.check_github_button"), (button -> {
            try {
                 Util.getPlatform().openUri(new URI("https://github.com/rfresh2/XaeroPlus"));
            } catch (Exception e) {
                 // ???
            }
        }))
        .bounds(this.width / 2 - 100,
            this.height / 6 + 168,
            200,
            20)
        .build());
    }
}

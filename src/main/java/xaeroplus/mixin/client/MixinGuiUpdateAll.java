package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.patreon.GuiUpdateAll;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Mixin(value = GuiUpdateAll.class, remap = false)
public abstract class MixinGuiUpdateAll extends GuiYesNo {

    public MixinGuiUpdateAll(GuiYesNoCallback p_i1082_1_, String p_i1082_2_, String p_i1082_3_, int p_i1082_4_) {
        super(p_i1082_1_, p_i1082_2_, p_i1082_3_, p_i1082_4_);
    }

    @Inject(method = "initGui", at = @At("TAIL"), remap = true)
    public void initGui(CallbackInfo ci) {
        this.buttonList.add(new GuiButton(999, this.width / 2 - 100, this.height / 6 + 168,
                                          I18n.format("gui.xaeroplus.check_github_button")));
    }

    @Inject(method = "actionPerformed", at = @At("TAIL"), remap = true)
    public void actionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == 999) {
            Desktop d = Desktop.getDesktop();
            try {
                d.browse(new URI("https://github.com/rfresh2/XaeroPlus"));
            } catch (IOException | URISyntaxException e) {
                // ???
            }
        }
    }
}

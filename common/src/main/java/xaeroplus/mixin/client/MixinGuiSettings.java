package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.gui.GuiSettings;
import xaero.lib.client.gui.ISettingEntry;
import xaero.lib.client.gui.ScreenBase;
import xaero.lib.client.gui.widget.MyTinyButton;
import xaero.lib.common.util.KeySortableByOther;
import xaeroplus.feature.extensions.XaeroPlusSettingEntry;
import xaeroplus.settings.Settings;

import java.util.ArrayList;

@Mixin(value = GuiSettings.class, remap = false)
public abstract class MixinGuiSettings extends ScreenBase {

    @Shadow
    protected int entriesPerPage;

    @Unique
    private int xaeroPlus$settingEntryWidth = 200;

    @Shadow private MyTinyButton nextButton;

    @Shadow private MyTinyButton prevButton;

    protected MixinGuiSettings(final Screen parent, final Screen escape, final Component titleIn) {
        super(parent, escape, titleIn);
    }

    @Inject(method = "init", at = @At("HEAD"))
    public void adjustEntriesPerPage(final CallbackInfo ci) {
        this.xaeroPlus$settingEntryWidth = 200; // default width
        this.entriesPerPage = 12; // fills height = 240
        if (Settings.REGISTRY.expandSettingEntries.get()) {
            if (this.height > 350) {
                int extraRows = Math.min((this.height - 240) / 50, 8);
                this.entriesPerPage = 12 + (2 * extraRows);
            }
            if (this.width > 800) {
                xaeroPlus$settingEntryWidth = 300;
            }
        }
    }

    @Inject(method = "init", at = @At(
        value = "RETURN"
    ))
    public void adjustForwardBackButtonPositionsForExtraRows(final CallbackInfo ci) {
        if (!Settings.REGISTRY.expandSettingEntries.get()) return;
        int extraRows = (this.entriesPerPage - 12) / 2;
        int yAdjust = (extraRows * 24);
        this.nextButton.setY(this.nextButton.getY() + yAdjust);
        this.prevButton.setY(this.prevButton.getY() + yAdjust);
        this.children().stream()
            .filter(child -> child instanceof Button)
            .map(child -> (Button) child)
            .filter(button -> button.getMessage().getContents() instanceof TranslatableContents)
            .filter(button -> ((TranslatableContents) button.getMessage().getContents()).getKey().equals("gui.xaero_back"))
            .findFirst()
            .ifPresent(button -> button.setY(button.getY() + yAdjust));
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"), remap = true)
    public boolean settingListToRenderRedirect(final ArrayList instance, final Object entryObject) {
        final KeySortableByOther<ISettingEntry> entry = (KeySortableByOther<ISettingEntry>) entryObject;
        ISettingEntry settingEntry = entry.getKey();
        if (settingEntry instanceof XaeroPlusSettingEntry<?> xaeroPlusSettingEntry) {
            if (!xaeroPlusSettingEntry.getXaeroPlusSetting().isVisible()) {
                return false;
            }
        }
        instance.add(entryObject);
        return false;
    }

    @WrapOperation(
        method = "init",
        slice = @Slice(
            from = @At(
                value = "FIELD",
                opcode = Opcodes.PUTFIELD,
                target = "Lxaero/lib/client/gui/GuiSettings;foundSomething:Z"
            )),
        at = @At(
            value = "INVOKE",
            target = "Lxaero/lib/client/gui/ISettingEntry;createWidget(III)Lnet/minecraft/client/gui/components/AbstractWidget;",
            ordinal = 0
        ),
        remap = true
    )
    public AbstractWidget adjustSettingEntryWidth(final ISettingEntry instance, final int x, final int y, final int w, final Operation<AbstractWidget> original, @Local(name = "i") int i) {
        if (!Settings.REGISTRY.expandSettingEntries.get()) return original.call(instance, x, y, w);
        var halfW = this.width / 2;
        var halfMargin = 5;
        var adjustedX = i % 2 == 0
            ? halfW - xaeroPlus$settingEntryWidth - halfMargin
            : halfW + halfMargin;
        return original.call(instance, adjustedX, y, xaeroPlus$settingEntryWidth);
    }
}

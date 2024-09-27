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
import xaero.common.IXaeroMinimap;
import xaero.common.gui.*;
import xaero.common.misc.KeySortableByOther;
import xaero.common.settings.ModOptions;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.Settings;

import java.lang.reflect.Field;
import java.util.ArrayList;

@Mixin(value = GuiSettings.class, remap = false)
public abstract class MixinCommonGuiSettings extends ScreenBase {
    protected MixinCommonGuiSettings(final IXaeroMinimap modMain, final Screen parent, final Screen escape, final Component titleIn) {
        super(modMain, parent, escape, titleIn);
    }

    @Shadow
    protected int entriesPerPage;

    @Unique
    private int xaeroPlus$settingEntryWidth = 200;

    @Shadow private MyTinyButton nextButton;

    @Shadow private MyTinyButton prevButton;

    @Inject(method = "init", at = @At("HEAD"))
    public void adjustEntriesPerPage(final CallbackInfo ci) {
        this.xaeroPlus$settingEntryWidth = 200; // default width
        this.entriesPerPage = 12; // fills height = 240
        if (Settings.REGISTRY.expandSettingEntries.get()) {
            if (this.height > 350) {
                int extraRows = Math.min((height - 240) / 50, 6);
                this.entriesPerPage = 12 + (2 * extraRows);
            }
            if (this.width > 800) {
                xaeroPlus$settingEntryWidth = 250;
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
        if (settingEntry instanceof ConfigSettingEntry) {
            try {
                Field option = ConfigSettingEntry.class.getDeclaredField("option");
                option.setAccessible(true);
                ModOptions modOptions = (ModOptions) option.get(settingEntry);
                String settingName = modOptions.getEnumString();
                var xpSetting = Settings.REGISTRY.getSettingByName(settingName);
                if (xpSetting != null) {
                    if (!xpSetting.isVisible()) {
                        // skip adding setting
                        return false;
                    }
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.warn("Failed to edit settings gui", e);
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
                target = "Lxaero/common/gui/GuiSettings;foundSomething:Z"
            )),
        at = @At(
            value = "INVOKE",
            target = "Lxaero/common/gui/ISettingEntry;createWidget(IIIZ)Lnet/minecraft/client/gui/components/AbstractWidget;",
            ordinal = 0
        ),
        remap = true
    )
    public AbstractWidget adjustSettingEntryWidth(final ISettingEntry instance, final int x, final int y, final int w, final boolean canEditIngameSettings, final Operation<AbstractWidget> original,
                                                  @Local(name = "i") int i) {
        if (!Settings.REGISTRY.expandSettingEntries.get()) return original.call(instance, x, y, w, canEditIngameSettings);
        int xOffset = ((i % 2 == 0) ? -1 : 1) * ((xaeroPlus$settingEntryWidth - 200) / 2);
        return original.call(instance, x + xOffset, y, xaeroPlus$settingEntryWidth, canEditIngameSettings);
    }
}

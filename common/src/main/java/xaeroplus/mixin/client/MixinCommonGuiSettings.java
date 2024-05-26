package xaeroplus.mixin.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.gui.*;
import xaero.common.misc.KeySortableByOther;
import xaero.common.settings.ModOptions;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSetting;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Optional;

@Mixin(value = GuiSettings.class, remap = false)
public abstract class MixinCommonGuiSettings extends ScreenBase {
    protected MixinCommonGuiSettings(final IXaeroMinimap modMain, final Screen parent, final Screen escape, final Component titleIn) {
        super(modMain, parent, escape, titleIn);
    }

    @Shadow
    protected int entriesPerPage;

    @Shadow private MyTinyButton nextButton;

    @Shadow private MyTinyButton prevButton;

    @Inject(method = "init", at = @At("HEAD"))
    public void adjustEntriesPerPage(final CallbackInfo ci) {
        this.entriesPerPage = 12; // fills height = 240
        if (XaeroPlusSettingRegistry.expandSettingEntries.getValue() && this.height > 350) {
            int extraRows = Math.min((height - 240) / 50, 6);
            this.entriesPerPage = 12 + (2 * extraRows);
        }
    }

    @Inject(method = "init", at = @At(
        value = "RETURN"
    ))
    public void adjustForwardBackButtonPositionsForExtraRows(final CallbackInfo ci) {
        if (!XaeroPlusSettingRegistry.expandSettingEntries.getValue()) return;
        int extraRows = (this.entriesPerPage - 12) / 2;
        int yAdjust = (extraRows * 24);
        this.nextButton.y = this.nextButton.y + yAdjust;
        this.prevButton.y = this.prevButton.y + yAdjust;
        this.children().stream()
            .filter(child -> child instanceof Button)
            .map(child -> (Button) child)
            .filter(button -> button.getMessage().getContents() instanceof TranslatableContents)
            .filter(button -> ((TranslatableContents) button.getMessage().getContents()).getKey().equals("gui.xaero_back"))
            .findFirst()
            .ifPresent(button -> button.y = button.y + yAdjust);
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
                Optional<XaeroPlusSetting> foundSetting = XaeroPlusSettingsReflectionHax.ALL_MINIMAP_SETTINGS.get().stream()
                        .filter(s -> s.getSettingName().equals(settingName))
                        .findFirst();
                if (foundSetting.isPresent()) {
                    if (!foundSetting.get().isVisible()) {
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
}

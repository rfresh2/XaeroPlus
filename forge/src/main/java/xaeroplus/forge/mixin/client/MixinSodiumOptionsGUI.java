package xaeroplus.forge.mixin.client;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.binding.GenericBinding;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.forge.XaeroPlusEmbeddiumOptionStorage;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.util.List;

@Pseudo
@Mixin(value = SodiumOptionsGUI.class, remap = false)
public class MixinSodiumOptionsGUI {

    @Final
    @Shadow
    private List<OptionPage> pages;

    @Inject(method = "<init>", at = @At(
        value = "RETURN"
    ))
    public void injectXPSettings(final Screen prevScreen, final CallbackInfo ci) {
        pages.add(new OptionPage(Component.literal("XaeroPlus"), ImmutableList.<OptionGroup>of(
            OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(Boolean.TYPE, XaeroPlusEmbeddiumOptionStorage.INSTANCE)
                         .setName(Component.translatable("setting.minimap.fps_limiter"))
                         .setTooltip(Component.translatable("setting.minimap.fps_limiter.tooltip"))
                         .setControl(TickBoxControl::new)
                         .setBinding(new GenericBinding<>(
                             (config, value) -> XaeroPlusSettingRegistry.minimapFpsLimiter.setValue(value),
                             config -> XaeroPlusSettingRegistry.minimapFpsLimiter.getValue()
                         ))
                         .build())
                .add(OptionImpl.createBuilder(int.class, XaeroPlusEmbeddiumOptionStorage.INSTANCE)
                         .setName(Component.translatable("setting.minimap.fps_limiter_limit"))
                         .setTooltip(Component.translatable("setting.minimap.fps_limiter_limit.tooltip"))
                         .setControl(option -> new SliderControl(
                             option,
                             (int) XaeroPlusSettingRegistry.minimapFpsLimit.getValueMin(),
                             (int) XaeroPlusSettingRegistry.minimapFpsLimit.getValueMax(),
                             (int) XaeroPlusSettingRegistry.minimapFpsLimit.getValueStep(),
                             ControlValueFormatter.number()))
                         .setBinding(new GenericBinding<>(
                             (config, value) -> XaeroPlusSettingRegistry.minimapFpsLimit.setValue((float) value),
                             config -> (int) XaeroPlusSettingRegistry.minimapFpsLimit.getValue()
                         ))
                         .build())
                .add(OptionImpl.createBuilder(int.class, XaeroPlusEmbeddiumOptionStorage.INSTANCE)
                         .setName(Component.translatable("setting.minimap.fps_limiter_limit"))
                         .setTooltip(Component.translatable("setting.minimap.fps_limiter_limit.tooltip"))
                         .setControl(option -> new SliderControl(
                             option,
                             (int) XaeroPlusSettingRegistry.minimapFpsLimit.getValueMin(),
                             (int) XaeroPlusSettingRegistry.minimapFpsLimit.getValueMax(),
                             (int) XaeroPlusSettingRegistry.minimapFpsLimit.getValueStep(),
                             ControlValueFormatter.number()))
                         .setBinding(new GenericBinding<>(
                             (config, value) -> XaeroPlusSettingRegistry.minimapFpsLimit.setValue((float) value),
                             config -> (int) XaeroPlusSettingRegistry.minimapFpsLimit.getValue()
                         ))
                         .build())
                .build()
        )));
    }
}

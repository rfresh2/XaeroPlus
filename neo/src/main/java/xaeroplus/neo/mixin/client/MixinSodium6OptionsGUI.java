package xaeroplus.neo.mixin.client;

import com.google.common.collect.ImmutableList;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI;
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.options.binding.GenericBinding;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatter;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.TickBoxControl;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.neo.XaeroPlusSodium6OptionStorage;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.util.List;

@Pseudo
@Mixin(value = SodiumOptionsGUI.class, remap = false)
public class MixinSodium6OptionsGUI {

    @Final
    @Shadow
    private List<OptionPage> pages;

    @Inject(method = "<init>", at = @At(
        value = "RETURN"
    ))
    public void injectXPSettings(final Screen prevScreen, final CallbackInfo ci) {
        if (!XaeroPlusSettingRegistry.sodiumSettingIntegration.getValue()) return;
        pages.add(new OptionPage(Component.literal("XaeroPlus"), ImmutableList.<OptionGroup>of(
            OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(Boolean.TYPE, XaeroPlusSodium6OptionStorage.INSTANCE)
                         .setName(Component.translatable("setting.minimap.fps_limiter"))
                         .setTooltip(Component.translatable("setting.minimap.fps_limiter.tooltip"))
                         .setControl(TickBoxControl::new)
                         .setBinding(new GenericBinding<>(
                             (config, value) -> XaeroPlusSettingRegistry.minimapFpsLimiter.setValue(value),
                             config -> XaeroPlusSettingRegistry.minimapFpsLimiter.getValue()
                         ))
                         .build())
                .add(OptionImpl.createBuilder(int.class, XaeroPlusSodium6OptionStorage.INSTANCE)
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
                .add(OptionImpl.createBuilder(int.class, XaeroPlusSodium6OptionStorage.INSTANCE)
                         .setName(Component.translatable("setting.minimap.minimap_scaling"))
                         .setTooltip(Component.translatable("setting.minimap.minimap_scaling.tooltip"))
                         .setControl(option -> new SliderControl(
                             option,
                             (int) XaeroPlusSettingRegistry.minimapScaleMultiplierSetting.getValueMin(),
                             (int) XaeroPlusSettingRegistry.minimapScaleMultiplierSetting.getValueMax(),
                             (int) XaeroPlusSettingRegistry.minimapScaleMultiplierSetting.getValueStep(),
                             ControlValueFormatter.number()))
                         .setBinding(new GenericBinding<>(
                             (config, value) -> XaeroPlusSettingRegistry.minimapScaleMultiplierSetting.setValue((float) value),
                             config -> (int) XaeroPlusSettingRegistry.minimapScaleMultiplierSetting.getValue()
                         ))
                         .build())
                .build()
        )));
    }
}

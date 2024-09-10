package xaeroplus.forge;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.binding.GenericBinding;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;
import org.embeddedt.embeddium.client.gui.options.OptionIdentifier;
import xaeroplus.settings.XaeroPlusSettingRegistry;

public class XaeroPlusEmbeddiumOptionsInit {
    public static void onEmbeddiumOptionGUIConstructionEvent(OptionGUIConstructionEvent event) {
        if (!XaeroPlusSettingRegistry.sodiumSettingIntegration.getValue()) return;
        event.addPage(new OptionPage(OptionIdentifier.create("xaeroplus", "options"), Component.literal("XaeroPlus"), ImmutableList.<OptionGroup>of(
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

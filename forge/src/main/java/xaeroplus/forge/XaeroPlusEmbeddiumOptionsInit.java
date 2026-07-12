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
import xaeroplus.settings.Settings;

public class XaeroPlusEmbeddiumOptionsInit {
    public static void onEmbeddiumOptionGUIConstructionEvent(OptionGUIConstructionEvent event) {
        if (!Settings.REGISTRY.sodiumSettingIntegration.get()) return;
        event.addPage(new OptionPage(OptionIdentifier.create("xaeroplus", "options"), Component.literal("XaeroPlus"), ImmutableList.<OptionGroup>of(
            OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(Boolean.TYPE, XaeroPlusEmbeddiumOptionStorage.INSTANCE)
                         .setName(Component.translatable("xaeroplus.setting.fps_limiter"))
                         .setTooltip(Component.translatable("xaeroplus.setting.fps_limiter.tooltip"))
                         .setControl(TickBoxControl::new)
                         .setBinding(new GenericBinding<>(
                             (config, value) -> Settings.REGISTRY.minimapFpsLimiter.setValue(value),
                             config -> Settings.REGISTRY.minimapFpsLimiter.get()
                         ))
                         .build())
                .add(OptionImpl.createBuilder(int.class, XaeroPlusEmbeddiumOptionStorage.INSTANCE)
                         .setName(Component.translatable("xaeroplus.setting.fps_limiter_limit"))
                         .setTooltip(Component.translatable("xaeroplus.setting.fps_limiter_limit.tooltip"))
                         .setControl(option -> new SliderControl(
                             option,
                             (int) Settings.REGISTRY.minimapFpsLimit.getValueMin(),
                             (int) Settings.REGISTRY.minimapFpsLimit.getValueMax(),
                             (int) Settings.REGISTRY.minimapFpsLimit.getValueStep(),
                             ControlValueFormatter.number()))
                         .setBinding(new GenericBinding<>(
                             (config, value) -> Settings.REGISTRY.minimapFpsLimit.setValue(value),
                             config -> Settings.REGISTRY.minimapFpsLimit.getAsInt()
                         ))
                         .build())
                .add(OptionImpl.createBuilder(int.class, XaeroPlusEmbeddiumOptionStorage.INSTANCE)
                         .setName(Component.translatable("xaeroplus.setting.minimap_scaling"))
                         .setTooltip(Component.translatable("xaeroplus.setting.minimap_scaling.tooltip"))
                         .setControl(option -> new SliderControl(
                             option,
                             (int) Settings.REGISTRY.minimapScaleMultiplierSetting.getValueMin(),
                             (int) Settings.REGISTRY.minimapScaleMultiplierSetting.getValueMax(),
                             (int) Settings.REGISTRY.minimapScaleMultiplierSetting.getValueStep(),
                             ControlValueFormatter.number()))
                         .setBinding(new GenericBinding<>(
                             (config, value) -> Settings.REGISTRY.minimapScaleMultiplierSetting.setValue(value),
                             config -> Settings.REGISTRY.minimapScaleMultiplierSetting.getAsInt()
                         ))
                         .build())
                .build()
        )));
    }
}

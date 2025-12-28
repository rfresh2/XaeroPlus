package xaeroplus.neo;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xaeroplus.settings.SettingHooks;
import xaeroplus.settings.Settings;

public class XaeroPlusSodiumConfigBuilder implements ConfigEntryPoint {
    @Override
    public void registerConfigLate(final ConfigBuilder builder) {
        if (!Settings.REGISTRY.sodiumSettingIntegration.get()) return;
        builder.registerModOptions("xaeroplus")
            .addPage(builder.createOptionPage()
                .setName(Component.literal("XaeroPlus"))
                .addOptionGroup(builder.createOptionGroup()
                    .setName(Component.literal("XaeroPlus"))
                    .addOption(builder.createBooleanOption(Identifier.fromNamespaceAndPath("xaeroplus", "minimap_fps_limiter"))
                        .setName(Component.translatable("xaeroplus.setting.fps_limiter"))
                        .setTooltip(Component.translatable("xaeroplus.setting.fps_limiter.tooltip"))
                        .setBinding(
                            Settings.REGISTRY.minimapFpsLimiter::setValue,
                            Settings.REGISTRY.minimapFpsLimiter::get)
                        .setStorageHandler(SettingHooks::saveSettings)
                        .setDefaultValue(false))
                    .addOption(builder.createIntegerOption(Identifier.fromNamespaceAndPath("xaeroplus", "minimap_fps_limit"))
                        .setName(Component.translatable("xaeroplus.setting.fps_limiter_limit"))
                        .setTooltip(Component.translatable("xaeroplus.setting.fps_limiter_limit.tooltip"))
                        .setRange(
                            (int) Settings.REGISTRY.minimapFpsLimit.getValueMin(),
                            (int) Settings.REGISTRY.minimapFpsLimit.getValueMax(),
                            (int) Settings.REGISTRY.minimapFpsLimit.getValueStep())
                        .setBinding(
                            Settings.REGISTRY.minimapFpsLimit::setValue,
                            Settings.REGISTRY.minimapFpsLimit::getAsInt
                        )
                        .setStorageHandler(SettingHooks::saveSettings)
                        .setValueFormatter(i -> Component.literal(String.valueOf(i)))
                        .setDefaultValue(60))
                    .addOption(builder.createIntegerOption(Identifier.fromNamespaceAndPath("xaeroplus", "minimap_scaling"))
                        .setName(Component.translatable("xaeroplus.setting.minimap_scaling"))
                        .setTooltip(Component.translatable("xaeroplus.setting.minimap_scaling.tooltip"))
                        .setRange(
                            (int) Settings.REGISTRY.minimapScaleMultiplierSetting.getValueMin(),
                            (int) Settings.REGISTRY.minimapScaleMultiplierSetting.getValueMax(),
                            (int) Settings.REGISTRY.minimapScaleMultiplierSetting.getValueStep())
                        .setBinding(
                            Settings.REGISTRY.minimapScaleMultiplierSetting::setValue,
                            Settings.REGISTRY.minimapScaleMultiplierSetting::getAsInt
                        )
                        .setStorageHandler(SettingHooks::saveSettings)
                        .setValueFormatter(i -> Component.literal(String.valueOf(i)))
                        .setDefaultValue(1))
                    )
                );

    }
}

package xaeroplus.util.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class XaeroPlusCompatibleMinimapMixinPlugin implements IMixinConfigPlugin {
    private VersionCheckResult versionCheckResult;
    // if we do not apply our settings mixins, they will be overwritten during xaero mod loading
    // if there is any version of minimap present we should try to apply these
    // could cause crashes in certain cases, but we can't really do anything about it - at least until XP settings are moved to a separate file
    private boolean tryMinimapSettingsMixins;
    private Set<String> xaeroMinimapSettingsMixins;
    // always try to apply these even when minimap is not present
    private Set<String> xaeroWorldMapSettingsMixins;

    @Override
    public void onLoad(final String mixinPackage) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
        versionCheckResult = MinimapBaseVersionCheck.versionCheck();
        tryMinimapSettingsMixins = versionCheckResult.anyPresentMinimapVersion().isPresent();
        if (!versionCheckResult.minimapCompatible()) {
            if (tryMinimapSettingsMixins) {
                xaeroMinimapSettingsMixins = Set.of(
                    "MixinBetterPVP",
                    "MixinCrashHandler",
                    "MixinMinimapModOption",
                    "MixinMinimapModOptionsAccessor",
                    "MixinMinimapModSettings",
                    "MixinXaeroMinimap"
                );
            }
            xaeroWorldMapSettingsMixins = Set.of(
                "MixinWorldMap",
                "MixinWorldMapModSettings",
                "MixinWorldMapOption"
            );
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return true;
        if (versionCheckResult.minimapCompatible()) return true;
        if (mixinClassName.startsWith("xaeroplus")) {
            var classNameSplit = mixinClassName.split("\\.");
            var mixinName = classNameSplit[classNameSplit.length - 1];
            if (xaeroWorldMapSettingsMixins.contains(mixinName)) return true;
            if (tryMinimapSettingsMixins)
                return xaeroMinimapSettingsMixins.contains(mixinName);
            return false;
        }
        return true;
    }

    @Override
    public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) {

    }
}

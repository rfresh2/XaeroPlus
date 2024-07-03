package xaeroplus.fabric.util.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

import static xaeroplus.fabric.util.compat.XaeroPlusMinimapCompatibilityChecker.versionCheckResult;

/**
 * Avoids applying XP mixins if Minimap is not present or not at a compatible version
 *
 * We want to either accept BetterPVP or Minimap at a specific version. But we can't make fabric.json enforce this so we have to do it ourselves
 */
public class XaeroPlusCompatibleMinimapMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(final String mixinPackage) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
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
            return mixinClassName.contains("MixinMinecraftClientFabric");
        } else {
            return true;
        }
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

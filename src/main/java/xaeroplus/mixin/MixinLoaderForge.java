package xaeroplus.mixin;

import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MixinLoaderForge implements IEarlyMixinLoader, IFMLLoadingPlugin {

    public MixinLoaderForge() {
    }

    @Override
    public List<String> getMixinConfigs() {
        ArrayList<String> list = new ArrayList<>();
        list.add("mixins.xaeroplus.json");
        if (FMLLaunchHandler.isDeobfuscatedEnvironment()) {
            list.add("mixins.baritone.json");
        }
        return list;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(final Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}

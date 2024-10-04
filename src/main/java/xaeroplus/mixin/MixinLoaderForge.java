package xaeroplus.mixin;

import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import zone.rong.mixinbooter.IEarlyMixinLoader;
import zone.rong.mixinbooter.MixinLoader;

import java.util.ArrayList;
import java.util.List;

@MixinLoader
public class MixinLoaderForge implements IEarlyMixinLoader {

    public MixinLoaderForge() {
        MixinBootstrap.init();
        if (FMLLaunchHandler.isDeobfuscatedEnvironment()) {
            Mixins.addConfigurations("mixins.baritone.json");
        }
        Mixins.addConfigurations("mixins.xaeroplus.json");
        MixinEnvironment.getDefaultEnvironment().setObfuscationContext("searge");
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
}

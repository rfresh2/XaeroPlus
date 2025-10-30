package xaeroplus.forge;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(value = "xaeroplus")
public class XaeroPlusForge {

    public XaeroPlusForge(FMLJavaModLoadingContext context) {
        BusGroup modBusGroup = context.getModBusGroup();
        if (FMLEnvironment.dist.isClient()) {
            // todo: mixinextra jarjar autoload broke in 60.0.13, check back later
            MixinExtrasBootstrap.init();
            XaeroPlusForgeClient client = new XaeroPlusForgeClient();
            client.init(context, modBusGroup);
        }
    }
}

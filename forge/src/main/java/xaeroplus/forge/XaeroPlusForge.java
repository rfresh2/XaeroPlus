package xaeroplus.forge;

import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(value = "xaeroplus")
public class XaeroPlusForge {

    public XaeroPlusForge(FMLJavaModLoadingContext context) {
        BusGroup modBusGroup = context.getModBusGroup();;
        if (FMLEnvironment.dist.isClient()) {
            XaeroPlusForgeClient client = new XaeroPlusForgeClient();
            client.init(context, modBusGroup);
        }
    }
}

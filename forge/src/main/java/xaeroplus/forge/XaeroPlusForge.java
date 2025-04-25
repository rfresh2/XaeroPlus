package xaeroplus.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(value = "xaeroplus")
public class XaeroPlusForge {
    public static final IEventBus FORGE_EVENT_BUS = MinecraftForge.EVENT_BUS;

    public XaeroPlusForge(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        if (FMLEnvironment.dist.isClient()) {
            XaeroPlusForgeClient client = new XaeroPlusForgeClient();
            client.init(context, modEventBus, FORGE_EVENT_BUS);
        }
    }
}

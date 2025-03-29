package xaeroplus.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(value = "xaeroplus")
public class XaeroPlusForge {
    public static final IEventBus FORGE_EVENT_BUS = MinecraftForge.EVENT_BUS;

    public XaeroPlusForge(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            XaeroPlusForgeClient client = new XaeroPlusForgeClient();
            client.init(context, modEventBus, FORGE_EVENT_BUS);
        });
    }
}

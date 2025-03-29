package xaeroplus.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import static net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get;

@Mod(value = "xaeroplus")
public class XaeroPlusForge {
    public static final IEventBus FORGE_EVENT_BUS = MinecraftForge.EVENT_BUS;

    public XaeroPlusForge() {
        IEventBus modEventBus = get().getModEventBus();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            XaeroPlusForgeClient client = new XaeroPlusForgeClient();
            client.init(modEventBus, FORGE_EVENT_BUS);
        });
    }
}

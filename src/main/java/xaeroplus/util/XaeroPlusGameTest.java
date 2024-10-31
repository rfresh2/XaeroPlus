package xaeroplus.util;

import net.minecraftforge.fml.common.FMLCommonHandler;
import org.spongepowered.asm.mixin.MixinEnvironment;
import xaeroplus.XaeroPlus;

public class XaeroPlusGameTest {
    public static void applyMixinsTest() {
        // forcing our mixins to apply by loading some classes that aren't loaded by just joining the game
        try {
            MixinEnvironment.getCurrentEnvironment().audit();
            XaeroPlus.LOGGER.info("Classload test complete");
        } catch (final Throwable e) {
            XaeroPlus.LOGGER.error("Classload test failed", e);
            FMLCommonHandler.instance().exitJava(1, true);
        }
    }
}

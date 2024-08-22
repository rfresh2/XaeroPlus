package xaeroplus.util;

import net.minecraftforge.fml.common.FMLCommonHandler;
import xaero.common.gui.GuiWaypoints;
import xaero.map.gui.GuiMap;
import xaeroplus.XaeroPlus;

public class XaeroPlusGameTest {
    public static void applyMixinsTest() {
        // forcing our mixins to apply by loading some classes that aren't loaded by just joining the game
        try {
            // todo: ideally we would iterate over every XP mixin class target and load them all
            //  not sure how to get a list of all mixin targets though
            String a = GuiMap.class.getSimpleName();
            String b = GuiWaypoints.class.getSimpleName();
            XaeroPlus.LOGGER.info("Classload test complete");
        } catch (final Throwable e) {
            XaeroPlus.LOGGER.error("Classload test failed", e);
            FMLCommonHandler.instance().exitJava(1, true);
        }
    }
}

package xaeroplus.util;

import xaero.common.gui.GuiWaypoints;
import xaero.map.gui.GuiMap;
import xaeroplus.XaeroPlus;

public class XaeroPlusGameTest {
    public static void applyMixinsTest() {
        // forcing our mixins to apply by loading some classes that aren't loaded by just joining the game
        try {
            // todo: ideally we would iterate over every XP mixin class target and load them all
            //  not sure how to get a list of all mixin targets though
            var a= GuiMap.class.getSimpleName();
            var b= GuiWaypoints.class.getSimpleName();
            XaeroPlus.LOGGER.info("Classload test complete");
        } catch (final Throwable e) {
            XaeroPlus.LOGGER.error("Classload test failed", e);
            System.exit(1);
        }
    }
}

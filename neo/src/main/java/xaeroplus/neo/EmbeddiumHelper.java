package xaeroplus.neo;

import xaeroplus.XaeroPlus;

public class EmbeddiumHelper {
    private static boolean isEmbeddiumPresent = false;
    private static boolean checked = false;

    public static boolean isEmbeddiumPresent() {
        if (!checked) {
            try {
                Class.forName("org.embeddedt.embeddium.api.OptionGUIConstructionEvent");
                XaeroPlus.LOGGER.info("Found Embeddium Options API support. Enabling Embeddium support");
                isEmbeddiumPresent = true;
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.info("Embeddium Options API support not found. Disabling Embeddium support");
                isEmbeddiumPresent = false;
            }
            checked = true;
        }
        return isEmbeddiumPresent;
    }
}

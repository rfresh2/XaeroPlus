package xaeroplus.feature.render.shaders;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import xaeroplus.XaeroPlus;

public class XaeroPlusShaders {
    public static HighlightShader HIGHLIGHT_SHADER = null;
    public static MultiColorHighlightShader MULTI_COLOR_HIGHLIGHT_SHADER = null;
    public static LinesShader LINES_SHADER = null;
    private static boolean firstReload = true;

    public static void onResourceReload(ResourceManager resourceManager) {
        try {
            if (HIGHLIGHT_SHADER != null) {
                HIGHLIGHT_SHADER.close();
            }
            if (MULTI_COLOR_HIGHLIGHT_SHADER != null) {
                MULTI_COLOR_HIGHLIGHT_SHADER.close();
            }
            if (LINES_SHADER != null) {
                LINES_SHADER.close();
            }
            HIGHLIGHT_SHADER = new HighlightShader(resourceManager);
            MULTI_COLOR_HIGHLIGHT_SHADER = new MultiColorHighlightShader(resourceManager);
            LINES_SHADER = new LinesShader(resourceManager);
            XaeroPlus.LOGGER.info("Reloaded Shaders");
        } catch (final Exception e) {
            if (firstReload) {
                throw new RuntimeException("Failed reloading shaders");
            }
            XaeroPlus.LOGGER.error("Error in shader reloader", e);
        }
        firstReload = false;
    }

    public static void ensureShaders() {
        if ((HIGHLIGHT_SHADER == null || MULTI_COLOR_HIGHLIGHT_SHADER == null || LINES_SHADER == null) && firstReload) {
            onResourceReload(Minecraft.getInstance().getResourceManager());
        }
    }
}

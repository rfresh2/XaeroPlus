package xaeroplus.feature.render;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import xaeroplus.XaeroPlus;

public class XaeroPlusShaders {
    @Nullable public static HighlightShader HIGHLIGHT_SHADER = null;
    private static boolean firstReload = true;

    public static void onResourceReload(ResourceManager resourceManager) {
        try {
            if (HIGHLIGHT_SHADER != null) {
                HIGHLIGHT_SHADER.close();
            }
            HIGHLIGHT_SHADER = new HighlightShader(resourceManager);
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
        if (HIGHLIGHT_SHADER == null && firstReload) {
            onResourceReload(Minecraft.getInstance().getResourceManager());
        }
    }
}

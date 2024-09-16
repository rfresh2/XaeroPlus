package xaeroplus.fabric.util;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import xaeroplus.feature.render.XaeroPlusShaders;

public class XPShaderResourceReloadListener implements SimpleSynchronousResourceReloadListener {
    private final ResourceLocation listenerId = ResourceLocation.fromNamespaceAndPath("xaeroplus", "shader_reload");

    @Override
    public ResourceLocation getFabricId() {
        return listenerId;
    }

    @Override
    public void onResourceManagerReload(final ResourceManager resourceManager) {
        XaeroPlusShaders.onResourceReload(resourceManager);
    }
}

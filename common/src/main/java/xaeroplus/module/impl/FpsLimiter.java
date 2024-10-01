package xaeroplus.module.impl;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.lenni0451.lambdaevents.EventHandler;
import xaeroplus.event.MinimapRenderEvent;
import xaeroplus.feature.render.buffered.BufferedComponent;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;

import java.util.function.Supplier;

public class FpsLimiter extends Module {
    // todo: Buffer and mutate the rotation framebuffer separately to not cause visual
    //  impact while minimap north is not locked
    // this must be initialized AFTER MC's screen is initialized
    private final Supplier<BufferedComponent> minimapRenderInstanceSupplier = Suppliers.memoize(
        () -> new BufferedComponent(Settings.REGISTRY.minimapFpsLimit::getAsInt));
    public static RenderTarget renderTargetOverwrite = null;

    @EventHandler
    public void onMinimapRenderEvent(final MinimapRenderEvent event) {
        event.cancelled = minimapRenderInstanceSupplier.get().render();
        event.postRenderCallback = minimapRenderInstanceSupplier.get()::postRender;
    }
}

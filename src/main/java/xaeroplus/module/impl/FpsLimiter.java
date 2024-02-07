package xaeroplus.module.impl;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.lenni0451.lambdaevents.EventHandler;
import xaeroplus.event.MinimapRenderEvent;
import xaeroplus.feature.render.buffered.BufferedComponent;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.util.function.Supplier;

@Module.ModuleInfo()
public class FpsLimiter extends Module {
    // this must be initialized AFTER MC's screen is initialized
    private final Supplier<BufferedComponent> minimapComponentSupplier = Suppliers.memoize(
        () -> new BufferedComponent(() -> (int) XaeroPlusSettingRegistry.minimapFpsLimit.getValue()));

    public static RenderTarget renderTargetOverwrite = null;

    @EventHandler
    public void onMinimapRenderEvent(final MinimapRenderEvent event) {
        event.cancelled = minimapComponentSupplier.get().render();
        event.postRenderCallback = minimapComponentSupplier.get()::postRender;
    }
}

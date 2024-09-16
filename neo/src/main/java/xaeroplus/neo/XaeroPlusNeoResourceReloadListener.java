package xaeroplus.neo;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import xaeroplus.feature.render.XaeroPlusShaders;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class XaeroPlusNeoResourceReloadListener implements PreparableReloadListener {
    @Override
    public CompletableFuture<Void> reload(final PreparationBarrier preparationBarrier, final ResourceManager resourceManager, final ProfilerFiller preparationsProfiler, final ProfilerFiller reloadProfiler, final Executor backgroundExecutor, final Executor gameExecutor) {
        return preparationBarrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
            XaeroPlusShaders.onResourceReload(resourceManager);
        }, gameExecutor);
    }
}

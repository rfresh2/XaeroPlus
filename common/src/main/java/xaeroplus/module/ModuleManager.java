package xaeroplus.module;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import xaeroplus.module.impl.*;

import static java.util.Arrays.asList;

public class ModuleManager {
    private static final Reference2ObjectMap<Class<? extends Module>, Module> modulesClassMap = new Reference2ObjectOpenHashMap<>();

    static {
        asList(
            new BaritoneGoalSync(),
            new FpsLimiter(),
            new Highways(),
            new LiquidNewChunks(),
            new OldChunks(),
            new OldBiomes(),
            new PaletteNewChunks(),
            new Portals(),
            new PortalSkipDetection(),
            new WaystoneSync(),
            new WorldTools()
        ).forEach(ModuleManager::addModule);
    }

    public static void addModule(Module module) {
        modulesClassMap.put(module.getClass(), module);
    }

    public static <T extends Module> T getModule(Class<T> clazz) {
        return (T) modulesClassMap.get(clazz);
    }
}

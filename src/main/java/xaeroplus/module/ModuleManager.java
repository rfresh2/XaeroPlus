package xaeroplus.module;

import xaeroplus.module.impl.*;

import java.util.LinkedHashMap;

import static java.util.Arrays.asList;

public class ModuleManager {
    private static final LinkedHashMap<Class<? extends Module>, Module> modulesClassMap = new LinkedHashMap<>();

    public static void init() {
        asList(
                new BaritoneGoalSync(),
                new NewChunks(),
                new Portals(),
                new PortalSkipDetection(),
                new WaystoneSync())
                .forEach(ModuleManager::addModule);
    }

    private static void addModule(Module module) {
        modulesClassMap.put(module.getClass(), module);
    }

    public static <T extends Module> T getModule(Class<T> clazz) {
        return (T) modulesClassMap.get(clazz);
    }
}

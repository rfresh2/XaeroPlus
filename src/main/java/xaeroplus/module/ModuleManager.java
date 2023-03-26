package xaeroplus.module;

import xaeroplus.XaeroPlus;
import xaeroplus.util.ClassUtil;

import java.util.LinkedHashMap;

import static java.util.Objects.isNull;

public class ModuleManager {
    private static final String modulePackage = "xaeroplus.module.impl";
    private static final LinkedHashMap<Class<? extends Module>, Module> modulesClassMap = new LinkedHashMap<>();

    static {
        init();
    }

    public static void init() {
        for (final Class<?> clazz : ClassUtil.findClassesInPath(modulePackage)) {
            if (isNull(clazz)) continue;
            if (Module.class.isAssignableFrom(clazz)) {
                try {
                    final Module module = (Module) clazz.newInstance();
                    addModule(module);
                } catch (InstantiationException | IllegalAccessException e) {
                    XaeroPlus.LOGGER.warn("Error initializing module class", e);
                }
            }
        }
    }

    private static void addModule(Module module) {
        modulesClassMap.put(module.getClass(), module);
    }

    public static <T extends Module> T getModule(Class<T> clazz) {
        return (T) modulesClassMap.get(clazz);
    }
}

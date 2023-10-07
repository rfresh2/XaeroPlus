package xaeroplus.module;

import xaeroplus.XaeroPlus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public abstract class Module {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ModuleInfo {
        boolean enabled() default false;
    }

    private boolean enabled = getDeclaration().enabled();

    private ModuleInfo getDeclaration() {
        return getClass().getAnnotation(ModuleInfo.class);
    }

    protected void onEnable() {

    }

    protected void onDisable() {

    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            enable();
        } else {
            disable();
        }
    }

    public void enable() {
        if (this.isEnabled()) return;
        this.enabled = true;
        try {
            onEnable();
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error enabling module: " + this.getClass().getSimpleName(), e);
        }
        XaeroPlus.EVENT_BUS.register(this);
    }

    public void disable() {
        if (!this.isEnabled()) return;
        this.enabled = false;
        XaeroPlus.EVENT_BUS.unregister(this);
        try {
            onDisable();
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error enabling module: " + this.getClass().getSimpleName(), e);
        }
    }

    public void toggle() {
        if (isEnabled()) {
            disable();
        } else if (!isEnabled()) {
            enable();
        }
    }
}

package xaeroplus.module;

import net.minecraft.client.Minecraft;
import xaeroplus.XaeroPlus;

public abstract class Module {
    private boolean enabled = false;
    public final Minecraft mc = Minecraft.getInstance();

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
        XaeroPlus.EVENT_BUS.register(this);
        try {
            onEnable();
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error enabling module: " + this.getClass().getSimpleName(), e);
        }
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
        } else {
            enable();
        }
    }
}

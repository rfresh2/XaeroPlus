package xaeroplus.mixin.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.gui.GuiWaypointsOptions;
import xaero.common.gui.MyBigButton;
import xaero.common.gui.ScreenBase;
import xaeroplus.settings.Settings;

@Mixin(value = GuiWaypointsOptions.class, remap = false)
public abstract class MixinGuiWaypointsOptions extends ScreenBase {
    @Unique private Button xaeroPlus$showWaypointDistancesButton;
    @Shadow private boolean buttonTest;

    protected MixinGuiWaypointsOptions(final IXaeroMinimap modMain, final Screen parent, final Screen escape, final Component titleIn) {
        super(modMain, parent, escape, titleIn);
    }

    @Inject(
        method = "init",
        at = @At("RETURN"),
        remap = true
    )
    public void injectShowWaypointDistancesButton(final CallbackInfo ci) {
        int prevButtonY = this.children().stream()
            .filter(c -> c instanceof MyBigButton)
            .map(c -> (MyBigButton) c)
            .filter(c -> c.getId() == 203)
            .findFirst()
            .map(AbstractWidget::getY)
            .orElse(280);
        addRenderableWidget(
            xaeroPlus$showWaypointDistancesButton = new MyBigButton(
                999,
                this.width / 2 + 3,
                prevButtonY + 25,
                xaeroPlus$getShowWaypointDistancesButtonComponent(),
                (b) -> {
                    this.buttonTest = true;
                    Settings.REGISTRY.showWaypointDistances.setValue(!Settings.REGISTRY.showWaypointDistances.get());
                    xaeroPlus$showWaypointDistancesButton.setMessage(xaeroPlus$getShowWaypointDistancesButtonComponent());
                }
            )
        );
    }

    @Unique
    private Component xaeroPlus$getShowWaypointDistancesButtonComponent() {
        return Component.literal(
            Settings.REGISTRY.showWaypointDistances.getTranslatedName()
                + ": "
                + I18n.get(Settings.REGISTRY.showWaypointDistances.get() ? "gui.xaero_on" : "gui.xaero_off"));
    }
}

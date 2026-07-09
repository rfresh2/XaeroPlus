package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.MinimapWorldManager;
import xaero.lib.client.gui.ScreenBase;
import xaero.lib.client.gui.widget.MySmallButton;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;

@Mixin(value = GuiWaypoints.class, remap = false)
public class MixinGuiWaypoints extends ScreenBase {

    private final int TOGGLE_ALL_ID = 69;
    @Shadow private MinimapWorld displayedWorld;
    @Shadow private ArrayList<Waypoint> waypointsSorted;
    @Shadow private MinimapSession session;
    @Shadow private MinimapWorldManager manager;
    @Shadow private boolean buttonClicked;
    @Shadow private ConcurrentSkipListSet<Integer> selectedListSet;

    protected MixinGuiWaypoints(final GuiScreen parent, final GuiScreen escape, final ITextComponent titleIn) {
        super(parent, escape, titleIn);
    }

    @Inject(method = "initGui()V", at = @At("TAIL"), remap = true)
    public void initGui(CallbackInfo ci) {
        this.buttonList.add(new MySmallButton(TOGGLE_ALL_ID, this.width / 2 + 213, this.height - 53, I18n.format("gui.waypoints.toggle_enable_all")));
    }

    @Inject(method = "actionPerformed", at = @At("TAIL"), remap = true)
    public void actionPerformed(GuiButton b, CallbackInfo ci) {
        if (b.enabled) {
            if (b.id == TOGGLE_ALL_ID) {
                waypointsSorted.stream().findFirst().ifPresent(firstWaypoint -> {
                    boolean firstIsEnabled = firstWaypoint.isDisabled();
                    waypointsSorted.forEach(waypoint -> waypoint.setDisabled(!firstIsEnabled));
                });
            }
        }
    }
}

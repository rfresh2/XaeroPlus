package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.teleport.MapTeleporter;
import xaero.map.world.MapWorld;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.Shared;

@Mixin(value = MapTeleporter.class, remap = false)
public class MixinMapTeleporter {
    private int dimensionId = 0;

    @Inject(method = "teleport", at = @At("HEAD"))
    public void teleportHeadInject(final GuiScreen screen, final MapWorld mapWorld, final int x, final int y, final int z, final CallbackInfo ci) {
        dimensionId = Shared.customDimensionId;
        // line below this will close GuiMap. which will reset Shared.customDimensionId if persist setting is off (default)
    }

    @Redirect(method = "teleport", at = @At(value = "INVOKE", target = "Lxaero/map/world/MapWorld;getTeleportCommandFormat()Ljava/lang/String;"))
    public String getTeleportCommandFormat(final MapWorld instance) {
        if (XaeroPlusSettingRegistry.crossDimensionTeleportCommand.getValue() && dimensionId != Minecraft.getMinecraft().world.provider.getDimension())
            return "/forge setdim " + Minecraft.getMinecraft().getSession().getUsername() + " " + dimensionId + " {x} {y} {z}";
        else
            return instance.getTeleportCommandFormat();
    }
}

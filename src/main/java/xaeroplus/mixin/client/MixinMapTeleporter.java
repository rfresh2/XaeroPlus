package xaeroplus.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.teleport.MapTeleporter;
import xaero.map.world.MapWorld;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.Shared;

import static net.minecraft.world.World.OVERWORLD;

@Mixin(value = MapTeleporter.class, remap = false)
public class MixinMapTeleporter {
    private RegistryKey<World> dimensionId = OVERWORLD;

    @Inject(method = "teleport", at = @At("HEAD"))
    public void teleportHeadInject(final Screen screen, final MapWorld mapWorld, final int x, final int y, final int z, final CallbackInfo ci) {
        dimensionId = Shared.customDimensionId;
        // line below this will close GuiMap. which will reset Shared.customDimensionId if persist setting is off (default)
    }

    @Redirect(method = "teleport", at = @At(value = "INVOKE", target = "Lxaero/map/world/MapWorld;getTeleportCommandFormat()Ljava/lang/String;"))
    public String getTeleportCommandFormat(final MapWorld instance) {
        try {
            if (XaeroPlusSettingRegistry.crossDimensionTeleportCommand.getValue() && dimensionId != MinecraftClient.getInstance().world.getRegistryKey())
                return "/execute in " + dimensionId.getValue() + " run " + instance.getTeleportCommandFormat().substring(1);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error performing cross-dimension teleport. falling back to default teleport", e);
        }
        return instance.getTeleportCommandFormat();
    }
}

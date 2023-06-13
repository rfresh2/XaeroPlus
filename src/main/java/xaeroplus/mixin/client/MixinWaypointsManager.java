package xaeroplus.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.DataFolderResolveUtil;

import static net.minecraft.world.World.NETHER;
import static net.minecraft.world.World.OVERWORLD;

@Mixin(value = WaypointsManager.class, remap = false)
public abstract class MixinWaypointsManager {

    @Shadow
    private MinecraftClient mc;
    @Shadow
    private String mainContainerID;
    @Shadow
    private String containerIDIgnoreCaseCache;

    @Shadow
    public abstract String ignoreContainerCase(String potentialContainerID, String current);

    @Shadow
    public abstract String getDimensionDirectoryName(RegistryKey<World> dimKey);

    @Inject(method = "getMainContainer", at = @At("HEAD"), cancellable = true)
    private void getMainContainer(ClientPlayNetworkHandler connection, CallbackInfoReturnable<String> cir) {
        DataFolderResolveUtil.resolveDataFolder(connection, cir);
    }

    @Inject(method = "getPotentialContainerID", at = @At("HEAD"), cancellable = true)
    private void getPotentialContainerID(CallbackInfoReturnable<String> cir) {
        if (!XaeroPlusSettingRegistry.owAutoWaypointDimension.getValue()) return;
        RegistryKey<World> dimension = mc.world.getRegistryKey();
        if (dimension == OVERWORLD || dimension == NETHER) {
            dimension = OVERWORLD;
        }
        cir.setReturnValue(this.ignoreContainerCase(
                this.mainContainerID + "/" + this.getDimensionDirectoryName(dimension), this.containerIDIgnoreCaseCache)
        );
    }
}

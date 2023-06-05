package xaeroplus.mixin.client;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.file.worldsave.WorldDataReader;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = WorldDataReader.class, remap = false)
public abstract class MixinWorldDataReader {

    @Shadow
    protected abstract boolean buildTile(
            NBTTagCompound nbttagcompound,
            MapTile tile,
            MapTileChunk tileChunk,
            int chunkX,
            int chunkZ,
            int insideRegionX,
            int insideRegionZ,
            int caveStart,
            int caveDepth,
            boolean worldHasSkylight,
            boolean ignoreHeightmaps,
            World world,
            boolean flowers
    );

    @Redirect(method = "buildTileChunk", at = @At(value = "INVOKE", target = "Lxaero/map/file/worldsave/WorldDataReader;buildTile(Lnet/minecraft/nbt/NBTTagCompound;Lxaero/map/region/MapTile;Lxaero/map/region/MapTileChunk;IIIIIIZZLnet/minecraft/world/World;Z)Z"))
    public boolean redirectBuildTile(final WorldDataReader instance, final NBTTagCompound nbtTagCompound, final MapTile tile, final MapTileChunk tileChunk, final int chunkX, final int chunkZ, final int insideRegionX, final int insideRegionZ, final int caveStart, final int caveDepth, final boolean worldHasSkylight, final boolean ignoreHeightmaps, final World world, final boolean flowers) {
        if (XaeroPlusSettingRegistry.netherCaveFix.getValue()) {
            boolean cave = caveStart != Integer.MAX_VALUE;
            boolean nether = tileChunk.getInRegion().getDim().getDimId() == -1;
            int customCaveStart = caveStart;
            if (!cave && nether) {
                customCaveStart = Integer.MIN_VALUE;
            }
            return buildTile(
                    nbtTagCompound,
                    tile,
                    tileChunk,
                    chunkX,
                    chunkZ,
                    insideRegionX,
                    insideRegionZ,
                    customCaveStart,
                    caveDepth,
                    worldHasSkylight,
                    ignoreHeightmaps,
                    world,
                    flowers);
        }
        return buildTile(
                nbtTagCompound,
                tile,
                tileChunk,
                chunkX,
                chunkZ,
                insideRegionX,
                insideRegionZ,
                caveStart,
                caveDepth,
                worldHasSkylight,
                ignoreHeightmaps,
                world,
                flowers);
    }
}

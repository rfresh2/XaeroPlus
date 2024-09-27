package xaeroplus.mixin.client;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.file.worldsave.WorldDataReader;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;
import xaeroplus.settings.Settings;

import static net.minecraft.world.level.Level.NETHER;

@Mixin(value = WorldDataReader.class, remap = false)
public abstract class MixinWorldDataReader {

    @Shadow
    protected abstract boolean buildTile(
            CompoundTag nbttagcompound,
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
            ServerLevel serverWorld,
            HolderLookup<Block> blockLookup,
            Registry<Block> blockRegistry,
            Registry<Fluid> fluidRegistry,
            Registry<Biome> biomeRegistry,
            boolean flowers,
            int worldBottomY,
            int worldTopY
    );

    @Redirect(method = "buildTileChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/file/worldsave/WorldDataReader;buildTile(Lnet/minecraft/nbt/CompoundTag;Lxaero/map/region/MapTile;Lxaero/map/region/MapTileChunk;IIIIIIZZLnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/HolderLookup;Lnet/minecraft/core/Registry;Lnet/minecraft/core/Registry;Lnet/minecraft/core/Registry;ZII)Z"),
            remap = true) // $REMAP
    public boolean redirectBuildTile(final WorldDataReader instance,
                                     CompoundTag nbttagcompound,
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
                                     ServerLevel serverWorld,
                                     HolderLookup<Block> blockLookup,
                                     Registry<Block> blockRegistry,
                                     Registry<Fluid> fluidRegistry,
                                     Registry<Biome> biomeRegistry,
                                     boolean flowers,
                                     int worldBottomY,
                                     int worldTopY) {
        if (Settings.REGISTRY.netherCaveFix.get()) {
            boolean cave = caveStart != Integer.MAX_VALUE;
            boolean nether = tileChunk.getInRegion().getDim().getDimId() == NETHER;
            int customCaveStart = caveStart;
            if (!cave && nether) {
                customCaveStart = Integer.MIN_VALUE;
            }
            return buildTile(
                    nbttagcompound,
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
                    serverWorld,
                    blockLookup,
                    blockRegistry,
                    fluidRegistry,
                    biomeRegistry,
                    flowers,
                    worldBottomY,
                    worldTopY
            );
        }
        return buildTile(
                nbttagcompound,
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
                serverWorld,
                blockLookup,
                blockRegistry,
                fluidRegistry,
                biomeRegistry,
                flowers,
                worldBottomY,
                worldTopY
        );
    }
}

package xaeroplus.module.impl;

import com.collarmc.pounce.Subscribe;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.PacketReceivedEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.HighlightAtChunkPos;
import xaeroplus.util.newchunks.NewChunksCache;
import xaeroplus.util.newchunks.NewChunksLocalCache;
import xaeroplus.util.newchunks.NewChunksSavingCache;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static xaeroplus.util.ChunkUtils.getActualDimension;
import static xaeroplus.util.ColorHelper.getColor;

@Module.ModuleInfo()
public class NewChunks extends Module {
    // todo: add newchunk detection mode setting: fluid flows (current), timing, etc.

    private NewChunksCache newChunksCache = new NewChunksLocalCache();
    private final Cache<Long, Byte> oldChunksCache = Caffeine.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(10L, TimeUnit.SECONDS)
            .build();
    private int newChunksColor = getColor(255, 0, 0, 100);
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Direction[] searchDirs = new Direction[] { Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.UP };

    public void setNewChunksCache(boolean disk) {
        try {
            Long2LongOpenHashMap map = newChunksCache.getNewChunksState();
            newChunksCache.onDisable();
            if (disk) {
                newChunksCache = new NewChunksSavingCache();
            } else {
                newChunksCache = new NewChunksLocalCache();
            }
            if (this.isEnabled()) {
                newChunksCache.onEnable();
                newChunksCache.loadPreviousState(map);
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error closing new chunks cache", e);
        }
    }

    @Subscribe
    public void onPacketReceivedEvent(final PacketReceivedEvent event) {
        if (mc.world == null) return;
        // credits to BleachHack for this fluid flow based detection method
        if (event.packet() instanceof ChunkDeltaUpdateS2CPacket) {
            ChunkDeltaUpdateS2CPacket packet = (ChunkDeltaUpdateS2CPacket) event.packet();

            packet.visitUpdates((pos, state) -> {
                if (!state.getFluidState().isEmpty() && !state.getFluidState().isStill()) {
                    ChunkPos chunkPos = new ChunkPos(pos);
                    for (Direction dir: searchDirs) {
                        if (mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill()
                                && oldChunksCache.getIfPresent(ChunkUtils.chunkPosToLong(chunkPos)) == null) {
                            newChunksCache.addNewChunk(chunkPos.x, chunkPos.z);
                            return;
                        }
                    }
                }
            });
        } else if (event.packet() instanceof BlockUpdateS2CPacket) {
            BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket) event.packet();

            if (!packet.getState().getFluidState().isEmpty() && !packet.getState().getFluidState().isStill()) {
                ChunkPos chunkPos = new ChunkPos(packet.getPos());

                for (Direction dir: searchDirs) {
                    if (mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill()
                            && oldChunksCache.getIfPresent(ChunkUtils.chunkPosToLong(chunkPos)) == null) {
                        newChunksCache.addNewChunk(chunkPos.x, chunkPos.z);
                        return;
                    }
                }
            }
        } else if (event.packet() instanceof ChunkDataS2CPacket) {
            ChunkDataS2CPacket packet = (ChunkDataS2CPacket) event.packet();

            ChunkPos pos = new ChunkPos(packet.getX(), packet.getZ());

            if (!newChunksCache.isNewChunk(pos.x, pos.z, getActualDimension()) && mc.world.getChunkManager().getChunk(packet.getX(), packet.getZ()) == null) {
                WorldChunk chunk = new WorldChunk(mc.world, pos);
                try {
                    chunk.loadFromPacket(packet.getChunkData().getSectionsDataBuf(), new NbtCompound(), packet.getChunkData().getBlockEntities(packet.getX(), packet.getZ()));
                } catch (ArrayIndexOutOfBoundsException e) {
                    return;
                }

                for (int x = 0; x < 16; x++) {
                    for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
                        for (int z = 0; z < 16; z++) {
                            FluidState fluid = chunk.getFluidState(x, y, z);

                            if (!fluid.isEmpty() && !fluid.isStill()) {
                                oldChunksCache.put(ChunkUtils.chunkPosToLong(pos), (byte) 0);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @Subscribe
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        newChunksCache.handleWorldChange();
    }

    @Subscribe
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        newChunksCache.handleTick();
    }

    @Override
    public void onEnable() {
        newChunksCache.onEnable();
    }

    @Override
    public void onDisable() {
        newChunksCache.onDisable();
    }

    public int getNewChunksColor() {
        return newChunksColor;
    }

    public void setRgbColor(final int color) {
        newChunksColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.newChunksAlphaSetting.getValue());
    }

    public void setAlpha(final float a) {
        newChunksColor = ColorHelper.getColorWithAlpha(newChunksColor, (int) (a));
    }

    public List<HighlightAtChunkPos> getNewChunksInRegion(
            final int leafRegionX, final int leafRegionZ,
            final int level,
            final RegistryKey<World> dimension) {
        return newChunksCache.getNewChunksInRegion(leafRegionX, leafRegionZ, level, dimension);
    }

    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final RegistryKey<World> dimensionId) {
        return newChunksCache.isNewChunk(chunkPosX, chunkPosZ, dimensionId);
    }
}

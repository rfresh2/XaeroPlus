package xaeroplus.util;

import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

public class ChunkScanner {

    public static boolean chunkContainsBlocks(ChunkAccess chunk, ReferenceSet<Block> filter, int yLevelMin) {
        final LevelChunkSection[] sectionArray = chunk.getSections();
        for (int i = 0; i < sectionArray.length; i++) {
            var sectionBottomY = chunk.getMinBuildHeight() + (i * 16);
            if (yLevelMin > sectionBottomY + 15) continue;
            int yScanStart = yLevelMin > sectionBottomY ? yLevelMin % 16 : 0;
            final LevelChunkSection section = sectionArray[i];
            if (section == null || section.hasOnlyAir()) continue;
            final PalettedContainer<BlockState> blockStateContainer = section.getStates();
            final PalettedContainer.Data<BlockState> paletteData = blockStateContainer.data;
            final BitStorage array = paletteData.storage();
            if (array == null) continue;
            if (!blockStateContainer.maybeHas(bs -> filter.contains(bs.getBlock()))) continue;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = yScanStart; y < 16; y++) {
                        BlockState state = blockStateContainer.get(x, y, z);
                        if (filter.contains(state.getBlock())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static void chunkScanBlockstatePredicate(ChunkAccess chunk, ReferenceSet<Block> filter, BlockStateScanPredicate statePredicate, int yLevelMin) {
        final LevelChunkSection[] sectionArray = chunk.getSections();
        for (int i = 0; i < sectionArray.length; i++) {
            var sectionBottomY = chunk.getMinBuildHeight() + (i * 16);
            if (yLevelMin > sectionBottomY + 15) continue;
            int yScanStart = yLevelMin > sectionBottomY ? yLevelMin % 16 : 0;
            final LevelChunkSection section = sectionArray[i];
            if (section == null || section.hasOnlyAir()) continue;
            final PalettedContainer<BlockState> blockStateContainer = section.getStates();
            final PalettedContainer.Data<BlockState> paletteData = blockStateContainer.data;
            final BitStorage array = paletteData.storage();
            if (array == null) continue;
            if (!blockStateContainer.maybeHas(bs -> filter.contains(bs.getBlock()))) continue;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = yScanStart; y < 16; y++) {
                        BlockState state = blockStateContainer.get(x, y, z);
                        if (statePredicate.test(chunk, state, x, (i*16) + y, z))
                            return;
                    }
                }
            }
        }
    }

    public interface BlockStateScanPredicate {
        boolean test(ChunkAccess chunkAccess, BlockState state, int relativeX, int y, int relativeZ);
    }
}

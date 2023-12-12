package xaeroplus.util;

import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;

public class ChunkScanner {

    public static boolean chunkContainsBlocks(Chunk chunk, ReferenceSet<Block> filter, int yLevelMin) {
        final ChunkSection[] sectionArray = chunk.getSectionArray();
        for (int i = 0; i < sectionArray.length; i++) {
            var sectionBottomY = chunk.getBottomY() + (i * 16);
            if (yLevelMin > sectionBottomY + 15) continue;
            int yScanStart = yLevelMin > sectionBottomY ? yLevelMin % 16 : 0;
            final ChunkSection section = sectionArray[i];
            if (section == null || section.isEmpty()) continue;
            final PalettedContainer<BlockState> blockStateContainer = section.getBlockStateContainer();
            final PalettedContainer.Data<BlockState> paletteData = blockStateContainer.data;
            final PaletteStorage array = paletteData.storage();
            if (array == null) continue;
            if (!blockStateContainer.hasAny(bs -> filter.contains(bs.getBlock()))) continue;
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
}

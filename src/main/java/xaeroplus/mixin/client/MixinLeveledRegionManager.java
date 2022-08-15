package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.region.LeveledRegion;
import xaero.map.region.LeveledRegionManager;
import xaero.map.region.MapRegion;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import static xaeroplus.XaeroPlus.MAX_LEVEL;

@Mixin(value = LeveledRegionManager.class, remap = false)
public class MixinLeveledRegionManager {

    @Shadow
    private HashMap<Integer, HashMap<Integer, LeveledRegion<?>>> regionTextureMap;
    @Shadow
    private List<LeveledRegion<?>> regionsListAll;
    @Shadow
    private List<LeveledRegion<?>> regionsListLoaded;

    private static final Method leveledRegionPutLeafMethod;
    private static final Method leveledRegionGetMethod;
    private static final Method levelRegionRemoveMethod;
    static {
        try {
            leveledRegionPutLeafMethod = LeveledRegion.class.getDeclaredMethod("putLeaf", int.class, int.class, MapRegion.class);
            leveledRegionPutLeafMethod.setAccessible(true);
            leveledRegionGetMethod = LeveledRegion.class.getDeclaredMethod("get", int.class, int.class, int.class);
            leveledRegionGetMethod.setAccessible(true);
            levelRegionRemoveMethod = LeveledRegion.class.getDeclaredMethod("remove", int.class, int.class, int.class);
            levelRegionRemoveMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @author rfresh2
     * @reason Replace constants regarding max cache level with variable
     */
    @Overwrite
    public void putLeaf(int X, int Z, MapRegion leaf) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int maxLevelX = X >> MAX_LEVEL;
        int maxLevelZ = Z >> MAX_LEVEL;
        HashMap column;
        synchronized(this.regionTextureMap) {
            column = this.regionTextureMap.get(maxLevelX);
            if (column == null) {
                column = new HashMap();
                this.regionTextureMap.put(maxLevelX, column);
            }
        }

        Object rootBranch;
        synchronized(column) {
            rootBranch = column.get(maxLevelZ);
            if (rootBranch == null) {
                rootBranch = new BranchLeveledRegion(leaf.getWorldId(), leaf.getDimId(), leaf.getMwId(), leaf.getDim(), MAX_LEVEL, maxLevelX, maxLevelZ, null);
                column.put(maxLevelZ, rootBranch);
            }
        }

        if (!(rootBranch instanceof MapRegion)) {
            // lord forgive me for i have sinned
            // accessors bugging tf out here for some reason
            leveledRegionPutLeafMethod.invoke(rootBranch, X, Z, leaf);
//            ((LeveledRegion)rootBranch).putLeaf(X, Z, leaf);
        }
    }

    /**
     * @author rfresh2
     * @reason Replace constants regarding max cache level with variable
     */
    @Overwrite
    public LeveledRegion<?> get(int leveledX, int leveledZ, int level) throws InvocationTargetException, IllegalAccessException {
        if (level > MAX_LEVEL) {
            throw new RuntimeException(new IllegalArgumentException());
        } else {
            int maxLevelX = leveledX >> MAX_LEVEL - level;
            int maxLevelZ = leveledZ >> MAX_LEVEL - level;
            HashMap column;
            synchronized(this.regionTextureMap) {
                column = this.regionTextureMap.get(maxLevelX);
            }

            if (column == null) {
                return null;
            } else {
                LeveledRegion rootBranch;
                synchronized(column) {
                    rootBranch = (LeveledRegion)column.get(maxLevelZ);
                }

                if (rootBranch == null) {
                    return null;
                } else {
                    return level == MAX_LEVEL
                            ? rootBranch
                            : (LeveledRegion<?>) leveledRegionGetMethod.invoke(rootBranch, leveledX, leveledZ, level);
                }
            }
        }
    }

    /**
     * @author rfresh2
     * @reason Replace constants regarding max cache level with variable
     */
    @Overwrite
    public boolean remove(int leveledX, int leveledZ, int level) throws InvocationTargetException, IllegalAccessException {
        if (level > MAX_LEVEL) {
            throw new RuntimeException(new IllegalArgumentException());
        } else {
            int maxLevelX = leveledX >> MAX_LEVEL - level;
            int maxLevelZ = leveledZ >> MAX_LEVEL - level;
            HashMap column;
            synchronized(this.regionTextureMap) {
                column = (HashMap)this.regionTextureMap.get(maxLevelX);
            }

            if (column == null) {
                return false;
            } else {
                LeveledRegion rootBranch;
                synchronized(column) {
                    rootBranch = (LeveledRegion)column.get(maxLevelZ);
                }

                if (rootBranch == null) {
                    return false;
                } else if (!(rootBranch instanceof MapRegion)) {
                    return (boolean) levelRegionRemoveMethod.invoke(rootBranch, leveledX, leveledZ, level);
                } else {
                    synchronized(column) {
                        column.remove(maxLevelZ);
                        return true;
                    }
                }
            }
        }
    }

}

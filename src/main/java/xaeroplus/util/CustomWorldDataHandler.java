package xaeroplus.util;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.nio.file.Path;

public interface CustomWorldDataHandler {
    Path getWorldDir(RegistryKey<World> dimId);
}

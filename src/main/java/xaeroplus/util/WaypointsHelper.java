package xaeroplus.util;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class WaypointsHelper {
    // nullable
    public static RegistryKey<World> getDimensionKeyForWaypointWorldKey(String waypointWorldKey) {
        String dimIdPart = waypointWorldKey.substring(waypointWorldKey.lastIndexOf(47) + 1).substring(4);
        if (dimIdPart.equals("0")) {
            return World.OVERWORLD;
        } else if (dimIdPart.equals("1")) {
            return World.END;
        } else if (dimIdPart.equals("-1")) {
            return World.NETHER;
        } else {
            String[] idArgs = dimIdPart.split("\\$");
            if (idArgs.length == 1) {
                return null;
            } else {
                try {
                    Integer.parseInt(idArgs[1]);
                    return null;
                } catch (NumberFormatException var5) {
                    return RegistryKey.of(RegistryKeys.WORLD, new Identifier(idArgs[0], idArgs[1].replace('%', '/')));
                }
            }
        }
    }

    public static int getDimensionForWaypointWorldKey(final String waypointWorldKey) {
        String dimIdPart = waypointWorldKey.substring(waypointWorldKey.lastIndexOf(47) + 1).substring(4);
        if (!dimIdPart.matches("-{0,1}[0-9]+")) {
            return Integer.MIN_VALUE;
        } else {
            int dimId;
            try {
                dimId = Integer.parseInt(dimIdPart);
            } catch (NumberFormatException var5) {
                return Integer.MIN_VALUE;
            }
            return dimId;
        }
    }
}

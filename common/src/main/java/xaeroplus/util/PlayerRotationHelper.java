package xaeroplus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class PlayerRotationHelper {

    public static void rotatePlayerTo(int x, int y, int z) {
        var mc = Minecraft.getInstance();
        if (!mc.isSameThread()) {
            throw new RuntimeException("rotatePlayerTo must be called on the main thread!");
        }
        var player = mc.player;
        if (player != null) {
            var pitch = calculatePitchTo(x, y, z);
            player.setXRot(pitch);
            var yaw = calculateYawTo(x, z);
            player.setYRot(yaw);
            player.setYHeadRot(yaw);
        }
    }

    public static void rotatePlayerTo(int x, int z) {
        var mc = Minecraft.getInstance();
        if (!mc.isSameThread()) {
            throw new RuntimeException("rotatePlayerTo must be called on the main thread!");
        }
        var player = mc.player;
        if (player != null) {
            var yaw = calculateYawTo(x, z);
            player.setYRot(yaw);
            player.setYHeadRot(yaw);
        }
    }

    static float calculateYawTo(int x, int z) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        double dx = x - player.getX();
        double dz = z - player.getZ();
        return Mth.wrapDegrees((float)(Mth.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F);
    }

    static float calculatePitchTo(int x, int y, int z) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        double dx = x - player.getX();
        double dy = y - player.getEyeY();
        double dz = z - player.getZ();
        double hDist = Math.sqrt(dx * dx + dz * dz);
        return Mth.wrapDegrees((float)(-(Mth.atan2(dy, hDist) * 180.0F / (float)Math.PI)));
    }
}

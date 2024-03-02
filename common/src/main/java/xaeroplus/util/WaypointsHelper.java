package xaeroplus.util;

public class WaypointsHelper {
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

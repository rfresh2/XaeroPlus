package xaeroplus.fabric.util.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import xaeroplus.XaeroPlus;

import java.util.Optional;

public class XaeroPlusMinimapCompatibilityChecker {

    public static VersionCheckResult versionCheckResult = XaeroPlusMinimapCompatibilityChecker.versionCheck();

    private static VersionCheckResult versionCheck() {
        try {
            var compatibleMinimapVersion = SemanticVersion.parse(XaeroPlusMinimapCompatibilityChecker.getCompatibleMinimapVersion());
            var minimapVersion = XaeroPlusMinimapCompatibilityChecker.getVersion("xaerominimap");
            var betterPvpVersion = XaeroPlusMinimapCompatibilityChecker.getVersion("xaerobetterpvp");
            return new VersionCheckResult(minimapVersion, betterPvpVersion, compatibleMinimapVersion);
        } catch (VersionParsingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<Version> getVersion(final String modId) {
        try {
            return FabricLoader.getInstance().getAllMods().stream()
                .filter(modContainer -> modContainer.getMetadata().getId().equals(modId))
                .map(modContainer -> modContainer.getMetadata().getVersion())
                .map(ver -> {
                    try {
                        return Version.parse(ver.getFriendlyString());
                    } catch (VersionParsingException e) {
                        return null;
                    }
                })
                .findFirst();
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to check version for {}", modId, e);
            return Optional.empty();
        }
    }

    private static String getCompatibleMinimapVersion() {
        return FabricLoader.getInstance().getModContainer("xaeroplus").get().getMetadata().getCustomValue("minimap_version").getAsString();
    }
}

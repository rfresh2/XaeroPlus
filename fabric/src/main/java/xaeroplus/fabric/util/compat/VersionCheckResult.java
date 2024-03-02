package xaeroplus.fabric.util.compat;

import net.fabricmc.loader.api.Version;

import java.util.Optional;

public record VersionCheckResult(
    Optional<Version> minimapVersion,
    Optional<Version> betterPvpVersion,
    Version expectedVersion
) {
    public boolean minimapCompatible() {
        var anyVersion = anyPresentMinimapVersion();
        return anyVersion.isPresent() && anyVersion.get().compareTo(expectedVersion()) == 0;
    }

    public Optional<Version> anyPresentMinimapVersion() {
        return minimapVersion().or(this::betterPvpVersion);
    }
}

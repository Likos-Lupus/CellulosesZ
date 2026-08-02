package top.likoslupus.cellulosesz.modules.teleport.persistence;

import top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettings;

import static java.util.Objects.requireNonNull;

public final class RandomTeleportSettingsMapper {

    private RandomTeleportSettingsMapper() {
        throw new AssertionError("No instances");
    }

    public static RandomTeleportSettings toDomain(
            RandomTeleportSettingDocument document,
            String world
    ) {
        requireNonNull(document, "document");
        try {
            return new RandomTeleportSettings(
                    document.centerX,
                    document.centerZ,
                    document.minRadius,
                    document.maxRadius
            );
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException(
                    "Invalid random teleport settings for world " + world,
                    failure
            );
        }
    }

    public static RandomTeleportSettingDocument fromDomain(RandomTeleportSettings settings) {
        requireNonNull(settings, "settings");
        var document = new RandomTeleportSettingDocument();
        document.centerX = settings.centerX();
        document.centerZ = settings.centerZ();
        document.minRadius = settings.minRadius();
        document.maxRadius = settings.maxRadius();
        return document;
    }

}

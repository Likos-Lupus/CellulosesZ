package top.likoslupus.cellulosesz.modules.teleport.persistence;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import static java.util.Objects.requireNonNull;

public final class LocationMapper {

    private LocationMapper() {
        throw new AssertionError("No instances");
    }

    public static CellLocation toDomain(LocationDocument document, String context) {
        requireNonNull(document, "document");
        try {
            return new CellLocation(
                    requireNonNull(document.world, context + ".world"),
                    document.x,
                    document.y,
                    document.z,
                    document.yaw,
                    document.pitch
            );
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Invalid persisted location " + context, failure);
        }
    }

    public static LocationDocument fromDomain(CellLocation location) {
        requireNonNull(location, "location");
        var document = new LocationDocument();
        document.world = location.world();
        document.x = location.x();
        document.y = location.y();
        document.z = location.z();
        document.yaw = location.yaw();
        document.pitch = location.pitch();
        return document;
    }

    public static LocationDocument copy(LocationDocument source) {
        var copy = new LocationDocument();
        copy.world = source.world;
        copy.x = source.x;
        copy.y = source.y;
        copy.z = source.z;
        copy.yaw = source.yaw;
        copy.pitch = source.pitch;
        return copy;
    }

}

package top.likoslupus.cellulosesz.modules.home.persistence;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class HomeMapper {

    private HomeMapper() {
        throw new AssertionError("No instances");
    }

    public static UUID uuid(HomeDocument document) {
        requireNonNull(document, "document");
        try {
            return UUID.fromString(requireNonNull(document.uuid, "uuid"));
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Invalid home document UUID", failure);
        }
    }

    public static Map<String, CellLocation> homes(HomeDocument document) {
        var mapped = new LinkedHashMap<String, CellLocation>();
        requireNonNull(document.homes, "homes").forEach((name, location) ->
                mapped.put(name, toDomain(location, name))
        );
        return Map.copyOf(mapped);
    }

    public static CellLocation toDomain(LocationDocument document, String context) {
        requireNonNull(document, "location");
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
            throw new IllegalArgumentException("Invalid home location " + context, failure);
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

    public static HomeDocument empty(UUID uuid) {
        var document = new HomeDocument();
        document.uuid = requireNonNull(uuid, "uuid").toString();
        return document;
    }

    public static HomeDocument copy(HomeDocument source) {
        var copy = new HomeDocument();
        copy.uuid = source.uuid;
        requireNonNull(source.homes, "homes").forEach((name, location) -> {
            var cloned = new LocationDocument();
            cloned.world = location.world;
            cloned.x = location.x;
            cloned.y = location.y;
            cloned.z = location.z;
            cloned.yaw = location.yaw;
            cloned.pitch = location.pitch;
            copy.homes.put(name, cloned);
        });

        return copy;
    }

}

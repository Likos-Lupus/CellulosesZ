package top.likoslupus.cellulosesz.modules.warp.persistence;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.warp.Warp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/** Explicit conversion between persisted warp documents and domain values. */
public final class WarpMapper {

    private WarpMapper() {
        throw new AssertionError("No instances");
    }

    public static Warp toDomain(WarpDocument document) {
        requireNonNull(document, "document");
        try {
            var location = requireNonNull(document.location, "location");
            return new Warp(
                    requireNonNull(document.name, "name"),
                    requireNonNull(document.displayName, "displayName"),
                    new BigDecimal(requireNonNull(document.cost, "cost")),
                    new CellLocation(
                            requireNonNull(location.world, "location.world"),
                            location.x,
                            location.y,
                            location.z,
                            location.yaw,
                            location.pitch
                    ),
                    Optional.ofNullable(document.createdBy).map(UUID::fromString),
                    Instant.ofEpochMilli(document.createdAt)
            );
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException(
                    "Invalid persisted warp document " + document.name,
                    failure
            );
        }
    }

    public static WarpDocument fromDomain(Warp warp) {
        requireNonNull(warp, "warp");
        var document = new WarpDocument();
        document.name = warp.name();
        document.displayName = warp.displayName();
        document.cost = warp.cost().toPlainString();
        var location = new LocationDocument();
        location.world = warp.location().world();
        location.x = warp.location().x();
        location.y = warp.location().y();
        location.z = warp.location().z();
        location.yaw = warp.location().yaw();
        location.pitch = warp.location().pitch();
        document.location = location;
        document.createdBy = warp.createdBy().map(UUID::toString).orElse(null);
        document.createdAt = warp.createdAt().toEpochMilli();
        return document;
    }

}

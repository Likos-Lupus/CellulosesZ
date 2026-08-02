package top.likoslupus.cellulosesz.modules.admin.persistence;

import top.likoslupus.cellulosesz.api.admin.Expiration;
import top.likoslupus.cellulosesz.api.admin.Jail;
import top.likoslupus.cellulosesz.api.admin.JailState;
import top.likoslupus.cellulosesz.api.admin.JailedPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class JailMapper {

    private JailMapper() {
    }

    public static Jail toDomain(JailDocument.JailEntry source) {
        requireNonNull(source, "source");
        try {
            return new Jail(
                    source.name,
                    toDomain(source.location),
                    source.createdBy,
                    Instant.ofEpochMilli(source.createdAt)
            );
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Invalid persisted jail: " + source.name, failure);
        }
    }

    private static CellLocation toDomain(LocationDocument source) {
        requireNonNull(source, "location");
        return new CellLocation(
                source.world,
                source.x, source.y, source.z,
                source.yaw, source.pitch
        );
    }

    public static JailDocument.JailEntry fromDomain(Jail source) {
        requireNonNull(source, "source");
        var target = new JailDocument.JailEntry();
        target.name = source.name();
        target.location = fromDomain(source.location());
        target.createdBy = source.createdBy();
        target.createdAt = source.createdAt().toEpochMilli();
        return target;
    }

    private static LocationDocument fromDomain(CellLocation source) {
        requireNonNull(source, "location");
        var target = new LocationDocument();
        target.world = source.world();
        target.x = source.x();
        target.y = source.y();
        target.z = source.z();
        target.yaw = source.yaw();
        target.pitch = source.pitch();
        return target;
    }

    public static JailedPlayer toDomain(JailDocument.JailedEntry source) {
        requireNonNull(source, "source");
        try {
            var expiration = source.permanent
                    ? Expiration.permanent()
                    : Expiration.at(Instant.ofEpochMilli(source.expiresAt));
            var returnLocation = source.hasReturnLocation
                    ? Optional.of(toDomain(source.returnLocation))
                    : Optional.<CellLocation>empty();
            return new JailedPlayer(
                    UUID.fromString(source.uuid),
                    source.name,
                    source.jail,
                    source.reason,
                    source.actorName,
                    Instant.ofEpochMilli(source.createdAt),
                    expiration,
                    returnLocation,
                    JailState.valueOf(source.state)
            );
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException(
                    "Invalid persisted jailed player: " + source.uuid,
                    failure
            );
        }
    }

    public static JailDocument.JailedEntry fromDomain(JailedPlayer source) {
        requireNonNull(source, "source");
        var target = new JailDocument.JailedEntry();
        target.uuid = source.uuid().toString();
        target.name = source.name();
        target.jail = source.jail();
        target.reason = source.reason();
        target.actorName = source.actor();
        target.createdAt = source.createdAt().toEpochMilli();
        target.permanent = source.expiration() instanceof Expiration.Permanent;
        target.expiresAt = source.expiration().expiresAt().map(Instant::toEpochMilli).orElse(0L);
        target.hasReturnLocation = source.returnLocation().isPresent();
        source.returnLocation().ifPresent(location -> target.returnLocation = fromDomain(location));
        target.state = source.state().name();
        return target;
    }

    public static JailDocument copy(JailDocument source) {
        requireNonNull(source, "source");
        var target = new JailDocument();
        source.jails.forEach(value -> target.jails.add(copy(value)));
        source.jailed.forEach(value -> target.jailed.add(copy(value)));
        return target;
    }

    private static JailDocument.JailEntry copy(JailDocument.JailEntry source) {
        var target = new JailDocument.JailEntry();
        target.name = source.name;
        target.location = copy(source.location);
        target.createdBy = source.createdBy;
        target.createdAt = source.createdAt;
        return target;
    }

    private static JailDocument.JailedEntry copy(JailDocument.JailedEntry source) {
        var target = new JailDocument.JailedEntry();
        target.uuid = source.uuid;
        target.name = source.name;
        target.jail = source.jail;
        target.reason = source.reason;
        target.actorUuid = source.actorUuid;
        target.actorName = source.actorName;
        target.createdAt = source.createdAt;
        target.permanent = source.permanent;
        target.expiresAt = source.expiresAt;
        target.returnLocation = copy(source.returnLocation);
        target.hasReturnLocation = source.hasReturnLocation;
        target.state = source.state;
        return target;
    }

    private static LocationDocument copy(LocationDocument source) {
        requireNonNull(source, "location");
        var target = new LocationDocument();
        target.world = source.world;
        target.x = source.x;
        target.y = source.y;
        target.z = source.z;
        target.yaw = source.yaw;
        target.pitch = source.pitch;
        return target;
    }

}

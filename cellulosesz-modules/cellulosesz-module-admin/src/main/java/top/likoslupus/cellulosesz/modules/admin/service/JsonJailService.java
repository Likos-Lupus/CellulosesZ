package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.Jail;
import top.likoslupus.cellulosesz.api.admin.JailService;
import top.likoslupus.cellulosesz.api.admin.JailedPlayer;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.modules.admin.data.JailDocument;

import java.nio.file.Path;
import java.util.*;

public final class JsonJailService implements JailService {

    private final StorageService storage;
    private final Path path;
    private final PlatformService platform;
    private final JailDocument document;

    public JsonJailService(
            StorageService storage,
            Path path,
            PlatformService platform
    ) {
        this.storage = storage;
        this.path = path;
        this.platform = platform;
        this.document = storage.load(path, JailDocument.class, JailDocument::new).join();
    }

    @Override
    public AdminResult setJail(
            String name,
            CellLocation location,
            String actor
    ) {
        var jail = new Jail();
        jail.name = normalize(name);
        jail.location = location;
        jail.createdBy = actor;
        jail.createdAt = System.currentTimeMillis();
        document.jails.removeIf(existing -> existing.name.equalsIgnoreCase(jail.name));
        document.jails.add(jail);
        save();

        return AdminResult.success(
                "service.admin.jail-set",
                Map.of("jail", jail.name)
        );
    }

    @Override
    public AdminResult deleteJail(String name) {
        var normalized = normalize(name);
        var removed = document.jails.removeIf(jail -> jail.name.equalsIgnoreCase(normalized));
        document.jailed.removeIf(jailed -> jailed.jail.equalsIgnoreCase(normalized));
        save();

        return removed ? AdminResult.success(
                "service.admin.jail-deleted",
                Map.of("jail", normalized)
        ) : AdminResult.failure(
                "service.admin.jail-not-found",
                Map.of("jail", normalized)
        );
    }

    @Override
    public Optional<Jail> jail(String name) {
        var normalized = normalize(name);
        return document.jails.stream()
                .filter(jail -> jail.name.equalsIgnoreCase(normalized))
                .findFirst();
    }

    @Override
    public Collection<Jail> jails() {
        return ListCopy.copy(document.jails);
    }

    @Override
    public AdminResult jailPlayer(
            CellPlayer player,
            String jailName,
            String actor,
            Long durationMillis,
            String reason
    ) {
        purgeExpired();
        var jail = jail(jailName);
        if (jail.isEmpty()) return AdminResult.failure(
                "service.admin.jail-not-found",
                Map.of("jail", jailName)
        );

        var record = new JailedPlayer();
        record.uuid = player.uuid();
        record.name = player.name();
        record.jail = jail.get().name;
        record.actor = actor;
        record.reason = reason;
        record.createdAt = System.currentTimeMillis();
        record.expiresAt = durationMillis <= 0L
                ? null
                : record.createdAt + durationMillis;
        document.jailed.removeIf(existing -> player.uuid().equals(existing.uuid));
        document.jailed.add(record);
        save();

        platform.teleport(player, jail.get().location);
        return AdminResult.success(
                "service.admin.player-jailed",
                Map.of(
                        "player", player.name(),
                        "jail", jail.get().name
                )
        );
    }

    @Override
    public AdminResult unjail(
            UUID uuid,
            String name,
            String actor
    ) {
        var removed = document.jailed.removeIf(jailed -> uuid.equals(jailed.uuid));
        save();
        return removed ? AdminResult.success(
                "service.admin.player-unjailed",
                Map.of("player", name)
        ) : AdminResult.failure(
                "service.admin.player-not-jailed",
                Map.of("player", name)
        );
    }

    @Override
    public Optional<JailedPlayer> jailed(UUID uuid) {
        purgeExpired();
        return document.jailed.stream()
                .filter(record -> uuid.equals(record.uuid))
                .findFirst();
    }

    @Override
    public Collection<JailedPlayer> jailedPlayers() {
        purgeExpired();
        return ListCopy.copy(document.jailed);
    }

    @Override
    public void purgeExpired() {
        var now = System.currentTimeMillis();
        if (document.jailed.removeIf(record -> record.expired(now))) save();
    }

    private String normalize(String name) {
        return name.trim().toLowerCase();
    }

    private void save() {
        storage.save(path, document);
    }

    private static final class ListCopy {

        static <T> Collection<T> copy(Collection<T> values) {
            return List.copyOf(values);
        }

    }

}

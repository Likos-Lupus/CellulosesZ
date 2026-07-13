package top.likoslupus.cellulosesz.modules.admin.service;

import org.jspecify.annotations.Nullable;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.Jail;
import top.likoslupus.cellulosesz.api.admin.JailService;
import top.likoslupus.cellulosesz.api.admin.JailedPlayer;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;
import top.likoslupus.cellulosesz.modules.admin.data.JailDocument;

import java.nio.file.Path;
import java.util.*;

public final class JsonJailService implements JailService {

    private final StorageService storage;
    private final Path path;
    private final PlatformService platform;
    private final AdminConfig config;
    private final JailDocument document;

    public JsonJailService(
            StorageService storage,
            Path path,
            PlatformService platform,
            AdminConfig config
    ) {
        this.storage = storage;
        this.path = path;
        this.platform = platform;
        this.config = config;
        this.document = storage.load(path, JailDocument.class, JailDocument::new).join();
    }

    @Override
    public synchronized AdminResult setJail(
            String name,
            CellLocation location,
            String actor
    ) {
        var jail = new Jail();
        jail.name = normalize(name);
        jail.location = location;
        jail.createdBy = actor;
        jail.createdAt = System.currentTimeMillis();

        var previous = List.copyOf(document.jails);
        document.jails.removeIf(existing -> existing.name.equalsIgnoreCase(jail.name));
        document.jails.add(jail);
        if (!save()) {
            restore(document.jails, previous);
            return AdminResult.failure("service.admin.persistence-failed");
        }

        return AdminResult.success(
                "service.admin.jail-set",
                Map.of("jail", jail.name)
        );
    }

    @Override
    public synchronized AdminResult deleteJail(String name) {
        var normalized = normalize(name);
        if (document.jails.stream()
                .noneMatch(jail -> jail.name.equalsIgnoreCase(normalized))
        ) {
            return AdminResult.failure(
                    "service.admin.jail-not-found",
                    Map.of("jail", normalized)
            );
        }

        var previousJails = List.copyOf(document.jails);
        var previousJailed = List.copyOf(document.jailed);
        var previousExpiry = new IdentityHashMap<JailedPlayer, OptionalLong>();
        document.jailed.forEach(record -> previousExpiry.put(
                record,
                record.expiresAt == null
                        ? OptionalLong.empty()
                        : OptionalLong.of(record.expiresAt)
        ));

        document.jails.removeIf(jail -> jail.name.equalsIgnoreCase(normalized));
        var affected = document.jailed.stream()
                .filter(jailed -> jailed.jail.equalsIgnoreCase(normalized))
                .toList();
        affected.forEach(record -> record.expiresAt = 0L);

        // Persist the non-jailed/pending-release state before moving any player. A failed cleanup save may leave
        // an already released player with an expired record, but it can no longer be confined and purgeExpired
        // can safely retry the idempotent release later.
        if (!save()) {
            previousExpiry.forEach((record, expiresAt) ->
                    record.expiresAt = expiresAt.isPresent()
                            ? expiresAt.getAsLong()
                            : null
            );
            restore(document.jails, previousJails);
            restore(document.jailed, previousJailed);
            return AdminResult.failure("service.admin.persistence-failed");
        }

        var pendingSnapshot = List.copyOf(document.jailed);
        var outcomes = affected.stream()
                .map(record -> new ReleaseAttempt(record, releasePlayer(record)))
                .toList();
        var released = outcomes.stream()
                .filter(attempt -> attempt.outcome() == ReleaseOutcome.RELEASED)
                .map(ReleaseAttempt::record)
                .toList();
        document.jailed.removeAll(released);
        if (!released.isEmpty() && !save()) {
            restore(document.jailed, pendingSnapshot);
            return AdminResult.failure("service.admin.persistence-failed");
        }

        var pending = outcomes.stream()
                .filter(attempt -> attempt.outcome() != ReleaseOutcome.RELEASED)
                .count();
        if (pending > 0L) {
            return AdminResult.failure(
                    "service.admin.jail-delete-release-pending",
                    Map.of("jail", normalized, "count", pending)
            );
        }
        return AdminResult.success(
                "service.admin.jail-deleted",
                Map.of("jail", normalized)
        );
    }

    @Override
    public synchronized Optional<Jail> jail(String name) {
        var normalized = normalize(name);
        return document.jails.stream()
                .filter(jail -> jail.name.equalsIgnoreCase(normalized))
                .findFirst();
    }

    @Override
    public synchronized Collection<Jail> jails() {
        return List.copyOf(document.jails);
    }

    @Override
    public synchronized AdminResult jailPlayer(
            CellPlayer player,
            String jailName,
            String actor,
            @Nullable Long durationMillis,
            String reason
    ) {
        purgeExpired();
        var jail = jail(jailName);
        if (jail.isEmpty()) {
            return AdminResult.failure(
                    "service.admin.jail-not-found",
                    Map.of("jail", jailName)
            );
        }

        var beforeTeleport = platform.location(player);
        if (!teleport(player, jail.orElseThrow().location)) {
            return AdminResult.failure(
                    "service.admin.jail-teleport-failed",
                    Map.of("player", player.name())
            );
        }

        var previousRecords = List.copyOf(document.jailed);
        var previous = document.jailed.stream()
                .filter(existing -> player.uuid().equals(existing.uuid))
                .findFirst();

        var record = new JailedPlayer();
        record.uuid = player.uuid();
        record.name = player.name();
        record.jail = jail.orElseThrow().name;
        record.actor = actor;
        record.reason = reason;
        record.createdAt = System.currentTimeMillis();
        record.expiresAt = durationMillis == null || durationMillis <= 0L
                ? null
                : record.createdAt + durationMillis;
        record.returnLocation = previous
                .flatMap(existing -> Optional.ofNullable(existing.returnLocation))
                .orElse(beforeTeleport);

        document.jailed.removeIf(existing -> player.uuid().equals(existing.uuid));
        document.jailed.add(record);
        if (!save()) {
            restore(document.jailed, previousRecords);
            if (!teleport(player, beforeTeleport)) {
                return AdminResult.failure(
                        "service.admin.jail-rollback-failed",
                        Map.of("player", player.name())
                );
            }
            return AdminResult.failure("service.admin.persistence-failed");
        }

        return AdminResult.success(
                "service.admin.player-jailed",
                Map.of(
                        "player", player.name(),
                        "jail", jail.orElseThrow().name
                )
        );
    }

    @Override
    public synchronized AdminResult unjail(
            UUID uuid,
            String name,
            String actor
    ) {
        var record = document.jailed.stream()
                .filter(jailed -> uuid.equals(jailed.uuid))
                .findFirst();
        if (record.isEmpty()) {
            return AdminResult.failure(
                    "service.admin.player-not-jailed",
                    Map.of("player", name)
            );
        }

        var jailed = record.orElseThrow();
        var previousExpiry = jailed.expiresAt;
        jailed.expiresAt = 0L;
        if (!save()) {
            jailed.expiresAt = previousExpiry;
            return AdminResult.failure("service.admin.persistence-failed");
        }

        var outcome = releasePlayer(jailed);
        if (outcome == ReleaseOutcome.FAILED) {
            return AdminResult.failure(
                    "service.admin.jail-release-failed",
                    Map.of("player", name)
            );
        }
        if (outcome == ReleaseOutcome.PENDING) {
            return AdminResult.success(
                    "service.admin.player-unjailed",
                    Map.of("player", name)
            );
        }

        document.jailed.remove(jailed);
        if (!save()) {
            document.jailed.add(jailed);
            return AdminResult.failure("service.admin.persistence-failed");
        }
        return AdminResult.success(
                "service.admin.player-unjailed",
                Map.of("player", name)
        );
    }

    @Override
    public synchronized Optional<JailedPlayer> jailed(UUID uuid) {
        purgeExpired();
        var now = System.currentTimeMillis();
        return document.jailed.stream()
                .filter(record -> uuid.equals(record.uuid))
                .filter(record -> !record.expired(now))
                .findFirst();
    }

    @Override
    public synchronized Collection<JailedPlayer> jailedPlayers() {
        purgeExpired();
        var now = System.currentTimeMillis();
        return document.jailed.stream()
                .filter(record -> !record.expired(now))
                .toList();
    }

    @Override
    public synchronized void purgeExpired() {
        var now = System.currentTimeMillis();
        var previous = List.copyOf(document.jailed);
        var released = document.jailed.stream()
                .filter(record -> record.expired(now))
                .filter(record -> releasePlayer(record) == ReleaseOutcome.RELEASED)
                .toList();
        if (released.isEmpty()) return;

        document.jailed.removeAll(released);
        if (!save()) restore(document.jailed, previous);
    }

    private ReleaseOutcome releasePlayer(JailedPlayer record) {
        if (!config.teleportOnJailRelease || record.returnLocation == null) {
            return ReleaseOutcome.RELEASED;
        }

        var player = platform.onlinePlayers().stream()
                .filter(online -> online.uuid().equals(record.uuid))
                .findFirst();
        if (player.isEmpty()) return ReleaseOutcome.PENDING;
        return teleport(player.orElseThrow(), record.returnLocation)
                ? ReleaseOutcome.RELEASED
                : ReleaseOutcome.FAILED;
    }

    private boolean teleport(CellPlayer player, CellLocation location) {
        try {
            return platform.teleport(player, location).join();
        } catch (RuntimeException _) {
            return false;
        }
    }

    private String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private boolean save() {
        try {
            storage.save(path, document).join();
            return true;
        } catch (RuntimeException _) {
            return false;
        }
    }

    private <T> void restore(List<T> target, List<T> snapshot) {
        target.clear();
        target.addAll(snapshot);
    }

    private enum ReleaseOutcome {

        RELEASED,
        PENDING,
        FAILED

    }

    private record ReleaseAttempt(
            JailedPlayer record,
            ReleaseOutcome outcome
    ) {

    }

}

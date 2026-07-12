package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.BanRecord;
import top.likoslupus.cellulosesz.api.admin.MuteService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.admin.data.MuteDocument;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class JsonMuteService implements MuteService {

    private final StorageService storage;
    private final Path path;
    private final UserService users;
    private final MuteDocument document;

    public JsonMuteService(
            StorageService storage,
            Path path,
            UserService users
    ) {
        this.storage = storage;
        this.path = path;
        this.users = users;
        this.document = storage.load(path, MuteDocument.class, MuteDocument::new).join();
    }

    @Override
    public AdminResult mute(
            UUID uuid,
            String name,
            String actor,
            Long durationMillis,
            String reason
    ) {
        purgeExpired();
        var record = new BanRecord();
        record.uuid = uuid;
        record.name = name;
        record.actor = actor;
        record.reason = reason;
        record.createdAt = System.currentTimeMillis();
        record.expiresAt = durationMillis <= 0L
                ? null
                : record.createdAt + durationMillis;
        document.records.removeIf(existing -> uuid.equals(existing.uuid));
        document.records.add(record);
        users.cached(uuid).ifPresent(user -> {
            user.state.mutedUntil = record.expiresAt == null
                    ? Long.MAX_VALUE
                    : record.expiresAt;
            users.markDirty(uuid);
        });
        save();

        return AdminResult.success(
                "service.admin.mute-success",
                Map.of("player", name)
        );
    }

    @Override
    public AdminResult unmute(
            UUID uuid,
            String name,
            String actor
    ) {
        document.records.removeIf(record -> uuid.equals(record.uuid));
        users.cached(uuid).ifPresent(user -> {
            user.state.mutedUntil = null;
            users.markDirty(uuid);
        });
        save();

        return AdminResult.success(
                "service.admin.unmute-success",
                Map.of("player", name)
        );
    }

    @Override
    public boolean muted(UUID uuid) {
        return record(uuid).isPresent();
    }

    @Override
    public Optional<BanRecord> record(UUID uuid) {
        purgeExpired();
        return document.records.stream()
                .filter(record -> uuid.equals(record.uuid))
                .findFirst();
    }

    @Override
    public void purgeExpired() {
        var now = System.currentTimeMillis();
        if (document.records.removeIf(record -> record.expired(now))) save();
    }

    private void save() {
        storage.save(path, document);
    }

}

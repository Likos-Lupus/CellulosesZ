package top.likoslupus.cellulosesz.modules.admin.service;

import org.jspecify.annotations.Nullable;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.BanRecord;
import top.likoslupus.cellulosesz.api.admin.MuteService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.modules.admin.data.MuteDocument;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class JsonMuteService implements MuteService {

    private final StorageService storage;
    private final Path path;
    private final MuteDocument document;

    public JsonMuteService(
            StorageService storage,
            Path path
    ) {
        this.storage = storage;
        this.path = path;
        this.document = storage.load(path, MuteDocument.class, MuteDocument::new).join();
    }

    @Override
    public synchronized AdminResult mute(
            UUID uuid,
            String name,
            String actor,
            @Nullable Long durationMillis,
            String reason
    ) {
        purgeExpired();
        var record = new BanRecord();
        record.uuid = uuid;
        record.name = name;
        record.actor = actor;
        record.reason = reason;
        record.createdAt = System.currentTimeMillis();
        record.expiresAt = durationMillis == null || durationMillis <= 0L
                ? null
                : record.createdAt + durationMillis;

        var previous = List.copyOf(document.records);
        document.records.removeIf(existing -> uuid.equals(existing.uuid));
        document.records.add(record);
        if (!save()) {
            restore(previous);
            return AdminResult.failure("service.admin.persistence-failed");
        }

        return AdminResult.success(
                "service.admin.mute-success",
                Map.of("player", name)
        );
    }

    @Override
    public synchronized AdminResult unmute(
            UUID uuid,
            String name,
            String actor
    ) {
        var previous = List.copyOf(document.records);
        document.records.removeIf(record -> uuid.equals(record.uuid));
        if (!save()) {
            restore(previous);
            return AdminResult.failure("service.admin.persistence-failed");
        }

        return AdminResult.success(
                "service.admin.unmute-success",
                Map.of("player", name)
        );
    }

    @Override
    public synchronized boolean muted(UUID uuid) {
        return record(uuid).isPresent();
    }

    @Override
    public synchronized Optional<BanRecord> record(UUID uuid) {
        purgeExpired();
        return document.records.stream()
                .filter(record -> uuid.equals(record.uuid))
                .findFirst();
    }

    @Override
    public synchronized void purgeExpired() {
        var now = System.currentTimeMillis();
        if (document.records.stream().noneMatch(record -> record.expired(now))) return;

        var previous = List.copyOf(document.records);
        document.records.removeIf(record -> record.expired(now));
        if (!save()) restore(previous);
    }

    private boolean save() {
        try {
            storage.save(path, document).join();
            return true;
        } catch (RuntimeException _) {
            return false;
        }
    }

    private void restore(List<BanRecord> records) {
        document.records.clear();
        document.records.addAll(records);
    }

}

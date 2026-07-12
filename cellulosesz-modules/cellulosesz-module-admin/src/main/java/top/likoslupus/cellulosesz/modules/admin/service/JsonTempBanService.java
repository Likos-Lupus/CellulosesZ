package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.BanRecord;
import top.likoslupus.cellulosesz.api.admin.TempBanService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.admin.data.TempBanDocument;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class JsonTempBanService implements TempBanService {

    private final StorageService storage;
    private final Path path;
    private final PlatformService platform;
    private final UserService users;
    private final MessageRenderer renderer;
    private final LocaleResolver locales;
    private final TempBanDocument document;

    public JsonTempBanService(
            StorageService storage,
            Path path,
            PlatformService platform,
            UserService users,
            MessageRenderer renderer,
            LocaleResolver locales
    ) {
        this.storage = storage;
        this.path = path;
        this.platform = platform;
        this.users = users;
        this.renderer = renderer;
        this.locales = locales;
        this.document = storage.load(path, TempBanDocument.class, TempBanDocument::new).join();
    }

    @Override
    public AdminResult tempBan(
            String target,
            String actor,
            long durationMillis,
            String reason
    ) {
        purgeExpired();
        var record = new BanRecord();
        record.uuid = platform.onlinePlayer(target)
                .map(CellPlayer::uuid)
                .or(() -> users.findUuidByName(target))
                .orElse(null);
        record.name = target;
        record.actor = actor;
        record.reason = reason;
        record.createdAt = System.currentTimeMillis();
        record.expiresAt = record.createdAt + durationMillis;
        document.records.removeIf(existing -> !existing.ip && same(existing, record));
        document.records.add(record);
        save();

        platform.onlinePlayer(target).ifPresent(player -> platform.kick(
                player,
                renderer.render(
                        locales.locale(player),
                        "service.admin.temp-ban-kick",
                        Map.of("reason", reason)
                ).plainText()
        ));
        return AdminResult.success(
                "service.admin.temp-ban-success",
                Map.of("player", target)
        );
    }

    @Override
    public AdminResult tempBanIp(
            String target,
            String actor,
            long durationMillis,
            String reason
    ) {
        purgeExpired();
        var record = new BanRecord();
        record.ip = true;
        record.address = target;
        record.name = target;
        record.actor = actor;
        record.reason = reason;
        record.createdAt = System.currentTimeMillis();
        record.expiresAt = record.createdAt + durationMillis;
        document.records.removeIf(existing -> existing.ip && target.equalsIgnoreCase(existing.address));
        document.records.add(record);
        save();

        return AdminResult.success(
                "service.admin.temp-ban-ip-success",
                Map.of("address", target)
        );
    }

    @Override
    public Optional<BanRecord> active(UUID uuid, String name) {
        purgeExpired();
        return document.records.stream()
                .filter(record -> !record.ip)
                .filter(record -> uuid.equals(record.uuid) || record.name.equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public Optional<BanRecord> activeIp(String address) {
        purgeExpired();
        return document.records.stream()
                .filter(record -> record.ip && address.equalsIgnoreCase(record.address))
                .findFirst();
    }

    @Override
    public void purgeExpired() {
        var now = System.currentTimeMillis();
        if (document.records.removeIf(record -> record.expired(now))) save();
    }

    private boolean same(BanRecord first, BanRecord second) {
        if (first.uuid != null && second.uuid != null) return first.uuid.equals(second.uuid);
        return first.name.equalsIgnoreCase(second.name);
    }

    private void save() {
        storage.save(path, document);
    }

}

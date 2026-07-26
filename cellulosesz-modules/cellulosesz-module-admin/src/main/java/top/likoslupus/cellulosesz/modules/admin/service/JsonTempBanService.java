package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.AdminStatus;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class JsonTempBanService implements TempBanService {

    private final StorageService storage;
    private final Path path;
    private final PlatformService platform;
    private final UserService users;
    private final MessageRenderer renderer;
    private final LocaleResolver locales;
    private final boolean kickOnlinePlayers;
    private TempBanDocument document;
    private CompletableFuture<Void> mutationTail = CompletableFuture.completedFuture(null);

    public JsonTempBanService(
            StorageService storage,
            Path path,
            PlatformService platform,
            UserService users,
            MessageRenderer renderer,
            LocaleResolver locales,
            boolean kickOnlinePlayers
    ) {
        this.storage = storage;
        this.path = path;
        this.platform = platform;
        this.users = users;
        this.renderer = renderer;
        this.locales = locales;
        this.kickOnlinePlayers = kickOnlinePlayers;
        this.document = storage.load(path, TempBanDocument.class, TempBanDocument::new).join();
        validate(document);
    }

    private void validate(TempBanDocument candidate) {
        candidate.records.forEach(record -> {
            if (record.ip()) {
                var normalized = IpAddresses.normalize(record.address());
                if (normalized.isEmpty() || !normalized.orElseThrow().equals(record.address())) {
                    throw new IllegalStateException("Stored IP ban is not normalized");
                }
            }
        });
    }

    @Override
    public CompletableFuture<AdminResult> tempBan(
            String target,
            String actor,
            long durationMillis,
            String reason
    ) {
        if (durationMillis <= 0L) return completedInvalidDuration();
        var uuid = platform.onlinePlayer(target)
                .map(CellPlayer::uuid)
                .or(() -> users.findUuidByName(target))
                .orElse(null);
        var createdAt = System.currentTimeMillis();
        final long expiresAt;
        try {
            expiresAt = Math.addExact(createdAt, durationMillis);
        } catch (ArithmeticException exception) {
            return completedInvalidDuration();
        }
        var record = new BanRecord(uuid, target, reason, actor, createdAt, expiresAt, false, null);
        return mutate(current -> {
            current.records.removeIf(existing -> !existing.ip() && same(existing, record));
            current.records.add(record);
            return AdminResult.success("service.admin.temp-ban-success", Map.of("player", target));
        }).thenApply(result -> {
            if (result.success() && kickOnlinePlayers) kickPlayer(target, reason);
            return result;
        });
    }

    @Override
    public CompletableFuture<AdminResult> tempBanIp(
            String target,
            String actor,
            long durationMillis,
            String reason
    ) {
        if (durationMillis <= 0L) return completedInvalidDuration();
        var normalized = IpAddresses.normalize(target);
        if (normalized.isEmpty()) {
            return CompletableFuture.completedFuture(AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "service.admin.invalid-address",
                    Map.of("address", target)
            ));
        }
        var address = normalized.orElseThrow();
        var createdAt = System.currentTimeMillis();
        final long expiresAt;
        try {
            expiresAt = Math.addExact(createdAt, durationMillis);
        } catch (ArithmeticException exception) {
            return completedInvalidDuration();
        }
        var record = new BanRecord(null, address, reason, actor, createdAt, expiresAt, true, address);
        return mutate(current -> {
            current.records.removeIf(existing -> existing.ip()
                    && address.equalsIgnoreCase(existing.address()));
            current.records.add(record);
            return AdminResult.success("service.admin.temp-ban-ip-success", Map.of("address", address));
        }).thenApply(result -> {
            if (result.success() && kickOnlinePlayers) kickAddress(address, reason);
            return result;
        });
    }

    @Override
    public CompletableFuture<AdminResult> unban(UUID uuid, String name, String actor) {
        return mutate(current -> current.records.removeIf(record -> !record.ip()
                && (uuid.equals(record.uuid()) || record.name().equalsIgnoreCase(name)))
                ? AdminResult.success("service.admin.temp-unban-success", Map.of("player", name))
                : AdminResult.failure(AdminStatus.NOT_FOUND,
                        "service.admin.temp-ban-not-found", Map.of("player", name)));
    }

    @Override
    public CompletableFuture<AdminResult> unbanIp(String address, String actor) {
        var normalized = IpAddresses.normalize(address);
        if (normalized.isEmpty()) {
            return CompletableFuture.completedFuture(AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "service.admin.invalid-address",
                    Map.of("address", address)
            ));
        }
        var value = normalized.orElseThrow();
        return mutate(current -> current.records.removeIf(record -> record.ip()
                && value.equalsIgnoreCase(record.address()))
                ? AdminResult.success("service.admin.temp-unban-ip-success", Map.of("address", value))
                : AdminResult.failure(AdminStatus.NOT_FOUND,
                        "service.admin.temp-ban-ip-not-found", Map.of("address", value)));
    }

    @Override
    public synchronized Optional<BanRecord> active(UUID uuid, String name) {
        var now = System.currentTimeMillis();
        return document.records.stream()
                .filter(record -> !record.ip() && !record.expired(now))
                .filter(record -> uuid.equals(record.uuid()) || record.name().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public synchronized Optional<BanRecord> activeIp(String address) {
        var normalized = IpAddresses.normalize(address);
        if (normalized.isEmpty()) return Optional.empty();
        var value = normalized.orElseThrow();
        var now = System.currentTimeMillis();
        return document.records.stream()
                .filter(record -> record.ip() && !record.expired(now))
                .filter(record -> value.equalsIgnoreCase(record.address()))
                .findFirst();
    }

    @Override
    public CompletableFuture<Integer> purgeExpired() {
        var result = new CompletableFuture<Integer>();
        enqueue(current -> {
            var before = current.records.size();
            current.records.removeIf(record -> record.expired(System.currentTimeMillis()));
            return new Mutation<>(current, before - current.records.size());
        }, result);
        return result;
    }

    private void kickAddress(String address, String reason) {
        platform.runOnServerThread(() -> platform.onlinePlayers().stream()
                .filter(player -> platform.address(player)
                        .flatMap(IpAddresses::normalize)
                        .map(address::equalsIgnoreCase)
                        .orElse(false))
                .forEach(player -> platform.kick(
                        player,
                        renderer.render(locales.locale(player), "service.admin.temp-ban-kick",
                                Map.of("reason", reason)).plainText()
                )));
    }

    private CompletableFuture<AdminResult> completedInvalidDuration() {
        return CompletableFuture.completedFuture(AdminResult.failure(
                AdminStatus.INVALID_INPUT, "service.admin.invalid-duration"));
    }

    private CompletableFuture<AdminResult> mutate(Function<TempBanDocument, AdminResult> operation) {
        var result = new CompletableFuture<AdminResult>();
        enqueue(current -> new Mutation<>(current, operation.apply(current)), result);
        return result;
    }

    private boolean same(BanRecord first, BanRecord second) {
        if (first.uuid() != null && second.uuid() != null) return first.uuid().equals(second.uuid());
        return first.name().equalsIgnoreCase(second.name());
    }

    private void kickPlayer(String target, String reason) {
        platform.runOnServerThread(() -> platform.onlinePlayer(target).ifPresent(player -> platform.kick(
                player,
                renderer.render(locales.locale(player), "service.admin.temp-ban-kick",
                        Map.of("reason", reason)).plainText()
        )));
    }

    private synchronized <T> void enqueue(
            Function<TempBanDocument, Mutation<T>> operation,
            CompletableFuture<T> result
    ) {
        mutationTail = mutationTail.handle((ignored, failure) -> null)
                .thenCompose(ignored -> {
                    TempBanDocument current;
                    synchronized (this) {
                        current = copy(document);
                    }
                    final Mutation<T> mutation;
                    try {
                        mutation = operation.apply(current);
                    } catch (RuntimeException exception) {
                        result.completeExceptionally(exception);
                        return CompletableFuture.completedFuture(null);
                    }
                    return storage.save(path, mutation.document()).handle((saved, failure) -> {
                        if (failure == null) {
                            synchronized (this) {
                                document = mutation.document();
                            }
                            result.complete(mutation.result());
                        } else if (mutation.result() instanceof AdminResult) {
                            @SuppressWarnings("unchecked")
                            var failureResult = (T) AdminResult.failure(
                                    AdminStatus.PERSISTENCE_FAILURE, "service.admin.persistence-failed");
                            result.complete(failureResult);
                        } else {
                            result.completeExceptionally(failure);
                        }
                        return (Void) null;
                    });
                });
        mutationTail.whenComplete((ignored, failure) -> {
            if (failure != null) result.completeExceptionally(failure);
        });
    }

    private TempBanDocument copy(TempBanDocument source) {
        var target = new TempBanDocument();
        target.records = new ArrayList<>(source.records);
        return target;
    }

    private record Mutation<T>(
            TempBanDocument document,
            T result
    ) {

    }

}

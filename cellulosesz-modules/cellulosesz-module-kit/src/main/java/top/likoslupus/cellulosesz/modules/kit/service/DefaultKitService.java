package top.likoslupus.cellulosesz.modules.kit.service;

import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.kit.KitClaimResult;
import top.likoslupus.cellulosesz.api.kit.KitDefinition;
import top.likoslupus.cellulosesz.api.kit.KitItem;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.CellUser;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.kit.KitConfig;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public final class DefaultKitService implements KitService {

    private static final Pattern KIT_ID = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,63}$");

    private final StorageService storage;
    private final UserService users;
    private final PlatformService platform;
    private final Optional<EconomyService> economy;
    private final KitConfig config;
    private final Path kitsDirectory;
    private final LinkedHashMap<String, KitDefinition> kits = new LinkedHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<UUID, CompletableFuture<Void>> claimTails = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, CompletableFuture<Void>> definitionTails = new java.util.concurrent.ConcurrentHashMap<>();

    public DefaultKitService(
            StorageService storage,
            UserService users,
            PlatformService platform,
            Optional<EconomyService> economy,
            KitConfig config,
            Path kitsDirectory
    ) {
        this.storage = requireNonNull(storage, "storage");
        this.users = requireNonNull(users, "users");
        this.platform = requireNonNull(platform, "platform");
        this.economy = requireNonNull(economy, "economy");
        this.config = requireNonNull(config, "config");
        this.kitsDirectory = requireNonNull(kitsDirectory, "kitsDirectory");
        reload().join();
    }

    @Override
    public CompletableFuture<Void> reload() {
        return storage.loadDirectory(kitsDirectory, KitDefinition.class)
                .thenCompose(loaded -> {
                    var next = new LinkedHashMap<String, KitDefinition>();
                    loaded.stream()
                            .map(this::validatedCopy)
                            .sorted(Comparator.comparing(kit -> kit.id))
                            .forEach(kit -> {
                                var previous = next.put(key(kit.id), kit);
                                if (previous != null) {
                                    throw new IllegalStateException("Duplicate kit id: " + kit.id);
                                }
                            });

                    if (next.isEmpty() && config.createStarterKitWhenEmpty) {
                        var starter = starterKit();
                        validate(starter);
                        return storage.save(path(starter.id), starter)
                                .thenRun(() -> replaceKits(Map.of(starter.id, starter)));
                    }

                    replaceKits(next);
                    return CompletableFuture.completedFuture(null);
                });
    }

    @Override
    public synchronized List<KitDefinition> kits() {
        return kits.values().stream().map(this::copyDefinition).toList();
    }

    @Override
    public synchronized Optional<KitDefinition> kit(String id) {
        return Optional.ofNullable(kits.get(key(id)))
                .map(this::copyDefinition);
    }

    @Override
    public CompletableFuture<Void> save(KitDefinition kit) {
        var candidate = validatedCopy(kit);
        var id = key(candidate.id);
        return enqueueDefinitionMutation(id, () -> storage.save(path(candidate.id), candidate)
                .thenRun(() -> {
                    synchronized (this) {
                        kits.put(id, candidate);
                    }
                }));
    }

    @Override
    public CompletableFuture<Boolean> delete(String id) {
        var normalizedId = key(id);
        if (!KIT_ID.matcher(normalizedId).matches()) {
            return CompletableFuture.completedFuture(false);
        }

        return enqueueDefinitionMutation(normalizedId, () -> {
            synchronized (this) {
                if (!kits.containsKey(normalizedId)) {
                    return CompletableFuture.completedFuture(false);
                }
            }

            return storage.delete(path(normalizedId)).thenApply(deleted -> {
                if (!deleted) return false;

                synchronized (this) {
                    kits.remove(normalizedId);
                }
                return true;
            });
        });
    }

    @Override
    public CompletableFuture<KitClaimResult> claim(CellPlayer player, KitDefinition kit) {
        requireNonNull(player, "player");
        var candidate = validatedCopy(kit);

        var result = new CompletableFuture<KitClaimResult>();
        claimTails.compute(player.uuid(), (uuid, previous) -> {
            var tail = previous == null ? CompletableFuture.<Void>completedFuture(null) : previous;
            var next = tail.handle((ignored, failure) -> null)
                    .thenCompose(ignored -> claimSerialized(player, candidate))
                    .handle((claimResult, failure) -> {
                        if (failure == null) result.complete(claimResult);
                        else result.completeExceptionally(failure);
                        return (Void) null;
                    });

            next.whenComplete((ignored, _) -> claimTails.remove(uuid, next));
            return next;
        });
        return result;
    }

    private CompletableFuture<KitClaimResult> claimSerialized(CellPlayer player, KitDefinition kit) {
        var cooldownKey = cooldownKey(kit.id);
        return users.update(player.uuid(), user -> reserveCooldown(user, cooldownKey, kit.cooldownSeconds))
                .thenCompose(reservation -> {
                    if (!reservation.accepted()) {
                        return CompletableFuture.completedFuture(reservation.failure());
                    }
                    var cost = parseMoney(kit.cost);
                    if (config.chargeKitCost && cost.signum() > 0 && economy.isEmpty()) {
                        return rollbackCooldown(player.uuid(), cooldownKey, reservation)
                                .thenApply(rolledBack -> rolledBack
                                        ? KitClaimResult.failure("service.kit.economy-unavailable")
                                        : KitClaimResult.failure("service.kit.rollback-failed"));
                    }

                    CompletableFuture<Optional<top.likoslupus.cellulosesz.api.economy.TransactionResult>> payment;
                    if (config.chargeKitCost && cost.signum() > 0) {
                        payment = economy.orElseThrow().withdraw(
                                player.uuid(),
                                cost,
                                TransactionCause.command(player.name(), "kit " + kit.id)
                        ).thenApply(Optional::of);
                    } else {
                        payment = CompletableFuture.completedFuture(Optional.empty());
                    }

                    return payment.thenCompose(paymentResult -> {
                        if (paymentResult.isPresent() && !paymentResult.orElseThrow().success()) {
                            return rollbackCooldown(player.uuid(), cooldownKey, reservation)
                                    .thenApply(rolledBack -> rolledBack
                                            ? KitClaimResult.failure(paymentResult.orElseThrow().message())
                                            : KitClaimResult.failure("service.kit.rollback-failed"));
                        }
                        return platform.callOnServerThread(() -> {
                            var prepared = platform.prepareInventoryGrant(player, kit.items);
                            return prepared.isPresent() && prepared.orElseThrow().commit();
                        }).thenCompose(granted -> {
                            if (granted) {
                                return CompletableFuture.completedFuture(KitClaimResult.success(
                                        "service.kit.claimed", Map.of("kit", kit.displayName)
                                ));
                            }
                            return compensateFailedClaim(
                                    player.uuid(), cooldownKey, reservation,
                                    cost, paymentResult.isPresent()
                            ).thenApply(rolledBack -> rolledBack
                                    ? KitClaimResult.failure("service.kit.inventory-unavailable")
                                    : KitClaimResult.failure("service.kit.rollback-failed"));
                        });
                    });
                });
    }

    private String cooldownKey(String kitId) {
        return "kit:" + key(kitId);
    }

    private CooldownReservation reserveCooldown(
            CellUser user,
            String cooldownKey,
            long cooldownSeconds
    ) {
        var now = System.currentTimeMillis();
        var hadPrevious = user.cooldowns.containsKey(cooldownKey);
        var previous = user.cooldowns.getOrDefault(cooldownKey, 0L);
        if (cooldownSeconds < 0L && hadPrevious) {
            return CooldownReservation.rejected(KitClaimResult.failure("service.kit.once"));
        }
        if (cooldownSeconds >= 0L && previous > now) {
            var seconds = Math.max(1L, (previous - now + 999L) / 1000L);
            return CooldownReservation.rejected(
                    KitClaimResult.failure("service.kit.cooldown", Map.of("seconds", seconds))
            );
        }
        if (cooldownSeconds == 0L) {
            return CooldownReservation.accepted(false, hadPrevious, previous, previous);
        }
        var reserved = nextClaimTime(now, cooldownSeconds);
        user.cooldowns.put(cooldownKey, reserved);
        return CooldownReservation.accepted(true, hadPrevious, previous, reserved);
    }

    private CompletableFuture<Boolean> rollbackCooldown(
            UUID uuid,
            String cooldownKey,
            CooldownReservation reservation
    ) {
        if (!reservation.changed()) return CompletableFuture.completedFuture(true);
        return users.update(uuid, user -> {
            if (user.cooldowns.getOrDefault(cooldownKey, Long.MIN_VALUE) != reservation.reserved()) {
                return false;
            }
            if (reservation.hadPrevious()) user.cooldowns.put(cooldownKey, reservation.previous());
            else user.cooldowns.remove(cooldownKey);
            return true;
        }).exceptionally(_ -> false);
    }

    private CompletableFuture<Boolean> compensateFailedClaim(
            UUID uuid,
            String cooldownKey,
            CooldownReservation reservation,
            BigDecimal cost,
            boolean charged
    ) {
        var cooldownRollback = rollbackCooldown(uuid, cooldownKey, reservation);
        var paymentRollback = charged
                ? economy.orElseThrow().deposit(
                uuid, cost, TransactionCause.system("kit claim refund")
        ).thenApply(result -> result.success()).exceptionally(_ -> false)
                : CompletableFuture.completedFuture(true);
        return cooldownRollback.thenCombine(paymentRollback, (first, second) -> first && second);
    }

    private long nextClaimTime(long now, long cooldownSeconds) {
        if (cooldownSeconds < 0L) return Long.MAX_VALUE;
        try {
            return Math.addExact(now, Math.multiplyExact(cooldownSeconds, 1000L));
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Kit cooldown is too large", exception);
        }
    }

    @Override
    public CompletableFuture<Void> resetCooldown(UUID uuid, String kitId) {
        return users.update(uuid, user -> {
            user.cooldowns.remove(cooldownKey(kitId));
            return true;
        }).thenApply(ignored -> null);
    }

    private KitDefinition validatedCopy(KitDefinition source) {
        var copy = copyDefinition(source);
        validate(copy);
        return copy;
    }

    private <T> CompletableFuture<T> enqueueDefinitionMutation(
            String id,
            Supplier<CompletableFuture<T>> operation
    ) {
        var result = new CompletableFuture<T>();
        definitionTails.compute(id, (key, previous) -> {
            var tail = previous == null ? CompletableFuture.<Void>completedFuture(null) : previous;
            var next = tail.handle((ignored, _) -> null)
                    .thenCompose(ignored -> {
                        try {
                            return operation.get();
                        } catch (Throwable failure) {
                            return CompletableFuture.failedFuture(failure);
                        }
                    })
                    .handle((value, failure) -> {
                        if (failure == null) result.complete(value);
                        else result.completeExceptionally(unwrapCompletionFailure(failure));
                        return (Void) null;
                    });
            next.whenComplete((ignored, _) -> definitionTails.remove(key, next));
            return next;
        });
        return result;
    }

    private Path path(String id) {
        var normalizedId = key(id);
        if (!KIT_ID.matcher(normalizedId).matches()) {
            throw new IllegalArgumentException("Invalid kit id: " + id);
        }
        return kitsDirectory.resolve(normalizedId + ".yml");
    }

    private void validate(KitDefinition kit) {
        requireNonNull(kit, "kit");

        kit.id = key(requireNonNull(kit.id, "kit.id"));
        if (!KIT_ID.matcher(kit.id).matches()) {
            throw new IllegalArgumentException("Invalid kit id: " + kit.id);
        }

        kit.displayName = requireNonNull(kit.displayName, "kit.displayName").trim();
        if (kit.displayName.isEmpty()) {
            throw new IllegalArgumentException("Kit displayName must not be blank");
        }

        kit.permission = requireNonNull(kit.permission, "kit.permission").trim();
        if (!kit.permission.isEmpty() && !kit.permission.startsWith("cellulosesz.")) {
            throw new IllegalArgumentException("Kit permission must use the cellulosesz.* namespace");
        }

        if (kit.cooldownSeconds < -1L || kit.cooldownSeconds > Long.MAX_VALUE / 1000L) {
            throw new IllegalArgumentException("Kit cooldown is outside the supported range");
        }

        kit.cost = requireNonNull(kit.cost, "kit.cost").trim();
        var cost = parseMoney(kit.cost);
        if (cost.signum() < 0) {
            throw new IllegalArgumentException("Kit cost must not be negative");
        }
        kit.cost = cost.toPlainString();

        requireNonNull(kit.items, "kit.items");
        if (kit.items.isEmpty()) {
            throw new IllegalArgumentException("Kit must contain at least one item");
        }
        var slots = new HashSet<Integer>();
        kit.items.forEach(item -> {
            requireNonNull(item, "kit item");
            if (item.slot < 0 || !slots.add(item.slot))
                throw new IllegalArgumentException("Invalid or duplicate kit slot");
            item.stack = item.validatedStack();
        });
    }

    private Throwable unwrapCompletionFailure(Throwable failure) {
        if (failure instanceof java.util.concurrent.CompletionException completion
                && completion.getCause() != null) {
            return completion.getCause();
        }
        return failure;
    }

    private BigDecimal parseMoney(String value) {
        return new BigDecimal(value);
    }

    private String key(String id) {
        return requireNonNull(id, "id").trim().toLowerCase(Locale.ROOT);
    }

    private KitDefinition copyDefinition(KitDefinition source) {
        requireNonNull(source, "kit");
        var copy = new KitDefinition();
        copy.id = requireNonNull(source.id, "kit.id");
        copy.displayName = requireNonNull(source.displayName, "kit.displayName");
        copy.permission = requireNonNull(source.permission, "kit.permission");
        copy.cooldownSeconds = source.cooldownSeconds;
        copy.cost = requireNonNull(source.cost, "kit.cost");
        copy.items = requireNonNull(source.items, "kit.items").stream()
                .map(item -> new KitItem(
                        requireNonNull(item, "kit item").slot,
                        item.validatedStack()
                ))
                .toList();
        return copy;
    }

    private KitDefinition starterKit() {
        var kit = new KitDefinition();
        kit.id = "starter";
        kit.displayName = "Starter";
        kit.permission = "cellulosesz.kit.starter";
        kit.cooldownSeconds = 86400L;
        kit.cost = "0.00";
        kit.items.add(new KitItem(0, "{\"id\":\"minecraft:bread\",\"count\":16}"));
        kit.items.add(new KitItem(1, "{\"id\":\"minecraft:stone_sword\",\"count\":1}"));
        return kit;
    }

    private synchronized void replaceKits(Map<String, KitDefinition> next) {
        kits.clear();
        next.forEach((id, definition) -> kits.put(key(id), validatedCopy(definition)));
    }

    private record CooldownReservation(
            boolean accepted,
            boolean changed,
            boolean hadPrevious,
            long previous,
            long reserved,
            KitClaimResult failure
    ) {

        private static CooldownReservation accepted(
                boolean changed,
                boolean hadPrevious,
                long previous,
                long reserved
        ) {
            return new CooldownReservation(
                    true, changed, hadPrevious, previous, reserved,
                    KitClaimResult.failure("service.kit.persistence-failed")
            );
        }

        private static CooldownReservation rejected(KitClaimResult failure) {
            return new CooldownReservation(false, false, false, 0L, 0L, failure);
        }

    }

}

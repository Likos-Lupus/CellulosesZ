package top.likoslupus.cellulosesz.modules.kit.service;

import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.economy.TransactionResult;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.kit.KitClaimResult;
import top.likoslupus.cellulosesz.api.kit.KitDefinition;
import top.likoslupus.cellulosesz.api.kit.KitItem;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.module.PreparedModuleReload;
import top.likoslupus.cellulosesz.api.module.PreparedReloads;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.CellUser;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.api.user.UserUpdate;
import top.likoslupus.cellulosesz.core.concurrent.KeyedSerialAsyncQueue;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.modules.kit.KitConfig;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public final class DefaultKitService implements KitService, AsyncInitializable, AsyncCloseable {

    private static final Pattern KIT_ID = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,63}$");
    private static final int MAXIMUM_PENDING_PER_PLAYER = 1_024;
    private static final int MAXIMUM_PENDING_PER_DEFINITION = 1_024;
    private static final int MAXIMUM_PENDING_RELOADS = 32;

    private final StorageService storage;
    private final UserService users;
    private final InventoryPlatformService inventory;
    private final ServerThreadExecutor serverThread;
    private final Optional<EconomyService> economy;
    private final Path kitsDirectory;
    private final KeyedSerialAsyncQueue<UUID> claims = new KeyedSerialAsyncQueue<>(
            Runnable::run,
            MAXIMUM_PENDING_PER_PLAYER
    );
    private final KeyedSerialAsyncQueue<String> definitions = new KeyedSerialAsyncQueue<>(
            Runnable::run,
            MAXIMUM_PENDING_PER_DEFINITION
    );
    private final SerialAsyncQueue reloads = new SerialAsyncQueue(
            Runnable::run,
            MAXIMUM_PENDING_RELOADS
    );
    private RuntimeState state;
    private long mutationVersion;

    public DefaultKitService(
            StorageService storage,
            UserService users,
            InventoryPlatformService inventory,
            ServerThreadExecutor serverThread,
            Optional<EconomyService> economy,
            KitConfig config,
            Path kitsDirectory
    ) {
        this.storage = requireNonNull(storage, "storage");
        this.users = requireNonNull(users, "users");
        this.inventory = requireNonNull(inventory, "inventory");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.economy = requireNonNull(economy, "economy");
        var initialConfig = requireNonNull(config, "config");
        this.kitsDirectory = requireNonNull(kitsDirectory, "kitsDirectory");
        this.state = new RuntimeState(
                Map.of(),
                initialConfig.createStarterKitWhenEmpty,
                initialConfig.chargeKitCost
        );
    }

    private static Throwable unwrap(Throwable failure) {
        var current = failure;
        while (current instanceof java.util.concurrent.CompletionException
                && current.getCause() != null
        ) {
            current = current.getCause();
        }
        return current;
    }

    @Override
    public CompletableFuture<Void> initialize() {
        var current = state;
        return prepareReload(
                current.createStarterKitWhenEmpty(),
                current.chargeKitCost()
        ).thenCompose(PreparedModuleReload::commit).toCompletableFuture();
    }

    public CompletableFuture<PreparedModuleReload> prepareReload(
            boolean createStarterKitWhenEmpty,
            boolean chargeKitCost
    ) {
        return reloads.submit(() -> storage.loadDirectory(
                                kitsDirectory,
                                KitDefinition.class
                        )
                        .thenApply(loaded -> {
                            var next = new LinkedHashMap<String, KitDefinition>();
                            loaded.stream()
                                    .map(this::validatedCopy)
                                    .sorted(Comparator.comparing(kit -> kit.id))
                                    .forEach(kit -> {
                                        var previous = next.put(key(kit.id), kit);
                                        if (previous != null) {
                                            throw new IllegalStateException(
                                                    "Duplicate kit id: " + kit.id);
                                        }
                                    });

                            var persistStarter = next.isEmpty() && createStarterKitWhenEmpty;
                            if (persistStarter) {
                                var starter = validatedCopy(starterKit());
                                next.put(key(starter.id), starter);
                            }

                            synchronized (this) {
                                return new PreparedReload(
                                        state,
                                        immutableDefinitions(next),
                                        createStarterKitWhenEmpty,
                                        chargeKitCost,
                                        mutationVersion,
                                        persistStarter
                                );
                            }
                        })
        );
    }

    @Override
    public synchronized List<KitDefinition> kits() {
        return state.kits().values().stream()
                .map(this::copyDefinition)
                .toList();
    }

    @Override
    public synchronized Optional<KitDefinition> kit(String id) {
        return Optional.ofNullable(state.kits().get(key(id)))
                .map(this::copyDefinition);
    }

    @Override
    public CompletableFuture<Void> save(KitDefinition kit) {
        var candidate = validatedCopy(kit);
        var id = key(candidate.id);

        return reloads.submit(() -> enqueueDefinitionMutation(
                id,
                () -> storage
                        .save(path(candidate.id), candidate)
                        .thenRun(() -> {
                            synchronized (this) {
                                var next = new LinkedHashMap<>(state.kits());
                                next.put(id, candidate);
                                state = state.withKits(immutableDefinitions(next));
                                mutationVersion++;
                            }
                        })
        ));
    }

    @Override
    public CompletableFuture<Boolean> delete(String id) {
        var normalizedId = key(id);
        if (!KIT_ID.matcher(normalizedId).matches()) {
            return CompletableFuture.completedFuture(false);
        }

        return reloads.submit(() -> enqueueDefinitionMutation(
                normalizedId,
                () -> {
                    synchronized (this) {
                        if (!state.kits().containsKey(normalizedId)) {
                            return CompletableFuture.completedFuture(false);
                        }
                    }

                    return storage
                            .delete(path(normalizedId))
                            .thenApply(deleted -> {
                                if (!deleted) {
                                    return false;
                                }

                                synchronized (this) {
                                    var next = new LinkedHashMap<>(state.kits());
                                    next.remove(normalizedId);
                                    state = state.withKits(immutableDefinitions(next));
                                    mutationVersion++;
                                }

                                return true;
                            });
                }
        ));
    }

    @Override
    public CompletableFuture<KitClaimResult> claim(CellPlayer player, KitDefinition kit) {
        requireNonNull(player, "player");
        var candidate = validatedCopy(kit);
        final boolean chargeKitCost;
        synchronized (this) {
            chargeKitCost = state.chargeKitCost();
        }

        return claims.submit(
                player.uuid(),
                () -> claimSerialized(player, candidate, chargeKitCost)
        );
    }

    private CompletableFuture<KitClaimResult> claimSerialized(
            CellPlayer player,
            KitDefinition kit,
            boolean chargeKitCost
    ) {
        var cooldownKey = cooldownKey(kit.id);
        return users
                .update(
                        player.uuid(),
                        user -> reserveCooldown(
                                user,
                                cooldownKey,
                                kit.cooldownSeconds
                        )
                )
                .thenCompose(reservation -> {
                    if (!reservation.accepted()) {
                        return CompletableFuture.completedFuture(reservation.failure());
                    }

                    var cost = parseMoney(kit.cost);
                    if (chargeKitCost
                            && cost.signum() > 0
                            && economy.isEmpty()
                    ) {
                        return rollbackCooldown(
                                player.uuid(),
                                cooldownKey,
                                reservation
                        ).thenApply(
                                rolledBack -> rolledBack
                                        ? KitClaimResult.failure("service.kit.economy-unavailable")
                                        : KitClaimResult.failure("service.kit.rollback-failed")
                        );
                    }

                    CompletableFuture<Optional<TransactionResult>> payment;
                    if (chargeKitCost && cost.signum() > 0) {
                        payment = economy
                                .orElseThrow()
                                .withdraw(
                                        player.uuid(),
                                        cost,
                                        TransactionCause.command(
                                                player.name(),
                                                "kit " + kit.id
                                        )
                                )
                                .thenApply(Optional::of);
                    } else {
                        payment = CompletableFuture.completedFuture(Optional.empty());
                    }

                    return payment.thenCompose(paymentResult -> {
                        if (paymentResult.isPresent()
                                && !paymentResult.orElseThrow().success()
                        ) {
                            return rollbackCooldown(player.uuid(), cooldownKey, reservation)
                                    .thenApply(rolledBack -> rolledBack
                                            ?
                                            KitClaimResult.failure(
                                                    paymentResult.orElseThrow().message()
                                            )
                                            : KitClaimResult.failure("service.kit.rollback-failed"));
                        }

                        return serverThread
                                .submit(() -> {
                                    var prepared = inventory.prepareGrant(player, kit.items);
                                    return prepared.successful()
                                            && prepared.value().isPresent()
                                            && prepared.value().orElseThrow().commit();
                                })
                                .thenCompose(granted -> {
                                    if (granted) {
                                        return CompletableFuture.completedFuture(KitClaimResult.success(
                                                "service.kit.claimed",
                                                Map.of("kit", kit.displayName)
                                        ));
                                    }

                                    return compensateFailedClaim(
                                            player.uuid(),
                                            cooldownKey,
                                            reservation,
                                            cost,
                                            paymentResult.isPresent()
                                    ).thenApply(rolledBack -> rolledBack
                                            ?
                                            KitClaimResult.failure(
                                                    "service.kit.inventory-unavailable"
                                            )
                                            : KitClaimResult.failure(
                                                    "service.kit.rollback-failed"
                                            )
                                    );
                                });
                    });
                });
    }

    private String cooldownKey(String kitId) {
        return "kit:" + key(kitId);
    }

    @SuppressWarnings("WrapperTypeMayBePrimitive")
    private UserUpdate<CooldownReservation> reserveCooldown(
            CellUser user,
            String cooldownKey,
            long cooldownSeconds
    ) {
        var now = System.currentTimeMillis();
        var hadPrevious = user.cooldowns().containsKey(cooldownKey);
        var previous = user.cooldowns().getOrDefault(cooldownKey, 0L);

        if (cooldownSeconds < 0L && hadPrevious) {
            return UserUpdate.of(
                    user,
                    CooldownReservation.rejected(KitClaimResult.failure("service.kit.once"))
            );
        }

        if (cooldownSeconds >= 0L && previous > now) {
            var seconds = Math.max(1L, (previous - now + 999L) / 1000L);
            return UserUpdate.of(
                    user,
                    CooldownReservation.rejected(
                            KitClaimResult.failure(
                                    "service.kit.cooldown",
                                    Map.of("seconds", seconds)
                            )
                    )
            );
        }

        if (cooldownSeconds == 0L) {
            return UserUpdate.of(
                    user,
                    CooldownReservation.accepted(false, hadPrevious, previous, previous)
            );
        }

        var reserved = nextClaimTime(now, cooldownSeconds);
        var cooldowns = new LinkedHashMap<>(user.cooldowns());
        cooldowns.put(cooldownKey, reserved);

        return UserUpdate.of(
                user.withCooldowns(cooldowns),
                CooldownReservation.accepted(true, hadPrevious, previous, reserved)
        );
    }

    private CompletableFuture<Boolean> rollbackCooldown(
            UUID uuid,
            String cooldownKey,
            CooldownReservation reservation
    ) {
        if (!reservation.changed()) {
            return CompletableFuture.completedFuture(true);
        }

        return users
                .update(
                        uuid,
                        user -> {
                            if (user.cooldowns().getOrDefault(cooldownKey, Long.MIN_VALUE)
                                    != reservation.reserved()
                            ) {
                                return UserUpdate.of(user, false);
                            }

                            var cooldowns = new LinkedHashMap<>(user.cooldowns());
                            if (reservation.hadPrevious()) {
                                cooldowns.put(cooldownKey, reservation.previous());
                            } else {
                                cooldowns.remove(cooldownKey);
                            }

                            return UserUpdate.of(user.withCooldowns(cooldowns), true);
                        }
                )
                .exceptionally(_ -> false);
    }

    private CompletableFuture<Boolean> compensateFailedClaim(
            UUID uuid,
            String cooldownKey,
            CooldownReservation reservation,
            BigDecimal cost,
            boolean charged
    ) {
        var cooldownRollback = rollbackCooldown(uuid, cooldownKey, reservation);
        CompletableFuture<Boolean> paymentRollback;

        if (charged) {
            paymentRollback = economy
                    .orElseThrow()
                    .deposit(
                            uuid,
                            cost,
                            TransactionCause.system("kit claim refund")
                    )
                    .thenApply(TransactionResult::success)
                    .exceptionally(_ -> false);
        } else {
            paymentRollback = CompletableFuture.completedFuture(true);
        }

        return cooldownRollback.thenCombine(
                paymentRollback,
                (first, second) -> first && second
        );
    }

    private long nextClaimTime(long now, long cooldownSeconds) {
        if (cooldownSeconds < 0L) {
            return Long.MAX_VALUE;
        }

        try {
            return Math.addExact(now, Math.multiplyExact(cooldownSeconds, 1000L));
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Kit cooldown is too large", exception);
        }
    }

    @Override
    public CompletableFuture<Void> resetCooldown(UUID uuid, String kitId) {
        var player = requireNonNull(uuid, "uuid");
        return claims.submit(
                player,
                () -> users.updateVoid(
                        player,
                        user -> {
                            var cooldowns = new LinkedHashMap<>(user.cooldowns());
                            cooldowns.remove(cooldownKey(kitId));
                            return user.withCooldowns(cooldowns);
                        }
                )
        );
    }

    private <T> CompletableFuture<T> enqueueDefinitionMutation(
            String id,
            Supplier<CompletableFuture<T>> operation
    ) {
        return definitions.submit(id, operation);
    }

    private Path path(String id) {
        var normalizedId = key(id);
        if (!KIT_ID.matcher(normalizedId).matches()) {
            throw new IllegalArgumentException("Invalid kit id: " + id);
        }

        return kitsDirectory.resolve(normalizedId + ".yml");
    }

    private Map<String, KitDefinition> immutableDefinitions(
            Map<String, KitDefinition> definitions
    ) {
        var copy = new LinkedHashMap<String, KitDefinition>();
        definitions.forEach((id, definition) -> copy.put(
                key(id),
                validatedCopy(definition)
        ));

        return Collections.unmodifiableMap(copy);
    }

    private KitDefinition validatedCopy(KitDefinition source) {
        var copy = copyDefinition(source);
        validate(copy);
        return copy;
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
        if (!kit.permission.isEmpty()
                && !kit.permission.startsWith("cellulosesz.")
        ) {
            throw new IllegalArgumentException("Kit permission must use the cellulosesz.* namespace");
        }

        if (kit.cooldownSeconds < -1L
                || kit.cooldownSeconds > Long.MAX_VALUE / 1000L
        ) {
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
            if (item.slot < 0 || !slots.add(item.slot)) {
                throw new IllegalArgumentException("Invalid or duplicate kit slot");
            }

            item.stack = item.validatedStack();
        });
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

    @Override
    public void stopAccepting() {
        reloads.stopAccepting();
        claims.stopAccepting();
        definitions.stopAccepting();
    }

    @Override
    public CompletableFuture<Void> drain() {
        return CompletableFuture.allOf(
                reloads.drain(),
                claims.drain(),
                definitions.drain()
        );
    }

    private record RuntimeState(
            Map<String, KitDefinition> kits,
            boolean createStarterKitWhenEmpty,
            boolean chargeKitCost
    ) {

        private RuntimeState withKits(Map<String, KitDefinition> next) {
            return new RuntimeState(next, createStarterKitWhenEmpty, chargeKitCost);
        }

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
                    true,
                    changed,
                    hadPrevious,
                    previous,
                    reserved,
                    KitClaimResult.failure("service.kit.persistence-failed")
            );
        }

        private static CooldownReservation rejected(KitClaimResult failure) {
            return new CooldownReservation(
                    false,
                    false,
                    false,
                    0L,
                    0L,
                    failure
            );
        }

    }

    private final class PreparedReload implements PreparedModuleReload {

        private final RuntimeState previous;
        private final Map<String, KitDefinition> candidate;
        private final boolean createStarterKitWhenEmpty;
        private final boolean chargeKitCost;
        private final long preparedVersion;
        private final boolean persistStarter;
        private final PreparedModuleReload delegate;
        private boolean starterCreated;
        private long committedVersion = -1L;

        private PreparedReload(
                RuntimeState previous,
                Map<String, KitDefinition> candidate,
                boolean createStarterKitWhenEmpty,
                boolean chargeKitCost,
                long preparedVersion,
                boolean persistStarter
        ) {
            this.previous = previous;
            this.candidate = candidate;
            this.createStarterKitWhenEmpty = createStarterKitWhenEmpty;
            this.chargeKitCost = chargeKitCost;
            this.preparedVersion = preparedVersion;
            this.persistStarter = persistStarter;
            this.delegate = PreparedReloads.of(
                    () -> reloads.submit(this::commitSerialized),
                    () -> reloads.submit(this::rollbackSerialized)
            );
        }

        private CompletionStage<Void> commitSerialized() {
            synchronized (DefaultKitService.this) {
                if (mutationVersion != preparedVersion) {
                    return CompletableFuture.failedFuture(new IllegalStateException(
                            "Kit definitions changed after reload preparation"
                    ));
                }
            }

            CompletionStage<Void> persistence;
            if (persistStarter) {
                var starter = candidate.get("starter");
                persistence = storage
                        .save(path("starter"), requireNonNull(starter, "starter"))
                        .thenRun(() -> starterCreated = true);
            } else {
                persistence = CompletableFuture.completedFuture(null);
            }

            return persistence.thenRun(() -> {
                synchronized (DefaultKitService.this) {
                    if (mutationVersion != preparedVersion) {
                        throw new IllegalStateException(
                                "Kit definitions changed during reload commit"
                        );
                    }

                    state = new RuntimeState(
                            candidate,
                            createStarterKitWhenEmpty,
                            chargeKitCost
                    );
                    mutationVersion++;
                    committedVersion = mutationVersion;
                }
            });
        }

        private CompletionStage<Void> rollbackSerialized() {
            synchronized (DefaultKitService.this) {
                if (committedVersion >= 0L) {
                    if (mutationVersion != committedVersion) {
                        return CompletableFuture.failedFuture(new IllegalStateException(
                                "Kit definitions changed after reload commit"
                        ));
                    }

                    state = previous;
                    mutationVersion++;
                }
            }

            return cleanupStaleStarter();
        }

        private CompletionStage<Void> cleanupStaleStarter() {
            if (!starterCreated) {
                return CompletableFuture.completedFuture(null);
            }

            return storage.delete(path("starter")).thenApply(_ -> null);
        }

        @Override
        public CompletionStage<Void> commit() {
            return delegate.commit();
        }

        @Override
        public CompletionStage<Void> rollback() {
            return delegate.rollback();
        }

    }

}

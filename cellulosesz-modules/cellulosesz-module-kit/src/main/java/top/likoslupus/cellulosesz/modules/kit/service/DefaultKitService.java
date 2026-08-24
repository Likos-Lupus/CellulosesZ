package top.likoslupus.cellulosesz.modules.kit.service;

import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.economy.TransactionResult;
import top.likoslupus.cellulosesz.api.kit.KitClaimResult;
import top.likoslupus.cellulosesz.api.kit.KitDefinition;
import top.likoslupus.cellulosesz.api.kit.KitItem;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.user.CellUser;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.api.user.UserUpdate;
import top.likoslupus.cellulosesz.common.item.InventoryItemSnapshot;
import top.likoslupus.cellulosesz.common.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.core.concurrent.KeyedSerialAsyncQueue;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.core.lifecycle.legacy.AsyncCloseable;
import top.likoslupus.cellulosesz.core.lifecycle.legacy.AsyncInitializable;
import top.likoslupus.cellulosesz.core.module.PreparedModuleReload;
import top.likoslupus.cellulosesz.core.module.PreparedReloads;
import top.likoslupus.cellulosesz.core.storage.StorageService;
import top.likoslupus.cellulosesz.modules.kit.KitConfig;
import top.likoslupus.cellulosesz.modules.kit.persistence.KitDocument;
import top.likoslupus.cellulosesz.modules.kit.persistence.KitMapper;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
        while (current instanceof CompletionException
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
                                KitDocument.class
                        )
                        .thenApply(documents -> documents.stream()
                                .map(KitMapper::toDomain)
                                .toList())
                        .thenApply(loaded -> {
                            var next = new LinkedHashMap<String, KitDefinition>();
                            loaded.stream()
                                    .map(this::validated)
                                    .sorted(Comparator.comparing(KitDefinition::id))
                                    .forEach(kit -> {
                                        var previous = next.put(key(kit.id()), kit);
                                        if (previous != null) {
                                            throw new IllegalStateException(
                                                    "Duplicate kit id: " + kit.id());
                                        }
                                    });

                            var persistStarter = next.isEmpty() && createStarterKitWhenEmpty;
                            if (persistStarter) {
                                var starter = validated(starterKit());
                                next.put(key(starter.id()), starter);
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

    private KitDefinition validated(KitDefinition kit) {
        requireNonNull(kit, "kit");
        var normalizedId = key(kit.id());
        if (!normalizedId.equals(kit.id()) || !KIT_ID.matcher(normalizedId).matches()) {
            throw new IllegalArgumentException("Invalid or non-normalized kit id: " + kit.id());
        }

        if (kit.permission() != null) {
            if (!kit.permission().startsWith("cellulosesz.")) {
                throw new IllegalArgumentException(
                        "Kit permission must use the cellulosesz.* namespace"
                );
            }
        }

        var cooldownSeconds = kit.cooldown().getSeconds();
        if (cooldownSeconds > Long.MAX_VALUE / 1000L) {
            throw new IllegalArgumentException("Kit cooldown is outside the supported range");
        }

        var slots = new HashSet<Integer>();
        kit.items().forEach(item -> {
            if (!slots.add(item.slot())) {
                throw new IllegalArgumentException("Duplicate kit slot: " + item.slot());
            }
        });

        return kit;
    }

    private String key(String id) {
        return requireNonNull(id, "id").trim().toLowerCase(Locale.ROOT);
    }

    private KitDefinition starterKit() {
        return new KitDefinition(
                "starter",
                "Starter",
                "cellulosesz.kit.starter",
                Duration.ofSeconds(86_400L),
                BigDecimal.ZERO,
                List.of(
                        new KitItem(0, "{\"id\":\"minecraft:bread\",\"count\":16}"),
                        new KitItem(1, "{\"id\":\"minecraft:stone_sword\",\"count\":1}")
                )
        );
    }

    private Map<String, KitDefinition> immutableDefinitions(
            Map<String, KitDefinition> definitions
    ) {
        var copy = new LinkedHashMap<String, KitDefinition>();
        definitions.forEach((id, definition) -> copy.put(key(id), validated(definition)));
        return Map.copyOf(copy);
    }

    @Override
    public synchronized List<KitDefinition> kits() {
        return List.copyOf(state.kits().values());
    }

    @Override
    public synchronized KitDefinition kit(String id) {
        return state.kits().get(key(id));
    }

    @Override
    public CompletableFuture<Void> save(KitDefinition kit) {
        var candidate = validated(kit);
        var id = key(candidate.id());

        return reloads.submit(() -> enqueueDefinitionMutation(
                id,
                () -> storage
                        .save(path(candidate.id()), KitMapper.fromDomain(candidate))
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
        var candidate = validated(kit);
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
        var cooldownKey = cooldownKey(kit.id());
        return users
                .update(
                        player.uuid(),
                        user -> reserveCooldown(
                                user,
                                cooldownKey,
                                kit.cooldown().getSeconds()
                        )
                )
                .thenCompose(reservation -> {
                    if (!reservation.accepted()) {
                        return CompletableFuture.completedFuture(reservation.failure());
                    }

                    var cost = kit.cost();
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
                                                "kit " + kit.id()
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
                                    var snapshots = kit.items().stream()
                                            .map(item -> new InventoryItemSnapshot(
                                                    item.slot(),
                                                    item.stack()
                                            ))
                                            .toList();
                                    var prepared = inventory.prepareGrant(player, snapshots);
                                    if (!prepared.successful()) {
                                        return PlatformResult.<Void>failure(
                                                prepared.status(),
                                                prepared.detail()
                                        );
                                    }
                                    return prepared.value().commit();
                                })
                                .thenCompose(grantResult -> {
                                    if (grantResult.successful()) {
                                        return CompletableFuture.completedFuture(KitClaimResult.success(
                                                "service.kit.claimed",
                                                MessageArguments.builder()
                                                        .add(kit.displayName())
                                                        .build()
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
                                                    "service.kit.inventory-unavailable",
                                                    MessageArguments.empty()
                                            )
                                            : KitClaimResult.failure(
                                                    "service.kit.rollback-failed",
                                                    MessageArguments.empty()
                                            )
                                    );
                                });
                    });
                });
    }

    private String cooldownKey(String kitId) {
        return "kit:" + key(kitId);
    }

    private UserUpdate<CooldownReservation> reserveCooldown(
            CellUser user,
            String cooldownKey,
            long cooldownSeconds
    ) {
        var now = System.currentTimeMillis();
        var hadPrevious = user.cooldowns().containsKey(cooldownKey);
        var previous = (long) user.cooldowns().getOrDefault(cooldownKey, 0L);

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
                                    MessageArguments.builder().add(seconds).build()
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
                );
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
                    .thenApply(TransactionResult::success);
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
                        .save(
                                path("starter"),
                                KitMapper.fromDomain(requireNonNull(starter, "starter"))
                        )
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

            return storage
                    .delete(path("starter"))
                    .thenApply(_ -> null);
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

package top.likoslupus.cellulosesz.modules.kit.service;

import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.kit.KitClaimResult;
import top.likoslupus.cellulosesz.api.kit.KitDefinition;
import top.likoslupus.cellulosesz.api.kit.KitItem;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.CellUser;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.kit.KitConfig;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class DefaultKitService implements KitService {

    private static final int CLAIM_LOCK_COUNT = 64;

    private final StorageService storage;
    private final UserService users;
    private final ItemService items;
    private final Optional<EconomyService> economy;
    private final KitConfig config;
    private final Path kitsDirectory;
    private final LinkedHashMap<String, KitDefinition> kits = new LinkedHashMap<>();
    private final Object[] claimLocks = new Object[CLAIM_LOCK_COUNT];

    public DefaultKitService(
            StorageService storage,
            UserService users,
            ItemService items,
            Optional<EconomyService> economy,
            KitConfig config,
            Path kitsDirectory
    ) {
        this.storage = storage;
        this.users = users;
        this.items = items;
        this.economy = economy;
        this.config = config;
        this.kitsDirectory = kitsDirectory;
        Arrays.setAll(claimLocks, _ -> new Object());
        reload().join();
    }

    @Override
    public synchronized CompletableFuture<Void> reload() {
        return storage.loadDirectory(kitsDirectory, KitDefinition.class)
                .thenCompose(loaded -> {
                    synchronized (this) {
                        kits.clear();
                        loaded.stream()
                                .peek(this::normalize)
                                .sorted(Comparator.comparing(kit -> kit.id))
                                .forEach(kit -> kits.put(key(kit.id), kit));
                    }

                    if (loaded.isEmpty() && config.createStarterKitWhenEmpty) {
                        return save(starterKit());
                    }

                    return CompletableFuture.completedFuture(null);
                });
    }

    @Override
    public synchronized List<KitDefinition> kits() {
        return List.copyOf(kits.values());
    }

    @Override
    public synchronized Optional<KitDefinition> kit(String id) {
        return Optional.ofNullable(kits.get(key(id)));
    }

    @Override
    public CompletableFuture<Void> save(KitDefinition kit) {
        normalize(kit);
        var id = key(kit.id);
        KitDefinition previous;
        synchronized (this) {
            previous = kits.put(id, kit);
        }
        return storage.save(path(kit.id), kit)
                .whenComplete((_, exception) -> {
                    if (exception == null) return;

                    synchronized (this) {
                        if (kits.get(id) != kit) return;

                        if (previous == null) {
                            kits.remove(id);
                        } else {
                            kits.put(id, previous);
                        }
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> delete(String id) {
        var key = key(id);
        KitDefinition previous;
        synchronized (this) {
            previous = kits.remove(key);
        }
        if (previous == null) return CompletableFuture.completedFuture(false);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.deleteIfExists(path(id));
                return true;
            } catch (IOException exception) {
                synchronized (this) {
                    kits.putIfAbsent(key, previous);
                }
                throw new CompletionException(exception);
            }
        });
    }

    @Override
    public CompletableFuture<KitClaimResult> claim(CellPlayer player, KitDefinition kit) {
        var cached = users.cached(player.uuid());
        if (cached.isEmpty()) {
            return CompletableFuture.completedFuture(
                    KitClaimResult.failure("service.kit.user-not-loaded")
            );
        }

        synchronized (claimLock(player.uuid())) {
            return CompletableFuture.completedFuture(claimLoaded(player, kit, cached.orElseThrow()));
        }
    }

    private Object claimLock(UUID uuid) {
        return claimLocks[Math.floorMod(uuid.hashCode(), claimLocks.length)];
    }

    private KitClaimResult claimLoaded(
            CellPlayer player,
            KitDefinition kit,
            CellUser user
    ) {
        normalize(kit);

        var now = System.currentTimeMillis();
        var cooldownKey = cooldownKey(kit.id);
        var alreadyClaimed = user.cooldowns.containsKey(cooldownKey);
        var availableAt = user.cooldowns.getOrDefault(cooldownKey, 0L);

        if (kit.cooldownSeconds < 0L && alreadyClaimed) {
            return KitClaimResult.failure("service.kit.once");
        }

        if (kit.cooldownSeconds >= 0L && availableAt > now) {
            var seconds = Math.max(1L, (availableAt - now + 999L) / 1000L);
            return KitClaimResult.failure(
                    "service.kit.cooldown",
                    Map.of("seconds", seconds)
            );
        }

        var hadPreviousClaim = user.cooldowns.containsKey(cooldownKey);
        var previousClaim = user.cooldowns.getOrDefault(cooldownKey, 0L);
        if (kit.cooldownSeconds != 0L) {
            user.cooldowns.put(
                    cooldownKey,
                    kit.cooldownSeconds < 0L ? Long.MAX_VALUE : now + kit.cooldownSeconds * 1000L
            );
            users.markDirty(player.uuid());
            try {
                users.save(player.uuid()).join();
            } catch (RuntimeException _) {
                restoreClaim(user.cooldowns, cooldownKey, hadPreviousClaim, previousClaim);
                users.markDirty(player.uuid());
                return KitClaimResult.failure("service.kit.persistence-failed");
            }
        }

        var cost = parseMoney(kit.cost);
        if (config.chargeKitCost && cost.signum() > 0) {
            if (economy.isEmpty()) {
                if (kit.cooldownSeconds != 0L) {
                    rollbackClaim(player.uuid(), user.cooldowns, cooldownKey, hadPreviousClaim, previousClaim);
                }
                return KitClaimResult.failure("service.kit.economy-unavailable");
            }

            var withdraw = economy.get().withdraw(
                    player.uuid(),
                    cost,
                    TransactionCause.command(player.name(), "kit " + kit.id)
            );

            if (!withdraw.success()) {
                if (kit.cooldownSeconds != 0L) {
                    rollbackClaim(player.uuid(), user.cooldowns, cooldownKey, hadPreviousClaim, previousClaim);
                }
                return KitClaimResult.failure(withdraw.message());
            }
        }

        for (var item : kit.items) {
            if (!items.give(player, item)) {
                // Keep the persisted claim marker. Some earlier items may already have been delivered, so clearing
                // it here would make one-time kits and cooldown kits repeatable after a partial inventory failure.
                return KitClaimResult.failure(
                        "service.kit.item-failed",
                        Map.of("item", item.normalizedItem())
                );
            }
        }

        return KitClaimResult.success(
                "service.kit.claimed",
                Map.of("kit", kit.displayName)
        );
    }

    private String cooldownKey(String kitId) {
        return "kit:" + key(kitId);
    }

    private void restoreClaim(
            Map<String, Long> cooldowns,
            String cooldownKey,
            boolean hadPrevious,
            long previous
    ) {
        if (hadPrevious) {
            cooldowns.put(cooldownKey, previous);
        } else {
            cooldowns.remove(cooldownKey);
        }
    }

    private BigDecimal parseMoney(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException _) {
            return BigDecimal.ZERO;
        }
    }

    private void rollbackClaim(
            UUID uuid,
            Map<String, Long> cooldowns,
            String cooldownKey,
            boolean hadPrevious,
            long previous
    ) {
        restoreClaim(cooldowns, cooldownKey, hadPrevious, previous);
        users.markDirty(uuid);
        try {
            users.save(uuid).join();
        } catch (RuntimeException _) {
            // The failed operation returned no items. Leaving the restored value dirty allows the normal save cycle to
            // retry without falsely reporting a successful claim.
        }
    }

    @Override
    public CompletableFuture<Void> resetCooldown(UUID uuid, String kitId) {
        return users.load(uuid).thenCompose(user -> {
            user.cooldowns.remove(cooldownKey(kitId));
            users.markDirty(uuid);
            return users.save(uuid);
        });
    }

    private void normalize(KitDefinition kit) {
        if (kit.id.isBlank()) kit.id = "kit";
        kit.id = key(kit.id);

        if (kit.displayName.isBlank()) kit.displayName = kit.id;
        if (kit.cost.isBlank()) kit.cost = "0.00";
        kit.items.forEach(item -> {
            if (item.count <= 0) item.count = 1;
            item.item = item.normalizedItem();
        });
    }

    private String key(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

    private KitDefinition starterKit() {
        var kit = new KitDefinition();
        kit.id = "starter";
        kit.displayName = "Starter";
        kit.permission = "cellulosesz.kit.starter";
        kit.cooldownSeconds = 86400L;
        kit.cost = "0.00";
        kit.items.add(new KitItem("minecraft:bread", 16));
        kit.items.add(new KitItem("minecraft:stone_sword", 1));
        return kit;
    }

    private Path path(String id) {
        return kitsDirectory.resolve(key(id) + ".yml");
    }

}

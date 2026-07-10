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
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.kit.KitConfig;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class DefaultKitService implements KitService {

    private final StorageService storage;
    private final UserService users;
    private final ItemService items;
    private final Optional<EconomyService> economy;
    private final KitConfig config;
    private final Path kitsDirectory;
    private final LinkedHashMap<String, KitDefinition> kits = new LinkedHashMap<>();

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
        synchronized (this) {
            kits.put(key(kit.id), kit);
        }
        return storage.save(path(kit.id), kit);
    }

    @Override
    public CompletableFuture<Boolean> delete(String id) {
        var removed = false;
        synchronized (this) {
            removed = kits.remove(key(id)) != null;
        }
        if (!removed) return CompletableFuture.completedFuture(false);
        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.deleteIfExists(path(id));
                return true;
            } catch (IOException exception) {
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<KitClaimResult> claim(CellPlayer player, KitDefinition kit) {
        return users.load(player.uuid()).thenApply(user -> {
            normalize(kit);
            var now = System.currentTimeMillis();
            var cooldownKey = cooldownKey(kit.id);
            var availableAt = user.cooldowns.getOrDefault(cooldownKey, 0L);
            if (availableAt > now) {
                var seconds = Math.max(1L, (availableAt - now + 999L) / 1000L);
                return KitClaimResult.failure("Kit 仍在冷却中，剩余 " + seconds + " 秒。 ");
            }

            var cost = parseMoney(kit.cost);
            if (config.chargeKitCost && cost.signum() > 0) {
                if (economy.isEmpty()) return KitClaimResult.failure("该 Kit 需要经济服务，但经济模块不可用。 ");
                var withdraw = economy.get()
                        .withdraw(player.uuid(), cost, TransactionCause.command(player.name(), "kit " + kit.id));
                if (!withdraw.success()) return KitClaimResult.failure(withdraw.message());
            }

            for (var item : kit.items) {
                if (!items.give(player, item)) {
                    return KitClaimResult.failure("发放物品失败: " + item.normalizedItem());
                }
            }

            if (kit.cooldownSeconds > 0L) {
                user.cooldowns.put(cooldownKey, now + kit.cooldownSeconds * 1000L);
                users.markDirty(player.uuid());
                users.save(player.uuid());
            }
            return KitClaimResult.success("已领取 Kit: " + kit.displayName);
        });
    }

    @Override
    public CompletableFuture<Void> resetCooldown(UUID uuid, String kitId) {
        return users.load(uuid).thenCompose(user -> {
            user.cooldowns.remove(cooldownKey(kitId));
            users.markDirty(uuid);
            return users.save(uuid);
        });
    }

    private String cooldownKey(String kitId) {
        return "kit:" + key(kitId);
    }

    private BigDecimal parseMoney(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
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

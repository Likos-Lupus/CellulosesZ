package top.likoslupus.cellulosesz.modules.item.service;

import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.api.user.UserState;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DefaultItemAutomationService implements ItemAutomationService {

    private final PlatformService platform;
    private final UserService users;
    private final ItemService items;
    private final ItemConfig config;

    public DefaultItemAutomationService(
            PlatformService platform,
            UserService users,
            ItemService items,
            ItemConfig config
    ) {
        this.platform = platform;
        this.users = users;
        this.items = items;
        this.config = config;
    }

    @Override
    public Optional<String> powerTool(UUID uuid, String itemId) {
        return users.cached(uuid)
                .flatMap(user -> Optional.ofNullable(
                        powerTools(user.state).get(normalize(itemId))
                ))
                .filter(command -> !command.isBlank());
    }

    @Override
    public Map<String, String> powerTools(UUID uuid) {
        return users.cached(uuid)
                .map(user -> Map.copyOf(powerTools(user.state)))
                .orElseGet(Map::of);
    }

    @Override
    public void setPowerTool(
            UUID uuid,
            String itemId,
            String command
    ) {
        users.cached(uuid).ifPresent(user -> {
            powerTools(user.state).put(normalize(itemId), stripSlash(command));
            users.markDirty(uuid);
        });
    }

    @Override
    public void clearPowerTool(UUID uuid, String itemId) {
        users.cached(uuid).ifPresent(user -> {
            powerTools(user.state).remove(normalize(itemId));
            users.markDirty(uuid);
        });
    }

    @Override
    public boolean executePowerTool(CellPlayer player) {
        if (!config.powerToolsEnabled) return false;

        var held = items.heldItemId(player);
        if (held.isEmpty()) return false;

        var command = powerTool(player.uuid(), held.get());
        return command.filter(value -> !value.isBlank())
                .map(value -> platform.dispatchPlayerCommand(player, value))
                .orElse(false);
    }

    @Override
    public boolean unlimited(UUID uuid, String itemId) {
        return users.cached(uuid)
                .map(user -> unlimitedItems(user.state)
                        .contains(normalize(itemId))
                )
                .orElse(false);
    }

    @Override
    public Set<String> unlimitedItems(UUID uuid) {
        return users.cached(uuid)
                .map(user -> Set.copyOf(unlimitedItems(user.state)))
                .orElseGet(Set::of);
    }

    @Override
    public void setUnlimited(UUID uuid, String itemId, boolean enabled) {
        users.cached(uuid).ifPresent(user -> {
            if (enabled) {
                unlimitedItems(user.state).add(normalize(itemId));
            } else {
                unlimitedItems(user.state).remove(normalize(itemId));
            }
            users.markDirty(uuid);
        });
    }

    @Override
    public void maintainUnlimited(CellPlayer player) {
        var configured = unlimitedItems(player.uuid());
        configured.forEach(itemId -> platform.maintainItemCount(
                player,
                itemId,
                Math.max(1, config.unlimitedMinimum)
        ));
    }

    private Map<String, String> powerTools(UserState state) {
        return state.powerTools;
    }

    private Set<String> unlimitedItems(UserState state) {
        return state.unlimitedItems;
    }

    private String normalize(String value) {
        var normalized = value.trim().toLowerCase();
        return normalized.indexOf(':') < 0
                ? "minecraft:%s".formatted(normalized)
                : normalized;
    }

    private String stripSlash(String command) {
        var normalized = command.trim();
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized;
    }

}

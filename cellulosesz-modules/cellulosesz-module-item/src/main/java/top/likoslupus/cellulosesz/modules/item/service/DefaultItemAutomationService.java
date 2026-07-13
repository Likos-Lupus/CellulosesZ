package top.likoslupus.cellulosesz.modules.item.service;

import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.*;

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
    public List<String> powerTool(UUID uuid, String itemId) {
        return users.cached(uuid)
                .map(user -> List.copyOf(
                        user.state.powerToolCommands.getOrDefault(
                                normalize(itemId),
                                List.of()
                        )
                ))
                .orElseGet(List::of);
    }

    @Override
    public Map<String, List<String>> powerTools(UUID uuid) {
        return users.cached(uuid)
                .map(user -> {
                    var copy = new LinkedHashMap<String, List<String>>();
                    user.state.powerToolCommands
                            .forEach((item, commands) ->
                                    copy.put(item, List.copyOf(commands))
                            );
                    return Map.copyOf(copy);
                })
                .orElseGet(Map::of);
    }

    @Override
    public void setPowerTool(
            UUID uuid,
            String itemId,
            String command
    ) {
        users.cached(uuid).ifPresent(user -> {
            user.state.powerToolCommands.put(
                    normalize(itemId),
                    new ArrayList<>(List.of(normalizeCommand(command)))
            );
            users.markDirty(uuid);
        });
    }

    @Override
    public void addPowerTool(
            UUID uuid,
            String itemId,
            String command
    ) {
        users.cached(uuid).ifPresent(user -> {
            var normalized = normalizeCommand(command);
            var commands = user.state.powerToolCommands.computeIfAbsent(
                    normalize(itemId),
                    _ -> new ArrayList<>()
            );
            if (!commands.contains(normalized)) commands.add(normalized);
            users.markDirty(uuid);
        });
    }

    @Override
    public boolean removePowerTool(
            UUID uuid,
            String itemId,
            String command
    ) {
        return users.cached(uuid)
                .map(user -> {
                    var commands = user.state.powerToolCommands.get(normalize(itemId));
                    if (commands == null) return false;

                    var removed = commands.remove(normalizeCommand(command));
                    if (commands.isEmpty()) user.state.powerToolCommands.remove(normalize(itemId));
                    if (removed) users.markDirty(uuid);
                    return removed;
                })
                .orElse(false);
    }

    @Override
    public void clearPowerTool(UUID uuid, String itemId) {
        users.cached(uuid).ifPresent(user -> {
            user.state.powerToolCommands.remove(normalize(itemId));
            users.markDirty(uuid);
        });
    }

    @Override
    public boolean executePowerTool(CellPlayer player, String clickedPlayerName) {
        if (!config.powerToolsEnabled || !powerToolsEnabled(player.uuid())) return false;

        var held = items.heldItemId(player);
        if (held.isEmpty()) return false;

        var commands = powerTool(player.uuid(), held.get());
        if (commands.isEmpty()) return false;

        var targetedClick = !clickedPlayerName.isBlank();
        var used = false;
        for (var configured : commands) {
            var targetsPlayer = configured.contains("{player}");
            if (targetsPlayer != targetedClick) continue;

            var value = configured.replace("{player}", clickedPlayerName).trim();
            if (value.startsWith("c:")) {
                var message = value.substring(2).trim();
                if (message.isBlank()) continue;

                platform.sendChatMessage(player, message);
            } else {
                if (value.isBlank()) continue;

                platform.dispatchPlayerCommand(player, value);
            }
            used = true;
        }

        return used;
    }

    @Override
    public boolean powerToolsEnabled(UUID uuid) {
        return users.cached(uuid)
                .map(user ->
                        user.preferences.powerToolsEnabled
                )
                .orElse(true);
    }

    @Override
    public void setPowerToolsEnabled(UUID uuid, boolean enabled) {
        users.cached(uuid).ifPresent(user -> {
            user.preferences.powerToolsEnabled = enabled;
            users.markDirty(uuid);
        });
    }

    @Override
    public boolean unlimited(UUID uuid, String itemId) {
        return users.cached(uuid)
                .map(user ->
                        user.state.unlimitedItems.contains(normalize(itemId))
                )
                .orElse(false);
    }

    @Override
    public Set<String> unlimitedItems(UUID uuid) {
        return users.cached(uuid)
                .map(user ->
                        Set.copyOf(user.state.unlimitedItems)
                )
                .orElseGet(Set::of);
    }

    @Override
    public void setUnlimited(
            UUID uuid,
            String itemId,
            boolean enabled
    ) {
        users.cached(uuid).ifPresent(user -> {
            if (enabled) {
                user.state.unlimitedItems.add(normalize(itemId));
            } else {
                user.state.unlimitedItems.remove(normalize(itemId));
            }
            users.markDirty(uuid);
        });
    }

    @Override
    public void maintainUnlimited(CellPlayer player, String itemId) {
        if (!unlimited(player.uuid(), itemId)) return;
        platform.maintainItemCount(
                player,
                normalize(itemId),
                Math.max(1, config.unlimitedMinimum)
        );
    }

    private String normalize(String value) {
        var normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.indexOf(':') < 0
                ? "minecraft:%s".formatted(normalized)
                : normalized;
    }

    private String normalizeCommand(String command) {
        var normalized = command.trim();
        if (normalized.startsWith("c:")) {
            return "c:" + normalized.substring(2).trim();
        }

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

}

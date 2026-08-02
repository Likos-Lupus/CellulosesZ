package top.likoslupus.cellulosesz.modules.item.service;

import top.likoslupus.cellulosesz.api.command.service.CommandDispatchOrigin;
import top.likoslupus.cellulosesz.api.command.service.PlayerChatDispatchService;
import top.likoslupus.cellulosesz.api.command.service.PlayerCommandDispatchRequest;
import top.likoslupus.cellulosesz.api.command.service.PlayerCommandDispatchService;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemPlatformService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.api.user.UserUpdate;
import top.likoslupus.cellulosesz.api.validation.TextChecks;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requirePositive;
import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNoControlCharacters;
import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public final class DefaultItemAutomationService implements ItemAutomationService {

    private final ItemPlatformService itemPlatform;
    private final PlayerChatDispatchService chat;
    private final UserService users;
    private final ItemService items;
    private final PlayerCommandDispatchService dispatch;
    private volatile ItemConfig config;

    public DefaultItemAutomationService(
            ItemPlatformService itemPlatform,
            PlayerChatDispatchService chat,
            UserService users,
            ItemService items,
            PlayerCommandDispatchService dispatch,
            ItemConfig config
    ) {
        this.itemPlatform = requireNonNull(itemPlatform, "itemPlatform");
        this.chat = requireNonNull(chat, "chat");
        this.users = requireNonNull(users, "users");
        this.items = requireNonNull(items, "items");
        this.dispatch = requireNonNull(dispatch, "dispatch");
        configure(config);
    }

    public void configure(ItemConfig config) {
        var snapshot = new ItemConfig();
        snapshot.copyFrom(requireNonNull(config, "config"));
        snapshot.validate();
        requirePositive(snapshot.unlimitedMinimum, "unlimitedMinimum");
        this.config = snapshot;
    }

    @Override
    public List<String> powerTool(UUID uuid, String itemId) {
        return users.cached(uuid)
                .map(user -> user.state().powerToolCommands().getOrDefault(
                        normalize(itemId),
                        List.of()
                ))
                .orElseGet(List::of);
    }

    @Override
    public Map<String, List<String>> powerTools(UUID uuid) {
        return users.cached(uuid)
                .map(user -> user.state().powerToolCommands())
                .orElseGet(Map::of);
    }

    @Override
    public CompletableFuture<PlatformResult<Void>> setPowerTool(
            UUID uuid,
            String itemId,
            String command
    ) {
        var item = normalize(itemId);
        var value = normalizeCommand(command);
        return updateTools(
                uuid,
                tools -> tools.put(item, List.of(value))
        );
    }

    @Override
    public CompletableFuture<PlatformResult<Void>> addPowerTool(
            UUID uuid,
            String itemId,
            String command
    ) {
        var item = normalize(itemId);
        var value = normalizeCommand(command);

        return updateTools(
                uuid,
                tools -> {
                    var commands = new ArrayList<>(tools.getOrDefault(item, List.of()));
                    if (!commands.contains(value)) {
                        commands.add(value);
                    }

                    tools.put(item, List.copyOf(commands));
                }
        );
    }

    @Override
    public CompletableFuture<PlatformResult<Boolean>> removePowerTool(
            UUID uuid,
            String itemId,
            String command
    ) {
        var item = normalize(itemId);
        var value = normalizeCommand(command);

        return users
                .update(
                        uuid,
                        user -> {
                            var tools = mutableTools(user.state().powerToolCommands());
                            var commands = new ArrayList<>(tools.getOrDefault(item, List.of()));
                            var removed = commands.remove(value);

                            if (commands.isEmpty()) {
                                tools.remove(item);
                            } else {
                                tools.put(item, List.copyOf(commands));
                            }

                            var updated = user.withState(user.state().withPowerToolCommands(tools));
                            return UserUpdate.of(updated, PlatformResult.success(removed));
                        }
                )
                .exceptionally(_ -> PlatformResult.failure(
                        PlatformOperationStatus.STORAGE_FAILURE,
                        "user-save-failed"
                ));
    }

    @Override
    public CompletableFuture<PlatformResult<Void>> clearPowerTool(UUID uuid, String itemId) {
        var item = normalize(itemId);
        return updateTools(uuid, tools -> tools.remove(item));
    }

    @Override
    public CompletableFuture<PlatformResult<Void>> clearAllPowerTools(UUID uuid) {
        return updateTools(uuid, Map::clear);
    }

    private CompletableFuture<PlatformResult<Void>> updateTools(
            UUID uuid,
            Consumer<Map<String, List<String>>> mutation
    ) {
        return users
                .updateVoid(
                        uuid,
                        user -> {
                            var tools = mutableTools(user.state().powerToolCommands());
                            mutation.accept(tools);
                            return user.withState(user.state().withPowerToolCommands(tools));
                        }
                )
                .thenApply(_ -> PlatformResult.success())
                .exceptionally(_ -> PlatformResult.failure(
                        PlatformOperationStatus.STORAGE_FAILURE,
                        "user-save-failed"
                ));
    }

    private static Map<String, List<String>> mutableTools(Map<String, List<String>> source) {
        var result = new LinkedHashMap<String, List<String>>();
        source.forEach((item, commands) ->
                result.put(item, List.copyOf(commands))
        );
        return result;
    }

    @Override
    public boolean executePowerTool(CellPlayer player, String clickedPlayerName) {
        if (!config.powerToolsEnabled || !powerToolsEnabled(player.uuid())) {
            return false;
        }

        var held = items.heldItemId(player);
        if (!held.successful() || held.value().isEmpty()) {
            return false;
        }

        var commands = powerTool(player.uuid(), held.value().orElseThrow());
        if (commands.isEmpty()) {
            return false;
        }

        var targetedClick = !clickedPlayerName.isBlank();
        var used = false;
        for (var configured : commands) {
            var targetsPlayer = configured.contains("{player}");
            if (targetsPlayer != targetedClick) {
                continue;
            }

            var value = configured.replace("{player}", clickedPlayerName).trim();
            if (!value.startsWith("c:")) {
                if (!value.isBlank()) {
                    dispatch.dispatch(PlayerCommandDispatchRequest.start(
                            player,
                            player.uuid(),
                            CommandDispatchOrigin.POWER_TOOL,
                            value
                    ));
                    used = true;
                }
            } else {
                var message = value.substring(2).trim();
                if (!message.isBlank()) {
                    used |= chat.dispatch(player, message).successful();
                }
            }
        }

        return used;
    }

    @Override
    public boolean powerToolsEnabled(UUID uuid) {
        return users.cached(uuid)
                .map(user -> user.preferences().powerToolsEnabled())
                .orElse(true);
    }

    @Override
    public CompletableFuture<PlatformResult<Void>> setPowerToolsEnabled(
            UUID uuid,
            boolean enabled
    ) {
        return users
                .updateVoid(
                        uuid,
                        user -> user.withPreferences(
                                user.preferences().withPowerToolsEnabled(enabled)
                        )
                )
                .thenApply(_ -> PlatformResult.success())
                .exceptionally(_ -> PlatformResult.failure(
                        PlatformOperationStatus.STORAGE_FAILURE,
                        "user-save-failed"
                ));
    }

    @Override
    public boolean unlimited(UUID uuid, String itemId) {
        return users.cached(uuid)
                .map(user -> user.state().unlimitedItems().contains(normalize(itemId)))
                .orElse(false);
    }

    @Override
    public Set<String> unlimitedItems(UUID uuid) {
        return users.cached(uuid)
                .map(user -> user.state().unlimitedItems())
                .orElseGet(Set::of);
    }

    @Override
    public CompletableFuture<PlatformResult<Void>> setUnlimited(
            UUID uuid,
            String itemId,
            boolean enabled
    ) {
        var item = normalize(itemId);
        return users
                .updateVoid(
                        uuid,
                        user -> {
                            var values = new LinkedHashSet<>(user.state().unlimitedItems());

                            if (enabled) {
                                values.add(item);
                            } else {
                                values.remove(item);
                            }

                            return user.withState(user.state().withUnlimitedItems(values));
                        }
                )
                .thenApply(_ -> PlatformResult.success())
                .exceptionally(_ -> PlatformResult.failure(
                        PlatformOperationStatus.STORAGE_FAILURE,
                        "user-save-failed"
                ));
    }

    @Override
    public CompletableFuture<PlatformResult<Void>> clearUnlimited(UUID uuid) {
        return users
                .updateVoid(
                        uuid,
                        user -> user.withState(user.state().withUnlimitedItems(Set.of()))
                )
                .thenApply(_ -> PlatformResult.success())
                .exceptionally(_ -> PlatformResult.failure(
                        PlatformOperationStatus.STORAGE_FAILURE,
                        "user-save-failed"
                ));
    }

    @Override
    public void maintainUnlimited(CellPlayer player, String itemId) {
        if (unlimited(player.uuid(), itemId)) {
            itemPlatform.maintainCount(
                    player,
                    normalize(itemId),
                    config.unlimitedMinimum
            );
        }
    }

    private static String normalize(String value) {
        var normalized = requireNonBlank(value, "itemId").trim().toLowerCase(Locale.ROOT);
        return normalized.indexOf(':') < 0
                ? "minecraft:" + normalized
                : normalized;
    }

    private String normalizeCommand(String command) {
        var normalized = requireNonBlank(command, "command").trim();
        normalized = TextChecks.requireMaxLength(normalized, 512, "command");
        requireNoControlCharacters(normalized, "command");

        if (normalized.startsWith("c:")) {
            return "c:" + requireNonBlank(normalized.substring(2).trim(), "message");
        }

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        return requireNonBlank(normalized, "command");
    }

}

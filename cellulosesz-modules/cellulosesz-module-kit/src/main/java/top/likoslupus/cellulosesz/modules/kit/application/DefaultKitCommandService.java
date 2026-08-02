package top.likoslupus.cellulosesz.modules.kit.application;

import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.kit.KitDefinition;
import top.likoslupus.cellulosesz.api.kit.KitItem;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public final class DefaultKitCommandService implements KitCommandService {

    private static final int MAX_DESCRIPTION_LENGTH = 128;
    private static final int MAX_DESCRIBED_ITEMS = 64;

    private final KitService kits;
    private final InventoryPlatformService inventory;
    private final PlayerResolver players;
    private final ServerThreadExecutor serverThread;

    public DefaultKitCommandService(
            KitService kits,
            InventoryPlatformService inventory,
            PlayerResolver players,
            ServerThreadExecutor serverThread
    ) {
        this.kits = requireNonNull(kits, "kits");
        this.inventory = requireNonNull(inventory, "inventory");
        this.players = requireNonNull(players, "players");
        this.serverThread = requireNonNull(serverThread, "serverThread");
    }

    @Override
    public Result list(Predicate<String> hasPermission) {
        var names = kits.kits().stream()
                .filter(kit -> kit.permission.isBlank()
                        || hasPermission.test(kit.permission)
                )
                .map(kit -> kit.id)
                .sorted()
                .toList();

        if (names.isEmpty()) {
            return success(GeneratedMessageKeys.COMMANDS_KIT_LIST_EMPTY);
        }

        return success(LocalizedMessage.of(
                GeneratedMessageKeys.COMMANDS_KIT_LIST,
                Map.of("kits", String.join(", ", names))
        ));
    }

    @Override
    public CompletableFuture<Result> claim(
            CellPlayer player,
            String rawName,
            Predicate<String> hasPermission
    ) {
        var name = normalize(rawName);
        var kit = kits.kit(name);

        if (kit.isEmpty()) {
            return CompletableFuture.completedFuture(missing(name));
        }

        var definition = kit.orElseThrow();

        if (!definition.permission.isBlank() && !hasPermission.test(definition.permission)) {
            return CompletableFuture.completedFuture(failure(
                    GeneratedMessageKeys.COMMANDS_KIT_KIT_COMMAND_ERROR_DO_NOT_PERMISSION_CLAIM_KIT
            ));
        }

        return kits.claim(player, definition)
                .handle((result, failure) -> failure != null
                        ? failed(GeneratedMessageKeys.SERVICE_KIT_PERSISTENCE_FAILED)
                        : new Result(result.success(), result.message())
                );
    }

    @Override
    public Result show(String rawName) {
        var name = normalize(rawName);
        var kit = kits.kit(name);

        if (kit.isEmpty()) {
            return missing(name);
        }

        var entries = new StringBuilder();
        var sorted = kit.orElseThrow().items.stream()
                .sorted(Comparator.comparingInt(item -> item.slot))
                .limit(MAX_DESCRIBED_ITEMS)
                .toList();

        for (var item : sorted) {
            var described = inventory.describeSnapshot(item);
            if (!described.successful() || described.value().isEmpty()) {
                return failure(GeneratedMessageKeys.COMMANDS_KIT_SHOW_KIT_COMMAND_ERROR_INVALID_ITEM);
            }

            var descriptor = described.value().orElseThrow();
            var id = truncate(descriptor.normalizedItem(), MAX_DESCRIPTION_LENGTH);
            entries.append("\n- [")
                    .append(item.slot)
                    .append("] ")
                    .append(id)
                    .append(" x")
                    .append(descriptor.count);
        }

        return success(LocalizedMessage.of(
                GeneratedMessageKeys.COMMANDS_KIT_DETAILS,
                Map.of(
                        "kit", kit.orElseThrow().displayName,
                        "entries", entries.toString()
                )
        ));
    }

    @Override
    public CompletableFuture<Result> create(
            CellPlayer player,
            String rawName,
            KitCooldown cooldown
    ) {
        var id = normalize(rawName);
        var cooldownSeconds = switch (requireNonNull(cooldown, "cooldown")) {
            case KitCooldown.Once ignored -> -1L;
            case KitCooldown.Seconds seconds -> seconds.value();
        };

        return serverThread.submit(() -> inventory.inventorySlots(player))
                .thenCompose(snapshot -> {
                    if (!snapshot.successful() || snapshot.value().isEmpty()) {
                        return CompletableFuture.completedFuture(failure(
                                GeneratedMessageKeys.COMMANDS_KIT_CREATE_KIT_COMMAND_ERROR_SNAPSHOT
                        ));
                    }

                    var slots = snapshot.value().orElseThrow();
                    if (slots.isEmpty()) {
                        return CompletableFuture.completedFuture(failure(
                                GeneratedMessageKeys.COMMANDS_KIT_CREATE_KIT_COMMAND_ERROR_EMPTY
                        ));
                    }

                    var definition = new KitDefinition();
                    definition.id = id;
                    definition.displayName = rawName;
                    definition.permission = "cellulosesz.kit." + id;
                    definition.cooldownSeconds = cooldownSeconds;
                    definition.items = slots.stream()
                            .map(slot -> new KitItem(
                                    slot.snapshot().slot,
                                    slot.snapshot().validatedStack()
                            ))
                            .toList();

                    return kits.save(definition)
                            .thenApply(_ -> success(LocalizedMessage.of(
                                    GeneratedMessageKeys.COMMANDS_KIT_CREATE_KIT_COMMAND_REPLY_CREATED_KIT,
                                    Map.of("kit", id)
                            )));
                })
                .exceptionally(_ -> failed(GeneratedMessageKeys.SERVICE_KIT_PERSISTENCE_FAILED));
    }

    @Override
    public CompletableFuture<Result> delete(String rawName) {
        var name = normalize(rawName);
        return kits.delete(name).handle((deleted, failure) -> {
            if (failure != null) {
                return failed(GeneratedMessageKeys.SERVICE_KIT_PERSISTENCE_FAILED);
            }

            if (!deleted) {
                return missingDelete(name);
            }

            return success(LocalizedMessage.of(
                    GeneratedMessageKeys.COMMANDS_KIT_DEL_KIT_COMMAND_REPLY_DELETED_KIT,
                    Map.of("kit", name)
            ));
        });
    }

    @Override
    public CompletableFuture<Result> reset(ResetRequest request) {
        var kitName = normalize(request.kit());
        var kit = kits.kit(kitName);
        if (kit.isEmpty()) {
            return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                    GeneratedMessageKeys.COMMANDS_KIT_KIT_RESET_COMMAND_ERROR_KIT,
                    Map.of("kit", kitName)
            )));
        }

        var explicitTarget = request.target()
                .map(String::trim)
                .filter(value -> !value.isEmpty());
        if (explicitTarget.isEmpty() && request.requester().isEmpty()) {
            return CompletableFuture.completedFuture(failure(
                    GeneratedMessageKeys.COMMANDS_KIT_KIT_RESET_COMMAND_ERROR_PLAYER_REQUIRED
            ));
        }

        if (explicitTarget.isPresent() && !request.canResetOthers()) {
            var requesterName = request.requester()
                    .map(p -> p.name().toLowerCase(Locale.ROOT));
            if (requesterName.isEmpty()
                    || !requesterName
                    .orElseThrow()
                    .equals(explicitTarget.orElseThrow().toLowerCase(Locale.ROOT))
            ) {
                return CompletableFuture.completedFuture(failure(
                        GeneratedMessageKeys.COMMANDS_KIT_KIT_RESET_COMMAND_ERROR_OTHERS
                ));
            }
        }

        var target = explicitTarget.orElseGet(() -> request.requester().orElseThrow().name());
        var viewer = request.requester().orElse(null);

        return players.resolve(target, viewer)
                .thenCompose(resolved -> {
                    var uuid = resolved.optionalUuid();
                    if (uuid.isEmpty()) {
                        return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                                GeneratedMessageKeys.COMMANDS_KIT_KIT_RESET_COMMAND_ERROR_PLAYER_NOT_FOUND,
                                Map.of("player", target)
                        )));
                    }

                    return kits.resetCooldown(uuid.orElseThrow(), kitName)
                            .thenApply(_ -> success(LocalizedMessage.of(
                                    GeneratedMessageKeys.COMMANDS_KIT_KIT_RESET_COMMAND_REPLY_KIT_COOLDOWN_RESET,
                                    Map.of(
                                            "kit", kitName,
                                            "player", resolved.name()
                                    )
                            )));
                })
                .exceptionally(_ -> failed(GeneratedMessageKeys.SERVICE_KIT_PERSISTENCE_FAILED));
    }

    @Override
    public List<String> kitNames() {
        return kits.kits().stream()
                .map(kit -> kit.id)
                .sorted()
                .toList();
    }

    @Override
    public List<String> claimableNames(Predicate<String> hasPermission) {
        requireNonNull(hasPermission, "hasPermission");
        return kits.kits().stream()
                .filter(kit -> kit.permission.isBlank() || hasPermission.test(kit.permission))
                .map(kit -> kit.id)
                .sorted()
                .toList();
    }

    private Result missingDelete(String name) {
        return failure(LocalizedMessage.of(
                GeneratedMessageKeys.COMMANDS_KIT_DEL_KIT_COMMAND_ERROR_KIT_DOES_NOT_EXIST,
                Map.of("kit", name)
        ));
    }

    private String truncate(String value, int maximum) {
        return value.length() <= maximum
                ? value
                : value.substring(0, maximum);
    }

    private String normalize(String value) {
        return requireNonNull(value, "name").trim().toLowerCase(Locale.ROOT);
    }

    private Result missing(String name) {
        return failure(LocalizedMessage.of(
                GeneratedMessageKeys.COMMANDS_KIT_KIT_COMMAND_ERROR_KIT_DOES_NOT_EXIST,
                Map.of("kit", name)
        ));
    }

    private Result failure(String key) {
        return failure(LocalizedMessage.of(key));
    }

    private Result failed(String key) {
        return new Result(CommandOutcome.Status.FAILED, LocalizedMessage.of(key));
    }

    private Result failure(LocalizedMessage message) {
        return new Result(false, message);
    }

    private Result success(String key) {
        return success(LocalizedMessage.of(key));
    }

    private Result success(LocalizedMessage message) {
        return new Result(true, message);
    }

}

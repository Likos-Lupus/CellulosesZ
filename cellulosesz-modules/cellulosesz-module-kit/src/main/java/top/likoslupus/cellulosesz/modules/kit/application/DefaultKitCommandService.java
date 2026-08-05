package top.likoslupus.cellulosesz.modules.kit.application;

import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.item.InventoryItemSnapshot;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.kit.KitDefinition;
import top.likoslupus.cellulosesz.api.kit.KitItem;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
                .filter(kit -> kit.permission().isEmpty()
                        || hasPermission.test(kit.permission().orElseThrow())
                )
                .map(KitDefinition::id)
                .sorted()
                .toList();

        if (names.isEmpty()) {
            return success("commands.kit.list-empty");
        }

        return success(LocalizedMessage.of(
                "commands.kit.list",
                MessageArguments.builder().add(String.join(", ", names)).build()
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

        if (definition.permission().isPresent()
                && !hasPermission.test(definition.permission().orElseThrow())
        ) {
            return CompletableFuture.completedFuture(failure(
                    "commands.kit.kit-command.error.do-not-permission-claim-kit"
            ));
        }

        return kits.claim(player, definition)
                .handle((result, failure) -> failure != null
                        ? failed("service.kit.persistence-failed")
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
        var sorted = kit.orElseThrow().items().stream()
                .sorted(Comparator.comparingInt(KitItem::slot))
                .limit(MAX_DESCRIBED_ITEMS)
                .toList();

        for (var item : sorted) {
            var described = inventory.describeSnapshot(
                    new InventoryItemSnapshot(item.slot(), item.stack())
            );

            if (!described.successful() || described.value().isEmpty()) {
                return failure("commands.kit.show-kit-command.error.invalid-item");
            }

            var descriptor = described.value().orElseThrow();
            var id = truncate(descriptor.normalizedItem(), MAX_DESCRIPTION_LENGTH);
            entries.append("\n- [")
                    .append(item.slot())
                    .append("] ")
                    .append(id)
                    .append(" x")
                    .append(descriptor.count());
        }

        return success(LocalizedMessage.of(
                "commands.kit.details",
                MessageArguments.builder()
                        .add(kit.orElseThrow().displayName())
                        .add(entries.toString())
                        .build()
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

        return serverThread
                .submit(() -> inventory.inventorySlots(player))
                .thenCompose(snapshot -> {
                    if (!snapshot.successful() || snapshot.value().isEmpty()) {
                        return CompletableFuture.completedFuture(failure(
                                "commands.kit.create-kit-command.error.snapshot"
                        ));
                    }

                    var slots = snapshot.value().orElseThrow();
                    if (slots.isEmpty()) {
                        return CompletableFuture.completedFuture(failure(
                                "commands.kit.create-kit-command.error.empty"
                        ));
                    }

                    var definition = new KitDefinition(
                            id,
                            rawName,
                            java.util.Optional.of("cellulosesz.kit." + id),
                            Duration.ofSeconds(cooldownSeconds),
                            BigDecimal.ZERO,
                            slots.stream()
                                    .map(slot -> new KitItem(
                                            slot.snapshot().slot(),
                                            slot.snapshot().stack()
                                    ))
                                    .toList()
                    );

                    return kits.save(definition)
                            .thenApply(_ -> success(LocalizedMessage.of(
                                    "commands.kit.create-kit-command.reply.created-kit",
                                    MessageArguments.builder().add(id).build()
                            )));
                })
                .exceptionally(_ -> failed("service.kit.persistence-failed"));
    }

    @Override
    public CompletableFuture<Result> delete(String rawName) {
        var name = normalize(rawName);
        return kits.delete(name).handle((deleted, failure) -> {
            if (failure != null) {
                return failed("service.kit.persistence-failed");
            }

            if (!deleted) {
                return missingDelete(name);
            }

            return success(LocalizedMessage.of(
                    "commands.kit.del-kit-command.reply.deleted-kit",
                    MessageArguments.builder().add(name).build()
            ));
        });
    }

    @Override
    public CompletableFuture<Result> reset(ResetRequest request) {
        var kitName = normalize(request.kit());
        var kit = kits.kit(kitName);

        if (kit.isEmpty()) {
            return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                    "commands.kit.kit-reset-command.error.kit",
                    MessageArguments.builder().add(kitName).build()
            )));
        }

        var explicitTarget = request.target()
                .map(String::trim)
                .filter(value -> !value.isEmpty());

        if (explicitTarget.isEmpty() && request.requester().isEmpty()) {
            return CompletableFuture.completedFuture(failure(
                    "commands.kit.kit-reset-command.error.player-required"
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
                        "commands.kit.kit-reset-command.error.others"
                ));
            }
        }

        var target = explicitTarget.orElseGet(() -> request.requester().orElseThrow().name());
        var viewer = request.requester().orElse(null);

        return players
                .resolve(target, viewer)
                .thenCompose(resolved -> {
                    var uuid = resolved.optionalUuid();
                    if (uuid.isEmpty()) {
                        return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                                "commands.kit.kit-reset-command.error.player-not-found",
                                MessageArguments.builder().add(target).build()
                        )));
                    }

                    return kits.resetCooldown(uuid.orElseThrow(), kitName)
                            .thenApply(_ -> success(LocalizedMessage.of(
                                    "commands.kit.kit-reset-command.reply.kit-cooldown-reset",
                                    MessageArguments.empty()
                            )));
                })
                .exceptionally(_ -> failed("service.kit.persistence-failed"));
    }

    @Override
    public List<String> kitNames() {
        return kits.kits().stream()
                .map(KitDefinition::id)
                .sorted()
                .toList();
    }

    @Override
    public List<String> claimableNames(Predicate<String> hasPermission) {
        requireNonNull(hasPermission, "hasPermission");
        return kits.kits().stream()
                .filter(kit -> kit.permission().isEmpty()
                        || hasPermission.test(kit.permission().orElseThrow())
                )
                .map(KitDefinition::id)
                .sorted()
                .toList();
    }

    private Result missingDelete(String name) {
        return failure(LocalizedMessage.of(
                "commands.kit.del-kit-command.error.kit-does-not-exist",
                MessageArguments.builder().add(name).build()
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
                "commands.kit.kit-command.error.kit-does-not-exist",
                MessageArguments.builder().add(name).build()
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

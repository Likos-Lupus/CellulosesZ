package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.service.ConfirmationKey;
import top.likoslupus.cellulosesz.api.command.service.ConfirmationService;
import top.likoslupus.cellulosesz.api.command.service.ConfirmationToken;
import top.likoslupus.cellulosesz.api.item.*;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.item.ItemRuntimeSettings;
import top.likoslupus.cellulosesz.modules.item.command.argument.ItemDescriptorArgument;

import java.time.Clock;
import java.time.Duration;
import java.util.*;
import org.jspecify.annotations.Nullable;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requireNonNegative;
import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requirePositive;
import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public final class ClearInventoryCommand implements CommandContributor {

    static final ConfirmationKey<ClearPayload> CONFIRMATION_KEY = new ConfirmationKey<>(
            "clearinventory",
            ClearPayload.class
    );

    private final InventoryPlatformService inventory;
    private final ItemService items;
    private final UserService users;
    private final ConfirmationService confirmations;
    private final PermissionService permissions;
    private final PlayerDirectory players;
    private final ItemRuntimeSettings config;
    private final Clock clock;

    public ClearInventoryCommand(
            InventoryPlatformService inventory,
            ItemService items,
            UserService users,
            ConfirmationService confirmations,
            PermissionService permissions,
            PlayerDirectory players,
            ItemRuntimeSettings config,
            Clock clock
    ) {
        this.inventory = requireNonNull(inventory, "inventory");
        this.items = requireNonNull(items, "items");
        this.users = requireNonNull(users, "users");
        this.confirmations = requireNonNull(confirmations, "confirmations");
        this.permissions = requireNonNull(permissions, "permissions");
        this.players = requireNonNull(players, "players");
        this.config = requireNonNull(config, "config");
        this.clock = requireNonNull(clock, "clock");
    }

    private static Target target(
            CommandContext<CommandSourceStack> command,
            @Nullable Target fixedTarget
    ) throws CommandSyntaxException {
        return fixedTarget == null
                ?
                Target.player(
                        EntityArgument.getPlayer(command, "player")
                                .getGameProfile()
                                .name()
                )
                : fixedTarget;
    }

    private static List<InventoryStackSelection> select(
            List<InventorySlotView> slots,
            InventoryClearFilter filter,
            int amount
    ) {
        var remaining = amount == 0
                ? Integer.MAX_VALUE
                : amount;
        var selected = new ArrayList<InventoryStackSelection>();

        for (var slot : slots) {
            var equipment = slot.kind() != InventorySlotKind.MAIN;

            if (equipment
                    && filter.kind()
                    != InventoryClearFilter.Kind.ALL_WITH_EQUIPMENT
            ) {
                continue;
            }

            if (filter.kind() == InventoryClearFilter.Kind.ITEM &&
                    !slot.descriptor()
                            .normalizedItem()
                            .equals(filter.itemId().orElseThrow())
            ) {
                continue;
            }

            var count = Math.min(
                    remaining,
                    slot.descriptor().count()
            );

            if (count > 0) {
                selected.add(new InventoryStackSelection(
                        slot.snapshot(),
                        count
                ));
            }

            remaining -= count;
            if (remaining == 0) {
                break;
            }
        }

        return List.copyOf(selected);
    }

    private ArgumentBuilder<CommandSourceStack, ?> playerTarget(
            CommandRegistrationContext context,
            CommandDescriptor descriptor
    ) {
        var branch = Commands.argument(
                        "player",
                        EntityArgument.player()
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        target(command, null),
                        InventoryClearFilter.inventory(),
                        0,
                        false
                ));

        return addFilterBranches(
                branch,
                context,
                descriptor,
                null
        );
    }

    private ArgumentBuilder<CommandSourceStack, ?> targetLiteral(
            CommandRegistrationContext context,
            CommandDescriptor descriptor,
            String literal,
            Target target
    ) {
        var branch = Commands.literal(literal)
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        target,
                        InventoryClearFilter.inventory(),
                        0,
                        false
                ));

        return addFilterBranches(
                branch,
                context,
                descriptor,
                target
        );
    }

    private ArgumentBuilder<CommandSourceStack, ?> allTarget(
            CommandRegistrationContext context,
            CommandDescriptor descriptor
    ) {
        var branch = Commands.literal("all")
                .requires(source -> context.hasPermission(
                        source,
                        "cellulosesz.command.clearinventory.all"
                ))
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        Target.all(),
                        InventoryClearFilter.inventory(),
                        0,
                        false
                ));

        return addFilterBranches(
                branch,
                context,
                descriptor,
                Target.all()
        );
    }

    private <T extends ArgumentBuilder<CommandSourceStack, T>> T addFilterBranches(
            T parent,
            CommandRegistrationContext context,
            CommandDescriptor descriptor,
            @Nullable Target fixedTarget
    ) {
        parent.then(Commands.literal("all")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        target(command, fixedTarget),
                        InventoryClearFilter.inventory(),
                        0,
                        false
                ))
        );

        parent.then(Commands.literal("equipment")
                .requires(source -> context.hasPermission(
                        source,
                        "cellulosesz.command.clearinventory.armor"
                ))
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        target(command, fixedTarget),
                        InventoryClearFilter.withEquipment(),
                        0,
                        false
                ))
        );

        parent.then(Commands.literal("item")
                .then(Commands.argument(
                                        "item",
                                        ItemDescriptorArgument.itemDescriptor(items, context.buildContext())
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        target(command, fixedTarget),
                                        InventoryClearFilter.item(
                                                ItemDescriptorArgument.get(command, "item").normalizedItem()
                                        ),
                                        0,
                                        false
                                ))
                                .then(Commands.argument(
                                                        "amount",
                                                        IntegerArgumentType.integer(
                                                                1,
                                                                1_000_000
                                                        )
                                                )
                                                .executes(command -> execute(
                                                        context,
                                                        command,
                                                        descriptor,
                                                        target(command, fixedTarget),
                                                        InventoryClearFilter.item(
                                                                ItemDescriptorArgument.get(
                                                                        command,
                                                                        "item"
                                                                ).normalizedItem()
                                                        ),
                                                        IntegerArgumentType.getInteger(
                                                                command,
                                                                "amount"
                                                        ),
                                                        false
                                                ))
                                )
                )
        );

        return parent;
    }

    private int confirm(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return ItemCommandSupport.sync(
                registration,
                command,
                descriptor,
                "clearinventory confirm",
                policy -> {
                    var actor = ItemCommandSupport.current(policy);

                    if (actor.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_SOURCE,
                                "player-confirmation-required"
                        );
                    }

                    var currentActor = actor.orElseThrow();
                    var payload = confirmations.consume(
                            currentActor.uuid(),
                            CONFIRMATION_KEY,
                            new ConfirmationToken(StringArgumentType.getString(command, "token"))
                    );

                    if (!payload.consumed()
                            || !payload.payload().orElseThrow()
                            .actor()
                            .equals(currentActor.uuid())
                    ) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.CONFLICT,
                                "confirmation-invalid"
                        );
                    }

                    var value = payload.payload().orElseThrow();
                    var resolved = value.targets()
                            .stream()
                            .map(players::onlinePlayer)
                            .toList();

                    if (resolved.stream().anyMatch(Optional::isEmpty)) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.NOT_FOUND,
                                "target-offline"
                        );
                    }

                    return executePrepared(
                            currentActor,
                            resolved.stream()
                                    .map(Optional::orElseThrow)
                                    .toList(),
                            value.filter(),
                            value.amount()
                    );
                }
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Target target,
            InventoryClearFilter filter,
            int amount,
            boolean confirmed
    ) {
        return ItemCommandSupport.sync(
                registration,
                command,
                descriptor,
                "clearinventory",
                policy -> {
                    var actor = policy.currentPlayer();
                    var targets = resolveTargets(target, actor);

                    if (targets.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.NOT_FOUND,
                                "target-offline"
                        );
                    }

                    if (targets.size() > config.clearMaximumTargets()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_INPUT,
                                "too-many-targets"
                        );
                    }

                    if (targets.size() > 1
                            && !policy.hasPermission(
                            "cellulosesz.command.clearinventory.multiple"
                    )) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.PERMISSION_DENIED,
                                "multiple-targets"
                        );
                    }

                    var filtered = targets.stream()
                            .filter(value -> !permissions.has(
                                    value,
                                    "cellulosesz.command.clearinventory.exempt"
                            ))
                            .sorted(Comparator.comparing(CellPlayer::uuid))
                            .toList();

                    if (filtered.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.PERMISSION_DENIED,
                                "targets-exempt"
                        );
                    }

                    var total = estimate(filtered, filter, amount);
                    var needsConfirmation = actor.isPresent()
                            &&
                            users.cached(actor.orElseThrow().uuid())
                                    .map(user ->
                                            user.preferences().confirmInventoryClears()
                                    )
                                    .orElse(true)
                            &&
                            (
                                    target.kind() == TargetKind.ALL
                                            || filtered.size() > 1
                                            || filter.kind() != InventoryClearFilter.Kind.ITEM
                                            || total >= config.clearLargeRemovalThreshold()
                            );

                    if (!confirmed
                            && needsConfirmation
                            && !policy.hasPermission(
                            "cellulosesz.command.clearinventory.bypass-confirm"
                    )) {
                        var currentActor = actor.orElseThrow();

                        var payload = new ClearPayload(
                                currentActor.uuid(),
                                filtered.stream()
                                        .map(CellPlayer::uuid)
                                        .toList(),
                                filter,
                                amount,
                                clock.millis()
                        );

                        var token = confirmations.request(
                                currentActor.uuid(),
                                CONFIRMATION_KEY,
                                payload,
                                Duration.ofSeconds(config.clearConfirmationTtlSeconds())
                        );

                        return PlatformResult.partial(
                                token.value(),
                                "confirmation-required"
                        );
                    }

                    return executePrepared(
                            actor.orElse(null),
                            filtered,
                            filter,
                            amount
                    );
                }
        );
    }

    private List<CellPlayer> resolveTargets(
            Target target,
            Optional<CellPlayer> actor
    ) {
        return switch (target.kind()) {
            case SELF -> actor.map(List::of)
                    .orElseGet(List::of);
            case PLAYER -> players.onlinePlayer(target.name())
                    .map(List::of)
                    .orElseGet(List::of);
            case ALL -> players.onlinePlayers();
        };
    }

    private int estimate(
            List<CellPlayer> targets,
            InventoryClearFilter filter,
            int amount
    ) {
        var total = 0;

        for (var target : targets) {
            var slots = inventory.inventorySlots(target);

            if (!slots.successful() || slots.value().isEmpty()) {
                continue;
            }

            var selected = select(
                    slots.value().orElseThrow(),
                    filter,
                    amount
            );

            total = Math.addExact(
                    total,
                    selected.stream()
                            .mapToInt(InventoryStackSelection::count)
                            .sum()
            );
        }

        return total;
    }

    private PlatformResult<ClearSummary> executePrepared(
            CellPlayer actor,
            List<CellPlayer> targets,
            InventoryClearFilter filter,
            int amount
    ) {
        var plans = new ArrayList<Plan>();
        var noMatch = 0;

        for (var target : targets) {
            var slots = inventory.inventorySlots(target);

            if (!slots.successful() || slots.value().isEmpty()) {
                return PlatformResult.failure(
                        slots.status(),
                        "inventory-snapshot-failed"
                );
            }

            var selections = select(
                    slots.value().orElseThrow(),
                    filter,
                    amount
            );

            if (selections.isEmpty()) {
                noMatch++;
                continue;
            }

            var prepared = inventory.prepareRemoval(
                    target,
                    selections
            );

            if (!prepared.successful() || prepared.value().isEmpty()) {
                return PlatformResult.failure(
                        prepared.status(),
                        "inventory-prepare-failed"
                );
            }

            plans.add(new Plan(
                    target,
                    prepared.value().orElseThrow(),
                    selections.stream()
                            .mapToInt(InventoryStackSelection::count)
                            .sum()
            ));
        }

        if (plans.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.NOT_FOUND,
                    "no-matches"
            );
        }

        var committed = new ArrayList<Plan>();

        for (var plan : plans) {
            var commit = plan.mutation().commit();
            if (!commit.successful()) {
                var rollbackFailures = new ArrayList<String>();
                for (var committedPlan : committed) {
                    var rollback = committedPlan.mutation().rollback();
                    if (!rollback.successful()) {
                        rollbackFailures.add(
                                committedPlan.player().uuid() + ": " + rollback.detail()
                        );
                    }
                }

                return PlatformResult.failure(
                        rollbackFailures.isEmpty()
                                ? commit.status()
                                : PlatformOperationStatus.ROLLBACK_FAILED,
                        rollbackFailures.isEmpty()
                                ? commit.detail()
                                : "Commit failed (" + commit.detail()
                                        + "); rollback failures: "
                                        + String.join("; ", rollbackFailures)
                );
            }

            committed.add(plan);
        }

        confirmations.clear(actor.uuid(), CONFIRMATION_KEY);

        return PlatformResult.success(new ClearSummary(
                committed.size(),
                noMatch,
                committed.stream()
                        .mapToInt(Plan::removed)
                        .sum()
        ));
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "clearinventory",
                "cellulosesz.command.clearinventory",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("clearinventory")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        Target.self(),
                        InventoryClearFilter.inventory(),
                        0,
                        false
                ))
                .then(targetLiteral(
                        context,
                        descriptor,
                        "self",
                        Target.self()
                ))
                .then(Commands.literal("player")
                        .requires(source -> context.hasPermission(
                                source,
                                "cellulosesz.command.clearinventory.others"
                        ))
                        .then(playerTarget(context, descriptor))
                )
                .then(allTarget(context, descriptor))
                .then(Commands.literal("confirm")
                        .then(Commands.argument(
                                                "token",
                                                StringArgumentType.word()
                                        )
                                        .executes(command -> confirm(
                                                context,
                                                command,
                                                descriptor
                                        ))
                        )
                );

        var node = context.registerDirect(
                moduleId(),
                descriptor,
                List.of("ci", "clearinv"),
                "commands.description.clearinventory",
                "/clearinventory [self|player <player>|all] "
                        + "[all|equipment|item <item> [amount]] "
                        + "| /clearinventory confirm <token>",
                root
        );

        context.registerAlias(
                moduleId(),
                descriptor,
                "ci",
                node
        );
        context.registerAlias(
                moduleId(),
                descriptor,
                "clearinv",
                node
        );
    }

    private enum TargetKind {

        SELF,
        PLAYER,
        ALL

    }

    private record Target(
            TargetKind kind,
            String name
    ) {

        private Target {
            requireNonNull(kind, "kind");
            name = requireNonNull(name, "name");

            if (kind == TargetKind.PLAYER) {
                requireNonBlank(name, "name");
            }
        }

        static Target self() {
            return new Target(TargetKind.SELF, "");
        }

        static Target player(String name) {
            return new Target(TargetKind.PLAYER, name);
        }

        static Target all() {
            return new Target(TargetKind.ALL, "");
        }

    }

    record ClearPayload(
            UUID actor,
            List<UUID> targets,
            InventoryClearFilter filter,
            int amount,
            long issuedAt
    ) {

        ClearPayload {
            requireNonNull(actor, "actor");
            targets = List.copyOf(targets);
            requireNonNull(filter, "filter");
            requireNonNegative(amount, "amount");
            requireNonNegative(issuedAt, "issuedAt");
        }

    }

    private record Plan(
            CellPlayer target,
            InventoryMutation mutation,
            int removed
    ) {

        private Plan {
            requireNonNull(target, "target");
            requireNonNull(mutation, "mutation");
            requirePositive(removed, "removed");
        }

    }

    private record ClearSummary(
            int successful,
            int noMatch,
            int removed
    ) {

        private ClearSummary {
            requirePositive(successful, "successful");
            requireNonNegative(noMatch, "noMatch");
            requirePositive(removed, "removed");
        }

    }

}

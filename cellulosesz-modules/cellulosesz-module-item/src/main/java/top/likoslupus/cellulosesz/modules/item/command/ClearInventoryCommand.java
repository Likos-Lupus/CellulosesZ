package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.service.ConfirmationService;
import top.likoslupus.cellulosesz.api.item.*;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.time.Duration;
import java.util.*;

public final class ClearInventoryCommand implements CellCommand {

    private static final String ACTION = "clearinventory";
    private static final UUID CONSOLE_ACTOR = new UUID(0L, 0L);

    private final PlatformService platform;
    private final InventoryPlatformService inventory;
    private final ItemService items;
    private final UserService users;
    private final ConfirmationService confirmations;
    private final PermissionService permissions;
    private final ItemConfig config;

    public ClearInventoryCommand(
            PlatformService platform,
            InventoryPlatformService inventory,
            ItemService items,
            UserService users,
            ConfirmationService confirmations,
            PermissionService permissions,
            ItemConfig config
    ) {
        this.platform = platform;
        this.inventory = inventory;
        this.items = items;
        this.users = users;
        this.confirmations = confirmations;
        this.permissions = permissions;
        this.config = config;
    }

    private static String normalize(String value) {
        var id = value.strip().toLowerCase(Locale.ROOT);
        return id.contains(":") ? id : "minecraft:" + id;
    }

    @Override
    public List<String> aliases() {
        return List.of("ci", "clearinv");
    }

    @Override
    public String permission() {
        return "cellulosesz.command.clearinventory";
    }

    @Override
    public String usage() {
        return "/clearinventory [self|player|*] [item|*|**] [amount] | /clearinventory confirm <token>";
    }

    @Override
    public String name() {
        return "clearinventory";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length == 2 && invocation.args()[0].equalsIgnoreCase("confirm")) {
            return confirm(invocation, invocation.args()[1]);
        }
        var parsed = parse(invocation);
        if (parsed.isEmpty()) return 0;
        return prepare(invocation, parsed.orElseThrow(), false, Optional.empty());
    }

    private int confirm(CommandInvocation invocation, String token) {
        var actor = actor(invocation);
        var payload = confirmations.consume(actor, ACTION, token, ClearPayload.class);
        if (payload.isEmpty() || !payload.orElseThrow().actor().equals(actor)) {
            invocation.errorKey("commands.item.clearinventory.confirm-invalid");
            return 0;
        }
        return prepare(invocation, payload.orElseThrow().request(), true, Optional.of(payload.orElseThrow()
                .targetUuids()));
    }

    private int prepare(
            CommandInvocation invocation, ClearRequest request, boolean confirmed,
            Optional<List<UUID>> expectedTargets
    ) {
        var resolved = resolveTargets(invocation, request);
        if (resolved.isEmpty()) return 0;
        var targets = resolved.orElseThrow();
        var targetUuids = targets.stream().map(CellPlayer::uuid).sorted().toList();
        if (expectedTargets.isPresent() && !expectedTargets.orElseThrow().equals(targetUuids)) {
            invocation.errorKey("commands.item.clearinventory.confirm-mismatch");
            return 0;
        }
        if (targets.size() > config.clearMaximumTargets) {
            invocation.errorKey("commands.item.clearinventory.too-many-targets", Map.of("maximum", config.clearMaximumTargets));
            return 0;
        }

        var plans = new ArrayList<Plan>();
        var noMatch = 0;
        var exempt = 0;
        var total = 0;
        for (var target : targets) {
            if (permissions.has(target.nativeHandle(), "cellulosesz.command.clearinventory.exempt")) {
                exempt++;
                continue;
            }
            var slots = inventory.inventorySlots(target);
            if (!slots.successful() || slots.value().isEmpty()) {
                invocation.errorKey("commands.item.clearinventory.snapshot-failed", Map.of("player", target.name()));
                return 0;
            }
            var selections = select(slots.value().orElseThrow(), request);
            if (selections.isEmpty()) {
                noMatch++;
                continue;
            }
            var count = selections.stream().mapToInt(InventoryStackSelection::count).sum();
            var mutation = platform.prepareInventoryRemoval(target, selections);
            if (mutation.isEmpty()) {
                invocation.errorKey("commands.item.clearinventory.prepare-failed", Map.of("player", target.name()));
                return 0;
            }
            total = Math.addExact(total, count);
            plans.add(new Plan(target, mutation.orElseThrow(), count));
        }

        if (plans.isEmpty()) {
            invocation.errorKey("commands.item.clearinventory.no-matches", Map.of("targets", targets.size(), "exempt", exempt));
            return 0;
        }

        if (!confirmed && requiresConfirmation(invocation, request, targets.size(), total)) {
            var actor = actor(invocation);
            if (actor.equals(CONSOLE_ACTOR)) {
                invocation.errorKey("commands.item.clearinventory.console-confirmation-unavailable");
                return 0;
            }
            var payload = new ClearPayload(actor, request, targetUuids, System.currentTimeMillis());
            var token = confirmations.request(
                    actor,
                    ACTION,
                    payload,
                    Duration.ofSeconds(config.clearConfirmationTtlSeconds)
            );
            invocation.replyKey("commands.item.clearinventory.confirm", Map.of(
                    "token", token,
                    "targets", targets.size(),
                    "items", total,
                    "seconds", config.clearConfirmationTtlSeconds
            ));
            return 1;
        }

        var committed = new ArrayList<Plan>();
        for (var plan : plans) {
            if (!plan.mutation().commit()) {
                var rollbackFailed = committed.stream()
                        .sorted(Comparator.comparing(planEntry -> planEntry.target().uuid()))
                        .map(Plan::mutation)
                        .map(InventoryMutation::rollback)
                        .filter(result -> !result)
                        .count();
                invocation.errorKey(
                        rollbackFailed == 0
                                ? "commands.item.clearinventory.conflict"
                                : "commands.item.clearinventory.rollback-failed",
                        Map.of("player", plan.target().name(), "rollbackFailures", rollbackFailed)
                );
                return 0;
            }
            committed.add(plan);
        }
        confirmations.clear(actor(invocation), ACTION);
        plans.forEach(plan -> invocation.replyKey("commands.item.clearinventory.target-success", Map.of(
                "player", plan.target().name(),
                "removed", plan.removed()
        )));
        invocation.replyKey("commands.item.clearinventory.summary", Map.of(
                "successful", plans.size(),
                "noMatch", noMatch,
                "failed", 0,
                "exempt", exempt,
                "removed", total
        ));
        return plans.size();
    }

    private Optional<ClearRequest> parse(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length > 3) return usage(invocation);
        var self = platform.player(invocation);
        final TargetKind kind;
        final String targetName;
        var index = 0;
        if (args.length == 0) {
            if (self.isEmpty()) {
                invocation.errorKey("commands.item.clearinventory.console-target-required");
                return Optional.empty();
            }
            kind = TargetKind.SELF;
            targetName = self.orElseThrow().name();
        } else if (args[0].equalsIgnoreCase("self")) {
            if (self.isEmpty()) {
                invocation.errorKey("commands.item.clearinventory.console-target-required");
                return Optional.empty();
            }
            kind = TargetKind.SELF;
            targetName = self.orElseThrow().name();
            index = 1;
        } else if (args[0].equals("*")) {
            if (!invocation.hasPermission("cellulosesz.command.clearinventory.all")) return denied(invocation);
            kind = TargetKind.ALL;
            targetName = "*";
            index = 1;
        } else {
            if (!invocation.hasPermission("cellulosesz.command.clearinventory.others")) return denied(invocation);
            kind = TargetKind.PLAYER;
            targetName = args[0];
            index = 1;
        }

        var filter = "*";
        var includeEquipment = false;
        if (index < args.length) {
            filter = args[index++];
            includeEquipment = filter.equals("**");
            if (includeEquipment && !invocation.hasPermission("cellulosesz.command.clearinventory.armor")) {
                return denied(invocation);
            }
            if (!filter.equals("*") && !filter.equals("**")) {
                var parsed = items.parse(filter);
                if (parsed.isEmpty() || !items.valid(parsed.orElseThrow())) {
                    invocation.errorKey("commands.item.clearinventory.unknown-item", Map.of("item", filter));
                    return Optional.empty();
                }
                filter = normalize(parsed.orElseThrow().item);
            }
        }
        var amount = 0;
        if (index < args.length) {
            try {
                amount = Integer.parseInt(args[index++]);
            } catch (NumberFormatException failure) {
                return invalidAmount(invocation);
            }
            if (amount <= 0) return invalidAmount(invocation);
        }
        if (index != args.length) return usage(invocation);
        return Optional.of(new ClearRequest(kind, targetName, filter, amount, includeEquipment));
    }

    private Optional<List<CellPlayer>> resolveTargets(CommandInvocation invocation, ClearRequest request) {
        if (request.kind() == TargetKind.ALL) {
            var players = platform.onlinePlayers();
            if (players.size() > 1 && !invocation.hasPermission("cellulosesz.command.clearinventory.multiple")) {
                return deniedTargets(invocation);
            }
            return Optional.of(players);
        }
        if (request.kind() == TargetKind.SELF) return platform.player(invocation).map(List::of);
        var target = invocation.resolvePlayer(request.targetName()).online();
        if (target.isEmpty()) {
            invocation.errorKey("commands.common.unknown-player", Map.of("player", request.targetName()));
            return Optional.empty();
        }
        return Optional.of(List.of(target.orElseThrow()));
    }

    private List<InventoryStackSelection> select(List<InventorySlotView> slots, ClearRequest request) {
        var result = new ArrayList<InventoryStackSelection>();
        var remaining = request.amount() == 0 ? Integer.MAX_VALUE : request.amount();
        for (var slot : slots) {
            if (slot.kind() != InventorySlotKind.MAIN && !request.includeEquipment()) continue;
            if (!request.filter().equals("*") && !request.filter().equals("**")
                    && !normalize(slot.descriptor().item).equals(request.filter())) continue;
            var count = Math.min(remaining, slot.descriptor().count);
            if (count > 0) result.add(new InventoryStackSelection(slot.snapshot(), count));
            remaining -= count;
            if (remaining == 0) break;
        }
        return List.copyOf(result);
    }

    private boolean requiresConfirmation(CommandInvocation invocation, ClearRequest request, int targets, int total) {
        if (invocation.hasPermission("cellulosesz.command.clearinventory.bypass-confirm")) return false;
        var player = platform.player(invocation);
        var preference = player.flatMap(value -> users.cached(value.uuid()))
                .map(user -> user.preferences.confirmInventoryClears)
                .orElse(true);
        return preference && (request.kind() == TargetKind.ALL || targets > 1
                || request.filter().equals("*") || request.filter().equals("**")
                || total >= config.clearLargeRemovalThreshold);
    }

    private UUID actor(CommandInvocation invocation) {
        return platform.player(invocation).map(CellPlayer::uuid).orElse(CONSOLE_ACTOR);
    }

    private Optional<ClearRequest> usage(CommandInvocation invocation) {
        invocation.errorKey("commands.item.clearinventory.usage", Map.of("usage", usage()));
        return Optional.empty();
    }

    private Optional<ClearRequest> invalidAmount(CommandInvocation invocation) {
        invocation.errorKey("commands.item.clearinventory.invalid-amount");
        return Optional.empty();
    }

    private Optional<ClearRequest> denied(CommandInvocation invocation) {
        invocation.errorKey("commands.common.no-permission");
        return Optional.empty();
    }

    private Optional<List<CellPlayer>> deniedTargets(CommandInvocation invocation) {
        invocation.errorKey("commands.common.no-permission");
        return Optional.empty();
    }

    private enum TargetKind {
        SELF,
        PLAYER,
        ALL
    }

    private record ClearRequest(
            TargetKind kind,
            String targetName,
            String filter,
            int amount,
            boolean includeEquipment
    ) {

        public ClearRequest {
            if (targetName.isBlank() || filter.isBlank() || amount < 0)
                throw new IllegalArgumentException("Invalid clear request");
        }

    }

    private record ClearPayload(
            UUID actor,
            ClearRequest request,
            List<UUID> targetUuids,
            long requestedAt
    ) {

        private ClearPayload {
            targetUuids = List.copyOf(targetUuids);
        }

    }

    private record Plan(
            CellPlayer target,
            InventoryMutation mutation,
            int removed
    ) {

    }

}

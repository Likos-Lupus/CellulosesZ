package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.entity.SpawnMobRequest;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SpawnMobCommand implements CellCommand {

    private final PlatformService platform;
    private final EntityPlatformService entities;
    private final WorldConfig config;

    public SpawnMobCommand(
            PlatformService platform,
            EntityPlatformService entities,
            WorldConfig config
    ) {
        this.platform = platform;
        this.entities = entities;
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.spawnmob";
    }

    @Override
    public String usage() {
        return "/spawnmob <entity> [amount] [player]";
    }

    @Override
    public String name() {
        return "spawnmob";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length < 1 || invocation.args().length > 3) return usage(invocation);
        var entity = normalize(invocation.args()[0]);
        if (!entities.validLivingEntity(entity)) {
            invocation.errorKey("commands.world.spawnmob.invalid-entity", Map.of("entity", invocation.args()[0]));
            return 0;
        }
        if (!entityPermission(invocation, entity)) {
            invocation.errorKey("commands.common.no-permission");
            return 0;
        }
        var amount = 1;
        if (invocation.args().length >= 2) {
            try {
                amount = Integer.parseInt(invocation.args()[1]);
                if (amount < 1 || amount > config.spawnMobMaximumAmount) throw new NumberFormatException();
            } catch (NumberFormatException failure) {
                invocation.errorKey("commands.world.spawnmob.invalid-amount", Map.of("maximum", config.spawnMobMaximumAmount));
                return 0;
            }
        }
        var anchor = anchor(invocation);
        if (anchor.isEmpty()) return 0;
        var result = entities.spawnMob(new SpawnMobRequest(entity, amount, anchor.orElseThrow()));
        if (!result.successful() || result.value().isEmpty()) {
            invocation.platformError(result.status());
            return 0;
        }
        var value = result.value().orElseThrow();
        invocation.replyKey(value.spawned() == value.requested()
                ? "commands.world.spawnmob.success"
                : "commands.world.spawnmob.partial", Map.of(
                "entity", value.entityId(),
                "requested", value.requested(),
                "spawned", value.spawned(),
                "player", anchor.orElseThrow().name()
        ));
        return value.spawned();
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.world.spawnmob.usage", Map.of("usage", usage()));
        return 0;
    }

    private static String normalize(String value) {
        var id = value.strip().toLowerCase(Locale.ROOT);
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static boolean entityPermission(CommandInvocation invocation, String entity) {
        var node = entity.replace(':', '.').replaceAll("[^a-z0-9_.-]", "_");
        return invocation.hasPermission("cellulosesz.command.spawnmob.entity.*")
                || invocation.hasPermission("cellulosesz.command.spawnmob.entity." + node);
    }

    private Optional<CellPlayer> anchor(CommandInvocation invocation) {
        if (invocation.args().length == 3) {
            if (!invocation.hasPermission("cellulosesz.command.spawnmob.others")) {
                invocation.errorKey("commands.common.no-permission");
                return Optional.empty();
            }
            var target = invocation.resolvePlayer(invocation.args()[2]).online();
            if (target.isEmpty())
                invocation.errorKey("commands.common.unknown-player", Map.of("player", invocation.args()[2]));
            return target;
        }
        var self = platform.player(invocation);
        if (self.isEmpty()) invocation.errorKey("commands.world.spawnmob.console-target-required");
        return self;
    }

}

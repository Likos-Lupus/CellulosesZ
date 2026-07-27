package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.SpawnerRequest;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.Locale;
import java.util.Map;

public final class SpawnerCommand implements CellCommand {

    private final PlatformService platform;
    private final WorldPlatformService worlds;
    private final EntityPlatformService entities;
    private final WorldConfig config;

    public SpawnerCommand(
            PlatformService platform,
            WorldPlatformService worlds,
            EntityPlatformService entities,
            WorldConfig config
    ) {
        this.platform = platform;
        this.worlds = worlds;
        this.entities = entities;
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.spawner";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/spawner <entity> [delayTicks]";
    }

    @Override
    public String name() {
        return "spawner";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length < 1 || invocation.args().length > 2) return usage(invocation);
        var entity = normalize(invocation.args()[0]);
        if (!entities.validLivingEntity(entity)) {
            invocation.errorKey("commands.world.spawner.invalid-entity", Map.of("entity", invocation.args()[0]));
            return 0;
        }
        if (!entityPermission(invocation, entity)) {
            invocation.errorKey("commands.common.no-permission");
            return 0;
        }
        var delay = config.spawnerDefaultDelayTicks;
        if (invocation.args().length == 2) {
            if (!invocation.hasPermission("cellulosesz.command.spawner.delay")) {
                invocation.errorKey("commands.common.no-permission");
                return 0;
            }
            try {
                delay = Integer.parseInt(invocation.args()[1]);
                if (delay < config.spawnerMinimumDelayTicks || delay > config.spawnerMaximumDelayTicks) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException failure) {
                invocation.errorKey("commands.world.spawner.invalid-delay", Map.of(
                        "minimum", config.spawnerMinimumDelayTicks,
                        "maximum", config.spawnerMaximumDelayTicks
                ));
                return 0;
            }
        }
        var result = worlds.configureSpawner(
                platform.player(invocation).orElseThrow(),
                config.targetDistance,
                new SpawnerRequest(entity, delay)
        );
        if (!result.successful() || result.value().isEmpty()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey("commands.world.spawner.success", Map.of(
                "entity", result.value().orElseThrow().entityId(),
                "delay", result.value().orElseThrow().delayTicks()
        ));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.world.spawner.usage", Map.of("usage", usage()));
        return 0;
    }

    private static String normalize(String value) {
        var id = value.strip().toLowerCase(Locale.ROOT);
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static boolean entityPermission(CommandInvocation invocation, String entity) {
        var node = entity.replace(':', '.').replaceAll("[^a-z0-9_.-]", "_");
        return invocation.hasPermission("cellulosesz.command.spawner.entity.*")
                || invocation.hasPermission("cellulosesz.command.spawner.entity." + node);
    }

}

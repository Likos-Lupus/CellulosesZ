package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.TreeType;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

abstract class AbstractTreeCommand implements CellCommand {

    private final PlatformService platform;
    private final WorldPlatformService worlds;
    private final WorldConfig config;
    private final Set<TreeType> allowed;
    private final TreeType defaultType;

    AbstractTreeCommand(
            PlatformService platform,
            WorldPlatformService worlds,
            WorldConfig config,
            Set<TreeType> allowed,
            TreeType defaultType
    ) {
        this.platform = platform;
        this.worlds = worlds;
        this.config = config;
        this.allowed = Set.copyOf(allowed);
        this.defaultType = defaultType;
        if (!this.allowed.contains(defaultType)) {
            throw new IllegalArgumentException("Default tree type must be allowed");
        }
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 1) return usageError(invocation);
        var type = invocation.args().length == 0
                ? Optional.of(defaultType)
                : parseType(invocation.args()[0]);
        if (type.isEmpty() || !allowed.contains(type.orElseThrow())) return usageError(invocation);
        var result = worlds.generateTree(
                platform.player(invocation).orElseThrow(),
                config.treeTargetDistance,
                type.orElseThrow()
        );
        if (!result.successful() || result.value().isEmpty()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey("commands.world.tree.success", Map.of(
                "type", type.orElseThrow().name().toLowerCase(Locale.ROOT),
                "world", result.value().orElseThrow().location().world
        ));
        return 1;
    }

    private int usageError(CommandInvocation invocation) {
        invocation.errorKey("commands.world.tree.usage", Map.of("usage", usage()));
        return 0;
    }

    protected Optional<TreeType> parseType(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "tree", "oak" -> Optional.of(TreeType.OAK);
            case "birch" -> Optional.of(TreeType.BIRCH);
            case "redwood", "spruce" -> Optional.of(TreeType.SPRUCE);
            case "redmushroom" -> Optional.of(TreeType.RED_MUSHROOM);
            case "brownmushroom" -> Optional.of(TreeType.BROWN_MUSHROOM);
            case "jungle" -> Optional.of(TreeType.JUNGLE);
            case "junglebush" -> Optional.of(TreeType.JUNGLE_BUSH);
            case "swamp" -> Optional.of(TreeType.SWAMP);
            default -> Optional.empty();
        };
    }

}

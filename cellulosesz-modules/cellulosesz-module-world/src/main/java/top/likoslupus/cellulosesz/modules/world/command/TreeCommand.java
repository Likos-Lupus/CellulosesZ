package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.world.TreeType;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.world.command.argument.TreeTypeArgument;
import top.likoslupus.cellulosesz.modules.world.config.WorldRuntimeSettings;

import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public final class TreeCommand implements CommandContributor {

    private static final Set<TreeType> ALLOWED = Set.of(
            TreeType.OAK,
            TreeType.BIRCH,
            TreeType.SPRUCE,
            TreeType.RED_MUSHROOM,
            TreeType.BROWN_MUSHROOM,
            TreeType.JUNGLE,
            TreeType.JUNGLE_BUSH,
            TreeType.SWAMP
    );
    private final WorldPlatformService worlds;
    private final WorldRuntimeSettings config;

    public TreeCommand(
            WorldPlatformService worlds,
            WorldRuntimeSettings config
    ) {
        this.worlds = requireNonNull(worlds, "worlds");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        register(context, "tree", "cellulosesz.command.tree", false);
    }

    private void register(
            CommandRegistrationContext context,
            String rootName,
            String permission,
            boolean large
    ) {
        var descriptor = WorldCommandSupport.descriptor(
                rootName,
                permission,
                CommandSourceKind.PLAYER_ONLY
        );
        var root = Commands.literal(rootName)
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        large
                                ? TreeType.LARGE_OAK
                                : TreeType.OAK
                ))
                .then(Commands.argument(
                                        "type",
                                        large
                                                ? TreeTypeArgument.bigTreeType(ALLOWED)
                                                : TreeTypeArgument.treeType(ALLOWED)
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        TreeTypeArgument.get(command, "type")
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description." + rootName,
                "/" + rootName + " [type]",
                root
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            TreeType type
    ) {
        return WorldCommandSupport.sync(
                registration,
                command,
                descriptor,
                "tree " + type,
                policy -> {
                    var player = WorldCommandSupport.current(policy);
                    return player
                            .<PlatformResult<?>>map(value -> worlds.generateTree(
                                    value,
                                    config.treeTargetDistance(),
                                    type
                            ))
                            .orElseGet(() -> PlatformResult.failure(
                                    PlatformOperationStatus.INVALID_SOURCE,
                                    "player-only"
                            ));
                }
        );
    }

    @Override
    public String moduleId() {
        return WorldCommandSupport.MODULE;
    }

}

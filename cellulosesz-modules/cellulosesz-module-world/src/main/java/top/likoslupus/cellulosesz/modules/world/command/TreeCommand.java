package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.world.TreeType;
import top.likoslupus.cellulosesz.common.world.WorldPlatformService;
import top.likoslupus.cellulosesz.modules.world.command.argument.TreeTypes;
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
        var descriptor = WorldCommandSupport.descriptor(
                "tree",
                "cellulosesz.command.tree",
                CommandSourceKind.PLAYER_ONLY
        );
        var root = Commands.literal("tree")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        TreeType.OAK
                ))
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((_, builder) -> CommandSuggestionSupport.suggest(
                                TreeTypes.normalSuggestions(),
                                builder
                        ))
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                TreeTypes.parseNormal(
                                        StringArgumentType.getString(command, "type"),
                                        ALLOWED
                                )
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tree",
                "/tree [type]",
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
                policy -> WorldCommandSupport.current(policy)
                        .<PlatformResult<?>>map(value -> worlds.generateTree(
                                value,
                                config.treeTargetDistance(),
                                type
                        ))
                        .orElseGet(() -> PlatformResult.failure(
                                PlatformOperationStatus.INVALID_SOURCE,
                                "player-only"
                        ))
        );
    }

    @Override
    public String moduleId() {
        return WorldCommandSupport.MODULE;
    }

}

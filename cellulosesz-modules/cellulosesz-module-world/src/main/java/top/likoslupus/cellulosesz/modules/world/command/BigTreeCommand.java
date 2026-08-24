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

public final class BigTreeCommand implements CommandContributor {

    private static final Set<TreeType> ALLOWED = Set.of(
            TreeType.LARGE_OAK,
            TreeType.LARGE_SPRUCE,
            TreeType.LARGE_JUNGLE,
            TreeType.DARK_OAK
    );
    private final WorldPlatformService worlds;
    private final WorldRuntimeSettings config;

    public BigTreeCommand(
            WorldPlatformService worlds,
            WorldRuntimeSettings config
    ) {
        this.worlds = requireNonNull(worlds, "worlds");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "bigtree",
                "cellulosesz.command.bigtree",
                CommandSourceKind.PLAYER_ONLY
        );
        var root = Commands.literal("bigtree")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        TreeType.LARGE_OAK
                ))
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((_, builder) -> CommandSuggestionSupport.suggest(
                                TreeTypes.largeSuggestions(),
                                builder
                        ))
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                TreeTypes.parseLarge(
                                        StringArgumentType.getString(command, "type"),
                                        ALLOWED
                                )
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.bigtree",
                "/bigtree [tree|oak|redwood|spruce|jungle|darkoak]",
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
                "bigtree " + type,
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

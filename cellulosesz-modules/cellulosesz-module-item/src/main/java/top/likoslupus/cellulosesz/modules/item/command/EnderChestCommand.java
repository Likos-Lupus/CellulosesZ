package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.PlayerNameArgument;
import top.likoslupus.cellulosesz.modules.item.application.InventoryCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class EnderChestCommand implements CommandContributor {

    private final InventoryCommandService service;
    private final PlayerDirectory players;

    public EnderChestCommand(
            InventoryCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "enderchest",
                "cellulosesz.item.enderchest",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("enderchest")
                .executes(command -> executeSelf(
                        context,
                        command,
                        descriptor
                ))
                .then(Commands.argument(
                                        "player",
                                        PlayerNameArgument.playerName()
                                )
                                .requires(source -> context.permissions().has(
                                        source,
                                        "cellulosesz.item.enderchest.others"
                                ))
                                .suggests((_, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                players::onlinePlayerNames,
                                                builder
                                        )
                                )
                                .executes(command -> executeTarget(
                                        context,
                                        command,
                                        descriptor
                                ))
                );

        var node = context.registerDirect(
                moduleId(),
                descriptor,
                List.of("echest"),
                "commands.description.enderchest",
                "/enderchest [player]",
                root
        );

        context.registerAlias(
                moduleId(),
                descriptor,
                "echest",
                node
        );
    }

    private int executeSelf(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "enderchest self",
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    return player
                            .<PlatformResult<?>>map(value ->
                                    service.openEnderChest(value, value)
                            )
                            .orElseGet(() -> PlatformResult.failure(
                                    PlatformOperationStatus.INVALID_SOURCE,
                                    "target-required"
                            ));
                }
        );
    }

    private int executeTarget(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "enderchest target",
                policy -> {
                    var target = ItemCommandSupport.target(
                            policy,
                            players,
                            PlayerNameArgument.get(command, "player")
                    );
                    var viewer = ItemCommandSupport.current(policy)
                            .or(() -> target);

                    if (target.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.NOT_FOUND,
                                "player-offline"
                        );
                    }

                    return service.openEnderChest(
                            viewer.orElseThrow(),
                            target.orElseThrow()
                    );
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}

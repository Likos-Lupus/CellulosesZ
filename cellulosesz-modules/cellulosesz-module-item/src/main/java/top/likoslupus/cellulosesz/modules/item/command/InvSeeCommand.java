package top.likoslupus.cellulosesz.modules.item.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
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

public final class InvSeeCommand implements CommandContributor {

    private final InventoryCommandService service;
    private final PlayerDirectory players;

    public InvSeeCommand(
            InventoryCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "invsee",
                "cellulosesz.item.invsee",
                CommandSourceKind.PLAYER_ONLY
        );

        var target = Commands.argument(
                        "player",
                        PlayerNameArgument.playerName()
                )
                .suggests((ignored, builder) ->
                        CommandSuggestionSupport.suggest(
                                players::onlinePlayerNames,
                                builder
                        )
                )
                .executes(command -> ItemCommandSupport.sync(
                        context,
                        command,
                        descriptor,
                        "invsee",
                        policy -> {
                            var viewer = ItemCommandSupport.current(policy);
                            var targetPlayer = ItemCommandSupport.target(
                                    policy,
                                    players,
                                    PlayerNameArgument.get(
                                            command,
                                            "player"
                                    )
                            );

                            if (viewer.isEmpty()
                                    || targetPlayer.isEmpty()) {
                                return PlatformResult.failure(
                                        PlatformOperationStatus.NOT_FOUND,
                                        "player-offline"
                                );
                            }

                            return service.openInventory(
                                    viewer.orElseThrow(),
                                    targetPlayer.orElseThrow()
                            );
                        }
                ));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.invsee",
                "/invsee <player>",
                Commands.literal("invsee").then(target)
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}

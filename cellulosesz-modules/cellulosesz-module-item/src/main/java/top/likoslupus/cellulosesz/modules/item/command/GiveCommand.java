package top.likoslupus.cellulosesz.modules.item.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.PlayerNameArgument;
import top.likoslupus.cellulosesz.modules.item.application.ItemCommandService;
import top.likoslupus.cellulosesz.modules.item.command.argument.ItemDescriptorArgument;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class GiveCommand implements CommandContributor {

    private final ItemCommandService service;
    private final ItemService items;
    private final PlayerDirectory players;

    public GiveCommand(
            ItemCommandService service,
            ItemService items,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.items = requireNonNull(items, "items");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "give",
                "cellulosesz.item.give",
                CommandSourceKind.ANY
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
                .then(Commands.argument(
                                        "item",
                                        ItemDescriptorArgument.itemDescriptor(items)
                                )
                                .executes(command -> ItemCommandSupport.sync(
                                        context,
                                        command,
                                        descriptor,
                                        "give item",
                                        policy -> {
                                            var player = ItemCommandSupport.target(
                                                    policy,
                                                    players,
                                                    PlayerNameArgument.get(
                                                            command,
                                                            "player"
                                                    )
                                            );

                                            if (player.isEmpty()) {
                                                return PlatformResult.failure(
                                                        PlatformOperationStatus.NOT_FOUND,
                                                        "player-offline"
                                                );
                                            }

                                            return service.grant(
                                                    player.orElseThrow(),
                                                    ItemDescriptorArgument.get(
                                                            command,
                                                            "item"
                                                    ),
                                                    policy.hasPermission(
                                                            "cellulosesz.item.give.blacklist"
                                                    ),
                                                    policy.hasPermission(
                                                            "cellulosesz.item.give.oversized"
                                                    )
                                            );
                                        }
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.give",
                "/give <player> <item-descriptor>",
                Commands.literal("give").then(target)
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}

package top.likoslupus.cellulosesz.modules.item.command;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.item.application.InventoryCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class InvSeeCommand implements CommandContributor {

    private final InventoryCommandService service;

    public InvSeeCommand(InventoryCommandService service) {
        this.service = requireNonNull(service, "service");
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
                        EntityArgument.player()
                )
                .executes(command -> {
                    var targetPlayer = MinecraftPlayers.wrap(
                            EntityArgument.getPlayer(command, "player")
                    );

                    return ItemCommandSupport.sync(
                            context,
                            command,
                            descriptor,
                            "invsee",
                            policy -> {
                                var viewer = ItemCommandSupport.current(policy);
                                if (viewer.isEmpty()) {
                                    return PlatformResult.failure(
                                            PlatformOperationStatus.INVALID_SOURCE,
                                            "player-only"
                                    );
                                }

                                return service.openInventory(
                                        viewer.orElseThrow(),
                                        targetPlayer
                                );
                            }
                    );
                });

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

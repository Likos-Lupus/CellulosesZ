package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.item.HatAction;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.item.application.InventoryCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class HatCommand implements CommandContributor {

    private final InventoryCommandService service;

    public HatCommand(InventoryCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "hat",
                "cellulosesz.command.hat",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("hat")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        HatAction.SWAP
                ))
                .then(Commands.literal("remove")
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                HatAction.REMOVE
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.hat",
                "/hat [remove]",
                root
        );
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            HatAction action
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "hat " + action.name().toLowerCase(),
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    return player
                            .<PlatformResult<?>>map(value ->
                                    service.hat(
                                            value,
                                            action,
                                            policy.hasPermission(
                                                    "cellulosesz.command.hat.ignore-binding"
                                            )
                                    )
                            )
                            .orElseGet(() -> PlatformResult.failure(
                                    PlatformOperationStatus.INVALID_SOURCE,
                                    "player-only"
                            ));
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}

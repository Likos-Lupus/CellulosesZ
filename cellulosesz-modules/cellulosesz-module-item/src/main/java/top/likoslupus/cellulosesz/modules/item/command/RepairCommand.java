package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.item.RepairScope;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.item.application.ItemCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class RepairCommand implements CommandContributor {

    private final ItemCommandService service;

    public RepairCommand(ItemCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "repair",
                "cellulosesz.item.repair",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("repair")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        RepairScope.HAND
                ))
                .then(Commands.literal("hand")
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                RepairScope.HAND
                        ))
                )
                .then(Commands.literal("all")
                        .requires(source -> context.permissions().has(
                                source,
                                "cellulosesz.item.repair.all"
                        ))
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                RepairScope.ALL
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.repair",
                "/repair [hand|all]",
                root
        );
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            RepairScope scope
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "repair " + scope.name().toLowerCase(),
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    return player
                            .<PlatformResult<?>>map(value -> service.repair(value, scope))
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

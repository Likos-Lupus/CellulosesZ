package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class PowerToolListCommand implements CommandContributor {

    private static final int PAGE_SIZE = 8;

    private final ItemAutomationService automation;

    public PowerToolListCommand(ItemAutomationService automation) {
        this.automation = requireNonNull(automation, "automation");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "powertoollist",
                "cellulosesz.command.powertoollist",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("powertoollist")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        1
                ))
                .then(Commands.argument(
                                "page",
                                IntegerArgumentType.integer(1)
                        )
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                IntegerArgumentType.getInteger(
                                        command,
                                        "page"
                                )
                        )));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.powertoollist",
                "/powertoollist [page]",
                root
        );
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            int page
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "powertoollist page=" + page,
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    if (player.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_SOURCE,
                                "player-only"
                        );
                    }

                    var entries = automation.powerTools(
                                    player.orElseThrow().uuid()
                            ).entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .skip((long) (page - 1) * PAGE_SIZE)
                            .limit(PAGE_SIZE)
                            .toList();

                    return entries.isEmpty() ?
                            PlatformResult.failure(
                                    PlatformOperationStatus.NOT_FOUND,
                                    "page-empty"
                            ) :
                            PlatformResult.success(entries);
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}

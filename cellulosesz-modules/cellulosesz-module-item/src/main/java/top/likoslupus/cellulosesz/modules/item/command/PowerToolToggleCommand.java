package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.StringArgumentType;
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
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.ToggleModes;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class PowerToolToggleCommand implements CommandContributor {

    private final ItemAutomationService automation;

    public PowerToolToggleCommand(ItemAutomationService automation) {
        this.automation = requireNonNull(automation, "automation");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "powertooltoggle",
                "cellulosesz.command.powertooltoggle",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("powertooltoggle")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        null
                ))
                .then(Commands.argument(
                                "state",
                                StringArgumentType.word()
                        )
                        .suggests((_, builder) -> CommandSuggestionSupport.suggest(
                                ToggleModes::suggestions,
                                builder
                        ))
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                ToggleModes.parse(StringArgumentType.getString(command, "state"))
                                        .enabled()
                        )));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.powertooltoggle",
                "/powertooltoggle [on|off]",
                root
        );
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            @Nullable Boolean requested
    ) {
        return ItemCommandSupport.async(
                context,
                command,
                descriptor,
                "powertooltoggle",
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    if (player.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                PlatformResult.failure(
                                        PlatformOperationStatus.INVALID_SOURCE,
                                        "player-only"
                                )
                        );
                    }

                    var currentPlayer = player.orElseThrow();
                    var uuid = currentPlayer.uuid();

                    if (automation.powerTools(uuid).isEmpty()) {
                        return CompletableFuture.completedFuture(
                                PlatformResult.failure(
                                        PlatformOperationStatus.INVALID_STATE,
                                        "no-bindings"
                                )
                        );
                    }

                    var enabled = requested == null
                            ? !automation.powerToolsEnabled(uuid)
                            : requested;

                    return automation.setPowerToolsEnabled(uuid, enabled);
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}

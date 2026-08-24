package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.ToggleModes;
import top.likoslupus.cellulosesz.core.command.service.ConfirmationService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class ClearInventoryConfirmToggleCommand implements CommandContributor {

    private final UserService users;
    private final ConfirmationService confirmations;

    public ClearInventoryConfirmToggleCommand(
            UserService users,
            ConfirmationService confirmations
    ) {
        this.users = requireNonNull(users, "users");
        this.confirmations = requireNonNull(
                confirmations,
                "confirmations"
        );
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "clearinventoryconfirmtoggle",
                "cellulosesz.command.clearinventoryconfirmtoggle",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("clearinventoryconfirmtoggle")
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
                                        ToggleModes.parse(StringArgumentType.getString(
                                                command,
                                                "state"
                                        )).enabled()
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.clearinventoryconfirmtoggle",
                "/clearinventoryconfirmtoggle [on|off]",
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
                "clearinventory confirmation",
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
                    var currentUser = users.cached(currentPlayer.uuid());
                    var current = currentUser == null
                            || currentUser.preferences().confirmInventoryClears();
                    var enabled = requested == null
                            ? !current
                            : requested;

                    return users
                            .updateVoid(
                                    currentPlayer.uuid(),
                                    user -> user.withPreferences(
                                            user.preferences().withConfirmInventoryClears(enabled)
                                    )
                            )
                            .thenApply(_ -> {
                                confirmations.clear(
                                        currentPlayer.uuid(),
                                        ClearInventoryCommand.CONFIRMATION_KEY
                                );
                                return PlatformResult.success();
                            })
                            .exceptionally(_ ->
                                    PlatformResult.failure(
                                            PlatformOperationStatus.STORAGE_FAILURE,
                                            "user-save-failed"
                                    )
                            );
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}

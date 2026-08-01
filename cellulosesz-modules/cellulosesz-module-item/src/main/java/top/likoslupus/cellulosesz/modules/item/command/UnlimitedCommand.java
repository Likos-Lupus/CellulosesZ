package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class UnlimitedCommand implements CommandContributor {

    private final ItemAutomationService automation;
    private final ItemService items;

    public UnlimitedCommand(
            ItemAutomationService automation,
            ItemService items
    ) {
        this.automation = requireNonNull(automation, "automation");
        this.items = requireNonNull(items, "items");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "unlimited",
                "cellulosesz.item.unlimited",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("unlimited")
                .executes(command -> held(
                        context,
                        command,
                        descriptor,
                        null
                ))
                .then(Commands.literal("on")
                        .executes(command -> held(
                                context,
                                command,
                                descriptor,
                                true
                        )))
                .then(Commands.literal("off")
                        .executes(command -> held(
                                context,
                                command,
                                descriptor,
                                false
                        )))
                .then(Commands.literal("list")
                        .executes(command -> list(
                                context,
                                command,
                                descriptor
                        )))
                .then(Commands.literal("clear")
                        .executes(command -> clear(
                                context,
                                command,
                                descriptor
                        )));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.unlimited",
                "/unlimited [on|off|list|clear]",
                root
        );
    }

    private int held(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            @Nullable Boolean requested
    ) {
        return ItemCommandSupport.async(
                context,
                command,
                descriptor,
                "unlimited",
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
                    var held = items.heldItemId(currentPlayer);

                    if (held.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                PlatformResult.failure(
                                        PlatformOperationStatus.INVALID_STATE,
                                        "empty-hand"
                                )
                        );
                    }

                    var item = held.orElseThrow();
                    var enabled = requested == null ?
                            !automation.unlimited(
                                    currentPlayer.uuid(),
                                    item
                            ) :
                            requested;

                    return automation
                            .setUnlimited(
                                    currentPlayer.uuid(),
                                    item,
                                    enabled
                            )
                            .thenApply(result -> {
                                if (result.successful() && enabled) {
                                    automation.maintainUnlimited(currentPlayer);
                                }

                                return result;
                            });
                }
        );
    }

    private int list(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "unlimited list",
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    return player
                            .<PlatformResult<?>>map(value ->
                                    PlatformResult.success(
                                            automation.unlimitedItems(
                                                    value.uuid()
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

    private int clear(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return ItemCommandSupport.async(
                context,
                command,
                descriptor,
                "unlimited clear",
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    return player
                            .map(value -> automation.clearUnlimited(value.uuid()))
                            .orElseGet(() -> CompletableFuture.completedFuture(
                                    PlatformResult.failure(
                                            PlatformOperationStatus.INVALID_SOURCE,
                                            "player-only"
                                    )
                            ));
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}

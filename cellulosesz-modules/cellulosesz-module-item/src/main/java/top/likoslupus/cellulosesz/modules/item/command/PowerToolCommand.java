package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class PowerToolCommand implements CommandContributor {

    private final ItemAutomationService automation;
    private final ItemService items;

    public PowerToolCommand(
            ItemAutomationService automation,
            ItemService items
    ) {
        this.automation = requireNonNull(automation, "automation");
        this.items = requireNonNull(items, "items");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "powertool",
                "cellulosesz.item.powertool",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("powertool")
                .then(Commands.literal("clear")
                        .executes(command -> held(
                                context,
                                command,
                                descriptor,
                                automation::clearPowerTool,
                                "clear"
                        )))
                .then(Commands.literal("clearall")
                        .executes(command -> all(
                                context,
                                command,
                                descriptor,
                                automation::clearAllPowerTools,
                                "clearall"
                        )))
                .then(Commands.literal("list")
                        .executes(command -> list(
                                context,
                                command,
                                descriptor
                        )))
                .then(Commands.literal("toggle")
                        .executes(command -> toggle(
                                context,
                                command,
                                descriptor
                        )))
                .then(Commands.literal("add")
                        .then(Commands.argument(
                                        "command",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command -> held(
                                        context,
                                        command,
                                        descriptor,
                                        (uuid, item) -> automation.addPowerTool(
                                                uuid,
                                                item,
                                                StringArgumentType.getString(
                                                        command,
                                                        "command"
                                                )
                                        ),
                                        "add"
                                ))))
                .then(Commands.literal("remove")
                        .then(Commands.argument(
                                        "command",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command -> heldBoolean(
                                        context,
                                        command,
                                        descriptor,
                                        (uuid, item) -> automation.removePowerTool(
                                                uuid,
                                                item,
                                                StringArgumentType.getString(
                                                        command,
                                                        "command"
                                                )
                                        ),
                                        "remove"
                                ))))
                .then(Commands.literal("command")
                        .then(Commands.argument(
                                        "command",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command -> held(
                                        context,
                                        command,
                                        descriptor,
                                        (uuid, item) -> automation.setPowerTool(
                                                uuid,
                                                item,
                                                StringArgumentType.getString(
                                                        command,
                                                        "command"
                                                )
                                        ),
                                        "command"
                                ))))
                .then(Commands.literal("chat")
                        .then(Commands.argument(
                                        "message",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command -> held(
                                        context,
                                        command,
                                        descriptor,
                                        (uuid, item) -> automation.setPowerTool(
                                                uuid,
                                                item,
                                                "c:" + StringArgumentType.getString(
                                                        command,
                                                        "message"
                                                )
                                        ),
                                        "chat"
                                ))));

        var node = context.registerDirect(
                moduleId(),
                descriptor,
                List.of("pt"),
                "commands.description.powertool",
                "/powertool <clear|clearall|list|toggle|add|remove|command|chat>",
                root
        );

        context.registerAlias(
                moduleId(),
                descriptor,
                "pt",
                node
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
                "powertool list",
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    return player
                            .<PlatformResult<?>>map(value ->
                                    PlatformResult.success(
                                            automation.powerTools(value.uuid())
                                    )
                            )
                            .orElseGet(() -> PlatformResult.failure(
                                    PlatformOperationStatus.INVALID_SOURCE,
                                    "player-only"
                            ));
                }
        );
    }

    private int toggle(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return all(
                context,
                command,
                descriptor,
                uuid -> automation.setPowerToolsEnabled(
                        uuid,
                        !automation.powerToolsEnabled(uuid)
                ),
                "toggle"
        );
    }

    private int all(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Function<UUID, CompletableFuture<PlatformResult<Void>>> operation,
            String audit
    ) {
        return ItemCommandSupport.async(
                context,
                command,
                descriptor,
                "powertool " + audit,
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    return player.map(value -> operation.apply(value.uuid()))
                            .orElseGet(() -> CompletableFuture.completedFuture(
                                    PlatformResult.failure(
                                            PlatformOperationStatus.INVALID_SOURCE,
                                            "player-only"
                                    )
                            ));
                }
        );
    }

    private int held(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            BiFunction<
                    UUID,
                    String,
                    CompletableFuture<PlatformResult<Void>>
                    > operation,
            String audit
    ) {
        return ItemCommandSupport.async(
                context,
                command,
                descriptor,
                "powertool " + audit,
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

                    return operation.apply(
                            currentPlayer.uuid(),
                            held.orElseThrow()
                    );
                }
        );
    }

    private int heldBoolean(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            BiFunction<
                    UUID,
                    String,
                    CompletableFuture<PlatformResult<Boolean>>
                    > operation,
            String audit
    ) {
        return ItemCommandSupport.async(
                context,
                command,
                descriptor,
                "powertool " + audit,
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

                    return operation.apply(
                            currentPlayer.uuid(),
                            held.orElseThrow()
                    );
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}

package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.item.SkullRequest;
import top.likoslupus.cellulosesz.api.item.SkullResult;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.item.application.InventoryCommandService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class SkullCommand implements CommandContributor {

    private final InventoryCommandService service;
    private final InventoryPlatformService inventory;

    public SkullCommand(
            InventoryCommandService service,
            InventoryPlatformService inventory
    ) {
        this.service = requireNonNull(service, "service");
        this.inventory = requireNonNull(inventory, "inventory");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "skull",
                "cellulosesz.command.skull",
                CommandSourceKind.PLAYER_ONLY
        );

        var owner = Commands.argument(
                        "owner",
                        StringArgumentType.word()
                )
                .executes(command -> modify(
                        context,
                        command,
                        descriptor
                ))
                .then(Commands.argument(
                                "player",
                                EntityArgument.player()
                        )
                        .requires(source ->
                                context.permissions().has(
                                        source,
                                        "cellulosesz.command.skull.others"
                                ) && context.permissions().has(
                                        source,
                                        "cellulosesz.command.skull.spawn.others"
                                )
                        )
                        .executes(command -> spawnOther(
                                context,
                                command,
                                descriptor
                        )));

        var root = Commands.literal("skull")
                .requires(source -> context.permissions().has(
                        source,
                        "cellulosesz.command.skull"
                ))
                .executes(command -> spawnSelf(
                        context,
                        command,
                        descriptor
                ))
                .then(owner);

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.skull",
                "/skull [owner] [player]",
                root
        );
    }

    private int spawnSelf(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return run(
                context,
                command,
                descriptor,
                policy -> {
                    var self = ItemCommandSupport.current(policy);

                    if (self.isEmpty()
                            || !policy.hasPermission(
                            "cellulosesz.command.skull.spawn"
                    )) {
                        return null;
                    }

                    var player = self.orElseThrow();

                    return new SkullRequest(
                            player.name(),
                            player,
                            true,
                            Optional.empty()
                    );
                }
        );
    }

    private int modify(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return run(
                context,
                command,
                descriptor,
                policy -> {
                    var self = ItemCommandSupport.current(policy);

                    if (self.isEmpty()
                            || !policy.hasPermission(
                            "cellulosesz.command.skull.modify"
                    )) {
                        return null;
                    }

                    var player = self.orElseThrow();
                    var held = inventory.heldSlot(player);

                    if (!held.successful() || held.value().isEmpty()) {
                        return null;
                    }

                    return new SkullRequest(
                            StringArgumentType.getString(command, "owner"),
                            player,
                            false,
                            Optional.of(
                                    held.value()
                                            .orElseThrow()
                                            .snapshot()
                            )
                    );
                }
        );
    }

    private int spawnOther(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) throws CommandSyntaxException {
        var target = MinecraftPlayers.wrap(
                EntityArgument.getPlayer(command, "player")
        );

        return run(
                context,
                command,
                descriptor,
                _ -> new SkullRequest(
                        StringArgumentType.getString(command, "owner"),
                        target,
                        true,
                        Optional.empty()
                )
        );
    }

    private int run(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Function<
                    MinecraftCommandPolicyContext,
                    @Nullable SkullRequest
                    > request
    ) {
        return ItemCommandSupport.async(
                context,
                command,
                descriptor,
                "skull profile",
                policy -> {
                    var value = request.apply(policy);

                    if (value == null) {
                        return CompletableFuture.completedFuture(
                                PlatformResult.failure(
                                        PlatformOperationStatus.INVALID_STATE,
                                        "invalid-skull-request"
                                )
                        );
                    }

                    @SuppressWarnings("unchecked")
                    var future = (CompletableFuture<PlatformResult<SkullResult>>) service.skull(
                            value
                    );
                    return future;
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}

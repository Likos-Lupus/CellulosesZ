package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.item.PotionItemRequest;
import top.likoslupus.cellulosesz.modules.item.application.ItemCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class PotionCommand implements CommandContributor {

    private final ItemCommandService service;

    public PotionCommand(ItemCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "potion",
                "cellulosesz.item.potion",
                CommandSourceKind.PLAYER_ONLY
        );

        var effect = Commands.argument(
                        "effect",
                        ResourceArgument.resource(
                                context.buildContext(),
                                Registries.MOB_EFFECT
                        )
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        PotionItemRequest.apply(
                                ResourceArgument.getMobEffect(command, "effect")
                                        .key()
                                        .identifier()
                                        .toString(),
                                180,
                                0
                        )
                ))
                .then(Commands.argument(
                                        "duration",
                                        IntegerArgumentType.integer(1, 86_400)
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        PotionItemRequest.apply(
                                                ResourceArgument.getMobEffect(command, "effect")
                                                        .key()
                                                        .identifier()
                                                        .toString(),
                                                IntegerArgumentType.getInteger(
                                                        command,
                                                        "duration"
                                                ),
                                                0
                                        )
                                ))
                                .then(Commands.argument(
                                                        "amplifier",
                                                        IntegerArgumentType.integer(0, 255)
                                                )
                                                .executes(command -> execute(
                                                        context,
                                                        command,
                                                        descriptor,
                                                        PotionItemRequest.apply(
                                                                ResourceArgument.getMobEffect(command, "effect")
                                                                        .key()
                                                                        .identifier()
                                                                        .toString(),
                                                                IntegerArgumentType.getInteger(
                                                                        command,
                                                                        "duration"
                                                                ),
                                                                IntegerArgumentType.getInteger(
                                                                        command,
                                                                        "amplifier"
                                                                )
                                                        )
                                                ))
                                )
                );

        var root = Commands.literal("potion")
                .then(Commands.literal("clear")
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                PotionItemRequest.clear()
                        ))
                )
                .then(effect);

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.potion",
                "/potion <clear|effect [durationSeconds] [amplifier]>",
                root
        );
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            PotionItemRequest request
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "potion",
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    return player
                            .<PlatformResult<?>>map(value -> service.potion(value, request))
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

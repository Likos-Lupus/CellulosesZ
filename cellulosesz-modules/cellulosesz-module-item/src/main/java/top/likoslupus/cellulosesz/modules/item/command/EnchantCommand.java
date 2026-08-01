package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.item.application.ItemCommandService;
import top.likoslupus.cellulosesz.modules.item.command.argument.EnchantmentArgument;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class EnchantCommand implements CommandContributor {

    private final ItemCommandService service;

    public EnchantCommand(ItemCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "enchant",
                "cellulosesz.item.enchant",
                CommandSourceKind.PLAYER_ONLY
        );

        var enchantment = Commands.argument(
                        "enchantment",
                        EnchantmentArgument.enchantment(
                                service.platform()::enchantmentIds
                        )
                )
                .suggests((_, builder) ->
                        CommandSuggestionSupport.suggest(
                                service.platform()::enchantmentIds,
                                builder
                        )
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        1
                ))
                .then(Commands.argument(
                                        "level",
                                        IntegerArgumentType.integer(1, 255)
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        IntegerArgumentType.getInteger(
                                                command,
                                                "level"
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.enchant",
                "/enchant <enchantment> [level]",
                Commands.literal("enchant").then(enchantment)
        );
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            int level
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "enchant",
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    if (player.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_SOURCE,
                                "player-only"
                        );
                    }

                    var unsafe = level > 5
                            && policy.hasPermission(
                            "cellulosesz.item.enchant.unsafe"
                    );

                    return service.enchant(
                            player.orElseThrow(),
                            EnchantmentArgument.get(command, "enchantment"),
                            level,
                            unsafe
                    );
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}

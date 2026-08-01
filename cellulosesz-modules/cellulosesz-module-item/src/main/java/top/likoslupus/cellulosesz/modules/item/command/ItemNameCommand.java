package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.api.validation.Checks;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.item.application.ItemCommandService;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class ItemNameCommand implements CommandContributor {

    private static final int MAXIMUM_LENGTH = 128;

    private final ItemCommandService service;

    public ItemNameCommand(ItemCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "itemname",
                "cellulosesz.item.name",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("itemname")
                .then(Commands.literal("clear")
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                Optional.empty()
                        ))
                )
                .then(Commands.argument(
                                        "name",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        Optional.of(validText(
                                                StringArgumentType.getString(
                                                        command,
                                                        "name"
                                                )
                                        ))
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.itemname",
                "/itemname <clear|name...>",
                root
        );
    }

    private static RichText validText(String value) {
        value = Checks.requireNonBlank(value, "name");
        value = Checks.requireMaxLength(
                value,
                MAXIMUM_LENGTH,
                "name"
        );
        value = Checks.requireNoControlCharacters(value, "name");

        return RichText.plain(value);
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Optional<RichText> name
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "itemname",
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    return player
                            .<PlatformResult<?>>map(value -> service.setName(value, name))
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

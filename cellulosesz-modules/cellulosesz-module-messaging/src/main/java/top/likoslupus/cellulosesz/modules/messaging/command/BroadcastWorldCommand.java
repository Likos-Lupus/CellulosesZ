package top.likoslupus.cellulosesz.modules.messaging.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.messaging.application.ChatCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class BroadcastWorldCommand implements CommandContributor {

    private final ChatCommandService service;

    public BroadcastWorldCommand(ChatCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = MessagingCommandSupport.descriptor(
                "broadcastworld",
                "cellulosesz.messaging.broadcastworld",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("broadcastworld")
                .then(Commands.argument(
                                        "world",
                                        StringArgumentType.word()
                                )
                                .suggests((_, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                service::worldNames,
                                                builder
                                        )
                                )
                                .then(Commands.argument(
                                                        "message",
                                                        StringArgumentType.greedyString()
                                                )
                                                .executes(command ->
                                                        MessagingCommandSupport.async(
                                                                context,
                                                                command,
                                                                descriptor,
                                                                "broadcastworld body redacted",
                                                                _ -> service.broadcastWorld(
                                                                        StringArgumentType.getString(
                                                                                command,
                                                                                "world"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                command,
                                                                                "message"
                                                                        )
                                                                )
                                                        )
                                                )
                                )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.broadcastworld",
                "/broadcastworld <world> <message>",
                root
        );
    }

    @Override
    public String moduleId() {
        return MessagingCommandSupport.MODULE;
    }

}

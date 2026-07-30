package top.likoslupus.cellulosesz.modules.messaging.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.messaging.application.ChatCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class BroadcastCommand implements CommandContributor {

    private final ChatCommandService service;

    public BroadcastCommand(ChatCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = MessagingCommandSupport.descriptor(
                "broadcast",
                "cellulosesz.messaging.broadcast",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("broadcast")
                .then(Commands.argument(
                                        "message",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command -> MessagingCommandSupport.async(
                                        context,
                                        command,
                                        descriptor,
                                        "broadcast body redacted",
                                        _ -> service.broadcast(
                                                StringArgumentType.getString(
                                                        command,
                                                        "message"
                                                )
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.broadcast",
                "/broadcast <message>",
                root
        );
    }

    @Override
    public String moduleId() {
        return MessagingCommandSupport.MODULE;
    }

}

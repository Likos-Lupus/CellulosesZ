package top.likoslupus.cellulosesz.modules.messaging.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.messaging.application.ChatCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class HelpOpCommand implements CommandContributor {

    private final ChatCommandService service;
    private final PlayerDirectory players;

    public HelpOpCommand(
            ChatCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = MessagingCommandSupport.descriptor(
                "helpop",
                "cellulosesz.messaging.helpop",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("helpop")
                .then(Commands.argument(
                                        "message",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command -> MessagingCommandSupport.async(
                                        context,
                                        command,
                                        descriptor,
                                        "helpop body redacted",
                                        policy -> service.helpOp(
                                                MessagingCommandSupport.player(
                                                        policy,
                                                        players
                                                ),
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
                "commands.description.helpop",
                "/helpop <message>",
                root
        );
    }

    @Override
    public String moduleId() {
        return MessagingCommandSupport.MODULE;
    }

}

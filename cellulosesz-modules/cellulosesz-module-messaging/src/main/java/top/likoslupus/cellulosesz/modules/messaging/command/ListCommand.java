package top.likoslupus.cellulosesz.modules.messaging.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.messaging.application.ChatCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class ListCommand implements CommandContributor {

    private final ChatCommandService service;
    private final PlayerDirectory players;

    public ListCommand(
            ChatCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = MessagingCommandSupport.descriptor(
                "list",
                "cellulosesz.messaging.list",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("list")
                .executes(command -> MessagingCommandSupport.async(
                        context,
                        command,
                        descriptor,
                        "list",
                        policy -> service.list(
                                MessagingCommandSupport.player(
                                        policy,
                                        players
                                )
                        )
                ));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.list",
                "/list",
                root
        );
    }

    @Override
    public String moduleId() {
        return MessagingCommandSupport.MODULE;
    }

}

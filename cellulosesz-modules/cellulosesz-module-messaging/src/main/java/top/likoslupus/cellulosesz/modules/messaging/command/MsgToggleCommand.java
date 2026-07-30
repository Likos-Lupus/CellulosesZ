package top.likoslupus.cellulosesz.modules.messaging.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.messaging.application.PrivateMessageCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class MsgToggleCommand implements CommandContributor {

    private final PrivateMessageCommandService service;
    private final PlayerDirectory players;

    public MsgToggleCommand(
            PrivateMessageCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = MessagingCommandSupport.descriptor(
                "msgtoggle",
                "cellulosesz.messaging.toggle",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("msgtoggle")
                .executes(command ->
                        MessagingCommandSupport.requirePlayer(
                                context,
                                command,
                                descriptor,
                                "msgtoggle",
                                players,
                                player -> service.toggleMessages(
                                        player.uuid()
                                )
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.msgtoggle",
                "/msgtoggle",
                root
        );
    }

    @Override
    public String moduleId() {
        return MessagingCommandSupport.MODULE;
    }

}

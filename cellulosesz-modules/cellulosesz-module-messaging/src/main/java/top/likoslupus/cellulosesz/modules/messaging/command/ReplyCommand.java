package top.likoslupus.cellulosesz.modules.messaging.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.messaging.application.PrivateMessageCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class ReplyCommand implements CommandContributor {

    private final PrivateMessageCommandService service;
    private final PlayerDirectory players;

    public ReplyCommand(
            PrivateMessageCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = MessagingCommandSupport.descriptor(
                "r",
                "cellulosesz.messaging.reply",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("r")
                .then(Commands.argument(
                                        "message",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command ->
                                        MessagingCommandSupport.requirePlayer(
                                                context,
                                                command,
                                                descriptor,
                                                "reply body redacted",
                                                players,
                                                player -> service.reply(
                                                        player,
                                                        StringArgumentType.getString(
                                                                command,
                                                                "message"
                                                        )
                                                )
                                        )
                                )
                );

        var node = context.registerDirect(
                moduleId(),
                descriptor,
                List.of("reply"),
                "commands.description.reply",
                "/r <message>",
                root
        );

        context.registerAlias(
                moduleId(),
                descriptor,
                "reply",
                node
        );
    }

    @Override
    public String moduleId() {
        return MessagingCommandSupport.MODULE;
    }

}

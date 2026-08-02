package top.likoslupus.cellulosesz.modules.messaging.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.messaging.application.PrivateMessageCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class MsgCommand implements CommandContributor {

    private final PrivateMessageCommandService service;
    private final PlayerDirectory players;

    public MsgCommand(
            PrivateMessageCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = MessagingCommandSupport.descriptor(
                "msg",
                "cellulosesz.messaging.msg",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("msg")
                .then(Commands.argument(
                                        "player",
                                        EntityArgument.player()
                                )
                                .suggests((command, builder) ->
                                        MessagingCommandSupport.playerFromSource(
                                                        command.getSource(),
                                                        players
                                                )
                                                .map(viewer ->
                                                        CommandSuggestionSupport.suggest(
                                                                () -> service.onlineNames(
                                                                        viewer
                                                                ),
                                                                builder
                                                        )
                                                )
                                                .orElseGet(builder::buildFuture)
                                )
                                .then(Commands.argument(
                                                        "message",
                                                        StringArgumentType.greedyString()
                                                )
                                                .executes(command ->
                                                        MessagingCommandSupport.requirePlayer(
                                                                context,
                                                                command,
                                                                descriptor,
                                                                "private message body redacted",
                                                                players,
                                                                sender -> service.send(
                                                                        sender,
                                                                        EntityArgument.getPlayer(command, "player")
                                                                                .getGameProfile()
                                                                                .name(),
                                                                        StringArgumentType.getString(
                                                                                command,
                                                                                "message"
                                                                        )
                                                                )
                                                        )
                                                )
                                )
                );

        var node = context.registerDirect(
                moduleId(),
                descriptor,
                List.of("tell", "w"),
                "commands.description.msg",
                "/msg <player> <message>",
                root
        );

        context.registerAlias(
                moduleId(),
                descriptor,
                "tell",
                node
        );

        context.registerAlias(
                moduleId(),
                descriptor,
                "w",
                node
        );
    }

    @Override
    public String moduleId() {
        return MessagingCommandSupport.MODULE;
    }

}

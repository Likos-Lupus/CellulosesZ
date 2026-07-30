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

public final class MeCommand implements CommandContributor {

    private final ChatCommandService service;
    private final PlayerDirectory players;

    public MeCommand(
            ChatCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = MessagingCommandSupport.descriptor(
                "me",
                "cellulosesz.messaging.me",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("me")
                .then(Commands.argument(
                                        "action",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command ->
                                        MessagingCommandSupport.requirePlayer(
                                                context,
                                                command,
                                                descriptor,
                                                "me body redacted",
                                                players,
                                                player -> service.me(
                                                        player,
                                                        StringArgumentType.getString(
                                                                command,
                                                                "action"
                                                        )
                                                )
                                        )
                                )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.me",
                "/me <action>",
                root
        );
    }

    @Override
    public String moduleId() {
        return MessagingCommandSupport.MODULE;
    }

}

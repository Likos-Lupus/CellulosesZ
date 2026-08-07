package top.likoslupus.cellulosesz.modules.teleport.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.ToggleModes;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportPreferenceCommandService;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class TpAutoCommand implements CommandContributor {

    private final TeleportPreferenceCommandService service;
    private final PlayerDirectory players;

    public TpAutoCommand(
            TeleportPreferenceCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "tpauto",
                "cellulosesz.teleport.tpauto",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("tpauto")
                .executes(command -> TeleportCommandResults.player(
                        context,
                        command,
                        descriptor,
                        "tpauto toggle",
                        players,
                        player -> service.autoAccept(player, Optional.empty())
                ))
                .then(Commands.argument("state", StringArgumentType.word())
                        .suggests((_, builder) -> CommandSuggestionSupport.suggest(
                                ToggleModes::suggestions,
                                builder
                        ))
                        .executes(command -> setState(
                                context,
                                command,
                                descriptor
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tpauto",
                "/tpauto [on|off]",
                root
        );
    }

    private int setState(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) throws CommandSyntaxException {
        var enabled = ToggleModes.parse(
                StringArgumentType.getString(command, "state")
        ).enabled();
        return TeleportCommandResults.player(
                context,
                command,
                descriptor,
                "tpauto set",
                players,
                player -> service.autoAccept(player, Optional.of(enabled))
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}

package top.likoslupus.cellulosesz.modules.admin.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.admin.application.JailCommandService;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminResult;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminStatus;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class SetJailCommand implements CommandContributor {

    private final JailCommandService service;
    private final PlayerDirectory players;

    public SetJailCommand(
            JailCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "setjail",
                "cellulosesz.admin.jail.set",
                CommandSourceKind.PLAYER_ONLY
        );

        var argument = Commands.argument(
                        "name",
                        StringArgumentType.word()
                )
                .executes(command -> AdminCommandResults.async(
                        context,
                        command,
                        descriptor,
                        "setjail",
                        policy -> AdminCommandResults.current(
                                        policy,
                                        players
                                )
                                .map(player -> service.set(
                                        player,
                                        StringArgumentType.getString(
                                                command,
                                                "name"
                                        )
                                ))
                                .orElseGet(() ->
                                        CompletableFuture.completedFuture(
                                                AdminResult.failure(
                                                        AdminStatus.INVALID_INPUT,
                                                        "common.player-only"
                                                )
                                        )
                                )
                ));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.setjail",
                "/setjail <name>",
                Commands.literal("setjail").then(argument)
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}

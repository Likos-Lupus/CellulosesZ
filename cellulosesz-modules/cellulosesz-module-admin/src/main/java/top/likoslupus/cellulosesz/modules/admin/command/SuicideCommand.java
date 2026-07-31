package top.likoslupus.cellulosesz.modules.admin.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.AdminStatus;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.admin.application.PlayerControlCommandService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class SuicideCommand implements CommandContributor {

    private final PlayerControlCommandService service;
    private final PlayerDirectory players;

    public SuicideCommand(
            PlayerControlCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "suicide",
                "cellulosesz.command.suicide",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("suicide")
                .executes(command -> AdminCommandResults.async(
                        context,
                        command,
                        descriptor,
                        "suicide",
                        policy -> AdminCommandResults.current(
                                        policy,
                                        players
                                )
                                .map(service::suicide)
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
                "commands.description.suicide",
                "/suicide",
                root
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}

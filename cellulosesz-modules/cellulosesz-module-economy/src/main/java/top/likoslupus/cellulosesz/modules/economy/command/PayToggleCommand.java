package top.likoslupus.cellulosesz.modules.economy.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.economy.application.PaymentCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class PayToggleCommand implements CommandContributor {

    private final PaymentCommandService service;
    private final PlayerDirectory players;

    public PayToggleCommand(
            PaymentCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = EconomyCommandSupport.descriptor(
                "paytoggle",
                "cellulosesz.economy.paytoggle",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("paytoggle")
                .executes(command ->
                        EconomyCommandSupport.requirePlayer(
                                context,
                                command,
                                descriptor,
                                "pay preference toggle",
                                players,
                                player -> service.togglePayments(
                                        player.uuid()
                                )
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.paytoggle",
                "/paytoggle",
                root
        );
    }

    @Override
    public String moduleId() {
        return EconomyCommandSupport.MODULE;
    }

}

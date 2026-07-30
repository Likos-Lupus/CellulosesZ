package top.likoslupus.cellulosesz.modules.economy.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.economy.application.PaymentCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class PayConfirmToggleCommand implements CommandContributor {

    private final PaymentCommandService service;
    private final PlayerDirectory players;

    public PayConfirmToggleCommand(
            PaymentCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = EconomyCommandSupport.descriptor(
                "payconfirmtoggle",
                "cellulosesz.economy.payconfirmtoggle",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("payconfirmtoggle")
                .executes(command ->
                        EconomyCommandSupport.requirePlayer(
                                context,
                                command,
                                descriptor,
                                "pay confirmation preference toggle",
                                players,
                                player -> service.toggleConfirmation(
                                        player.uuid()
                                )
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.payconfirmtoggle",
                "/payconfirmtoggle",
                root
        );
    }

    @Override
    public String moduleId() {
        return EconomyCommandSupport.MODULE;
    }

}

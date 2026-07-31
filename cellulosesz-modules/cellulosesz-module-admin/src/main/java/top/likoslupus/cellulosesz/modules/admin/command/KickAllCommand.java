package top.likoslupus.cellulosesz.modules.admin.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.admin.application.ModerationCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class KickAllCommand implements CommandContributor {

    private final ModerationCommandService service;
    private final PlayerDirectory players;

    public KickAllCommand(
            ModerationCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "kickall",
                "cellulosesz.admin.kickall",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("kickall")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        ""
                ))
                .then(Commands.argument(
                                        "reason",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        StringArgumentType.getString(
                                                command,
                                                "reason"
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.kickall",
                "/kickall [reason]",
                root
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String reason
    ) {
        return AdminCommandResults.async(
                registration,
                command,
                descriptor,
                "kickall reason-present=" + !reason.isBlank(),
                policy -> service.kickAll(
                        AdminCommandResults.actor(policy, players),
                        reason
                )
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}

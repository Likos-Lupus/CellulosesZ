package top.likoslupus.cellulosesz.modules.playerstate.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.playerstate.ExperienceAction;
import top.likoslupus.cellulosesz.api.playerstate.ExperienceRequest;
import top.likoslupus.cellulosesz.api.playerstate.ExperienceUnit;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerAbilityCommandService;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandResult;
import top.likoslupus.cellulosesz.modules.playerstate.command.argument.ExperienceAmountArgument;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class ExpCommand implements CommandContributor {

    private final PlayerAbilityCommandService service;
    private final PlayerDirectory players;

    public ExpCommand(
            PlayerAbilityCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "exp",
                "cellulosesz.command.exp",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("exp")
                .then(Commands.literal("show")
                        .executes(command -> selfShow(
                                context,
                                command,
                                descriptor
                        ))
                        .then(playerArgument(
                                        context,
                                        "cellulosesz.command.exp.others"
                                )
                                        .executes(command -> otherShow(
                                                context,
                                                command,
                                                descriptor
                                        ))
                        )
                )
                .then(mutation(
                        context,
                        descriptor,
                        "reset",
                        ExperienceAction.RESET,
                        "cellulosesz.command.exp.reset"
                ))
                .then(mutation(
                        context,
                        descriptor,
                        "set",
                        ExperienceAction.SET,
                        "cellulosesz.command.exp.set"
                ))
                .then(mutation(
                        context,
                        descriptor,
                        "give",
                        ExperienceAction.GIVE,
                        "cellulosesz.command.exp.give"
                ))
                .then(mutation(
                        context,
                        descriptor,
                        "take",
                        ExperienceAction.TAKE,
                        "cellulosesz.command.exp.take"
                ));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.exp",
                "/exp <show|reset|set|give|take> ...",
                root
        );
    }

    private LiteralArgumentBuilder<CommandSourceStack> mutation(
            CommandRegistrationContext context,
            CommandDescriptor descriptor,
            String literal,
            ExperienceAction action,
            String permission
    ) {
        var branch = Commands.literal(literal)
                .requires(source ->
                        context.permissions().has(
                                source,
                                permission
                        )
                );

        if (action == ExperienceAction.RESET) {
            return branch
                    .executes(command -> selfMutation(
                            context,
                            command,
                            descriptor,
                            new ExperienceRequest(
                                    action,
                                    ExperienceUnit.POINTS,
                                    0
                            )
                    ))
                    .then(playerArgument(
                                    context,
                                    permission + ".others"
                            )
                                    .executes(command -> otherMutation(
                                            context,
                                            command,
                                            descriptor,
                                            new ExperienceRequest(
                                                    action,
                                                    ExperienceUnit.POINTS,
                                                    0
                                            )
                                    ))
                    );
        }

        return branch
                .then(Commands.argument(
                                        "amount",
                                        ExperienceAmountArgument.amount()
                                )
                                .executes(command -> selfMutation(
                                        context,
                                        command,
                                        descriptor,
                                        request(command, action)
                                ))
                )
                .then(playerArgument(
                                context,
                                permission + ".others"
                        )
                                .then(Commands.argument(
                                                        "amount",
                                                        ExperienceAmountArgument.amount()
                                                )
                                                .executes(command -> otherMutation(
                                                        context,
                                                        command,
                                                        descriptor,
                                                        request(command, action)
                                                ))
                                )
                );
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> playerArgument(
            CommandRegistrationContext context,
            String permission
    ) {
        return Commands.argument(
                        "player",
                        EntityArgument.player()
                )
                .requires(source ->
                        context.permissions().has(
                                source,
                                permission
                        )
                );
    }

    private static ExperienceRequest request(
            CommandContext<CommandSourceStack> command,
            ExperienceAction action
    ) {
        var amount = ExperienceAmountArgument.get(
                command,
                "amount"
        );

        return new ExperienceRequest(
                action,
                amount.unit(),
                amount.amount()
        );
    }

    private int selfShow(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return PlayerStateCommandSupport.async(
                context,
                command,
                descriptor,
                "exp show self",
                policy -> PlayerStateCommandSupport.currentPlayer(
                                policy,
                                players
                        )
                        .map(service::experience)
                        .orElseGet(() ->
                                CompletableFuture.completedFuture(
                                        PlayerStateCommandResult.failure(
                                                "commands.playerstate.exp.console-target-required"
                                        )
                                )
                        )
        );
    }

    private int otherShow(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return PlayerStateCommandSupport.async(
                context,
                command,
                descriptor,
                "exp show other",
                _ -> service.experience(MinecraftPlayers.wrap(
                        EntityArgument.getPlayer(command, "player")
                ))
        );
    }

    private int selfMutation(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            ExperienceRequest request
    ) {
        return PlayerStateCommandSupport.async(
                context,
                command,
                descriptor,
                "exp mutation self",
                policy -> PlayerStateCommandSupport.currentPlayer(
                                policy,
                                players
                        )
                        .map(player ->
                                service.mutateExperience(
                                        player,
                                        request
                                )
                        )
                        .orElseGet(() ->
                                CompletableFuture.completedFuture(
                                        PlayerStateCommandResult.failure(
                                                "commands.playerstate.exp.console-target-required"
                                        )
                                )
                        )
        );
    }

    private int otherMutation(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            ExperienceRequest request
    ) {
        return PlayerStateCommandSupport.async(
                context,
                command,
                descriptor,
                "exp mutation other",
                _ -> service.mutateExperience(
                        MinecraftPlayers.wrap(EntityArgument.getPlayer(command, "player")),
                        request
                )
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}

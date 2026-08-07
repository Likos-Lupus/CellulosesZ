package top.likoslupus.cellulosesz.modules.playerstate.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.playerstate.PersonalTimeSetting;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerAbilityCommandService;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandResult;
import top.likoslupus.cellulosesz.modules.playerstate.command.argument.PersonalTimes;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class PTimeCommand implements CommandContributor {

    private final PlayerAbilityCommandService service;
    private final PlayerDirectory players;

    public PTimeCommand(
            PlayerAbilityCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "ptime",
                "cellulosesz.playerstate.ptime",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("ptime")
                .then(Commands.argument("time", StringArgumentType.word())
                        .suggests((_, builder) -> CommandSuggestionSupport.suggest(
                                PersonalTimes.suggestions(),
                                builder
                        ))
                        .executes(command -> self(
                                context,
                                command,
                                descriptor,
                                setting(command)
                        ))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> context.hasPermission(
                                        source,
                                        "cellulosesz.playerstate.ptime.others"
                                ))
                                .executes(command -> other(
                                        context,
                                        command,
                                        descriptor,
                                        setting(command)
                                ))
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.ptime",
                "/ptime <day|night|dawn|noon|midnight|reset|ticks> [player]",
                root
        );
    }

    private static PersonalTimeSetting setting(
            CommandContext<CommandSourceStack> command
    ) throws CommandSyntaxException {
        return PersonalTimes.parse(
                StringArgumentType.getString(command, "time")
        );
    }

    private int self(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            PersonalTimeSetting setting
    ) {
        return PlayerStateCommandSupport.async(
                context,
                command,
                descriptor,
                "ptime self",
                policy -> PlayerStateCommandSupport.currentPlayer(
                                policy,
                                players
                        )
                        .map(player ->
                                service.personalTime(player, setting)
                        )
                        .orElseGet(() -> CompletableFuture.completedFuture(
                                PlayerStateCommandResult.failure("common.player-only")
                        ))
        );
    }

    private int other(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            PersonalTimeSetting setting
    ) throws CommandSyntaxException {
        var target = MinecraftPlayers.wrap(
                EntityArgument.getPlayer(command, "player")
        );
        return PlayerStateCommandSupport.async(
                context,
                command,
                descriptor,
                "ptime other",
                _ -> service.personalTime(target, setting)
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}

package top.likoslupus.cellulosesz.modules.playerstate.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.playerstate.PersonalWeatherSetting;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerAbilityCommandService;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandResult;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class PWeatherCommand implements CommandContributor {

    private final PlayerAbilityCommandService service;
    private final PlayerDirectory players;

    public PWeatherCommand(
            PlayerAbilityCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "pweather",
                "cellulosesz.playerstate.pweather",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("pweather");

        Arrays.stream(PersonalWeatherSetting.values())
                .map(setting ->
                        branch(
                                context,
                                descriptor,
                                setting
                        )
                )
                .forEach(root::then);

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.pweather",
                "/pweather <clear|rain|thunder|reset> [player]",
                root
        );
    }

    private LiteralArgumentBuilder<CommandSourceStack> branch(
            CommandRegistrationContext context,
            CommandDescriptor descriptor,
            PersonalWeatherSetting setting
    ) {
        return Commands.literal(
                        setting.name().toLowerCase(Locale.ROOT)
                )
                .executes(command -> self(
                        context,
                        command,
                        descriptor,
                        setting
                ))
                .then(Commands.argument(
                                        "player",
                                        EntityArgument.player()
                                )
                                .requires(source -> context.permissions().has(
                                        source,
                                        "cellulosesz.playerstate.pweather.others"
                                ))
                                .executes(command -> other(
                                        context,
                                        command,
                                        descriptor,
                                        setting
                                ))
                );
    }

    private int self(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            PersonalWeatherSetting setting
    ) {
        return PlayerStateCommandSupport.async(
                context,
                command,
                descriptor,
                "pweather self",
                policy -> PlayerStateCommandSupport.currentPlayer(
                                policy,
                                players
                        )
                        .map(player ->
                                service.personalWeather(
                                        player,
                                        setting
                                )
                        )
                        .orElseGet(() ->
                                CompletableFuture.completedFuture(
                                        PlayerStateCommandResult.failure(
                                                "common.player-only"
                                        )
                                )
                        )
        );
    }

    private int other(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            PersonalWeatherSetting setting
    ) throws CommandSyntaxException {
        var target = MinecraftPlayers.wrap(
                EntityArgument.getPlayer(command, "player")
        );

        return PlayerStateCommandSupport.async(
                context,
                command,
                descriptor,
                "pweather other",
                _ -> service.personalWeather(
                        target,
                        setting
                )
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}

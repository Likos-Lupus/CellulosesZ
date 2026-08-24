package top.likoslupus.cellulosesz.modules.messaging.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.messaging.application.PrivateMessageCommandService;
import top.likoslupus.cellulosesz.modules.messaging.domain.MessageResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class ReplyToggleCommand implements CommandContributor {

    private final PrivateMessageCommandService service;
    private final PlayerDirectory players;

    public ReplyToggleCommand(
            PrivateMessageCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = MessagingCommandSupport.descriptor(
                "rtoggle",
                "cellulosesz.messaging.rtoggle",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("rtoggle")
                .executes(command -> self(
                        context,
                        command,
                        descriptor,
                        Optional.empty()
                ))
                .then(Commands.literal("on")
                        .executes(command -> self(
                                context,
                                command,
                                descriptor,
                                Optional.of(true)
                        ))
                )
                .then(Commands.literal("off")
                        .executes(command -> self(
                                context,
                                command,
                                descriptor,
                                Optional.of(false)
                        ))
                )
                .then(Commands.argument("player", StringArgumentType.word())
                        .requires(source -> context.hasPermission(
                                source,
                                "cellulosesz.messaging.rtoggle.others"
                        ))
                        .suggests((_, builder) ->
                                CommandSuggestionSupport.suggest(
                                        service::knownNames,
                                        builder
                                )
                        )
                        .executes(command -> other(
                                context,
                                command,
                                descriptor,
                                Optional.empty()
                        ))
                        .then(Commands.literal("on")
                                .executes(command -> other(
                                        context,
                                        command,
                                        descriptor,
                                        Optional.of(true)
                                ))
                        )
                        .then(Commands.literal("off")
                                .executes(command -> other(
                                        context,
                                        command,
                                        descriptor,
                                        Optional.of(false)
                                ))
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.rtoggle",
                "/rtoggle [on|off|player [on|off]]",
                root
        );
    }

    private int self(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Optional<Boolean> requested
    ) {
        return MessagingCommandSupport.requirePlayer(
                context,
                command,
                descriptor,
                "rtoggle",
                players,
                player -> service.replyPreference(
                        player.uuid(),
                        player.name(),
                        requested
                )
        );
    }

    private int other(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Optional<Boolean> requested
    ) {
        return MessagingCommandSupport.async(
                context,
                command,
                descriptor,
                "rtoggle other",
                policy -> {
                    if (!policy.hasPermission(
                            "cellulosesz.messaging.rtoggle.others"
                    )) {
                        return CompletableFuture.completedFuture(
                                MessageResult.failure(
                                        "common.no-permission"
                                )
                        );
                    }

                    var token = StringArgumentType.getString(command, "player");

                    var viewer = MessagingCommandSupport.player(
                            policy,
                            players
                    );

                    if (viewer.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                MessageResult.failure(
                                        "common.player-only"
                                )
                        );
                    }

                    return service.knownTarget(
                                    token,
                                    viewer.orElseThrow()
                            )
                            .thenCompose(target ->
                                    service.replyPreference(
                                            target.uuid(),
                                            target.name(),
                                            requested
                                    )
                            );
                }
        );
    }

    @Override
    public String moduleId() {
        return MessagingCommandSupport.MODULE;
    }

}

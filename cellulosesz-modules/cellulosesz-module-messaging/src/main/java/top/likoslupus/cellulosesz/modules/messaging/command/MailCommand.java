package top.likoslupus.cellulosesz.modules.messaging.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandExecutions;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;
import top.likoslupus.cellulosesz.modules.messaging.application.MailCommandService;
import top.likoslupus.cellulosesz.modules.messaging.command.argument.MailDurations;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class MailCommand implements CommandContributor {

    private final MailCommandService service;
    private final PlayerDirectory players;
    private final Supplier<List<String>> knownNames;

    public MailCommand(
            MailCommandService service,
            PlayerDirectory players,
            Supplier<List<String>> knownNames
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
        this.knownNames = requireNonNull(knownNames, "knownNames");
    }

    private static void respond(
            MinecraftCommandPolicyContext policy,
            MailCommandService.Result result
    ) {
        policy.respondAll(
                result.success(),
                result.messages()
        );
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> playerArgument() {
        return Commands.argument(
                        "player",
                        StringArgumentType.word()
                )
                .suggests((_, builder) ->
                        CommandSuggestionSupport.suggest(
                                knownNames,
                                builder
                        )
                );
    }

    private int read(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            int page
    ) {
        return self(
                context,
                command,
                descriptor,
                "mail read",
                uuid -> service.read(uuid, page)
        );
    }

    private int send(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Optional<Duration> duration
    ) {
        return CommandExecutions.async(
                context,
                command,
                descriptor,
                "mail send body redacted",
                policy -> service.send(
                        MessagingCommandSupport.player(
                                policy,
                                players
                        ),
                        StringArgumentType.getString(command, "player"),
                        duration,
                        StringArgumentType.getString(
                                command,
                                "message"
                        )
                ),
                (policy, result) -> {
                    respond(policy, result);
                    return CommandOutcome.fromSuccess(result.success());
                }
        );
    }

    private int delete(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        var id = UuidArgument.getUuid(command, "id");
        return self(
                context,
                command,
                descriptor,
                "mail delete",
                uuid -> service.delete(uuid, id)
        );
    }

    private int self(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String audit,
            Function<
                    UUID,
                    CompletableFuture<MailCommandService.Result>
                    > operation
    ) {
        return CommandExecutions.async(
                context,
                command,
                descriptor,
                audit,
                policy -> policy.playerUuid()
                        .map(operation)
                        .orElseGet(() ->
                                CompletableFuture.completedFuture(
                                        MailCommandService.Result.failure(
                                                LocalizedMessage.of(
                                                        "common.player-only"
                                                )
                                        )
                                )
                        ),
                (policy, result) -> {
                    respond(policy, result);
                    return CommandOutcome.fromSuccess(result.success());
                }
        );
    }

    @Override
    public String moduleId() {
        return MessagingCommandSupport.MODULE;
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = MessagingCommandSupport.descriptor(
                "mail",
                "cellulosesz.messaging.mail",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("mail")
                .executes(command -> read(
                        context,
                        command,
                        descriptor,
                        1
                ))
                .then(Commands.literal("read")
                        .executes(command -> read(
                                context,
                                command,
                                descriptor,
                                1
                        ))
                        .then(Commands.argument(
                                                "page",
                                                IntegerArgumentType.integer(1)
                                        )
                                        .executes(command -> read(
                                                context,
                                                command,
                                                descriptor,
                                                IntegerArgumentType.getInteger(
                                                        command,
                                                        "page"
                                                )
                                        ))
                        )
                )
                .then(Commands.literal("unread")
                        .executes(command -> self(
                                context,
                                command,
                                descriptor,
                                "mail unread",
                                service::unread
                        ))
                )
                .then(Commands.literal("delete")
                        .then(Commands.argument(
                                                "id",
                                                UuidArgument.uuid()
                                        )
                                        .executes(command -> delete(
                                                context,
                                                command,
                                                descriptor
                                        ))
                        )
                )
                .then(Commands.literal("clear")
                        .executes(command -> self(
                                context,
                                command,
                                descriptor,
                                "mail clear",
                                service::clear
                        ))
                )
                .then(Commands.literal("send")
                        .then(playerArgument()
                                .then(Commands.argument(
                                                        "message",
                                                        StringArgumentType.greedyString()
                                                )
                                                .executes(command -> send(
                                                        context,
                                                        command,
                                                        descriptor,
                                                        Optional.empty()
                                                ))
                                )
                        )
                )
                .then(Commands.literal("sendtemp")
                        .then(playerArgument()
                                .then(Commands.argument("duration", StringArgumentType.word())
                                        .then(Commands.argument(
                                                                "message",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(command -> send(
                                                                context,
                                                                command,
                                                                descriptor,
                                                                Optional.of(
                                                                        MailDurations.parse(
                                                                                StringArgumentType.getString(
                                                                                        command,
                                                                                        "duration"
                                                                                )
                                                                        )
                                                                )
                                                        ))
                                        )
                                )
                        )
                )
                .then(Commands.literal("sendall")
                        .requires(source -> context.hasPermission(
                                source,
                                "cellulosesz.messaging.mail.sendall"
                        ))
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(command -> CommandExecutions.async(
                                        context,
                                        command,
                                        descriptor,
                                        "mail sendall body redacted",
                                        policy -> {
                                            if (!policy.hasPermission(
                                                    "cellulosesz.messaging.mail.sendall"
                                            )) {
                                                return CompletableFuture
                                                        .completedFuture(
                                                                MailCommandService.Result.failure(
                                                                        LocalizedMessage.of(
                                                                                "common.no-permission"
                                                                        )
                                                                )
                                                        );
                                            }

                                            return service.sendAll(
                                                    MessagingCommandSupport.player(
                                                            policy,
                                                            players
                                                    ),
                                                    StringArgumentType.getString(
                                                            command,
                                                            "message"
                                                    )
                                            );
                                        },
                                        (policy, result) -> {
                                            respond(policy, result);
                                            return CommandOutcome.fromSuccess(result.success());
                                        }
                                ))
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.mail",
                "/mail [read [page]|unread|delete <id>|clear|send <player> <message>|sendtemp <player> <duration> <message>|sendall <message>]",
                root
        );
    }

}

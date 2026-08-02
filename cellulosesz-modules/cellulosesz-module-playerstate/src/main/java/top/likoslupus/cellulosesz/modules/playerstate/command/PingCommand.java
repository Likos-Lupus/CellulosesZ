package top.likoslupus.cellulosesz.modules.playerstate.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandExecutions;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;

import java.util.List;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requirePositive;

public final class PingCommand implements CommandContributor {

    private final int maximumLength;

    public PingCommand(int maximumLength) {
        this.maximumLength = requirePositive(maximumLength, "maximumLength");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "ping",
                "cellulosesz.command.ping",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("ping")
                .executes(command -> CommandExecutions.sync(
                        context,
                        command,
                        descriptor,
                        "ping",
                        policy -> {
                            policy.reply(
                                    LocalizedMessage.of(
                                            "commands.playerstate.ping.pong"
                                    )
                            );

                            return 1;
                        }
                ))
                .then(Commands.argument(
                                        "message",
                                        StringArgumentType.greedyString()
                                )
                                .executes(command -> CommandExecutions.sync(
                                        context,
                                        command,
                                        descriptor,
                                        "ping echo",
                                        policy -> {
                                            var message = StringArgumentType.getString(
                                                    command,
                                                    "message"
                                            ).strip();

                                            if (message.isBlank()
                                                    || message.length() > maximumLength
                                                    || message.codePoints().anyMatch(
                                                    Character::isISOControl
                                            )) {
                                                policy.error(
                                                        LocalizedMessage.of(
                                                                "commands.playerstate.ping.invalid-message"
                                                        )
                                                );

                                                return 0;
                                            }

                                            policy.reply(
                                                    LocalizedMessage.of(
                                                            "commands.playerstate.ping.echo",
                                                            MessageArguments.builder()
                                                                    .put("message", message)
                                                                    .build()
                                                    )
                                            );

                                            return 1;
                                        }
                                ))
                );

        var node = context.registerDirect(
                moduleId(),
                descriptor,
                List.of("pong"),
                "commands.description.ping",
                "/ping [message]",
                root
        );

        context.registerAlias(
                moduleId(),
                descriptor,
                "pong",
                node
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}

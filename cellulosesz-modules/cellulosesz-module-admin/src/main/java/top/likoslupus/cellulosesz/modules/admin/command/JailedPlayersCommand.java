package top.likoslupus.cellulosesz.modules.admin.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.admin.application.JailCommandService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class JailedPlayersCommand implements CommandContributor {

    private final JailCommandService service;

    public JailedPlayersCommand(JailCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "jailedplayers",
                "cellulosesz.admin.jail.list",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("jailedplayers")
                .executes(command -> list(
                        context,
                        command,
                        descriptor,
                        1
                ))
                .then(Commands.argument(
                                        "page",
                                        IntegerArgumentType.integer(1)
                                )
                                .executes(command -> list(
                                        context,
                                        command,
                                        descriptor,
                                        IntegerArgumentType.getInteger(
                                                command,
                                                "page"
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.jailedplayers",
                "/jailedplayers [page]",
                root
        );
    }

    private int list(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            int page
    ) {
        return AdminCommandResults.async(
                registration,
                command,
                descriptor,
                "jailedplayers page=" + page,
                _ -> CompletableFuture.completedFuture(
                        AdminResult.success(
                                "service.admin.jailed-list",
                                MessageArguments.builder()
                                        .add(page)
                                        .add(service.jailedPlayers().stream()
                                                .skip((long) (page - 1) * 10)
                                                .limit(10)
                                                .map(jailed ->
                                                        "%s:%s:%s".formatted(
                                                                jailed.name(),
                                                                jailed.jail(),
                                                                jailed.state()
                                                        )
                                                )
                                                .toList()
                                                .toString()
                                        )
                                        .build()
                        )
                )
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}

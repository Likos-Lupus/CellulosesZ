package top.likoslupus.cellulosesz.modules.admin.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.admin.application.JailCommandService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class JailsCommand implements CommandContributor {

    private final JailCommandService service;

    public JailsCommand(JailCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "jails",
                "cellulosesz.admin.jail.list",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("jails")
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
                "commands.description.jails",
                "/jails [page]",
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
                "jails page=" + page,
                _ -> CompletableFuture.completedFuture(
                        AdminResult.success(
                                "service.admin.jails-list",
                                Map.of(
                                        "page",
                                        page,
                                        "jails",
                                        service.jails()
                                                .stream()
                                                .skip((long) (page - 1) * 10)
                                                .limit(10)
                                                .map(jail ->
                                                        "%s@%s".formatted(
                                                                jail.name(),
                                                                jail.location().world
                                                        )
                                                )
                                                .toList()
                                                .toString()
                                )
                        )
                )
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}

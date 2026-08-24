package top.likoslupus.cellulosesz.modules.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandExecutions;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.core.command.catalog.CommandCatalog;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.core.runtime.RuntimeService;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class CellulosesZCommand implements CommandContributor {

    private static final String MODULE = "command";

    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            MODULE,
            "cellulosesz",
            "cellulosesz.command.root",
            CommandSourceKind.ANY
    );

    private final RuntimeService runtime;
    private final ServerThreadExecutor serverThread;

    public CellulosesZCommand(
            RuntimeService runtime,
            ServerThreadExecutor serverThread
    ) {
        this.runtime = requireNonNull(runtime, "runtime");
        this.serverThread = requireNonNull(
                serverThread,
                "serverThread"
        );
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var root = Commands.literal("cellulosesz")
                .executes(command -> CommandExecutions.sync(
                        context,
                        command,
                        DESCRIPTOR,
                        "root",
                        policy -> {
                            policy.reply(
                                    LocalizedMessage.of(
                                            "cellulosesz.version",
                                            MessageArguments.builder()
                                                    .add(runtime.version())
                                                    .build()
                                    )
                            );

                            policy.reply(
                                    LocalizedMessage.of(
                                            "commands.command.cellulosesz.usage"
                                    )
                            );

                            return 1;
                        }
                ))
                .then(Commands.literal("version")
                        .executes(command -> CommandExecutions.sync(
                                context,
                                command,
                                DESCRIPTOR,
                                "version",
                                policy -> {
                                    policy.reply(
                                            LocalizedMessage.of(
                                                    "cellulosesz.version",
                                                    MessageArguments.builder()
                                                            .add(runtime.version())
                                                            .build()
                                            )
                                    );

                                    return 1;
                                }
                        ))
                )
                .then(Commands.literal("reload")
                        .requires(source -> context.hasPermission(
                                source,
                                "cellulosesz.command.reload"
                        ))
                        .executes(command -> CommandExecutions.async(
                                context,
                                command,
                                DESCRIPTOR,
                                "reload",
                                policy -> {
                                    if (!policy.hasPermission(
                                            "cellulosesz.command.reload"
                                    )) {
                                        policy.error(
                                                LocalizedMessage.of("common.no-permission")
                                        );

                                        return CompletableFuture.completedFuture(
                                                false
                                        );
                                    }

                                    policy.reply(
                                            LocalizedMessage.of(
                                                    "cellulosesz.reload-started"
                                            )
                                    );

                                    return runtime.reload()
                                            .thenCompose(_ ->
                                                    serverThread.submit(
                                                            () -> true
                                                    )
                                            );
                                },
                                (policy, success) -> {
                                    if (success) {
                                        policy.reply(
                                                LocalizedMessage.of(
                                                        "cellulosesz.reloaded"
                                                )
                                        );
                                    }
                                    return CommandOutcome.fromSuccess(success);
                                }
                        ))
                )
                .then(Commands.literal("modules")
                        .requires(source -> context.hasPermission(
                                source,
                                "cellulosesz.command.modules"
                        ))
                        .executes(command -> CommandExecutions.sync(
                                context,
                                command,
                                DESCRIPTOR,
                                "modules",
                                policy -> {
                                    if (!policy.hasPermission(
                                            "cellulosesz.command.modules"
                                    )) {
                                        policy.error(
                                                LocalizedMessage.of(
                                                        "common.no-permission"
                                                )
                                        );

                                        return 0;
                                    }

                                    policy.reply(
                                            LocalizedMessage.of(
                                                    "cellulosesz.modules-header"
                                            )
                                    );

                                    runtime.modules()
                                            .stream()
                                            .sorted(Comparator.comparing(
                                                    info -> info.id()
                                                            .toLowerCase(
                                                                    Locale.ROOT
                                                            )
                                            ))
                                            .forEach(info ->
                                                    policy.reply(
                                                            LocalizedMessage.of(
                                                                    "cellulosesz.module-row",
                                                                    MessageArguments.builder()
                                                                            .add(info.id())
                                                                            .add(
                                                                                    info.enabled()
                                                                            )
                                                                            .add(
                                                                                    info.phase()
                                                                                            .name()
                                                                            )
                                                                            .build()
                                                            )
                                                    )
                                            );

                                    return 1;
                                }
                        ))
                )
                .then(Commands.literal("debug")
                        .requires(source -> context.hasPermission(
                                source,
                                "cellulosesz.command.debug"
                        ))
                        .executes(command -> CommandExecutions.sync(
                                context,
                                command,
                                DESCRIPTOR,
                                "debug",
                                policy -> {
                                    if (!policy.hasPermission(
                                            "cellulosesz.command.debug"
                                    )) {
                                        policy.error(
                                                LocalizedMessage.of(
                                                        "common.no-permission"
                                                )
                                        );

                                        return 0;
                                    }

                                    var catalog = context.services()
                                            .require(CommandCatalog.class);

                                    policy.reply(
                                            LocalizedMessage.of(
                                                    "commands.command.cellulosesz.debug",
                                                    MessageArguments.builder()
                                                            .add(runtime.version())
                                                            .add(
                                                                    runtime.modules().size()
                                                            )
                                                            .add(
                                                                    catalog.commands().size()
                                                            )
                                                            .build()
                                            )
                                    );

                                    return 1;
                                }
                        ))
                );

        var node = context.registerDirect(
                moduleId(),
                DESCRIPTOR,
                List.of("cellz", "cz"),
                "commands.description.cellulosesz",
                "/cellulosesz [version|reload|modules|debug]",
                root
        );

        context.registerAlias(
                moduleId(),
                DESCRIPTOR,
                "cellz",
                node
        );

        context.registerAlias(
                moduleId(),
                DESCRIPTOR,
                "cz",
                node
        );
    }

    @Override
    public String moduleId() {
        return MODULE;
    }

}

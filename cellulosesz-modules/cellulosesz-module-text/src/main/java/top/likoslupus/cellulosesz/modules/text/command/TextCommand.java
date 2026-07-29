package top.likoslupus.cellulosesz.modules.text.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.text.application.TextCommandService;

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class TextCommand implements CommandContributor {

    private static final String MODULE = "text";

    private final TextCommandService service;

    public TextCommand(TextCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        if (!context.moduleEnabled(MODULE)) {
            return;
        }

        var service = this.service;

        registerPaged(
                context,
                service,
                "info",
                "cellulosesz.text.info",
                "/info [page]",
                service::info
        );

        registerPaged(
                context,
                service,
                "motd",
                "cellulosesz.text.motd",
                "/motd [page]",
                service::motd
        );

        registerPaged(
                context,
                service,
                "rules",
                "cellulosesz.text.rules",
                "/rules [page]",
                service::rules
        );

        registerCustom(context, service);
    }

    private void registerPaged(
            CommandRegistrationContext context,
            TextCommandService service,
            String name,
            String permission,
            String usage,
            IntFunction<TextCommandService.PageResult> operation
    ) {
        var descriptor = descriptor(name, permission);

        var root = Commands.literal(name)
                .executes(command -> executePage(
                        context,
                        command,
                        descriptor,
                        () -> operation.apply(1)
                ))
                .then(Commands.argument(
                                        "page",
                                        IntegerArgumentType.integer(1)
                                )
                                .executes(command -> executePage(
                                        context,
                                        command,
                                        descriptor,
                                        () -> operation.apply(
                                                IntegerArgumentType.getInteger(
                                                        command,
                                                        "page"
                                                )
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "",
                usage,
                root
        );
    }

    private void registerCustom(
            CommandRegistrationContext context,
            TextCommandService service
    ) {
        var descriptor = descriptor(
                "customtext",
                "cellulosesz.text.customtext"
        );

        var name = Commands.argument("name", StringArgumentType.word())
                .suggests((_, builder) ->
                        CommandSuggestionSupport.suggest(
                                service::customNames,
                                builder
                        )
                )
                .executes(command -> executePage(
                        context,
                        command,
                        descriptor,
                        () -> service.custom(
                                StringArgumentType.getString(
                                        command,
                                        "name"
                                ),
                                1
                        )
                ))
                .then(Commands.argument(
                                        "page",
                                        IntegerArgumentType.integer(1)
                                )
                                .executes(command -> executePage(
                                        context,
                                        command,
                                        descriptor,
                                        () -> service.custom(
                                                StringArgumentType.getString(
                                                        command,
                                                        "name"
                                                ),
                                                IntegerArgumentType.getInteger(
                                                        command,
                                                        "page"
                                                )
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "",
                "/customtext <name> [page]",
                Commands.literal("customtext").then(name)
        );
    }

    private CommandDescriptor descriptor(
            String name,
            String permission
    ) {
        return new CommandDescriptor(
                MODULE,
                name,
                permission,
                CommandSourceKind.ANY
        );
    }

    private int executePage(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Supplier<TextCommandService.PageResult> operation
    ) {
        return registration.execute(
                command,
                descriptor,
                "page request",
                context -> {
                    var result = operation.get();

                    context.respondAll(
                            result.success(),
                            result.messages()
                    );

                    return result.success() ? 1 : 0;
                }
        );
    }

    @Override
    public String moduleId() {
        return MODULE;
    }

}

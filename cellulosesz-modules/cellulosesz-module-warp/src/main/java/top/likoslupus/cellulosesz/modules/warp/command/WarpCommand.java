package top.likoslupus.cellulosesz.modules.warp.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;
import top.likoslupus.cellulosesz.modules.warp.application.WarpCommandService;
import top.likoslupus.cellulosesz.modules.warp.command.argument.WarpSelectors;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class WarpCommand implements CommandContributor {

    private static final String MODULE = "warp";

    private final WarpCommandService service;

    public WarpCommand(WarpCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public String moduleId() {
        return MODULE;
    }

    @Override
    public void register(CommandRegistrationContext context) {
        if (!context.moduleEnabled(MODULE)) {
            return;
        }

        var service = this.service;

        registerWarp(context, service);
        registerSet(context, service);
        registerDelete(context, service);
        registerInfo(context, service);
    }

    private void registerWarp(
            CommandRegistrationContext context,
            WarpCommandService service
    ) {
        var descriptor = player(
                "warp",
                "cellulosesz.warp.use"
        );

        var root = Commands.literal("warp")
                .executes(command -> executeAsync(
                        context,
                        command,
                        descriptor,
                        policy -> service.list(
                                1,
                                policy::hasPermission
                        )
                ))
                .then(Commands.argument("nameOrPage", StringArgumentType.word())
                        .suggests((command, builder) -> CommandSuggestionSupport.suggest(
                                () -> service.usableNames(
                                        permission -> context.hasPermission(
                                                command.getSource(),
                                                permission
                                        )
                                ),
                                builder
                        ))
                        .executes(command -> executeWarpSelection(
                                context,
                                command,
                                descriptor,
                                service
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of("warps"),
                "",
                "/warp [name|page]",
                root
        );

        context.registerSemantic(
                moduleId(),
                descriptor,
                "warps",
                Commands.literal("warps")
                        .executes(command -> executeAsync(
                                context,
                                command,
                                descriptor,
                                policy -> service.list(
                                        1,
                                        policy::hasPermission
                                )
                        ))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(command -> executeAsync(
                                        context,
                                        command,
                                        descriptor,
                                        policy -> service.list(
                                                IntegerArgumentType.getInteger(command, "page"),
                                                policy::hasPermission
                                        )
                                ))
                        )
        );
    }

    private int executeWarpSelection(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            WarpCommandService service
    ) throws CommandSyntaxException {
        return switch (WarpSelectors.parse(
                StringArgumentType.getString(command, "nameOrPage")
        )) {
            case WarpSelectors.Selection.Page page -> executeAsync(
                    context,
                    command,
                    descriptor,
                    policy -> service.list(
                            page.value(),
                            policy::hasPermission
                    )
            );
            case WarpSelectors.Selection.Name name -> executeAsync(
                    context,
                    command,
                    descriptor,
                    policy -> service.teleport(
                            request(policy),
                            name.value(),
                            policy::hasPermission
                    )
            );
        };
    }

    private void registerSet(
            CommandRegistrationContext context,
            WarpCommandService service
    ) {
        var descriptor = player(
                "setwarp",
                "cellulosesz.warp.create"
        );

        var root = Commands.literal("setwarp")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(command -> executeAsync(
                                context,
                                command,
                                descriptor,
                                policy -> service.set(
                                        request(policy),
                                        StringArgumentType.getString(
                                                command,
                                                "name"
                                        ),
                                        policy::hasPermission
                                )
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "",
                "/setwarp <name>",
                root
        );
    }

    private void registerDelete(
            CommandRegistrationContext context,
            WarpCommandService service
    ) {
        var descriptor = any(
                "delwarp",
                "cellulosesz.warp.delete"
        );

        var root = Commands.literal("delwarp")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((_, builder) ->
                                CommandSuggestionSupport.suggest(
                                        service::cachedNames,
                                        builder
                                )
                        )
                        .executes(command -> executeAsync(
                                context,
                                command,
                                descriptor,
                                _ -> service.delete(
                                        StringArgumentType.getString(
                                                command,
                                                "name"
                                        )
                                )
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "",
                "/delwarp <name>",
                root
        );
    }

    private void registerInfo(
            CommandRegistrationContext context,
            WarpCommandService service
    ) {
        var descriptor = any(
                "warpinfo",
                "cellulosesz.warp.info"
        );

        var root = Commands.literal("warpinfo")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((_, builder) ->
                                CommandSuggestionSupport.suggest(
                                        service::cachedNames,
                                        builder
                                )
                        )
                        .executes(command -> executeAsync(
                                context,
                                command,
                                descriptor,
                                _ -> service.info(
                                        StringArgumentType.getString(
                                                command,
                                                "name"
                                        )
                                )
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "",
                "/warpinfo <name>",
                root
        );
    }

    private int executeAsync(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Function<
                    MinecraftCommandPolicyContext,
                    CompletableFuture<WarpCommandService.Result>
                    > operation
    ) {
        return registration.execute(
                command,
                descriptor,
                "warp request",
                policy -> {
                    if (descriptor.requiredSourceKind()
                            == CommandSourceKind.PLAYER_ONLY
                            && policy.playerUuid().isEmpty()
                    ) {
                        policy.error(
                                LocalizedMessage.of("common.player-only")
                        );

                        return CompletableFuture.completedFuture(
                                CommandOutcome.rejected(0)
                        );
                    }

                    return operation
                            .apply(policy)
                            .thenApply(result -> {
                                policy.respond(result.success(), result.message());
                                return CommandOutcome.fromStatus(result.status());
                            });
                }
        );
    }

    private WarpCommandService.Request request(
            MinecraftCommandPolicyContext context
    ) {
        return new WarpCommandService.Request(
                context.playerUuid().orElseThrow(),
                context.playerName().orElse(""),
                context.hasPermission(
                        "cellulosesz.warp.bypass-cooldown"
                ),
                context.hasPermission(
                        "cellulosesz.warp.bypass-warmup"
                )
        );
    }

    private CommandDescriptor player(
            String name,
            String permission
    ) {
        return new CommandDescriptor(
                MODULE,
                name,
                permission,
                CommandSourceKind.PLAYER_ONLY
        );
    }

    private CommandDescriptor any(
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

}

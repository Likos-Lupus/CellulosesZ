package top.likoslupus.cellulosesz.modules.home.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
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
import top.likoslupus.cellulosesz.modules.home.application.HomeCommandService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class HomeCommand implements CommandContributor {

    private static final String MODULE = "home";

    private final HomeCommandService service;

    public HomeCommand(HomeCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        if (!context.moduleEnabled(MODULE)) {
            return;
        }

        var service = this.service;

        registerHome(context, service);
        registerSet(context, service);
        registerDelete(context, service);
        registerRename(context, service);
    }

    private void registerHome(
            CommandRegistrationContext context,
            HomeCommandService service
    ) {
        var descriptor = descriptor(
                "home",
                "cellulosesz.home.use"
        );

        var root = Commands.literal("home")
                .executes(command -> executeAsync(
                        context,
                        command,
                        descriptor,
                        policy -> service.teleport(
                                request(policy),
                                "home"
                        )
                ))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((command, builder) ->
                                CommandSuggestionSupport.suggest(
                                        () -> context.player(command.getSource())
                                                .map(player -> service.cachedNames(
                                                        player.uuid()
                                                ))
                                                .orElseGet(Set::of),
                                        builder
                                )
                        )
                        .executes(command -> executeAsync(
                                context,
                                command,
                                descriptor,
                                policy -> service.teleport(
                                        request(policy),
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
                List.of("homes"),
                "",
                "/home [name] | /homes",
                root
        );

        context.registerSemantic(
                moduleId(),
                descriptor,
                "homes",
                Commands.literal("homes")
                        .executes(command -> executeAsync(
                                context,
                                command,
                                descriptor,
                                policy -> service.list(
                                        policy.playerUuid().orElseThrow()
                                )
                        ))
        );
    }

    private void registerSet(
            CommandRegistrationContext context,
            HomeCommandService service
    ) {
        var descriptor = descriptor(
                "sethome",
                "cellulosesz.home.set"
        );

        var root = Commands.literal("sethome")
                .executes(command -> executeAsync(
                        context,
                        command,
                        descriptor,
                        policy -> service.set(
                                request(policy),
                                "home",
                                policy.hasPermission(
                                        "cellulosesz.home.bypass-limit"
                                )
                        )
                ))
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
                                        policy.hasPermission(
                                                "cellulosesz.home.bypass-limit"
                                        )
                                )
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "",
                "/sethome [name]",
                root
        );
    }

    private void registerDelete(
            CommandRegistrationContext context,
            HomeCommandService service
    ) {
        var descriptor = descriptor(
                "delhome",
                "cellulosesz.home.delete"
        );

        var root = Commands.literal("delhome")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((command, builder) ->
                                CommandSuggestionSupport.suggest(
                                        () -> context.player(command.getSource())
                                                .map(player -> service.cachedNames(
                                                        player.uuid()
                                                ))
                                                .orElseGet(Set::of),
                                        builder
                                )
                        )
                        .executes(command -> executeAsync(
                                context,
                                command,
                                descriptor,
                                policy -> service.delete(
                                        policy.playerUuid().orElseThrow(),
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
                "/delhome <name>",
                root
        );
    }

    private void registerRename(
            CommandRegistrationContext context,
            HomeCommandService service
    ) {
        var descriptor = descriptor(
                "renamehome",
                "cellulosesz.home.rename"
        );

        var root = Commands.literal("renamehome")
                .then(Commands.argument("old", StringArgumentType.word())
                        .suggests((command, builder) ->
                                CommandSuggestionSupport.suggest(
                                        () -> context.player(command.getSource())
                                                .map(player -> service.cachedNames(
                                                        player.uuid()
                                                ))
                                                .orElseGet(Set::of),
                                        builder
                                )
                        )
                        .then(Commands.argument(
                                                "new",
                                                StringArgumentType.word()
                                        )
                                        .executes(command -> executeAsync(
                                                context,
                                                command,
                                                descriptor,
                                                policy -> service.rename(
                                                        policy.playerUuid()
                                                                .orElseThrow(),
                                                        StringArgumentType.getString(
                                                                command,
                                                                "old"
                                                        ),
                                                        StringArgumentType.getString(
                                                                command,
                                                                "new"
                                                        )
                                                )
                                        ))
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "",
                "/renamehome <old> <new>",
                root
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
                CommandSourceKind.PLAYER_ONLY
        );
    }

    private int executeAsync(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Function<
                    MinecraftCommandPolicyContext,
                    CompletableFuture<HomeCommandService.Result>
                    > operation
    ) {
        return registration.execute(
                command,
                descriptor,
                "home request",
                policy -> {
                    if (policy.playerUuid().isEmpty()) {
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

    private HomeCommandService.Request request(
            MinecraftCommandPolicyContext context
    ) {
        return new HomeCommandService.Request(
                context.playerUuid().orElseThrow(),
                context.playerName().orElse(""),
                context.hasPermission(
                        "cellulosesz.home.bypass-cooldown"
                ),
                context.hasPermission(
                        "cellulosesz.home.bypass-warmup"
                )
        );
    }

    @Override
    public String moduleId() {
        return MODULE;
    }

}

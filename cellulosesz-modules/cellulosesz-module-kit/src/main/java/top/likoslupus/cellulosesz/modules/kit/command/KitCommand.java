package top.likoslupus.cellulosesz.modules.kit.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;
import top.likoslupus.cellulosesz.modules.kit.application.KitCommandService;
import top.likoslupus.cellulosesz.modules.kit.application.KitCooldown;
import top.likoslupus.cellulosesz.modules.kit.command.argument.KitCooldowns;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class KitCommand implements CommandContributor {

    private static final String MODULE = "kit";

    private final KitCommandService service;

    public KitCommand(KitCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        if (!context.moduleEnabled(MODULE)) {
            return;
        }

        var service = this.service;

        registerKit(context, service);
        registerShow(context, service);
        registerCreate(context, service);
        registerDelete(context, service);
        registerReset(context, service);
    }

    private void registerKit(
            CommandRegistrationContext context,
            KitCommandService service
    ) {
        var descriptor = player("kit", "cellulosesz.kit.use");

        var root = Commands.literal("kit")
                .executes(command -> executeSync(
                        context,
                        command,
                        descriptor,
                        policy -> service.list(policy::hasPermission)
                ))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((command, builder) ->
                                CommandSuggestionSupport.suggest(
                                        () -> service.claimableNames(
                                                permission -> context.hasPermission(
                                                        command.getSource(),
                                                        permission
                                                )
                                        ),
                                        builder
                                )
                        )
                        .executes(command -> executeAsync(
                                context,
                                command,
                                descriptor,
                                policy -> {
                                    var player = policy.currentPlayer();

                                    if (player.isEmpty()) {
                                        return CompletableFuture.completedFuture(
                                                offline(policy)
                                        );
                                    }

                                    return service.claim(
                                            player.orElseThrow(),
                                            StringArgumentType.getString(
                                                    command,
                                                    "name"
                                            ),
                                            policy::hasPermission
                                    );
                                }
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of("kits"),
                "",
                "/kit [name] | /kits",
                root
        );

        context.registerSemantic(
                moduleId(),
                descriptor,
                "kits",
                Commands.literal("kits")
                        .executes(command -> executeSync(
                                context,
                                command,
                                descriptor,
                                policy -> service.list(policy::hasPermission)
                        ))
        );
    }

    private void registerShow(
            CommandRegistrationContext context,
            KitCommandService service
    ) {
        var descriptor = any("showkit", "cellulosesz.kit.show");

        var root = Commands.literal("showkit")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((_, builder) ->
                                CommandSuggestionSupport.suggest(
                                        service::kitNames,
                                        builder
                                )
                        )
                        .executes(command -> executeSync(
                                context,
                                command,
                                descriptor,
                                _ -> service.show(
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
                "/showkit <name>",
                root
        );
    }

    private void registerCreate(
            CommandRegistrationContext context,
            KitCommandService service
    ) {
        var descriptor = player("createkit", "cellulosesz.kit.create");

        var cooldown = Commands.argument("cooldown", StringArgumentType.word())
                .suggests((_, builder) -> CommandSuggestionSupport.suggest(
                        KitCooldowns::suggestions,
                        builder
                ))
                .executes(command -> create(
                        context,
                        command,
                        descriptor,
                        service,
                        KitCooldowns.parse(
                                StringArgumentType.getString(command, "cooldown")
                        )
                ));

        var root = Commands.literal("createkit")
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(cooldown)
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "",
                "/createkit <name> <seconds|once|one-time>",
                root
        );
    }

    private void registerDelete(
            CommandRegistrationContext context,
            KitCommandService service
    ) {
        var descriptor = any("delkit", "cellulosesz.kit.delete");

        var root = Commands.literal("delkit")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((_, builder) ->
                                CommandSuggestionSupport.suggest(
                                        service::kitNames,
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
                "/delkit <name>",
                root
        );
    }

    private void registerReset(
            CommandRegistrationContext context,
            KitCommandService service
    ) {
        var descriptor = any("kitreset", "cellulosesz.kit.reset");

        var kit = Commands.argument("kit", StringArgumentType.word())
                .suggests((_, builder) ->
                        CommandSuggestionSupport.suggest(
                                service::kitNames,
                                builder
                        )
                )
                .executes(command -> executeAsync(
                        context,
                        command,
                        descriptor,
                        policy -> service.reset(
                                resetRequest(
                                        policy,
                                        command,
                                        Optional.empty()
                                )
                        )
                ))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((_, builder) ->
                                CommandSuggestionSupport.suggest(
                                        context::onlinePlayerNames,
                                        builder
                                )
                        )
                        .executes(command -> executeAsync(
                                context,
                                command,
                                descriptor,
                                policy -> service.reset(
                                        resetRequest(
                                                policy,
                                                command,
                                                Optional.of(
                                                        StringArgumentType.getString(
                                                                command,
                                                                "player"
                                                        )
                                                )
                                        )
                                )
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "",
                "/kitreset <kit> [player]",
                Commands.literal("kitreset").then(kit)
        );
    }

    private CommandDescriptor player(String name, String permission) {
        return new CommandDescriptor(
                MODULE,
                name,
                permission,
                CommandSourceKind.PLAYER_ONLY
        );
    }

    private int executeSync(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Function<
                    MinecraftCommandPolicyContext,
                    KitCommandService.Result
                    > operation
    ) {
        return registration.execute(
                command,
                descriptor,
                "kit request",
                policy -> {
                    if (descriptor.requiredSourceKind()
                            == CommandSourceKind.PLAYER_ONLY
                            && policy.currentPlayer().isEmpty()
                    ) {
                        policy.error(
                                LocalizedMessage.of("common.player-only")
                        );
                        return CompletableFuture.completedFuture(
                                CommandOutcome.rejected(0)
                        );
                    }

                    var result = operation.apply(policy);
                    policy.respond(result.success(), result.message());
                    return CompletableFuture.completedFuture(
                            CommandOutcome.fromStatus(result.status())
                    );
                }
        );
    }

    private int executeAsync(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Function<
                    MinecraftCommandPolicyContext,
                    CompletableFuture<KitCommandService.Result>
                    > operation
    ) {
        return registration.execute(
                command,
                descriptor,
                "kit request",
                policy -> {
                    if (descriptor.requiredSourceKind()
                            == CommandSourceKind.PLAYER_ONLY
                            && policy.currentPlayer().isEmpty()
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

    private KitCommandService.Result offline(
            MinecraftCommandPolicyContext policy
    ) {
        return new KitCommandService.Result(
                false,
                LocalizedMessage.of(
                        "commands.common.player-offline",
                        MessageArguments.builder()
                                .add(policy.playerName() == null
                                        ? "unknown"
                                        : policy.playerName())
                                .build()
                )
        );
    }

    @Override
    public String moduleId() {
        return MODULE;
    }

    private CommandDescriptor any(String name, String permission) {
        return new CommandDescriptor(
                MODULE,
                name,
                permission,
                CommandSourceKind.ANY
        );
    }

    private int create(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            KitCommandService service,
            KitCooldown cooldown
    ) {
        return executeAsync(
                registration,
                command,
                descriptor,
                policy -> {
                    var player = policy.currentPlayer();

                    if (player.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                offline(policy)
                        );
                    }

                    return service.create(
                            player.orElseThrow(),
                            StringArgumentType.getString(command, "name"),
                            cooldown
                    );
                }
        );
    }

    private KitCommandService.ResetRequest resetRequest(
            MinecraftCommandPolicyContext policy,
            CommandContext<CommandSourceStack> command,
            Optional<String> target
    ) {
        return new KitCommandService.ResetRequest(
                policy.currentPlayer(),
                StringArgumentType.getString(command, "kit"),
                target,
                policy.hasPermission("cellulosesz.kit.reset.others")
        );
    }

}

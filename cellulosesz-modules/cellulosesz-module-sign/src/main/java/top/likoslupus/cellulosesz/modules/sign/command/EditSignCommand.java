package top.likoslupus.cellulosesz.modules.sign.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.sign.*;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandExecutions;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;
import top.likoslupus.cellulosesz.modules.sign.SignRuntimeSettings;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class EditSignCommand implements CommandContributor {

    private final SignPlatformService platform;
    private final SignService signs;
    private final ServerThreadExecutor serverThread;
    private final SignRuntimeSettings config;
    private final Map<UUID, Clipboard> clipboards = new ConcurrentHashMap<>();

    public EditSignCommand(
            SignPlatformService platform,
            SignService signs,
            ServerThreadExecutor serverThread,
            SignRuntimeSettings config
    ) {
        this.platform = requireNonNull(platform, "platform");
        this.signs = requireNonNull(signs, "signs");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.config = requireNonNull(config, "config");
    }

    private static boolean disallowedControl(int codePoint) {
        return codePoint == 0
                || codePoint == '\r'
                || codePoint == '\n'
                || Character.isISOControl(codePoint);
    }

    private static String side(boolean front) {
        return front
                ? "front"
                : "back";
    }

    @Override
    public String moduleId() {
        return SignCommandSupport.MODULE;
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = SignCommandSupport.descriptor();
        var root = Commands.literal("editsign")
                .then(Commands.literal("copy")
                        .executes(command -> executeSync(
                                context,
                                command,
                                descriptor,
                                policy -> copy(policy, target(policy))
                        )))
                .then(Commands.literal("paste")
                        .executes(command -> executeAsync(
                                context,
                                command,
                                descriptor,
                                policy -> paste(policy, target(policy))
                        )))
                .then(Commands.literal("clear")
                        .then(Commands.argument("line", IntegerArgumentType.integer(1, 4))
                                .executes(command -> executeAsync(
                                        context,
                                        command,
                                        descriptor,
                                        policy -> clear(
                                                policy,
                                                target(policy),
                                                IntegerArgumentType.getInteger(command, "line") - 1
                                        )
                                ))))
                .then(Commands.literal("set")
                        .then(Commands.argument("line", IntegerArgumentType.integer(1, 4))
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(command -> executeAsync(
                                                context,
                                                command,
                                                descriptor,
                                                policy -> set(
                                                        policy,
                                                        target(policy),
                                                        IntegerArgumentType.getInteger(
                                                                command,
                                                                "line"
                                                        ) - 1,
                                                        StringArgumentType.getString(
                                                                command,
                                                                "text"
                                                        )
                                                )
                                        )))));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.editsign",
                "/editsign <set <line:1..4> <text...>|clear <line:1..4>|copy|paste>",
                root
        );
    }

    public void clearClipboard(UUID playerUuid) {
        clipboards.remove(requireNonNull(playerUuid, "playerUuid"));
    }

    private int executeSync(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Function<
                    MinecraftCommandPolicyContext,
                    PlatformResult<?>
                    > operation
    ) {
        return CommandExecutions.sync(
                registration,
                command,
                descriptor,
                "edit sign",
                policy -> SignCommandSupport.respond(policy, operation.apply(policy))
        );
    }

    private int executeAsync(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Function<
                    MinecraftCommandPolicyContext,
                    CompletableFuture<PlatformResult<?>>
                    > operation
    ) {
        return CommandExecutions.async(
                registration,
                command,
                descriptor,
                "edit sign",
                operation,
                (policy, result) -> {
                    SignCommandSupport.respond(policy, result);
                    return CommandOutcome.fromPlatformStatus(result.status());
                }
        );
    }

    private PlatformResult<SignSnapshot> target(MinecraftCommandPolicyContext policy) {
        var player = policy.currentPlayer();
        if (player.isEmpty()) {
            return PlatformResult.failure(PlatformOperationStatus.INVALID_SOURCE, "player-only");
        }

        return platform.target(player.orElseThrow(), config.editTargetDistance());
    }

    private PlatformResult<?> copy(
            MinecraftCommandPolicyContext policy,
            PlatformResult<SignSnapshot> targetResult
    ) {
        if (!targetResult.successful() || targetResult.value().isEmpty()) {
            return targetResult;
        }

        var player = policy.currentPlayer().orElseThrow();
        var target = targetResult.value().orElseThrow();

        clipboards.put(player.uuid(), new Clipboard(target.lines()));
        policy.reply(LocalizedMessage.of(
                "commands.sign.editsign.copied",
                Map.of("side", side(target.front()))
        ));

        return PlatformResult.success(target);
    }

    private CompletableFuture<PlatformResult<?>> paste(
            MinecraftCommandPolicyContext policy,
            PlatformResult<SignSnapshot> targetResult
    ) {
        if (!targetResult.successful() || targetResult.value().isEmpty()) {
            return CompletableFuture.completedFuture(targetResult);
        }

        var player = policy.currentPlayer().orElseThrow();
        var clipboard = clipboards.get(player.uuid());

        if (clipboard == null) {
            return CompletableFuture.completedFuture(PlatformResult.failure(
                    PlatformOperationStatus.NOT_FOUND,
                    "clipboard-empty"
            ));
        }

        return mutate(policy, targetResult.value().orElseThrow(), clipboard.lines());
    }

    private CompletableFuture<PlatformResult<?>> clear(
            MinecraftCommandPolicyContext policy,
            PlatformResult<SignSnapshot> targetResult,
            int line
    ) {
        if (!targetResult.successful() || targetResult.value().isEmpty()) {
            return CompletableFuture.completedFuture(targetResult);
        }

        var target = targetResult.value().orElseThrow();
        var replacement = new ArrayList<>(target.lines());

        replacement.set(line, "");
        return mutate(policy, target, replacement);
    }

    private CompletableFuture<PlatformResult<?>> set(
            MinecraftCommandPolicyContext policy,
            PlatformResult<SignSnapshot> targetResult,
            int line,
            String text
    ) {
        if (!targetResult.successful() || targetResult.value().isEmpty()) {
            return CompletableFuture.completedFuture(targetResult);
        }

        var textFailure = validateText(policy, text);
        if (textFailure.isPresent()) {
            return CompletableFuture.completedFuture(textFailure.orElseThrow());
        }

        var target = targetResult.value().orElseThrow();
        var replacement = new ArrayList<>(target.lines());

        replacement.set(line, text);
        return mutate(policy, target, replacement);
    }

    private Optional<PlatformResult<?>> validateText(
            MinecraftCommandPolicyContext policy,
            String text
    ) {
        if (text.codePointCount(0, text.length()) > config.editMaximumLineLength()) {
            return Optional.of(PlatformResult.failure(
                    PlatformOperationStatus.INVALID_INPUT,
                    "line-too-long"
            ));
        }

        if (text.codePoints().anyMatch(EditSignCommand::disallowedControl)) {
            return Optional.of(PlatformResult.failure(
                    PlatformOperationStatus.INVALID_INPUT,
                    "control-character"
            ));
        }

        if (text.indexOf('§') >= 0
                && !policy.hasPermission("cellulosesz.command.editsign.color")
        ) {
            return Optional.of(PlatformResult.failure(
                    PlatformOperationStatus.PERMISSION_DENIED,
                    "color-denied"
            ));
        }

        if (text.matches("(?s).*<#[0-9a-fA-F]{6}>.*")
                && !policy.hasPermission("cellulosesz.command.editsign.rgb")
        ) {
            return Optional.of(PlatformResult.failure(
                    PlatformOperationStatus.PERMISSION_DENIED,
                    "rgb-denied"
            ));
        }

        if ((text.contains("<") || text.contains(">"))
                && !policy.hasPermission("cellulosesz.command.editsign.format")
        ) {
            return Optional.of(PlatformResult.failure(
                    PlatformOperationStatus.PERMISSION_DENIED,
                    "format-denied"
            ));
        }
        return Optional.empty();
    }

    private CompletableFuture<PlatformResult<?>> mutate(
            MinecraftCommandPolicyContext policy,
            SignSnapshot target,
            List<String> requested
    ) {
        var player = policy.currentPlayer().orElseThrow();
        var allowWaxed = policy.hasPermission("cellulosesz.command.editsign.waxed");

        if (target.waxed() && !allowWaxed) {
            return CompletableFuture.completedFuture(PlatformResult.failure(
                    PlatformOperationStatus.PERMISSION_DENIED,
                    "waxed"
            ));
        }

        var replacement = signs.formattedLines(List.copyOf(requested));
        var execution = signs.edit(
                player,
                target.location(),
                target.front(),
                target.lines(),
                replacement
        );

        if (!execution.handled()) {
            return serverThread
                    .submit(() -> platform.compareAndReplace(new SignWriteRequest(
                            player,
                            target.location(),
                            target.front(),
                            target.lines(),
                            replacement,
                            allowWaxed
                    )));
        }

        return execution.preparation()
                .thenCompose(commit -> serverThread
                        .submit(() ->
                                platform.compareAndReplace(new SignWriteRequest(
                                        player,
                                        target.location(),
                                        target.front(),
                                        target.lines(),
                                        replacement,
                                        allowWaxed
                                ))
                        )
                        .thenCompose(write -> {
                            if (!write.successful()) {
                                return commit
                                        .complete(false)
                                        .thenApply(_ -> write);
                            }

                            return commit
                                    .complete(true)
                                    .thenCompose(result -> {
                                        if (result.success()) {
                                            return CompletableFuture.completedFuture(write);
                                        }

                                        return serverThread
                                                .submit(() -> rollback(
                                                        player,
                                                        target,
                                                        replacement,
                                                        allowWaxed,
                                                        result
                                                ));
                                    });
                        })
                );
    }

    private PlatformResult<?> rollback(
            CellPlayer player,
            SignSnapshot target,
            List<String> replacement,
            boolean allowWaxed,
            SignUseResult result
    ) {
        var rollback = platform.compareAndReplace(new SignWriteRequest(
                player,
                target.location(),
                target.front(),
                replacement,
                target.lines(),
                allowWaxed
        ));

        if (!rollback.successful()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.CONFLICT,
                    "persistence-and-rollback-failed"
            );
        }

        return PlatformResult.failure(
                PlatformOperationStatus.STORAGE_FAILURE,
                result.optionalMessage()
                        .map(LocalizedMessage::key)
                        .orElse("persistence-failed")
        );
    }

    private record Clipboard(List<String> lines) {

        private Clipboard {
            lines = List.copyOf(lines);
            if (lines.size() != 4) {
                throw new IllegalArgumentException("lines must contain exactly four entries");
            }
        }

    }

}

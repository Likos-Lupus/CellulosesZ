package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.command.CommandContinuation;
import top.likoslupus.cellulosesz.core.command.CommandMiddleware;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public final class MuteCommandMiddleware implements CommandMiddleware {

    private final MuteService mutes;
    private volatile Set<String> blockedCommands;

    public MuteCommandMiddleware(
            MuteService mutes,
            AdminConfig config
    ) {
        this.mutes = mutes;
        configure(config);
    }

    public void configure(AdminConfig config) {
        blockedCommands = config.muteCommands.stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public CompletionStage<CommandOutcome> invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation next
    ) {
        var senderUuid = context.playerUuid();
        if (blocked(descriptor.canonicalName())
                && !context.hasPermission("cellulosesz.admin.mute.bypass")
                && senderUuid != null
                && mutes.muted(senderUuid)
        ) {
            context.error(LocalizedMessage.of(
                    "commands.admin.mute-command-middleware.error.muted-cannot-use-command"
            ));
            return CompletableFuture.completedFuture(CommandOutcome.rejected());
        }
        return next.proceed();
    }

    public boolean blocked(String root) {
        var normalized = root.trim().toLowerCase(Locale.ROOT);
        var current = blockedCommands;
        return current.contains("*") || current.contains(normalized);
    }

}

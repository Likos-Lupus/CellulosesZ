package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.MuteService;
import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;

import java.util.Locale;

public final class MuteCommandMiddleware implements CommandMiddleware {

    private final MuteService mutes;
    private volatile AdminConfig config;

    public MuteCommandMiddleware(
            MuteService mutes,
            AdminConfig config
    ) {
        this.mutes = mutes;
        this.config = config;
    }

    public void configure(AdminConfig config) {
        this.config = config;
    }

    @Override
    public int invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation next
    ) {
        if (blocked(descriptor.canonicalName())
                && !context.hasPermission("cellulosesz.admin.mute.bypass")
                && context.playerUuid().filter(mutes::muted).isPresent()
        ) {
            context.error(LocalizedMessage.of(
                    GeneratedMessageKeys.COMMANDS_ADMIN_MUTE_COMMAND_MIDDLEWARE_ERROR_MUTED_CANNOT_USE_COMMAND
            ));
            return 0;
        }
        return next.proceed();
    }

    public boolean blocked(String root) {
        var normalized = root.trim().toLowerCase(Locale.ROOT);
        return config.muteCommands.stream().anyMatch(value ->
                value.equals("*") || value.equalsIgnoreCase(normalized)
        );
    }

}

package top.likoslupus.cellulosesz.common.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.common.text.MinecraftTextAdapter;

import static java.util.Objects.requireNonNull;

public final class MinecraftCommandResponder {

    private final PlatformService platform;
    private final MessageRenderer renderer;
    private final LocaleResolver locales;
    private final ServerThreadExecutor serverThread;
    private final CellulosesZLogger logger;

    public MinecraftCommandResponder(
            PlatformService platform,
            MessageRenderer renderer,
            LocaleResolver locales,
            ServerThreadExecutor serverThread,
            CellulosesZLogger logger
    ) {
        this.platform = requireNonNull(platform, "platform");
        this.renderer = requireNonNull(renderer, "renderer");
        this.locales = requireNonNull(locales, "locales");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.logger = requireNonNull(logger, "logger");
    }

    public void reply(CommandSourceStack source, LocalizedMessage message) {
        sendLocalized(source, message, false);
    }

    private void sendLocalized(
            CommandSourceStack source,
            LocalizedMessage message,
            boolean error
    ) {
        requireNonNull(message, "message");
        schedule(() -> sendNow(
                source,
                renderer.render(
                        locale(source),
                        message.key(),
                        message.placeholders()
                ),
                error
        ));
    }

    private void schedule(Runnable task) {
        try {
            serverThread.execute(task);
        } catch (IllegalStateException expectedShutdown) {
            logger.debug("Dropping command response because the server is unavailable: " + expectedShutdown.getMessage());
        } catch (RuntimeException failure) {
            logger.error("Failed to schedule command response", failure);
        }
    }

    private void sendNow(
            CommandSourceStack source,
            RichText message,
            boolean error
    ) {
        try {
            var component = MinecraftTextAdapter.toComponent(message, logger);
            if (error) {
                source.sendFailure(component);
            } else {
                source.sendSuccess(() -> component, false);
            }
        } catch (RuntimeException failure) {
            if (source.getEntity() instanceof ServerPlayer player
                    && player.hasDisconnected()
            ) {
                logger.debug("Dropping command response for disconnected player " + player.getUUID());
            } else {
                logger.error("Failed to send command response", failure);
            }
        }
    }

    private String locale(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player
                && !player.hasDisconnected()
        ) {
            return platform.player(player)
                    .map(locales::locale)
                    .orElseGet(locales::consoleLocale);
        }
        return locales.consoleLocale();
    }

    public void error(CommandSourceStack source, LocalizedMessage message) {
        sendLocalized(source, message, true);
    }

    public void reply(CommandSourceStack source, RichText message) {
        sendRich(source, message, false);
    }

    private void sendRich(
            CommandSourceStack source,
            RichText message,
            boolean error
    ) {
        schedule(() -> sendNow(source, requireNonNull(message, "message"), error));
    }

    public void error(CommandSourceStack source, RichText message) {
        sendRich(source, message, true);
    }

}

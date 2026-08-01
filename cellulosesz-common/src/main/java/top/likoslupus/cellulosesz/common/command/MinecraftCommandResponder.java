package top.likoslupus.cellulosesz.common.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.text.*;
import top.likoslupus.cellulosesz.common.text.MinecraftTextAdapter;

import static java.util.Objects.requireNonNull;

public final class MinecraftCommandResponder {

    private final PlayerDirectory players;
    private final PlayerAudienceService audiences;
    private final MessageRenderer renderer;
    private final LocaleResolver locales;
    private final ServerThreadExecutor serverThread;
    private final CellulosesZLogger logger;

    public MinecraftCommandResponder(
            PlayerDirectory players,
            PlayerAudienceService audiences,
            MessageRenderer renderer,
            LocaleResolver locales,
            ServerThreadExecutor serverThread,
            CellulosesZLogger logger
    ) {
        this.players = requireNonNull(players, "players");
        this.audiences = requireNonNull(audiences, "audiences");
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
            logger.debug("Dropping command response because the server is unavailable: "
                    + expectedShutdown.getMessage()
            );
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
            if (source.getEntity() instanceof ServerPlayer player) {
                var audience = players.onlinePlayer(player.getUUID());
                if (audience.isEmpty()) {
                    logger.debug("Dropping command response for disconnected player "
                            + player.getUUID()
                    );
                    return;
                }

                audiences.send(audience.orElseThrow(), message);
                return;
            }

            var component = MinecraftTextAdapter.toComponent(message, logger);
            if (error) {
                source.sendFailure(component);
            } else {
                source.sendSuccess(() -> component, false);
            }
        } catch (RuntimeException failure) {
            logger.error("Failed to send command response", failure);
        }
    }

    private String locale(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return players.onlinePlayer(player.getUUID())
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
        schedule(() -> sendNow(
                source,
                requireNonNull(message, "message"),
                error
        ));
    }

    public void error(CommandSourceStack source, RichText message) {
        sendRich(source, message, true);
    }

}

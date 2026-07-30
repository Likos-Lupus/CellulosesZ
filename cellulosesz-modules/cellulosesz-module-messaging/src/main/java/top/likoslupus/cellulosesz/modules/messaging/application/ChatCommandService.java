package top.likoslupus.cellulosesz.modules.messaging.application;

import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.messaging.MessageResult;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.api.world.WorldResolution;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class ChatCommandService {

    private final ServiceRegistry services;
    private final PlayerDirectory players;
    private final PlayerAudienceService audiences;
    private final PlayerLocationPlatformService locations;
    private final WorldDirectory worlds;
    private final ServerThreadExecutor serverThread;
    private final PermissionService permissions;
    private final DisplayNameService displayNames;
    private final MessageRenderer renderer;
    private final MessagingConfig config;

    public ChatCommandService(
            ServiceRegistry services,
            PlayerDirectory players,
            PlayerAudienceService audiences,
            PlayerLocationPlatformService locations,
            WorldDirectory worlds,
            ServerThreadExecutor serverThread,
            PermissionService permissions,
            DisplayNameService displayNames,
            MessageRenderer renderer,
            MessagingConfig config
    ) {
        this.services = requireNonNull(services, "services");
        this.players = requireNonNull(players, "players");
        this.audiences = requireNonNull(audiences, "audiences");
        this.locations = requireNonNull(locations, "locations");
        this.worlds = requireNonNull(worlds, "worlds");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.permissions = requireNonNull(permissions, "permissions");
        this.displayNames = requireNonNull(displayNames, "displayNames");
        this.renderer = requireNonNull(renderer, "renderer");
        this.config = requireNonNull(config, "config");
    }

    public CompletableFuture<MessageResult> broadcast(String message) {
        var invalid = validate(message);
        if (invalid.isPresent()) {
            return CompletableFuture.completedFuture(invalid.orElseThrow());
        }

        return serverThread
                .submit(() -> deliver(
                        players.onlinePlayers(),
                        "messaging.broadcast",
                        Map.of("message", message)
                ))
                .thenApply(counts -> deliveryResult(
                        "commands.messaging.broadcast-result",
                        counts
                ));
    }

    private Optional<MessageResult> validate(String message) {
        if (message.isBlank()) {
            return Optional.of(MessageResult.failure("service.messaging.empty-message"));
        }

        if (message.length() > config.maxMessageLength) {
            return Optional.of(MessageResult.failure(
                    "commands.messaging.message-too-long",
                    Map.of("maximum", config.maxMessageLength)
            ));
        }

        return Optional.empty();
    }

    private Counts deliver(
            List<CellPlayer> recipients,
            String key,
            Map<String, ?> placeholders
    ) {
        var sent = 0;
        var failed = 0;
        for (var player : recipients) {
            var result = audiences.send(
                    player,
                    renderer.render(audiences.locale(player), key, placeholders)
            );
            if (result.successful()) {
                sent++;
            } else {
                failed++;
            }
        }
        return new Counts(sent, failed);
    }

    private MessageResult deliveryResult(String key, Counts counts) {
        return deliveryResult(key, counts, Map.of());
    }

    private MessageResult deliveryResult(
            String key,
            Counts counts,
            Map<String, ?> extra
    ) {
        var values = new LinkedHashMap<String, Object>(extra);
        values.put("sent", counts.sent());
        values.put("failed", counts.failed());
        return counts.sent() > 0
                ? MessageResult.success(key, values)
                : MessageResult.failure(key, values);
    }

    public CompletableFuture<MessageResult> broadcastWorld(String input, String message) {
        var invalid = validate(message);
        if (invalid.isPresent()) {
            return CompletableFuture.completedFuture(invalid.orElseThrow());
        }

        return serverThread
                .submit(() -> {
                    var resolution = worlds.resolve(input);
                    if (resolution.status() != WorldResolution.Status.RESOLVED) {
                        return new WorldDelivery(resolution, new Counts(0, 0));
                    }

                    var world = resolution.worldId().orElseThrow();
                    var recipients = players.onlinePlayers().stream()
                            .filter(player -> locations.currentLocation(player).world.equals(world))
                            .toList();

                    return new WorldDelivery(
                            resolution,
                            deliver(recipients, "messaging.broadcast", Map.of("message", message))
                    );
                })
                .thenApply(result -> switch (result.resolution().status()) {
                    case NOT_FOUND -> MessageResult.failure(
                            "commands.messaging.broadcast-world-command.world",
                            Map.of("world", input)
                    );
                    case AMBIGUOUS -> MessageResult.failure(
                            "commands.messaging.broadcast-world-command.ambiguous",
                            Map.of(
                                    "world", input,
                                    "candidates", String.join(", ", result.resolution().candidates())
                            )
                    );
                    case RESOLVED -> deliveryResult(
                            "commands.messaging.broadcast-world-result",
                            result.counts(),
                            Map.of("world", result.resolution().worldId().orElseThrow())
                    );
                });
    }

    public CompletableFuture<MessageResult> helpOp(Optional<CellPlayer> sender, String message) {
        var invalid = validate(message);
        if (invalid.isPresent()) {
            return CompletableFuture.completedFuture(invalid.orElseThrow());
        }

        return serverThread
                .submit(() -> {
                    var senderName = sender
                            .map(displayNames::displayName)
                            .orElseGet(() -> renderer.render("en_us", "common.console"));
                    var recipients = players.onlinePlayers().stream()
                            .filter(player -> permissions.has(
                                    player,
                                    "cellulosesz.messaging.helpop.receive"
                            ))
                            .toList();

                    if (recipients.isEmpty()) {
                        return new Counts(0, 0);
                    }

                    return deliver(
                            recipients,
                            "messaging.helpop",
                            Map.of("sender", senderName, "message", message)
                    );
                })
                .thenApply(counts -> counts.sent() == 0
                        ? MessageResult.failure("commands.messaging.helpop-no-recipient")
                        : deliveryResult("commands.messaging.helpop-result", counts)
                );
    }

    public CompletableFuture<MessageResult> me(CellPlayer actor, String action) {
        var invalid = validate(action);
        if (invalid.isPresent()) {
            return CompletableFuture.completedFuture(invalid.orElseThrow());
        }

        return serverThread
                .submit(() -> {
                    var recipients = players.onlinePlayers().stream()
                            .filter(viewer -> canSee(viewer, actor))
                            .toList();
                    return deliver(
                            recipients,
                            "messaging.me",
                            Map.of(
                                    "player", displayNames.displayName(actor),
                                    "action", action
                            )
                    );
                })
                .thenApply(counts -> deliveryResult("commands.messaging.me-result", counts));
    }

    private boolean canSee(CellPlayer viewer, CellPlayer target) {
        return services.optional(VanishService.class)
                .map(service -> service.canSee(viewer, target.uuid()))
                .orElse(true);
    }

    public CompletableFuture<MessageResult> list(Optional<CellPlayer> viewer) {
        return serverThread
                .submit(() -> {
                    var visible = players.onlinePlayers().stream()
                            .filter(player ->
                                    viewer.map(current -> canSee(current, player))
                                            .orElse(true)
                            )
                            .limit(100)
                            .toList();
                    var text = RichText.empty();

                    for (int i = 0; i < visible.size(); i++) {
                        if (i > 0) {
                            text = text.append(RichText.plain(", "));
                        }
                        text = text.append(displayNames.displayName(visible.get(i)));
                    }

                    return MessageResult.success(
                            "player.list",
                            Map.of(
                                    "count", visible.size(),
                                    "players", text)
                    );
                });
    }

    public List<String> worldNames() {
        return worlds.loadedWorldIds();
    }

    private record Counts(
            int sent,
            int failed
    ) {

    }

    private record WorldDelivery(
            WorldResolution resolution,
            Counts counts
    ) {

    }

}

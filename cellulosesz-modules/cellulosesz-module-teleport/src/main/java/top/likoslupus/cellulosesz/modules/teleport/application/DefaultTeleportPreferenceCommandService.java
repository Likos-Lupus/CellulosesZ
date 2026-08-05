package top.likoslupus.cellulosesz.modules.teleport.application;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.api.user.UserUpdate;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandStatus.NOT_FOUND;
import static top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandStatus.PERSISTENCE_FAILURE;

import static java.util.Objects.requireNonNull;

public final class DefaultTeleportPreferenceCommandService
        implements TeleportPreferenceCommandService {

    private final UserService users;
    private final PlayerResolver resolver;

    public DefaultTeleportPreferenceCommandService(
            UserService users,
            PlayerResolver resolver
    ) {
        this.users = requireNonNull(users, "users");
        this.resolver = requireNonNull(resolver, "resolver");
    }

    @Override
    public CompletableFuture<TeleportCommandResult> autoAccept(
            CellPlayer player,
            Optional<Boolean> requested
    ) {
        return users
                .update(
                        player.uuid(),
                        user -> {
                            var next = requested.orElse(!user.preferences().teleportAutoAccept());
                            return UserUpdate.of(
                                    user.withPreferences(user
                                            .preferences()
                                            .withTeleportAutoAccept(next)),
                                    next
                            );
                        }
                )
                .thenApply(enabled -> TeleportCommandResult.success(
                        "commands.teleport.tp-auto-command.reply.changed",
                        MessageArguments.builder().add(enabled
                                ? "on"
                                : "off"
                        ).build()
                ))
                .exceptionally(_ -> TeleportCommandResult.failure(
                        PERSISTENCE_FAILURE,
                        "commands.teleport.preference-persistence-failed"
                ));
    }

    @Override
    public CompletableFuture<TeleportCommandResult> toggle(
            CellPlayer actor,
            Optional<String> target,
            Optional<Boolean> requested
    ) {
        if (target.isEmpty()) {
            return update(actor.uuid(), actor.name(), requested);
        }

        return resolver
                .resolve(target.orElseThrow(), actor)
                .thenCompose(resolved -> resolved.optionalUuid().isEmpty()
                        ?
                        CompletableFuture.completedFuture(TeleportCommandResult.failure(
                                NOT_FOUND,
                                "commands.common.player-not-found",
                                MessageArguments.builder()
                                        .add(target.orElseThrow())
                                        .build()
                        ))
                        : update(
                                resolved.optionalUuid().orElseThrow(),
                                resolved.name(),
                                requested
                        )
                );
    }

    private CompletableFuture<TeleportCommandResult> update(
            UUID uuid,
            String name,
            Optional<Boolean> requested
    ) {
        return users
                .update(
                        uuid,
                        user -> {
                            var next = requested.orElse(!user.preferences().teleportRequests());
                            return UserUpdate.of(
                                    user.withPreferences(user
                                            .preferences()
                                            .withTeleportRequests(next)),
                                    next
                            );
                        }
                )
                .thenApply(enabled -> TeleportCommandResult.success(
                        "commands.teleport.tp-toggle-command.reply.changed",
                        MessageArguments.builder().add(name).add(enabled
                                ? "on"
                                : "off"
                        ).build()
                ))
                .exceptionally(_ -> TeleportCommandResult.failure(
                        PERSISTENCE_FAILURE,
                        "commands.teleport.preference-persistence-failed"
                ));
    }

}

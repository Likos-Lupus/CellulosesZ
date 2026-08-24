package top.likoslupus.cellulosesz.common.command.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.UUID;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNoControlCharacters;

import static java.util.Objects.requireNonNull;

public record PlayerCommandDispatchRequest(
        CellPlayer target,
        UUID actorId,
        CommandDispatchOrigin origin,
        String command,
        UUID chainToken
) {

    public static final UUID CONSOLE_ACTOR_ID = new UUID(0L, 0L);

    public PlayerCommandDispatchRequest {
        requireNonNull(target, "target");
        requireNonNull(actorId, "actorId");
        requireNonNull(origin, "origin");
        command = requireNonNull(command, "command");
        requireNoControlCharacters(command, "command");
        requireNonNull(chainToken, "chainToken");
    }

    public static PlayerCommandDispatchRequest start(
            CellPlayer target,
            UUID actorId,
            CommandDispatchOrigin origin,
            String command
    ) {
        return new PlayerCommandDispatchRequest(
                target,
                actorId,
                origin,
                command,
                UUID.randomUUID()
        );
    }

}

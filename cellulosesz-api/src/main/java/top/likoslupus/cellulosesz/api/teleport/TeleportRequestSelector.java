package top.likoslupus.cellulosesz.api.teleport;

import java.util.UUID;

import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public sealed interface TeleportRequestSelector
        permits TeleportRequestSelector.RequestId, TeleportRequestSelector.PlayerName {

    record RequestId(
            UUID id
    ) implements TeleportRequestSelector {

        public RequestId {
            requireNonNull(id, "id");
        }

    }

    record PlayerName(
            String name
    ) implements TeleportRequestSelector {

        public PlayerName {
            name = requireNonBlank(name, "name").trim();
        }

    }

}

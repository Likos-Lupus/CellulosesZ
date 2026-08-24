package top.likoslupus.cellulosesz.modules.teleport.domain;

import java.util.UUID;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;

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

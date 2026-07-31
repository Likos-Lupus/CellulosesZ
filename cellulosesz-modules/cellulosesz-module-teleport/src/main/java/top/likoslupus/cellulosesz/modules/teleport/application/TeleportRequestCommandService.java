package top.likoslupus.cellulosesz.modules.teleport.application;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestSelector;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestType;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface TeleportRequestCommandService {

    CompletableFuture<TeleportCommandResult> create(
            CellPlayer requester,
            String target,
            TeleportRequestType type,
            boolean bypassPreference
    );

    CompletableFuture<TeleportCommandResult> createAll(
            CellPlayer requester,
            boolean bypassPreference
    );

    CompletableFuture<TeleportCommandResult> accept(
            CellPlayer target,
            Optional<TeleportRequestSelector> selector,
            boolean automatic
    );

    CompletableFuture<TeleportCommandResult> deny(
            CellPlayer target,
            Optional<TeleportRequestSelector> selector
    );

    CompletableFuture<TeleportCommandResult> cancel(
            CellPlayer requester,
            Optional<TeleportRequestSelector> selector
    );

}

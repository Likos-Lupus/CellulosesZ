package top.likoslupus.cellulosesz.modules.warp.application;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public interface WarpCommandService {

    CompletableFuture<Result> list(int page, Predicate<String> hasPermission);

    CompletableFuture<Result> teleport(
            Request request,
            String name,
            Predicate<String> hasPermission
    );

    CompletableFuture<Result> set(
            Request request,
            String name,
            Predicate<String> hasPermission
    );

    CompletableFuture<Result> delete(String name);

    CompletableFuture<Result> info(String name);

    List<String> cachedNames();

    List<String> usableNames(Predicate<String> hasPermission);

    void configure(WarpConfig config);

    record Request(
            UUID playerUuid,
            String playerName,
            boolean bypassCooldown,
            boolean bypassWarmup
    ) {

    }

    record Result(
            boolean success,
            LocalizedMessage message
    ) {

    }

}

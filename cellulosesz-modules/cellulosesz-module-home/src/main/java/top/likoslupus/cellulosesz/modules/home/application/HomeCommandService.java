package top.likoslupus.cellulosesz.modules.home.application;

import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.modules.home.HomeConfig;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public interface HomeCommandService {

    CompletableFuture<Result> list(UUID playerUuid);

    CompletableFuture<Result> teleport(Request request, String name);

    CompletableFuture<Result> set(
            Request request,
            String name,
            boolean bypassLimit
    );

    CompletableFuture<Result> delete(UUID playerUuid, String name);

    CompletableFuture<Result> rename(
            UUID playerUuid,
            String oldName,
            String newName
    );

    Set<String> cachedNames(UUID playerUuid);

    void configure(HomeConfig config);

    record Request(
            UUID playerUuid,
            String playerName,
            boolean bypassCooldown,
            boolean bypassWarmup
    ) {

    }

    record Result(
            CommandOutcome.Status status,
            LocalizedMessage message
    ) {

        public Result(
                boolean success,
                LocalizedMessage message
        ) {
            this(
                    success
                            ? CommandOutcome.Status.SUCCESS
                            : CommandOutcome.Status.REJECTED, message
            );
        }

        public Result {
            requireNonNull(status, "status");
            requireNonNull(message, "message");
        }

        public boolean success() {
            return status == CommandOutcome.Status.SUCCESS;
        }

    }

}

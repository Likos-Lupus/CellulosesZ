package top.likoslupus.cellulosesz.modules.kit.application;

import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public interface KitCommandService {

    Result list(Predicate<String> hasPermission);

    CompletableFuture<Result> claim(
            CellPlayer player,
            String name,
            Predicate<String> hasPermission
    );

    Result show(String name);

    CompletableFuture<Result> create(
            CellPlayer player,
            String name,
            KitCooldown cooldown
    );

    CompletableFuture<Result> delete(String name);

    CompletableFuture<Result> reset(ResetRequest request);

    List<String> kitNames();

    List<String> claimableNames(Predicate<String> hasPermission);

    record ResetRequest(
            Optional<CellPlayer> requester,
            String kit,
            Optional<String> target,
            boolean canResetOthers
    ) {

        public ResetRequest {
            requireNonNull(requester, "requester");
            kit = requireNonNull(kit, "kit");
            requireNonNull(target, "target");
        }

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

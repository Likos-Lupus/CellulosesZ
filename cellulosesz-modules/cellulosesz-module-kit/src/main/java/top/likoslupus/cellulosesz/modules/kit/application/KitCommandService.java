package top.likoslupus.cellulosesz.modules.kit.application;

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
            requester = requireNonNull(requester, "requester");
            kit = requireNonNull(kit, "kit");
            target = requireNonNull(target, "target");
        }

    }

    record Result(
            boolean success,
            LocalizedMessage message
    ) {

    }

}

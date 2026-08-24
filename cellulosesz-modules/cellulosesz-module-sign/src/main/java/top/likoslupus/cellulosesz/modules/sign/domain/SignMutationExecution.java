package top.likoslupus.cellulosesz.modules.sign.domain;

import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public record SignMutationExecution(
        boolean handled,
        CompletableFuture<SignMutationCommit> preparation
) {

    public SignMutationExecution {
        requireNonNull(preparation, "preparation");
        if (!handled && !preparation.isDone()) {
            throw new IllegalArgumentException("An unhandled sign mutation must already be complete");
        }
    }

    public static SignMutationExecution pass() {
        var commit = SignMutationCommits.completed(SignUseResult.pass());
        return new SignMutationExecution(false, CompletableFuture.completedFuture(commit));
    }

    public static SignMutationExecution handled(CompletableFuture<SignMutationCommit> preparation) {
        return new SignMutationExecution(true, preparation);
    }

}

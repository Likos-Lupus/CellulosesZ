package top.likoslupus.cellulosesz.api.sign;

import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public record SignExecution(
        boolean handled,
        CompletableFuture<SignUseResult> result
) {

    public SignExecution {
        requireNonNull(result, "result");
        if (!handled && !result.isDone()) {
            throw new IllegalArgumentException("An unhandled sign execution must already be complete");
        }
    }

    public static SignExecution pass() {
        return new SignExecution(false, CompletableFuture.completedFuture(SignUseResult.pass()));
    }

    public static SignExecution handled(CompletableFuture<SignUseResult> result) {
        return new SignExecution(true, result);
    }

}

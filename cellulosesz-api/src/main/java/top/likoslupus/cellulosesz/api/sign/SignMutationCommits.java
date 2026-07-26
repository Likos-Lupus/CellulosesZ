package top.likoslupus.cellulosesz.api.sign;

import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class SignMutationCommits {

    private SignMutationCommits() {
    }

    public static SignMutationCommit completed(SignUseResult result) {
        return new CompletedCommit(requireNonNull(result, "result"));
    }

    private record CompletedCommit(SignUseResult result) implements SignMutationCommit {

        @Override
        public boolean platformActionRequired() {
            return false;
        }

        @Override
        public CompletableFuture<SignUseResult> complete(boolean platformApplied) {
            return CompletableFuture.completedFuture(result);
        }

    }

}

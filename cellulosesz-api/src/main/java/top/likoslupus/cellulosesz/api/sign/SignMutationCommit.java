package top.likoslupus.cellulosesz.api.sign;

import java.util.concurrent.CompletableFuture;

public interface SignMutationCommit {

    SignUseResult result();

    boolean platformActionRequired();

    CompletableFuture<SignUseResult> complete(boolean platformApplied);

}

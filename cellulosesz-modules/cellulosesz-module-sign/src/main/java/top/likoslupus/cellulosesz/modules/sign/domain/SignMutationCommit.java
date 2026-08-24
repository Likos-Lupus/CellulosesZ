package top.likoslupus.cellulosesz.modules.sign.domain;

import java.util.concurrent.CompletableFuture;

public interface SignMutationCommit {

    SignUseResult result();

    boolean platformActionRequired();

    CompletableFuture<SignUseResult> complete(boolean platformApplied);

}

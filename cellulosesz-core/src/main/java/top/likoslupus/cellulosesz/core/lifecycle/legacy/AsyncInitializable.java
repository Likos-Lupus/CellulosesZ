package top.likoslupus.cellulosesz.core.lifecycle.legacy;

import java.util.concurrent.CompletableFuture;

public interface AsyncInitializable {

    CompletableFuture<Void> initialize();

}

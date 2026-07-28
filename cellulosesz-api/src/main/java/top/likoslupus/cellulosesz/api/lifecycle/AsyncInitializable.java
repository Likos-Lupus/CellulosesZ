package top.likoslupus.cellulosesz.api.lifecycle;

import java.util.concurrent.CompletableFuture;

public interface AsyncInitializable {

    CompletableFuture<Void> initialize();

}

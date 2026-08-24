package top.likoslupus.cellulosesz.core.lifecycle.legacy;

import java.util.concurrent.CompletableFuture;

public interface AsyncCloseable {

    default CompletableFuture<Void> closeAsync() {
        stopAccepting();
        return drain();
    }

    void stopAccepting();

    CompletableFuture<Void> drain();

}

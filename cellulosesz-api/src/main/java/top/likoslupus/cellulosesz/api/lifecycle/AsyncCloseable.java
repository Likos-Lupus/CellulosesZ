package top.likoslupus.cellulosesz.api.lifecycle;

import java.util.concurrent.CompletableFuture;

public interface AsyncCloseable {

    default CompletableFuture<Void> closeAsync() {
        stopAccepting();
        return drain();
    }

    void stopAccepting();

    CompletableFuture<Void> drain();

}

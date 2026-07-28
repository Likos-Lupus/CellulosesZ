package top.likoslupus.cellulosesz.api.lifecycle;

import java.util.concurrent.CompletableFuture;

public interface AsyncCloseable {

    CompletableFuture<Void> closeAsync();

}

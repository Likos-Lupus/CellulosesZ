package top.likoslupus.cellulosesz.api.module;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.service.Registration;

import java.util.concurrent.CompletableFuture;

public interface ModuleScope {

    String owner();

    <R extends Registration> R own(R registration);

    @Deprecated(forRemoval = true)
    default void own(AsyncCloseable closeable) {
        throw new UnsupportedOperationException(
                "AsyncCloseable registration is deprecated in ModuleScope"
        );
    }

    boolean closing();

    @Deprecated(forRemoval = true)
    default CompletableFuture<Void> closeAsync() {
        return CompletableFuture.completedFuture(null);
    }

}

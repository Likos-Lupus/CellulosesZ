package top.likoslupus.cellulosesz.api.module;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.service.Registration;

import java.util.concurrent.CompletableFuture;

public interface ModuleScope {

    String owner();

    <R extends Registration> R own(R registration);

    void own(AsyncCloseable closeable);

    boolean closing();

    CompletableFuture<Void> closeAsync();

}

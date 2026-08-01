package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.module.ModuleScope;
import top.likoslupus.cellulosesz.api.service.Registration;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.jspecify.annotations.Nullable;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public final class DefaultModuleScope implements ModuleScope {

    private final String owner;
    private final List<Object> resources = new ArrayList<>();
    private final Set<AsyncCloseable> closeables = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean closing;
    private @Nullable CompletableFuture<Void> closeFuture;

    public DefaultModuleScope(String owner) {
        this.owner = requireNonBlank(owner, "owner");
    }

    @Override
    public String owner() {
        return owner;
    }

    @Override
    public synchronized <R extends Registration> R own(R registration) {
        requireNonNull(registration, "registration");
        if (!owner.equals(registration.owner())) {
            registration.close();
            throw new IllegalArgumentException(
                    "Registration owner %s does not match module scope owner %s".formatted(
                            registration.owner(),
                            owner
                    )
            );
        }

        if (closing) {
            registration.close();
            throw new IllegalStateException("Module scope is closing: " + owner);
        }

        resources.add(registration);
        return registration;
    }

    @Override
    public synchronized void own(AsyncCloseable closeable) {
        requireNonNull(closeable, "closeable");
        if (closing) {
            closeable.stopAccepting();
            closeable.drain();
            throw new IllegalStateException("Module scope is closing: " + owner);
        }

        if (closeables.add(closeable)) {
            resources.add(closeable);
        }
    }

    @Override
    public synchronized boolean closing() {
        return closing;
    }

    @Override
    public synchronized CompletableFuture<Void> closeAsync() {
        if (closeFuture != null) {
            return closeFuture;
        }

        closing = true;
        var reverse = new ArrayList<>(resources);
        Collections.reverse(reverse);

        var failures = new ArrayList<Throwable>();
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var resource : reverse) {
            chain = chain.thenCompose(_ -> closeResource(resource)
                    .handle((_, failure) -> {
                        if (failure != null) {
                            failures.add(unwrap(failure));
                        }
                        return (Void) null;
                    })
            );
        }

        closeFuture = chain
                .thenCompose(_ -> {
                    if (failures.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    var aggregate = new IllegalStateException(
                            "Module scope close failed: " + owner
                    );
                    failures.forEach(aggregate::addSuppressed);
                    return CompletableFuture.failedFuture(aggregate);
                });
        return closeFuture;
    }

    private static CompletableFuture<Void> closeResource(Object resource) {
        try {
            if (resource instanceof Registration registration) {
                registration.close();
                return CompletableFuture.completedFuture(null);
            }

            var closeable = (AsyncCloseable) resource;
            closeable.stopAccepting();

            return requireNonNull(closeable.drain(), "drain future");
        } catch (Throwable failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private static Throwable unwrap(Throwable failure) {
        var current = failure;
        while (current instanceof CompletionException
                && current.getCause() != null
        ) {
            current = current.getCause();
        }
        return current;
    }

}

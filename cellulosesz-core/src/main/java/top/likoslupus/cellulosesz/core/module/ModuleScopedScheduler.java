package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.scheduler.TaskHandle;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class ModuleScopedScheduler implements Scheduler {

    private final String owner;
    private final Scheduler delegate;
    private final DefaultModuleScope scope;

    ModuleScopedScheduler(
            String owner,
            Scheduler delegate,
            DefaultModuleScope scope
    ) {
        this.owner = owner;
        this.delegate = delegate;
        this.scope = scope;
    }

    @Override
    public TaskHandle sync(
            Runnable task,
            String ignoredOwner
    ) {
        return scope.own(delegate.sync(task, owner));
    }

    @Override
    public TaskHandle syncLater(
            Runnable task,
            long ticks,
            String ignoredOwner
    ) {
        return scope.own(delegate.syncLater(task, ticks, owner));
    }

    @Override
    public TaskHandle syncRepeating(
            Runnable task,
            long delayTicks,
            long periodTicks,
            String ignoredOwner
    ) {
        return scope.own(delegate.syncRepeating(task, delayTicks, periodTicks, owner));
    }

    @Override
    public CompletableFuture<Void> async(Runnable task) {
        return delegate.async(task);
    }

    @Override
    public <T> CompletableFuture<T> async(Supplier<T> supplier) {
        return delegate.async(supplier);
    }

}

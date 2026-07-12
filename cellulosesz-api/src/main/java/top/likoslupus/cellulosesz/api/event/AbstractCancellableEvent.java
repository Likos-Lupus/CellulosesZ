package top.likoslupus.cellulosesz.api.event;

public abstract class AbstractCancellableEvent implements CancellableEvent {

    private boolean cancelled;

    @Override
    public final boolean cancelled() {
        return cancelled;
    }

    @Override
    public final void cancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}

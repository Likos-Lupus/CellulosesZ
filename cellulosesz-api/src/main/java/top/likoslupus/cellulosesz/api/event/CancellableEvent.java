package top.likoslupus.cellulosesz.api.event;

public interface CancellableEvent {

    boolean cancelled();

    default void cancel() {
        cancelled(true);
    }

    void cancelled(boolean cancelled);

}

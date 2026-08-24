package top.likoslupus.cellulosesz.api.event

public interface CancellableEvent {

    public fun cancelled(): Boolean

    public fun cancel() {
        cancelled(true)
    }

    public fun cancelled(cancelled: Boolean)

}

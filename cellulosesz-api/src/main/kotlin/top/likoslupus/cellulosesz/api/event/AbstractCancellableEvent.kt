package top.likoslupus.cellulosesz.api.event

public abstract class AbstractCancellableEvent : CancellableEvent {

    private var cancelled: Boolean = false

    override fun cancelled(): Boolean = cancelled

    override fun cancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }

}

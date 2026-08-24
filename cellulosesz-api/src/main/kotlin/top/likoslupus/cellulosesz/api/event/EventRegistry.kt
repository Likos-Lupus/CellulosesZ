package top.likoslupus.cellulosesz.api.event

import top.likoslupus.cellulosesz.api.service.Registration
import java.util.function.Consumer

public interface EventRegistry {

    public fun <T : Any> listen(
        eventType: Class<T>,
        listener: Consumer<T>
    ): Registration =
        listen(eventType, listener, "global")

    public fun <T : Any> listen(
        eventType: Class<T>,
        listener: Consumer<T>,
        owner: String
    ): Registration

    public fun <T : CancellableEvent> fireCancellable(event: T): Boolean {
        fire(event)
        return !event.cancelled()
    }

    public fun <T : Any> fire(event: T)

}

package top.likoslupus.cellulosesz.administration.integration

import com.mojang.logging.LogUtils
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Fan-out of committed moderation facts to the registered observers. One observer corresponds to the
 * composition root's forwarding adapter; observers are isolated, so a throwing one is logged and
 * never blocks the others or the committed action that produced the fact.
 */
internal class ModerationEventBus {

    private val observers = CopyOnWriteArrayList<ModerationDomainObserver>()

    fun subscribe(observer: ModerationDomainObserver): ModerationEventRegistration {
        observers += observer
        return ModerationEventRegistration { observers.remove(observer) }
    }

    fun publish(event: ModerationDomainEvent) {
        observers.forEach { observer ->
            runCatching { observer.onEvent(event) }
                    .onFailure { LOGGER.warn("moderation_observer_failed", it) }
        }
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}

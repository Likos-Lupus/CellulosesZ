package top.likoslupus.cellulosesz.communication

import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import kotlin.time.AbstractLongTimeSource
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

/** Deterministic monotonic source for reply-timeout and rate-limit tests. */
internal class FakeTimeSource : AbstractLongTimeSource(DurationUnit.NANOSECONDS) {

    private var nanos: Long = 0L

    override fun read(): Long = nanos

    fun advance(by: Duration) {
        nanos += by.inWholeNanoseconds
    }

}

internal fun timeSourceOf(source: FakeTimeSource = FakeTimeSource()): TimeSource =
    source

/** Runs the block immediately, emulating "already on the server thread". */
internal object ImmediateServerThreadRunner : ServerThreadRunner {

    override suspend fun <T> run(block: () -> T): T =
        block()

}

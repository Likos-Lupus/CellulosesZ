package top.likoslupus.cellulosesz.communication

import net.kyori.adventure.text.Component
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import top.likoslupus.cellulosesz.core.text.model.DetailView
import top.likoslupus.cellulosesz.core.text.model.ListPage
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import java.util.*
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

/** Presentation stub that exposes the semantic key instead of a real theme. */
internal class FakePresentation : TextPresentation {

    override fun renderFor(playerId: UUID?, notice: Notice): List<Component> =
        listOf(Component.text(notice.text.key.value))

    override fun renderFor(playerId: UUID?, page: ListPage): List<Component> =
        listOf(Component.text(page.title.key.value))

    override fun renderFor(playerId: UUID?, detail: DetailView): List<Component> =
        listOf(Component.text(detail.title.key.value))

}

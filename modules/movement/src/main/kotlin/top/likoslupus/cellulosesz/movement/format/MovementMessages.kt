package top.likoslupus.cellulosesz.movement.format

import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.core.text.presentation.MessageSupport
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.time.Instant
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Maps movement domain results to semantic values. It holds no language and no colour: the
 * presentation layer renders per recipient.
 */
internal object MovementMessages : MessageSupport() {

    internal fun blockPosition(position: StoredPosition): TextArgument =
        TextArgument.Position(
            x = floor(position.x).toInt(),
            y = floor(position.y).toInt(),
            z = floor(position.z).toInt(),
            dimension = position.dimension,
        )

    internal fun ago(from: Instant, now: Instant): Duration =
        (now.toEpochMilli() - from.toEpochMilli()).coerceAtLeast(0L).milliseconds

}

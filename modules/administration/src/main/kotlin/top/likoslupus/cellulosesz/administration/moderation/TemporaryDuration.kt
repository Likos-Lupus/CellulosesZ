package top.likoslupus.cellulosesz.administration.moderation

import java.time.Duration

/** A parsed, positive temporary-punishment duration. */
internal data class TemporaryDuration(
    val duration: Duration,
) {

    val seconds: Long get() = duration.seconds

}

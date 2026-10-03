package top.likoslupus.cellulosesz.foundation.time

import java.time.Duration

/** A parsed, positive duration shared by temporary punishments and temporary mail. */
public data class TemporaryDuration(
    public val duration: Duration,
) {

    public val seconds: Long get() = duration.seconds

}

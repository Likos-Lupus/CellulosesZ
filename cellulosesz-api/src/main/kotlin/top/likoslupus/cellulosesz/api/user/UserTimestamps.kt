package top.likoslupus.cellulosesz.api.user

import top.likoslupus.cellulosesz.api.validation.requireNonNegative

@JvmRecord
public data class UserTimestamps(
    public val firstJoin: Long,
    public val lastJoin: Long,
    public val lastQuit: Long,
    public val playTimeMillis: Long,
    public val lastActivityAt: Long,
    public val activeSessionStartedAt: Long?
) {

    init {
        firstJoin.requireNonNegative { "firstJoin" }
        lastJoin.requireNonNegative { "lastJoin" }
        lastQuit.requireNonNegative { "lastQuit" }
        playTimeMillis.requireNonNegative { "playTimeMillis" }
        lastActivityAt.requireNonNegative { "lastActivityAt" }
        activeSessionStartedAt?.requireNonNegative { "activeSessionStartedAt" }
    }

    public fun withFirstJoin(value: Long): UserTimestamps =
        UserTimestamps(
            value,
            lastJoin,
            lastQuit,
            playTimeMillis,
            lastActivityAt,
            activeSessionStartedAt
        )

    public fun withLastJoin(value: Long): UserTimestamps =
        UserTimestamps(
            firstJoin,
            value,
            lastQuit,
            playTimeMillis,
            lastActivityAt,
            activeSessionStartedAt
        )

    public fun withLastQuit(value: Long): UserTimestamps =
        UserTimestamps(
            firstJoin,
            lastJoin,
            value,
            playTimeMillis,
            lastActivityAt,
            activeSessionStartedAt
        )

    public fun withPlayTimeMillis(value: Long): UserTimestamps =
        UserTimestamps(
            firstJoin,
            lastJoin,
            lastQuit,
            value,
            lastActivityAt,
            activeSessionStartedAt
        )

    public fun withLastActivityAt(value: Long): UserTimestamps =
        UserTimestamps(
            firstJoin,
            lastJoin,
            lastQuit,
            playTimeMillis,
            value,
            activeSessionStartedAt
        )

    public fun withActiveSessionStartedAt(value: Long?): UserTimestamps =
        UserTimestamps(
            firstJoin,
            lastJoin,
            lastQuit,
            playTimeMillis,
            lastActivityAt,
            value
        )

    public companion object {

        @JvmStatic
        public fun defaults(): UserTimestamps =
            UserTimestamps(
                firstJoin = 0,
                lastJoin = 0,
                lastQuit = 0,
                playTimeMillis = 0,
                lastActivityAt = 0,
                activeSessionStartedAt = null
            )

    }

}

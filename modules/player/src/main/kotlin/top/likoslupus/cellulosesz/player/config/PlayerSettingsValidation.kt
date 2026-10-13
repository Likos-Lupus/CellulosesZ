package top.likoslupus.cellulosesz.player.config

import top.likoslupus.cellulosesz.foundation.config.ValidationError

public object PlayerSettingsValidation {

    public fun validate(settings: PlayerSettings): List<ValidationError> =
        buildList {
            val afk = settings.afk
            if (afk.autoAfterSeconds < 1) {
                add(
                    ValidationError(
                        "players.afk.autoAfterSeconds",
                        "must be at least 1"
                    )
                )
            }
            if (afk.checkIntervalTicks < 1) {
                add(
                    ValidationError(
                        "players.afk.checkIntervalTicks",
                        "must be at least 1"
                    )
                )
            }
            if (!afk.movementThreshold.isFinite() || afk.movementThreshold < 0.0) {
                add(
                    ValidationError(
                        "players.afk.movementThreshold",
                        "must be a finite non-negative number"
                    )
                )
            }
            if (!afk.rotationThresholdDegrees.isFinite() || afk.rotationThresholdDegrees < 0.0) {
                add(
                    ValidationError(
                        "players.afk.rotationThresholdDegrees",
                        "must be a finite non-negative number",
                    )
                )
            }

            val sessions = settings.sessions
            if (sessions.heartbeatIntervalSeconds < 1) {
                add(
                    ValidationError(
                        "players.sessions.heartbeatIntervalSeconds",
                        "must be at least 1"
                    )
                )
            }
            if (sessions.maxRetryBufferSize < 1) {
                add(
                    ValidationError(
                        "players.sessions.maxRetryBufferSize",
                        "must be at least 1"
                    )
                )
            }

            val nicknames = settings.nicknames
            if (nicknames.minimumVisibleLength < 1) {
                add(
                    ValidationError(
                        "players.nicknames.minimumVisibleLength",
                        "must be at least 1"
                    )
                )
            }
            if (nicknames.maximumVisibleLength < nicknames.minimumVisibleLength) {
                add(
                    ValidationError(
                        "players.nicknames.maximumVisibleLength",
                        "must not be smaller than minimumVisibleLength",
                    )
                )
            }
            if (nicknames.maximumVisibleLength > MAX_VISIBLE_NICKNAME_LENGTH) {
                add(
                    ValidationError(
                        "players.nicknames.maximumVisibleLength",
                        "must be at most $MAX_VISIBLE_NICKNAME_LENGTH",
                    )
                )
            }

            val nearby = settings.nearby
            if (nearby.defaultRadius < 1) {
                add(
                    ValidationError(
                        "players.nearby.defaultRadius",
                        "must be at least 1"
                    )
                )
            }
            if (nearby.maximumRadius < nearby.defaultRadius) {
                add(
                    ValidationError(
                        "players.nearby.maximumRadius",
                        "must not be smaller than defaultRadius",
                    )
                )
            }
            if (nearby.maximumResults < 1) {
                add(
                    ValidationError(
                        "players.nearby.maximumResults",
                        "must be at least 1"
                    )
                )
            }
        }

    /** Machine safety cap; the stored `normalized_nickname` column is VARCHAR(255). */
    private const val MAX_VISIBLE_NICKNAME_LENGTH: Int = 64

}

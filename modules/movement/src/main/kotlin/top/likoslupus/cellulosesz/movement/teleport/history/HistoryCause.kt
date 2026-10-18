package top.likoslupus.cellulosesz.movement.teleport.history

/**
 * The kind of history row behind `/back`. A player keeps at most one [TELEPORT] row (the location
 * before their last successful teleport) and one [DEATH] row (their last death location, recorded by
 * the application's death event). `/back` prefers the death row when permitted.
 */
internal enum class HistoryCause {

    TELEPORT,
    DEATH,

}

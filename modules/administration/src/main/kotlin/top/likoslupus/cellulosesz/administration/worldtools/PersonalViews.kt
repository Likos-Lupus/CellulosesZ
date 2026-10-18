package top.likoslupus.cellulosesz.administration.worldtools

import java.util.*

/** A player's personal weather override. */
internal enum class PersonalWeather {

    CLEAR,
    RAIN,
    THUNDER;

    companion object {

        fun parse(raw: String): PersonalWeather? =
            when (raw.lowercase()) {
                "clear", "sunny" -> CLEAR
                "rain" -> RAIN
                "thunder" -> THUNDER
                else -> null
            }

    }

}

/** A player's personal time override. [frozen] pauses the clock for that player. */
internal data class PersonalTimeView(
    val ticks: Long,
    val frozen: Boolean,
)

/**
 * Session-scoped personal `/ptime` and `/pweather` overrides. Cleared on quit and re-sent to the
 * client on join and dimension change. Server-thread confined.
 */
internal class PersonalViews {

    private val times = HashMap<UUID, PersonalTimeView>()
    private val weathers = HashMap<UUID, PersonalWeather>()

    fun time(playerId: UUID): PersonalTimeView? =
        times[playerId]

    fun weather(playerId: UUID): PersonalWeather? =
        weathers[playerId]

    fun setTime(playerId: UUID, view: PersonalTimeView) {
        times[playerId] = view
    }

    fun clearTime(playerId: UUID): Boolean =
        times.remove(playerId) != null

    fun setWeather(playerId: UUID, weather: PersonalWeather) {
        weathers[playerId] = weather
    }

    fun clearWeather(playerId: UUID): Boolean =
        weathers.remove(playerId) != null

    fun clear(playerId: UUID) {
        times.remove(playerId)
        weathers.remove(playerId)
    }

    fun clearAll() {
        times.clear()
        weathers.clear()
    }

    fun tracked(): List<UUID> =
        (times.keys + weathers.keys).toList()

}

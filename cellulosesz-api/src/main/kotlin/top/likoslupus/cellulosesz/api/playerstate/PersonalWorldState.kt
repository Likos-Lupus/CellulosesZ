package top.likoslupus.cellulosesz.api.playerstate

/**
 * Typed personal world overrides, independent of the legacy persisted representation.
 */
@JvmRecord
public data class PersonalWorldState(
    public val time: PersonalTimeSetting,
    public val weather: PersonalWeatherSetting
) {

    public companion object {

        @JvmStatic
        public fun reset(): PersonalWorldState =
            PersonalWorldState(
                PersonalTimeSetting.reset(),
                PersonalWeatherSetting.RESET
            )

    }

}

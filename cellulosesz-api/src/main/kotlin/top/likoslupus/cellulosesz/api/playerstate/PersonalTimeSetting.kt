package top.likoslupus.cellulosesz.api.playerstate

public sealed interface PersonalTimeSetting {

    @JvmRecord
    public data class Fixed(
        public val ticks: Long
    ) : PersonalTimeSetting

    @JvmRecord
    public data class Relative(
        public val offset: Long
    ) : PersonalTimeSetting

    public data object Reset : PersonalTimeSetting

    public companion object {

        @JvmStatic
        public fun reset(): PersonalTimeSetting = Reset

    }

}

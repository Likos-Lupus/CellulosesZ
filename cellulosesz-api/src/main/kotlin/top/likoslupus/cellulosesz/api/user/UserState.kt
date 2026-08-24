package top.likoslupus.cellulosesz.api.user

import top.likoslupus.cellulosesz.api.util.toImmutableList
import top.likoslupus.cellulosesz.api.util.toImmutableMap
import top.likoslupus.cellulosesz.api.util.toImmutableSet
import java.util.*

public class UserState(
    @get:JvmName("afk") public val afk: Boolean,
    @get:JvmName("god") public val god: Boolean,
    @get:JvmName("flying") public val flying: Boolean,
    @get:JvmName("vanished") public val vanished: Boolean,
    @get:JvmName("nickname") public val nickname: String?,
    @get:JvmName("personalTime") public val personalTime: Long?,
    @get:JvmName("personalWeather") public val personalWeather: String?,
    powerToolCommands: Map<String, List<String>>,
    unlimitedItems: Set<String>
) {

    @get:JvmName("powerToolCommands") public val powerToolCommands: Map<String, List<String>> =
        defensiveCopyCommands(powerToolCommands)

    @get:JvmName("unlimitedItems") public val unlimitedItems: Set<String> =
        unlimitedItems.toImmutableSet()

    public fun withAfk(value: Boolean): UserState =
        UserState(
            value,
            god,
            flying,
            vanished,
            nickname,
            personalTime,
            personalWeather,
            powerToolCommands,
            unlimitedItems
        )

    public fun withGod(value: Boolean): UserState =
        UserState(
            afk,
            value,
            flying,
            vanished,
            nickname,
            personalTime,
            personalWeather,
            powerToolCommands,
            unlimitedItems
        )

    public fun withFlying(value: Boolean): UserState =
        UserState(
            afk,
            god,
            value,
            vanished,
            nickname,
            personalTime,
            personalWeather,
            powerToolCommands,
            unlimitedItems
        )

    public fun withVanished(value: Boolean): UserState =
        UserState(
            afk,
            god,
            flying,
            value,
            nickname,
            personalTime,
            personalWeather,
            powerToolCommands,
            unlimitedItems
        )

    public fun withNickname(value: String?): UserState =
        UserState(
            afk,
            god,
            flying,
            vanished,
            value,
            personalTime,
            personalWeather,
            powerToolCommands,
            unlimitedItems
        )

    public fun withPersonalTime(value: Long?): UserState =
        UserState(
            afk,
            god,
            flying,
            vanished,
            nickname,
            value,
            personalWeather,
            powerToolCommands,
            unlimitedItems
        )

    public fun withPersonalWeather(value: String?): UserState =
        UserState(
            afk,
            god,
            flying,
            vanished,
            nickname,
            personalTime,
            value,
            powerToolCommands,
            unlimitedItems
        )

    public fun withPowerToolCommands(value: Map<String, List<String>>): UserState =
        UserState(
            afk,
            god,
            flying,
            vanished,
            nickname,
            personalTime,
            personalWeather,
            value,
            unlimitedItems
        )

    public fun withUnlimitedItems(value: Set<String>): UserState =
        UserState(
            afk,
            god,
            flying,
            vanished,
            nickname,
            personalTime,
            personalWeather,
            powerToolCommands,
            value
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserState) return false
        return afk == other.afk &&
                god == other.god &&
                flying == other.flying &&
                vanished == other.vanished &&
                nickname == other.nickname &&
                personalTime == other.personalTime &&
                personalWeather == other.personalWeather &&
                powerToolCommands == other.powerToolCommands &&
                unlimitedItems == other.unlimitedItems
    }

    override fun hashCode(): Int =
        Objects.hash(
            afk,
            god,
            flying,
            vanished,
            nickname,
            personalTime,
            personalWeather,
            powerToolCommands,
            unlimitedItems
        )

    override fun toString(): String =
        "UserState[afk=$afk, god=$god, flying=$flying, vanished=$vanished, nickname=$nickname, personalTime=$personalTime, personalWeather=$personalWeather, powerToolCommands=$powerToolCommands, unlimitedItems=$unlimitedItems]"

    public companion object {

        private fun defensiveCopyCommands(
            commands: Map<String, List<String>>
        ): Map<String, List<String>> {
            val commandCopy = LinkedHashMap<String, List<String>>()
            commands.forEach { (item, cmds) ->
                commandCopy[item] = cmds.toImmutableList()
            }
            return commandCopy.toImmutableMap()
        }

        @JvmStatic
        public fun defaults(): UserState =
            UserState(
                afk = false,
                god = false,
                flying = false,
                vanished = false,
                nickname = null,
                personalTime = null,
                personalWeather = null,
                powerToolCommands = emptyMap(),
                unlimitedItems = emptySet()
            )

    }

}

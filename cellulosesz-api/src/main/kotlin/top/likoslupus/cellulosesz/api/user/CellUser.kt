package top.likoslupus.cellulosesz.api.user

import top.likoslupus.cellulosesz.api.util.toImmutableMap
import java.util.*

public class CellUser(
    @get:JvmName("uuid") public val uuid: UUID,
    @get:JvmName("lastKnownName") public val lastKnownName: String?,
    @get:JvmName("timestamps") public val timestamps: UserTimestamps,
    @get:JvmName("state") public val state: UserState,
    @get:JvmName("preferences") public val preferences: UserPreferences,
    @get:JvmName("relations") public val relations: UserRelations,
    cooldowns: Map<String, Long>
) {

    @get:JvmName("cooldowns") public val cooldowns: Map<String, Long> =
        cooldowns.toImmutableMap()

    public fun withLastKnownName(value: String?): CellUser =
        CellUser(uuid, value, timestamps, state, preferences, relations, cooldowns)

    public fun withTimestamps(value: UserTimestamps): CellUser =
        CellUser(uuid, lastKnownName, value, state, preferences, relations, cooldowns)

    public fun withState(value: UserState): CellUser =
        CellUser(uuid, lastKnownName, timestamps, value, preferences, relations, cooldowns)

    public fun withPreferences(value: UserPreferences): CellUser =
        CellUser(uuid, lastKnownName, timestamps, state, value, relations, cooldowns)

    public fun withRelations(value: UserRelations): CellUser =
        CellUser(uuid, lastKnownName, timestamps, state, preferences, value, cooldowns)

    public fun withCooldowns(value: Map<String, Long>): CellUser =
        CellUser(uuid, lastKnownName, timestamps, state, preferences, relations, value)

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other !is CellUser -> false
            else -> uuid == other.uuid &&
                    lastKnownName == other.lastKnownName &&
                    timestamps == other.timestamps &&
                    state == other.state &&
                    preferences == other.preferences &&
                    relations == other.relations &&
                    cooldowns == other.cooldowns
        }
    }

    override fun hashCode(): Int =
        Objects.hash(
            uuid,
            lastKnownName,
            timestamps,
            state,
            preferences,
            relations,
            cooldowns
        )

    override fun toString(): String =
        "CellUser[uuid=$uuid, lastKnownName=$lastKnownName, timestamps=$timestamps, state=$state, preferences=$preferences, relations=$relations, cooldowns=$cooldowns]"

    public companion object {

        @JvmStatic
        public fun create(uuid: UUID): CellUser =
            CellUser(
                uuid = uuid,
                lastKnownName = null,
                timestamps = UserTimestamps.defaults(),
                state = UserState.defaults(),
                preferences = UserPreferences.defaults(),
                relations = UserRelations.defaults(),
                cooldowns = emptyMap()
            )

    }

}

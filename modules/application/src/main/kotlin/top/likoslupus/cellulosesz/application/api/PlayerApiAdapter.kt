package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.ApiQuery
import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiEventListener
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.player.*
import top.likoslupus.cellulosesz.player.integration.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/** The Player domain adapter. The composition root forwards domain facts through the `dispatch*` methods. */
internal class PlayerApiAdapter(
    private val port: PlayerIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
    preferences: PlayerPreferencesApi,
) : PlayersApi {

    private val sinks = PlayerEventSinks()

    override val presence: PlayerPresenceApi = PlayerPresenceAdapter(port, dispatcher)

    override val nicknames: PlayerNicknameApi = PlayerNicknameAdapter(port, dispatcher)

    override val preferences: PlayerPreferencesApi = preferences

    override val events: PlayerEvents = sinks.events

    fun dispatchAfkChanged(playerId: UUID, afk: Boolean, cause: PlayerAfkCause) =
        sinks.dispatchAfk(PlayerAfkChanged(playerId, afk, cause.toApiCause()))

    fun dispatchNicknameChanged(playerId: UUID, previous: String?, current: String?) =
        sinks.dispatchNickname(PlayerNicknameChanged(playerId, previous, current))

    fun dispatchPresenceChanged(playerId: UUID, online: Boolean) =
        sinks.dispatchPresence(PlayerPresenceChanged(playerId, online))

    fun dispatchPreferenceChanged(event: PlayerPreferenceChanged) =
        sinks.dispatchPreference(event)

    fun clear() = sinks.clear()

}

private class PlayerPresenceAdapter(
    private val port: PlayerIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : PlayerPresenceApi {

    override fun online(playerId: UUID): ApiQuery<Boolean> =
        ApiQuery.Available(port.online(playerId))

    override fun afk(playerId: UUID): ApiQuery<Boolean?> =
        ApiQuery.Available(port.afk(playerId))

    override fun profile(
        playerId: UUID,
        callback: ApiCallback<PlayerProfileOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            when (val result = port.profile(playerId)) {
                is PlayerProfileResult.Found -> ApiCallResult.Completed(
                    PlayerProfileOutcome.Found(result.snapshot.toView(port))
                )

                PlayerProfileResult.Unknown ->
                    ApiCallResult.Completed(PlayerProfileOutcome.Unknown)

                PlayerProfileResult.StorageUnavailable ->
                    ApiCallResult.Unavailable("player.profile")
            }
        },
        deliver = callback::onComplete,
    )

}

private class PlayerNicknameAdapter(
    private val port: PlayerIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : PlayerNicknameApi {

    override fun current(
        playerId: UUID,
        callback: ApiCallback<NicknameQueryOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            when (val result = port.nickname(playerId)) {
                is NicknameResult.Current -> {
                    val name = result.name
                    ApiCallResult.Completed(
                        when (name) {
                            null -> NicknameQueryOutcome.None
                            else -> NicknameQueryOutcome.Found(name)
                        }
                    )
                }

                NicknameResult.Unknown ->
                    ApiCallResult.Completed(NicknameQueryOutcome.Unknown)

                NicknameResult.StorageUnavailable ->
                    ApiCallResult.Completed(NicknameQueryOutcome.StorageUnavailable)
            }
        },
        deliver = callback::onComplete,
    )

    override fun changeOwn(
        actorId: UUID,
        newName: String?,
        callback: ApiCallback<NicknameChangeOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            when (val result = port.changeOwnNickname(actorId, newName)) {
                is NicknameChangeResult.Changed -> {
                    val name = result.name
                    ApiCallResult.Completed(
                        when (name) {
                            null -> NicknameChangeOutcome.Cleared
                            else -> NicknameChangeOutcome.Changed(name)
                        }
                    )
                }

                NicknameChangeResult.Disabled ->
                    ApiCallResult.Completed(NicknameChangeOutcome.Disabled)

                NicknameChangeResult.InvalidName ->
                    ApiCallResult.Completed(NicknameChangeOutcome.InvalidName)

                NicknameChangeResult.NameTaken ->
                    ApiCallResult.Completed(NicknameChangeOutcome.NameTaken)

                NicknameChangeResult.Reserved ->
                    ApiCallResult.Completed(NicknameChangeOutcome.Reserved)

                NicknameChangeResult.StorageUnavailable ->
                    ApiCallResult.Completed(NicknameChangeOutcome.StorageUnavailable)
            }
        },
        deliver = callback::onComplete,
    )

}

private class PlayerEventSinks {

    private val afk = CopyOnWriteArrayList<ApiEventListener<PlayerAfkChanged>>()
    private val nicknames = CopyOnWriteArrayList<ApiEventListener<PlayerNicknameChanged>>()
    private val presence = CopyOnWriteArrayList<ApiEventListener<PlayerPresenceChanged>>()
    private val preferences = CopyOnWriteArrayList<ApiEventListener<PlayerPreferenceChanged>>()

    val events: PlayerEvents = object : PlayerEvents {

        override fun onAfkChanged(listener: ApiEventListener<PlayerAfkChanged>): Subscription =
            afk.subscribe(listener)

        override fun onNicknameChanged(
            listener: ApiEventListener<PlayerNicknameChanged>,
        ): Subscription =
            nicknames.subscribe(listener)

        override fun onPresenceChanged(
            listener: ApiEventListener<PlayerPresenceChanged>,
        ): Subscription =
            presence.subscribe(listener)

        override fun onPreferenceChanged(
            listener: ApiEventListener<PlayerPreferenceChanged>,
        ): Subscription =
            preferences.subscribe(listener)

    }

    fun dispatchAfk(event: PlayerAfkChanged) =
        afk.forEach { runCatching { it.onEvent(event) } }

    fun dispatchNickname(event: PlayerNicknameChanged) =
        nicknames.forEach { runCatching { it.onEvent(event) } }

    fun dispatchPresence(event: PlayerPresenceChanged) =
        presence.forEach { runCatching { it.onEvent(event) } }

    fun dispatchPreference(event: PlayerPreferenceChanged) =
        preferences.forEach { runCatching { it.onEvent(event) } }

    fun clear() {
        afk.clear()
        nicknames.clear()
        presence.clear()
        preferences.clear()
    }

}

private fun <T> CopyOnWriteArrayList<ApiEventListener<T>>.subscribe(
    listener: ApiEventListener<T>,
): Subscription {
    add(listener)
    return Subscription { remove(listener) }
}

private fun PlayerProfileSnapshot.toView(port: PlayerIntegrationPort): PlayerPresenceView =
    PlayerPresenceView(
        playerId = playerId,
        lastKnownName = lastKnownName,
        online = online,
        afk = port.afk(playerId) ?: false,
        nickname = null,
        firstSeen = firstSeen,
        lastSeen = lastSeen,
        playtime = playtime,
    )

private fun PlayerAfkCause.toApiCause(): AfkChangeCause = when (this) {
    PlayerAfkCause.MANUAL -> AfkChangeCause.MANUAL
    PlayerAfkCause.ACTIVITY -> AfkChangeCause.ACTIVITY
    PlayerAfkCause.TIMEOUT -> AfkChangeCause.TIMEOUT
    PlayerAfkCause.OTHER -> AfkChangeCause.OTHER
}

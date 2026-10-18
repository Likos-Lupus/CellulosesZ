package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.player.*
import top.likoslupus.cellulosesz.application.api.extension.ApplicationExtensionHub
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.preference.PreferenceWriteResult
import java.time.Instant
import java.util.*
import top.likoslupus.cellulosesz.core.preference.PlayerPreference as CorePreference

/**
 * Adapts the shared preference store onto the public API. Reads are answered from the in-memory
 * cache; writes go through [PlayerPreferenceService] and publish a committed
 * [top.likoslupus.cellulosesz.api.player.PlayerPreferenceChanged] fact only when the stored value
 * changed. Both paths require the registered addon handle and a live capability grant.
 */
internal class PlayerPreferencesApiAdapter(
    private val preferences: PlayerPreferenceService,
    private val known: KnownPlayerResolver,
    private val hub: ApplicationExtensionHub,
    private val dispatcher: ApiRequestDispatcher,
    private val dispatch: (PlayerPreferenceChanged) -> Unit,
) : PlayerPreferencesApi {

    override fun read(
        access: AddonAccess,
        playerId: UUID,
        callback: ApiCallback<PlayerPreferencesOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            when {
                !hub.isAuthorizedAccess(access, CAP_READ) ->
                    ApiCallResult.Completed(PlayerPreferencesOutcome.NotAuthorized)

                known.knownById(playerId) == null ->
                    ApiCallResult.Completed(PlayerPreferencesOutcome.UnknownPlayer)

                else -> ApiCallResult.Completed(
                    PlayerPreferencesOutcome.Available(view(playerId))
                )
            }
        },
        deliver = callback::onComplete,
    )

    override fun change(
        request: PlayerPreferenceChangeRequest,
        callback: ApiCallback<PlayerPreferenceChangeOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            when {
                !hub.isAuthorizedAccess(request.access, CAP_WRITE) ->
                    ApiCallResult.Completed(PlayerPreferenceChangeOutcome.NotAuthorized)

                known.knownById(request.playerId) == null ->
                    ApiCallResult.Completed(PlayerPreferenceChangeOutcome.UnknownPlayer)

                else -> {
                    when (
                        preferences.set(
                            request.playerId,
                            request.preference.toCore(),
                            request.enabled,
                        )
                    ) {
                        is PreferenceWriteResult.Updated -> {
                            dispatch(
                                PlayerPreferenceChanged(
                                    request.playerId,
                                    request.preference,
                                    request.enabled,
                                    Instant.now(),
                                )
                            )
                            ApiCallResult.Completed(
                                PlayerPreferenceChangeOutcome.Changed(
                                    request.playerId,
                                    request.preference,
                                    request.enabled,
                                )
                            )
                        }

                        is PreferenceWriteResult.Unchanged -> ApiCallResult.Completed(
                            PlayerPreferenceChangeOutcome.Unchanged(
                                request.playerId,
                                request.preference,
                                request.enabled,
                            )
                        )

                        PreferenceWriteResult.StorageUnavailable ->
                            ApiCallResult.Unavailable("player.preferences")
                    }
                }
            }
        },
        deliver = callback::onComplete,
    )

    private fun view(playerId: UUID): PlayerPreferencesView =
        PlayerPreferencesView(
            playerId = playerId,
            teleportEnabled = preferences.isEnabled(
                playerId,
                CorePreference.TELEPORT_ENABLED
            ),
            acceptingPay = preferences.isEnabled(
                playerId,
                CorePreference.ACCEPTING_PAY
            ),
            payConfirmation = preferences.isEnabled(
                playerId,
                CorePreference.PAY_CONFIRMATION
            ),
            replyToLastRecipient = preferences.isEnabled(
                playerId,
                CorePreference.REPLY_TO_LAST_RECIPIENT,
            ),
            clearInventoryConfirmation = preferences.isEnabled(
                playerId,
                CorePreference.CLEAR_INVENTORY_CONFIRMATION,
            ),
        )

    private companion object {

        private const val CAP_READ = "player.preferences.read"
        private const val CAP_WRITE = "player.preferences.write"

    }

}

private fun PlayerPreference.toCore(): CorePreference =
    when (this) {
        PlayerPreference.TELEPORT_ENABLED -> CorePreference.TELEPORT_ENABLED
        PlayerPreference.ACCEPTING_PAY -> CorePreference.ACCEPTING_PAY
        PlayerPreference.PAY_CONFIRMATION -> CorePreference.PAY_CONFIRMATION
        PlayerPreference.REPLY_TO_LAST_RECIPIENT -> CorePreference.REPLY_TO_LAST_RECIPIENT
        PlayerPreference.CLEAR_INVENTORY_CONFIRMATION -> CorePreference.CLEAR_INVENTORY_CONFIRMATION
    }

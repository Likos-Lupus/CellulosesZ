package top.likoslupus.cellulosesz.administration.jail

import com.mojang.logging.LogUtils
import top.likoslupus.cellulosesz.administration.config.JailSettings
import top.likoslupus.cellulosesz.administration.integration.ModerationDomainEvent
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.ModerationReason
import top.likoslupus.cellulosesz.administration.moderation.ModerationState
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditAction
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditService
import top.likoslupus.cellulosesz.administration.moderation.audit.PersistedModerationActor
import top.likoslupus.cellulosesz.administration.moderation.identity.MinecraftAccountResolver
import top.likoslupus.cellulosesz.administration.moderation.protection.ProtectionResult
import top.likoslupus.cellulosesz.administration.moderation.protection.TargetProtectionPolicy
import top.likoslupus.cellulosesz.foundation.time.TemporaryDuration
import java.time.Clock
import java.util.*
import kotlinx.coroutines.CancellationException

/**
 * Owns jail locations, active sentences, and their persistence. Locations and sentences are held in
 * server-thread-confined maps; repositories switch to IO. A sentence is published only after its row
 * is written (fail closed): if the write fails, the player is never moved. A release first marks the
 * row `RELEASE_PENDING`; the confinement tick later performs the return transfer and deletes the row.
 *
 * `[administration.jail].enabled = false` blocks new jails and location changes but never stops the
 * enforcement of existing sentences: confinement, correction and release keep running.
 */
internal class JailService(
    private val runtime: JailRuntime,
    private val locations: JailLocationRepository,
    private val sentences: JailSentenceRepository,
    private val accountResolver: MinecraftAccountResolver,
    private val protection: TargetProtectionPolicy,
    private val audit: ModerationAuditService,
    private val transfer: () -> JailTransferPort,
    private val positions: JailPlayerPositionSource,
    private val settings: () -> JailSettings,
    private val clock: Clock,
    private val events: (ModerationDomainEvent) -> Unit = {},
) {

    private val locationCache = LinkedHashMap<String, JailLocation>()
    private val sentenceCache = HashMap<UUID, JailSentence>()

    private val transfers = JailTransferCoordinator(
        runtime = runtime,
        transfer = transfer,
        sentences = sentences,
        settings = settings,
        clock = clock,
        confined = { playerId -> sentenceCache[playerId] },
        markReleasePending = { sentence, at ->
            sentenceCache[sentence.playerId] = sentence.copy(
                status = JailStatus.RELEASE_PENDING,
                releaseRequestedAt = at,
            )
            publishConfined()
        },
        forget = { playerId ->
            sentenceCache.remove(playerId)
            publishConfined()
        },
    )

    private val ticker = JailOnlineTicker(
        runtime = runtime,
        sentences = sentences,
        sentenceFor = { playerId -> sentenceCache[playerId] },
        onRemainingChanged = { sentence -> sentenceCache[sentence.playerId] = sentence },
        onExpired = transfers::scheduleExpiryRelease,
    )

    @Volatile private var state: ModerationState = ModerationState.LOADING

    fun state(): ModerationState =
        state

    /** Server-thread confined. A release-pending player is still confined until the return commits. */
    fun isConfined(playerId: UUID): Boolean =
        sentenceCache.containsKey(playerId)

    fun locations(): List<JailLocation> =
        locationCache.values.toList()

    fun sentences(): List<JailSentence> =
        sentenceCache.values.toList()

    fun sentence(playerId: UUID): JailSentence? =
        sentenceCache[playerId]

    suspend fun load() {
        val loadedLocations = try {
            locations.loadAll()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to load jail locations; jail is degraded", exception)
            state = ModerationState.DEGRADED
            return
        }

        val loadedSentences = try {
            sentences.loadAll()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to load jail sentences; jail is degraded", exception)
            state = ModerationState.DEGRADED
            return
        }

        runtime.onServerThread {
            locationCache.clear()
            loadedLocations.forEach { locationCache[it.name.value] = it }
            sentenceCache.clear()
            loadedSentences.forEach { sentenceCache[it.playerId] = it }
            publishConfined()
            state = ModerationState.READY
        }
    }

    suspend fun setLocation(
        rawName: String,
        destination: JailDestination,
        createdBy: String?,
    ): SetJailOutcome {
        if (state == ModerationState.STOPPING) {
            return SetJailOutcome.RuntimeStopping
        }
        if (state == ModerationState.LOADING) {
            return SetJailOutcome.StorageUnavailable
        }
        if (!settings().enabled) {
            return SetJailOutcome.Disabled
        }
        val name = JailName.parse(rawName)
            ?: return SetJailOutcome.InvalidName

        val existing = runtime.onServerThread { locationCache[name.value] }
        if (existing == null &&
            runtime.onServerThread { locationCache.size } >= settings().maxJails
        ) {
            return SetJailOutcome.LimitReached
        }

        val location = JailLocation(
            name = name,
            dimension = destination.dimension,
            x = destination.x,
            y = destination.y,
            z = destination.z,
            yaw = destination.yaw,
            pitch = destination.pitch,
            radius = existing?.radius ?: settings().allowedRadius,
            createdAt = existing?.createdAt ?: clock.instant(),
            createdBy = existing?.createdBy ?: createdBy,
        )
        val stored = try {
            locations.put(location)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to persist jail location {}", name.value, exception)
            false
        }

        if (!stored) {
            return SetJailOutcome.StorageUnavailable
        }

        runtime.onServerThread { locationCache[name.value] = location }
        return SetJailOutcome.Saved(name)
    }

    suspend fun deleteLocation(rawName: String): DeleteJailOutcome {
        if (state == ModerationState.STOPPING) {
            return DeleteJailOutcome.RuntimeStopping
        }

        val name = JailName.parse(rawName)
            ?: return DeleteJailOutcome.NotFound
        if (!runtime.onServerThread { locationCache.containsKey(name.value) }) {
            return DeleteJailOutcome.NotFound
        }
        if (runtime.onServerThread { sentenceCache.values.any { it.jailName == name } }) {
            return DeleteJailOutcome.HasSentences
        }

        val removed = try {
            locations.remove(name)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to delete jail location {}", name.value, exception)
            return DeleteJailOutcome.StorageUnavailable
        }

        if (!removed) {
            return DeleteJailOutcome.NotFound
        }

        runtime.onServerThread { locationCache.remove(name.value) }
        return DeleteJailOutcome.Deleted
    }

    suspend fun jail(
        actor: ModerationActor,
        rawTarget: String,
        rawJailName: String,
        duration: TemporaryDuration?,
        reason: ModerationReason,
    ): JailActionOutcome {
        val target = resolve(rawTarget)
            ?: return JailActionOutcome.TargetUnknown
        val jailName = JailName.parse(rawJailName)
            ?: return JailActionOutcome.InvalidJailName
        return jail(actor, target, jailName, duration, reason)
    }

    suspend fun release(actor: ModerationActor, rawTarget: String): JailActionOutcome {
        val target = resolve(rawTarget)
            ?: return JailActionOutcome.TargetUnknown
        return release(actor, target)
    }

    suspend fun toggle(
        actor: ModerationActor,
        rawTarget: String,
        reason: ModerationReason,
    ): JailActionOutcome {
        val target = resolve(rawTarget)
            ?: return JailActionOutcome.TargetUnknown
        return if (runtime.onServerThread { sentenceCache.containsKey(target.id) }) {
            release(actor, target)
        } else {
            if (!settings().enabled) {
                return JailActionOutcome.Disabled
            }
            val jailName = resolveJailName()
                ?: return JailActionOutcome.NoDefaultJail
            jail(actor, target, jailName, duration = null, reason)
        }
    }

    private suspend fun jail(
        actor: ModerationActor,
        target: PlayerIdentity,
        jailName: JailName,
        duration: TemporaryDuration?,
        reason: ModerationReason,
    ): JailActionOutcome {
        if (state == ModerationState.STOPPING) {
            return JailActionOutcome.RuntimeStopping
        }
        if (state == ModerationState.LOADING) {
            return JailActionOutcome.StorageUnavailable
        }
        if (!settings().enabled) {
            return JailActionOutcome.Disabled
        }

        when (runtime.onServerThread { protection.check(actor, target) }) {
            ProtectionResult.SELF -> return JailActionOutcome.SelfTarget
            ProtectionResult.PROTECTED -> return JailActionOutcome.TargetProtected
            ProtectionResult.ALLOWED -> Unit
        }

        val location = runtime.onServerThread { locationCache[jailName.value] }
            ?: return JailActionOutcome.JailUnknown(jailName)
        if (runtime.onServerThread { sentenceCache.containsKey(target.id) }) {
            return JailActionOutcome.AlreadyJailed
        }

        val now = clock.instant()
        val durationSeconds = duration?.seconds
        val mode = JailSentenceMath.modeFor(durationSeconds, settings().jailOnlineTime)
        val expiresAt = when (mode) {
            JailSentenceMode.WALL_CLOCK -> now.plusSeconds(durationSeconds!!)
            else -> null
        }
        val remainingSeconds = when (mode) {
            JailSentenceMode.ONLINE -> durationSeconds
            else -> null
        }
        val returnPosition = runtime.onServerThread { positions.position(target.id) }

        val sentence = JailSentence(
            playerId = target.id,
            playerName = target.name,
            jailName = jailName,
            status = JailStatus.ACTIVE,
            actor = PersistedModerationActor.of(actor),
            reason = reason.value,
            issuedAt = now,
            mode = mode,
            expiresAt = expiresAt,
            remainingSeconds = remainingSeconds,
            returnPosition = returnPosition,
            releaseRequestedAt = null,
        )
        val stored = try {
            sentences.put(sentence)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to persist jail sentence for {}", target.id, exception)
            false
        }

        if (!stored) {
            return JailActionOutcome.StorageUnavailable
        }

        runtime.onServerThread {
            sentenceCache[target.id] = sentence
            publishConfined()
        }

        val recorded = audit.record(
            actor,
            ModerationAuditAction.JAIL,
            target,
            reason.value,
            expiresAt,
        )
        val transferred = transfers.runTransfer(target.id, location, JailTransferKind.CONFINEMENT)
        val actionId = UUID.randomUUID().toString()
        events(ModerationDomainEvent.Confined(target.id, actionId))
        return JailActionOutcome.Jailed(
            target,
            jailName,
            transferred,
            recorded,
            expiresAt,
            remainingSeconds,
            returnPosition != null,
            actionId,
        )
    }

    private suspend fun release(
        actor: ModerationActor,
        target: PlayerIdentity,
    ): JailActionOutcome {
        if (state == ModerationState.STOPPING) {
            return JailActionOutcome.RuntimeStopping
        }
        if (state == ModerationState.LOADING) {
            return JailActionOutcome.StorageUnavailable
        }

        val current = runtime.onServerThread { sentenceCache[target.id] }
            ?: return JailActionOutcome.NotJailed
        if (current.status == JailStatus.RELEASE_PENDING) {
            return JailActionOutcome.ReleaseRequested(target, auditRecorded = false)
        }

        val now = clock.instant()
        val updated = try {
            sentences.updateStatus(
                target.id,
                JailStatus.RELEASE_PENDING,
                now,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to mark jail sentence for release {}", target.id, exception)
            return JailActionOutcome.StorageUnavailable
        }
        if (!updated) {
            return JailActionOutcome.StorageUnavailable
        }

        runtime.onServerThread {
            sentenceCache[target.id] = current.copy(
                status = JailStatus.RELEASE_PENDING,
                releaseRequestedAt = now,
            )
            publishConfined()
        }

        val recorded = audit.record(
            actor,
            ModerationAuditAction.UNJAIL,
            target
        )
        val actionId = UUID.randomUUID().toString()
        events(ModerationDomainEvent.Released(target.id, actionId))
        return JailActionOutcome.ReleaseRequested(target, recorded, actionId)
    }

    /**
     * Server-thread confinement tick. Re-anchors an escaped active player, forces a timed sentence
     * that has expired into release, and drives release-pending players out. Only observes the
     * player named by [playerId]. Runs whether or not the jail capability is enabled.
     */
    fun onObservation(
        playerId: UUID,
        dimension: String,
        x: Double,
        y: Double,
        z: Double,
    ) {
        val sentence = sentenceCache[playerId] ?: return
        when (sentence.status) {
            JailStatus.ACTIVE -> {
                val expiresAt = sentence.expiresAt
                if (expiresAt != null && !clock.instant().isBefore(expiresAt)) {
                    transfers.scheduleExpiryRelease(playerId)
                    return
                }

                val location = locationCache[sentence.jailName.value] ?: return
                val radius = location.radius
                val dx = x - location.x
                val dy = y - location.y
                val dz = z - location.z
                if (dimension != location.dimension
                    || dx * dx + dy * dy + dz * dz > radius * radius
                ) {
                    transfers.scheduleTransfer(
                        playerId,
                        location,
                        JailTransferKind.CORRECTION
                    )
                }
            }

            JailStatus.RELEASE_PENDING -> transfers.scheduleRelease(playerId)
        }
    }

    /**
     * Schedules an immediate confinement transfer for a confined player who has just joined. Called
     * from the join callback, so it must never touch storage inline.
     */
    fun onPlayerJoined(playerId: UUID) {
        val sentence = sentenceCache[playerId] ?: return
        when (sentence.status) {
            JailStatus.ACTIVE -> {
                val location = locationCache[sentence.jailName.value] ?: return
                transfers.scheduleTransfer(playerId, location, JailTransferKind.CONFINEMENT)
            }

            JailStatus.RELEASE_PENDING -> transfers.scheduleRelease(playerId)
        }
    }

    fun shutdown() {
        state = ModerationState.STOPPING
        JailState.clear()
        JailRespawnState.clear()
        ticker.clear()
    }

    /** Server-thread online-time tick; the accounting itself lives in [JailOnlineTicker]. */
    fun onOnlineTick(playerId: UUID) {
        ticker.onOnlineTick(playerId)
    }

    /** Persists the remaining online seconds of a player's sentence and stops tracking them. */
    fun onPlayerQuit(playerId: UUID) {
        ticker.onPlayerQuit(playerId)
    }

    /** Republishes the confined-id snapshot read by the command-restriction mixin. Server thread. */
    private fun publishConfined() {
        JailState.update(sentenceCache.keys)
    }

    private suspend fun resolve(rawTarget: String): PlayerIdentity? =
        runtime.onServerThread { accountResolver.resolve(rawTarget) }

    private fun resolveJailName(): JailName? {
        val configured = settings().defaultJail
        if (configured.isNotBlank()) {
            return JailName.parse(configured)
        }
        return locationCache.keys.firstOrNull()?.let(JailName::parse)
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}

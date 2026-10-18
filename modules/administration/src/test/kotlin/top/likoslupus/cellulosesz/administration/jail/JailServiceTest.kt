package top.likoslupus.cellulosesz.administration.jail

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.administration.config.JailSettings
import top.likoslupus.cellulosesz.administration.config.ModerationSettings
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.ModerationReason
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditFile
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditRepository
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditService
import top.likoslupus.cellulosesz.administration.moderation.identity.MinecraftAccountResolver
import top.likoslupus.cellulosesz.administration.moderation.protection.TargetProtectionPolicy
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.foundation.time.TemporaryDuration
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

class JailServiceTest {

    private val playerId = UUID.randomUUID()
    private val clock = Clock.fixed(
        Instant.ofEpochMilli(10_000),
        ZoneOffset.UTC
    )

    private fun harness(
        timeline: MutableList<String> = mutableListOf(),
        failSentenceWrite: Boolean = false,
        jailSettings: JailSettings = JailSettings(),
    ): Harness {
        val known = object : KnownPlayerResolver {
            private val identity = KnownPlayerIdentity(playerId, "Alice")

            override fun onlineByName(name: String): KnownPlayerIdentity? =
                identity.takeIf { name.equals("Alice", ignoreCase = true) }

            override fun onlineById(id: UUID): KnownPlayerIdentity? =
                identity.takeIf { it.id == id }

            override fun knownByName(name: String): KnownPlayerIdentity? =
                onlineByName(name)

            override fun knownById(id: UUID): KnownPlayerIdentity? =
                onlineById(id)
        }
        val sentences = FakeSentenceRepository(timeline, failSentenceWrite)
        val transfer = FakeTransferPort(timeline)
        val service = JailService(
            runtime = InlineJailRuntime(),
            locations = FakeLocationRepository(),
            sentences = sentences,
            accountResolver = MinecraftAccountResolver(known),
            protection = TargetProtectionPolicy(RuntimeKernel()) { ModerationSettings() },
            audit = ModerationAuditService(
                FakeAuditRepository(),
                { ModerationSettings() },
                clock
            ),
            transfer = { transfer },
            positions = { null },
            settings = { jailSettings },
            clock = clock,
        )
        return Harness(service, sentences, transfer, timeline)
    }

    private fun reason(): ModerationReason =
        checkNotNull(ModerationReason.parse("griefing", 256))

    private val jailDestination = JailDestination(
        dimension = "minecraft:overworld",
        x = 5.0,
        y = 64.0,
        z = 5.0,
        yaw = 0f,
        pitch = 0f,
    )

    @Test
    fun `jail writes the sentence before transferring`() =
        runBlocking {
            val harness = harness()
            harness.service.load()
            assertEquals(
                SetJailOutcome.Saved(
                    checkNotNull(
                        JailName.parse("cells")
                    )
                ),
                harness.service.setLocation(
                    "cells",
                    jailDestination,
                    "Admin"
                )
            )

            val outcome = harness.service.jail(
                ModerationActor.Console,
                "Alice",
                "cells",
                null,
                reason()
            )

            assertTrue(outcome is JailActionOutcome.Jailed)
            assertTrue((outcome as JailActionOutcome.Jailed).transferred)
            val putIndex = harness.timeline.indexOf("sentence.put")
            val transferIndex = harness.timeline.indexOf("transfer")
            assertTrue(
                putIndex >= 0,
                "sentence must be written"
            )
            assertTrue(
                transferIndex >= 0,
                "transfer must run"
            )
            assertTrue(
                putIndex < transferIndex,
                "sentence must be written before the transfer"
            )
            assertTrue(harness.service.isConfined(playerId))
        }

    @Test
    fun `jail fails closed when the sentence cannot be written`() =
        runBlocking {
            val harness = harness(failSentenceWrite = true)
            harness.service.load()
            harness.service.setLocation(
                "cells",
                jailDestination,
                "Admin"
            )

            val outcome = harness.service.jail(
                ModerationActor.Console,
                "Alice",
                "cells",
                null,
                reason()
            )

            assertEquals(
                JailActionOutcome.StorageUnavailable,
                outcome
            )
            assertFalse(
                harness.timeline.contains("transfer"),
                "no transfer may run when the write fails"
            )
            assertFalse(harness.service.isConfined(playerId))
        }

    @Test
    fun `release marks the sentence and the tick removes it`() =
        runBlocking {
            val harness = harness()
            harness.service.load()
            harness.service.setLocation(
                "cells",
                jailDestination,
                "Admin"
            )
            harness.service.jail(
                ModerationActor.Console,
                "Alice",
                "cells",
                null,
                reason()
            )

            val released = harness.service.release(
                ModerationActor.Console,
                "Alice"
            )
            assertTrue(released is JailActionOutcome.ReleaseRequested)
            assertEquals(
                JailStatus.RELEASE_PENDING,
                harness.service.sentence(playerId)?.status
            )
            assertTrue(harness.service.isConfined(playerId))

            harness.service.onObservation(
                playerId,
                "minecraft:overworld",
                100.0,
                64.0,
                100.0
            )

            assertFalse(harness.service.isConfined(playerId))
            assertTrue(harness.timeline.contains("release"))
            assertTrue(harness.timeline.contains("sentence.remove"))
        }

    @Test
    fun `toggle jails a free player and releases a jailed one`() =
        runBlocking {
            val harness = harness()
            harness.service.load()
            harness.service.setLocation(
                "cells",
                jailDestination,
                "Admin"
            )

            val jailed = harness.service.toggle(
                ModerationActor.Console,
                "Alice",
                reason()
            )
            assertTrue(jailed is JailActionOutcome.Jailed)

            val released = harness.service.toggle(
                ModerationActor.Console,
                "Alice",
                reason()
            )
            assertTrue(released is JailActionOutcome.ReleaseRequested)
        }

    @Test
    fun `an active sentence re-anchors a player that left the radius`() =
        runBlocking {
            val harness = harness()
            harness.service.load()
            harness.service.setLocation(
                "cells",
                jailDestination,
                "Admin"
            )
            harness.service.jail(
                ModerationActor.Console,
                "Alice",
                "cells",
                null,
                reason()
            )
            val transferCallsBefore = harness.transfer.log.size

            harness.service.onObservation(
                playerId,
                "minecraft:overworld",
                100.0,
                64.0,
                100.0
            )

            assertTrue(harness.transfer.log.size > transferCallsBefore)
            assertTrue(harness.service.isConfined(playerId))
        }

    @Test
    fun `online jail time counts only online ticks`() =
        runBlocking {
            val harness = harness(jailSettings = JailSettings(jailOnlineTime = true))
            harness.service.load()
            harness.service.setLocation(
                "cells",
                jailDestination,
                "Admin"
            )
            val outcome = harness.service.jail(
                ModerationActor.Console,
                "Alice",
                "cells",
                TemporaryDuration(Duration.ofSeconds(60)),
                reason(),
            )
            assertTrue(outcome is JailActionOutcome.Jailed)
            val sentence = harness.service.sentence(playerId)
            assertNotNull(sentence)
            assertEquals(JailSentenceMode.ONLINE, sentence?.mode)
            assertEquals(60L, sentence?.remainingSeconds)
            assertNull(sentence?.expiresAt)

            repeat(20) { harness.service.onOnlineTick(playerId) }

            assertEquals(
                59L,
                harness.service.sentence(playerId)?.remainingSeconds
            )
        }

    @Test
    fun `wall-clock jail time does not count online ticks`() =
        runBlocking {
            val harness = harness()
            harness.service.load()
            harness.service.setLocation(
                "cells",
                jailDestination,
                "Admin"
            )
            harness.service.jail(
                ModerationActor.Console,
                "Alice",
                "cells",
                TemporaryDuration(Duration.ofSeconds(60)),
                reason(),
            )
            val sentence = harness.service.sentence(playerId)
            assertEquals(JailSentenceMode.WALL_CLOCK, sentence?.mode)
            assertNull(sentence?.remainingSeconds)

            repeat(40) { harness.service.onOnlineTick(playerId) }

            assertEquals(
                JailSentenceMode.WALL_CLOCK,
                harness.service.sentence(playerId)?.mode,
            )
        }

    private data class Harness(
        val service: JailService,
        val sentences: FakeSentenceRepository,
        val transfer: FakeTransferPort,
        val timeline: MutableList<String>,
    )

    private class InlineJailRuntime : JailRuntime {

        override suspend fun <T> onServerThread(block: () -> T): T =
            block()

        override fun launch(block: suspend CoroutineScope.() -> Unit) {
            runBlocking { block(this) }
        }

    }

    private class FakeLocationRepository : JailLocationRepository {

        private val stored = LinkedHashMap<String, JailLocation>()

        override suspend fun loadAll(): List<JailLocation> =
            stored.values.toList()

        override suspend fun put(location: JailLocation) {
            stored[location.name.value] = location
        }

        override suspend fun remove(name: JailName): Boolean =
            stored.remove(name.value) != null

    }

    private class FakeSentenceRepository(
        private val timeline: MutableList<String>,
        private val failWrite: Boolean,
    ) : JailSentenceRepository {

        private val stored = LinkedHashMap<UUID, JailSentence>()

        override suspend fun loadAll(): List<JailSentence> =
            stored.values.toList()

        override suspend fun put(sentence: JailSentence) {
            timeline += "sentence.put"
            if (failWrite) {
                throw JailDataException("write refused")
            }
            stored[sentence.playerId] = sentence
        }

        override suspend fun updateStatus(
            playerId: UUID,
            status: JailStatus,
            releaseRequestedAt: Instant?,
        ): Boolean {
            timeline += "sentence.updateStatus"
            val current = stored[playerId] ?: return false
            stored[playerId] = current.copy(
                status = status,
                releaseRequestedAt = releaseRequestedAt,
            )
            return true
        }

        override suspend fun remove(playerId: UUID): Boolean {
            timeline += "sentence.remove"
            return stored.remove(playerId) != null
        }

        override suspend fun updateRemaining(
            playerId: UUID,
            remainingSeconds: Long
        ): Boolean {
            val current = stored[playerId] ?: return false
            stored[playerId] = current.copy(remainingSeconds = remainingSeconds)
            return true
        }

    }

    private class FakeTransferPort(
        private val timeline: MutableList<String>,
    ) : JailTransferPort {

        val log = mutableListOf<String>()

        override suspend fun transfer(
            playerId: UUID,
            destination: JailDestination,
            kind: JailTransferKind,
        ): JailTransferResult {
            timeline += "transfer"
            log += "transfer"
            return JailTransferResult.Transferred
        }

        override suspend fun release(playerId: UUID): JailTransferResult {
            timeline += "release"
            log += "release"
            return JailTransferResult.Transferred
        }

    }

    private class FakeAuditRepository : ModerationAuditRepository {

        override suspend fun append(entry: ModerationAuditFile) =
            Unit

    }

}

package top.likoslupus.cellulosesz.administration.moderation.mute

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.administration.moderation.audit.PersistedModerationActor
import java.time.Instant
import java.util.*

class MuteFileTest {

    @Test
    fun `round trips an active mute`() {
        val mute = Mute(
            playerId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
            playerName = "Alice",
            actor = PersistedModerationActor(
                "player",
                "22222222-2222-2222-2222-222222222222",
                "Bob"
            ),
            reason = "spam",
            issuedAt = Instant.ofEpochMilli(1_000L),
            expiresAt = Instant.ofEpochMilli(5_000L),
        )

        assertEquals(
            mute,
            mute.toFile().toDomain()
        )
    }

    @Test
    fun `round trips a permanent mute`() {
        val mute = Mute(
            playerId = UUID.randomUUID(),
            playerName = "Alice",
            actor = PersistedModerationActor(
                "console",
                null,
                "console"
            ),
            reason = "spam",
            issuedAt = Instant.ofEpochMilli(1_000L),
            expiresAt = null,
        )

        val decoded = mute.toFile().toDomain()
        assertEquals(mute, decoded)
        assertNull(decoded.expiresAt)
    }

}

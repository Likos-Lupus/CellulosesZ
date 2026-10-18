package top.likoslupus.cellulosesz.player.list

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class PlayerListGrouperTest {

    private val owner = UUID.randomUUID()
    private val builder = UUID.randomUUID()
    private val loner = UUID.randomUUID()

    private val groups = listOf(
        PlayerListGroupSpec(
            "owner",
            "Owner",
            "cellulosesz.group.owner"
        ),
        PlayerListGroupSpec(
            "builder",
            "Builder",
            "cellulosesz.group.builder"
        ),
    )
    private val default = PlayerListGroupSpec(
        "default",
        "Default",
        null
    )

    private val holds = mapOf(
        owner to setOf("cellulosesz.group.owner"),
        builder to setOf("cellulosesz.group.builder"),
        loner to emptySet(),
    )

    private fun candidates() = listOf(
        PlayerListCandidate(owner, "Zoe", afk = false),
        PlayerListCandidate(builder, "bob", afk = true),
        PlayerListCandidate(loner, "Ann", afk = false),
    )

    @Test
    fun `assigns the first matching group and falls back to the default`() {
        val entries = group()

        assertEquals(
            listOf(
                "owner" to "Zoe",
                "builder" to "bob",
                "default" to "Ann"
            ),
            entries.map { it.groupId to it.name },
        )
        assertEquals("Default", entries.last().groupLabel)
    }

    @Test
    fun `higher groups win when a player matches several`() {
        val shared = UUID.randomUUID()
        val both = mapOf(
            shared to setOf(
                "cellulosesz.group.owner",
                "cellulosesz.group.builder"
            )
        )

        val entries = PlayerListGrouper.group(
            candidates = listOf(
                PlayerListCandidate(
                    shared,
                    "Both",
                    afk = false
                )
            ),
            groups = groups,
            defaultGroup = default,
            filterGroupId = null,
            holds = { id, permission -> both[id]?.contains(permission) == true },
        )

        assertEquals("owner", entries.single().groupId)
    }

    @Test
    fun `filters to a requested group`() {
        val entries = group(filterGroupId = "builder")

        assertEquals(listOf("bob"), entries.map { it.name })
        assertEquals(true, entries.single().afk)
    }

    @Test
    fun `orders by group then by name within a group`() {
        val alpha = UUID.randomUUID()
        val zeta = UUID.randomUUID()
        val shared = mapOf<UUID, Set<String>>(
            alpha to emptySet(),
            zeta to emptySet()
        )

        val entries = PlayerListGrouper.group(
            candidates = listOf(
                PlayerListCandidate(zeta, "Zeta", afk = false),
                PlayerListCandidate(alpha, "Alpha", afk = false),
            ),
            groups = emptyList(),
            defaultGroup = default,
            filterGroupId = null,
            holds = { id, permission -> shared[id]?.contains(permission) == true },
        )

        assertEquals(
            listOf("Alpha", "Zeta"),
            entries.map { it.name }
        )
    }

    @Test
    fun `does not duplicate a default group that is already configured`() {
        val ordered = PlayerListGrouper.orderedGroups(
            groups,
            groups.first()
        )

        assertEquals(groups, ordered)
    }

    private fun group(filterGroupId: String? = null): List<PlayerListEntry> =
        PlayerListGrouper.group(
            candidates = candidates(),
            groups = groups,
            defaultGroup = default,
            filterGroupId = filterGroupId,
            holds = { id, permission -> holds[id]?.contains(permission) == true },
        )

}

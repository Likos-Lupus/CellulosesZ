package top.likoslupus.cellulosesz.communication.lifecycle

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.ParsingException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.communication.config.JoinQuitSettings

class JoinQuitRendererTest {

    @Test
    fun `resolve replaces known tokens and keeps unknown ones`() {
        assertEquals(
            "Alice from minecraft:overworld",
            JoinQuitRenderer.resolve(
                "{PLAYER} from {WORLD}",
                mapOf(
                    "PLAYER" to "Alice",
                    "WORLD" to "minecraft:overworld"
                ),
            ),
        )
        assertEquals(
            "{PREFIX} Alice",
            JoinQuitRenderer.resolve(
                "{PREFIX} {PLAYER}",
                mapOf("PLAYER" to "Alice")
            ),
        )
    }

    @Test
    fun `selects the first join and username change templates`() {
        val settings = JoinQuitSettings(
            joinFormat = "join",
            firstJoinFormat = "first",
            newUsernameFormat = "renamed",
        )

        assertEquals(
            "first",
            JoinQuitRenderer.selectJoinTemplate(
                settings,
                firstJoin = true,
                newUsername = true
            ),
        )
        assertEquals(
            "renamed",
            JoinQuitRenderer.selectJoinTemplate(
                settings,
                firstJoin = false,
                newUsername = true
            ),
        )
        assertEquals(
            "join",
            JoinQuitRenderer.selectJoinTemplate(
                settings,
                firstJoin = false,
                newUsername = false
            ),
        )
    }

    @Test
    fun `hides when the online count exceeds the threshold`() {
        assertFalse(JoinQuitRenderer.hideAbovePlayerCount(10, 0))
        assertFalse(JoinQuitRenderer.hideAbovePlayerCount(10, 10))
        assertTrue(JoinQuitRenderer.hideAbovePlayerCount(11, 10))
    }

    @Test
    fun `renders a strict template with styling`() {
        val component = JoinQuitRenderer.parse(
            JoinQuitRenderer.resolve(
                "<yellow>{PLAYER}</yellow> joined",
                mapOf("PLAYER" to "Alice"),
            )
        )

        assertEquals(
            Component.text()
                    .append(Component.text("Alice", NamedTextColor.YELLOW))
                    .append(Component.text(" joined"))
                    .build(),
            component,
        )
    }

    @Test
    fun `escapes player-controlled values so they cannot inject tags`() {
        val escaped = JoinQuitRenderer.escape("<red>evil")
        val component = JoinQuitRenderer.parse(
            JoinQuitRenderer.resolve(
                "{PLAYER}",
                mapOf("PLAYER" to escaped)
            )
        )

        assertEquals(
            Component.text("<red>evil"),
            component
        )
    }

    @Test
    fun `rejects a malformed template`() {
        assertThrows(ParsingException::class.java) {
            JoinQuitRenderer.parse("<yellow>oops")
        }
    }

}

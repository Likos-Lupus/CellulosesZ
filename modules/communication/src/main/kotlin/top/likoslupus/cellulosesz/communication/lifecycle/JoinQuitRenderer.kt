package top.likoslupus.cellulosesz.communication.lifecycle

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import top.likoslupus.cellulosesz.communication.config.JoinQuitSettings

/**
 * Pure rendering rules for lifecycle announcements. The [MiniMessage] instance is strict: a
 * malformed admin template fails at config-load/reload time rather than being silently accepted.
 * Substituted values are escaped before parsing, so a nickname or username can never inject tags.
 */
internal object JoinQuitRenderer {

    private val TOKEN: Regex = Regex("\\{([A-Z_]+)}")

    /** The strict parser shared by the service and by config validation. */
    val parser: MiniMessage = MiniMessage.builder().strict(true).build()

    /** Picks the template for a join, preferring the first-join and username-change variants. */
    fun selectJoinTemplate(
        settings: JoinQuitSettings,
        firstJoin: Boolean,
        newUsername: Boolean,
    ): String =
        when {
            firstJoin -> settings.firstJoinFormat
            newUsername -> settings.newUsernameFormat
            else -> settings.joinFormat
        }

    /** True when the online count exceeds the configured suppression threshold. 0 never hides. */
    fun hideAbovePlayerCount(
        onlineCount: Int,
        hideAbove: Int,
    ): Boolean =
        hideAbove in 1..<onlineCount

    /** Replaces every `{TOKEN}` with its value, leaving unknown tokens untouched. */
    fun resolve(
        template: String,
        values: Map<String, String>,
    ): String =
        TOKEN.replace(template) {
            values[it.groupValues[1]]
                ?: it.value
        }

    /** Escapes MiniMessage tags in a player-controlled value so it cannot inject formatting. */
    fun escape(value: String): String =
        parser.escapeTags(value)

    /** Strictly parses a resolved template. Throws on a malformed tag. */
    fun parse(template: String): Component =
        parser.deserialize(template)

}

package top.likoslupus.cellulosesz.core.text.model

/**
 * A constrained, safe interaction attached to a notice or row. There is deliberately no
 * `RunCommand(String)`: only well-known intents can produce a runnable command, and every intent is
 * re-validated by the owning domain service when it executes.
 */
public sealed interface UiAction {

    public val label: LocalizedText

    /** Fills the chat input; the player still confirms by pressing enter. The safe default. */
    public data class SuggestCommand(
        override val label: LocalizedText,
        public val command: String,
    ) : UiAction

    /** Read-only documentation navigation generated from a validated DSL command path. */
    public data class HelpPage(
        override val label: LocalizedText,
        public val commandPath: String,
        public val page: Int,
    ) : UiAction

    /** Accept/deny a teleport request. The renderer builds a fixed `/tpaccept`/`/tpdeny` command. */
    public data class TeleportDecision(
        override val label: LocalizedText,
        public val senderName: String,
        public val accept: Boolean,
    ) : UiAction

    /** Opens a validated HTTPS URL. */
    public data class OpenHttps(
        override val label: LocalizedText,
        public val url: String,
    ) : UiAction

}

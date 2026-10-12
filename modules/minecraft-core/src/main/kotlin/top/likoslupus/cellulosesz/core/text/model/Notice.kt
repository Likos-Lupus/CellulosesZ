package top.likoslupus.cellulosesz.core.text.model

/** The semantic tone of a notice. Tone is independent from priority and display channel. */
public enum class NoticeTone {

    NEUTRAL,
    SUCCESS,
    INFO,
    WARNING,
    ERROR,

}

/**
 * How important delivery is. [HIGH]/[CRITICAL] notices always go to chat even when the caller would
 * otherwise use the action bar; an error is not automatically critical and a low notice is not dropped.
 */
public enum class NoticePriority {

    LOW,
    NORMAL,
    HIGH,
    CRITICAL,

}

/**
 * A single player-facing result. The default is a one-line summary; [detail] adds a second line only
 * when it genuinely helps. [actions] are optional, already-safe interactions rendered on one line.
 */
public data class Notice(
    public val text: LocalizedText,
    public val tone: NoticeTone = NoticeTone.NEUTRAL,
    public val detail: LocalizedText? = null,
    public val actions: List<UiAction> = emptyList(),
    public val priority: NoticePriority = NoticePriority.NORMAL,
)

package top.likoslupus.cellulosesz.core.text.model

import top.likoslupus.cellulosesz.core.text.MessageKey

/**
 * A locale-independent description of user-visible text: a translation key plus typed display
 * arguments. It carries no language and no colour; rendering happens once per recipient so a single
 * value can be delivered to players with different language preferences.
 */
public data class LocalizedText(
    public val key: MessageKey,
    public val arguments: List<TextArgument> = emptyList(),
)

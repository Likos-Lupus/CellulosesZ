package top.likoslupus.cellulosesz.core.text.presentation

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import kotlin.math.floor
import kotlin.time.Duration

/**
 * The shared vocabulary of a per-feature message catalog: tonal notices and the typed argument
 * constructors every catalog uses. Feature catalogs extend it instead of re-declaring the same
 * eight helpers, so the meaning of a tone or an argument kind is defined exactly once.
 *
 * The helpers are public so a catalog may also be extended from a separate file (a catalog split by
 * feature keeps its call sites as `FeatureMessages.someResult(...)` and only its implementation moves).
 */
public abstract class MessageSupport {

    public fun notice(
        key: MessageKey,
        vararg arguments: TextArgument,
    ): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.ERROR
        )

    public fun error(
        key: MessageKey,
        vararg arguments: TextArgument,
    ): Notice =
        notice(key, *arguments)

    public fun success(
        key: MessageKey,
        vararg arguments: TextArgument,
    ): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.SUCCESS
        )

    public fun warning(
        key: MessageKey,
        vararg arguments: TextArgument,
    ): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.WARNING
        )

    public fun info(
        key: MessageKey,
        vararg arguments: TextArgument,
    ): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.INFO
        )

    public fun literal(value: String): TextArgument =
        TextArgument.Literal(value)

    public fun identifier(value: String): TextArgument =
        TextArgument.Identifier(value)

    public fun playerName(value: String): TextArgument =
        TextArgument.PlayerName(value)

    public fun command(value: String): TextArgument =
        TextArgument.Command(value)

    public fun count(value: Int): TextArgument =
        TextArgument.Count(value.toLong())

    public fun count(value: Long): TextArgument =
        TextArgument.Count(value)

    public fun count(value: Double): TextArgument =
        TextArgument.Count(value.toLong())

    public fun elapsed(value: Duration): TextArgument =
        TextArgument.Elapsed(value)

    public fun position(
        x: Int,
        y: Int,
        z: Int,
        dimension: String? = null,
    ): TextArgument =
        TextArgument.Position(
            x,
            y,
            z,
            dimension
        )

    public fun position(
        x: Double,
        y: Double,
        z: Double,
        dimension: String? = null,
    ): TextArgument =
        TextArgument.Position(
            x = floor(x).toInt(),
            y = floor(y).toInt(),
            z = floor(z).toInt(),
            dimension = dimension,
        )

    public fun field(label: MessageKey, value: LocalizedText): DetailField =
        DetailField(
            LocalizedText(label),
            value
        )

    /** A value-only localized text, for places that need a fragment rather than a whole notice. */
    public fun bare(vararg arguments: TextArgument): LocalizedText =
        LocalizedText(
            MessageKey("core.value"),
            arguments.toList()
        )

}

package top.likoslupus.cellulosesz.core.text.i18n.template

/** Result of compiling a raw translation. Failures are typed values, not control-flow exceptions. */
internal sealed interface TemplateParseResult {

    data class Success(val template: MessageTemplate) : TemplateParseResult

    data class Failure(
        val offset: Int,
        val reason: String,
    ) : TemplateParseResult

}

/**
 * A tiny single-pass compiler for the ordinary-message template language:
 *
 * ```text
 * plain text        -> PRIMARY
 * [[marked text]]   -> SECONDARY
 * {0}, {1}, ...     -> positional argument
 * ```
 *
 * Escapes: `\[` `\]` `\{` `\}` `\\`. `[[` must open a secondary span and `]]` must close it; the
 * template must finish in PRIMARY. This is not Markdown and must never be used for command-document
 * resources.
 */
internal object MessageTemplateParser {

    fun parse(source: String): TemplateParseResult {
        val parts = mutableListOf<MessagePart>()
        val indices = sortedSetOf<Int>()
        val buffer = StringBuilder()
        var role = MessageRole.PRIMARY
        var index = 0

        fun flushText() {
            if (buffer.isNotEmpty()) {
                parts += MessagePart.Text(buffer.toString(), role)
                buffer.clear()
            }
        }

        while (index < source.length) {
            when (val char = source[index]) {
                '\\' -> {
                    if (index + 1 >= source.length) {
                        return TemplateParseResult.Failure(
                            index,
                            "dangling escape at end of template"
                        )
                    }
                    val literal = when (val next = source[index + 1]) {
                        '[', ']', '{', '}', '\\' -> next
                        else -> return TemplateParseResult.Failure(
                            index + 1,
                            "invalid escape \\$next"
                        )
                    }
                    buffer.append(literal)
                    index += 2
                }

                '[' if index + 1 < source.length && source[index + 1] == '[' -> {
                    if (role != MessageRole.PRIMARY) {
                        return TemplateParseResult.Failure(
                            index,
                            "unexpected '[[' outside primary text"
                        )
                    }
                    flushText()
                    role = MessageRole.SECONDARY
                    index += 2
                }

                ']' if index + 1 < source.length && source[index + 1] == ']' -> {
                    if (role != MessageRole.SECONDARY) {
                        return TemplateParseResult.Failure(
                            index,
                            "unexpected ']]' outside a secondary span"
                        )
                    }
                    flushText()
                    role = MessageRole.PRIMARY
                    index += 2
                }

                '{' -> {
                    val close = source.indexOf('}', index + 1)
                    if (close < 0) {
                        return TemplateParseResult.Failure(
                            index,
                            "unterminated argument placeholder"
                        )
                    }
                    val body = source.substring(index + 1, close)
                    if (body.isEmpty() || !body.all { it in '0'..'9' }) {
                        return TemplateParseResult.Failure(
                            index,
                            "invalid argument placeholder {$body}"
                        )
                    }
                    val argumentIndex = body.toIntOrNull()
                        ?: return TemplateParseResult.Failure(
                            index,
                            "invalid argument placeholder {$body}"
                        )
                    flushText()
                    parts += MessagePart.Argument(argumentIndex, role)
                    indices += argumentIndex
                    index = close + 1
                }

                else -> {
                    buffer.append(char)
                    index++
                }
            }
        }

        if (role != MessageRole.PRIMARY) {
            return TemplateParseResult.Failure(
                source.length,
                "'[[' has no matching ']]'"
            )
        }

        flushText()
        return TemplateParseResult.Success(
            MessageTemplate(
                parts,
                indices
            )
        )
    }

}

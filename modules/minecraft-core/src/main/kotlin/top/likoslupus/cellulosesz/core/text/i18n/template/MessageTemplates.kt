package top.likoslupus.cellulosesz.core.text.i18n.template

/**
 * Compiles a runtime-supplied (e.g. addon-registered) translation template. Returns `null` for a
 * malformed template so callers treat it as a typed rejection rather than an exception.
 */
public fun compileMessageTemplate(source: String): MessageTemplate? =
    when (val parsed = MessageTemplateParser.parse(source)) {
        is TemplateParseResult.Success -> parsed.template
        is TemplateParseResult.Failure -> null
    }

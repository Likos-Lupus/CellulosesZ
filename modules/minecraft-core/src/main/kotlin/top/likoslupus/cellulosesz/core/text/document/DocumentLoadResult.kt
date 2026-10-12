package top.likoslupus.cellulosesz.core.text.document

/**
 * The result of loading a bundled command document. It separates "not present" from "present but
 * unparseable" so a packaged-document defect is never mistaken for a missing document.
 */
public sealed interface DocumentLoadResult {

    public data class Found(public val document: MarkdownDocument) : DocumentLoadResult

    public data object Missing : DocumentLoadResult

    public data class Invalid(public val reason: String) : DocumentLoadResult

}

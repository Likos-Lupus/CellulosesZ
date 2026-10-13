package top.likoslupus.cellulosesz.communication.page

import top.likoslupus.cellulosesz.core.text.document.MarkdownDocument

/** The outcome of loading a server info page for one language. Expected failures are typed. */
internal sealed interface ServerPageResult {

    /** The page parsed successfully and is ready to render. */
    data class Found(val document: MarkdownDocument) : ServerPageResult

    /** Server pages are disabled in config. */
    data object Disabled : ServerPageResult

    /** The requested name is not a safe page identifier. */
    data object InvalidName : ServerPageResult

    /** No file exists for the requested name in any supported language. */
    data object NoSuchPage : ServerPageResult

    /** The configured pages directory does not exist yet. */
    data object RootMissing : ServerPageResult

    /** The page file exceeds `maxPageBytes`. */
    data object TooLarge : ServerPageResult

    /** More page files exist than `maxPages` allows. */
    data object TooManyPages : ServerPageResult

    /** The file exists but is not valid Markdown for the document renderer. */
    data object Invalid : ServerPageResult

}

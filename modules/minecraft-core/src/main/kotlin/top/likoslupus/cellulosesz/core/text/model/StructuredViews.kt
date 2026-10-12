package top.likoslupus.cellulosesz.core.text.model

/** Validated page coordinates for a paginated list. */
public data class PageInfo(
    public val index: Int,
    public val count: Int,
) {

    init {
        require(count >= 1) { "page count must be at least 1" }
        require(index in 1..count) { "page index $index out of 1..$count" }
    }

}

/** One row of a [ListPage]. */
public data class ListRow(
    public val main: LocalizedText,
    public val supporting: LocalizedText? = null,
    public val action: UiAction? = null,
)

/** A paginated list of rows with a localized title and empty state. */
public data class ListPage(
    public val title: LocalizedText,
    public val rows: List<ListRow>,
    public val emptyText: LocalizedText,
    public val page: PageInfo? = null,
    public val summary: LocalizedText? = null,
    public val previous: UiAction? = null,
    public val next: UiAction? = null,
)

/** One labelled field of a [DetailView]. */
public data class DetailField(
    public val label: LocalizedText,
    public val value: LocalizedText,
)

/** A short labelled detail block, used for status and info output. */
public data class DetailView(
    public val title: LocalizedText,
    public val fields: List<DetailField>,
    public val footer: LocalizedText? = null,
)

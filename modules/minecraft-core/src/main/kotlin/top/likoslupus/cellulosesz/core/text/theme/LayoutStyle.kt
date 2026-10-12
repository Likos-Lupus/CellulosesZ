package top.likoslupus.cellulosesz.core.text.theme

/** Vertical layout policy derived from [TextDensity]: spacing only, never semantics or wording. */
public data class LayoutStyle(
    public val sectionSpacing: Boolean,
)

public fun TextDensity.toLayoutStyle(): LayoutStyle =
    when (this) {
        TextDensity.COMFORTABLE -> LayoutStyle(sectionSpacing = true)
        TextDensity.COMPACT -> LayoutStyle(sectionSpacing = false)
    }

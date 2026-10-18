package top.likoslupus.cellulosesz.administration.config

import kotlinx.serialization.Serializable

/**
 * Policy for `/editsign`. Editing is front-only by default, text-only (click/hover events are always
 * stripped) and colour codes require `EDIT_SIGN_COLORS`.
 */
@Serializable
public data class SignEditingSettings(
    public val enabled: Boolean = true,
    /** Maximum number of characters per sign line. Longer input is rejected, not truncated. */
    public val maxLineLength: Int = 384,
    /** Whether `&`/`§` colour codes may appear in edited text at all. */
    public val allowColors: Boolean = true,
    /** Whether the back of the sign may be edited. */
    public val allowBack: Boolean = false,
    /** Maximum distance, in blocks, the editor may stand from the target sign. */
    public val maxDistance: Double = 6.0,
)

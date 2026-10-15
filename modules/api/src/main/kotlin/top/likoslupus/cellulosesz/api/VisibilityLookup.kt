package top.likoslupus.cellulosesz.api

import java.util.*

/**
 * Read-only visibility query. It answers CellulosesZ's own rules (for example vanish) for a specific
 * viewer+target pair; it is never a global "who is hidden" list, and it does not describe what other
 * mods might render.
 */
public fun interface VisibilityLookup {

    public fun canSee(viewerId: UUID, targetId: UUID): ApiQuery<Boolean>

}

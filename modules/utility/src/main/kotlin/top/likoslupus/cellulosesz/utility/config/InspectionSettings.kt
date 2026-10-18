package top.likoslupus.cellulosesz.utility.config

import kotlinx.serialization.Serializable

/**
 * Product policy for container inspection. `/invsee` is read-only unless [editable] is enabled, in
 * which case a viewer with the edit permission manipulates the target's real inventory.
 */
@Serializable
public data class InspectionSettings(
    public val editable: Boolean = true,
)

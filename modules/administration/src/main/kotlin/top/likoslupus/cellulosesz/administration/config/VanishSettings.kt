package top.likoslupus.cellulosesz.administration.config

import kotlinx.serialization.Serializable

/** Policy for vanished players' interaction with items and join/quit announcements. */
@Serializable
public data class VanishSettings(
    /** A vanished player may not pick up item entities. */
    public val preventItemPickup: Boolean = true,
    /** A vanished player may not drop items. */
    public val preventItemDrop: Boolean = false,
    /** Hide the join and quit announcement of a vanished player. */
    public val hideJoinQuit: Boolean = true,
)

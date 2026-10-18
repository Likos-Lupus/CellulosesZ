package top.likoslupus.cellulosesz.administration.moderation.mute

/**
 * Pure decision for whether a requested mute duration is within the configured cap. A moderator
 * holding `cellulosesz.mute.unlimited` bypasses the cap entirely. A `null` cap means no limit.
 */
internal object MuteDurationPolicy {

    fun permits(
        requestedSeconds: Long?,
        capSeconds: Long?,
        unlimited: Boolean
    ): Boolean =
        when {
            requestedSeconds == null -> true
            unlimited -> true
            capSeconds == null -> true
            requestedSeconds <= capSeconds -> true
            else -> false
        }

}

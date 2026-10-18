package top.likoslupus.cellulosesz.administration.worldtools

/**
 * Tags TNT spawned by `/nuke`. The explosion mixin recognises the tag and replaces the normal
 * TNT explosion with a zero-power, non-destructive one.
 */
public object NukeTntMarker {

    public const val TAG: String = "cellulosesz.nuke"

}

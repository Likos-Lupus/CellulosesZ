package top.likoslupus.cellulosesz.utility.kit

/**
 * How a kit claim behaves when the target inventory cannot hold the whole kit. Only explicit
 * all-or-nothing ([REJECT]) and explicit overflow ([DROP]) semantics are supported; silent partial
 * delivery is intentionally impossible.
 */
public enum class KitOverflowPolicy {

    /** The kit must fit completely, otherwise no claim is reserved and nothing is inserted. */
    REJECT,

    /** What fits goes into the inventory; the remainder is dropped at the player's feet. */
    DROP,

}

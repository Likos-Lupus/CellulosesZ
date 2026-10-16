package top.likoslupus.cellulosesz.api.actor

import java.util.*

/**
 * The origin of a business intent.
 *
 * A [Player] actor is only honoured after the composition root has verified that the referenced
 * player is online and holds the required business capability. It is never a privilege escalation
 * token, and a system/addon actor is only obtainable through the registration window
 * ([AddonAccess]), never by passing a caller-supplied string.
 */
public sealed interface ApiActor {

    public data class Player(public val playerId: UUID) : ApiActor

}

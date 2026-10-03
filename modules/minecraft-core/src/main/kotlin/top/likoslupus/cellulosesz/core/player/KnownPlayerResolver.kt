package top.likoslupus.cellulosesz.core.player

import java.util.*

/**
 * Resolves player identities without ever touching the network. Online lookups use the server's
 * player list; offline lookups are limited to identities this server already knows.
 *
 * Implementations are server-thread confined; callers must hop through the kernel when off-thread.
 */
public interface KnownPlayerResolver {

    public fun onlineByName(name: String): KnownPlayerIdentity?

    public fun onlineById(id: UUID): KnownPlayerIdentity?

    public fun knownByName(name: String): KnownPlayerIdentity?

    public fun knownById(id: UUID): KnownPlayerIdentity?

}

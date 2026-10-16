package top.likoslupus.cellulosesz.api.actor

/**
 * Metadata an addon declares when it registers. It is used for attribution, diagnostics and
 * server-configured allow-listing only.
 *
 * It is **not** a cryptographic identity: every mod runs in the same server JVM, so [id] must never
 * be treated as an unforgeable security token.
 */
public class AddonDescriptor(
    public val id: String,
    public val displayName: String,
)

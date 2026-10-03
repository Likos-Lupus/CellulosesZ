package top.likoslupus.cellulosesz.core.runtime

/**
 * Minimal suspension seam for services that must publish state on the server thread but should stay
 * unit testable without a running Minecraft server. Production code adapts [RuntimeKernel]; tests
 * use an immediate runner.
 */
public interface ServerThreadRunner {

    public suspend fun <T> run(block: () -> T): T

}

/** Adapts the kernel's server-thread boundary into a [ServerThreadRunner]. */
public fun RuntimeKernel.serverThreadRunner(): ServerThreadRunner =
    object : ServerThreadRunner {
        override suspend fun <T> run(block: () -> T): T =
            onServerThread(block)
    }

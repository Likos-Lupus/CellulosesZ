package top.likoslupus.cellulosesz.core.permission

import net.minecraft.commands.CommandSourceStack

/**
 * Loader-specific permission integration (Fabric permissions API, NeoForge PermissionAPI). The
 * implementation owns the tri-state semantics; this interface and its callers never import a loader
 * or third-party permission API.
 */
public interface PermissionBridge {

    public val backendName: String

    /** Returns true when [source] is allowed to use [spec], honouring the vanilla fallback. */
    public fun test(source: CommandSourceStack, spec: PermissionSpec): Boolean

}

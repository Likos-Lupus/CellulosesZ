package top.likoslupus.cellulosesz.core.permission

import net.minecraft.commands.CommandSourceStack

/**
 * Fallback bridge used when a platform has no third-party provider (and in tests). It reproduces the
 * exact pre-P0 vanilla behaviour, so installing no permission manager changes nothing.
 */
public object VanillaFallbackPermissionBridge : PermissionBridge {

    override val backendName: String
        get() = "vanilla"

    override fun test(source: CommandSourceStack, spec: PermissionSpec): Boolean =
        vanillaFallback(source, spec)

}

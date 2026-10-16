package top.likoslupus.cellulosesz.core.permission

/**
 * Splits a fully-qualified CellulosesZ permission node into the loader-local name required by
 * loaders that compose `modId + "." + localName` into the final node (NeoForge's
 * `PermissionNode(String modId, String nodeName, ...)`).
 *
 * The canonical node is always dot-separated (`cellulosesz.command.tp`) and so is every loader's
 * reported node; the local name is therefore `command.tp`, never `command/tp`. Splitting is kept as
 * a pure, Minecraft-free function so the contract is unit-testable and the loader adapter stays thin.
 */
public object PermissionNodeNames {

    /** The CellulosesZ mod id, also the leading namespace of every permission node. */
    public const val MOD_ID: String = "cellulosesz"

    private const val PREFIX: String = "$MOD_ID."

    /**
     * Returns the loader-local part of [node], for example `command.tp` for
     * `cellulosesz.command.tp`.
     *
     * Rejects a node without the [MOD_ID] namespace and any local part that carries a path separator
     * (`/`), a namespace separator (`:`) or whitespace, so a slash-form node can never be produced.
     */
    public fun localName(node: String): String {
        require(node.startsWith(PREFIX)) {
            "permission node '$node' must start with '$PREFIX'"
        }
        val local = node.substring(PREFIX.length)
        require(local.isNotEmpty()) {
            "permission node '$node' has an empty local part"
        }
        require('/' !in local && ':' !in local && local.none { it.isWhitespace() }) {
            "permission node '$node' local part must be dot-separated without '/' or ':'"
        }
        return local
    }

}

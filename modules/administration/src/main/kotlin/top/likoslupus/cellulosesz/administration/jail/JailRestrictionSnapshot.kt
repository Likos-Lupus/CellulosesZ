package top.likoslupus.cellulosesz.administration.jail

import top.likoslupus.cellulosesz.administration.config.JailSettings

/**
 * Immutable snapshot of the jail command restriction. It is published atomically (volatile swap) by
 * [top.likoslupus.cellulosesz.administration.AdministrationFeature] and read by
 * [top.likoslupus.cellulosesz.administration.mixin.CommandsRestrictionMixin] on the command path, so
 * a reload can replace the whole rule without partial visibility. It holds only strings and a flag;
 * it never references the composition graph.
 */
internal data class JailRestrictionSnapshot(
    val enabled: Boolean,
    val allowedRoutes: Set<String>,
) {

    /**
     * True when [commandLine]'s first token is an allowed route *and* the restriction is enabled.
     * Blank input and a disabled snapshot are never allowed, so the bridge fails closed on its own.
     */
    fun isAllowed(commandLine: String?): Boolean {
        if (!enabled) {
            return false
        }
        val token = normalizeFirstToken(commandLine) ?: return false
        return token in allowedRoutes
    }

    internal companion object {

        val NONE: JailRestrictionSnapshot =
            JailRestrictionSnapshot(false, emptySet())

        private val WHITESPACE = Regex("\\s+")

        /** Leading slash and surrounding whitespace are ignored; the token is lowercased. */
        fun normalizeFirstToken(commandLine: String?): String? {
            val trimmed = commandLine
                    ?.trim()
                    ?.removePrefix("/")
                    ?.trim()
                ?: return null
            if (trimmed.isEmpty()) {
                return null
            }
            return trimmed.split(WHITESPACE, limit = 2).first().lowercase()
        }

    }

}

/**
 * Turns the configured allow-list into a [JailRestrictionSnapshot]. Each configured entry is also
 * accepted in its namespaced form, so `/helpop` and `/cellulosesz:helpop` are treated identically.
 * Judgment lives here in Kotlin; the Java mixin only calls the narrow snapshot bridge.
 */
internal object JailCommandAccessPolicy {

    fun snapshotFrom(settings: JailSettings): JailRestrictionSnapshot {
        val allowed = buildSet {
            settings.allowedCommands.forEach { raw ->
                val entry = raw.trim().lowercase()
                if (entry.isEmpty()) {
                    return@forEach
                }
                add(entry)
                if (!entry.contains(':')) {
                    add("cellulosesz:$entry")
                }
            }
        }
        return JailRestrictionSnapshot(
            enabled = settings.restrictCommands,
            allowedRoutes = allowed
        )
    }

}

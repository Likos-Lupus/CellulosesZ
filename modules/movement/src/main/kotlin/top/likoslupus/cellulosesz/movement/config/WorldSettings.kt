package top.likoslupus.cellulosesz.movement.config

import kotlinx.serialization.Serializable

/**
 * `/world` alias table. Keys are lowercase aliases, values are dimension resource ids (for example
 * `minecraft:the_nether`). An alias is only an input convenience: a raw dimension id is always
 * accepted, and the canonical display name resolves back to the first matching alias.
 */
@Serializable
public data class WorldSettings(
    public val aliases: Map<String, String> = DEFAULT_ALIASES,
) {

    public companion object {

        public val DEFAULT_ALIASES: Map<String, String> = linkedMapOf(
            "overworld" to "minecraft:overworld",
            "nether" to "minecraft:the_nether",
            "end" to "minecraft:the_end",
        )

    }

}

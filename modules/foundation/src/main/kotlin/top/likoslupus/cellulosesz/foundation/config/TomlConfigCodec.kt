package top.likoslupus.cellulosesz.foundation.config

import dev.eav.tomlkt.Toml
import kotlinx.serialization.KSerializer

/**
 * TOML decoder backed by kotlinx.serialization. Unknown keys are rejected so a typo in the user
 * TOML fails the load instead of being silently ignored.
 */
public class TomlConfigCodec<T>(
    private val serializer: KSerializer<T>,
) : ConfigDecoder<T> {

    private val toml = Toml {
        ignoreUnknownKeys = false
    }

    override fun decode(text: String): T =
        toml.decodeFromString(
            serializer,
            text
        )

}

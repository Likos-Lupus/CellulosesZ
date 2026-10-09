package top.likoslupus.cellulosesz.application.config

import top.likoslupus.cellulosesz.application.bootstrap.CellulosesZ
import top.likoslupus.cellulosesz.foundation.persistence.AtomicFile
import java.nio.file.Files
import java.nio.file.Path

/**
 * Writes the packaged, commented TOML template on first boot. The template is never re-encoded or
 * overwritten, so user comments survive.
 */
internal object ConfigDefaults {

    private const val RESOURCE = "/defaults/cellulosesz.toml"

    fun writeIfMissing(path: Path) {
        if (Files.exists(path)) {
            return
        }
        val stream = CellulosesZ::class.java.getResourceAsStream(RESOURCE)
            ?: error("packaged default config '$RESOURCE' is missing from the distribution")
        val text = stream.use { it.readBytes().decodeToString() }
        AtomicFile.writeUtf8AtomicallyBlocking(path, text)
    }

}

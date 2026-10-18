package top.likoslupus.cellulosesz.application.config

import top.likoslupus.cellulosesz.application.bootstrap.CellulosesZ
import top.likoslupus.cellulosesz.foundation.config.ConfigFileSpec
import top.likoslupus.cellulosesz.foundation.persistence.AtomicFile
import java.nio.file.Files
import java.nio.file.Path

/**
 * Writes each packaged, commented TOML template on first boot. Files are never re-encoded or
 * overwritten, so user comments survive. The main file and the six module files are independent:
 * a missing file is recreated with its own scope only.
 */
internal object ConfigDefaults {

    fun writeIfMissing(directory: Path, files: List<ConfigFileSpec>) {
        Files.createDirectories(directory)
        files.forEach { spec ->
            val path = directory.resolve(spec.fileName)
            if (Files.exists(path)) {
                return@forEach
            }
            val stream = CellulosesZ::class.java.getResourceAsStream(spec.templateResource)
                ?: error(
                    "packaged default config '${spec.templateResource}' is missing " +
                            "from the distribution"
                )
            val text = stream.use { it.readBytes().decodeToString() }
            AtomicFile.writeUtf8AtomicallyBlocking(path, text)
        }
    }

}

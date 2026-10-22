package top.likoslupus.cellulosesz.foundation.database.driver

import top.likoslupus.cellulosesz.foundation.database.DatabaseType
import top.likoslupus.cellulosesz.foundation.database.StorageException
import java.util.*

/**
 * The driver artifacts this build was compiled against, one per [DatabaseType]. The manifest is
 * generated from `gradle/libs.versions.toml` at build time, so the pinned coordinates and hashes
 * can never drift from the version catalog.
 */
public class DriverCatalog private constructor(
    private val byType: Map<DatabaseType, DriverArtifact>,
) {

    public fun artifact(type: DatabaseType): DriverArtifact? = byType[type]

    public companion object {

        private const val RESOURCE = "/cellulosesz/drivers/drivers.properties"

        /**
         * Reads the packaged manifest. A missing manifest is a broken distribution: the mod has no
         * driver for any backend, so failing here is clearer than failing on every connection.
         */
        public fun load(): DriverCatalog {
            val stream = DriverCatalog::class.java.getResourceAsStream(RESOURCE)
                ?: throw StorageException(
                    "driver manifest '$RESOURCE' is missing from the distribution; " +
                            "the build did not inject generateDriverManifest"
                )
            val properties = Properties()
            stream.use { properties.load(it) }
            return of(
                properties.stringPropertyNames()
                        .sorted()
                        .associate { backend ->
                            backend.toType() to parse(
                                backend,
                                properties.getProperty(backend)
                            )
                        }
            )
        }

        /** Builds a catalog without the packaged manifest; used by tests and by tooling. */
        public fun of(entries: Map<DatabaseType, DriverArtifact>): DriverCatalog =
            DriverCatalog(LinkedHashMap(entries))

        private fun String.toType(): DatabaseType =
            DatabaseType.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
                ?: throw StorageException("driver manifest names an unknown backend '$this'")

        private fun parse(backend: String, value: String): DriverArtifact {
            val parts = value.split('|')
            if (parts.size != 5) {
                throw StorageException(
                    "driver manifest entry '$backend' is malformed: expected " +
                            "'coordinate|file|size|sha256|repositoryPath'"
                )
            }
            val size = parts[2].toLongOrNull()
                ?: throw StorageException("driver manifest entry '$backend' has a non-numeric size")
            if (parts[3].length != 64) {
                throw StorageException("driver manifest entry '$backend' has a malformed SHA-256")
            }
            return DriverArtifact(
                coordinate = parts[0],
                fileName = parts[1],
                size = size,
                sha256 = parts[3],
                repositoryPath = parts[4],
            )
        }

    }

}

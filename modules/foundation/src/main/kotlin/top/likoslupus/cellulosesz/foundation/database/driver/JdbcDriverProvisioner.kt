package top.likoslupus.cellulosesz.foundation.database.driver

import top.likoslupus.cellulosesz.foundation.database.DatabaseType
import top.likoslupus.cellulosesz.foundation.database.DriverSettings
import top.likoslupus.cellulosesz.foundation.database.ResolvedDatabaseDescriptor
import java.nio.file.Path

/**
 * Resolves the driver for one backend, in a fixed order:
 *
 * 1. the mod class loader (a bundled jar, a development run, or a driver another mod ships);
 * 2. the verified copy in the driver directory (an operator-provided or previously downloaded jar);
 * 3. a download from the configured repository, when `[database.driver] auto-download` is on.
 *
 * A backend without a usable driver fails the cold start; it never falls back to another backend.
 */
public class JdbcDriverProvisioner private constructor(
    private val catalog: DriverCatalog,
    private val settings: DriverSettings,
    private val store: DriverStore,
    private val fetcher: DriverFetcher,
) {

    public fun provision(descriptor: ResolvedDatabaseDescriptor): DriverProvision {
        if (isOnClasspath(descriptor.driverClassName)) {
            return DriverProvision.Ready(
                DriverHandle.Classpath,
                DriverSource.CLASSPATH
            )
        }

        val artifact = catalog.artifact(descriptor.type)
            ?: return DriverProvision.Unavailable(
                "this build has no driver registered for backend " +
                        "'${descriptor.type.name.lowercase()}'"
            )

        store.verified(artifact)?.let { jar ->
            return DriverProvision.Ready(
                external(descriptor, artifact, jar),
                DriverSource.CACHED
            )
        }

        if (!settings.autoDownload) {
            return DriverProvision.Unavailable(missing(descriptor, artifact))
        }

        runCatching { fetcher.fetch(artifact, store) }.onFailure { failure ->
            return DriverProvision.Unavailable(
                missing(descriptor, artifact) + "\n  download failed: ${failure.message}"
            )
        }

        val jar = store.verified(artifact)
            ?: return DriverProvision.Unavailable(missing(descriptor, artifact))
        return DriverProvision.Ready(
            external(descriptor, artifact, jar),
            DriverSource.DOWNLOADED
        )
    }

    public fun expectedPath(type: DatabaseType): Path? =
        catalog.artifact(type)?.let(store::expectedPath)

    private fun external(
        descriptor: ResolvedDatabaseDescriptor,
        artifact: DriverArtifact,
        jar: Path,
    ): DriverHandle.External {
        val driver = IsolatedDriverLoader.load(
            artifact,
            jar,
            descriptor.driverClassName
        )
        return DriverHandle.External(
            DriverDataSource(
                driver = driver,
                jdbcUrl = descriptor.jdbcUrl,
                username = descriptor.username,
                password = descriptor.secret.resolve(),
            )
        )
    }

    private fun missing(
        descriptor: ResolvedDatabaseDescriptor,
        artifact: DriverArtifact,
    ): String {
        val target = store.expectedPath(artifact)
        return buildString {
            appendLine(
                "the ${descriptor.type.name.lowercase()} driver ${artifact.coordinate} is not available"
            )
            appendLine("  expected file: $target")
            appendLine("  download from: ${fetcher.repositoryUrl(artifact)}")
            appendLine("  sha256:        ${artifact.sha256}")
            append(
                "  place the jar at the expected path (an offline bundle is published with every " +
                        "release) or set [database.driver] auto-download = true"
            )
        }
    }

    private fun isOnClasspath(driverClassName: String): Boolean =
        runCatching {
            Class.forName(
                driverClassName,
                false,
                JdbcDriverProvisioner::class.java.classLoader
            )
        }.isSuccess

    public companion object {

        /**
         * Builds a provisioner for one cold start. [gameRoot] is the server/client game directory;
         * [DriverSettings.directory] is resolved underneath it.
         */
        public fun create(settings: DriverSettings, gameRoot: Path): JdbcDriverProvisioner {
            val store = DriverStore(gameRoot.resolve(settings.directory))
            return JdbcDriverProvisioner(
                DriverCatalog.load(),
                settings,
                store,
                DriverFetcher(settings)
            )
        }

        /** Test/tooling entry point with an explicit catalog and store. */
        public fun of(
            catalog: DriverCatalog,
            settings: DriverSettings,
            store: DriverStore,
        ): JdbcDriverProvisioner =
            JdbcDriverProvisioner(
                catalog,
                settings,
                store,
                DriverFetcher(settings)
            )

    }

}

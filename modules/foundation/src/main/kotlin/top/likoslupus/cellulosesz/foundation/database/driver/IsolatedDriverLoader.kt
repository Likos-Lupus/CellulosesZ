package top.likoslupus.cellulosesz.foundation.database.driver

import top.likoslupus.cellulosesz.foundation.database.StorageException
import java.net.URLClassLoader
import java.nio.file.Path
import java.sql.Driver
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads a downloaded driver in its own child class loader. `DriverManager` is deliberately not
 * used: it only accepts drivers visible to the *caller's* class loader, so a jar loaded here would
 * be invisible to HikariCP. The child loader is cached per artifact hash and lives for the process
 * lifetime, because pooled connections keep referring to the classes it loaded.
 */
public object IsolatedDriverLoader {

    private val loaders: ConcurrentHashMap<String, URLClassLoader> = ConcurrentHashMap()

    public fun load(
        artifact: DriverArtifact,
        jar: Path,
        driverClassName: String
    ): Driver =
        loadWith(
            artifact,
            jar,
            driverClassName,
            IsolatedDriverLoader::class.java.classLoader
        )

    /** Test seam: the parent loader decides what the child can see. */
    internal fun loadWith(
        artifact: DriverArtifact,
        jar: Path,
        driverClassName: String,
        parent: ClassLoader,
    ): Driver {
        val loader = loaders.computeIfAbsent("${artifact.sha256.lowercase()}@${parent.hashCode()}") {
            URLClassLoader(arrayOf(jar.toUri().toURL()), parent)
        }
        val type = runCatching { Class.forName(driverClassName, true, loader) }
                .getOrElse {
                    throw StorageException(
                        "driver class '$driverClassName' is not present in ${artifact.fileName}",
                        it,
                    )
                }
        return runCatching { type.getConstructor().newInstance() as? Driver }
                .getOrElse {
                    throw StorageException(
                        "driver class '$driverClassName' from ${artifact.fileName} cannot be instantiated",
                        it,
                    )
                }
            ?: throw StorageException(
                "'$driverClassName' from ${artifact.fileName} does not implement java.sql.Driver"
            )
    }

}

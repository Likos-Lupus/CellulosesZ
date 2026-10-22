import cellulosesz.checks.DriverManifestEntry
import cellulosesz.checks.driverCoordinates
import cellulosesz.checks.sha256Of
import cellulosesz.checks.writeDriverManifest

/**
 * JDBC drivers are not packaged inside the mod jar: the runtime provisions the driver for the
 * active backend at cold start (ADR 0024). This plugin generates the manifest the runtime verifies
 * against, and exposes the driver jars so the distribution build can publish them separately.
 *
 * Applied to `:modules:foundation`, which owns the JDBC stack.
 */

val catalog = the<VersionCatalogsExtension>().named("libs")
val coordinates = driverCoordinates(catalog)

/** The pinned driver jars. Non-transitive: a driver is shipped exactly as published. */
val driverArtifacts = configurations.create("driverArtifacts") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
    description = "Pinned JDBC driver artifacts."
}

dependencies {
    // Each driver ships exactly as published: no optional client libraries (protobuf for the MySQL
    // X DevAPI, checker-qual for PostgreSQL) are pulled in, matching what the runtime resolves.
    coordinates.forEach { coordinate ->
        val dependency = add(
            driverArtifacts.name,
            coordinate.notation
        ) as ModuleDependency
        dependency.isTransitive = false
    }
}

/** Consumable view of the same jars, used by the offline bundle in the root build. */
val jdbcDrivers = configurations.create("jdbcDrivers") {
    isCanBeConsumed = true
    isCanBeResolved = true
    extendsFrom(driverArtifacts)
    description = "JDBC driver jars exposed for packaging."
}

val manifestDirectory = layout.buildDirectory.dir("generated/driver-manifest")
val manifestFile = manifestDirectory.map { it.file("cellulosesz/drivers/drivers.properties") }

val generateDriverManifest = tasks.register("generateDriverManifest") {
    group = "build"
    description = "Writes the runtime JDBC driver manifest (coordinates, sizes, SHA-256)."
    val jars: FileCollection = driverArtifacts
    val drivers = coordinates
    inputs.files(jars)
    inputs.property("drivers", drivers.map { it.notation })
    outputs.dir(manifestDirectory)

    doLast {
        val byName = jars.files.associateBy { it.name }
        val entries = drivers.map { coordinate ->
            val jar = byName[coordinate.fileName]
                ?: error(
                    "driver manifest: ${coordinate.notation} did not resolve to ${coordinate.fileName}"
                )
            DriverManifestEntry(
                backend = coordinate.backend,
                notation = coordinate.notation,
                fileName = coordinate.fileName,
                size = jar.length(),
                sha256 = sha256Of(jar),
                repositoryPath = coordinate.repositoryPath,
            )
        }
        writeDriverManifest(manifestFile.get().asFile, entries)
    }
}

tasks.withType<ProcessResources>().configureEach {
    from(generateDriverManifest)
}

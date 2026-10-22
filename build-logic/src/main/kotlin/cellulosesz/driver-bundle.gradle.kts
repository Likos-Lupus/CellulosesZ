import cellulosesz.checks.readDriverManifest
import cellulosesz.checks.renderDriverBundleReadme
import org.gradle.api.artifacts.VersionCatalogsExtension

/**
 * Packages the JDBC drivers that the runtime provisions (ADR 0024) into one loader-independent
 * archive, so a host that cannot reach Maven Central can be provisioned offline. The archive is
 * reproducible and its README lists every file's SHA-256 and the backend it serves.
 *
 * Applied to the root project; the jars and their manifest come from `:modules:foundation`.
 */

val catalog = the<VersionCatalogsExtension>().named("libs")
val modVersion = catalog.findVersion("mod").orElseThrow {
    IllegalStateException("version catalog has no version alias 'mod'")
}.requiredVersion

val foundation = project(":modules:foundation")
val manifestFile = foundation.layout.buildDirectory
    .file("generated/driver-manifest/cellulosesz/drivers/drivers.properties")
val manifestTaskPath = ":modules:foundation:generateDriverManifest"

val driverBundle = configurations.create("driverBundle") {
    isCanBeConsumed = false
    isCanBeResolved = true
    description = "JDBC drivers packaged as the offline driver bundle."
}

dependencies {
    add(
        driverBundle.name,
        project(
            mapOf(
                "path" to ":modules:foundation",
                "configuration" to "jdbcDrivers",
            )
        )
    )
}

val bundleReadme = layout.buildDirectory.file("driver-bundle/readme/README.txt")

val generateDriverBundleReadme = tasks.register("generateDriverBundleReadme") {
    description = "Writes the offline JDBC driver bundle instructions with per-file checksums."
    dependsOn(manifestTaskPath)
    inputs.file(manifestFile)
    outputs.file(bundleReadme)

    doLast {
        val entries = readDriverManifest(manifestFile.get().asFile)
        val target = bundleReadme.get().asFile
        target.parentFile.mkdirs()
        target.writeText(renderDriverBundleReadme(entries))
    }
}

tasks.register<Zip>("packageDriverBundle") {
    group = "build"
    description = "Packages the JDBC drivers for offline installation (release asset)."
    archiveFileName.set("cellulosesz-drivers-$modVersion.zip")
    destinationDirectory.set(layout.buildDirectory.dir("driver-bundle"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    from(driverBundle) { into("cellulosesz/drivers") }
    from(generateDriverBundleReadme)
}

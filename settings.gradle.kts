pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.architectury.dev/") { name = "Architectury" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version providers.gradleProperty("stonecutter_version")
}

stonecutter {
    create(rootProject) {
        version("26.1.2-fabric", "26.1.2").buildscript("build.fabric.gradle.kts")
        version("26.1.2-neoforge", "26.1.2").buildscript("build.neoforge.gradle.kts")
        vcsVersion = "26.1.2-fabric"
    }
}

// Architectury Loom selects its platform from the `loom.platform` project property, which must be
// present before the Loom plugin is applied. Stonecutter cells share the root gradle.properties, so
// the value is injected per cell here.
gradle.beforeProject(object : org.gradle.api.Action<org.gradle.api.Project> {
    override fun execute(project: org.gradle.api.Project) {
        val platform = when (project.name) {
            "26.1.2-fabric" -> "fabric"
            "26.1.2-neoforge" -> "neoforge"
            else -> null
        }
        if (platform != null) {
            project.extensions.extraProperties.set("loom.platform", platform)
        }
    }
})

rootProject.name = "CellulosesZ"

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
    // Hoisted so every project shares one plugin classloader (see gradle.properties).
    id("org.jetbrains.kotlin.jvm") version providers.gradleProperty("kotlin_version") apply false
    id("org.jetbrains.kotlin.plugin.serialization") version providers.gradleProperty("kotlin_version") apply false
    id("dev.architectury.loom-no-remap") version providers.gradleProperty("architectury_loom_version") apply false
    id("architectury-plugin") version providers.gradleProperty("architectury_plugin_version") apply false
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.architectury.dev/") { name = "Architectury" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    }
}

// Compile-time architecture modules. These are fixed single-version compiled subprojects that
// establish Kotlin `internal` boundaries; they are NOT Stonecutter version cells.
include(
    ":modules:foundation",
    ":modules:minecraft-core",
    ":modules:movement",
    ":modules:communication",
    ":modules:administration",
    ":modules:utility",
    ":modules:application",
)

stonecutter {
    create(rootProject) {
        version("26.1.2-fabric", "26.1.2").buildscript("build.fabric.gradle.kts")
        version("26.1.2-neoforge", "26.1.2").buildscript("build.neoforge.gradle.kts")
        vcsVersion = "26.1.2-fabric"
    }
}

// Architectury Loom selects its platform from the `loom.platform` project property, which must be
// present before the Loom plugin is applied. Only the two distribution cells are loader-specific;
// the architecture modules stay platform-neutral common projects.
gradle.beforeProject(object : Action<Project> {
    override fun execute(project: Project) {
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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("dev.architectury.loom-no-remap")
    id("architectury-plugin")
}

val modId = providers.gradleProperty("mod_id").get()
val modGroup = providers.gradleProperty("mod_group").get()
val minecraftVersion = libs.versions.minecraft.get()
val javaVersion = libs.versions.java.get().toInt()

group = modGroup
version = "${libs.versions.mod.get()}+$minecraftVersion"
base.archivesName.set(modId)

val internalModules = listOf(
    ":modules:foundation",
    ":modules:minecraft-core",
    ":modules:movement",
    ":modules:communication",
    ":modules:administration",
    ":modules:utility",
    ":modules:application",
)

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    silentMojangMappingsLicense()
    mods {
        register("cellulosesz") {
            sourceSet(sourceSets["main"])
        }
    }
}

sourceSets["main"].apply {
    java.setSrcDirs(listOf(rootProject.file("platform/neoforge/src/main/java")))
    kotlin.setSrcDirs(listOf(rootProject.file("platform/neoforge/src/main/kotlin")))
    resources.setSrcDirs(listOf(rootProject.file("platform/neoforge/src/main/resources")))
}

sourceSets["test"].apply {
    java.setSrcDirs(emptyList<String>())
    kotlin.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    maven("https://maven.architectury.dev/") { name = "Architectury" }
}

// Architectury Loom registers the `neoForge` configuration lazily, so the loader
// dependency is attached as soon as the configuration appears.
val dependencyHandler = dependencies
configurations.matching { it.name == "neoForge" }.configureEach {
    dependencyHandler.add(
        name,
        "net.neoforged:neoforge:${libs.versions.neoforge.get()}"
    )
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")

    implementation(libs.architectury.neoforge)

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    internalModules.forEach { path ->
        val dependency = add("implementation", project(path))
        (dependency as ModuleDependency).isTransitive = false
    }

    // Kotlin runtime is shipped Jar-in-Jar because NeoForge users must not need KotlinForForge.
    include(libs.kotlin.stdlib) { isTransitive = false }
    include(libs.kotlinx.coroutines.core.jvm) { isTransitive = false }
    include(libs.kotlinx.serialization.core.jvm) { isTransitive = false }
    include(libs.kotlinx.serialization.json.jvm) { isTransitive = false }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

kotlin {
    jvmToolchain(javaVersion)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

tasks.named<ProcessResources>("processResources") {
    val values = mapOf(
        "version" to project.version.toString(),
        "minecraft_version" to minecraftVersion,
        "neoforge_version" to libs.versions.neoforge.get(),
    )
    inputs.properties(values)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(values)
    }
}

tasks.named<Jar>("jar") {
    archiveFileName.set("$modId-$version-neoforge.jar")
    from(rootProject.file("LICENSE.txt")) {
        rename { "${it}_$modId" }
    }
    internalModules.forEach { path ->
        from(project(path).the<SourceSetContainer>()["main"].output)
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

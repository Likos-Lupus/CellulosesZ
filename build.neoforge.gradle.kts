import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.architectury.loom.no.remap)
    alias(libs.plugins.architectury.plugin)
}

val modId = providers.gradleProperty("mod_id").get()
val modGroup = providers.gradleProperty("mod_group").get()
val minecraftVersion = libs.versions.minecraft.get()
val javaVersion = libs.versions.java.get().toInt()

group = modGroup
version = "${libs.versions.mod.get()}+$minecraftVersion"
base.archivesName.set(modId)

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
    java.setSrcDirs(listOf(rootProject.file("src/neoforge/java")))
    kotlin.setSrcDirs(
        listOf(
            rootProject.file("src/common/kotlin"),
            rootProject.file("src/neoforge/kotlin"),
        )
    )
    resources.setSrcDirs(
        listOf(
            rootProject.file("src/common/resources"),
            rootProject.file("src/neoforge/resources")
        )
    )
}

sourceSets["test"].apply {
    kotlin.setSrcDirs(listOf(rootProject.file("src/common/test/kotlin")))
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
    dependencyHandler.add(name, "net.neoforged:neoforge:${libs.versions.neoforge.get()}")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")

    implementation(libs.architectury.neoforge)

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // Kotlin runtime is shipped Jar-in-Jar because NeoForge users must not need KotlinForForge.
    include(libs.kotlin.stdlib) { isTransitive = false }
    include(libs.kotlinx.coroutines.core.jvm) { isTransitive = false }
    include(libs.kotlinx.serialization.core.jvm) { isTransitive = false }
    include(libs.kotlinx.serialization.json.jvm) { isTransitive = false }

    compileOnly(libs.jspecify)
    testCompileOnly(libs.jspecify)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
    withSourcesJar()
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
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
}

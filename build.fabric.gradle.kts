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

// Fixed compile-time architecture modules that are flattened into the distribution jar. These are
// NOT loader cells and NOT separately shipped artifacts.
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
    fabric()
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
    java.setSrcDirs(listOf(rootProject.file("platform/fabric/src/main/java")))
    kotlin.setSrcDirs(listOf(rootProject.file("platform/fabric/src/main/kotlin")))
    resources.setSrcDirs(listOf(rootProject.file("platform/fabric/src/main/resources")))
}

sourceSets["test"].apply {
    kotlin.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://maven.architectury.dev/") { name = "Architectury" }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.language.kotlin)
    implementation(libs.architectury.fabric)
    implementation(libs.fabric.permissions.api)

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    internalModules.forEach { path ->
        val dependency = add("implementation", project(path))
        (dependency as ModuleDependency).isTransitive = false
    }

    // Storage/config/permission runtime is nested as Fabric jars (not flattened), so driver
    // resource layout (SQLite natives, service files) and license metadata survive intact.
    include(libs.hikari) { isTransitive = false }
    include(libs.tomlkt) { isTransitive = false }
    include(libs.sqlite.jdbc) { isTransitive = false }
    include(libs.h2) { isTransitive = false }
    include(libs.postgresql) { isTransitive = false }
    include(libs.mariadb) { isTransitive = false }
    include(libs.mysql) { isTransitive = false }
    include(libs.fabric.permissions.api) { isTransitive = false }
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
        "loader_version" to libs.versions.fabric.loader.get(),
        "fabric_version" to libs.versions.fabric.api.get(),
        "fabric_language_kotlin_version" to libs.versions.fabric.language.kotlin.get(),
        "architectury_version" to libs.versions.architectury.api.get(),
    )
    inputs.properties(values)
    filesMatching("fabric.mod.json") {
        expand(values)
    }
}

tasks.named<Jar>("jar") {
    archiveFileName.set("$modId-$version-fabric.jar")
    from(rootProject.file("LICENSE.txt")) {
        rename { "${it}_$modId" }
    }
    // Flatten internal module outputs into the distribution jar. External mods (Fabric API,
    // Architectury, FLK) are NOT included.
    internalModules.forEach { path ->
        from(project(path).the<SourceSetContainer>()["main"].output)
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

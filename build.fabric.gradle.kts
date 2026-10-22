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
    ":modules:api",
    ":modules:foundation",
    ":modules:minecraft-core",
    ":modules:movement",
    ":modules:communication",
    ":modules:administration",
    ":modules:utility",
    ":modules:player",
    ":modules:economy",
    ":modules:application",
)

// Runtime libraries shipped inside the mod jar: nested (never flattened) so SQLite native
// resources and service metadata survive. JDBC drivers are deliberately absent: the mod
// provisions the driver for the active backend at cold start (ADR 0024).
val bundledRuntime = listOf(
    libs.hikari,
    libs.tomlkt,
    libs.adventure.platform.fabric,
    libs.commonmark,
)

// Drivers are only needed on the development class path (`runServer`) and by the module tests;
// they are never nested into the shipped jar.
val driverRuntime = listOf(
    libs.sqlite.jdbc,
    libs.h2,
    libs.postgresql,
    libs.mariadb,
    libs.mysql,
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
    kotlin.setSrcDirs(
        listOf(
            rootProject.file("platform/common/src/main/kotlin"),
            rootProject.file("platform/fabric/src/main/kotlin"),
        )
    )
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

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    internalModules.forEach { path ->
        val dependency = add("implementation", project(path))
        (dependency as ModuleDependency).isTransitive = false
    }

    // These must be on the dev launch classpath as well as nested into the shipped jar: `include`
    // only nests, so without the plain `implementation` the dimension run is missing
    // Toml/Hikari/drivers. Nesting (not flattening) keeps SQLite natives and driver service files.
    bundledRuntime.forEach { implementation(it) }
    bundledRuntime.forEach { include(it) { isTransitive = false } }
    driverRuntime.forEach { implementation(it) }

    // Optional native LuckPerms integration. compileOnly: the API is never bundled or nested; the
    // adapter class is only loaded when LuckPerms is actually present (see ADR 0020).
    compileOnly(libs.luckperms.api)
    compileOnly(libs.checker.qual)
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
    internalModules.forEach { path ->
        from(project(path).the<SourceSetContainer>()["main"].output)
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

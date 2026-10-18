import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("dev.architectury.loom-no-remap")
    id("architectury-plugin")
    `java-library`
}

val javaVersion = libs.versions.java.get().toInt()
val minecraftVersion = libs.versions.minecraft.get()
// The catalog has no dedicated minimessage alias; reuse the adventure version it pins.
val adventureVersion: String = requireNotNull(libs.adventure.api.get().version)

group = providers.gradleProperty("mod_group").get()

architectury {
    common("fabric", "neoforge")
}

loom {
    silentMojangMappingsLicense()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")

    implementation(project(":modules:foundation"))
    implementation(project(":modules:minecraft-core"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    compileOnly(libs.jspecify)
    compileOnly(libs.sponge.mixin)
    // Strict MiniMessage parsing for lifecycle/death templates. The runtime copy is supplied by the
    // bundled adventure platform mod, so this stays compile-only in the distribution.
    compileOnly("net.kyori:adventure-text-minimessage:$adventureVersion")
    testCompileOnly(libs.jspecify)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.sqlite.jdbc)
    testImplementation("net.kyori:adventure-text-minimessage:$adventureVersion")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

kotlin {
    jvmToolchain(javaVersion)
    explicitApi()
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

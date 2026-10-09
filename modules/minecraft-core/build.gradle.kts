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
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // Adventure Component is part of the public text API (the future document renderer consumes the
    // same output type). The mod-platform audience bridge is a runtime/platform detail supplied by
    // the loader cells, so it is compile-only here and is never exposed as a feature API.
    api(libs.adventure.api)
    compileOnly(libs.adventure.platform.mod.shared)

    compileOnly(libs.jspecify)
    testCompileOnly(libs.jspecify)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.sqlite.jdbc)
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

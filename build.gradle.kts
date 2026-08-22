import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.architectury.plugin) apply false
    alias(libs.plugins.architectury.loom.no.remap) apply false
    alias(libs.plugins.shadow) apply false
    `maven-publish`
}

val cellulosesJavaVersion = libs.versions.java.get().toInt()
val jspecifyDependency = libs.jspecify
val junitDependency = libs.junit.jupiter
val junitPlatformLauncherDependency = libs.junit.platform.launcher

allprojects {
    group = "top.likoslupus"
    version = providers.gradleProperty("mod_version").get()

    repositories {
        maven("https://maven.architectury.dev/") {
            name = "Architectury"
        }
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        maven("https://maven.neoforged.net/releases/") {
            name = "NeoForge"
        }
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    extensions.configure<BasePluginExtension> {
        archivesName.set(
            providers.gradleProperty("archives_base_name").map { baseName ->
                if (project.name == "cellulosesz-fabric") baseName else "${baseName}-${project.name}"
            })
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(cellulosesJavaVersion))
        withSourcesJar()
    }

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(cellulosesJavaVersion)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
            javaParameters.set(true)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(cellulosesJavaVersion)
        options.compilerArgs.addAll(
            listOf(
                "-parameters", "-Xlint:unchecked", "-Xlint:deprecation"
            )
        )
    }

    dependencies {
        "compileOnly"(jspecifyDependency)
        "testCompileOnly"(jspecifyDependency)
        "testImplementation"(junitDependency)
        "testRuntimeOnly"(junitPlatformLauncherDependency)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}


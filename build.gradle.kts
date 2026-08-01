import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    alias(libs.plugins.architectury.plugin) apply false
    alias(libs.plugins.architectury.loom.no.remap) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.spotless)
    `maven-publish`
}

val cellulosesJavaVersion = libs.versions.java.get().toInt()
val jspecifyDependency = libs.jspecify
val lombokDependency = libs.lombok
val junitDependency = libs.junit.jupiter

val formatterConfig = rootProject.file("jdt.xml")

allprojects {
    group = "top.likoslupus"
    version = providers.gradleProperty("mod_version").get()

    repositories {
        maven("https://maven.architectury.dev/") {
            name = "Architectury"
        }
        maven("https://maven.neoforged.net/releases/") {
            name = "NeoForge"
        }
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<BasePluginExtension> {
        archivesName.set(
            providers.gradleProperty("archives_base_name")
                .map { baseName ->
                    if (project.name == "cellulosesz-fabric") baseName else "${baseName}-${project.name}"
                }
        )
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(cellulosesJavaVersion))
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(cellulosesJavaVersion)
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:unchecked", "-Xlint:deprecation"))
    }

    dependencies {
        "compileOnly"(jspecifyDependency)
        "compileOnly"(lombokDependency)
        "annotationProcessor"(lombokDependency)
        "testCompileOnly"(lombokDependency)
        "testAnnotationProcessor"(lombokDependency)
        "testCompileOnly"(jspecifyDependency)
        "testImplementation"(junitDependency)
    }

    tasks.withType<Test>().configureEach { useJUnitPlatform() }

    extensions.configure<SpotlessExtension> {
        java {
            target("src/main/java/**/*.java", "src/test/java/**/*.java")
            targetExclude(
                "**/build/**",
                "**/generated/**",
                "**/generated-sources/**"
            )
            eclipse().configFile(formatterConfig)
            palantirJavaFormat(libs.versions.palantir.java.format.get())
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    tasks.named("check") {
        dependsOn("spotlessCheck")
    }

    tasks.named("build") {
        dependsOn("spotlessCheck")
    }
}


tasks.named("spotlessApply") {
    dependsOn(subprojects.map { it.tasks.named("spotlessApply") })
}

tasks.named("spotlessCheck") {
    dependsOn(subprojects.map { it.tasks.named("spotlessCheck") })
}

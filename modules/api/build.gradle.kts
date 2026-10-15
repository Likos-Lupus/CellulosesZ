import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-library`
    `maven-publish`
}

val javaVersion = libs.versions.java.get().toInt()

group = providers.gradleProperty("mod_group").get()
version = providers.gradleProperty("public_api_version").get()
base.archivesName.set("cellulosesz-api")

dependencies {
    // The only compile dependency a consumer inherits. No Minecraft, Loader, database or feature
    // classes are visible from here.
    api(libs.kotlin.stdlib)
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
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("api") {
            from(components["java"])
            artifactId = "cellulosesz-api"
            pom {
                name.set("CellulosesZ Public API")
                description.set("Experimental public contracts for CellulosesZ integrations")
                licenses {
                    license {
                        name.set("GPL-3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                    }
                }
            }
        }
    }
}

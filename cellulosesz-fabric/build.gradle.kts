plugins {
    alias(libs.plugins.architectury.loom.no.remap)
    alias(libs.plugins.architectury.plugin)
    alias(libs.plugins.shadow)
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    mods {
        register("cellulosesz") {
            sourceSet(sourceSets["main"])
        }
    }
}

val commonConfiguration = configurations.create("common") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val developmentFabric = configurations.named("developmentFabric")
val shadowBundle = configurations.create("shadowBundle") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations.named("compileClasspath") { extendsFrom(commonConfiguration) }
configurations.named("runtimeClasspath") { extendsFrom(commonConfiguration) }
configurations.named("testCompileClasspath") { extendsFrom(commonConfiguration) }
configurations.named("testRuntimeClasspath") { extendsFrom(commonConfiguration) }
developmentFabric.configure { extendsFrom(commonConfiguration) }

val loomCommonProjects = listOf(
    ":cellulosesz-common",
    ":cellulosesz-modules:cellulosesz-module-text",
    ":cellulosesz-modules:cellulosesz-module-home",
    ":cellulosesz-modules:cellulosesz-module-warp",
    ":cellulosesz-modules:cellulosesz-module-kit",
    ":cellulosesz-modules:cellulosesz-module-command",
    ":cellulosesz-modules:cellulosesz-module-messaging",
    ":cellulosesz-modules:cellulosesz-module-economy",
    ":cellulosesz-modules:cellulosesz-module-playerstate",
    ":cellulosesz-modules:cellulosesz-module-admin",
    ":cellulosesz-modules:cellulosesz-module-teleport",
    ":cellulosesz-modules:cellulosesz-module-item",
    ":cellulosesz-modules:cellulosesz-module-world",
    ":cellulosesz-modules:cellulosesz-module-sign"
)
val plainJvmProjects = listOf(
    ":cellulosesz-api",
    ":cellulosesz-core",
    ":cellulosesz-modules:cellulosesz-module-user",
    ":cellulosesz-modules:cellulosesz-module-permission"
)

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.language.kotlin)
    implementation(libs.architectury.fabric)

    loomCommonProjects.forEach { path ->
        add(
            commonConfiguration.name, project(path = path)
        ) {
            isTransitive = false
        }
        add(
            shadowBundle.name, project(
                path = path, configuration = "transformProductionFabric"
            )
        ) {
            isTransitive = false
        }
    }

    plainJvmProjects.forEach { path ->
        implementation(project(path))
        add(
            shadowBundle.name, project(path)
        ) {
            isTransitive = false
        }
    }

    implementation(libs.jackson.databind)
    implementation(libs.jackson.yaml)
    implementation(libs.jackson.kotlin)
    add(shadowBundle.name, libs.jackson.databind)
    add(shadowBundle.name, libs.jackson.yaml)
    add(shadowBundle.name, dependencies.create(libs.jackson.kotlin.get()).apply {
        (this as? ModuleDependency)?.isTransitive = false
    })
}

val archiveBaseName = providers.gradleProperty("archives_base_name")
val licenseFile = rootProject.file("LICENSE.txt")

tasks.named<Jar>("jar") {
    archiveClassifier.set("raw")
    inputs.property("archivesName", archiveBaseName)
    from(licenseFile) {
        rename {
            "${it}_${archiveBaseName.get()}"
        }
    }
}

tasks.shadowJar {
    configurations.set(listOf(shadowBundle))
    archiveClassifier.set("")
    mergeServiceFiles()
    inputs.property("archivesName", archiveBaseName)
    from(licenseFile) {
        rename {
            "${it}_${archiveBaseName.get()}"
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    val values = mapOf(
        "version" to project.version.toString(),
        "minecraft_version" to libs.versions.minecraft.get(),
        "loader_version" to libs.versions.fabric.loader.get(),
        "fabric_version" to libs.versions.fabric.api.get(),
        "fabric_language_kotlin_version" to libs.versions.fabric.language.kotlin.get(),
        "architectury_version" to libs.versions.architectury.api.get()
    )
    values.forEach(inputs::property)
    filesMatching("fabric.mod.json") {
        expand(values)
    }
}

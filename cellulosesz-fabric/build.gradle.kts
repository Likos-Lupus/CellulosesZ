plugins {
    alias(libs.plugins.fabric.loom)
}


loom {
    mods {
        register("cellulosesz") {
            sourceSet(sourceSets["main"])
        }
    }
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)

    implementation(project(":cellulosesz-api"))
    implementation(project(":cellulosesz-core"))

    implementation(project(":cellulosesz-modules:cellulosesz-module-user"))
    implementation(project(":cellulosesz-modules:cellulosesz-module-command"))
    implementation(project(":cellulosesz-modules:cellulosesz-module-permission"))
    implementation(project(":cellulosesz-modules:cellulosesz-module-teleport"))
    implementation(project(":cellulosesz-modules:cellulosesz-module-home"))
    implementation(project(":cellulosesz-modules:cellulosesz-module-warp"))
    implementation(project(":cellulosesz-modules:cellulosesz-module-economy"))
    implementation(project(":cellulosesz-modules:cellulosesz-module-kit"))
    implementation(project(":cellulosesz-modules:cellulosesz-module-item"))
    implementation(project(":cellulosesz-modules:cellulosesz-module-messaging"))
    implementation(project(":cellulosesz-modules:cellulosesz-module-admin"))
    implementation(project(":cellulosesz-modules:cellulosesz-module-playerstate"))
    implementation(project(":cellulosesz-modules:cellulosesz-module-world"))
    implementation(project(":cellulosesz-modules:cellulosesz-module-sign"))
    implementation(project(":cellulosesz-modules:cellulosesz-module-text"))

    include(project(":cellulosesz-api"))
    include(project(":cellulosesz-core"))
    include(project(":cellulosesz-modules:cellulosesz-module-user"))
    include(project(":cellulosesz-modules:cellulosesz-module-command"))
    include(project(":cellulosesz-modules:cellulosesz-module-permission"))
    include(project(":cellulosesz-modules:cellulosesz-module-teleport"))
    include(project(":cellulosesz-modules:cellulosesz-module-home"))
    include(project(":cellulosesz-modules:cellulosesz-module-warp"))
    include(project(":cellulosesz-modules:cellulosesz-module-economy"))
    include(project(":cellulosesz-modules:cellulosesz-module-kit"))
    include(project(":cellulosesz-modules:cellulosesz-module-item"))
    include(project(":cellulosesz-modules:cellulosesz-module-messaging"))
    include(project(":cellulosesz-modules:cellulosesz-module-admin"))
    include(project(":cellulosesz-modules:cellulosesz-module-playerstate"))
    include(project(":cellulosesz-modules:cellulosesz-module-world"))
    include(project(":cellulosesz-modules:cellulosesz-module-sign"))
    include(project(":cellulosesz-modules:cellulosesz-module-text"))

    implementation(libs.jackson.databind)
    implementation(libs.jackson.yaml)
    implementation(libs.classgraph)

    include(libs.jackson.databind)
    include(libs.jackson.yaml)
    include(libs.classgraph)
}

tasks.named<ProcessResources>("processResources") {
    val modVersion = project.version.toString()
    val minecraftVersion = libs.versions.minecraft.get()
    val loaderVersion = libs.versions.fabric.loader.get()
    val fabricVersion = libs.versions.fabric.api.get()

    inputs.property("version", modVersion)
    inputs.property("minecraft_version", minecraftVersion)
    inputs.property("loader_version", loaderVersion)
    inputs.property("fabric_version", fabricVersion)

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to modVersion,
                "minecraft_version" to minecraftVersion,
                "loader_version" to loaderVersion,
                "fabric_version" to fabricVersion
            )
        )
    }
}

tasks.named<Jar>("jar") {
    val archiveBaseName = providers.gradleProperty("archives_base_name")
    inputs.property("archivesName", archiveBaseName)
    from(rootProject.file("LICENSE.txt")) {
        rename { "${it}_${archiveBaseName.get()}" }
    }
}

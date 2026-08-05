plugins {
    alias(libs.plugins.architectury.loom.no.remap)
    alias(libs.plugins.architectury.plugin)
}

architectury {
    common("fabric", "neoforge")
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

    implementation(libs.architectury.common)
    implementation(project(":cellulosesz-api"))
    implementation(project(":cellulosesz-core"))
    implementation(libs.jackson.databind)

    compileOnly(libs.luckperms.api)
}

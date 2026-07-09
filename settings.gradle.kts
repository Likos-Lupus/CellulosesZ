pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "CellulosesZ"

include(":cellulosesz-api")
include(":cellulosesz-core")
include(":cellulosesz-fabric")

include(":cellulosesz-modules:cellulosesz-module-user")
include(":cellulosesz-modules:cellulosesz-module-command")
include(":cellulosesz-modules:cellulosesz-module-permission")
include(":cellulosesz-modules:cellulosesz-module-teleport")
include(":cellulosesz-modules:cellulosesz-module-home")
include(":cellulosesz-modules:cellulosesz-module-warp")
include(":cellulosesz-modules:cellulosesz-module-economy")
include(":cellulosesz-modules:cellulosesz-module-kit")
include(":cellulosesz-modules:cellulosesz-module-item")
include(":cellulosesz-modules:cellulosesz-module-messaging")
include(":cellulosesz-modules:cellulosesz-module-admin")
include(":cellulosesz-modules:cellulosesz-module-playerstate")
include(":cellulosesz-modules:cellulosesz-module-world")
include(":cellulosesz-modules:cellulosesz-module-sign")
include(":cellulosesz-modules:cellulosesz-module-text")

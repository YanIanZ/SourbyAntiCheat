dependencyResolutionManagement {
    versionCatalogs {
        create("libs") { from(files("libs.versions.toml")) }
        create("testlibs") { from(files("testlibs.versions.toml")) }
    }
}
pluginManagement { repositories { gradlePluginPortal() } }
rootProject.name = "SourbyAntiCheat"
include("common")
include("bukkit")
include("bungee")
include("velocity")

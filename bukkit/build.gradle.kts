import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission

plugins {
    id("sac.base-conventions")
    id("sac.shadow-conventions")
    id("de.eldoria.plugin-yml.bukkit") version "0.8.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.grim.ac/snapshots") {
        content { includeGroup("ac.grim.grimac"); includeGroup("com.github.retrooper") }
    }
    maven("https://nexus.scarsz.me/content/repositories/releases") {
        content { includeGroup("github.scarsz") }
    }
    maven("https://repo.opencollab.dev/maven-releases/") {
        content { includeGroup("org.geysermc") }
    }
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        content { includeGroup("me.clip") }
    }
}

dependencies {
    implementation(project(":common"))
    implementation(libs.packetevents.spigot)
    implementation(libs.bundles.cloud.api)
    implementation(libs.cloud.paper)
    implementation("ac.grim.grimac:GrimAPI:1.4.0.0")
    implementation("ac.grim.grimac:grim-internal:1.4.0.0")
    implementation("ac.grim.grimac:grim-bukkit-internal:1.4.0.0")
    compileOnly(libs.paper.api)
    compileOnly(libs.placeholderapi)
    implementation(libs.adventure.platform.bukkit)
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

bukkit {
    name = "SourbyAntiCheat"
    main = "dev.yanianz.sourbyanticheat.platform.bukkit.SacBukkitLoaderPlugin"
    version = rootProject.version.toString()
    apiVersion = "1.21"
    foliaSupported = true
    authors = listOf("YanIanZ")

    softDepend = listOf("ProtocolLib", "Essentials", "ViaVersion", "ViaBackwards",
        "Geyser-Spigot", "floodgate", "PlaceholderAPI", "Spartan", "FastLogin",
        "driverholder-mysql", "driverholder-postgresql", "driverholder-mongodb", "SourbyCraft")

    permissions {
        register("sac.alerts") { description = "Receive SAC alerts"; default = Permission.Default.OP }
        register("sac.profile") { description = "View profiles"; default = Permission.Default.OP }
        register("sac.verbose") { description = "Toggle verbose"; default = Permission.Default.OP }
        register("sac.exempt") { description = "Exempt players"; default = Permission.Default.OP }
        register("sac.admin") { description = "Admin access"; default = Permission.Default.OP }
        register("sac.command") { description = "Base commands"; default = Permission.Default.OP }
        register("sac.audit.view") { description = "View audit logs"; default = Permission.Default.OP }
        register("sac.audit.export") { description = "Export audit logs"; default = Permission.Default.OP }
    }
}

tasks.shadowJar {
    archiveClassifier = ""
    archiveFileName = "SourbyAntiCheat.jar"
}
tasks.build { dependsOn(tasks.shadowJar) }
tasks.test { useJUnitPlatform() }

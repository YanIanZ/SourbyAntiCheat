plugins {
    id("sac.base-conventions")
    id("maven-publish")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.grim.ac/snapshots") {
        content { includeGroup("ac.grim.grimac"); includeGroup("com.github.retrooper") }
    }
    maven("https://repo.viaversion.com") {
        content { includeGroup("com.viaversion") }
    }
    maven("https://nexus.scarsz.me/content/repositories/releases") {
        content { includeGroup("github.scarsz") }
    }
    maven("https://repo.opencollab.dev/maven-releases/") {
        content { includeGroup("org.geysermc") }
    }
    maven("https://repo.opencollab.dev/maven-snapshots/") {
        content { includeGroup("org.geysermc.floodgate") }
    }
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        content { includeGroup("me.clip") }
    }
}

dependencies {
    api(libs.packetevents.api)
    api(libs.bundles.cloud.api)
    api(libs.configuralize) {
        artifact { classifier = "slim" }
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    api(libs.snakeyaml)
    api(libs.fastutil)
    api(libs.hikaricp)
    api(libs.netty)
    api(libs.jetbrains.annotations)
    api(libs.adventure.text.minimessage)

    implementation("com.google.guava:guava:33.4.8-jre")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("ac.grim.grimac:GrimAPI:1.4.0.0")
    implementation("ac.grim.grimac:grim-internal:1.4.0.0")
    implementation("ac.grim.grimac:grim-internal-shims:1.4.0.0")

    compileOnly(libs.paper.api)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.viaversion)
    compileOnly(libs.viabackwards)
    // SpartanAPI classes are built-in at me.vagdedes.spartan.api.*
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.16.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.yanianz"
            artifactId = "sourbyanticheat-common"
            version = project.version as String
            from(components["java"])
        }
    }
}

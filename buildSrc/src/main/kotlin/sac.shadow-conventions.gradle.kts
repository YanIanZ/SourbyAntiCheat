import versioning.BuildConfig

plugins {
    id("com.gradleup.shadow")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
    if (BuildConfig.relocate) {
        relocate("com.github.retrooper.packetevents", "dev.yanianz.sourbyanticheat.shaded.packetevents")
        relocate("io.github.retrooper.packetevents", "dev.yanianz.sourbyanticheat.shaded.packetevents")
        relocate("github.scarsz.configuralize", "dev.yanianz.sourbyanticheat.shaded.configuralize")
        relocate("it.unimi.dsi.fastutil", "dev.yanianz.sourbyanticheat.shaded.fastutil")
        relocate("org.yaml.snakeyaml", "dev.yanianz.sourbyanticheat.shaded.snakeyaml")
        relocate("org.incendo.cloud", "dev.yanianz.sourbyanticheat.shaded.cloud")
        relocate("net.kyori", "dev.yanianz.sourbyanticheat.shaded.kyori")
        relocate("org.intellij", "dev.yanianz.sourbyanticheat.shaded.intellij")
        relocate("org.jetbrains", "dev.yanianz.sourbyanticheat.shaded.jetbrains")
        relocate("com.google", "dev.yanianz.sourbyanticheat.shaded.google")
    }
    exclude("META-INF/**")
}

// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.manager.init.start;

import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import dev.yanianz.sourbyanticheat.utils.reflection.GeyserUtil;
import dev.yanianz.sourbyanticheat.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class PacketEventsCheck implements StartableInitable {

    @Override
    public void start() {
        String serverBrand = getServerBrand();
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();

        LogUtil.info("Server: " + serverBrand + " (PE: " + version.getReleaseName() + " proto " + version.getProtocolVersion() + ")");
        LogUtil.info("ViaVersion: " + (ViaVersionUtil.isAvailable ? "DETECTED" : "not found"));
        LogUtil.info("GeyserMC: " + (GeyserUtil.isAvailable() ? "DETECTED" : "not found"));

        if (version.isOlderThan(ServerVersion.V_1_8)) {
            LogUtil.error("SAC requires Minecraft 1.8+. Got " + version.getReleaseName());
        }

        if (serverBrand.contains("SourbyCraft") || serverBrand.contains("Unknown")) {
            LogUtil.warn("Custom server software detected (" + serverBrand + ").");
            LogUtil.warn("PacketEvents compatibility may be limited on non-Paper forks.");
        }

        try {
            Class.forName("dev.yanianz.sourbyanticheat.shaded.google.gson.Gson");
        } catch (ClassNotFoundException e) {
            LogUtil.error("Gson shading broken — rebuild required.");
        }

        try {
            var pdm = dev.yanianz.sourbyanticheat.SacAPI.INSTANCE.getPlayerDataManager();
            LogUtil.info("SAC initialized — " + dev.yanianz.sourbyanticheat.SacAPI.INSTANCE.getPlatform().name());
        } catch (Exception ignored) {}
    }

    private static String getServerBrand() {
        try {
            return org.bukkit.Bukkit.getServer().getName() + " " + org.bukkit.Bukkit.getVersion();
        } catch (Exception e) {
            return "Unknown";
        }
    }
}

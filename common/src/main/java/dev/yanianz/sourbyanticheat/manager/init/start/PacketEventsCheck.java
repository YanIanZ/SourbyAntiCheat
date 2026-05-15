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
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();

        if (version.isOlderThan(ServerVersion.V_1_8)) {
            LogUtil.error("SAC requires Minecraft 1.8+. Detected: " + version.getReleaseName());
        }

        if (version.getProtocolVersion() >= 769) {
            LogUtil.warn("SAC running on " + version.getReleaseName() + " — untested version, report issues on GitHub.");
        }

        LogUtil.info("PacketEvents: " + version.getReleaseName() + " (protocol " + version.getProtocolVersion() + ")");
        LogUtil.info("ViaVersion: " + (ViaVersionUtil.isAvailable ? "DETECTED" : "not found"));
        LogUtil.info("GeyserMC/Floodgate: " + (GeyserUtil.isAvailable() ? "DETECTED (Bedrock support active)" : "not found"));

        try {
            var pdm = dev.yanianz.sourbyanticheat.SacAPI.INSTANCE.getPlayerDataManager();
            LogUtil.info("SAC initialized — platform: " + dev.yanianz.sourbyanticheat.SacAPI.INSTANCE.getPlatform().name());
        } catch (Exception ignored) {}

        try {
            Class.forName("dev.yanianz.sourbyanticheat.shaded.google.gson.Gson");
        } catch (ClassNotFoundException e) {
            LogUtil.error("Gson shading failed — SAC may crash on PacketEvents init. Please rebuild.");
        }
    }
}

// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.reflection.GeyserUtil;
import dev.yanianz.sourbyanticheat.utils.viaversion.ViaVersionUtil;
import net.kyori.adventure.text.Component;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import net.kyori.adventure.text.format.TextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

public class SacStatus implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("status", Description.of("Show SAC health status"))
                        .permission("sac.alerts")
                        .handler(this::handleStatus)
        );
    }

    private void handleStatus(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        var api = SacAPI.INSTANCE;

        boolean nettyFailed = dev.yanianz.sourbyanticheat.netty.SacNettyInjector.isInjectionFailed();
        boolean geyser = GeyserUtil.isAvailable();
        boolean viaversion = ViaVersionUtil.isAvailable;
        boolean spartan = SpartanCrossCheck.isAvailable();

        sender.sendMessage(Component.text("=== SAC Status ===", SacColors.GOLD));
        sender.sendMessage(line("Version", "1.0.0"));
        sender.sendMessage(line("Uptime", formatUptime(api.getUptime())));
        sender.sendMessage(line("Platform", api.getPlatform().name()));
        sender.sendMessage(line("Tick", String.valueOf(api.getTickManager().currentTick)));
        sender.sendMessage(line("Netty", nettyFailed ? "FAILED (PacketEvents)" : "ACTIVE", nettyFailed ? SacColors.RED : SacColors.GREEN));
        sender.sendMessage(line("ViaVersion", viaversion ? "DETECTED" : "NOT FOUND", viaversion ? SacColors.GREEN : SacColors.GRAY));
        sender.sendMessage(line("GeyserMC", geyser ? "DETECTED" : "NOT FOUND", geyser ? SacColors.GREEN : SacColors.GRAY));
        sender.sendMessage(line("SpartanAPI", spartan ? "ACTIVE v" + (SpartanCrossCheck.getSpartanVersion() != null ? SpartanCrossCheck.getSpartanVersion() : "?") : "DISABLED", spartan ? SacColors.GREEN : SacColors.GRAY));
        if (spartan) {
            sender.sendMessage(line("  Agree", SpartanCrossCheck.getAgreements() + "/" + SpartanCrossCheck.getTotalFlags()
                + " (" + String.format("%.0f%%", SpartanCrossCheck.getAgreementRate() * 100) + ")"));
        }

        var pdm = api.getPlayerDataManager();
        int tracked = pdm.getEntries().size();
        int checks = tracked > 0 ? pdm.getEntries().iterator().next().checkManager.allChecks.size() : 0;
        int active = tracked > 0 ? (int) pdm.getEntries().iterator().next().checkManager.allChecks.values().stream()
            .filter(c -> ((dev.yanianz.sourbyanticheat.checks.Check)c).isEnabled()).count() : 0;
        sender.sendMessage(line("Tracked", String.valueOf(tracked)));
        sender.sendMessage(line("Checks", active + "/" + checks + " active"));
    }

    private static Component line(String key, String value) {
        return line(key, value, SacColors.WHITE);
    }

    private static Component line(String key, String value, TextColor color) {
        return Component.text()
                .append(Component.text("  " + key + ": ", SacColors.GRAY))
                .append(Component.text(value, color))
                .build();
    }

    private static String formatUptime(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60;
        if (h > 0) return h + "h " + (m % 60) + "m";
        if (m > 0) return m + "m " + (s % 60) + "s";
        return s + "s";
    }
}

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
                        .permission("sac.status")
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

        var pdm = api.getPlayerDataManager();
        int tracked = pdm.getEntries().size();
        int checks = tracked > 0 ? pdm.getEntries().iterator().next().checkManager.allChecks.size() : 0;
        int active = tracked > 0 ? (int) pdm.getEntries().iterator().next().checkManager.allChecks.values().stream()
            .filter(c -> ((dev.yanianz.sourbyanticheat.checks.Check)c).isEnabled()).count() : 0;

        sender.sendMessage(SacColors.spacer());
        sender.sendMessage(SacColors.header("SAC Health Dashboard"));
        sender.sendMessage(SacColors.spacer());

        // ── Core Info ─────────────────
        sender.sendMessage(SacColors.subHeader("Core"));
        sender.sendMessage(SacColors.kv("Version", SacAPI.INSTANCE.getExternalAPI().getSacVersion(), SacColors.ACCENT));
        sender.sendMessage(SacColors.kv("Platform", api.getPlatform().name(), SacColors.WHITE));
        sender.sendMessage(SacColors.kv("Uptime", formatUptime(api.getUptime()), SacColors.WHITE));
        sender.sendMessage(SacColors.kv("Tick", String.valueOf(api.getTickManager().currentTick), SacColors.DARK_GRAY));
        sender.sendMessage(SacColors.spacer());

        // ── Services ──────────────────
        sender.sendMessage(SacColors.subHeader("Services"));
        String nettyLabel = nettyFailed
            ? "Netty " + SacColors.DASH + " PacketEvents fallback"
            : "Netty " + SacColors.DASH + " " + dev.yanianz.sourbyanticheat.netty.SacNettyInjector.getSuccessCount() + " injected";
        sender.sendMessage(SacColors.statusBadge(nettyLabel, !nettyFailed));
        sender.sendMessage(SacColors.statusBadge("ViaVersion", viaversion));
        sender.sendMessage(SacColors.statusBadge("GeyserMC", geyser));

        String spartanLabel = spartan
            ? "SpartanAPI v" + (SpartanCrossCheck.getSpartanVersion() != null ? SpartanCrossCheck.getSpartanVersion() : "?")
            : "SpartanAPI";
        sender.sendMessage(SacColors.statusBadge(spartanLabel, spartan));

        if (spartan && SpartanCrossCheck.getTotalFlags() > 0) {
            double rate = SpartanCrossCheck.getAgreementRate();
            sender.sendMessage(Component.text()
                .append(Component.text("     Agreement  ", SacColors.DARK_GRAY))
                .append(SacColors.progressBar(rate, 16))
                .build());
        }
        sender.sendMessage(SacColors.spacer());

        // ── Tracking ──────────────────
        sender.sendMessage(SacColors.subHeader("Tracking"));
        sender.sendMessage(SacColors.kv("Players", String.valueOf(tracked), tracked > 0 ? SacColors.GREEN : SacColors.GRAY));
        sender.sendMessage(SacColors.kv("Checks", active + "/" + checks + " active",
            active == checks ? SacColors.GREEN : SacColors.YELLOW));
        sender.sendMessage(SacColors.spacer());
        sender.sendMessage(SacColors.footer());
    }

    private static String formatUptime(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60, d = h / 24;
        if (d > 0) return d + "d " + (h % 24) + "h " + (m % 60) + "m";
        if (h > 0) return h + "h " + (m % 60) + "m";
        if (m > 0) return m + "m " + (s % 60) + "s";
        return s + "s";
    }
}

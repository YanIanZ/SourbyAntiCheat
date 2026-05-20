// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.command.PlayerSelector;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.player.PlatformPlayer;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SacSpartan implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("spartan", Description.of("View Spartan cross-check stats"))
                        .permission("sac.spartan")
                        .required("target", adapter.singlePlayerSelectorParser())
                        .handler(this::handleSpartan)
        );
    }

    private void handleSpartan(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector target = context.get("target");
        PlatformPlayer tp = target.getSinglePlayer().getPlatformPlayer();
        if (tp == null) return;

        UUID uuid = tp.getUniqueId();
        SpartanCrossCheck.CrossCheckStats stats = SpartanCrossCheck.getStats(uuid);
        boolean available = SpartanCrossCheck.isAvailable();

        sender.sendMessage(SacColors.spacer());
        sender.sendMessage(SacColors.header("Spartan Cross-Check"));
        sender.sendMessage(SacColors.spacer());

        sender.sendMessage(SacColors.statusBadge("SpartanAPI", available));
        sender.sendMessage(SacColors.kv("Player", tp.getName(), SacColors.ACCENT));
        sender.sendMessage(SacColors.kv("Min-VL Threshold", String.valueOf(SpartanCrossCheck.getMinVL()), SacColors.DARK_GRAY));
        sender.sendMessage(SacColors.spacer());

        if (stats.agreements > 0 || stats.disagreements > 0) {
            sender.sendMessage(SacColors.subHeader("Correlation"));
            sender.sendMessage(SacColors.kv(SacColors.CHECKMARK + " Agreements", String.valueOf(stats.agreements), SacColors.GREEN));
            sender.sendMessage(SacColors.kv(SacColors.CROSS + " Disagreements", String.valueOf(stats.disagreements), SacColors.RED));
            sender.sendMessage(Component.text()
                .append(Component.text("   Rate  ", SacColors.GRAY))
                .append(SacColors.progressBar(stats.agreementRate(), 20))
                .build());
        } else {
            sender.sendMessage(Component.text("   No cross-check data for this player yet.", SacColors.DARK_GRAY));
        }

        sender.sendMessage(SacColors.spacer());
        sender.sendMessage(SacColors.footer());
    }
}

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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
                        .permission("sac.alerts")
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

        sender.sendMessage(Component.text("--- SAC-Spartan Cross-Check ---", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("SpartanAPI: " + (available ? "ENABLED" : "DISABLED"), available ? NamedTextColor.GREEN : NamedTextColor.RED));
        sender.sendMessage(Component.text("Player: " + tp.getName(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Agreements: " + stats.agreements, NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Disagreements: " + stats.disagreements, NamedTextColor.RED));
        sender.sendMessage(Component.text("Agreement Rate: " + String.format("%.1f%%", stats.agreementRate() * 100), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Min-VL threshold: " + SpartanCrossCheck.getMinVL(), NamedTextColor.GRAY));
    }
}

// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.command.CommandUtils;
import dev.yanianz.sourbyanticheat.platform.api.command.PlayerSelector;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.player.PlatformPlayer;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.NotNull;

public class SacReset implements BuildableCommand {
    private static SuggestionProvider<Sender> CHECK_SUGGESTIONS;

    private static SuggestionProvider<Sender> getCheckSuggestions() {
        if (CHECK_SUGGESTIONS != null) return CHECK_SUGGESTIONS;
        var pdm = SacAPI.INSTANCE.getPlayerDataManager();
        if (pdm.getEntries().isEmpty()) return (ctx, input) -> java.util.concurrent.CompletableFuture.completedFuture(java.util.Collections.emptyList());
        var player = pdm.getEntries().iterator().next();
        var names = player.checkManager.allChecks.values().stream()
            .filter(c -> ((Check)c).getCheckName() != null)
            .map(c -> ((Check)c).getCheckName())
            .sorted()
            .toArray(String[]::new);
        CHECK_SUGGESTIONS = CommandUtils.fromStrings(names);
        return CHECK_SUGGESTIONS;
    }

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("reset", Description.of("Reset a player's violations"))
                        .permission("sac.admin")
                        .required("target", adapter.singlePlayerSelectorParser())
                        .handler(this::handleReset)
        );

        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("reset", Description.of("Reset a specific check's violations"))
                        .permission("sac.admin")
                        .required("target", adapter.singlePlayerSelectorParser())
                        .required("check", StringParser.stringParser(), getCheckSuggestions())
                        .handler(this::handleResetCheck)
        );
    }

    private void handleReset(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector target = context.get("target");
        PlatformPlayer tp = target.getSinglePlayer().getPlatformPlayer();
        if (tp == null) return;

        SacPlayer player = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(tp.getUniqueId());
        if (player == null) {
            sender.sendMessage(Component.text("Player not tracked by SAC.", NamedTextColor.RED));
            return;
        }

        int count = 0;
        for (var entry : player.checkManager.allChecks.entrySet()) {
            var check = (Check) entry.getValue();
            check.violations = 0;
            count++;
        }

        sender.sendMessage(Component.text("Reset " + count + " checks for " + tp.getName() + ".", NamedTextColor.GREEN));
        LogUtil.info("SAC reset: " + sender.getName() + " reset all VLs for " + tp.getName());
    }

    private void handleResetCheck(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector target = context.get("target");
        String checkName = context.get("check");
        PlatformPlayer tp = target.getSinglePlayer().getPlatformPlayer();
        if (tp == null) return;

        SacPlayer player = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(tp.getUniqueId());
        if (player == null) {
            sender.sendMessage(Component.text("Player not tracked by SAC.", NamedTextColor.RED));
            return;
        }

        for (var entry : player.checkManager.allChecks.entrySet()) {
            var check = (Check) entry.getValue();
            if (check.getCheckName() != null && check.getCheckName().equalsIgnoreCase(checkName)) {
                double oldVL = check.violations;
                check.violations = 0;
                sender.sendMessage(Component.text("Reset " + check.getCheckName() + " for " + tp.getName()
                        + " (was VL=" + String.format("%.1f", oldVL) + ").", NamedTextColor.GREEN));
                LogUtil.info("SAC reset: " + sender.getName() + " reset " + check.getCheckName()
                        + " VL for " + tp.getName() + " (was " + String.format("%.1f", oldVL) + ")");
                return;
            }
        }
        sender.sendMessage(Component.text("Check '" + checkName + "' not found.", NamedTextColor.RED));
    }
}

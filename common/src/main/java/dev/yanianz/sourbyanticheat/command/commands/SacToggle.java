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
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.NotNull;

public class SacToggle implements BuildableCommand {
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
                        .literal("toggle", Description.of("Enable/disable a check for a player"))
                        .permission("sac.admin")
                        .required("check_name", org.incendo.cloud.parser.standard.StringParser.stringParser(), getCheckSuggestions())
                        .required("target", adapter.singlePlayerSelectorParser())
                        .handler(this::handleToggle)
        );
    }

    private void handleToggle(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        String checkName = context.get("check_name");
        PlayerSelector target = context.get("target");
        PlatformPlayer tp = target.getSinglePlayer().getPlatformPlayer();
        if (tp == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }

        SacPlayer player = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(tp.getUniqueId());
        if (player == null) {
            sender.sendMessage(Component.text("Player is not tracked by SAC.", NamedTextColor.RED));
            return;
        }

        Check found = null;
        for (var entry : player.checkManager.allChecks.entrySet()) {
            if (entry.getValue().getCheckName() != null
                    && entry.getValue().getCheckName().equalsIgnoreCase(checkName)) {
                found = (Check) entry.getValue();
                break;
            }
        }

        if (found == null) {
            sender.sendMessage(Component.text("Check '" + checkName + "' not found.", NamedTextColor.RED));
            sender.sendMessage(Component.text("Use /sac list to see available checks.", NamedTextColor.GRAY));
            return;
        }

        boolean newState = !found.isEnabled();
        found.setEnabled(newState);

        sender.sendMessage(Component.text()
                .append(Component.text("Check ", NamedTextColor.GRAY))
                .append(Component.text(found.getCheckName(), NamedTextColor.YELLOW))
                .append(Component.text(" for ", NamedTextColor.GRAY))
                .append(Component.text(tp.getName(), NamedTextColor.WHITE))
                .append(Component.text(" → ", NamedTextColor.GRAY))
                .append(Component.text(newState ? "ENABLED" : "DISABLED",
                        newState ? NamedTextColor.GREEN : NamedTextColor.RED))
                .build());

        LogUtil.info("SAC toggle: " + sender.getName() + " set " + found.getCheckName()
                + " → " + (newState ? "enabled" : "disabled") + " for " + tp.getName());
    }
}

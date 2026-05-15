package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SacSummary implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("summary", Description.of("Show violation summary"))
                        .permission("sac.alerts")
                        .handler(this::handleSummary)
        );
    }

    private void handleSummary(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        var pdm = SacAPI.INSTANCE.getPlayerDataManager();
        var entries = pdm.getEntries();

        if (entries.isEmpty()) {
            sender.sendMessage(Component.text("No tracked players.", SacColors.GRAY));
            return;
        }

        Map<String, Double> checkVLs = new HashMap<>();
        Map<String, Integer> playerVLs = new HashMap<>();

        for (var sp : entries) {
            int total = 0;
            for (var entry : sp.checkManager.allChecks.entrySet()) {
                Check c = (Check) entry.getValue();
                if (c.getCheckName() == null) continue;
                total += c.violations;
                checkVLs.merge(c.getCheckName(), c.violations, Double::sum);
            }
            playerVLs.put(sp.getName(), total);
        }

        sender.sendMessage(Component.text("=== SAC Violation Summary ===", SacColors.GOLD));

        sender.sendMessage(Component.text("Top Checks:", SacColors.CYAN));
        checkVLs.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .forEach(e -> sender.sendMessage(Component.text()
                .append(Component.text("  " + e.getKey() + " ", SacColors.WHITE))
                .append(Component.text(String.format("%.0f VL", e.getValue()), SacColors.YELLOW))
                .build()));

        sender.sendMessage(Component.text("Top Players:", SacColors.CYAN));
        playerVLs.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .forEach(e -> sender.sendMessage(Component.text()
                .append(Component.text("  " + e.getKey() + " ", SacColors.WHITE))
                .append(Component.text(e.getValue() + " VL", e.getValue() > 50 ? SacColors.RED : SacColors.YELLOW))
                .build()));

        int totalChecks = checkVLs.size();
        double totalVL = checkVLs.values().stream().mapToDouble(Double::doubleValue).sum();
        sender.sendMessage(Component.text()
            .append(Component.text("Total: ", SacColors.GRAY))
            .append(Component.text(String.format("%.0f VL", totalVL), SacColors.GREEN))
            .append(Component.text(" across ", SacColors.GRAY))
            .append(Component.text(totalChecks + " checks", SacColors.WHITE))
            .append(Component.text(" for ", SacColors.GRAY))
            .append(Component.text(entries.size() + " players", SacColors.WHITE))
            .build());
    }
}

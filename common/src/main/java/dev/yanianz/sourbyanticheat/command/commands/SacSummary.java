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
import java.util.concurrent.atomic.AtomicInteger;

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
            sender.sendMessage(Component.text("   No tracked players.", SacColors.DARK_GRAY));
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

        double maxCheckVL = checkVLs.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        int maxPlayerVL = playerVLs.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        sender.sendMessage(SacColors.spacer());
        sender.sendMessage(SacColors.header("Violation Summary"));
        sender.sendMessage(SacColors.spacer());

        // ── Top Checks ──
        sender.sendMessage(SacColors.subHeader("Hottest Checks"));
        AtomicInteger checkRank = new AtomicInteger(1);
        checkVLs.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .forEach(e -> {
                int r = checkRank.getAndIncrement();
                sender.sendMessage(Component.text()
                    .append(SacColors.rank(r))
                    .append(Component.text(e.getKey() + " ", SacColors.WHITE))
                    .append(Component.text(String.format("%.0f", e.getValue()), SacColors.vlColor(e.getValue())))
                    .append(Component.text("  ", SacColors.MUTED))
                    .append(SacColors.progressBar(e.getValue() / maxCheckVL, 12))
                    .build());
            });
        sender.sendMessage(SacColors.spacer());

        // ── Top Players ──
        sender.sendMessage(SacColors.subHeader("Highest VL Players"));
        AtomicInteger playerRank = new AtomicInteger(1);
        playerVLs.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .forEach(e -> {
                int r = playerRank.getAndIncrement();
                sender.sendMessage(Component.text()
                    .append(SacColors.rank(r))
                    .append(Component.text(e.getKey() + " ", SacColors.WHITE))
                    .append(Component.text(e.getValue() + " VL", SacColors.vlColor(e.getValue())))
                    .append(Component.text("  ", SacColors.MUTED))
                    .append(SacColors.progressBar((double) e.getValue() / maxPlayerVL, 12))
                    .build());
            });
        sender.sendMessage(SacColors.spacer());

        // ── Footer Stats ──
        double totalVL = checkVLs.values().stream().mapToDouble(Double::doubleValue).sum();
        sender.sendMessage(Component.text()
            .append(Component.text("   " + SacColors.DIAMOND + " ", SacColors.ACCENT))
            .append(Component.text(String.format("%.0f VL", totalVL), SacColors.vlColor(totalVL)))
            .append(Component.text("  across ", SacColors.MUTED))
            .append(Component.text(checkVLs.size() + " checks", SacColors.DARK_GRAY))
            .append(Component.text("  for ", SacColors.MUTED))
            .append(Component.text(entries.size() + " players", SacColors.DARK_GRAY))
            .build());
        sender.sendMessage(SacColors.footer());
    }
}

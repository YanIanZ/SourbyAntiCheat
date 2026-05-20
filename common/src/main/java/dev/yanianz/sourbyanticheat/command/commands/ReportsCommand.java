package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.manager.ReportManager;
import dev.yanianz.sourbyanticheat.platform.api.command.PlayerSelector;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReportsCommand implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
            commandManager.commandBuilder("reports")
                .permission("sac.staff")
                .handler(this::handleList)
        );
        commandManager.command(
            commandManager.commandBuilder("reports")
                .literal("clear")
                .required("target", adapter.singlePlayerSelectorParser())
                .permission("sac.staff")
                .handler(this::handleClear)
        );
    }

    private void handleList(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        List<ReportManager.Report> reports = ReportManager.getAllReports();
        if (reports.isEmpty()) {
            sender.sendMessage(Component.text("No pending reports.", SacColors.ACCENT));
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");
        sender.sendMessage(Component.text("--- Pending Reports (" + reports.size() + ") ---", SacColors.HIGHLIGHT));
        for (ReportManager.Report r : reports) {
            String line = r.reporterName() + " -> " + r.targetName() + ": " + r.reason()
                + " (" + fmt.format(new Date(r.timestamp())) + ")";
            sender.sendMessage(Component.text("  " + line, SacColors.ACCENT));
        }
    }

    private void handleClear(@NotNull CommandContext<Sender> context) {
        PlayerSelector targetSel = context.getOrDefault("target", null);
        if (targetSel == null) return;
        Sender target = targetSel.getSinglePlayer();
        if (target == null) {
            context.sender().sendMessage(Component.text("Player not found.", SacColors.RED));
            return;
        }
        ReportManager.clearReports(target.getUniqueId());
        context.sender().sendMessage(Component.text("Reports cleared for " + target.getName() + ".", SacColors.ACCENT));
    }
}

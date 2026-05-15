package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.predictionengine.MovementCheckRunner;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

public class SacPerf {

    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        Command.Builder<Sender> grimCommand = commandManager.commandBuilder("sac", "sac");

        Command.Builder<Sender> configuredBuilder = grimCommand
                .literal("perf", "performance")
                .permission("sac.performance")
                .handler(this::handlePerformance);

        commandManager.command(configuredBuilder);
    }

    private void handlePerformance(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        double millis = MovementCheckRunner.predictionNanos / 1000000;
        double longMillis = MovementCheckRunner.longPredictionNanos / 1000000;

        // Determine color based on performance
        var shortColor = millis < 1.0 ? SacColors.GREEN : millis < 3.0 ? SacColors.YELLOW : SacColors.RED;
        var longColor = longMillis < 1.0 ? SacColors.GREEN : longMillis < 3.0 ? SacColors.YELLOW : SacColors.RED;

        sender.sendMessage(SacColors.spacer());
        sender.sendMessage(SacColors.header("Performance"));
        sender.sendMessage(SacColors.spacer());

        sender.sendMessage(SacColors.subHeader("Prediction Engine"));
        sender.sendMessage(Component.text()
            .append(Component.text("   avg/500   ", SacColors.GRAY))
            .append(Component.text(String.format("%.3f", millis), shortColor))
            .append(Component.text(" ms", SacColors.DARK_GRAY))
            .append(Component.text("  ", SacColors.MUTED))
            .append(SacColors.progressBar(Math.min(millis / 5.0, 1.0), 12))
            .build());

        sender.sendMessage(Component.text()
            .append(Component.text("   avg/20k   ", SacColors.GRAY))
            .append(Component.text(String.format("%.3f", longMillis), longColor))
            .append(Component.text(" ms", SacColors.DARK_GRAY))
            .append(Component.text("  ", SacColors.MUTED))
            .append(SacColors.progressBar(Math.min(longMillis / 5.0, 1.0), 12))
            .build());

        sender.sendMessage(SacColors.spacer());
        sender.sendMessage(SacColors.footer());
    }
}

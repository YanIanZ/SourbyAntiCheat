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
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

public class ReportCommand implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
            commandManager.commandBuilder("report")
                .required("target", adapter.singlePlayerSelectorParser())
                .required("reason", StringParser.greedyStringParser())
                .handler(this::handleReport)
        );
    }

    private void handleReport(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (!sender.isPlayer()) {
            sender.sendMessage(Component.text("Only players can use this command.", SacColors.RED));
            return;
        }

        PlayerSelector targetSel = context.getOrDefault("target", null);
        if (targetSel == null) return;

        Sender target = targetSel.getSinglePlayer();
        if (target == null || !target.isPlayer()) {
            sender.sendMessage(Component.text("Player not found.", SacColors.RED));
            return;
        }

        String reason = context.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            sender.sendMessage(Component.text("Please provide a reason.", SacColors.RED));
            return;
        }

        ReportManager.ReportResult result = ReportManager.fileReport(
            sender.getUniqueId(), sender.getName(),
            target.getUniqueId(), target.getName(), reason);

        sender.sendMessage(Component.text(result.message(), result.success() ? SacColors.ACCENT : SacColors.RED));
    }
}

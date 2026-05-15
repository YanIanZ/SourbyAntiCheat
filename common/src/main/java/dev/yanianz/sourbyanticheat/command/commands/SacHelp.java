package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class SacHelp implements BuildableCommand {

    private static final Map<String, String> COMMANDS = new LinkedHashMap<>() {{
        put("alerts", "Toggle alert notifications");
        put("brands", "View client brands");
        put("debug", "Debug mode toggle");
        put("dump", "Export debug log");
        put("gui", "Open control panel GUI");
        put("help", "Show this help");
        put("history", "View violation history");
        put("list players", "List tracked players");
        put("list checks", "List active checks + VLs");
        put("log", "Upload debug log");
        put("perf", "Performance stats");
        put("profile <player>", "View player profile");
        put("reload", "Reload configuration");
        put("reset <player>", "Reset player violations");
        put("sendalert", "Send test alert");
        put("spartan <player>", "SpartanAPI cross-check stats");
        put("spectate <player>", "Spectate a player");
        put("status", "System health status");
        put("stopspectating", "Stop spectating");
        put("testwebhook", "Test Discord webhook");
        put("toggle <check> <player>", "Enable/disable check");
        put("verbose", "Toggle verbose mode");
        put("version", "Version + update check");
    }};

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("help", Description.of("Display help information"))
                        .permission("sac.help")
                        .handler(this::handleHelp)
        );
    }

    private void handleHelp(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        sender.sendMessage(Component.text("=== SAC Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Use /sac <command> or click below:", NamedTextColor.GRAY));

        for (var entry : COMMANDS.entrySet()) {
            sender.sendMessage(Component.text()
                .append(Component.text("  /sac ", NamedTextColor.GRAY))
                .append(Component.text(entry.getKey(), NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.suggestCommand("/sac " + entry.getKey().split(" ")[0])))
                .append(Component.text(" — " + entry.getValue(), NamedTextColor.GRAY))
                .build());
        }
    }
}

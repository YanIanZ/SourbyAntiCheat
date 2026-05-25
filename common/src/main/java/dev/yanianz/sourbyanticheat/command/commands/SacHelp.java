package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class SacHelp implements BuildableCommand {

    /** Curated to the core command surface. Legacy commands are hidden behind
     *  commands.legacy-enabled and intentionally not listed here. */
    public static final Map<String, String[]> COMMAND_GROUPS = new LinkedHashMap<>() {{
        put("Monitoring", new String[]{
            "alerts|Toggle alert notifications",
            "verbose|Toggle verbose mode",
            "status|System health dashboard",
            "top|Top 10 offenders by VL"
        });
        put("Player Intel", new String[]{
            "info <player>|Detailed violation breakdown",
            "profile <player>|View player profile card",
            "history <player>|View violation history log",
            "brands|Toggle client brand display",
            "list players|List tracked players",
            "list checks|List active checks + VLs"
        });
        put("Check Management", new String[]{
            "toggle <check> <player>|Enable/disable check for player",
            "reset <player>|Reset player violations",
            "gui|Open control panel GUI"
        });
        put("Administration", new String[]{
            "exempt <player>|Toggle player exemption",
            "reload|Reload configuration",
            "note <player> <msg>|Add staff note"
        });
        put("Integration", new String[]{
            "spectate <player>|Spectate a player",
            "stopspectating [here]|Stop spectating",
            "version|Version + update check"
        });
    }};

    /** Single source of truth for rendering the help page. Used by the
     *  {@code /sac help} command and the bare {@code /sac} root handler. */
    public static void renderTo(@NotNull Sender sender) {
        sender.sendMessage(SacColors.spacer());
        sender.sendMessage(SacColors.header("SAC Command Reference"));
        sender.sendMessage(SacColors.spacer());

        for (var group : COMMAND_GROUPS.entrySet()) {
            sender.sendMessage(SacColors.subHeader(group.getKey()));
            for (String cmdLine : group.getValue()) {
                String[] parts = cmdLine.split("\\|", 2);
                sender.sendMessage(SacColors.cmdEntry(parts[0], parts.length > 1 ? parts[1] : ""));
            }
            sender.sendMessage(SacColors.spacer());
        }

        sender.sendMessage(Component.text()
            .append(Component.text("  Tip: ", SacColors.HIGHLIGHT))
            .append(Component.text("Click any command above to auto-fill it", SacColors.GRAY))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("  Admin/diagnostic commands hidden — set ", SacColors.GRAY))
            .append(Component.text("commands.legacy-enabled: true", SacColors.HIGHLIGHT))
            .build());
        sender.sendMessage(SacColors.footer());
    }

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
        renderTo(context.sender());
    }
}

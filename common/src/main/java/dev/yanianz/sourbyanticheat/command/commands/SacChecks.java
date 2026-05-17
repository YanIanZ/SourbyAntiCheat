package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

public class SacChecks implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("checks", Description.of("List all checks (opens GUI for players)"))
                        .permission("sac.list")
                        .handler(this::handleChecks)
        );
    }

    private void handleChecks(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        if (sender.isPlayer()) {
            try {
                Class<?> guiClass = Class.forName("dev.yanianz.sourbyanticheat.platform.bukkit.gui.SacGUI");
                var platformPlayer = sender.getPlatformPlayer();
                if (platformPlayer != null) {
                    try {
                        org.bukkit.entity.Player player = (org.bukkit.entity.Player) platformPlayer.getClass().getMethod("getPlayer").invoke(platformPlayer);
                        if (player != null) {
                            guiClass.getMethod("openMain", org.bukkit.entity.Player.class).invoke(null, player);
                            return;
                        }
                    } catch (Exception ignored) {}
                }
            } catch (ClassNotFoundException ignored) {}
        }

        var pdm = SacAPI.INSTANCE.getPlayerDataManager();
        if (pdm.getEntries().isEmpty()) {
            sender.sendMessage(Component.text("   No tracked players - checks load per-player.", SacColors.DARK_GRAY));
            return;
        }
        var player = pdm.getEntries().iterator().next();
        var checks = player.checkManager.allChecks;

        long enabled = checks.values().stream().filter(c -> ((Check) c).isEnabled()).count();

        sender.sendMessage(SacColors.spacer());
        sender.sendMessage(SacColors.header("Check Registry"));
        sender.sendMessage(Component.text()
            .append(Component.text("   ", SacColors.MUTED))
            .append(Component.text(enabled + " active", SacColors.GREEN))
            .append(Component.text(" / " + checks.size() + " total", SacColors.DARK_GRAY))
            .build());
        sender.sendMessage(SacColors.spacer());

        checks.values().stream()
            .filter(c -> c.getCheckName() != null)
            .sorted((a, b) -> a.getCheckName().compareToIgnoreCase(b.getCheckName()))
            .forEach(c -> {
                var check = (Check) c;
                double vl = check.violations;
                sender.sendMessage(Component.text()
                    .append(Component.text("   ", SacColors.MUTED))
                    .append(Component.text(check.isEnabled() ? SacColors.DOT : SacColors.CROSS,
                        check.isEnabled() ? SacColors.GREEN : SacColors.RED))
                    .append(Component.text(" " + check.getCheckName() + " ", check.isEnabled() ? SacColors.WHITE : SacColors.DARK_GRAY))
                    .append(vl > 0.5 ? Component.text(String.valueOf((int) vl), SacColors.vlColor(vl)) : Component.empty())
                    .append(check.isExperimental() ? Component.text(" *", SacColors.PURPLE) : Component.empty())
                    .clickEvent(ClickEvent.suggestCommand("/sac toggle " + check.getCheckName() + " "))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to toggle " + check.getCheckName(), SacColors.GRAY)))
                    .build());
            });
        sender.sendMessage(SacColors.footer());
    }
}

package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.command.PlayerSelector;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.player.PlatformPlayer;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;

public class SacInfo implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("info", Description.of("Show detailed player violation info"))
                        .permission("sac.profile")
                        .required("target", adapter.singlePlayerSelectorParser())
                        .handler(this::handleInfo)
        );
    }

    private void handleInfo(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector target = context.get("target");
        PlatformPlayer tp = target.getSinglePlayer().getPlatformPlayer();
        if (tp == null) return;

        SacPlayer sp = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(tp.getUniqueId());
        if (sp == null) {
            sender.sendMessage(Component.text("  " + SacColors.CROSS + " Player not tracked by SAC.", SacColors.RED));
            return;
        }

        sender.sendMessage(SacColors.spacer());
        sender.sendMessage(SacColors.header(sp.getName() + " " + SacColors.DASH + " Violations"));
        sender.sendMessage(SacColors.spacer());

        var checks = new ArrayList<>(sp.checkManager.allChecks.values());
        checks.sort(Comparator.comparingDouble(c -> -((Check) c).violations));

        int shown = 0;
        for (var entry : checks) {
            Check c = (Check) entry;
            if (c.getCheckName() == null || c.violations < 0.5) continue;
            if (shown++ > 15) {
                sender.sendMessage(Component.text("   ... and more (use /sac gui)", SacColors.DARK_GRAY));
                break;
            }

            double vl = c.violations;
            Component line = Component.text()
                .append(Component.text("   ", SacColors.MUTED))
                .append(Component.text(c.isEnabled() ? SacColors.DOT : SacColors.CROSS, c.isEnabled() ? SacColors.vlColor(vl) : SacColors.RED))
                .append(Component.text(" " + c.getCheckName() + " ", c.isEnabled() ? SacColors.WHITE : SacColors.DARK_GRAY))
                .append(Component.text(String.format("%.1f", vl), SacColors.vlColor(vl)))
                .append(c.isExperimental() ? Component.text(" *", SacColors.PURPLE) : Component.empty())
                .hoverEvent(HoverEvent.showText(Component.text()
                    .append(Component.text(c.getCheckName() + "\n", SacColors.WHITE))
                    .append(Component.text("VL: " + String.format("%.2f", vl) + "\n", SacColors.vlColor(vl)))
                    .append(Component.text("Status: " + (c.isEnabled() ? "Enabled" : "Disabled") + "\n", c.isEnabled() ? SacColors.GREEN : SacColors.RED))
                    .append(c.isExperimental() ? Component.text("Experimental\n", SacColors.PURPLE) : Component.empty())
                    .append(Component.text("Click to toggle", SacColors.GRAY))
                    .build()))
                .clickEvent(ClickEvent.runCommand("/sac toggle " + c.getCheckName() + " " + sp.getName()))
                .build();
            sender.sendMessage(line);
        }

        if (shown == 0) {
            sender.sendMessage(Component.text("   " + SacColors.CHECKMARK + " No active violations.", SacColors.GREEN));
        }

        sender.sendMessage(SacColors.spacer());

        int totalVL = (int) checks.stream().mapToDouble(c -> ((Check) c).violations).sum();
        long enabled = checks.stream().filter(c -> ((Check) c).isEnabled()).count();
        sender.sendMessage(Component.text()
            .append(Component.text("   Total  ", SacColors.GRAY))
            .append(Component.text(totalVL + " VL", SacColors.vlColor(totalVL)))
            .append(Component.text("  " + SacColors.DASH + "  ", SacColors.MUTED))
            .append(Component.text(enabled + "/" + checks.size() + " checks", SacColors.DARK_GRAY))
            .build());
        sender.sendMessage(SacColors.footer());
    }
}

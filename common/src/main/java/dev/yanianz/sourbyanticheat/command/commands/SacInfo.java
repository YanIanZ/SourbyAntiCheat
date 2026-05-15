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
            sender.sendMessage(Component.text("Player not tracked by SAC.", SacColors.RED));
            return;
        }

        sender.sendMessage(Component.text("=== " + sp.getName() + " Violation Info ===", SacColors.GOLD));

        var checks = new ArrayList<>(sp.checkManager.allChecks.values());
        checks.sort(Comparator.comparingDouble(c -> -((Check) c).violations));

        int shown = 0;
        for (var entry : checks) {
            Check c = (Check) entry;
            if (c.getCheckName() == null || c.violations < 0.5) continue;
            if (shown++ > 15) break;

            sender.sendMessage(Component.text()
                .append(Component.text("  " + c.getCheckName() + " ", c.isEnabled() ? SacColors.WHITE : SacColors.RED))
                .append(Component.text(String.format("VL=%.1f", c.violations), SacColors.YELLOW))
                .append(c.isEnabled() ? Component.empty() : Component.text(" [OFF]", SacColors.RED))
                .build());
        }

        if (shown == 0) {
            sender.sendMessage(Component.text("  No active violations.", SacColors.GREEN));
        }

        int totalVL = (int) checks.stream().mapToDouble(c -> ((Check) c).violations).sum();
        long enabled = checks.stream().filter(c -> ((Check) c).isEnabled()).count();
        sender.sendMessage(Component.text()
            .append(Component.text("Total: ", SacColors.GRAY))
            .append(Component.text(totalVL + " VL", SacColors.GREEN))
            .append(Component.text(" | ", SacColors.GRAY))
            .append(Component.text(enabled + "/" + checks.size() + " checks enabled", SacColors.WHITE))
            .build());
    }
}

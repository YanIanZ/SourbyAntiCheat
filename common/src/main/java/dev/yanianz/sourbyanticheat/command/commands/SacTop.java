package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class SacTop implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("top", Description.of("Show top offenders by VL"))
                        .permission("sac.alerts")
                        .handler(this::handleTop)
        );
    }

    private void handleTop(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        var entries = new ArrayList<>(SacAPI.INSTANCE.getPlayerDataManager().getEntries());

        if (entries.isEmpty()) {
            sender.sendMessage(Component.text("No tracked players.", SacColors.GRAY));
            return;
        }

        sender.sendMessage(Component.text("=== Top Offenders ===", SacColors.GOLD));

        entries.stream()
            .map(p -> new Object(){ SacPlayer sp = p; int vl = getTotalVL(p); })
            .filter(e -> e.vl > 0)
            .sorted((a, b) -> Integer.compare(b.vl, a.vl))
            .limit(10)
            .forEach(e -> sender.sendMessage(Component.text()
                .append(Component.text(String.format("  #%d ", entries.indexOf(e.sp) + 1), SacColors.GRAY))
                .append(Component.text(e.sp.getName() + " ", SacColors.YELLOW))
                .append(Component.text(e.vl + " VL", e.vl > 100 ? SacColors.RED : SacColors.GREEN))
                .build()));

        long totalVL = entries.stream().mapToLong(p -> getTotalVL(p)).sum();
        sender.sendMessage(Component.text()
            .append(Component.text("Total: ", SacColors.GRAY))
            .append(Component.text(totalVL + " VL", SacColors.GREEN))
            .append(Component.text(" across " + entries.size() + " players", SacColors.GRAY))
            .build());
    }

    private static int getTotalVL(SacPlayer sp) {
        return (int) sp.checkManager.allChecks.values().stream()
            .mapToDouble(c -> ((Check) c).violations).sum();
    }
}

package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
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
import java.util.concurrent.atomic.AtomicInteger;

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
            sender.sendMessage(Component.text("   No tracked players.", SacColors.DARK_GRAY));
            return;
        }

        sender.sendMessage(SacColors.spacer());
        sender.sendMessage(SacColors.header("Top Offenders"));
        sender.sendMessage(SacColors.spacer());

        AtomicInteger rank = new AtomicInteger(1);
        entries.stream()
            .map(p -> new Object(){ SacPlayer sp = p; int vl = getTotalVL(p); })
            .filter(e -> e.vl > 0)
            .sorted((a, b) -> Integer.compare(b.vl, a.vl))
            .limit(10)
            .forEach(e -> {
                int r = rank.getAndIncrement();
                sender.sendMessage(Component.text()
                    .append(SacColors.rank(r))
                    .append(Component.text(e.sp.getName() + " ", SacColors.WHITE)
                        .clickEvent(ClickEvent.runCommand("/sac info " + e.sp.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click for details", SacColors.GRAY))))
                    .append(Component.text(e.vl + " VL", SacColors.vlColor(e.vl)))
                    .append(Component.text("  ", SacColors.MUTED))
                    .append(SacColors.progressBar((double) e.vl / Math.max(200, e.vl), 10))
                    .build());
            });

        if (rank.get() == 1) {
            sender.sendMessage(Component.text("   " + SacColors.CHECKMARK + " All players clean!", SacColors.GREEN));
        }

        sender.sendMessage(SacColors.spacer());

        long totalVL = entries.stream().mapToLong(SacTop::getTotalVL).sum();
        sender.sendMessage(Component.text()
            .append(Component.text("   Total  ", SacColors.GRAY))
            .append(Component.text(totalVL + " VL", SacColors.vlColor(totalVL)))
            .append(Component.text("  across ", SacColors.MUTED))
            .append(Component.text(entries.size() + " players", SacColors.DARK_GRAY))
            .build());
        sender.sendMessage(SacColors.footer());
    }

    private static int getTotalVL(SacPlayer sp) {
        return (int) sp.checkManager.allChecks.values().stream()
            .mapToDouble(c -> ((Check) c).violations).sum();
    }
}

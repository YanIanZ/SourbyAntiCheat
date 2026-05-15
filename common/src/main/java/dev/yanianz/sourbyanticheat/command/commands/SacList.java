package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import ac.grim.grimac.api.GrimIdentity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.command.CommandUtils;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.player.PlatformPlayer;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SacList implements BuildableCommand {

    // Mainly for debugging purposes. Useful for seeing which players are exempt or not.

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(commandManager.commandBuilder("sac", "sac")
                .literal("list")
                .permission("sac.list")
                .required("list", StringParser.stringParser(), SUGGESTIONS)
                .handler(commandContext -> handleList(commandContext.sender(), commandContext.getOrDefault("list", "?").toLowerCase()))
                .build());
    }

    private final SuggestionProvider<Sender> SUGGESTIONS = CommandUtils.fromStrings("players", "checks");

    private void handleList(Sender sender, String id) {
        switch (id) {
            case "players" -> handleListPlayers(sender);
            case "checks" -> handleListChecks(sender);
            default -> sender.sendMessage(Component.text()
                    .append(Component.text("  " + SacColors.CROSS + " Invalid: ", SacColors.RED))
                    .append(Component.text(id, SacColors.WHITE))
                    .append(Component.text(" " + SacColors.DASH + " use ", SacColors.MUTED))
                    .append(Component.text("players", SacColors.ACCENT)
                        .clickEvent(ClickEvent.runCommand("/sac list players")))
                    .append(Component.text(" or ", SacColors.MUTED))
                    .append(Component.text("checks", SacColors.ACCENT)
                        .clickEvent(ClickEvent.runCommand("/sac list checks")))
                    .build());
        }
    }

    private void handleListChecks(Sender sender) {
        var pdm = SacAPI.INSTANCE.getPlayerDataManager();
        if (pdm.getEntries().isEmpty()) {
            sender.sendMessage(Component.text("   No tracked players " + SacColors.DASH + " checks load per-player.", SacColors.DARK_GRAY));
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
                    .append(vl > 0.5 ? Component.text(String.format("%.1f", vl), SacColors.vlColor(vl)) : Component.empty())
                    .append(check.isExperimental() ? Component.text(" *", SacColors.PURPLE) : Component.empty())
                    .clickEvent(ClickEvent.suggestCommand("/sac toggle " + check.getCheckName() + " "))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to toggle " + check.getCheckName(), SacColors.GRAY)))
                    .build());
            });
        sender.sendMessage(SacColors.footer());
    }

    private Component playerComponent(String name, UUID uuid, boolean online, boolean exempt) {
        return Component.text(name)
                .color(exempt ? SacColors.DARK_GRAY
                        : (online ? SacColors.WHITE : SacColors.RED))
                .clickEvent(ClickEvent.copyToClipboard(name))
                .hoverEvent(HoverEvent.showText(playerHoverComponent(uuid, online, exempt, true)));
    }

    private Component playerHoverComponent(UUID uuid, boolean online, boolean exempt, boolean registered) {
        var builder = Component.text();
        builder.append(Component.text()
                .append(Component.text("UUID: ", SacColors.GRAY))
                .append(Component.text(uuid + "", SacColors.WHITE))
                .append(Component.newline())
                .append(Component.text("Status: ", SacColors.GRAY))
                .append(online ? Component.text(SacColors.DOT + " Online", SacColors.GREEN)
                        : Component.text(SacColors.CROSS + " Offline", SacColors.RED)));
        if (exempt) {
            builder.append(Component.newline());
            builder.append(Component.text(SacColors.STAR + " Exempt", SacColors.PURPLE));
        }
        if (!registered) {
            builder.append(Component.newline());
            builder.append(Component.text(SacColors.CROSS + " Not Registered", SacColors.RED));
        }
        return builder.build();
    }

    private void handleListPlayers(Sender sender) {
        final Map<UUID, PlatformPlayer> onlinePlayers = SacAPI.INSTANCE.getPlatformPlayerFactory().getOnlinePlayers()
                .stream().collect(Collectors.toMap(GrimIdentity::getUniqueId, Function.identity()));
        final Set<PlatformPlayer> unregisteredPlayers = new HashSet<>(onlinePlayers.values());

        sender.sendMessage(SacColors.spacer());
        sender.sendMessage(SacColors.header("Player List"));
        sender.sendMessage(SacColors.spacer());

        final TextComponent.Builder builder = Component.text();
        builder.append(Component.text("   ", SacColors.MUTED));

        boolean after = false;
        for (SacPlayer entry : SacAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (after) {
                builder.append(Component.text(", ", SacColors.MUTED));
            } else {
                after = true;
            }
            PlatformPlayer platformPlayer = onlinePlayers.get(entry.getUniqueId());
            if (platformPlayer != null) unregisteredPlayers.remove(platformPlayer);
            boolean online = platformPlayer != null && platformPlayer.isOnline();
            boolean exempt = !SacAPI.INSTANCE.getPlayerDataManager().shouldCheck(entry.user);
            builder.append(playerComponent(entry.getName(), entry.getUniqueId(), online, exempt));
        }

        for (PlatformPlayer platformPlayer : unregisteredPlayers) {
            if (after) {
                builder.append(Component.text(", ", SacColors.MUTED));
            } else {
                after = true;
            }
            builder.append(Component.text(platformPlayer.getName(), SacColors.PURPLE)
                    .clickEvent(ClickEvent.suggestCommand(platformPlayer.getName()))
                    .hoverEvent(HoverEvent.showText(playerHoverComponent(platformPlayer.getUniqueId(), platformPlayer.isOnline(), false, false)))
            );
        }

        sender.sendMessage(builder.build());
        sender.sendMessage(SacColors.spacer());
        sender.sendMessage(SacColors.footer());
    }

}

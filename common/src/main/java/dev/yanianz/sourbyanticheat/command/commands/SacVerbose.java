package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.manager.AlertManagerImpl;
import dev.yanianz.sourbyanticheat.manager.datastore.PlayerToggleStore;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.player.PlatformPlayer;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SacVerbose implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("verbose")
                        .permission("sac.verbose")
                        .handler(this::handleVerbose)
        );
    }

    private void handleVerbose(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (sender.isPlayer()) {
            PlatformPlayer p = Objects.requireNonNull(context.sender().getPlatformPlayer());
            AlertManagerImpl am = SacAPI.INSTANCE.getAlertManager();
            boolean newState = !am.hasVerboseEnabled(p);
            am.setVerboseEnabled(p, newState, false);
            PlayerToggleStore toggles = SacAPI.INSTANCE.getDataStoreLifecycle().playerToggleStore();
            toggles.applyUserToggle(p.getUniqueId(), PlayerToggleStore.KEY_VERBOSE, newState);
            // setVerboseEnabled(true) cascades to setAlertsEnabled(true) in AlertManager
            // — mirror that into the toggle store so the persisted alerts row tracks the
            // implied state, otherwise a verbose-on staff member would re-toggle alerts
            // off on next reconnect when persisted alerts is still false.
            if (newState) toggles.applyUserToggle(p.getUniqueId(), PlayerToggleStore.KEY_ALERTS, true);
        } else if (sender.isConsole()) {
            SacAPI.INSTANCE.getAlertManager().toggleConsoleVerbose();
        }
    }
}

package dev.yanianz.sourbyanticheat.platform.bukkit.command;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.platform.api.command.PlayerSelector;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.platform.bukkit.sender.BukkitSenderFactory;
import lombok.RequiredArgsConstructor;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class BukkitPlayerSelectorAdapter implements PlayerSelector {
    private final SinglePlayerSelector bukkitSelector;

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Sender getSinglePlayer() {
        return ((BukkitSenderFactory) SacAPI.INSTANCE.getSenderFactory()).map(bukkitSelector.single());
    }

    @Override
    public Collection<Sender> getPlayers() {
        return Collections.singletonList(((BukkitSenderFactory) SacAPI.INSTANCE.getSenderFactory()).map(bukkitSelector.single()));
    }

    @Override
    public String inputString() {
        return bukkitSelector.inputString();
    }
}

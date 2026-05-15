package dev.yanianz.sourbyanticheat.platform.bukkit.manager;

import dev.yanianz.sourbyanticheat.platform.api.manager.MessagePlaceHolderManager;
import dev.yanianz.sourbyanticheat.platform.api.player.PlatformPlayer;
import dev.yanianz.sourbyanticheat.platform.bukkit.player.BukkitPlatformPlayer;
import dev.yanianz.sourbyanticheat.utils.reflection.ReflectionUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitMessagePlaceHolderManager implements MessagePlaceHolderManager {
    public static final boolean hasPlaceholderAPI = ReflectionUtils.hasClass("me.clip.placeholderapi.PlaceholderAPI");

    @Override
    public @NotNull String replacePlaceholders(@Nullable PlatformPlayer player, @NotNull String string) {
        if (!hasPlaceholderAPI) return string;
        return PlaceholderAPI.setPlaceholders(player instanceof BukkitPlatformPlayer bukkitPlatformPlayer ? bukkitPlatformPlayer.getBukkitPlayer() : null, string);
    }
}

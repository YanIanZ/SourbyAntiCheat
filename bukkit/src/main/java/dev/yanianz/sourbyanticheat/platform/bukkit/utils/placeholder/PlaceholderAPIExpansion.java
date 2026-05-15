package dev.yanianz.sourbyanticheat.platform.bukkit.utils.placeholder;

import dev.yanianz.sourbyanticheat.SacAPI;
import ac.grim.grimac.api.GrimUser;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class PlaceholderAPIExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "sac";
    }

    public @NotNull String getAuthor() {
        return String.join(", ", SacAPI.INSTANCE.getGrimPlugin().getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return SacAPI.INSTANCE.getExternalAPI().getSacVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        Set<String> staticReplacements = SacAPI.INSTANCE.getExternalAPI().getStaticReplacements().keySet();
        Set<String> variableReplacements = SacAPI.INSTANCE.getExternalAPI().getVariableReplacements().keySet();
        ArrayList<String> placeholders = new ArrayList<>(staticReplacements.size() + variableReplacements.size());
        for (String s : staticReplacements) {
            placeholders.add(s.equals("%sac_version%") ? s : "%sac_" + s.replace("%", "") + "%");
        }
        for (String s : variableReplacements) {
            placeholders.add(s.equals("%player%") ? "%sac_player%" : "%sac_player_" + s.replace("%", "") + "%");
        }
        return placeholders;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        for (Map.Entry<String, String> entry : SacAPI.INSTANCE.getExternalAPI().getStaticReplacements().entrySet()) {
            String key = entry.getKey().equals("%sac_version%")
                    ? "version"
                    : entry.getKey().replace("%", "");
            if (params.equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }

        if (offlinePlayer instanceof Player player) {
            SacPlayer grimPlayer = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(player.getUniqueId());
            if (grimPlayer == null) return null;

            for (Map.Entry<String, Function<GrimUser, String>> entry : SacAPI.INSTANCE.getExternalAPI().getVariableReplacements().entrySet()) {
                String key = entry.getKey().equals("%player%")
                        ? "player"
                        : "player_" + entry.getKey().replace("%", "");
                if (params.equalsIgnoreCase(key)) {
                    return entry.getValue().apply(grimPlayer);
                }
            }
        }

        return null;
    }
}

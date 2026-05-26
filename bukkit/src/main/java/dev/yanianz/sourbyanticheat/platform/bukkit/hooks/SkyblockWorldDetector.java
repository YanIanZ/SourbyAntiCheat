package dev.yanianz.sourbyanticheat.platform.bukkit.hooks;

import dev.yanianz.sourbyanticheat.profile.Profile;
import dev.yanianz.sourbyanticheat.profile.ProfileWorldMap;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a present skyblock plugin's default island world(s) to the SKYBLOCK
 * profile. No plugin API dependency — uses each plugin's well-known default
 * world-name patterns. Custom world names: configure profile-worlds.yml.
 */
public final class SkyblockWorldDetector {

    private SkyblockWorldDetector() {}

    /** Bukkit plugin name -&gt; default island world globs. */
    public static Map<String, List<String>> defaultGlobs() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("SuperiorSkyblock2", List.of("SuperiorWorld*"));
        m.put("BentoBox", List.of("bskyblock_world*", "aoneblock_world*",
                "acidisland_world*", "caveblock_world*"));
        return m;
    }

    /** Register globs for every present skyblock plugin onto the world map. */
    public static void apply(ProfileWorldMap worldMap) {
        if (worldMap == null) return;
        for (var entry : defaultGlobs().entrySet()) {
            try {
                if (Bukkit.getPluginManager().getPlugin(entry.getKey()) == null) continue;
                for (String glob : entry.getValue()) worldMap.addMapping(glob, Profile.SKYBLOCK);
                LogUtil.info("Skyblock: mapped " + entry.getKey() + " worlds -> SKYBLOCK profile");
            } catch (Throwable t) {
                LogUtil.warn("Skyblock detect failed for " + entry.getKey() + ": " + t);
            }
        }
    }
}

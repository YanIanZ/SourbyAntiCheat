package dev.yanianz.sourbyanticheat.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConfigMigratorV1ToV2 {
    private static final Logger LOG = Logger.getLogger(ConfigMigratorV1ToV2.class.getName());

    public static final Set<String> KEPT = Set.of(
            "Reach", "NoSwingAttack", "AutoClicker", "FastBow", "FastEat", "MultiAttack",
            "SelfInteract", "FlightA", "Speed", "NoSlow", "Step", "Spider",
            "FastBreak", "NoSwingBreak", "FarBreak", "ScaffoldA", "FabricatedPlace",
            "FarPlace", "BedFucker", "NoClip", "ExploitA", "ExploitB", "ExploitC",
            "BadPacketsA", "BadPacketsB", "BadPacketsC", "PacketOrderA",
            "NettyFlood", "PayloadCheck", "Post", "TimerA", "Tower",
            "CrashA", "CrashB", "CrashC", "CrashD", "MultiActionsA"
    );

    @SuppressWarnings("unchecked")
    public void migrate(Path checksYml) {
        if (!Files.exists(checksYml)) return;
        try {
            var loaded = new Yaml().load(Files.newBufferedReader(checksYml));
            if (!(loaded instanceof Map<?, ?> root)) {
                markFailure(checksYml, new IOException("root is not a map"));
                return;
            }
            Object ver = root.get("config-version");
            if (ver instanceof Number n && n.intValue() >= 2) return;

            Path backup = checksYml.resolveSibling("checks.v1.bak.yml");
            Files.copy(checksYml, backup, StandardCopyOption.REPLACE_EXISTING);

            Object checksObj = root.get("checks");
            var v1Checks = checksObj instanceof Map
                    ? (Map<String, Map<String, Object>>) checksObj
                    : Map.<String, Map<String, Object>>of();
            var practiceOverrides = new LinkedHashMap<String, Map<String, Object>>();
            for (var e : v1Checks.entrySet()) {
                if (KEPT.contains(e.getKey())) practiceOverrides.put(e.getKey(), e.getValue());
            }

            var v2 = new LinkedHashMap<String, Object>();
            v2.put("config-version", 2);
            var profiles = new LinkedHashMap<String, Object>();
            for (var p : new String[]{"GENERIC", "PRACTICE", "BEDWARS", "SKYWARS", "SKYBLOCK", "LOBBY"}) {
                var section = new LinkedHashMap<String, Object>();
                section.put("disabled", List.of());
                section.put("overrides", "PRACTICE".equals(p) ? practiceOverrides : Map.of());
                section.put("leniency", List.of());
                profiles.put(p, section);
            }
            v2.put("profiles", profiles);

            var opts = new DumperOptions();
            opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            opts.setIndent(2);
            Files.writeString(checksYml, new Yaml(opts).dump(v2));
            LOG.info(() -> "Migrated checks.yml v1 -> v2. Backup: " + backup);
        } catch (IOException | RuntimeException ex) {
            markFailure(checksYml, ex);
        }
    }

    private void markFailure(Path checksYml, Throwable ex) {
        LOG.log(Level.SEVERE, "config migration failed; original preserved", ex);
        try { Files.writeString(checksYml.resolveSibling(checksYml.getFileName() + ".failed-migration"),
                "see logs: " + ex.getMessage() + "\n"); } catch (IOException ignored) {}
    }
}

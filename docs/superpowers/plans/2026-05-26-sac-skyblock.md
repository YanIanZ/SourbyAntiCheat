# SAC Skyblock Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans / subagent-driven-development. Checkbox steps.

**Goal:** Auto-map a present skyblock plugin's island world(s) to the SKYBLOCK profile, no manual config.

**Architecture:** `ProfileWorldMap` gains a thread-safe `addMapping`; a presence-gated `SkyblockWorldDetector` registers known default island world globs → SKYBLOCK at `onEnable`, right after the map is built. Island fly is already handled (abilities packet), so no exemption code.

**Tech Stack:** Java 17+, Bukkit (`bukkit`), JUnit5 (`common`). Spec: `docs/superpowers/specs/2026-05-26-sac-skyblock-design.md`. Sub-project 2 of 4.

**Lombok note:** ignore IDE diagnostics; trust `./gradlew`.

---

## Task 1: ProfileWorldMap.addMapping (thread-safe)

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/profile/ProfileWorldMap.java`
- Create: `common/src/test/java/dev/yanianz/sourbyanticheat/profile/ProfileWorldMapAddMappingTest.java`

- [ ] **Step 1: Failing test**

```java
package dev.yanianz.sourbyanticheat.profile;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class ProfileWorldMapAddMappingTest {

    @Test
    void addedMappingIsMatched() {
        ProfileWorldMap map = new ProfileWorldMap(new LinkedHashMap<>(), Profile.GENERIC);
        assertEquals(Profile.GENERIC, map.lookup("SuperiorWorld"));
        map.addMapping("SuperiorWorld*", Profile.SKYBLOCK);
        assertEquals(Profile.SKYBLOCK, map.lookup("SuperiorWorld"));
        assertEquals(Profile.SKYBLOCK, map.lookup("SuperiorWorld_nether"));
        assertEquals(Profile.GENERIC, map.lookup("lobby"));
    }

    @Test
    void fileMappingsTakePrecedenceOverLaterAdds() {
        LinkedHashMap<String, Profile> raw = new LinkedHashMap<>();
        raw.put("arena", Profile.BEDWARS);
        ProfileWorldMap map = new ProfileWorldMap(raw, Profile.GENERIC);
        map.addMapping("arena", Profile.SKYBLOCK); // later add must NOT override existing first-match
        assertEquals(Profile.BEDWARS, map.lookup("arena"));
    }
}
```

- [ ] **Step 2: Run — expect FAIL** (`./gradlew :common:test --tests '*ProfileWorldMapAddMappingTest'`)

- [ ] **Step 3: Implement** — replace `ProfileWorldMap.java` body:

```java
package dev.yanianz.sourbyanticheat.profile;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public final class ProfileWorldMap {
    // Lock-free reads (hot path), copy-on-write adds (rare, enable-time). Order =
    // first-match precedence: file mappings inserted first, runtime adds appended.
    private final List<Map.Entry<Pattern, Profile>> compiled = new CopyOnWriteArrayList<>();
    private final Profile fallback;

    public ProfileWorldMap(LinkedHashMap<String, Profile> raw, Profile fallback) {
        this.fallback = fallback;
        for (var e : raw.entrySet()) {
            compiled.add(new AbstractMap.SimpleImmutableEntry<>(globToRegex(e.getKey()), e.getValue()));
        }
    }

    /** Append a runtime glob->profile mapping (lower precedence than existing entries). */
    public void addMapping(String glob, Profile profile) {
        if (glob == null || profile == null) return;
        compiled.add(new AbstractMap.SimpleImmutableEntry<>(globToRegex(glob), profile));
    }

    public Profile lookup(String worldName) {
        if (worldName == null) return fallback;
        for (var e : compiled) {
            if (e.getKey().matcher(worldName).matches()) return e.getValue();
        }
        return fallback;
    }

    private static Pattern globToRegex(String glob) {
        var sb = new StringBuilder("^");
        for (char c : glob.toCharArray()) {
            if (c == '*') sb.append(".*");
            else if ("\\.[](){}+?^$|".indexOf(c) >= 0) sb.append('\\').append(c);
            else sb.append(c);
        }
        return Pattern.compile(sb.append('$').toString());
    }
}
```

- [ ] **Step 4: Run — expect PASS (2 tests)**
- [ ] **Step 5: Commit** `feat(profile): ProfileWorldMap.addMapping (runtime world->profile)`

---

## Task 2: SkyblockWorldDetector

**Files:**
- Create: `bukkit/.../platform/bukkit/hooks/SkyblockWorldDetector.java`
- Create: `common/src/test/java/.../profile/SkyblockGlobsTest.java` (tests the pure globs table — placed in common so the table is shared/testable: actually the table lives in the bukkit class; test the bukkit-independent map via a static method)

Because the globs table needs no Bukkit, expose it as a `public static Map<String, List<String>> defaultGlobs()` on the detector and unit-test it. The test lives in bukkit test sources.

- [ ] **Step 1: Implement detector**

```java
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

    /** Bukkit plugin name -> default island world globs. */
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
```

- [ ] **Step 2: Test the globs table** — `bukkit/src/test/java/.../SkyblockGlobsTest.java`:

```java
package dev.yanianz.sourbyanticheat.platform.bukkit.hooks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkyblockGlobsTest {
    @Test
    void knownPluginsPresent() {
        var g = SkyblockWorldDetector.defaultGlobs();
        assertTrue(g.containsKey("SuperiorSkyblock2"));
        assertTrue(g.containsKey("BentoBox"));
        assertTrue(g.get("SuperiorSkyblock2").contains("SuperiorWorld*"));
        assertTrue(g.get("BentoBox").contains("bskyblock_world*"));
    }
}
```

> NOTE: if the `bukkit` module has no test source set / JUnit configured, place this test under `common` test sources is NOT possible (class is in bukkit). Instead, if bukkit has no test task, SKIP this test and rely on compile + the `ProfileWorldMapAddMappingTest`. Check `bukkit/build.gradle.kts` for a `tasks.test`/`testImplementation` before adding; the plan's Task root already notes bukkit may lack tests.

- [ ] **Step 3: Compile** `./gradlew :bukkit:compileJava` — SUCCESS.
- [ ] **Step 4: Commit** `feat(skyblock): SkyblockWorldDetector (auto-map island worlds)`

---

## Task 3: Wire into onEnable + docs

**Files:**
- Modify: `bukkit/.../platform/bukkit/SacBukkitLoaderPlugin.java`
- Modify: `common/src/main/resources/profile-worlds.yml`

- [ ] **Step 1:** In `onEnable`, immediately after `ProfileWorldMap profileWorldMap = new ProfileWorldMap(worldMap, defaultProfile);` (line ~165), add:

```java
            dev.yanianz.sourbyanticheat.platform.bukkit.hooks.SkyblockWorldDetector.apply(profileWorldMap);
```

- [ ] **Step 2:** Add a doc comment near the top of `profile-worlds.yml` (after `defaultProfile:`):

```yaml
# Skyblock worlds are auto-mapped to the SKYBLOCK profile when a supported
# skyblock plugin is installed (SuperiorSkyblock2, BentoBox). For custom world
# names or other plugins, add an explicit "world-name*": SKYBLOCK entry below.
```

- [ ] **Step 3:** `./gradlew build` + `./gradlew :common:test` — SUCCESS.
- [ ] **Step 4: Commit** `feat(skyblock): auto-detect island worlds in onEnable + docs`

---

## Self-Review (authoring)
- **Spec coverage:** addMapping (Task 1), SkyblockWorldDetector + known globs (Task 2), onEnable wiring + docs (Task 3). Island fly / world-profile / TP exemption explicitly out of scope (already handled). IridiumSkyblock/custom names → profile-worlds.yml (documented).
- **Placeholders:** none. One inline NOTE flags a real conditional (bukkit test source set existence) with a concrete fallback.
- **Type consistency:** `ProfileWorldMap.addMapping(String, Profile)` defined Task 1, used Task 2. `SkyblockWorldDetector.apply(ProfileWorldMap)` / `defaultGlobs()` defined Task 2, used Task 3 + test. `lookup(String)` unchanged signature.

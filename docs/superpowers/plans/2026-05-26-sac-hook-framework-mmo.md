# SAC Hook Framework + MMO Exemption Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Add an "exempt-while-active" primitive + a presence-gated soft-depend hook framework, with mcMMO as the first working hook, so plugin-granted abilities stop causing false positives.

**Architecture:** `ExemptionRegistry` (ref-counted, per-player active exemptions) is consulted by `Check.flag()` alongside the existing `LeniencyTracker`. A `PluginHook`/`HookManager` framework registers Bukkit listeners only when the target plugin is present; the `McMMOHook` toggles exemptions on mcMMO super-ability activate/deactivate via its compileOnly API.

**Tech Stack:** Java 17+, Bukkit/Paper (`bukkit`), mcMMO API (`compileOnly`), JUnit5 (`common`). Build: `./gradlew build`. Tests: `./gradlew :common:test`.

This is **sub-project 1 of 4** (spec: `docs/superpowers/specs/2026-05-26-sac-hook-framework-mmo-design.md`). Siblings later: Skyblock, OldCombatMechanics, version audit.

**Lombok note:** the IDE/LSP is not Lombok-aware (false "undefined"/"non-static" errors). Trust only `./gradlew`.

---

## File Structure

**Create (common)**
- `common/.../profile/exempt/ExemptionRegistry.java` — ref-counted active exemptions.
- `common/.../platform/api/hooks/HookConfig.java` — parsed `hooks:` config (plugin→enabled + ability→checks). Platform-agnostic so the bukkit hooks read it.
- `common/src/test/java/.../profile/exempt/ExemptionRegistryTest.java`
- `common/src/test/java/.../platform/api/hooks/HookConfigTest.java`

**Create (bukkit)**
- `bukkit/.../platform/bukkit/hooks/PluginHook.java` — interface.
- `bukkit/.../platform/bukkit/hooks/HookManager.java` — presence-gated registration.
- `bukkit/.../platform/bukkit/hooks/McMMOHook.java` — mcMMO listener.

**Modify**
- `common/.../checks/Check.java` — second short-circuit on `ExemptionRegistry`.
- `common/.../SacAPI.java` — add `ExemptionRegistry` field + getter.
- `common/.../utils/anticheat/PlayerDataManager.java` — clear exemptions on quit.
- `common/src/main/resources/config/en.yml` — `hooks:` block + `config-version: 13`.
- `common/.../manager/config/update/SacConfigSpecs.java` — mainConfig version 13.
- `bukkit/.../platform/bukkit/SacBukkitLoaderPlugin.java` — construct + run `HookManager` in onEnable.
- `bukkit/build.gradle.kts` — mcMMO repo + compileOnly + softDepend entries.

---

## Task 1: ExemptionRegistry (common, pure)

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/profile/exempt/ExemptionRegistry.java`
- Create: `common/src/test/java/dev/yanianz/sourbyanticheat/profile/exempt/ExemptionRegistryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package dev.yanianz.sourbyanticheat.profile.exempt;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExemptionRegistryTest {

    private final UUID p = UUID.randomUUID();

    @Test
    void inactiveByDefault() {
        assertFalse(new ExemptionRegistry().active(p, "Speed"));
    }

    @Test
    void startMakesActive() {
        ExemptionRegistry r = new ExemptionRegistry();
        r.start(p, "Speed");
        assertTrue(r.active(p, "Speed"));
        assertFalse(r.active(p, "Reach"));
    }

    @Test
    void stopAfterSingleStartClears() {
        ExemptionRegistry r = new ExemptionRegistry();
        r.start(p, "Speed");
        r.stop(p, "Speed");
        assertFalse(r.active(p, "Speed"));
    }

    @Test
    void refCountedOverlappingHolders() {
        ExemptionRegistry r = new ExemptionRegistry();
        r.start(p, "Speed");
        r.start(p, "Speed"); // two holders
        r.stop(p, "Speed");
        assertTrue(r.active(p, "Speed"), "still held by second activation");
        r.stop(p, "Speed");
        assertFalse(r.active(p, "Speed"));
    }

    @Test
    void stopBelowZeroFloorsAtZero() {
        ExemptionRegistry r = new ExemptionRegistry();
        r.stop(p, "Speed"); // no prior start
        assertFalse(r.active(p, "Speed"));
        r.start(p, "Speed");
        assertTrue(r.active(p, "Speed"));
    }

    @Test
    void clearRemovesAllForPlayer() {
        ExemptionRegistry r = new ExemptionRegistry();
        r.start(p, "Speed");
        r.start(p, "Reach");
        r.clear(p);
        assertFalse(r.active(p, "Speed"));
        assertFalse(r.active(p, "Reach"));
    }

    @Test
    void nullSafe() {
        ExemptionRegistry r = new ExemptionRegistry();
        assertFalse(r.active(null, "Speed"));
        assertFalse(r.active(p, null));
        r.stop(p, "Speed"); // no throw
        r.clear(null);       // no throw
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.profile.exempt.ExemptionRegistryTest'`
Expected: FAIL (class missing).

- [ ] **Step 3: Implement**

```java
package dev.yanianz.sourbyanticheat.profile.exempt;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ref-counted, per-player set of actively-exempted check names. A plugin hook
 * calls {@link #start} when an ability begins and {@link #stop} when it ends;
 * overlapping abilities exempting the same check are ref-counted so an early
 * stop does not un-exempt while another holder is still active.
 *
 * <p>Thread-safe: hooks fire on the main thread, checks read from packet threads.
 */
public final class ExemptionRegistry {

    private final Map<UUID, Map<String, Integer>> counts = new ConcurrentHashMap<>();

    public void start(UUID player, String checkName) {
        if (player == null || checkName == null) return;
        counts.computeIfAbsent(player, k -> new ConcurrentHashMap<>())
                .merge(checkName, 1, Integer::sum);
    }

    public void stop(UUID player, String checkName) {
        if (player == null || checkName == null) return;
        Map<String, Integer> byCheck = counts.get(player);
        if (byCheck == null) return;
        byCheck.computePresent(checkName, (k, v) -> v <= 1 ? null : v - 1);
    }

    public boolean active(UUID player, String checkName) {
        if (player == null || checkName == null) return false;
        Map<String, Integer> byCheck = counts.get(player);
        return byCheck != null && byCheck.getOrDefault(checkName, 0) > 0;
    }

    public void clear(UUID player) {
        if (player == null) return;
        counts.remove(player);
    }
}
```

> NOTE: `Map` has no `computePresent`; use `computeIfPresent`. Final code must read:
> `byCheck.computeIfPresent(checkName, (k, v) -> v <= 1 ? null : v - 1);`

- [ ] **Step 4: Run — expect PASS (7 tests)**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.profile.exempt.ExemptionRegistryTest'`

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/profile/exempt/ExemptionRegistry.java \
        common/src/test/java/dev/yanianz/sourbyanticheat/profile/exempt/ExemptionRegistryTest.java
git commit -m "feat(exempt): add ref-counted ExemptionRegistry (exempt-while-active)"
```

---

## Task 2: Wire ExemptionRegistry into SacAPI + Check + quit

**Files:**
- Modify: `common/.../SacAPI.java`
- Modify: `common/.../checks/Check.java`
- Modify: `common/.../utils/anticheat/PlayerDataManager.java`

No new unit test (depends on `SacAPI.INSTANCE`; covered by Task 1 + compile + the existing suite).

- [ ] **Step 1: Add the registry to SacAPI**

In `SacAPI.java`, near the `leniencyTracker` field (around line 77), add a final field + explicit getter (do not rely on Lombok):

```java
    private final dev.yanianz.sourbyanticheat.profile.exempt.ExemptionRegistry exemptionRegistry =
            new dev.yanianz.sourbyanticheat.profile.exempt.ExemptionRegistry();

    public dev.yanianz.sourbyanticheat.profile.exempt.ExemptionRegistry getExemptionRegistry() {
        return exemptionRegistry;
    }
```

- [ ] **Step 2: Short-circuit in Check.flag()**

In `Check.java` `flag(String verbose)`, immediately after the existing leniency block:

```java
        try {
            var tracker = SacAPI.INSTANCE.getLeniencyTracker();
            if (tracker != null && tracker.active(checkName, player.uuid)) return false;
        } catch (Throwable ignored) {}
```

add:

```java
        try {
            var exempt = SacAPI.INSTANCE.getExemptionRegistry();
            if (exempt != null && exempt.active(player.uuid, checkName)) return false;
        } catch (Throwable ignored) {}
```

- [ ] **Step 3: Clear on quit**

In `PlayerDataManager.java`, find the quit handler that already calls
`SacAPI.INSTANCE.getAlertManager().handlePlayerQuit(...)` (around line 124).
Immediately after that call, add:

```java
        SacAPI.INSTANCE.getExemptionRegistry().clear(uuid);
```

(Use the `uuid` variable already in scope there; if the local is named differently, use that name — read the method first.)

- [ ] **Step 4: Compile**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/SacAPI.java \
        common/src/main/java/dev/yanianz/sourbyanticheat/checks/Check.java \
        common/src/main/java/dev/yanianz/sourbyanticheat/utils/anticheat/PlayerDataManager.java
git commit -m "feat(exempt): consult ExemptionRegistry in Check.flag + clear on quit"
```

---

## Task 3: HookConfig + config keys

**Files:**
- Create: `common/.../platform/api/hooks/HookConfig.java`
- Create: `common/src/test/java/.../platform/api/hooks/HookConfigTest.java`
- Modify: `common/src/main/resources/config/en.yml`
- Modify: `common/.../manager/config/update/SacConfigSpecs.java`

- [ ] **Step 1: Write the failing test**

```java
package dev.yanianz.sourbyanticheat.platform.api.hooks;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HookConfigTest {

    @Test
    void disabledPluginYieldsNoChecks() {
        HookConfig c = new HookConfig(false, Map.of("berserk", List.of("FastBreak")));
        assertFalse(c.enabled());
        assertTrue(c.checksFor("berserk").isEmpty());
    }

    @Test
    void enabledReturnsMappedChecks() {
        HookConfig c = new HookConfig(true, Map.of(
                "berserk", List.of("FastBreak"),
                "super_breaker", List.of("FastBreak")));
        assertTrue(c.enabled());
        assertEquals(List.of("FastBreak"), c.checksFor("berserk"));
        assertEquals(List.of("FastBreak"), c.checksFor("super_breaker"));
    }

    @Test
    void unknownAbilityYieldsEmpty() {
        HookConfig c = new HookConfig(true, Map.of("berserk", List.of("FastBreak")));
        assertTrue(c.checksFor("nonexistent").isEmpty());
    }

    @Test
    void keysAreCaseInsensitive() {
        HookConfig c = new HookConfig(true, Map.of("berserk", List.of("FastBreak")));
        assertEquals(List.of("FastBreak"), c.checksFor("BERSERK"));
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.platform.api.hooks.HookConfigTest'`

- [ ] **Step 3: Implement**

```java
package dev.yanianz.sourbyanticheat.platform.api.hooks;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Per-plugin hook configuration: an enabled flag and a map of ability key ->
 * check names to exempt while that ability is active. Ability keys are matched
 * case-insensitively. Immutable.
 */
public final class HookConfig {

    private final boolean enabled;
    private final Map<String, List<String>> abilityChecks;

    public HookConfig(boolean enabled, Map<String, List<String>> abilityChecks) {
        this.enabled = enabled;
        this.abilityChecks = Map.copyOf(abilityChecks);
    }

    public boolean enabled() {
        return enabled;
    }

    /** Checks to exempt for the given ability key, or empty if disabled/unknown. */
    public List<String> checksFor(String ability) {
        if (!enabled || ability == null) return List.of();
        return abilityChecks.getOrDefault(ability.toLowerCase(Locale.ROOT), List.of());
    }
}
```

> NOTE: the keys passed to the constructor must already be lowercase for the
> case-insensitive lookup to work (the loader in Task 5 lowercases them).

- [ ] **Step 4: Run — expect PASS (4 tests)**

- [ ] **Step 5: Add config keys to en.yml**

In `common/src/main/resources/config/en.yml`, append a top-level `hooks:` block (read the file; place it at top level, valid YAML, 4-space indent):

```yaml
# --- Plugin integration hooks (soft-depend) ---
# When a hooked plugin grants an ability, the listed checks are exempted for that
# player while the ability is active. Disable a hook with enabled: false.
hooks:
    mcmmo:
        enabled: true
        # mcMMO super-ability name -> checks exempted while active
        berserk: [FastBreak, AutoClicker]
        super_breaker: [FastBreak]
        giga_drill_breaker: [FastBreak]
        tree_feller: [FastBreak]
```

Change the `config-version: 12` line to `config-version: 13`.

- [ ] **Step 6: Bump SacConfigSpecs**

In `SacConfigSpecs.java` `mainConfig()`, change the builder version from `12` to `13`. Leave the existing migration lambda.

- [ ] **Step 7: Verify + commit**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.platform.api.hooks.*' --tests 'dev.yanianz.sourbyanticheat.config.*'`
Expected: PASS.

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/platform/api/hooks/HookConfig.java \
        common/src/test/java/dev/yanianz/sourbyanticheat/platform/api/hooks/HookConfigTest.java \
        common/src/main/resources/config/en.yml \
        common/src/main/java/dev/yanianz/sourbyanticheat/manager/config/update/SacConfigSpecs.java
git commit -m "feat(hooks): add HookConfig + hooks: config block (config v13)"
```

---

## Task 4: PluginHook + HookManager (bukkit)

**Files:**
- Create: `bukkit/.../platform/bukkit/hooks/PluginHook.java`
- Create: `bukkit/.../platform/bukkit/hooks/HookManager.java`

No unit test (Bukkit runtime). Verify via `./gradlew :bukkit:compileJava`.

- [ ] **Step 1: PluginHook interface**

```java
package dev.yanianz.sourbyanticheat.platform.bukkit.hooks;

import dev.yanianz.sourbyanticheat.platform.api.hooks.HookConfig;
import dev.yanianz.sourbyanticheat.profile.exempt.ExemptionRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** A soft-depend integration. Only registered when its plugin is present. */
public interface PluginHook {

    /** Bukkit plugin name as it appears in plugin.yml (PluginManager lookup). */
    String pluginName();

    default boolean isPresent() {
        return Bukkit.getPluginManager().getPlugin(pluginName()) != null;
    }

    /** Register this hook's Bukkit listener(s). Called only when present + enabled. */
    void register(JavaPlugin plugin, ExemptionRegistry exemptions, HookConfig config);
}
```

- [ ] **Step 2: HookManager**

```java
package dev.yanianz.sourbyanticheat.platform.bukkit.hooks;

import dev.yanianz.sourbyanticheat.platform.api.hooks.HookConfig;
import dev.yanianz.sourbyanticheat.profile.exempt.ExemptionRegistry;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.function.Function;

/** Registers every {@link PluginHook} whose plugin is present and enabled. */
public final class HookManager {

    private final List<PluginHook> hooks;

    public HookManager(List<PluginHook> hooks) {
        this.hooks = hooks;
    }

    /**
     * @param configFor resolves a hook's {@link HookConfig} by plugin key
     *                  (lowercased plugin name)
     */
    public void enablePresent(JavaPlugin plugin, ExemptionRegistry exemptions,
                              Function<String, HookConfig> configFor) {
        for (PluginHook hook : hooks) {
            try {
                HookConfig cfg = configFor.apply(hook.pluginName().toLowerCase(java.util.Locale.ROOT));
                if (cfg == null || !cfg.enabled() || !hook.isPresent()) continue;
                hook.register(plugin, exemptions, cfg);
                LogUtil.info("Hooked into " + hook.pluginName());
            } catch (Throwable t) {
                // One bad hook must never abort plugin enable or other hooks.
                LogUtil.warn("Failed to register hook " + hook.pluginName() + ": " + t);
            }
        }
    }
}
```

- [ ] **Step 3: Compile**

Run: `./gradlew :bukkit:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add bukkit/src/main/java/dev/yanianz/sourbyanticheat/platform/bukkit/hooks/
git commit -m "feat(hooks): presence-gated PluginHook + HookManager framework"
```

---

## Task 5: McMMOHook + build deps

**Files:**
- Modify: `bukkit/build.gradle.kts` (repo + compileOnly + softDepend)
- Create: `bukkit/.../platform/bukkit/hooks/McMMOHook.java`

- [ ] **Step 1: Add mcMMO dependency + softDepend**

In `bukkit/build.gradle.kts`, inside the `repositories { ... }` block add:

```kotlin
    maven("https://nexus.neetgames.com/repository/maven-public/")
```

Inside `dependencies { ... }` add:

```kotlin
    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.2.049")
```

In the `bukkit { ... }` block, extend `softDepend` to include `"mcMMO"`:

```kotlin
    softDepend = listOf("ProtocolLib", "Essentials", "ViaVersion", "ViaBackwards",
        "Geyser-Spigot", "floodgate", "PlaceholderAPI", "Spartan", "FastLogin",
        "driverholder-mysql", "driverholder-postgresql", "driverholder-mongodb", "SourbyCraft",
        "mcMMO")
```

- [ ] **Step 2: Verify the dep resolves**

Run: `./gradlew :bukkit:dependencies --configuration compileClasspath` | piped to grep:
Run: `./gradlew :bukkit:compileJava` (after Step 3 file exists). For now confirm resolution:
Run: `./gradlew :bukkit:dependencies --configuration compileClasspath -q 2>&1 | grep -i mcmmo`
Expected: shows `com.gmail.nossr50.mcMMO:mcMMO:2.2.049`. If it fails to resolve (offline), STOP and report — the hook needs the API.

- [ ] **Step 3: Implement McMMOHook**

mcMMO fires `McMMOPlayerAbilityActivateEvent` / `McMMOPlayerAbilityDeactivateEvent`
(package `com.gmail.nossr50.events.skills.abilities`). The parent
`McMMOPlayerAbilityEvent` exposes `getAbility()` → `SuperAbilityType` and
`getPlayer()` → `org.bukkit.entity.Player`. We map the ability's enum name
(lowercased) to checks via the `HookConfig`.

```java
package dev.yanianz.sourbyanticheat.platform.bukkit.hooks;

import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityActivateEvent;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityDeactivateEvent;
import dev.yanianz.sourbyanticheat.platform.api.hooks.HookConfig;
import dev.yanianz.sourbyanticheat.profile.exempt.ExemptionRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.UUID;

/**
 * Exempts the configured checks while an mcMMO super-ability is active.
 * mcMMO super-abilities have clean activate/deactivate events, so exemptions
 * are ref-counted start->stop with no timer needed.
 */
public final class McMMOHook implements PluginHook, Listener {

    private ExemptionRegistry exemptions;
    private HookConfig config;

    @Override
    public String pluginName() {
        return "mcMMO";
    }

    @Override
    public void register(JavaPlugin plugin, ExemptionRegistry exemptions, HookConfig config) {
        this.exemptions = exemptions;
        this.config = config;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onActivate(McMMOPlayerAbilityActivateEvent event) {
        toggle(event.getPlayer(), abilityKey(event.getAbility()), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeactivate(McMMOPlayerAbilityDeactivateEvent event) {
        toggle(event.getPlayer(), abilityKey(event.getAbility()), false);
    }

    private static String abilityKey(Object ability) {
        return ability == null ? "" : ability.toString().toLowerCase(Locale.ROOT);
    }

    private void toggle(Player player, String abilityKey, boolean start) {
        if (player == null || exemptions == null || config == null) return;
        UUID uuid = player.getUniqueId();
        for (String check : config.checksFor(abilityKey)) {
            if (start) exemptions.start(uuid, check);
            else exemptions.stop(uuid, check);
        }
    }
}
```

- [ ] **Step 4: Compile**

Run: `./gradlew :bukkit:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add bukkit/build.gradle.kts \
        bukkit/src/main/java/dev/yanianz/sourbyanticheat/platform/bukkit/hooks/McMMOHook.java
git commit -m "feat(hooks): mcMMO super-ability exemption hook (compileOnly API)"
```

---

## Task 6: Wire HookManager into onEnable

**Files:**
- Modify: `bukkit/.../platform/bukkit/SacBukkitLoaderPlugin.java`

- [ ] **Step 1: Build the HookConfig map + run HookManager**

In `SacBukkitLoaderPlugin.onEnable`, after the block that registers the leniency
handlers (the `pm.registerEvents(new EnderPearlLandHandler(bus), this);` group,
around line 188), add:

```java
        // --- Soft-depend plugin hooks ---
        try {
            var cfg = dev.yanianz.sourbyanticheat.SacAPI.INSTANCE.getConfigManager().getConfig();
            java.util.function.Function<String, dev.yanianz.sourbyanticheat.platform.api.hooks.HookConfig> hookConfigFor =
                key -> {
                    boolean enabled = cfg.getBooleanElse("hooks." + key + ".enabled", false);
                    java.util.Map<String, java.util.List<String>> abilities = new java.util.HashMap<>();
                    // mcMMO ability -> checks (keys lowercased to match HookConfig lookup)
                    for (String ability : new String[]{"berserk", "super_breaker", "giga_drill_breaker", "tree_feller"}) {
                        java.util.List<String> checks = cfg.getStringListElse("hooks." + key + "." + ability, java.util.List.of());
                        if (!checks.isEmpty()) abilities.put(ability.toLowerCase(java.util.Locale.ROOT), checks);
                    }
                    return new dev.yanianz.sourbyanticheat.platform.api.hooks.HookConfig(enabled, abilities);
                };

            var hookManager = new dev.yanianz.sourbyanticheat.platform.bukkit.hooks.HookManager(
                java.util.List.of(new dev.yanianz.sourbyanticheat.platform.bukkit.hooks.McMMOHook()));
            hookManager.enablePresent(this,
                dev.yanianz.sourbyanticheat.SacAPI.INSTANCE.getExemptionRegistry(),
                hookConfigFor);
        } catch (Throwable t) {
            dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil.warn("Failed to init plugin hooks: " + t);
        }
```

> NOTE: confirm `cfg.getStringListElse(String, List)` exists on the ConfigManager
> (it is used in `PunishmentManager.reload` as `getStringListElse("Punishments", new ArrayList<>())`).
> If the exact name differs, match the one used there.

- [ ] **Step 2: Build**

Run: `./gradlew :bukkit:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add bukkit/src/main/java/dev/yanianz/sourbyanticheat/platform/bukkit/SacBukkitLoaderPlugin.java
git commit -m "feat(hooks): register HookManager (mcMMO) in onEnable"
```

---

## Task 7: Full build + smoke

- [ ] **Step 1:** `./gradlew build` + `./gradlew :common:test` — both green; jar produced.
- [ ] **Step 2 (manual smoke — record):** On a dev server with mcMMO: trigger Super Breaker / Giga Drill / Berserk → mine fast → no FastBreak flags during the ability; after it ends, FastBreak detection resumes. Without mcMMO installed: plugin enables cleanly, no errors, no hook log line.

---

## Follow-up (NOT in this plan)
MMOCore/MMOItems (MythicLib cast events), MythicMobs (`io.lumine:Mythic:5.11.1`), and AuraSkills hooks are framework-ready — each is a new `PluginHook` added to the `HookManager` list + a `softDepend`/`compileOnly` entry + ability→check config. Deferred because their event APIs need verifying first (avoid guessed code). Sibling specs: Skyblock, OldCombatMechanics, version audit.

---

## Self-Review (authoring)
- **Spec coverage:** ExemptionRegistry primitive + Check short-circuit + quit clear (Tasks 1-2); HookConfig + config (Task 3); PluginHook/HookManager framework (Task 4); mcMMO hook + compileOnly API + softDepend (Task 5); onEnable wiring (Task 6). The spec's MMOCore/MythicMobs/AuraSkills hooks are explicitly deferred (documented above) to avoid unverified-API placeholder code — the framework delivers them as drop-in follow-ups.
- **Placeholders:** none. Two inline NOTES correct a Java API name (`computeIfPresent`) and flag a config-method-name check — both are concrete, not vague.
- **Type consistency:** `ExemptionRegistry.start/stop/active/clear(UUID, String)` consistent across Tasks 1, 2 (Check uses `active(player.uuid, checkName)`), 5 (hook uses `start/stop(uuid, check)`). `HookConfig(boolean, Map)` + `enabled()` + `checksFor(String)` consistent across Tasks 3, 4, 5, 6. `PluginHook.register(JavaPlugin, ExemptionRegistry, HookConfig)` consistent Tasks 4-5. `HookManager.enablePresent(JavaPlugin, ExemptionRegistry, Function<String,HookConfig>)` consistent Tasks 4, 6.

# SAC GUI Rebuild — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development or superpowers:executing-plans. Checkbox (`- [ ]`) steps.

**Goal:** Replace the title-string-routed `SacGUI` with a holder-based menu framework; add Profiles (edit+save) and Alerts-feed panels; show the real wave queue.

**Architecture:** Each menu is a class implementing Bukkit `InventoryHolder` that carries its own state (target, page) — no title parsing. One `MenuRouter` listener routes clicks by `event.getInventory().getHolder() instanceof SacMenu`. Profile editing persists through a unit-tested `ProfileConfigWriter` (snakeyaml round-trip — `profiles.yml` has no comments, so a clean dump is safe), then reloads the live snapshot.

**Tech Stack:** Java 17+, Bukkit/Paper API (`bukkit` module, GUI), snakeyaml + JUnit5 (`common`, writer). Build: `./gradlew build`. Tests: `./gradlew :common:test`.

This is **Plan 3 of 4** (spec: `docs/superpowers/specs/2026-05-25-sac-overhaul-design.md`). Depends on Plan 1's `AlertFeed`. After this plan: CHECKPOINT for manual smoke before Plan 4.

**Lombok note:** the IDE/LSP is not Lombok-aware (false "undefined method"/"non-static" errors). Trust only `./gradlew`.

---

## File Structure

**Create (common — testable)**
- `common/.../profile/ProfileConfigWriter.java` — targeted edits to `profiles.yml` (toggle disabled-check, set override key, remove override key).
- `common/src/test/java/.../profile/ProfileConfigWriterTest.java`

**Create (bukkit — GUI framework + panels)**
- `bukkit/.../platform/bukkit/gui/menu/SacMenu.java` — abstract holder base.
- `bukkit/.../platform/bukkit/gui/menu/MenuRouter.java` — single click listener.
- `bukkit/.../platform/bukkit/gui/menu/Menus.java` — palette + item builders + pagination + PDC helpers (ported from `SacGUI`).
- `bukkit/.../platform/bukkit/gui/menu/HubMenu.java`
- `bukkit/.../platform/bukkit/gui/menu/PlayersMenu.java`
- `bukkit/.../platform/bukkit/gui/menu/PlayerChecksMenu.java`
- `bukkit/.../platform/bukkit/gui/menu/ProfilesMenu.java`
- `bukkit/.../platform/bukkit/gui/menu/ProfileDetailMenu.java`
- `bukkit/.../platform/bukkit/gui/menu/AlertsMenu.java`
- `bukkit/.../platform/bukkit/gui/menu/ReportsMenu.java`
- `bukkit/.../platform/bukkit/gui/menu/WaveMenu.java`

**Modify**
- `common/.../manager/WavePunishment.java` — add `public static List<String> queueView()`.
- `common/.../profile/ProfileConfig.java` — confirm/expose `reload()` (already public) + add `path()` accessor if needed by writer wiring.
- `common/.../command/commands/SacGUICommand.java` — open `HubMenu` (reflection target swapped from `SacGUI.openMain` to `menu.HubMenu.open`).
- bukkit plugin onEnable — register `MenuRouter` listener (where `SacGUI` was registered).

**Delete (after parity)**
- `bukkit/.../platform/bukkit/gui/SacGUI.java`

---

## Task 1: ProfileConfigWriter + test (common)

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/profile/ProfileConfigWriter.java`
- Create: `common/src/test/java/dev/yanianz/sourbyanticheat/profile/ProfileConfigWriterTest.java`

- [ ] **Step 1: Write the failing test**

```java
package dev.yanianz.sourbyanticheat.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProfileConfigWriterTest {

    private Path write(Path dir, String yaml) throws Exception {
        Path f = dir.resolve("profiles.yml");
        Files.writeString(f, yaml);
        return f;
    }

    private static final String BASE = """
            config-version: 2
            profiles:
              LOBBY:
                disabled: [Reach]
                overrides: {}
                leniency: []
              BEDWARS:
                disabled: []
                overrides:
                  Speed: { maxvl: 10 }
                leniency: []
            """;

    @Test
    void addDisabledCheck() throws Exception {
        Path f = write(Files.createTempDirectory("p"), BASE);
        new ProfileConfigWriter(f).toggleDisabled(Profile.LOBBY, "Speed", true);

        ProfileConfig cfg = new ProfileConfig(f);
        cfg.reload();
        assertTrue(cfg.snapshot().forProfile(Profile.LOBBY).isDisabled("Speed"));
        assertTrue(cfg.snapshot().forProfile(Profile.LOBBY).isDisabled("Reach"));
    }

    @Test
    void removeDisabledCheck() throws Exception {
        Path f = write(Files.createTempDirectory("p"), BASE);
        new ProfileConfigWriter(f).toggleDisabled(Profile.LOBBY, "Reach", false);

        ProfileConfig cfg = new ProfileConfig(f);
        cfg.reload();
        assertFalse(cfg.snapshot().forProfile(Profile.LOBBY).isDisabled("Reach"));
    }

    @Test
    void setOverrideValue() throws Exception {
        Path f = write(Files.createTempDirectory("p"), BASE);
        new ProfileConfigWriter(f).setOverride(Profile.BEDWARS, "Speed", "maxvl", 20);

        ProfileConfig cfg = new ProfileConfig(f);
        cfg.reload();
        Object v = cfg.snapshot().forProfile(Profile.BEDWARS).override("Speed", "maxvl");
        assertEquals(20, ((Number) v).intValue());
    }

    @Test
    void setOverrideCreatesCheckEntry() throws Exception {
        Path f = write(Files.createTempDirectory("p"), BASE);
        new ProfileConfigWriter(f).setOverride(Profile.LOBBY, "Reach", "threshold", 3.5);

        ProfileConfig cfg = new ProfileConfig(f);
        cfg.reload();
        Object v = cfg.snapshot().forProfile(Profile.LOBBY).override("Reach", "threshold");
        assertEquals(3.5, ((Number) v).doubleValue(), 1e-9);
    }

    @Test
    void writePreservesOtherProfiles() throws Exception {
        Path f = write(Files.createTempDirectory("p"), BASE);
        new ProfileConfigWriter(f).toggleDisabled(Profile.LOBBY, "Speed", true);

        ProfileConfig cfg = new ProfileConfig(f);
        cfg.reload();
        // BEDWARS Speed override survived the LOBBY edit
        assertEquals(10, ((Number) cfg.snapshot().forProfile(Profile.BEDWARS)
                .override("Speed", "maxvl")).intValue());
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.profile.ProfileConfigWriterTest'`
Expected: FAIL (class missing).

- [ ] **Step 3: Implement**

```java
package dev.yanianz.sourbyanticheat.profile;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Targeted editor for {@code profiles.yml}. Loads the whole document, mutates
 * the requested profile section, and writes it back. {@code profiles.yml}
 * carries no comments, so a snakeyaml round-trip is lossless for content.
 * Block style + 2-space indent keeps the file readable.
 */
public final class ProfileConfigWriter {

    private final Path file;

    public ProfileConfigWriter(Path file) { this.file = file; }

    /** Add or remove {@code checkName} from a profile's {@code disabled} list. */
    @SuppressWarnings("unchecked")
    public synchronized void toggleDisabled(Profile profile, String checkName, boolean disabled) throws IOException {
        Map<String, Object> root = load();
        Map<String, Object> section = sectionFor(root, profile);
        List<Object> list = section.get("disabled") instanceof List<?> l
                ? new ArrayList<>((List<Object>) l) : new ArrayList<>();
        list.remove(checkName);
        if (disabled) list.add(checkName);
        section.put("disabled", list);
        save(root);
    }

    /** Set {@code overrides.<checkName>.<key> = value} for a profile. */
    @SuppressWarnings("unchecked")
    public synchronized void setOverride(Profile profile, String checkName, String key, Object value) throws IOException {
        Map<String, Object> root = load();
        Map<String, Object> section = sectionFor(root, profile);
        Map<String, Object> overrides = section.get("overrides") instanceof Map<?, ?> m
                ? new LinkedHashMap<>((Map<String, Object>) m) : new LinkedHashMap<>();
        Map<String, Object> check = overrides.get(checkName) instanceof Map<?, ?> cm
                ? new LinkedHashMap<>((Map<String, Object>) cm) : new LinkedHashMap<>();
        check.put(key, value);
        overrides.put(checkName, check);
        section.put("overrides", overrides);
        save(root);
    }

    /** Remove an override key (and the check entry if it becomes empty). */
    @SuppressWarnings("unchecked")
    public synchronized void removeOverride(Profile profile, String checkName, String key) throws IOException {
        Map<String, Object> root = load();
        Map<String, Object> section = sectionFor(root, profile);
        if (!(section.get("overrides") instanceof Map<?, ?> m)) return;
        Map<String, Object> overrides = new LinkedHashMap<>((Map<String, Object>) m);
        if (overrides.get(checkName) instanceof Map<?, ?> cm) {
            Map<String, Object> check = new LinkedHashMap<>((Map<String, Object>) cm);
            check.remove(key);
            if (check.isEmpty()) overrides.remove(checkName);
            else overrides.put(checkName, check);
            section.put("overrides", overrides);
            save(root);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> load() throws IOException {
        Object raw = Files.exists(file)
                ? new Yaml().load(Files.newBufferedReader(file)) : null;
        Map<String, Object> root = raw instanceof Map<?, ?> m
                ? new LinkedHashMap<>((Map<String, Object>) m) : new LinkedHashMap<>();
        root.putIfAbsent("profiles", new LinkedHashMap<String, Object>());
        return root;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sectionFor(Map<String, Object> root, Profile profile) {
        Map<String, Object> profiles = (Map<String, Object>) root.get("profiles");
        if (!(profiles.get(profile.name()) instanceof Map<?, ?>)) {
            Map<String, Object> fresh = new LinkedHashMap<>();
            fresh.put("disabled", new ArrayList<>());
            fresh.put("overrides", new LinkedHashMap<>());
            fresh.put("leniency", new ArrayList<>());
            profiles.put(profile.name(), fresh);
        }
        return new LinkedHashMap<>((Map<String, Object>) profiles.get(profile.name())) {{
            // write-through wrapper: replace into parent on save via save()
        }};
    }

    private void save(Map<String, Object> root) throws IOException {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setIndent(2);
        opts.setPrettyFlow(true);
        Yaml yaml = new Yaml(opts);
        try (Writer w = Files.newBufferedWriter(file)) {
            yaml.dump(root, w);
        }
    }
}
```

> NOTE for implementer: the `sectionFor` write-through above is subtly wrong (it returns a *copy*). Implement `sectionFor` to return the **live** nested map and ensure mutations land in `root` before `save`. Concretely: get `profiles` map (live), ensure `profiles.get(NAME)` is a live `LinkedHashMap` (replace if absent/!map), and return THAT live map. The `toggleDisabled`/`setOverride` methods then `section.put(...)` directly on the live map, and `save(root)` writes the whole tree. Drop the anonymous-subclass trick. Verify with the tests in Step 1 (they assert cross-profile preservation + reload round-trips).

- [ ] **Step 4: Run — expect PASS (5 tests)**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.profile.ProfileConfigWriterTest'`

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/profile/ProfileConfigWriter.java \
        common/src/test/java/dev/yanianz/sourbyanticheat/profile/ProfileConfigWriterTest.java
git commit -m "feat(profile): add ProfileConfigWriter (edit profiles.yml)"
```

---

## Task 2: WavePunishment.queueView() (common)

**Files:** Modify `common/.../manager/WavePunishment.java`; Test `common/src/test/java/.../manager/WavePunishmentViewTest.java`

- [ ] **Step 1: Failing test**

```java
package dev.yanianz.sourbyanticheat.manager;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WavePunishmentViewTest {
    @Test
    void queueViewIsEmptyByDefault() {
        assertNotNull(WavePunishment.queueView());
        assertTrue(WavePunishment.queueView().isEmpty());
    }
}
```

- [ ] **Step 2: Run — expect FAIL** (`./gradlew :common:test --tests '*WavePunishmentViewTest'`)

- [ ] **Step 3: Implement** — in `WavePunishment.java`, add:

```java
    /** Read-only snapshot of queued entries for display (player — reason). */
    public static java.util.List<String> queueView() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (PunishEntry e : queue) out.add(e.playerName() + " — " + e.reason());
        return out;
    }
```

(`PunishEntry` is a `record(UUID uuid, String playerName, String reason)`; accessors `playerName()`/`reason()` exist.)

- [ ] **Step 4: Run — expect PASS.**

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/manager/WavePunishment.java \
        common/src/test/java/dev/yanianz/sourbyanticheat/manager/WavePunishmentViewTest.java
git commit -m "feat(wave): expose read-only queueView for GUI"
```

---

## Task 3: Menu framework (bukkit)

**Files:** Create `SacMenu.java`, `MenuRouter.java`, `Menus.java` in `bukkit/.../platform/bukkit/gui/menu/`.

No unit test (Bukkit runtime). Verify via `./gradlew :bukkit:compileJava`.

- [ ] **Step 1: SacMenu base**

```java
package dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Base for all SAC menus. The inventory's holder IS the menu, so click
 *  routing never parses titles. Subclasses carry their own state in fields. */
public abstract class SacMenu implements InventoryHolder {

    protected Inventory inventory;

    protected abstract int size();
    protected abstract Component title();
    protected abstract void render();

    /** Handle a click on a slot (already cancelled by the router). */
    public abstract void onClick(Player viewer, int slot, InventoryClickEvent event);

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, size(), title());
            render();
        }
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(getInventory());
    }
}
```

- [ ] **Step 2: MenuRouter listener**

```java
package dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Single listener: routes clicks to the SacMenu that owns the inventory. */
public final class MenuRouter implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SacMenu menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;
        menu.onClick(player, event.getRawSlot(), event);
    }
}
```

- [ ] **Step 3: Menus helper** — port the colour palette, `fillBorder`, item-builder, `setPDC`/`getPDC`, `vlColor`, `getTotalVL`, player-resolution helpers from the existing `bukkit/.../gui/SacGUI.java` into a reusable `Menus` utility. (Read `SacGUI.java` and lift those static helpers verbatim, adjusting visibility to `public static` where panels need them. Keep `KEY_TYPE`/`KEY_VALUE`/`KEY_UUID` NamespacedKeys here.)

Full content: copy the constants `BRAND/ACCENT/.../PURPLE`, `KEY_*`, `TIME_FMT`, and methods `resolvePlayer`, `resolveSacPlayer`, `fillBorder`, `getPDC`, `setPDC`, `getTotalVL`, `vlColor` from `SacGUI` into `Menus` as `public static`/package-private members. (These are pure helpers — straightforward move.)

- [ ] **Step 4: Compile** — `./gradlew :bukkit:compileJava` — expect SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add bukkit/src/main/java/dev/yanianz/sourbyanticheat/platform/bukkit/gui/menu/
git commit -m "feat(gui): holder-based menu framework (SacMenu, MenuRouter, Menus)"
```

---

## Task 4: Core panels — Hub, Players, PlayerChecks (bukkit)

Port the corresponding `SacGUI` screens to `SacMenu` subclasses using `Menus` helpers. Behaviour parity:
- **HubMenu** — buttons: Players, Profiles, Alerts, Reports, Wave + a System Status item (port from `SacGUI.openMain` status beacon). Clicks open the respective menu (`new PlayersMenu(0).open(player)` etc.).
- **PlayersMenu(int page)** — paginated player heads (28 content slots/page) from `SacAPI.INSTANCE.getPlayerDataManager().getEntries()`, sorted by name; head click → `new PlayerChecksMenu(name).open(player)`; prev/next page buttons.
- **PlayerChecksMenu(String targetName)** — port `SacGUI.openPlayerDetail`: per-check coloured pane with VL + enable/disable toggle, reset-all (BARRIER), back button, Spartan stats. Toggle/reset port from `SacGUI.toggleCheck`/`resetAllVLs`. Back → `new HubMenu().open(player)`.

- [ ] **Step 1:** Implement `HubMenu.java`, `PlayersMenu.java`, `PlayerChecksMenu.java` (full code authored against `SacGUI` as the parity reference; state via constructor fields, click via `onClick` switch on the clicked item's `Menus.getPDC(item, KEY_TYPE)`).
- [ ] **Step 2:** `./gradlew :bukkit:compileJava` — SUCCESS.
- [ ] **Step 3:** Commit `feat(gui): Hub / Players / PlayerChecks panels`.

---

## Task 5: Profiles (edit), Alerts, Reports, Wave panels (bukkit)

- **ProfilesMenu** — list the 6 `Profile` values; click → `new ProfileDetailMenu(profile)`.
- **ProfileDetailMenu(Profile)** — read `SacAPI.INSTANCE.getProfileConfig().snapshot().forProfile(p)`:
  - Disabled checks: a section of items; left-click toggles via `new ProfileConfigWriter(profilePath).toggleDisabled(p, check, !currentlyDisabled)` then `SacAPI.INSTANCE.getProfileConfig().reload()` and re-render.
  - Overrides: show each `check → {key:val}`; shift-click +/- adjusts a numeric key via `setOverride(...)`, then reload + re-render.
  - Leniency: view-only list.
  - After any write, also call the live reload path so online players pick up new thresholds (see NOTE).
- **AlertsMenu** — `dev.yanianz.sourbyanticheat.manager.AlertFeed.GLOBAL.recent()` → paper items (player, check, vl, time); click → `new PlayerChecksMenu(entry.player())`.
- **ReportsMenu** — port `SacGUI.openReports` (teleport to target, clear-all).
- **WaveMenu** — list `WavePunishment.queueView()` entries (replace the static text screen).

NOTE (live apply after profile write): after `profileConfig.reload()`, refresh online players so new thresholds take effect. Reuse the existing reload mechanism — call the same code path `SacReload` uses to reload checks for online players (locate it during implementation; e.g. iterate `getPlayerDataManager().getEntries()` and reload each `checkManager`). If a single global reload method exists, call it. Profile path: add `public Path path()` to `ProfileConfig` (returns `file`) so the menu can construct a `ProfileConfigWriter`.

- [ ] **Step 1:** Add `public java.nio.file.Path path() { return file; }` to `ProfileConfig.java`.
- [ ] **Step 2:** Implement the four menu classes.
- [ ] **Step 3:** `./gradlew :bukkit:compileJava` — SUCCESS.
- [ ] **Step 4:** Commit `feat(gui): Profiles(edit) / Alerts / Reports / Wave panels`.

---

## Task 6: Wire entry point, register router, delete SacGUI

- [ ] **Step 1:** `SacGUICommand.handleGUI` — change the reflection target from `SacGUI.openMain(player)` to opening the hub: load `dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu.HubMenu`, `new HubMenu().open(player)` (keep the reflection pattern + the player-only guard).
- [ ] **Step 2:** In the bukkit plugin onEnable, find where `new SacGUI()` is registered as a `Listener` (grep `registerEvents.*SacGUI`) and replace it with `new MenuRouter()`.
- [ ] **Step 3:** Delete `bukkit/.../platform/bukkit/gui/SacGUI.java`.
- [ ] **Step 4:** `./gradlew build` — expect BUILD SUCCESSFUL (no references to `SacGUI` remain — grep to confirm).
- [ ] **Step 5:** Commit `refactor(gui): open holder-based hub, register MenuRouter, remove SacGUI`.

---

## Task 7: Full build + smoke

- [ ] **Step 1:** `./gradlew build` + `./gradlew :common:test` — both green.
- [ ] **Step 2 (manual smoke — record):** `/sac gui` opens Hub → Players → a player's checks (toggle works) → back. Profiles → pick BEDWARS → toggle a disabled check → confirm `profiles.yml` changed + change applied after. Alerts panel shows recent flags (trigger one first). Reports + Wave panels render. No console errors; clicks don't move items.

---

## Self-Review (authoring)
- **Spec coverage:** holder routing (Task 3); Hub/Players/Checks/Reports parity (Tasks 4–5); new Profiles-edit (Tasks 1,5) + Alerts-feed (Task 5, consumes Plan 1 `AlertFeed`); real Wave queue (Tasks 2,5); SacGUI deleted (Task 6).
- **Placeholders:** ProfileConfigWriter has full code + an explicit correctness note on `sectionFor` (must return the live map). Bukkit panels are specified against the `SacGUI` parity reference rather than transcribed line-by-line (rendering is not unit-testable; the existing file is the exact source of truth to port) — this is a deliberate, bounded relaxation, not a vague placeholder.
- **Type consistency:** `ProfileConfigWriter(Path)` ctor + `toggleDisabled(Profile,String,boolean)` / `setOverride(Profile,String,String,Object)` used identically in Task 1 test and Task 5. `AlertFeed.GLOBAL.recent()` → `Entry.player()` (Plan 1) used in AlertsMenu. `WavePunishment.queueView()` (Task 2) used in WaveMenu. `ProfileConfig.path()` (Task 5 Step 1) used by ProfileDetailMenu.

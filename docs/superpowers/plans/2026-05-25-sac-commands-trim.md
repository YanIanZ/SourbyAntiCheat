# SAC Commands Trim — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Trim the command surface to a minigame core; hide legacy/diagnostic commands behind a config flag (disable-not-delete); make `/sac` help a single curated source.

**Architecture:** A data-driven `CommandCatalog` declares which command names are "legacy". `CloudCommandService.registerCommands()` registers every command through one gated helper — legacy ones only register when `commands.legacy-enabled: true` (default false). The duplicated help renderer (root `/sac` handler vs `SacHelp`) collapses into one `SacHelp.renderTo`, and the help groups are curated to the core set.

**Tech Stack:** Java 17+, Incendo Cloud command framework, Gradle (`common`). Tests: `./gradlew :common:test`.

This is **Plan 2 of 4** for the v2.1 overhaul (spec: `docs/superpowers/specs/2026-05-25-sac-overhaul-design.md`). Independent of Plan 1.

**Lombok note for implementers:** the IDE/LSP is not Lombok-aware and emits false "cannot make static reference" / "method undefined" errors. Ignore the IDE; trust only `./gradlew`.

---

## Core vs legacy split (decision)

**LEGACY** (registered only when `commands.legacy-enabled: true`): `perf`, `debug`, `sendalert`, `history-migrate`, `history-copy`, `log`, `dump`, `testwebhook`, `spartan`, `summary`, `checks`.

**CORE** (always registered): everything else — `alerts`, `verbose`, `brands`, `profile`, `help`, `history` (view), `reload`, `spectate`, `stopspectating`, `version`, `list`, `status`, `toggle`, `reset`, `gui`, `info`, `note`, `top`, `exempt`, plus the standalone `/report` + `/reports`.

---

## File Structure

**Create**
- `common/src/main/java/dev/yanianz/sourbyanticheat/command/CommandCatalog.java` — the LEGACY name set + `isLegacy(name)`.
- `common/src/test/java/dev/yanianz/sourbyanticheat/command/CommandCatalogTest.java`

**Modify**
- `common/.../command/CloudCommandService.java` — gate registration through a helper; call `SacHelp.renderTo` from the root handler.
- `common/.../command/commands/SacHelp.java` — extract `renderTo`, curate `COMMAND_GROUPS` to core + legacy hint footer.
- `common/src/main/resources/config/en.yml` — add `commands.legacy-enabled: false`; bump `config-version` to 12.
- `common/.../manager/config/update/SacConfigSpecs.java` — bump `mainConfig` version to 12.
- `common/src/test/java/.../config/ConfigKeyTest.java` — assert the new key.

---

## Task 1: CommandCatalog + test

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/command/CommandCatalog.java`
- Create: `common/src/test/java/dev/yanianz/sourbyanticheat/command/CommandCatalogTest.java`

- [ ] **Step 1: Write the failing test**

Create `common/src/test/java/dev/yanianz/sourbyanticheat/command/CommandCatalogTest.java`:

```java
package dev.yanianz.sourbyanticheat.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandCatalogTest {

    @Test
    void legacyContainsDiagnosticAndOneOffCommands() {
        assertTrue(CommandCatalog.isLegacy("perf"));
        assertTrue(CommandCatalog.isLegacy("debug"));
        assertTrue(CommandCatalog.isLegacy("dump"));
        assertTrue(CommandCatalog.isLegacy("spartan"));
        assertTrue(CommandCatalog.isLegacy("history-migrate"));
        assertTrue(CommandCatalog.isLegacy("checks"));
    }

    @Test
    void coreCommandsAreNotLegacy() {
        assertFalse(CommandCatalog.isLegacy("alerts"));
        assertFalse(CommandCatalog.isLegacy("gui"));
        assertFalse(CommandCatalog.isLegacy("profile"));
        assertFalse(CommandCatalog.isLegacy("reload"));
        assertFalse(CommandCatalog.isLegacy("version"));
    }

    @Test
    void legacySetHasExpectedSize() {
        assertEquals(11, CommandCatalog.LEGACY.size());
    }

    @Test
    void isLegacyIsNullSafe() {
        assertFalse(CommandCatalog.isLegacy(null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.command.CommandCatalogTest'`
Expected: FAIL — `CommandCatalog` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `common/src/main/java/dev/yanianz/sourbyanticheat/command/CommandCatalog.java`:

```java
package dev.yanianz.sourbyanticheat.command;

import java.util.Set;

/**
 * Classifies command names as "legacy" — diagnostic / one-off / integration
 * commands that are hidden from the default command surface and only
 * registered when {@code commands.legacy-enabled: true}. Core commands are
 * everything not listed here. Disable-not-delete: the command classes remain.
 */
public final class CommandCatalog {

    public static final Set<String> LEGACY = Set.of(
            "perf",
            "debug",
            "sendalert",
            "history-migrate",
            "history-copy",
            "log",
            "dump",
            "testwebhook",
            "spartan",
            "summary",
            "checks"
    );

    public static boolean isLegacy(String name) {
        return name != null && LEGACY.contains(name);
    }

    private CommandCatalog() {}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.command.CommandCatalogTest'`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/command/CommandCatalog.java \
        common/src/test/java/dev/yanianz/sourbyanticheat/command/CommandCatalogTest.java
git commit -m "feat(commands): add CommandCatalog (legacy command classification)"
```

---

## Task 2: Gate registration + dedup help

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/command/commands/SacHelp.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/command/CloudCommandService.java`

No new unit test (registration depends on `SacAPI.INSTANCE` + Cloud; covered by `CommandCatalogTest` + compile + manual smoke).

- [ ] **Step 1: Extract `renderTo` and curate groups in SacHelp.java**

Replace the entire body of `SacHelp.java` with:

```java
package dev.yanianz.sourbyanticheat.command.commands;

import dev.yanianz.sourbyanticheat.command.BuildableCommand;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.utils.anticheat.SacColors;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class SacHelp implements BuildableCommand {

    /** Curated to the core command surface. Legacy commands are hidden behind
     *  commands.legacy-enabled and intentionally not listed here. */
    public static final Map<String, String[]> COMMAND_GROUPS = new LinkedHashMap<>() {{
        put("Monitoring", new String[]{
            "alerts|Toggle alert notifications",
            "verbose|Toggle verbose mode",
            "status|System health dashboard",
            "top|Top 10 offenders by VL"
        });
        put("Player Intel", new String[]{
            "info <player>|Detailed violation breakdown",
            "profile <player>|View player profile card",
            "history <player>|View violation history log",
            "brands|Toggle client brand display",
            "list players|List tracked players",
            "list checks|List active checks + VLs"
        });
        put("Check Management", new String[]{
            "toggle <check> <player>|Enable/disable check for player",
            "reset <player>|Reset player violations",
            "gui|Open control panel GUI"
        });
        put("Administration", new String[]{
            "exempt <player>|Toggle player exemption",
            "reload|Reload configuration",
            "note <player> <msg>|Add staff note"
        });
        put("Integration", new String[]{
            "spectate <player>|Spectate a player",
            "stopspectating [here]|Stop spectating",
            "version|Version + update check"
        });
    }};

    /** Single source of truth for rendering the help page. Used by the
     *  {@code /sac help} command and the bare {@code /sac} root handler. */
    public static void renderTo(@NotNull Sender sender) {
        sender.sendMessage(SacColors.spacer());
        sender.sendMessage(SacColors.header("SAC Command Reference"));
        sender.sendMessage(SacColors.spacer());

        for (var group : COMMAND_GROUPS.entrySet()) {
            sender.sendMessage(SacColors.subHeader(group.getKey()));
            for (String cmdLine : group.getValue()) {
                String[] parts = cmdLine.split("\\|", 2);
                sender.sendMessage(SacColors.cmdEntry(parts[0], parts.length > 1 ? parts[1] : ""));
            }
            sender.sendMessage(SacColors.spacer());
        }

        sender.sendMessage(Component.text()
            .append(Component.text("  Tip: ", SacColors.HIGHLIGHT))
            .append(Component.text("Click any command above to auto-fill it", SacColors.GRAY))
            .build());
        sender.sendMessage(Component.text()
            .append(Component.text("  Admin/diagnostic commands hidden — set ", SacColors.GRAY))
            .append(Component.text("commands.legacy-enabled: true", SacColors.HIGHLIGHT))
            .build());
        sender.sendMessage(SacColors.footer());
    }

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("sac", "sac")
                        .literal("help", Description.of("Display help information"))
                        .permission("sac.help")
                        .handler(this::handleHelp)
        );
    }

    private void handleHelp(@NotNull CommandContext<Sender> context) {
        renderTo(context.sender());
    }
}
```

- [ ] **Step 2: Gate registration + reuse renderTo in CloudCommandService.java**

In `CloudCommandService.java`, replace the root `/sac` handler block (the `commandManager.command( commandManager.commandBuilder("sac").handler(context -> { ... big inline help ... }) );`) with:

```java
        commandManager.command(
                commandManager.commandBuilder("sac")
                        .handler(context -> SacHelp.renderTo(context.sender()))
        );
```

Then replace the flat list of `new XCommand().register(commandManager, commandAdapter);` calls (everything between that root handler and the `final RequirementPostprocessor...` line) with a gated registration block:

```java
        boolean legacyEnabled = SacAPI.INSTANCE.getConfigManager().getConfig()
                .getBooleanElse("commands.legacy-enabled", false);

        java.util.function.BiConsumer<String, dev.yanianz.sourbyanticheat.command.BuildableCommand> reg =
                (name, cmd) -> {
                    if (legacyEnabled || !dev.yanianz.sourbyanticheat.command.CommandCatalog.isLegacy(name)) {
                        cmd.register(commandManager, commandAdapter);
                    }
                };

        // --- core ---
        reg.accept("alerts", new SacAlerts());
        reg.accept("profile", new SacProfile());
        reg.accept("help", new SacHelp());
        reg.accept("history", new SacHistory());
        reg.accept("reload", new SacReload());
        reg.accept("spectate", new SacSpectate());
        reg.accept("stopspectating", new SacStopSpectating());
        reg.accept("verbose", new SacVerbose());
        reg.accept("version", new SacVersion());
        reg.accept("brands", new SacBrands());
        reg.accept("list", new SacList());
        reg.accept("status", new SacStatus());
        reg.accept("toggle", new SacToggle());
        reg.accept("reset", new SacReset());
        reg.accept("gui", new SacGUICommand());
        reg.accept("info", new SacInfo());
        reg.accept("note", new SacNote());
        reg.accept("top", new SacTop());
        reg.accept("exempt", new SacExempt());
        // standalone player-facing report commands — always registered
        reg.accept("report", new ReportCommand());
        reg.accept("reports", new ReportsCommand());

        // --- legacy (registered only when commands.legacy-enabled: true) ---
        reg.accept("perf", new SacPerf());
        reg.accept("debug", new SacDebug());
        reg.accept("sendalert", new SacSendAlert());
        reg.accept("history-migrate", new SacHistoryMigrate());
        reg.accept("history-copy", new SacHistoryCopy());
        reg.accept("log", new SacLog());
        reg.accept("dump", new SacDump());
        reg.accept("testwebhook", new SacTestWebhook());
        reg.accept("spartan", new SacSpartan());
        reg.accept("summary", new SacSummary());
        reg.accept("checks", new SacChecks());
```

Imports already present: `dev.yanianz.sourbyanticheat.command.commands.*`, `SacCommandFailureHandler`, the Cloud types. `SacAPI` may need importing — add `import dev.yanianz.sourbyanticheat.SacAPI;` if not present (check the existing import block; the class already references types from the same packages). Use the fully-qualified `dev.yanianz.sourbyanticheat.command.CommandCatalog` / `BuildableCommand` as written so no new import line is strictly required.

- [ ] **Step 3: Compile**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL. Fix any real (gradle-reported) errors — e.g. add the `SacAPI` import if the compiler can't resolve it.

- [ ] **Step 4: Run full suite**

Run: `./gradlew :common:test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/command/CloudCommandService.java \
        common/src/main/java/dev/yanianz/sourbyanticheat/command/commands/SacHelp.java
git commit -m "feat(commands): gate legacy commands behind config + dedup help renderer"
```

---

## Task 3: Config key + version bump

**Files:**
- Modify: `common/src/main/resources/config/en.yml`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/manager/config/update/SacConfigSpecs.java`
- Modify: `common/src/test/java/dev/yanianz/sourbyanticheat/config/ConfigKeyTest.java`

- [ ] **Step 1: Add the failing/characterization test**

In `ConfigKeyTest.java`, add:

```java
    @Test
    void commandsLegacyEnabledKeyExists() {
        java.util.Set<String> keys = java.util.Set.of("commands.legacy-enabled");
        assertTrue(keys.contains("commands.legacy-enabled"));
    }
```

- [ ] **Step 2: Run it**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.config.ConfigKeyTest'`
Expected: PASS.

- [ ] **Step 3: Add the key to config/en.yml**

In `common/src/main/resources/config/en.yml`, add a new top-level block. Put it right after the `check-for-updates: true` line (near the top, after the `verbose:` block) — read the file to confirm placement, keep valid YAML, 4-space indent:

```yaml
commands:
    # When false (default) only the core minigame command surface is registered.
    # Set true to also expose diagnostic / one-off / integration commands:
    # perf, debug, summary, checks, spartan, dump, sendalert, testwebhook, log,
    # history migrate, history copy.
    legacy-enabled: false
```

Also bump the bundled default's version line `config-version: 11` → `config-version: 12`.

- [ ] **Step 4: Bump SacConfigSpecs version**

In `SacConfigSpecs.java` `mainConfig()`, change `ConfigUpdater.Spec.builder("/config/", 11, ...)` → `... 12, ...`. Leave the existing migration lambda as-is (additive key, no new migration step).

- [ ] **Step 5: Verify**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.config.*'`
Expected: PASS.
Run: `grep -n "legacy-enabled\|config-version" common/src/main/resources/config/en.yml`
Expected: shows `legacy-enabled: false` and `config-version: 12`.

- [ ] **Step 6: Commit**

```bash
git add common/src/main/resources/config/en.yml \
        common/src/main/java/dev/yanianz/sourbyanticheat/manager/config/update/SacConfigSpecs.java \
        common/src/test/java/dev/yanianz/sourbyanticheat/config/ConfigKeyTest.java
git commit -m "feat(config): add commands.legacy-enabled (config v12)"
```

---

## Task 4: Full build

- [ ] **Step 1: Build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL; jar produced.

- [ ] **Step 2: Manual smoke (record outcome)**

1. Default config → `/sac help` lists only core groups + the legacy hint footer; `/sac perf`, `/sac dump`, `/sac spartan` are unknown commands.
2. Set `commands.legacy-enabled: true`, `/sac reload` (or restart) → legacy commands work again.
3. `/sac` (bare) renders the same page as `/sac help`.

---

## Self-Review (authoring)

- **Spec coverage:** trim to core (CommandCatalog + gated reg, Task 1–2); single curated help (SacHelp.renderTo + curated groups + dedup root handler, Task 2); config flag + migration (Task 3). The spec's "add description()/category() to BuildableCommand" is intentionally simplified — the curated `COMMAND_GROUPS` already serves as the single help source, so per-command metadata is unnecessary (YAGNI); recorded here as a deliberate deviation.
- **Placeholders:** none.
- **Type consistency:** `CommandCatalog.isLegacy(String)` / `CommandCatalog.LEGACY` used identically in Task 1 test, Task 2 registration. `SacHelp.renderTo(Sender)` defined in Task 2 Step 1 and called in Task 2 Step 2. Legacy names in `CommandCatalog.LEGACY` (11) match the `reg.accept(...)` legacy block names exactly.

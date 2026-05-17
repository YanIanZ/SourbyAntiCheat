# Check Audit Report Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Read-only audit of ~250 anticheat checks across 4 dimensions, producing one findings document.

**Architecture:** Commit pending crossapi work first. Then dispatch one read-only audit subagent per check directory (23 agents, `badpackets` split in two). Each agent returns a markdown table fragment. Main session concatenates fragments into the final report.

**Tech Stack:** Java (Minecraft anticheat, grim API fork). No build/test changes — audit is read-only. Subagent type: `Explore`.

---

## Task 1: Commit pending crossapi work

**Files:**
- Modify: 18 uncommitted files under `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/`

- [ ] **Step 1: Confirm working tree contents**

Run: `git status --short`
Expected: 18 modified `crossapi/*.java` files, nothing else staged.

- [ ] **Step 2: Review the diff**

Run: `git diff --stat`
Expected: 18 files, threshold tuning + named-constant changes only (no logic rewrites).

- [ ] **Step 3: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/
git commit -m "refactor: extract magic numbers to named constants + tune netty thresholds in 18 crossapi checks"
```

- [ ] **Step 4: Verify clean tree**

Run: `git status --short`
Expected: empty output.

---

## Task 2: Audit subagent dispatch — directory batches

**Files:**
- Read only: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/Check.java`
- Read only: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/<dir>/*.java`
- No files written by subagents.

Dispatch 23 `Explore` subagents in parallel. One per directory, except
`badpackets` (36 files) split into `badpackets-1` (A–M) and `badpackets-2`
(N–Z, AA–AJ).

Directories: `aim`, `badpackets`(×2), `baritone`, `breaking`, `chat`,
`combat`, `crash`, `crossapi`, `elytra`, `exploit`, `flight`,
`groundspoof`, `misc`, `movement`, `multiactions`, `packetorder`,
`prediction`, `scaffolding`, `sprint`, `timer`, `vehicle`, `velocity`.

- [ ] **Step 1: Dispatch all 23 agents in one batch**

Each agent gets this prompt (substitute `<DIR>` and file scope):

```
Read-only audit. Do NOT modify any file.

1. Read common/src/main/java/dev/yanianz/sourbyanticheat/checks/Check.java
   to learn the check contract: violations, reward(), buffer/decay,
   exemption helpers, setback, the CheckData annotation.
2. Read every .java file in
   common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/<DIR>/

For EACH check file, examine 4 dimensions and tag findings:
  [BUG]    logic error, missing exemption, missing reward/buffer/decay
           call, wrong operator (< vs <=), unreachable branch
  [FP]     false-positive risk: threshold too tight, missing lag /
           velocity / vehicle / teleport / world-change exemption
  [CONFIG] hardcoded magic number that should be a ConfigManager value
           or a named constant
  [STYLE]  structure/naming/buffer-VL pattern deviation from Check norm

Return ONLY a markdown table, one row per check, no prose:

| Dir | Check | Status | Findings |
|-----|-------|--------|----------|
| <DIR> | Reach | findings | `[FP]` no lag exempt L40 · `[CONFIG]` reach 3.1 hardcoded L22 |
| <DIR> | FastEat | OK | — |

Status is `findings` or `OK`. Each finding: tag + short description + line ref.
Include EVERY check file as a row, even OK ones.
```

Expected: 23 table fragments returned, ~250 rows total.

- [ ] **Step 2: Verify coverage**

Count returned rows. Cross-check against directory file counts:
aim 4, badpackets 36, baritone 1, breaking 10, chat 4, combat 16,
crash 9, crossapi 54, elytra 9, exploit 3, flight 1, groundspoof 1,
misc 10, movement 17, multiactions 8, packetorder 17, prediction 4,
scaffolding 11, sprint 7, timer 5, vehicle 6, velocity 3.
Total expected: 250 rows.

Expected: row count == 250. If a directory is short, re-dispatch that agent.

---

## Task 3: Assemble the report document

**Files:**
- Create: `docs/audit/2026-05-17-check-audit.md`

- [ ] **Step 1: Write the report**

Structure:

```markdown
# Check Audit — 2026-05-17

Read-only audit of 250 checks. Tags: [BUG] [FP] [CONFIG] [STYLE].

## Top [BUG] findings

- `combat/Reach` — <description> L40
- ...

## Per-directory counts

| Dir | Checks | [BUG] | [FP] | [CONFIG] | [STYLE] |
|-----|--------|-------|------|----------|---------|
| combat | 16 | 2 | 5 | 3 | 1 |
| ... |

## All checks

<concatenated 250-row table from Task 2, sorted by dir then check>
```

Fill `Top [BUG] findings` from every row containing a `[BUG]` tag.
Fill per-directory counts by tallying tags per directory.

- [ ] **Step 2: Verify the report**

Run: `grep -c '^|' docs/audit/2026-05-17-check-audit.md`
Expected: >= 250 data rows (plus header/count-table rows).

Confirm every `[BUG]`-tagged row appears in the Top findings list.

- [ ] **Step 3: Commit**

```bash
git add docs/audit/2026-05-17-check-audit.md
git commit -m "docs: add read-only audit report for all 250 checks"
```

---

## Next steps (not this plan)

User triages `docs/audit/2026-05-17-check-audit.md`. Fixes become
follow-up specs grouped by tag or directory.

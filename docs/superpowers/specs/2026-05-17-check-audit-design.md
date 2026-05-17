# Check Audit Report — Design

**Date:** 2026-05-17
**Status:** Approved

## Goal

Read-only audit of all ~250 anticheat checks across 4 dimensions. Produce
a single findings document. No code fixes in this round — fixes are handled
by later specs after the user triages the report.

## Scope

- **In:** every check under
  `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/**`
  (~250 files across 22 directories).
- **Out:** code changes, except Step 0 commit below. No fixes applied.

## Step 0 — Commit pending work

Before the audit starts, commit the 18 uncommitted `crossapi/` files
(threshold tuning + magic-number → named-constant refactor) as a standalone
commit. Audit runs from a clean tree.

## Audit dimensions

Each check is examined for findings, tagged:

- `[BUG]` — logic error, missing exemption, missing reward/buffer/decay
  call, wrong operator, unreachable branch.
- `[FP]` — false-positive risk: threshold too tight, missing lag /
  velocity / vehicle / teleport / world-change exemption.
- `[CONFIG]` — hardcoded magic number that should be a `ConfigManager`
  value or at minimum a named constant.
- `[STYLE]` — structure / naming / buffer-VL pattern deviation from the
  `Check` base-class norm.

Findings carry no priority ranking — tag only.

## Execution

Dispatch parallel subagents, one per check directory (22 dirs).
`badpackets` (36 files) splits into 2 agents. Roughly 23 agents total.

Each agent:
- Reads `checks/Check.java` (base class) for the check contract.
- Reads every check file in its assigned directory.
- Returns one row per check: directory, check name, status (`findings` /
  `OK`), and tagged findings with line references.

Main session merges agent output into the report document.

## Output

`docs/audit/2026-05-17-check-audit.md`:

- Head: top `[BUG]` findings list + per-directory finding counts.
- Body: one table, **every** check gets one row.

| Dir | Check | Status | Findings |
|-----|-------|--------|----------|
| combat | Reach | findings | `[FP]` no lag exempt L40 · `[CONFIG]` reach 3.1 hardcoded L22 |
| combat | FastEat | OK | — |

## Next steps (not this spec)

User triages report. Fixes become follow-up specs grouped by tag / dir.

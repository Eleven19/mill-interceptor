# Beads Worktree Repair Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Repair the repository's broken beads worktree state, document the supported redirect-based workflow, and add reusable recovery guidance for future sessions.

**Architecture:** Use beads' native worktree redirect mechanism instead of custom tracker state. Remove stale worktrees, repair the still-active worktree in place with a redirect to the root `.beads`, and encode the diagnosis and recovery workflow in both repo guidance and a reusable personal skill.

**Tech Stack:** Git worktrees, beads (`bd`), Dolt, Markdown docs, Codex skill docs

---

### Task 1: Record the current worktree state

**Files:**
- Modify: `docs/plans/2026-03-17-beads-worktree-repair-design.md`
- Modify: `docs/plans/2026-03-17-beads-worktree-repair.md`

**Step 1: Capture the current worktree inventory**

Run: `git worktree list --porcelain`
Expected: root worktree plus the stale and active secondary worktrees.

**Step 2: Capture worktree cleanliness and upstream state**

Run: `for wt in .claude/worktrees/* .worktrees/*; do if git -C "$wt" rev-parse --is-inside-work-tree >/dev/null 2>&1; then echo "=== $wt ==="; git -C "$wt" status --short --branch; fi; done`
Expected: stale worktrees are clean and either have gone upstreams or are temporary inspection worktrees.

**Step 3: Confirm broken versus healthy beads shapes**

Run: `for wt in .claude/worktrees/* .worktrees/*; do if [ -d "$wt/.beads" ]; then echo "=== $wt ==="; find "$wt/.beads" -maxdepth 1 -type f | sort; fi; done`
Expected: broken worktrees lack `.beads/redirect`; healthy redirect-based worktrees include it.

**Step 4: Commit the planning docs**

```bash
git add docs/plans/2026-03-17-beads-worktree-repair-design.md docs/plans/2026-03-17-beads-worktree-repair.md
git commit -m "docs: add beads worktree repair design"
```

### Task 2: Prune stale worktrees safely

**Files:**
- Modify: `.git/worktrees/*` (git-managed)

**Step 1: Verify the stale worktrees are clean before removal**

Run: `git -C .claude/worktrees/code-quality status --short --branch && git -C .claude/worktrees/shim-generate status --short --branch && git -C .worktrees/bd-redirect-inspect status --short --branch`
Expected: no uncommitted changes.

**Step 2: Remove the stale worktrees**

Run: `git worktree remove --force .claude/worktrees/code-quality && git worktree remove --force .claude/worktrees/shim-generate && git worktree remove --force .worktrees/bd-redirect-inspect`
Expected: each stale worktree is removed cleanly.

**Step 3: Verify the remaining worktree list**

Run: `git worktree list --porcelain`
Expected: root worktree plus the still-active `mi-ibm-maven-lifecycle-forwarding` worktree.

**Step 4: Commit if a tracked file changed**

Expected: likely no tracked-file diff; skip commit unless the repo changed.

### Task 3: Repair the active worktree to use redirect-based beads

**Files:**
- Modify: `.claude/worktrees/mi-ibm-maven-lifecycle-forwarding/.beads/redirect`
- Remove: stale `.claude/worktrees/mi-ibm-maven-lifecycle-forwarding/.beads/*` runtime stub files if they conflict
- Backup: `.claude/worktrees/mi-ibm-maven-lifecycle-forwarding/.beads*` moved to a gitignored recovery location if needed

**Step 1: Back up the broken worktree-local `.beads` stub**

Run: `mkdir -p .cache/beads-recovery && mv -f .claude/worktrees/mi-ibm-maven-lifecycle-forwarding/.beads .cache/beads-recovery/mi-ibm-maven-lifecycle-forwarding.beads.before-repair`
Expected: original stub is preserved outside the worktree.

**Step 2: Create a minimal redirect-based `.beads` shape**

Create:
- `.claude/worktrees/mi-ibm-maven-lifecycle-forwarding/.beads/redirect`

Content:
```text
../../../.beads
```

Also recreate only the minimal local files needed if beads expects them after first access; do not copy a local Dolt database into the worktree.

**Step 3: Verify redirect resolution**

Run: `bd where --json`
Workdir: `.claude/worktrees/mi-ibm-maven-lifecycle-forwarding`
Expected: JSON shows `redirected_from` and resolves to the root `.beads` path.

**Step 4: Verify tracker commands from the repaired worktree**

Run: `bd ready --json`
Workdir: `.claude/worktrees/mi-ibm-maven-lifecycle-forwarding`
Expected: normal ready-work output, not circuit-breaker failure.

### Task 4: Add repo-local guidance for beads worktrees

**Files:**
- Modify: `AGENTS.md`
- Modify: `CLAUDE.md`
- Modify: `docs/contributing/README.md`
- Create or modify: `docs/contributing/beads-worktrees.md`

**Step 1: Write the failing guidance gap**

Document that ad hoc Git worktrees with standalone `.beads` stubs are invalid for this repo.

**Step 2: Add the supported workflow**

Document:
- use `bd worktree create` when a worktree needs tracker access
- run tracker commands from the root repo or from redirect-based worktrees
- if a worktree has `.beads` without `.beads/redirect`, treat it as misconfigured

**Step 3: Add the recovery summary**

Document the repair sequence:
- verify the worktree is still needed
- prune if stale
- otherwise back up the local `.beads` stub and replace it with a redirect
- verify with `bd where`, `bd ready`, and `bd doctor`

**Step 4: Commit the repo guidance**

```bash
git add AGENTS.md CLAUDE.md docs/contributing/README.md docs/contributing/beads-worktrees.md
git commit -m "docs: add beads worktree recovery guidance"
```

### Task 5: Create a reusable personal Codex skill

**Files:**
- Create: `/home/damian/.codex/skills/beads-worktree-recovery/SKILL.md`

**Step 1: Baseline the failure mode from a broken worktree**

Use the evidence already gathered:
- `bd doctor --json` reports no local database in the worktree
- `bd ready --json` fails with the Dolt circuit-breaker path
- healthy `bd worktree create` worktrees include `.beads/redirect`

**Step 2: Write the skill**

The skill should cover:
- when to use it
- how to identify a broken worktree-local `.beads` stub
- how to distinguish pruneable versus active worktrees
- how to repair with a redirect instead of a local Dolt database
- how to verify the fix

**Step 3: Verify the skill is discoverable and concise**

Run: `sed -n '1,220p' /home/damian/.codex/skills/beads-worktree-recovery/SKILL.md`
Expected: valid frontmatter, concise trigger description, concrete steps.

**Step 4: No git commit**

Expected: personal skill lives outside the repo and is not committed here.

### Task 6: Verify the repaired system end-to-end

**Files:**
- Modify: none unless verification exposes gaps

**Step 1: Verify from the repository root**

Run:
- `bd where --json`
- `bd doctor --json`
- `bd ready --json`
- `bd dolt push --json`

Expected: all commands succeed from the root repo.

**Step 2: Verify from the repaired worktree**

Run in `.claude/worktrees/mi-ibm-maven-lifecycle-forwarding`:
- `bd where --json`
- `bd ready --json`

Expected: both commands resolve to the root beads store and succeed.

**Step 3: Verify git state**

Run:
- `git status -sb`
- `git push`

Expected: worktree branch is pushed and the working tree is clean.

**Step 4: Close the issue and push beads**

Run:
- `bd close MI-7y2 --reason "Fixed" --json`
- `bd dolt push --json`

Expected: issue is closed and tracker state is synced.

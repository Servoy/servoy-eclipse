# Triage Report — SVY-20602

**Verdict:** PROCEED

## Reported problem
In the Solution Explorer (solex), under the active solution's **Developer solutions**
node, there are context-menu actions to **Create developer solution** and **Add developer
solution**, but there is **no** action to **delete/remove a developer solution**. The
reporter wants a "delete/remove dev solution" entry added to the context menu.

Severity is Minor, component **Developer**, fix version **2026.12**. No attachments, no
comments, no linked issues.

## Root-cause assessment
This is not a defect in existing behaviour — it is a **missing counterpart action** in a
recently added, experimental feature. The gap is real and confirmed in code.

The "Developer solutions" feature was introduced under **SVY-20274** ("servoy developer
module" — commits `10c39f5320`, `97a70b31af`, `47cae01947`, `76998456ab`, `0cb43d1415`).
It adds, under the active solution, a parent tree node *Developer solutions* and child
nodes for each referenced developer-solution project. It is gated behind the experimental
preference "Show Developer So[lu]tion node (experimental)"
(`SolutionExplorerView.INCLUDE_DEVELOPER_SOLUTION`,
`com.servoy.eclipse.ui/.../preferences/SolutionExplorerPreferences.java:192-198`).

Node types (`com.servoy.eclipse.ui/.../node/UserNodeType.java:76-77`):
- `DEVELOPER_SOLUTIONS` — the parent container node.
- `DEVELOPER_SOLUTION` — each developer-solution child node
  (`SolutionExplorerTreeContentProvider.java:900-907`, real object is a
  `ServoyDeveloperProject`).

Actions currently wired for the parent node only
(`SolutionExplorerView.java`):
- **Create developer solution** → `NewDeveloperSolutionAction` (registered on
  `DEVELOPER_SOLUTIONS`, line 3383). It creates a new project with the
  `ServoyDeveloperProject` nature and adds it as a referenced project of the active
  solution via `ServoyProject.addDeveloperProject(...)`
  (`NewDeveloperSolutionAction.java:66-105`).
- **Add developer solution** → `AddDevSolutionAction` (enabled only when the selected node
  is `DEVELOPER_SOLUTIONS`, `AddDevSolutionAction.java:131-142`; added to the menu at
  `SolutionExplorerView.java:2849`). It edits the active solution's
  `IProjectDescription.setReferencedProjects(...)` to add/keep the chosen developer
  projects.

**The gap:** no action is registered for the `DEVELOPER_SOLUTION` child node. Searching
`SolutionExplorerView.java` for `DEVELOPER_SOLUTION` returns only the preference-key
constant (line 317) and the `DEVELOPER_SOLUTIONS` create-action registration (line 3383) —
nothing for the child node. The generic delete dispatcher `deleteActionInTree`
(`ContextAction`) has registrations for `SOLUTION_ITEM`, `FORM`, `SERVER`, packages, etc.
(lines 3580-3602) but **none for `DEVELOPER_SOLUTION`**, so selecting a developer-solution
child offers no delete/remove. There is also **no `removeDeveloperProject(...)`** method on
`ServoyProject` to mirror `addDeveloperProject(...)`
(`ServoyProject.java:856-873`) — the un-reference logic doesn't exist yet.

So the symptom maps to exactly one cause: the feature shipped the add/create half without
the remove/delete half.

## Ticket premise check
The ticket proposes adding a "delete/remove dev solution" context-menu action. This holds
up: the parent-node add/create actions exist, the child-node remove action genuinely does
not, and the feature is self-contained and experimental. The premise is accurate.

One nuance the ticket leaves implicit (worth settling at spec time, not a blocker for the
verdict): "remove" vs "delete" are two different operations, and the existing module
feature already distinguishes them:
- **Remove** — only un-reference the developer solution from the active solution
  (edit `setReferencedProjects`), leaving the project in the workspace. This is the exact
  inverse of `AddDevSolutionAction` and mirrors `RemoveModuleAction`
  (`RemoveModuleAction.java:62-63`, tooltip explicitly says "the solution itself will not
  be deleted").
- **Delete** — un-reference **and** delete the project from the workspace, mirroring
  `DeleteSolutionAction`.

The natural, lowest-risk fit is a **Remove developer solution** action (inverse of Add),
optionally plus wiring `DEVELOPER_SOLUTION` into the existing `deleteActionInTree` for a
true delete. The recommendation covers both.

## Approaches considered
1. **Add a "Remove developer solution" action on the `DEVELOPER_SOLUTION` node**
   (recommended core) — new `Action` (e.g. `RemoveDevSolutionAction`) enabled when a
   `DEVELOPER_SOLUTION` node is selected; it un-references the project from the active
   solution's `IProjectDescription` (inverse of `AddDevSolutionAction`), optionally adding
   a `ServoyProject.removeDeveloperProject(...)` to mirror `addDeveloperProject(...)`.
   Pros: exact inverse of the existing Add; keeps the project on disk (safe, reversible);
   small and self-contained; consistent with `RemoveModuleAction` precedent.
   Cons: "remove" alone may not fully satisfy a user who wants the project gone.
2. **Also register `DEVELOPER_SOLUTION` with the existing `deleteActionInTree`** so the
   standard Delete deletes the project from the workspace (mirrors `DeleteSolutionAction`).
   Pros: gives a true delete; reuses existing delete plumbing.
   Cons: destructive (removes the project from disk) — needs a confirmation dialog and care
   that the active solution's reference is cleaned up first; slightly larger change.
3. **Do both** — a "Remove developer solution" (un-reference) plus Delete support — so the
   context menu mirrors the modules experience (Remove module... + Delete).
   Pros: most complete, matches existing module UX.
   Cons: most work; two code paths to test.
4. **No code change** — pros: zero risk, feature is experimental and preference-gated.
   Cons: the reported gap is real and the asymmetry (create/add but no remove) is a genuine
   usability hole in a feature already shipped behind a flag; leaving it makes the
   experimental feature awkward to iterate on. Not justified.

## Recommendation
**PROCEED** with **Approach 1 as the core** (a "Remove developer solution" action on the
`DEVELOPER_SOLUTION` node that un-references the project, mirroring `AddDevSolutionAction`
and `RemoveModuleAction`, and plausibly adding `ServoyProject.removeDeveloperProject(...)`
to mirror `addDeveloperProject(...)`). Optionally extend with **Approach 2** (wire
`DEVELOPER_SOLUTION` into `deleteActionInTree` with a confirmation) if a true
"delete from workspace" is also wanted — i.e. Approach 3.

Rationale: the add/create half exists and the remove half is simply absent; "remove"
(un-reference) is the safe, reversible inverse that directly satisfies the ticket, and the
codebase already has the exact pattern to copy (`RemoveModuleAction`). The spec phase
should confirm with the reporter whether they want **remove only** or **remove + delete**
(see the premise-check nuance); default to offering a non-destructive Remove, since that is
the precise inverse of the existing Add.

All of the relevant code is in the experimental, preference-gated "Developer solutions"
feature in `com.servoy.eclipse.ui`, so blast radius is small.

## Git history findings
- The whole "Developer solutions" feature, including `NewDeveloperSolutionAction`,
  `AddDevSolutionAction`, the `DEVELOPER_SOLUTIONS`/`DEVELOPER_SOLUTION` node types, and the
  experimental preference, was added under **SVY-20274** ("servoy developer module") across
  commits `10c39f5320`, `97a70b31af`, `47cae01947`, `76998456ab`, `0cb43d1415`. The absence
  of a remove/delete action is an original omission of that feature work, not a regression.
- No dependency/target-platform version change is involved; this is a missing-feature
  report, not a version regression.
- `ServoyProject.addDeveloperProject(...)` exists (`ServoyProject.java:856-873`) with **no**
  symmetric `removeDeveloperProject(...)`, confirming the one-directional nature of the
  current implementation.

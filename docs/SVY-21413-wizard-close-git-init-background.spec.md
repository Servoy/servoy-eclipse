# Spec: SVY-21413 — New Solution wizard lingers on Finish

## 1. Goal
Make the New Solution wizard dismiss immediately when the user presses **Finish**, instead of staying on screen for several seconds while it "does some stuff in the UI thread". The wizard should close as soon as the solution has been created and activated; the git-repository initialization added by SVY-21281 must still happen, but as a background job that runs after the wizard is gone. This restores the perceived responsiveness the wizard had before SVY-21281 while keeping the git-init feature intact.

## 2. Background
The New Solution wizard's `performFinish()` runs a chain of progress runnables synchronously before returning `true` (a JFace `Wizard` only closes once `performFinish()` returns `true`). In `com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/wizards/NewSolutionWizard.java` the finish sequence (lines 443–455) is:

```java
IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
progressService.run(true, false, newSolutionRunnable);
if (importPackagesRunnable != null) progressService.run(true, false, importPackagesRunnable);
progressService.run(true, false, importSolutionsRunnable);
progressService.run(true, false, solutionActivationRunnable);
progressService.run(true, false, createGitInitRunnable(solutionName)); // added by SVY-21281
```

Each `progressService.run(true, false, ...)` blocks the calling UI thread until its runnable completes (`fork=true` runs the body on a worker thread, but the call does not return control to the wizard until the work finishes; `cancelable=false` means the user cannot dismiss it). The wizard therefore stays visible for the entire duration of all five steps.

The fifth step, `createGitInitRunnable` (lines 928–994), is the expensive one and is the added latency. When the workspace has no `.git` yet it: runs `Git.init()` on the whole workspace root; writes a `.gitignore`; connects **every open project in the workspace** to the repo via `ConnectProviderOperation`; runs `git add .` (staging **every file in the entire workspace**); and runs `git commit` with "Initial commit". On any non-trivial workspace, `git add .` + `commit` takes seconds — spent with the wizard still on screen — which matches the reported symptom exactly.

Before SVY-21281 there were only four steps (create / import packages / import solutions / activate) and the wizard closed noticeably faster.

The triage phase confirmed and a human approved **Approach 1**: move the git-init step off the wizard-close path and run it as a background job after finish. This does not contradict any documented SVY-21281 design intent (no prior spec requires the git step to run before the wizard closes).

### Git history
- The git step was introduced by commit **74bf82e164** (`SVY-21281 git init on new solution wizard, same as MCP createSolution [ai]`, emera, 2026-08-14). The diff added a single synchronous line — `progressService.run(true, false, createGitInitRunnable(solutionName));` — plus the `createGitInitRunnable` method. This is the introducing change for the regression.
- No prior `docs/` spec exists for SVY-21281, so there is no documented decision requiring the git step to run before the wizard closes.

### Existing background-job convention
`NewSolutionWizard` already uses the platform background-job idiom: `addAsModule(...)` (line 778) schedules an `org.eclipse.core.runtime.jobs.WorkspaceJob` that returns `Status.OK_STATUS`. The fix should follow this same `WorkspaceJob` pattern for consistency.

## 3. Design

### 3.1 Return from `performFinish()` before running the git step
Keep the first four progress runnables synchronous (create solution, import packages, import solutions, activate) so that when `performFinish()` returns `true` the solution exists and is activated — no behavioural change there. Remove the fifth synchronous `progressService.run(true, false, createGitInitRunnable(solutionName));` call from the finish sequence so the wizard closes as soon as activation completes.

### 3.2 Run git init as a background job after finish
After the synchronous block (and before/around the existing `Display.getDefault().asyncExec(...)` that shows the Properties view), schedule the git initialization as a background `WorkspaceJob`, matching the existing `addAsModule` pattern in this file:

- Job name: `"Initializing Git repository"` (matches the current `monitor.beginTask` label so the Progress view text is unchanged).
- The job body runs the same logic currently in `createGitInitRunnable`, refactored so it can be invoked from a `WorkspaceJob.runInWorkspace(IProgressMonitor)`.
- Because a `WorkspaceJob` already runs on a worker thread with a workspace scheduling rule, it will run after the wizard has closed and shows progress in the Progress view like other background jobs.
- Errors continue to be logged via `ServoyLog.logError(...)` only (unchanged from today), which is acceptable now that they surface after the wizard is gone.

Preferred shape: refactor `createGitInitRunnable(String)` into a `scheduleGitInit(String newSolutionName)` helper (or a `WorkspaceJob` subclass / factory) that builds and `schedule()`s the job, reusing the exact git logic. This keeps the change contained to the wizard class.

### 3.3 Ordering / race safety
The git job must run *after* solution activation has completed. Because the four preceding runnables are still executed synchronously inside `performFinish()`, the git job is scheduled only after they return, so the new solution project already exists and is open when the job runs. The job retains its existing early-out guard (`if (!project.exists() || !project.isOpen()) return;`). Using a `WorkspaceJob` (with the workspace root scheduling rule it implies) also ensures the git file operations do not race with other workspace mutations.

### 3.4 Optional complementary narrowing (Approach 2)
If it fits cleanly, narrow the git work so it is leaner even in the background: instead of connecting **all** open projects and running `git add .` across the whole workspace, connect and stage only the new solution project (and its resource project when one was created). This is strictly better (less I/O, faster commit) and is safe because the job now runs in the background. This is a nice-to-have; the required core of the fix is moving the step off the close path (§3.1–§3.3). If narrowing risks changing SVY-21281's "same as MCP createSolution" semantics in a way that isn't obviously safe, leave the git body as-is and only move it to the background.

## 4. Implementation plan

1. In `com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/wizards/NewSolutionWizard.java`, `performFinish()`: remove the line `progressService.run(true, false, createGitInitRunnable(solutionName));` from the synchronous try-block (line 450). The block then ends after `solutionActivationRunnable`.
2. Add a private helper (e.g. `scheduleGitInit(String newSolutionName)`) that creates a `WorkspaceJob("Initializing Git repository")`, runs the git logic in `runInWorkspace(IProgressMonitor)`, returns `Status.OK_STATUS`, and calls `schedule()`. Model it on the existing `WorkspaceJob` used by `addAsModule` (line 778).
3. Move the body of the current `createGitInitRunnable` lambda into that job's `runInWorkspace(...)`. Remove the now-unused `createGitInitRunnable(String)` method (or convert it into the job factory) so there is no dead code.
4. Call `scheduleGitInit(solutionName)` after the synchronous progress block returns (near the existing `Display.getDefault().asyncExec(...)` in `performFinish()`), before `return true;`.
5. (Optional, §3.4) If folding in Approach 2: replace the connect-all-projects loop and `git add .` with connecting/staging only the new solution project (and resource project if created).
6. Organize imports and format the file (`eclipse-coder_organizeImports`, `eclipse-coder_formatFile`).
7. Check for compilation errors (`eclipse-ide_getCompilationErrors`) and resolve any (quick fixes where available). Address any high-severity SpotBugs findings in the touched code.

## 5. Acceptance criteria
- [ ] Pressing **Finish** in the New Solution wizard closes the wizard immediately (no multi-second lingering) even when the workspace has no `.git` yet.
- [ ] The new solution is created and activated before the wizard closes (unchanged from current behaviour).
- [ ] Git repository initialization still occurs for the new solution when the workspace has no `.git`: `git init`, `.gitignore` creation, project connect, and an initial commit — now performed by a background job.
- [ ] When the workspace already has a `.git`, the new solution project is still connected to the existing repository (existing branch preserved), now in the background.
- [ ] Git progress appears in the Progress view under "Initializing Git repository"; git errors are still logged via `ServoyLog` (not shown as a blocking dialog).
- [ ] No compilation errors; no new dead code; imports organized and file formatted; no new high-severity SpotBugs findings in the changed code.

## 6. Out of scope
- Changing the create / import-packages / import-solutions / activation runnables or their ordering.
- Changing the MCP `createSolution` git behaviour that SVY-21281 mirrored (unless the optional §3.4 narrowing is applied here, in which case it is limited to this wizard).
- Reworking how git errors are surfaced to the user (keep current log-only behaviour).
- Adding a UI toggle for whether git init runs.

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| Should the optional Approach 2 narrowing (stage only the new solution project instead of `git add .`) be included now, or deferred to keep parity with MCP `createSolution` semantics? | dev/reviewer | open |
| Is it acceptable that a git-init failure now surfaces only in the log after the wizard has closed (no post-close notification)? | product | open |

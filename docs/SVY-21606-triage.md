# Triage Report — SVY-21606

**Verdict:** PROCEED

## Reported problem
Changing something in SPM (the Servoy/Web Package Manager, the Angular "WPM") triggers a
Titanium NG client build. An **already-open, running** Titanium client picks up the
"building" mark (the loading/building overlay), but once the build finishes the running
client is **not refreshed** — it stays stuck on the building state instead of reloading
into the freshly built client. See the attached recording
(`Recording 2026-10-14 151553.mp4`).

The ticket proposes no specific solution; it only describes the symptom ("That doesn't get
refreshed when build is done").

## Root-cause assessment

The "build in progress → loading page → finished client" handoff is driven entirely by the
browser **re-requesting `index.html`**, which only happens for a *fresh* page load. There is
no server→client push that tells an **already-connected** client to reload when the build
finishes. Evidence:

1. **Who serves the building page** —
   `com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/IndexPageFilter.java`
   lines 101–111: on any `/index.html` request for `rfb/angular2` or `/solution/`, **while**
   `WebPackagesListener.isBuildRunning()` is true, the filter returns
   `resources/loadingclient.html` instead of the real client.

2. **How the building page escapes itself** —
   `com.servoy.eclipse.ngclient.ui/resources/loadingclient.html` line 5:
   `<meta http-equiv="refresh" content="3.5">`. The building page re-requests the URL every
   3.5 s; each re-request re-enters `IndexPageFilter`, and once `isBuildRunning()` is false
   and `dist/app/browser/index.html` exists, the real client is served. This mechanism works
   **only because the building page is a plain HTML page that keeps reloading itself**.

3. **Why a running client never recovers** — an already-loaded Titanium client is an Angular
   app on a long-lived websocket; it does **not** periodically GET `index.html`, so it never
   re-enters `IndexPageFilter`. The only code path that makes a *running* client reload is in
   `com.servoy.eclipse.ngclient.ui/node/src/sablo/websocket.service.ts` lines 260–271: the
   `onconnecting` handler calls `window.location.reload()` **only** when the server closes the
   socket with reason `CLIENT-OUT-OF-SYNC`. A package rebuild does not send that reason, so no
   reload is triggered.

4. **The build-done hook does nothing for running clients** —
   `com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/WebPackagesListener.java`:
   the `PackageCheckerJob` done-listener (`checkPackages`, line 2201) and the
   `NodeFolderCreatorJob` done-listener (`createNodeFolderAndCheckPackages`, line 2122) only
   act on **failure** (`handleTitaniumNGClientBuildFailure`). On a **successful** build nothing
   notifies connected clients. So the "building mark" the reporter sees on the running client
   is never cleared by a reload.

Net: the refresh-after-build behaviour was designed around the self-refreshing
`loadingclient.html` page (fresh loads / newly opened clients), and the running-client case
was never wired up. This is a genuine gap in this repository's code, not user misconfiguration
and not third-party behaviour.

**Note on SPM/WPM triggering:** the SPM install path
(`com.servoy.eclipse.designer.wpm/.../InstallWebPackageHandler.importPackage`) wraps the work
in `WebPackagesListener.setIgnore(true)`/`setIgnore(false)`; the `setIgnore(false)` call
re-runs `checkPackages(false)`, which is what schedules the rebuild that flips
`isBuildRunning()` true. That is the trigger the ticket refers to.

## Ticket premise check
The ticket proposes no fix, only the symptom, so there is no premise to overturn. The symptom
is real and reproducible from the code: the running-client reload-on-build-done path simply
does not exist. The fix belongs in this project's Developer/TiNG-build code (notify or reload
connected clients when a successful build completes), not in the solution or a dependency.

## Approaches considered

1. **Push a reload to connected clients when a successful TiNG build finishes.**
   Hook the success branch of the build-done listeners in `WebPackagesListener`
   (`checkPackages` / `createNodeFolderAndCheckPackages`) to notify the running
   Developer/NG clients so they reload (e.g. reuse the existing `CLIENT-OUT-OF-SYNC`
   websocket-reason reload path in `websocket.service.ts`, or add an analogous
   "build-finished, reload" server→client message).
   - Pros: fixes the exact reported gap; reuses the existing client-side reload mechanism;
     scoped to the build lifecycle that already has done-listeners.
   - Cons: needs a reliable way to reach the active clients from the ngclient.ui plugin
     (client handler / endpoint registry); must fire only on a genuine success and only when
     a build was actually in progress, to avoid spurious reloads.

2. **Make the running client poll/observe the build state and reload itself when it clears.**
   Add a lightweight client-side check (or a build-state signal on the existing websocket) so
   the client that is showing the building mark reloads when `isBuildRunning()` goes false.
   - Pros: keeps the decision on the client; no need to enumerate clients server-side.
   - Cons: adds client-side polling or a new state channel; more moving parts than reusing the
     existing reload-on-reason path; the client that is "stuck" may already be in a degraded
     state.

3. **Give `loadingclient.html`-style self-refresh behaviour to the running client's building
   overlay.** If the building mark is rendered in-app, have that overlay trigger a reload once
   the build completes.
   - Pros: localized to the overlay UI.
   - Cons: only helps if/when the overlay is shown; does not cover a running client that was
     mid-session when the build started and is not showing that overlay.

4. **No code change.**
   - Pros: none.
   - Cons: leaves a real, reproducible defect — running clients stay stuck on the building
     state after an SPM-triggered rebuild and must be manually reloaded.

## Recommendation
PROCEED with **Approach 1**: on a **successful** TiNG build completion, notify the running
Developer/NG client(s) to reload, reusing the existing client reload path (the
`CLIENT-OUT-OF-SYNC` → `window.location.reload()` handler in
`com.servoy.eclipse.ngclient.ui/node/src/sablo/websocket.service.ts`, or an equivalent
"build finished" server→client reason). Wire it into the success branch of the build-done
listeners in `WebPackagesListener` (`checkPackages` / `createNodeFolderAndCheckPackages`),
guarded so it fires only when a build was actually running and completed OK.

Approach 2 (client-observed build state) is the reasonable alternative if reaching the active
clients from the ngclient.ui plugin proves awkward. The implementation phase should confirm,
in a running Developer, exactly which component renders the "building mark" on an already-open
client and which client/endpoint registry is reachable from `WebPackagesListener`, so the
reload is pushed to the right clients.

## Git history findings
- `IndexPageFilter.java` lines 101–111 (the `isBuildRunning()` → `loadingclient.html` branch)
  were last touched in `6a95cafbc1` ("Introduce end-of-line normalization", Johan Compagner,
  2026-06-06) — a whitespace/EOL-only change, so blame points at the normalization commit
  rather than the behavioural author; the logic predates it. The subsequent change to the file
  (`117dd4cce0 SVY-21254 update IndexPageFilter to use LoginResult`) did not touch this branch.
- `loadingclient.html`'s self-refresh (`meta refresh 3.5s`) and the overall building-page
  mechanism are long-standing (history shows only EOL normalization and unrelated edits on the
  file). No commit ever added a running-client reload-on-build-done path — consistent with this
  being an original gap rather than a regression.
- No prior `docs/SVY-21606-*.spec.md` or related triage/spec exists in `docs/`.
- No dependency/target-platform version bump is involved; this is not a version regression.

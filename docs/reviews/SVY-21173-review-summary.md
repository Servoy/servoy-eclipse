# SVY-21173 — Peer Review Summary

**Scope reviewed:** servoy-eclipse `lts_2026`, commits `6bf41fe14e`, `c3d2cb399c`, `a98a08c4d4`, `97536e3a89` (code) + `fad920a2fd`, `72b185a151` (docs). Headless Cypress form-test runner reopen fixes.

## Risk verdict

**MODERATE.** The worst surviving items are Medium-severity regressions that pull new static-singleton and off-event-thread shutdown machinery onto a shared interactive-Developer code path (the ngclient `Activator` session factory); all are parameter-gated and bounded, there is no High finding and security relevance is Low — but the blast radius reaches the live Developer process and every fix was verified by hand only, so run the manual checks before relying on it. Merge-forward to `release`/`master` is clean (the two ngclient files and `AbstractWorkspaceExporter` are already byte-identical to `origin/release`).

## Manual test plan

1. **Headless runner:** `run_cypress_form_tests.bat -data <workspace> -s <solution> -as <appserver> -o <out> -timeout 120 -verbose` against a solution with an in-memory DB server and several form specs. Confirm each spec renders *its own* form (not the solution's first form) and the run finishes with `Total: N | Passed: N | Export DONE.`, the correct exit code, and no `ArrayIndexOutOfBoundsException` on shutdown.
2. **Developer UI:** trigger "Run All Cypress Form Tests" on a Cypress-enabled solution; confirm each spec renders its own form and the action completes without hanging.
3. **Developer UI, repeated trigger:** run "Run Cypress Form Test(s)" several times and across different forms to exercise the preview-client reuse/retarget and the off-event-thread teardown; confirm no hang (watch for a websocket request thread stalled on the 10s latch).
4. **Regression — other headless exporters:** run WAR, solution, and mobile exports headlessly; confirm each still shuts down cleanly and reports the same success/failure exit code (the shared `start()` finally now also catches `RuntimeException`).
5. **Regression — normal NG client:** launch a Developer debug client plus the `nodebug` and `svy_developer` variants; confirm each still opens normally (the three existing branches are untouched; the new `formpreview` branch is reached only when the request param is present).

No automated test covers any of the four fixes; verification is manual only.

## Possible improvements / follow-ups

- **No automated regression test** for four distinct bug fixes landing on a maintenance line — a regression would re-surface silently. Consider at least one locking test before relying on this long-term.
- **`retarget()` leaves the static `pendingTargetFormName` stale** (`FormPreviewNGClient`). Harmless today (nothing re-reads it after construction), but a sharp edge if future code reads it as "current target". Worth either updating it in `retarget()` or documenting why not.
- **Two duplicate `CLIENT_ENDPOINT` session-factory registrations** both now carry a `formpreview` branch (the runner's own and the shared ngclient `Activator`) — the exact divergence that produced the "wrong form" defects. Already deferred to a follow-up; worth consolidating into one registration.
- **CI topology:** the loopback unauthenticated `?formpreview=` render (origin check disabled) is fine on dedicated/ephemeral agents but worth confirming for shared multi-tenant build hosts.
- **SVY-21529** (DAL off-event-thread teardown) was fixed under this key via `97536e3a89`; already resolved on this branch.

## Security

Low relevance. Build-time/CI developer tooling plus an in-Developer preview path over local, trusted inputs; no new remotely exploitable surface. Table creation uses `servoy_shared` object APIs, not DDL string concatenation — no injection. The `formpreview` auth-bypass client is pre-existing and parameter-gated, not widened. No static analysis was run this session (code inspection only).

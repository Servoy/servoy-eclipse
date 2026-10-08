# Peer Review Summary — SVY-21312: Developer IDE blocked when Servoy Cloud login fails

**Risk: LOW** — the core fix (distinguishing "cloud down" from "wrong credentials" so the IDE no longer loops/blocks at startup) is sound, covered by targeted regression tests, and leaves the happy path unchanged. The two findings that originally drove a higher rating were both reassessed with the author during review and don't hold up as real risks (see below).

Scope: `servoy-eclipse` @ `release`, commits `3e77f2e269`, `1f2d0b2287` (empty follow-up, same tree as `3e77f2e269` — no additional change), `286239e452` (release-only) plus the independently-landed `7bf95d8902`/`e1153f73a7` reachable from `release`/`master`/`lts_2024`/`lts_2025`.

## Findings reassessed during review (not blocking)

- **Branch divergence (`ServoyLoginDialog.java` differs between `release` and `lts_2024`/`lts_2025`/`master`).** The automated regression pass flagged this as a merge-conflict risk (different HTTP client, a removed sync `doLogin()` overload, a rewritten listener contract). **Author clarification: `lts_2024`/`lts_2025` are old maintenance lines that predate Servoy AI/Pilot entirely** — there's no cross-line merge expected to reconcile this file; the "divergence" is simply that `release` has AI-era code these old lines never had. Not a real risk.
- **Background-retry path not setting `SERVOY_SKILLS_ZIP`.** The regression pass noted that the primary login path sets both `GENAI_API_KEY` and `SERVOY_SKILLS_ZIP` on success, while the 5-minute background-retry path (`scheduleCloudRetry`, around line 581-593) only sets `GENAI_API_KEY`. **Author clarification: `SERVOY_SKILLS_ZIP` is strictly a Servoy Developer concern, set on demand when needed — the retry path isn't expected to carry it.** Not a bug; no fix needed.

## Manual test plan

**Verifying the fix**
1. Point the login endpoint at something returning HTTP 500/503 (`servoy.test.crowd.url` system property or a local stub/proxy — see `getCrowdUrl()`). With no stored credentials, launch Developer and enter any credentials. Expect: IDE starts, a one-time "Servoy Cloud is currently unreachable" dialog appears, no infinite re-prompt loop.
2. Restart Developer. Expect: no login dialog (credentials were saved from step 1), IDE starts straight into degraded mode, status bar shows the warning icon with "Cloud unavailable - logged in as `<user>`" tooltip.
3. Point the endpoint at a connection-refused/unreachable address instead of 5xx. Repeat steps 1-2; same expected behavior.
4. While still in degraded mode, restore the real endpoint and wait up to 5 minutes. Expect: status bar icon flips back to normal "Logged in as `<user>`" without user action.
5. Enter deliberately wrong credentials against a working endpoint (401/403). Expect: dialog re-prompts immediately, no credentials persisted, no "cloud unavailable" messaging — unchanged from before the fix.

**Regression checks**
6. Happy path: real credentials, cloud reachable — confirm `GENAI_API_KEY` gets set and Servoy Pilot picks it up normally.
7. Trigger NG Desktop export and the WAR export pipeline-setup wizard while cloud is down (degraded mode). Expect feature-specific "Servoy Cloud is currently unreachable, X requires a cloud connection" messages, not a generic/blank failure.
8. Open the Start Page / Tutorials view and the valuelist-combobox cloud-login redirect while in degraded mode. These were *not* updated to check `isCloudReachable()` — confirm they at least no-op safely rather than throwing.
9. Use "Logout" on the status bar control while in degraded mode, then log back in with cloud still down and then reachable, to confirm no stale `cloudReachable=false` state survives a manual logout/relogin cycle.

**Automated checks worth running:** `com.servoy.eclipse.ui.tests` → `AllLoginDialogTests` suite (`LoginTokenResponseTest`, `ServoyLoginDialogGetLoginTokenTest`, `ServoyLoginDialogDoLoginBehaviorTest`, `ServoyLoginDialogCloudStateTest`). Spotbugs on `ServoyLoginDialog.java`, `ServoyLoginStatus.java`, `Activator.java` — not run in this review; per AGENTS.md the two highest severities are blocking.

**Surfaces to cover:** `com.servoy.eclipse.ui` (primary), `com.servoy.eclipse.exporter.ngdesktop` and `com.servoy.eclipse.exporter.war` (secondary, cloud-token consumers). Test first-ever-launch (no secure storage node yet) and returning-user (stored credentials) paths separately — different branches, different side effects.

## Possible improvements / follow-ups

- `OpenStartPage`, `TutorialView`, and `ComboboxPropertyAuthenticator` were named in the spec's implementation plan (step 6) as consumers that should check `isCloudReachable()` on a null token, but weren't updated — confirm whether this was intentionally deferred.
- Whether first-login credentials should be held in memory only until server-validated, rather than written to secure storage immediately, remains an open question in the spec (§7) — worth a product decision.

## Scope reviewed

`servoy-eclipse` @ `release`, commits `3e77f2e269` / `1f2d0b2287` (empty) / `286239e452` (release-only), plus `7bf95d8902` / `e1153f73a7` (also on `master`, `lts_2024`, `lts_2025`).

# SVY-21304 — Review Summary

**Risk: ELEVATED** — not because the diff is broken (it's a clean, mechanically contained
version bump across two repos, matching the pattern of every prior Tomcat upgrade here), but
because the target version, Apache Tomcat 11.0.25, is already missing fixes shipped in
11.0.26 (released a month after this commit), several hitting WebSocket code paths Servoy's
NG Client and designer actually use.

## Manual test plan

**Verifying the fix**
1. Build the Developer IDE product against the updated target (both repos checked out at
   these commits) and confirm the build resolves the exact `11.0.25` version with no
   Tycho/p2 "missing exact version" error.
2. Launch the Developer IDE, open an NG Client-backed solution, and confirm the embedded
   Tomcat-powered web server starts and serves the client normally.
3. Run a WAR export of a sample solution and confirm the exported WAR's `lib/` directory
   contains the 11.0.25 jars, and that `WarExporter`'s filename-based jar deletion
   (`servlet-api.jar`, `jsp-api.jar`, `server-bootstrap.jar`, `tomcat-juli.jar`) still removes
   exactly those four and nothing else.

**Regression checks**
1. Exercise the Developer designer's websocket-based editors (form designer drag/drop,
   property inspector) to confirm no behavior change in the embedded Tomcat's WebSocket
   handling.
2. Run a headless Cypress test pass (if available) since the embedded Tomcat also backs
   headless test running — confirm no regression from the jar swap.
3. If any deployment in scope uses an AJP connector, specifically test it — `CVE-2026-78383`
   (AJP DoS) is still open in 11.0.25.

## Possible improvements / follow-ups

- A follow-up bump to Apache Tomcat 11.0.26+ is worth tracking: it fixes 10 CVEs that are
  still open in 11.0.25, including WebSocket security-constraint bypass and DoS issues on
  code paths this product uses. The user is filing a follow-up issue for this.
- Confirm whether SVY-21304 was raised in response to a specific CVE against 11.0.22, or is
  routine version-currency maintenance — relevant context for anyone chasing a specific CVE
  fix later.
- Confirm whether any deployment in scope has an AJP connector enabled (relevant to
  `CVE-2026-78383`'s real-world impact — not visible from this diff).
- Confirm the Tomcat bump has also been propagated to `master`, per the ticket's stated plan
  (`lts_2026` → `release` → `master`); this review only verified `release`/`lts_2026` sync.

## Scope reviewed

- `servoy-eclipse` (branch `release`, in sync with `lts_2026`) — commit `96888166e4`,
  `com.servoy.eclipse.feature/feature.xml` (+1/-1).
- `servoy-eclipse-tomcat` (branch `release`, in sync with `lts_2026`) — commit `a2730e4`,
  35 files (binary jar swap plus manifest/classpath/pom version metadata).

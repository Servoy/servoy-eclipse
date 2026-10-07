# Triage Report — SVY-21510

**Verdict:** PROCEED

## Reported problem

When an MCP tool (Cypress form/E2E test run, `showFormInBrowser`, `screenshotForm`, `testE2E`, etc.)
runs, it flips the server-wide Servoy property `servoy.ngclient.testingMode` to `true`. The ticket
states this itself "should not really be an issue" — the actual complaint is a side effect: once
that property is `true`, the Developer's own "open form in developer view" shortcut stops working.

Per the comments:
- Johan Compagner: confused why the shortcut is related at all.
- Rene van Veen: "when testing mode is enabled then it doesn't work because it goes in production
  build" — i.e. flipping the global flag changes which build artifact gets served, and that breaks
  the shortcut.

The ticket's proposed solution: "All the MCP calls should just use the query parameter in the URL
instead of toggling the Servoy property."

## Root-cause assessment

`servoy.ngclient.testingMode` is read in exactly one way at the lowest level:

```java
// servoy_shared: com.servoy.j2db.util.Utils.isInTestingMode(INGClientApplication client)
public static boolean isInTestingMode(INGClientApplication client)
{
    if (client != null)
    {
        Object clientIsInTesting = client.getClientProperty(Settings.TESTING_MODE);
        if (clientIsInTesting != null)
        {
            return Utils.getAsBoolean(clientIsInTesting);
        }
    }
    return Utils.getAsBoolean(Settings.getInstance().getProperty(Settings.TESTING_MODE, "false"));
}
```

So the mechanism to make this **per-client** (and therefore per-request/URL) already exists:
`client.getClientProperty(...)`/`putClientProperty(...)` is checked first and overrides the
global `Settings` property. All of the current MCP-side callers, however, bypass that entirely and
set the **global static** property directly:

- `com.servoy.eclipse.cypress.headless.CypressFormTestRunner.enableTestingMode()` (line 427)
- `com.servoy.eclipse.cypress.actions.RunCypressFormTestHandler.enableTestingMode()` (line 108)
- `com.servoy.eclipse.cypress.actions.RunSingleTestHandler` (line 24)
- `com.servoy.eclipse.cypress.actions.RunAllE2ETestsHandler` (line 56)
- `com.servoy.eclipse.cypress.actions.RunAllCypressFormTestsHandler` (lines 78, 141)
- `com.servoy.eclipse.developer.mcp.servers.ServoyTestingServer.ensureTestingMode()`
  (line 60) — called from `showFormInBrowser`, `testForm`, `showAndTest`, `testE2E`, `showAndTestE2E`.
  **Correction (found during manual testing, 2026-10-05): this class lives in THIS repo**
  (`com.servoy.eclipse.developer.mcp`), not in a separate Servoy-Copilot repo as originally assumed
  below and in the resulting spec. It is the actual AI/MCP tool surface behind prompts like
  "create a screenshot for form X" or "test form X" — i.e. the primary path a user exercises,
  more so than the Eclipse-menu-driven handlers above. The original "out of scope / separate repo"
  reasoning in this report's Recommendation section is **wrong** and is superseded by
  `docs/SVY-21510-testing-mode-url-param.spec.md`'s updated scope, which now includes this class.

All of these call `Settings.getInstance().setProperty("servoy.ngclient.testingMode", "true")`,
which is process-wide and persists for the remainder of the Developer session (nothing ever resets
it back to `false`).

**Why the shortcut breaks — this is the part the ticket under-describes, found by tracing every
reader of the flag, not just the obvious NG-client ones:**

`Utils.isInTestingMode(null)` (client is `null`) is also read in
`com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob` to pick which **Angular build
command** compiles the solution's TiNG bundle:

```java
// WebPackagesListener.java:210-216 (PackageCheckerJob constructor)
else
{
    if (Utils.isInTestingMode(null))
    {
        toRun = "build_sourcemap";       // ng build ... --configuration production --source-map
    }
    // else stays "build_debug_nowatch"  // ng build ngclient2 --watch=false (dev build)
}
```

(`git blame`: this branch was introduced in commit `3f16a6ba0`, "SVY-20573 Instead of test server
per server start it for a client only", 2025-10-10.)

This is a **build-time** decision, made once, with no `INGClientApplication` in scope (`client` is
passed as `null`) — there is no request, no URL, no per-session context here at all. It determines
which npm/ng script compiles the solution's `dist/app/browser` bundle that `IndexPageFilter` later
serves to **every** browser tab for that solution, including the developer's own manually-opened
"open in developer view" tab. `build_sourcemap` still passes `--configuration production`, so this
is not literally "goes to a production build" as Rene phrased it, but it is a *different* build
configuration/invocation (`build_sourcemap` vs. the normal dev `build_debug_nowatch`) than what the
Developer shortcut normally triggers and expects — explaining the "shortcut doesn't work anymore"
symptom reported in the ticket.

Because this flag is a static, un-scoped boolean read at Angular-build time, no amount of switching
the *client-rendering* call sites (`FormLayoutGenerator`, `ChildrenJSONGenerator`,
`ServoyAttributesPropertyType`, `NGClientWebsocketSession.sendUIProperties`) to a per-request/URL
mechanism touches this path. The `WebPackagesListener` build-mode read is irreducibly global by
construction — it fires in a background `Job` the moment package/dependency changes are detected,
independent of any individual browser connection.

## Ticket premise check

The ticket's premise is **half right**: the specific symptom (shortcut stops working) is a real,
confirmed regression-causing side effect of the current implementation, and the general direction
("don't mutate shared Developer state from an MCP tool call") is sound engineering practice for an
AI-driven testing feature that runs concurrently with a human using the same Developer instance.

But "all MCP calls should just use the query parameter instead" is not fully achievable as stated:

- **Client-scoped testing-mode reads** (`FormLayoutGenerator`, `ChildrenJSONGenerator`,
  `ServoyAttributesPropertyType`, `NGClientWebsocketSession.sendUIProperties`,
  `servoy_public_impl.service.ts#isInTestingMode`) *can* become purely per-request: each one already
  receives an `INGClientApplication`/client instance, and `Utils.isInTestingMode` already prefers
  `client.getClientProperty(...)` over the global. A `?testingMode=true` URL/`formpreview` request
  parameter read in `NGClientWebsocketSession.init`/`onOpen` (mirroring how `formpreview` itself is
  already read there) and stored via `client.putClientProperty(Settings.TESTING_MODE, true)` would
  cover 100% of these call sites with zero impact on any other open session/tab, including the
  Developer's own shortcut-opened one.

- **The `WebPackagesListener` Angular build-mode decision cannot** take a URL parameter — it runs
  before any HTTP request exists, in a background compile job. If "testing mode" is meant to also
  control which build variant gets compiled (sourcemaps for easier Cypress/MCP debugging), that
  needs its own trigger (e.g. an explicit argument passed into the headless/MCP build-invocation
  path, or scoped to the `CypressFormTestRunner`'s own headless process rather than the shared
  Developer instance), not a reinterpretation of the global flag.

So the ticket's title/summary ("MCP calls enabled testing mode on the admin page") is a reasonable
restatement of the symptom, but the one-line fix it proposes needs to be split: convert the
client/request-scoped call sites to a per-request mechanism (straightforward, low risk, fixes the
reported shortcut regression), and treat the build-mode selection in `WebPackagesListener` as a
separate concern that is out of scope for "use a query parameter" and should keep working off some
explicit, non-shared signal instead of the global flag — or simply stop keying the Angular build
mode off `isInTestingMode` at all once the Cypress/MCP flows no longer need it to force sourcemaps.

## Approaches considered

1. **Per-client/request testing-mode flag for all client-rendering MCP flows, with a dedicated
   headless-only path for the build-mode selection** — PROCEED recommendation.
   - Pros: fixes the reported regression at its root (Developer's own session is never mutated by
     an MCP call); matches the existing `client.getClientProperty` override mechanism that
     `Utils.isInTestingMode` was already designed to support; `formpreview` already demonstrates the
     exact pattern (`requestParams.containsKey("formpreview")` in
     `NGClientWebsocketSession`/various `IWebsocketSessionFactory` overrides) to extend.
   - Cons: touches several call sites across `servoy_ngclient`, `com.servoy.eclipse.cypress`, and
     Servoy-Copilot's `com.servoy.eclipse.developer.mcp`; the `CypressFormTestRunner` headless
     process has no "other session" to protect (it *is* the whole process) so for it, keeping (or
     simplifying) the global `Settings` toggle is harmless and arguably simpler — only the
     Developer-embedded MCP tools (`ServoyTestingServer`, `RunCypressFormTestHandler`,
     `RunSingleTestHandler`, `RunAllE2ETestsHandler`, `RunAllCypressFormTestsHandler`) that run
     *inside the shared Developer instance* actually need the fix.

2. **Just switch the global `Settings.setProperty` calls to also pass a `formpreview`-style URL
   query parameter, without touching `WebPackagesListener`** — literal reading of the ticket.
   - Pros: smallest diff; matches the ticket text exactly.
   - Cons: does not fix the reported regression. The build-mode decision in `WebPackagesListener`
     is what actually breaks the shortcut, and it reads the global flag with `client == null` —
     a URL parameter cannot reach it. This approach would plausibly leave the bug reporter's actual
     complaint (shortcut broken) unresolved while still "doing something" with query params for the
     parts that didn't need it as urgently.

3. **Stop keying the Angular build mode off `isInTestingMode` entirely; always build `build_debug_nowatch`
   in Developer and let Cypress/MCP tools request sourcemaps via an explicit build argument when they
   need them (e.g. only in `CypressFormTestRunner`'s headless invocation).**
   - Pros: removes the only truly un-fixable (non-request-scoped) consumer of the global flag,
     simplifying the eventual migration to per-request state everywhere else; sourcemap-on-demand
     for debugging a failing Cypress run can be requested explicitly instead of being an accidental
     side effect of "someone opened a form in a browser 10 minutes ago via MCP".
   - Cons: changes behavior for a case the ticket doesn't mention (nobody is complaining about
     missing sourcemaps); needs its own regression check against whatever workflow added SVY-20573.

4. **No code change** — leave the global toggle as is, document that MCP tool use will temporarily
   affect the Angular build mode for the solution and that a Developer restart (or manually clearing
   the Settings property) resets it.
   - Pros: zero engineering risk.
   - Cons: does not address a Critical-priority, reproducible regression that actively blocks a
     developer from using Developer's own "open in developer view" shortcut after any MCP tool call
     touches a form — unacceptable given the ticket's priority and the fact AI-assisted/MCP flows
     are meant to run alongside normal human development, not block it.

## Recommendation

**PROCEED** with approach 1 as the primary fix, scoped narrowly:

- In `servoy_ngclient` (`NGClientWebsocketSession.onOpen`/`init`, mirroring the existing
  `formpreview` request-param handling), read an explicit request parameter (e.g. `testingMode=true`)
  when present and call `client.putClientProperty(Settings.TESTING_MODE, Boolean.TRUE)` on that
  client only. This requires no change to `Utils.isInTestingMode` — it already prefers the
  per-client property.
- Update the Developer-embedded MCP tool call sites that currently call
  `Settings.getInstance().setProperty("servoy.ngclient.testingMode", "true")` while the shared
  Developer instance is running — `ServoyTestingServer.ensureTestingMode()` (Servoy-Copilot),
  `RunCypressFormTestHandler`, `RunSingleTestHandler`, `RunAllE2ETestsHandler`,
  `RunAllCypressFormTestsHandler` — to instead append the new URL parameter to whatever URL they
  construct/navigate to (`FormPreviewService.showFormInBrowser`/`screenshotForm` already build a
  `?formpreview=<form>` URL and are the natural place to append `&testingMode=true`), and stop
  calling the global `Settings.setProperty` for those flows.
- Leave `CypressFormTestRunner.enableTestingMode()` (the standalone headless CI runner, a separate
  OS process with no shared Developer instance to protect) using the global `Settings` property —
  it is harmless there and rewriting it adds risk for no benefit.
- Treat `WebPackagesListener`'s build-mode selection as a follow-up/out-of-scope decision (approach 3)
  and flag it explicitly to Johan/Rene rather than silently leaving it reading the (now rarely-set)
  global flag — recommend confirming with them whether sourcemap-on-MCP-call is even still desired
  before changing it, since it predates this ticket (SVY-20573) and may be relied on by the Cypress
  CI flow.

Alternative considered and rejected for now: approach 2 (literal ticket text, URL param only) would
ship something but not fix the actual reported symptom, since the regression's root cause is the
unscoped build-mode read, not the per-render `data-cy`/testing-mode attributes.

## Git history findings

- `com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/WebPackagesListener.java`,
  commit `3f16a6ba0` ("SVY-20573 Instead of test server per server start it for a client only",
  lvostinar, 2025-10-10): introduced the `Utils.isInTestingMode(null)` → `build_sourcemap` branch in
  `PackageCheckerJob`'s constructor. This is the mechanism that ties the global testing-mode flag to
  the Angular build configuration and is the direct cause of "shortcut to open in developer view
  isn't working anymore" once an MCP call flips the flag.
- `Utils.isInTestingMode(INGClientApplication)` (`servoy_shared`) already supports a per-client
  override via `getClientProperty`/`putClientProperty` — this predates SVY-21510 and is the existing
  extension point the fix should use; no new mechanism needs to be invented.
- The `formpreview` request-parameter pattern (`NGClientWebsocketSession`/
  `IWebsocketSessionFactory` overrides in `CypressFormTestRunner.activateNgClientBundle` and the
  Developer `Activator`) is the precedent to follow for adding a `testingMode` request parameter —
  introduced in commit `67871456a` ("SVY-21025 ... show form in browser ... bypass authentication").
- Related MCP work in the separate `Servoy-Copilot` repo (`ServoyTestingServer.ensureTestingMode()`,
  commits under SVY-21025/SVY-21136/SVY-21195/SVY-21296/SVY-21369/SVY-21514) built up the current
  set of call sites that all share the same global-toggle pattern; none of them scope the flag to a
  single client/session.

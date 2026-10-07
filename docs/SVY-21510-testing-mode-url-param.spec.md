# Spec: SVY-21510 — MCP calls (Cypress / screenshot) enabled testing mode on the admin page

## 1. Goal

Stop MCP-driven Cypress/form-preview test runs from mutating the shared Developer
instance's global `servoy.ngclient.testingMode` property. Instead, testing mode must be
switched on per-request, via a `testingMode=true` URL parameter read by the websocket
session that serves that specific client. This fixes the reported regression: after any
MCP tool call touched a form, the Developer's own "open form in developer view" shortcut
stopped working, because the previous implementation flipped a server-wide static flag
that outlived the single test request and affected every other browser tab/session,
including the Developer's own.

## 2. Background

### 2.1 How `servoy.ngclient.testingMode` is read today

`Utils.isInTestingMode(INGClientApplication client)` (`servoy_shared`,
`com.servoy.j2db.util.Utils`) is the single read path for this flag:

```java
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

It already prefers a **per-client** override (`client.getClientProperty(...)`) over the
global `Settings` property. This method requires **no change** — the per-client override
mechanism it already supports is exactly what this spec uses.

Call sites that key client-rendering behaviour (data-cy attributes, etc.) off this flag,
all passing a real client/application instance:
- `ChildrenJSONGenerator.java:459`
- `FormElement.java:466`
- `NGClientWebsocketSession.java:408` (`sendUIProperties`, sends `TESTING_MODE` to the
  browser's client-side UI properties)
- `ServoyAttributesPropertyType.java:68`
- `FormLayoutGenerator.java:296,530`

None of these need to change — they already resolve per-client once the client property
is set correctly on that specific client.

### 2.2 Today's global-mutation call sites (the problem)

Six call sites unconditionally flip the **global, process-wide** `Settings` property
whenever an MCP/Cypress test flow starts, and nothing ever resets it back to `false`:

| Call site | File | Context |
|---|---|---|
| `ServoyTestingServer.ensureTestingMode()` | Servoy-Copilot repo (`com.servoy.eclipse.developer.mcp`) | **Out of scope** — separate repo, see §6 |
| `RunCypressFormTestHandler.enableTestingMode()` | `com.servoy.eclipse.cypress/src/.../actions/RunCypressFormTestHandler.java:108` | Developer handler, runs inside shared Developer instance |
| `RunSingleTestHandler` (inline, inside the `Job.run()`) | `.../actions/RunSingleTestHandler.java:24` | Developer handler |
| `RunAllE2ETestsHandler` (inline, inside the `Job.run()`) | `.../actions/RunAllE2ETestsHandler.java:56` | Developer handler |
| `RunAllCypressFormTestsHandler` (inline, twice: `execute()` and `runFormTests()`) | `.../actions/RunAllCypressFormTestsHandler.java:78,141` | Developer handler |
| `CypressFormTestRunner.enableTestingMode()` | `com.servoy.eclipse.cypress/src/.../headless/CypressFormTestRunner.java:427` | **Confirmed standalone headless process** — see §2.4, left unchanged |

### 2.3 Why the Developer shortcut breaks (confirmed root cause, from triage)

`Utils.isInTestingMode(null)` (client is `null`, so the global `Settings` fallback is
read) is also consulted in `WebPackagesListener.PackageCheckerJob`'s constructor
(`com.servoy.eclipse.ngclient.ui/src/.../WebPackagesListener.java:210-216`, introduced in
commit `3f16a6ba0`, SVY-20573) to pick which Angular build command compiles the
solution's TiNG bundle:

```java
else
{
	if (Utils.isInTestingMode(null))
	{
		toRun = "build_sourcemap";
	}
	// else stays "build_debug_nowatch"
}
```

This is a one-time, request-independent decision made in a background compile `Job`.
Once any of the handlers in §2.2 flips the global flag to `true`, the next package
rebuild compiles with `build_sourcemap` instead of the normal dev `build_debug_nowatch`
— a different build invocation than what "open in developer view" expects — which is the
confirmed cause of the "shortcut doesn't work anymore" symptom. **This spec intentionally
does not touch `WebPackagesListener`** (see §6, §7) — eliminating the Developer-embedded
handlers' global-flag writes removes the trigger for most everyday MCP-driven flows, but
the mechanism itself is called out as a separate follow-up decision.

### 2.4 Why `CypressFormTestRunner` is left alone

`CypressFormTestRunner` (`com.servoy.eclipse.cypress/src/.../headless/CypressFormTestRunner.java`)
is confirmed, from reading its class Javadoc and `exportActiveSolution()`/
`installDriverAwareServiceClassLoader()`/`startWebServer()` flow, to be a **standalone
headless Eclipse application**: it extends `AbstractWorkspaceExporter`, is registered as
an `org.eclipse.core.runtime.applications` extension, and is launched as its own OS
process (`./servoy_developer -application com.servoy.eclipse.cypress.cypressFormTestRunner
-data /workspace ...`) for CI. It is never co-resident with an interactive Developer
session — there is no "other session" for it to protect by scoping testing mode
per-request. Its `enableTestingMode()` call stays as-is.

### 2.5 The `formpreview` precedent to follow

Several places in this codebase already extend request parameters the same way this
spec needs to for `testingMode`:

- `NGClientWebsocketSession.onOpen(Map<String, List<String>> requestParams)` already
  reads request params directly (e.g. `requestParams.containsKey("clienttype")` at
  `NGClientWebsocketSession.java:220`) and stores results on fields/the client.
- `com.servoy.eclipse.ngclient.startup.Activator.start()` registers an anonymous
  `IWebsocketSessionFactory` whose session subclasses `NGClientWebsocketSession` and
  overrides `init(Map<String, List<String>> requestParams)` to branch on
  `requestParams.containsKey("formpreview")`, `"nodebug"`, `"svy_developer"` — creating a
  different kind of client (`FormPreviewNGClient`, `NGClient`, the shared
  `developerNGClient`) per branch (commit `67871456a`, SVY-21025/SVY-21509).
- `CypressFormTestRunner.activateNgClientBundle()` installs an equivalent anonymous
  `IWebsocketSessionFactory` for the headless process, with its own `formpreview` branch.
- `NGClient.handleArguments(String[] args, StartupArguments argumentsScope)` already has
  a `svy_testmode` **startup argument** path (not an HTTP request parameter, but the same
  idea) that calls `putClientProperty(Settings.TESTING_MODE, ...)`:

  ```java
  if (argumentsScope != null && argumentsScope.containsKey("svy_testmode"))
  {
  	putClientProperty(Settings.TESTING_MODE, Boolean.valueOf(Utils.getAsBoolean(argumentsScope.get("svy_testmode"))));
  }
  ```

  This is a different code path (deeplink/startup-argument handling via
  `StartupArguments`, used when `?svy_testmode=true` reaches `handleArguments` through
  the args/argument-scope flow) and already demonstrates `putClientProperty` is the
  correct, existing extension point. This spec adds an equivalent, more direct read in
  `onOpen`/`init` so testing mode is set as early as session creation, independent of
  whether `handleArguments` happens to run.

- `FormSpecGenerator.getFormUrl()` and `FormPreviewService.showFormInBrowser/screenshotForm`
  already build `?formpreview=<formName>&svy_testmode=true` / `?formpreview=<formName>`
  URLs — the `&svy_testmode=true` suffix is already present in the Cypress-form-spec URL
  built by `FormSpecGenerator` (`.../services/FormSpecGenerator.java:287`), but note this
  is consumed by the **startup-argument** path (§2.5 above via `NGClient.handleArguments`),
  not a request-parameter read in `onOpen`/`init`. This existing `svy_testmode` request
  parameter is reused as-is in the new `onOpen`/`init` code added by this spec (see §3.1),
  rather than inventing a second, differently-named parameter — keeping one contract for
  "this request wants testing mode" across both the startup-argument and the
  session-creation code paths.

## 3. Design

### 3.1 Per-client testing-mode flag read in `NGClientWebsocketSession`

Add a check, mirroring the existing `requestParams.containsKey("clienttype")` handling in
`onOpen`, that reads the `svy_testmode` request parameter (the same name already produced
by `FormSpecGenerator.getFormUrl()`, see §2.5) and sets it directly as a per-client
property — independent of whether `handleArguments`/`StartupArguments` processing runs
for this request:

```java
// in NGClientWebsocketSession.onOpen(Map<String, List<String>> requestParams), near the
// existing "clienttype" handling, before the client is used further:
if (requestParams.containsKey("svy_testmode"))
{
	client.putClientProperty(Settings.TESTING_MODE,
		Boolean.valueOf(Utils.getAsBoolean(requestParams.get("svy_testmode").get(0))));
}
```

Placement: `onOpen` already guards `client != null` is implied (it is created in `init`,
which always runs before `onOpen` per the `BaseWebsocketSession` lifecycle — confirmed by
reading `NGClientWebsocketSession.init()`, which lazily creates the client, and `onOpen()`,
which already calls `client.getRuntimeProperties()...` unconditionally). Add the new check
right after the existing `clienttype` block, before `lastSentStyleSheets = null;`.

This makes the flag effective for `sendUIProperties()` (`NGClientWebsocketSession.java:408`,
sends `Settings.TESTING_MODE` into the browser's client-side UI properties for this
client only) and for every `Utils.isInTestingMode(client)` call that passes a real client
(§2.1 table) — all without touching `Utils.isInTestingMode` itself, and without affecting
any other open session/tab.

No change is needed to the `formpreview`-branching `Activator`/`CypressFormTestRunner`
session factories: they subclass `NGClientWebsocketSession` and call
`super.init(requestParams)` / inherit `onOpen`, so the new check in the base class applies
to form-preview sessions too.

### 3.2 Developer-embedded MCP handlers: stop touching the global `Settings` property

For each handler confirmed to run **inside the shared, interactively-used Developer
instance** (not a separate OS process), replace the global `Settings.setProperty(...)`
call with appending `&testingMode=true` — wait, this repo's existing convention (§2.5)
uses the parameter name `svy_testmode`, not `testingMode`. To stay consistent with the
URL contract `FormSpecGenerator` already emits and that `NGClient.handleArguments`
already consumes, the appended parameter is **`svy_testmode=true`**, not a new
`testingMode` name. (The triage/approved-approach text used `testingMode` as a
placeholder name; this spec aligns on the parameter name already live in this repo,
`svy_testmode`, per §2.5. See open question in §7 about Servoy-Copilot's naming.)

Handlers to change (all four run inside the shared Developer process — confirmed by
their package, `org.eclipse.core.runtime.jobs.Job` usage inside the running IDE, and
`CypressConsoleUtil`/`CypressTestResultsView` UI integration, as opposed to
`CypressFormTestRunner`'s standalone headless application class):

1. **`RunCypressFormTestHandler`** (`com.servoy.eclipse.cypress/src/.../actions/RunCypressFormTestHandler.java`)
   - `enableTestingMode()` currently does `Settings.getInstance().setProperty("servoy.ngclient.testingMode", "true")`
     and is called from the `Job.run()` body before `runFormTestCore(...)`.
   - `runFormTestCore`/`FormSpecRunner.runFormCypressTests` do not themselves build the
     target URL — the URL is embedded in the pre-generated `.spec.cy.js` file
     (`FormSpecGenerator.getFormUrl()`, already `?formpreview=<form>&svy_testmode=true`,
     §2.5). Because the spec file **already contains** `svy_testmode=true`, the fix for
     this handler is simply to **delete the `enableTestingMode()` call and the global
     `Settings.setProperty`** — the per-request flag arrives via the URL the spec visits,
     which the new `onOpen` check (§3.1) now honours. No URL construction changes needed
     here.
   - Remove the now-unused `enableTestingMode()` method (or keep it a no-op only if a
     test directly asserts its presence — confirm via `grep` for test references before
     deleting; see implementation step 4).

2. **`RunSingleTestHandler`** (`.../actions/RunSingleTestHandler.java:24`)
   - Same situation: the inline `Settings.getInstance().setProperty(...)` call precedes
     `runner.runFormCypressTests(testName, true)` / `runner.runE2ECypressTests(testName, true)`.
   - Form-test case: the spec file visited already carries `svy_testmode=true` (same as
     above) — delete the `Settings.setProperty` line.
   - E2E-test case (`TestType.E2E`): E2E specs are hand-authored/generated by
     `generateCypressE2ETest` and may call `cy.visit(...)` with arbitrary relative URLs
     that do **not** already carry `svy_testmode=true` (unlike the form-spec generator).
     Confirm at implementation time (`grep` the E2E spec template in
     `ServoyTestingServer.generateCypressE2ETest`, `.../servers/ServoyTestingServer.java:1079-1085`)
     — if the generated `cy.visit('...')` URL lacks `svy_testmode=true`, this spec's fix
     for the E2E path is to simply remove the global `Settings.setProperty` call without
     adding a replacement query param (accepting that E2E specs render without forced
     `data-cy` testing-mode attributes unless the spec author added `svy_testmode=true`
     themselves) — because rewriting the E2E spec generator's URL contract is a larger
     change than this ticket's scope. Flag as noted in §7.

3. **`RunAllE2ETestsHandler`** (`.../actions/RunAllE2ETestsHandler.java:56`)
   - Same E2E situation as above — delete the global `Settings.setProperty` call inside
     the `Job.run()` body; no safe per-request substitute without changing the E2E spec
     URL contract (out of scope, see §7).

4. **`RunAllCypressFormTestsHandler`** (`.../actions/RunAllCypressFormTestsHandler.java:78,141`)
   - Both occurrences (`execute()`'s `Job.run()` and `runFormTests()`'s `Job.run()`) run
     only **form** tests (`TestType.FORM`), whose specs are generated by
     `FormSpecGenerator` and already carry `svy_testmode=true` in the visited URL (§2.5).
   - Delete both `Settings.getInstance().setProperty("servoy.ngclient.testingMode", "true")`
     lines; no replacement needed.

Net effect: once `onOpen`/`init` honour `svy_testmode` per-client (§3.1), none of the
four Developer-embedded handlers above need to touch any global flag — the already-
generated spec URLs carry the parameter that makes testing mode effective for exactly the
NG client instance Cypress drives, and the Developer's own forms/tabs are never affected.

### 3.3 `WebPackagesListener` build-mode selection — explicitly out of scope

`PackageCheckerJob`'s `Utils.isInTestingMode(null)` → `build_sourcemap` branch
(`WebPackagesListener.java:210-216`) is **not modified** by this spec. It is irreducibly
tied to the global `Settings` flag (no request/client exists at Angular-build time), and
removing the Developer-embedded handlers' global-flag writes (§3.2) means this branch
will in practice almost never see `true` any more for everyday MCP-driven form/Cypress
runs — which already addresses the reported shortcut regression without changing this
code. See §6 and §7 for the explicit follow-up/confirmation needed before any further
change here.

## 4. Implementation plan

1. **`servoy_ngclient` — `NGClientWebsocketSession.java`**
   - In `onOpen(Map<String, List<String>> requestParams)`, add the `svy_testmode`
     request-parameter check described in §3.1, placed after the existing `clienttype`
     block and before `lastSentStyleSheets = null;`. Use `client.putClientProperty(Settings.TESTING_MODE, ...)`
     exactly as `NGClient.handleArguments` already does for the startup-argument path (so
     the two code paths agree on using the per-client property, not a field).
   - No change needed to `Utils.isInTestingMode` (`servoy_shared`) — it already prefers
     the per-client property.

2. **`com.servoy.eclipse.cypress` — four Developer-embedded handlers**
   - `RunCypressFormTestHandler.java`: delete the `Settings.getInstance().setProperty("servoy.ngclient.testingMode", "true")`
     call inside `enableTestingMode()`'s caller (the `Job.run()` body) and remove the
     `enableTestingMode()` method itself if nothing else references it (check via
     `grep`/`eclipse-ide_findReferences` first — the public `enableTestingMode()` is
     directly unit-tested per the Testing section catalogue conventions in this repo, so
     if a test asserts its behavior, update that test instead of silently deleting the
     method; see step 4).
   - `RunSingleTestHandler.java`: delete the inline `Settings.getInstance().setProperty(...)`
     line in `execute(...)`'s `Job.run()` body.
   - `RunAllE2ETestsHandler.java`: delete the inline `Settings.getInstance().setProperty(...)`
     line in `execute()`'s `Job.run()` body.
   - `RunAllCypressFormTestsHandler.java`: delete both inline `Settings.getInstance().setProperty(...)`
     lines (`execute()` and `runFormTests()`).
   - Leave `CypressFormTestRunner.java`'s `enableTestingMode()` (headless, separate
     process, §2.4) unchanged.

3. **No change to `FormSpecGenerator.java`** — its generated URL already carries
   `svy_testmode=true` (§2.5); this spec relies on that existing contract rather than
   duplicating it with a second parameter name.

4. **Tests to update/add**
   - `com.servoy.eclipse.cypress.tests` — add/adjust unit tests for
     `RunCypressFormTestHandler`, `RunSingleTestHandler`, `RunAllE2ETestsHandler`,
     `RunAllCypressFormTestsHandler` asserting the global `Settings.TESTING_MODE`
     property is **not** mutated by these handlers any more (reset the property before
     each test and assert it is unchanged after calling the handler's test-running path).
     Check existing tests under `com.servoy.eclipse.cypress.tests` for any assertion that
     currently expects `Settings.getInstance().getProperty("servoy.ngclient.testingMode")`
     to become `"true"` after calling these handlers, and update/remove those assertions.
   - `servoy_ngclient.tests` (or wherever `NGClientWebsocketSession` unit/plugin tests
     live — confirm location via `eclipse-ide_findTestClasses` at implementation time) —
     add a test that constructs an `onOpen` call with `requestParams` containing
     `svy_testmode=["true"]` and asserts `client.getClientProperty(Settings.TESTING_MODE)`
     is `Boolean.TRUE` afterwards, and that the global `Settings` property is untouched.
   - Confirm no existing integration test in `com.servoy.eclipse.developer.mcp.tests`
     (`CypressFormTestingIntegrationTest`, `ShowFormInBrowserIntegrationTest`) depends on
     the global flag being set as a side effect of these Developer-embedded handlers —
     those tests already assert on `formpreview=`/`svy_testmode=true` being present in
     generated URLs/specs, which is unaffected by this change.

5. **Compilation/import check:** `NGClientWebsocketSession.java` already imports
   `com.servoy.j2db.util.Settings` and `com.servoy.j2db.util.Utils` — no new imports
   needed for step 1. The four cypress-handler files already import
   `com.servoy.j2db.util.Settings` fully-qualified inline — simply remove the now-unused
   call; check whether `com.servoy.j2db.util.Settings` becomes a fully unused qualified
   reference (no `import` statement to clean up, since they use the fully-qualified form
   `com.servoy.j2db.util.Settings.getInstance()` inline).

## 5. Acceptance criteria

- [ ] Opening a Cypress-generated form-test spec URL (`?formpreview=<form>&svy_testmode=true`)
      sets `Settings.TESTING_MODE` as a **per-client** property on that NG client only
      (verified via `client.getClientProperty(Settings.TESTING_MODE)` after `onOpen`),
      and the global `Settings.getInstance().getProperty("servoy.ngclient.testingMode")`
      remains unchanged by that request.
- [ ] Running a Cypress form test via `RunCypressFormTestHandler`, `RunSingleTestHandler`
      (form type), or `RunAllCypressFormTestsHandler` no longer calls
      `Settings.getInstance().setProperty("servoy.ngclient.testingMode", "true")` — the
      global property is untouched before/after the run.
- [ ] After running any of the above Developer-embedded Cypress form tests, the
      Developer's "open form in developer view" shortcut (any form, same Developer
      session) continues to compile/open with the normal dev build
      (`build_debug_nowatch`), not `build_sourcemap` — i.e. the regression described in
      the ticket (confirmed by Rene van Veen's comment) no longer reproduces for the
      Developer-embedded Cypress form-test flows.
- [ ] `CypressFormTestRunner` (headless CI runner) is unchanged and continues to pass
      its existing tests — it still uses the global `Settings` property, which is
      harmless in its own dedicated process.
- [ ] `Utils.isInTestingMode(INGClientApplication)` is unchanged.
- [ ] All existing Cypress/MCP-related unit and integration tests
      (`com.servoy.eclipse.cypress.tests`, `com.servoy.eclipse.developer.mcp.tests`)
      pass, with any assertions that depended on the old global-flag side effect updated
      per §4 step 4.

## 6. Out of scope

- **`com.servoy.eclipse.developer.mcp.servers.ServoyTestingServer.ensureTestingMode()`**
  lives in the separate **Servoy-Copilot** repository, not this repo. It is called from
  `showFormInBrowser`, `screenshotForm`, `testForm`, `testE2E`, `showAndTest` and has the
  exact same global-`Settings` pattern as the handlers fixed here. **This spec does not
  change it** — it is a necessary follow-up in the Servoy-Copilot repo so that MCP-driven
  form preview/screenshot tools get the same fix. For compatibility, that follow-up
  should use the **same parameter name** this spec standardizes on in this repo
  (`svy_testmode`, already used by `FormPreviewService.showFormInBrowser`'s and
  `screenshotForm`'s generated `?formpreview=<form>` URLs — note neither currently
  appends `svy_testmode=true`; the Servoy-Copilot follow-up should add it there too) —
  see open question in §7.
- **`WebPackagesListener`'s `PackageCheckerJob` build-mode selection**
  (`Utils.isInTestingMode(null)` → `build_sourcemap`, SVY-20573) is not changed by this
  spec. See §3.3 and the open question below.
- **`RunSingleTestHandler`/`RunAllE2ETestsHandler`'s E2E (non-form) test flows** are not
  given a per-request testing-mode replacement in this spec — only their global-flag
  write is removed. Adding `svy_testmode=true` to arbitrary E2E `cy.visit(...)` calls
  would require changing the E2E spec generator/template contract
  (`ServoyTestingServer.generateCypressE2ETest`, Servoy-Copilot repo) and is left for a
  follow-up if E2E tests are found to need forced testing-mode attributes.
- No change to `NGClient.handleArguments`'s existing `svy_testmode` startup-argument
  handling — it already works correctly per-client and is left as-is.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should `WebPackagesListener`'s build-mode selection stop keying off `isInTestingMode(null)` entirely (approach 3 from triage), now that the Developer-embedded handlers no longer set the global flag during normal use? Is sourcemap-on-MCP-call (SVY-20573) still a desired behavior for any remaining caller of the global flag (e.g. `CypressFormTestRunner`, or the Servoy-Copilot `ServoyTestingServer` before its own fix lands)? | Johan Compagner / Rene van Veen | open |
| Should the Servoy-Copilot repo's `ServoyTestingServer.ensureTestingMode()` follow-up use the `svy_testmode` parameter name (matching this repo's existing `FormSpecGenerator`/`NGClient.handleArguments` convention) rather than a new `testingMode` name, to keep one contract across both repos? | Whoever picks up the Servoy-Copilot follow-up | open |
| Do `RunSingleTestHandler`/`RunAllE2ETestsHandler`'s E2E test runs actually need forced testing-mode `data-cy` attributes in practice, or was the global flag write there just copy-paste from the form-test handlers with no functional need (since E2E specs don't rely on `svy_testmode` today)? If no need is found, no further action is required beyond deleting the dead `Settings.setProperty` call (already in this spec's plan). | QA / whoever authored the E2E Cypress flow | open |

# Spec: SVY-21528 — Consolidate duplicated NG client websocket session factories

## 1. Goal

Replace the two independently maintained, anonymous `IWebsocketSessionFactory` implementations registered for
`WebsocketSessionFactory.CLIENT_ENDPOINT` (one in `com.servoy.eclipse.ngclient.startup.Activator`, one in
`CypressFormTestRunner.activateNgClientBundle()`) with ONE named class in `com.servoy.eclipse.ngclient.startup`.
Both call sites register an instance of that class, parameterised only by what legitimately differs (the
`IDesignerCallback`: real in Developer, `null` headless). A parameter branch (`formpreview`, `nodebug`,
`svy_developer`, debug client) then exists in exactly one place and cannot drift again (the SVY-21173 bug), and the
factory can be recognised by type.

> **Note:** this spec was written before the runner's own registration was removed. Wherever it says the runner
> (`CypressFormTestRunner.activateNgClientBundle()`) registers the factory, read section 8 ("As built"): that method
> was deleted afterwards and the headless runner relies on `Activator.start`.

## 2. Background

Approved approach: option (a) "extract and parameterise". Triage: `docs/SVY-21528-triage.md`. Constraints come from
`docs/SVY-21173-cypress-runner-reopen-fixes.spec.md` (§2.3, §2.4, §3.5) and `docs/reviews/SVY-21173-review-summary.md`
(line 23, consolidation deferred to this ticket). Introducing commit (carried from triage, not re-investigated):
`a98a08c4d4` ("SVY-21173 ... headlessly", 2026-09-30) added the `formpreview` branch to both factories; follow-up
`97536e3a89` aligned `FormPreviewNGClient` and the Activator.

### 2.1 The three registrations

1. `ResourceProvider.initSpecProvider()` (ngclient.startup.resourceprovider): plain `new WebsocketSessionFactory()`
   only if none is registered yet. **Deliberately left as is** (fallback, out of scope).
2. `ngclient.startup.Activator.start()`: anonymous factory, only when `ApplicationServerRegistry.getServiceRegistry() != null`.
   `NGClientWebsocketSession(sessionKey, designerCallback)`; `init` branches: `formpreview` (reuse/retarget or new
   `FormPreviewNGClient`), `nodebug` (plain `NGClient`), `svy_developer` (static `developerNGClient`), else
   debug-client path (15 min window timeout, `IDebugClientHandler` service, fallback plain `NGClient`).
   `createEventDispatcher()` is overridden so the "Debug command reader" thread counts as the event dispatch thread.
3. `CypressFormTestRunner.activateNgClientBundle()`: anonymous factory, `designerCallback == null`, only the
   `formpreview` branch, otherwise `super.init()`. Unconditionally overwrites 1/2.

Important existing facts:

- `Activator.designerCallback` and `Activator.developerNGClient` are **static fields set after `start()`**
  (`com.servoy.eclipse.core.Activator` line ~432 `setDesignerCallback`, `com.servoy.eclipse.ui.Activator` line ~394
  `setDeveloperNGClient`). The current anonymous class reads them lazily at `createSession`/`init` time. The new class
  must preserve this late binding (see 3.2).
- Packages: `com.servoy.eclipse.ngclient` MANIFEST already has `Export-Package: com.servoy.eclipse.ngclient.startup`;
  `com.servoy.eclipse.cypress` MANIFEST already has `Require-Bundle: com.servoy.eclipse.ngclient` and
  `Import-Package: com.servoy.eclipse.ngclient.startup` (already used for `FormPreviewNGClient`). **No MANIFEST change
  is expected**; verify only (cypress already imports `org.sablo.websocket`; after the change it may drop the now
  unused `IWebsocketSession`/`WebsocketSessionKey` imports in code, not in the MANIFEST).
- `formpreview` semantics: a real NG client showing the requested form WITH DATA, bypassing authentication through
  `FormPreviewNGClient.showDefaultLogin()`. It is used to test forms in a browser (Cypress); it is NOT the designer's
  dataless form-template preview.

## 3. Design

### 3.1 New class

`com.servoy.eclipse.ngclient.startup.NGClientWebsocketSessionFactory` (name proposed; public, non-final, non-anonymous,
`implements IWebsocketSessionFactory`, next to `FormPreviewNGClient`). Because it is the only factory type for
`CLIENT_ENDPOINT` that carries the full branching, `instanceof NGClientWebsocketSessionFactory` becomes a reliable
check (not required by any caller now; do NOT reintroduce an "already registered, skip" guard in the runner — the
runner keeps overwriting unconditionally, per SVY-21173).

Constructor takes suppliers so late-bound statics keep working:

```java
public NGClientWebsocketSessionFactory(Supplier<IDesignerCallback> designerCallback,
                                       Supplier<NGClient> developerNGClient)
```

- Activator: `new NGClientWebsocketSessionFactory(() -> designerCallback, () -> developerNGClient)` (lambdas over the
  existing statics; "registers it with the real designerCallback" — real value resolved at session creation).
- Runner: `new NGClientWebsocketSessionFactory(() -> null, () -> null)`. A convenience constructor/static factory
  `forHeadless()` may wrap this.

`createSession(WebsocketSessionKey)` returns a named (static nested, package-private or private) subclass
`FactorySession extends NGClientWebsocketSession` — named rather than anonymous, so `instanceof` on the session is
also possible — constructed with `designerCallback.get()`. It holds the one `init` and the Developer-specific
`createEventDispatcher` override.

### 3.2 `init` (single, all branches, same order as today)

`if (getClient() == null)` then:

1. `formpreview` → see 3.3.
2. `nodebug` → `setClient(new NGClient(this, designerCallback))`.
3. `svy_developer` → `setClient(developerNGClient.get())`. Headless: supplier returns `null`; `setClient(null)` would be
   a silent no-op-ish state. Make it safe: if the supplier returns `null`, log a `Debug.warn` and fall through to the
   plain `new NGClient(this, designerCallback)` (inert, no NPE). Developer behaviour unchanged (non-null client).
4. else (debug/plain fallback): unchanged logic — window-timeout bump (guarded by `getWindowTimeout() ==
   DEFAULT_WINDOW_TIMEOUT`), `ApplicationServerRegistry.getServiceRegistry().getService(IDebugClientHandler.class)`;
   if the service is `null` (headless) → `new NGClient(this, designerCallback)`; else existing debug client recycle /
   `createDebugNGClient(this)`. Null-check `getServiceRegistry()` itself too (inert if absent).

Behavioural difference in headless vs. today: the old headless factory delegated to `super.init(requestParams)` when
`formpreview` was absent. The new path creates a plain `NGClient` via the fallback (`IDebugClientHandler` is null
headless) — same class `super.init` would create. **Must be verified** (see 4, step 6, and AC): read
`NGClientWebsocketSession.init` in the servoy_ngclient bundle to confirm that what it does (client creation, any
extra session setup) is equivalent; if `super.init` does anything more than creating the `NGClient`, call
`super.init(requestParams)` in the fallback when no debug handler service exists, instead of `new NGClient`.
The 15-minute window timeout is also applied headless; this is harmless (a longer idle timeout for a test session)
but should be noted; alternatively apply it only when the debug service is non-null (preferred, keeps headless
identical to today). Decision: apply only when `IDebugClientHandler` service is non-null.

### 3.3 `formpreview` branch and the retarget risk

Shared branch (from Activator): read the form name from `requestParams.get("formpreview").get(0)`; if
`FormPreviewNGClient.getInstance()` is non-null, not shut down and its websocket session key equals this session's key →
`existing.retarget(formName)` + `setClient(existing)`; else `setPendingTargetFormName(formName)` + `new
FormPreviewNGClient(this, designerCallback, formName)` + `setClient`.

Risk: the old headless factory always created a new client, never reused. Analysis of `FormPreviewNGClient`:

- The constructor shuts down any previous live instance (`shutdownOnEventThread`) and sets `instance = this`, so
  licenses are released when a new client is created.
- `FormSpecRunner` (line ~94) calls `FormPreviewNGClient.shutdownExisting()` before each spec: it shuts the instance
  down and sets `instance = null`. Hence at the start of every spec `getInstance()` is `null`/shut down and the shared
  branch takes the "new client" path — the SVY-21173 per-spec release is unchanged.
- Reuse/retarget only triggers when the same websocket session key re-sends `init` while the instance is alive
  (e.g. a page reload within one spec). Retargeting the live client then shows the requested form and does not
  allocate an extra client/license, which is no worse (arguably better) than creating a new one.

Decision: keep the shared reuse/retarget logic in headless (no flag), relying on `shutdownExisting()` for per-spec
isolation. Add a Javadoc note on the branch documenting this dependency. If review prefers zero behaviour change
headless, the fallback is a boolean constructor parameter `reuseFormPreviewClient` (false headless); not planned.

`FormPreviewNGClient.getInstance()` Javadoc ("used by the session factory (Activator)") should be updated to reference
the new class.

### 3.4 `createEventDispatcher` override

Move the Developer override (`isEventDispatchThread` / `getCurrentEventIfOnEventThread` honouring the "Debug command
reader" thread) into the named session class unchanged. It is a pure thread-name check, inert headless.

### 3.5 Comments and Javadoc

- `Activator.java`: remove the large `formpreview` comment block that says "The headless CypressFormTestRunner installs
  an equivalent factory of its own" (stale — there is now one shared class). Replace with a one-line comment above
  `setWebsocketSessionFactory` that Developer and the headless Cypress runner register the same
  `NGClientWebsocketSessionFactory` type. Remove the commented-out `getNGClient` block only if untouched by review
  preference (leave it; out of scope).
- `ResourceProvider.initSpecProvider()` (registration #1): at most add a one-line comment: "plain fallback; replaced by
  NGClientWebsocketSessionFactory from ngclient.startup.Activator (Developer) or CypressFormTestRunner (headless)".
  No logic change.
- `CypressFormTestRunner.activateNgClientBundle()` Javadoc: rewrite. Remove the claim that the Activator's factory is
  anonymous with no `formpreview` branch and the `instanceof` explanation; state instead that it registers the shared
  `NGClientWebsocketSessionFactory` with a `null` designer callback, that registration still unconditionally overwrites
  the previous factory (a guard is unnecessary and must not be added: the previous one may be the plain
  `ResourceProvider` fallback), and that the `formpreview` contract is "a real NG client with data, auth bypassed".
  Fix the "overwriting is safe" paragraph (no longer "delegates via `super.init`").
- `FormPreviewNGClient` class Javadoc: add a sentence that this is the data-bearing Cypress/browser test client, not the
  designer's dataless preview.

## 4. Implementation plan

1. Create `com.servoy.eclipse.ngclient/src/com/servoy/eclipse/ngclient/startup/NGClientWebsocketSessionFactory.java`
   (license header like neighbours, tabs, imports organised) with the constructor, `createSession`, named session
   class, unified `init`, `createEventDispatcher`. Use eclipse-coder `createFile`.
2. Edit `Activator.start()`: replace the anonymous factory with
   `new NGClientWebsocketSessionFactory(() -> designerCallback, () -> developerNGClient)`; remove now unused imports
   (`List`, `Map`, `Event`, `IEventDispatcher`, `BaseWebsocketSession`, `IWebsocketSession`, `IWebsocketSessionFactory`
   if unused, `WebsocketSessionKey`, `IDebugClientHandler`, `NGClient` stays for the static, `NGClientWebsocketSession`,
   `NGEventDispatcher`); keep the `getServiceRegistry() != null` guard. Update comments (3.5).
3. Edit `CypressFormTestRunner.activateNgClientBundle()`: register `new NGClientWebsocketSessionFactory(() -> null, () -> null)`;
   rewrite Javadoc; remove unused imports (`IWebsocketSession`, `IWebsocketSessionFactory`, `WebsocketSessionKey`,
   `NGClientWebsocketSession`, `FormPreviewNGClient` if no longer referenced — check; `WebsocketSessionFactory` import is
   still needed for `CLIENT_ENDPOINT`), add import of the new class. Keep the try/catch and `outputExtra` messages
   (update text to "shared NG client session factory").
4. Update `FormPreviewNGClient` Javadoc (3.3, 3.5) and add the optional one-line comment in `ResourceProvider`.
5. MANIFESTs: verify `com.servoy.eclipse.ngclient` exports `ngclient.startup` and `com.servoy.eclipse.cypress`
   imports it (both true today) — no edits expected. Do not edit MANIFEST by hand over the 72-byte limit; use eclipse-coder
   if a change turns out to be needed.
6. Read `NGClientWebsocketSession.init` (servoy_ngclient) to confirm 3.2 equivalence for the headless no-`formpreview` path.
7. Run `getCompilationErrors`, `organizeImports`, `formatFile` per AGENTS.md; address Spotbugs high-severity findings.
8. Add tests (section 4.1).

### 4.1 Testing

> **As implemented:** open question 1 was settled by creating a new plainjunit-only fragment,
> `com.servoy.eclipse.ngclient.tests` (fragment of `com.servoy.eclipse.ngclient`, module added to the root `pom.xml`),
> instead of using `ngclient.ui.tests` or `com.servoy.eclipse.tests` as proposed below. The two seams
> (`selectClientKind`, `shouldReuseFormPreviewClient`) are `public static`, not package-private, because the tests live in
> `...startup.plainjunit`, a different package from the factory. See section 8.

Location: `com.servoy.eclipse.ngclient` has no `.tests` project in this repo listing; the closest existing is
`com.servoy.eclipse.ngclient.ui.tests`. Place the branch-selection test there under
`com.servoy.eclipse.ngclient.ui.tests.plainjunit...` only if `com.servoy.eclipse.ngclient` can be added to its
`Require-Bundle` without pulling workspace/OSGi state; otherwise place it in `com.servoy.eclipse.tests` under
`com.servoy.eclipse.tests.plainjunit`. (Open question 1.) Per AGENTS.md package placement: tests that need no OSGi /
`Activator.getInstance()` go in `plainjunit` (run with `eclipse-ide_runJUnitTests`); anything needing a running
workbench goes in `integrationpde`. Never mix kinds in one package. Test class names end in `Test`.

Plain-JUnit feasible (design for it): extract the branch decision into a package-private pure static method on the
factory, e.g. `static ClientKind selectClientKind(Map<String,List<String>> params, boolean hasDeveloperNGClient,
boolean hasDebugHandler)` returning an enum (`FORM_PREVIEW`, `NO_DEBUG`, `DEVELOPER`, `DEBUG`, `PLAIN`). Tests
(`NGClientWebsocketSessionFactoryTest`, `plainjunit`):

- `formpreview` wins over `nodebug`/`svy_developer` (order preserved).
- `nodebug` → `NO_DEBUG`; `svy_developer` with developer client present → `DEVELOPER`; `svy_developer` with `null`
  developer client → `PLAIN` (headless-safe).
- none of the params, with debug handler → `DEBUG`; without (headless) → `PLAIN`.
- Factory is a named type: `new NGClientWebsocketSessionFactory(() -> null, () -> null) instanceof
  IWebsocketSessionFactory`, and `getClass().isAnonymousClass()` is false (guards the SVY-21173 regression).
- Retarget decision: also extract (package-private static, in the factory or `FormPreviewNGClient`) the "reuse existing
  vs create new" predicate (`existing != null && !shutDown && sameSessionKey`) and test the three false cases and the
  true case — assertion that after `shutdownExisting()` (`getInstance()==null`) the new-client path is chosen.

Not covered by plain tests: end-to-end session creation (needs the NG client stack) — `integrationpde` only if cheap;
otherwise verified manually: run a Cypress spec headless with the Cypress runner (per-spec formpreview, no "No more
licenses available") and in Developer via "Run Cypress Form Test(s)", plus a normal debug-client launch and a
`?nodebug` and `?svy_developer` launch.

## 5. Acceptance criteria

- [ ] One named class `com.servoy.eclipse.ngclient.startup.NGClientWebsocketSessionFactory` holds all `init` branches
      and the Developer `createEventDispatcher` override; no anonymous session factory remains in `Activator` or
      `CypressFormTestRunner`.
- [ ] `Activator.start()` registers it with the real designer callback / developer NG client resolved lazily (values set
      after `start()` are honoured).
- [ ] `CypressFormTestRunner.activateNgClientBundle()` registers the same class with `null` callback and `null` developer client.
- [ ] Debug and `svy_developer` branches are inert headless (no NPE with null `IDebugClientHandler` service, null
      `getServiceRegistry()` or null developer client).
- [ ] Developer behaviour is unchanged for formpreview (incl. reuse/retarget), `nodebug`, `svy_developer`, and debug-client launches.
- [ ] Headless: each spec gets its own `FormPreviewNGClient` (via `shutdownExisting()`), no license leak; the headless
      no-`formpreview` path creates the same client as the old `super.init`.
- [ ] Stale comments fixed: Activator ("equivalent factory of its own"), `CypressFormTestRunner.activateNgClientBundle`
      Javadoc, `FormPreviewNGClient.getInstance()` Javadoc; registration #1 untouched except an optional one-line comment.
- [ ] No MANIFEST regression; bundles resolve; no compilation errors; no new high-severity Spotbugs issues.
- [ ] Plain-JUnit tests from 4.1 pass in the correct `plainjunit` package.

## 6. Out of scope

- Registration #1 (`ResourceProvider.initSpecProvider()` plain `WebsocketSessionFactory` fallback): unchanged.
- Options (b) removing the runner registration and (c) a pluggable parameter-handler/listener extension point.
- Making the `formpreview` auth bypass or `IndexPageFilter` changes.
- Changing `FormPreviewNGClient` behaviour (retarget/shutdown logic) beyond Javadoc.
- Cleaning up the commented-out `getNGClient`/extension-point code in `Activator`.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Which test project hosts the plain tests (`ngclient.ui.tests` needing a new Require-Bundle on `com.servoy.eclipse.ngclient`, vs `com.servoy.eclipse.tests`)? | Dev / Johan | Open (assumption: `com.servoy.eclipse.tests` plainjunit if ngclient bundle is already resolvable there) |
| Is the proposed name `NGClientWebsocketSessionFactory` acceptable? | Johan | Open (assumption: yes) |
| Does `NGClientWebsocketSession.init(requestParams)` do anything beyond creating the client that headless fallback must retain (3.2)? | Dev | Open — verify in implementation step 6 |
| Keep reuse/retarget on in headless (decision) or gate it with a flag? | Johan | Assumption: keep on, per-spec isolation via `shutdownExisting()` |
| Suppliers vs. plain values in constructor (needed because statics are set after `start()`)? | Dev | Assumption: suppliers |

Status of the open questions, as built: test host project = new `com.servoy.eclipse.ngclient.tests` fragment; the name
`NGClientWebsocketSessionFactory` was kept; `NGClientWebsocketSession.init` was read and creates only a plain
`NGClient`, so the headless fallback is equivalent; reuse/retarget is kept on in headless; suppliers were used.

## 8. As built (deviations from the plan above)

- **Headless fallback (3.2):** verified against `NGClientWebsocketSession.init` in `servoy_ngclient`: when no client
  exists it only calls `setClient(new NGClient(this, designerCallback))`. The factory's plain path does the same, so no
  `super.init` call is needed. The 15-minute window timeout is applied only in the DEBUG branch (handler present).
- **`svy_developer` without a developer client:** logs `Debug.warn` and creates a plain `NGClient` (previously
  `setClient(null)`).
- **Branch selection** is extracted into `selectClientKind(params, hasDeveloperNGClient, hasDebugHandler)` returning
  `ClientKind` (`FORM_PREVIEW`, `NO_DEBUG`, `DEVELOPER`, `DEBUG`, `PLAIN`), and the reuse decision into
  `shouldReuseFormPreviewClient(exists, shutDown, sameSessionKey)`. Both are `public static` (see 4.1).
- **Runner registration removed (follow-up after review of the ticket comment):** `CypressFormTestRunner` no longer
  has `activateNgClientBundle()` and registers nothing. The headless runner relies on `Activator.start`, which
  registers the shared factory; there the suppliers return `null` because `designerCallback` and `developerNGClient`
  are only set by the Developer-only `core` and `ui` activators. This supersedes sections 3.1 (Runner), 3.5
  (`activateNgClientBundle` Javadoc), implementation plan step 3 and the acceptance criterion about the runner
  registering the class. Consequence: the explicit "unconditionally overwrite" no longer exists, and the registration
  depends on `Activator.start` seeing a non-null service registry (set earlier by `j2db_server`'s `Activator.start`,
  while the ngclient bundle activates lazily). `forHeadless()` is no longer used by production code (only by a plain
  test); it documents the headless configuration.
- **Not done:** the optional one-line comment in `ResourceProvider` (registration #1 is untouched).
- **Added beyond the spec:** a headless launch configuration,
  `com.servoy.eclipse.cypress/launch_files/Cypress Form Test Runner (headless).launch`, running solution
  `testscenario1`. It passes `-ie` (ignore build errors) because that solution has two build error markers.
- **Verification done:** 15 plain-JUnit tests pass. In Developer, Cypress form runs and a `showFormInBrowser` request
  selected `FORM_PREVIEW` and a plain launch selected `DEBUG`. A headless runner run of two specs passed, each with its
  own `FormPreviewNGClient`, both with the runner registration and after removing it (the second spec, `fx_twin_a`, is
  not the solution's first form, so it passing shows the `formpreview` branch was reached).
- **Not verified:** the reuse/retarget path (every request so far had a new session key), `?nodebug`, and
  `?svy_developer`.

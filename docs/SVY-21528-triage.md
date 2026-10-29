# Triage Report — SVY-21528

**Verdict:** NEEDS_INPUT (at triage time; the open questions were answered afterwards, see "Resolution" at the end of this report)

## Reported problem

Two (actually **three** — see below) places register an `IWebsocketSessionFactory`
for the same `WebsocketSessionFactory.CLIENT_ENDPOINT` slot, and the behaviour of a
client request depends on which registration wins. Any request-parameter branch
(`formpreview`, `nodebug`, `svy_developer`, debug-client) added to one factory and not
the other silently changes behaviour depending on how the NG client was launched. This
divergence already produced a real bug during SVY-21173: the `formpreview` branch was
present only in the headless runner's factory, so when Developer's own Activator factory
won, `?formpreview=<form>` was ignored and every form spec got the solution's
`firstFormID` instead of its own form, failing with
`Expected to find element: [data-cy^="<form>."]`.

The ticket also notes a secondary smell: the Activator registers an **anonymous**
factory, so nothing can tell by type whether a "formpreview-aware" factory is already
installed — an `instanceof` guard tried for this was always false.

## Root-cause assessment

There are **three** registrations against `CLIENT_ENDPOINT`, not two, and only the first
two carry request-parameter branching:

1. `com.servoy.eclipse.ngclient.startup.resourceprovider.ResourceProvider.initSpecProvider()`
   (line 236–239): registers a **plain** `new WebsocketSessionFactory()` *only if none is
   registered yet* (`getWebsocketSessionFactory(...) == null` guard). This is the base/no-op
   registration with no custom `init` branching.
2. `com.servoy.eclipse.ngclient.startup.Activator.start()` (line 81): an **anonymous**
   `IWebsocketSessionFactory` building an `NGClientWebsocketSession` with a non-null
   `designerCallback`, whose `init` branches on `formpreview` → `FormPreviewNGClient`
   (with reuse/retarget), `nodebug` → plain `NGClient`, `svy_developer` → the shared
   `developerNGClient`, else the debug-client path. Runs only when
   `ApplicationServerRegistry.getServiceRegistry() != null`. This is the **Developer** path.
3. `com.servoy.eclipse.cypress.headless.CypressFormTestRunner.activateNgClientBundle()`
   (line 479–504): an **anonymous** `IWebsocketSessionFactory` building an
   `NGClientWebsocketSession` with a **null** `designerCallback`, whose `init` handles only
   `formpreview` → `FormPreviewNGClient` and otherwise delegates to `super.init()`. It
   **unconditionally overwrites** whatever (1) or (2) installed. This is the **headless CI**
   path.

The branching that legitimately differs between (2) and (3) is exactly: `designerCallback`
(non-null in Developer, null headless) and whether the debug/`svy_developer` paths exist
(meaningless headless, where no debug client handler or developer NG client is present —
`Activator.developerNGClient`/`designerCallback` are wired up only by
`com.servoy.eclipse.core.Activator` / `com.servoy.eclipse.ui.Activator`, which do not run
in the UI-disabled headless exporter). The `formpreview` branch is common to both and is
the one that drifted.

Git history confirms this is a known, deliberately-deferred follow-up. The introducing
commit `a98a08c4d4` ("SVY-21173 … headlessly") added the `formpreview` branch to **both**
factories independently precisely because they are duplicated, and its own message plus
`docs/SVY-21173-cypress-runner-reopen-fixes.spec.md` §2.3/§2.4/§3.5 document that the two
registrations are separate and that the `instanceof`-type detection is unreliable (the
Activator's factory is anonymous). `docs/reviews/SVY-21173-review-summary.md` line 23
explicitly flags "two duplicate CLIENT_ENDPOINT session-factory registrations … worth
consolidating into one registration" and says it was deferred to a follow-up — which is
this ticket. So the root cause (duplicated, independently-maintained factory registrations
for one endpoint) is real and correctly identified.

## Ticket premise check

The ticket's *problem statement* is accurate and well-evidenced. Its *proposed solution* —
"extract factory creation into one place, parameterised by what legitimately differs
(`designerCallback`: non-null in Developer, null headless); if two registrations must
remain, give them a shared identifiable type" — is a reasonable direction but is **not the
only one, and the ticket's own reporter (the architect, Johan Compagner) challenges it in
the single comment on the issue.** He asks three things that materially change the design:

1. *"Why do we have the register of that in `CypressFormTestRunner` in the first place? Why
   not fully remove that? Because it always should be running in a place where this is
   already registered, right?"* — i.e. maybe registration (3) should not exist at all; the
   headless runner runs inside an environment where (2) (or at least (1)) already registers
   a factory.
2. Is the real need just the handling of the extra `formpreview` parameter (an actual-client
   preview with data, not the designer's quick dataless form template)?
3. *"If it is just handling that parameter, can't we make it so that you can register a
   parameter-handling listener to the Activator? So that what you really want to do there you
   can do by just 'extending' the existing stuff?"* — i.e. a pluggable
   parameter-handler/listener extension on the single Activator factory, rather than a second
   whole factory.

These three proposals (consolidate-and-parameterise vs. delete-the-runner-registration vs.
pluggable-listener-extension) lead to **materially different root-cause fixes and blast
radius**, and the ticket does not contain the facts needed to choose:

- Whether (2) actually runs in the headless `CypressFormTestRunner` process. The runner is
  an `AbstractWorkspaceExporter`-based headless Eclipse application; `ngclient.startup`
  has `Bundle-ActivationPolicy: lazy` with this exact `Activator`, so whether it has been
  started (and whether `ApplicationServerRegistry.getServiceRegistry() != null` at that
  point) in headless mode determines whether Johan's "just remove the runner's registration"
  is viable. The runner installs its own factory *after* the app server is up, strongly
  suggesting the Activator's factory is either not installed or not suitable headless — but
  this must be confirmed by the reporter/empirically, not assumed.
- Whether the headless path can tolerate the Developer factory's debug/`svy_developer`
  branches (which reference a debug client handler and `developerNGClient` that do not exist
  headless), or whether those branches are safely inert when their request parameters are
  absent.
- Which of the three designs the architect actually wants — the comment reads as a request
  to reconsider, not an approval of the ticket's own "extract-and-parameterise" wording.

Per the divergence test, ≥2 of these lead to different root causes and the ticket lacks the
detail to rule any out, so this is a `NEEDS_INPUT`, not a `PROCEED`.

## Approaches considered

1. **Consolidate into one shared factory, parameterised by `designerCallback`** (the
   ticket's own proposal). A single `NGClientWebsocketSession`-building factory with one
   `init` carrying all branches (`formpreview`, `nodebug`, `svy_developer`, debug, plain),
   instantiated once by the Activator (with the real `designerCallback`) and, if still
   needed, once by the runner (with `null`).
   - Pros: removes the `formpreview`-type drift by having one `init`; smallest conceptual
     change; keeps both call sites.
   - Cons: still two registrations racing for one slot (the runner still overwrites); the
     "which wins" fragility is reduced but not eliminated; a shared factory that carries
     debug/`svy_developer` branches must be proven inert headless.

2. **Remove the runner's registration entirely** (Johan's question 1). Let the headless
   runner rely on whatever already registers for `CLIENT_ENDPOINT`, adding only the
   `formpreview` handling it needs.
   - Pros: eliminates the duplicate outright — the strongest fix if the Activator's factory
     is actually installed and usable headless.
   - Cons: only correct if (2) genuinely runs and its debug/developer branches are harmless
     headless; the headless factory deliberately passes `designerCallback == null` and skips
     those branches, so this needs verification that nothing regresses. If (2) is **not**
     active headless, this approach is simply wrong.

3. **Pluggable `formpreview` parameter-handler/listener on the single Activator factory**
   (Johan's question 3). Keep one factory; let the runner (and Developer) contribute the
   extra `formpreview` handling via a registered listener/extension instead of a whole second
   factory.
   - Pros: one registration, one `init`; the only thing that varies (`formpreview`
     handling / `designerCallback`) becomes an explicit, testable extension point; matches
     the architect's stated preference for "extending the existing stuff".
   - Cons: larger design change (new listener/extension API); must be designed so the
     headless and Developer contributions are both expressible.

4. **No code change.** Leave three registrations; keep both `formpreview` branches in sync
   by discipline.
   - Pros: zero risk now.
   - Cons: the drift already caused one shipped bug; the review summary and the ticket both
     call for consolidation; leaving it guarantees the next parameter added to one factory
     and not the other reintroduces the same class of defect. Not advisable.

## Recommendation

**NEEDS_INPUT.** The duplication is real and worth fixing (approach 4 is not advisable), but
the reporter/architect has, in the issue's only comment, explicitly questioned the ticket's
own "extract-and-parameterise" framing and offered two alternative designs (delete the
runner's registration; make `formpreview` a pluggable listener on the single Activator
factory). These lead to materially different implementations and blast radii, and the ticket
does not contain the facts needed to choose between them. The specific questions are listed
below. Once answered, this is a straightforward `PROCEED` on the chosen design.

## Git history findings

- Introducing commit for the current duplicated state: `a98a08c4d4e2316b52144afcf347e82578b73508`
  ("SVY-21173 Create a dedicated Servoy product/application for running Cypress form tests
  headlessly[ai]", Diana Bunaciu, 2026-09-30). Its message documents that the `formpreview`
  branch was added to **both** factories independently and that
  `CypressFormTestRunner.activateNgClientBundle()`'s old `!(existingFactory instanceof
  WebsocketSessionFactory)` guard was always true (Activator registers an anonymous factory)
  so the runner's factory was never installed — the guard was removed and the factory now
  unconditionally overwrites.
- Follow-up alignment commit: `97536e3a89` ("SVY-21173 align FormPreviewNGClient and ngclient
  Activator with release[ai]").
- `docs/SVY-21173-cypress-runner-reopen-fixes.spec.md` §2.3, §2.4, §3.5 and
  `docs/reviews/SVY-21173-review-summary.md` line 23 all pre-document this duplication and
  that consolidation was *deliberately deferred to a follow-up* — this ticket — and warn that
  no `instanceof`/type-sniffing detection should be reintroduced because the Activator's
  factory is anonymous.
- No dependency/target-platform version change is involved; this is not a regression from an
  upgrade.

## Questions for the reporter (NEEDS_INPUT only)

1. Which design do you want? (a) one shared factory parameterised by `designerCallback`
   (the ticket's wording); (b) remove `CypressFormTestRunner.activateNgClientBundle()`
   entirely and rely on the already-registered factory, adding only `formpreview` handling;
   or (c) a pluggable `formpreview` parameter-handling listener/extension on the single
   `ngclient.startup.Activator` factory (your comment's suggestion).
2. For option (b): can you confirm that `com.servoy.eclipse.ngclient.startup.Activator.start()`
   actually runs — and registers its factory — inside the headless `CypressFormTestRunner`
   process (UI-disabled exporter, embedded app server)? Its debug/`svy_developer` branches
   reference a debug client handler and `developerNGClient` that are wired up only by the
   Developer-only `com.servoy.eclipse.core`/`com.servoy.eclipse.ui` activators; are those
   branches guaranteed inert headless (because their request parameters never appear), or
   would relying on that factory headless risk hitting them?
3. Is the `formpreview` requirement specifically "a real NG client showing that form *with
   data*" (as opposed to the designer's dataless quick form-template preview)? Confirming
   this fixes the contract the consolidated handling must satisfy.
4. Should the base `WebsocketSessionFactory` registered by `ResourceProvider.initSpecProvider()`
   (registration #1) be folded into the same consolidation, or left as the fallback it is
   today?

## Resolution (added after the human decision and the implementation)

1. **Design:** option (a), one shared named factory parameterised by `designerCallback` (and the developer NG client).
2. **Does the Activator's factory run headless?** Yes. In a headless `CypressFormTestRunner` run the log shows
   `Activator.start` finding no factory, registering one (service registry available), and then
   `CypressFormTestRunner.activateNgClientBundle` replacing it. The runner therefore still overwrites unconditionally,
   as designed in SVY-21173. The debug and `svy_developer` branches were not hit headless: the debug-handler service
   is absent, `designerCallback` is `null`, and the log showed `kind=FORM_PREVIEW` for every spec.
3. **`formpreview` contract:** a real NG client showing the requested form with data, authentication bypassed through
   `FormPreviewNGClient.showDefaultLogin()`. It is not the designer's dataless form-template preview.
4. **Registration #1** (`ResourceProvider.initSpecProvider`) is left as the plain fallback. In both Developer and
   headless runs it found the shared factory already registered and did nothing.
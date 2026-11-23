# Triage Report — SVY-21714

**Verdict:** PROCEED

> **Implementation target is NOT this repository.** SVY-21714 concerns the `servoy-client`
> and `servoy-eclipse` git repositories (checked out under `C:\Users\Cristian\git\servoy\master\`),
> **not** Servoy-Copilot. None of the nine affected files exist in Servoy-Copilot — the
> `ServoyAI` label and the `[AI]` suffix only mark the ticket as AI-assisted analysis. This
> triage report is written into `Servoy-Copilot/docs/` because that is the configured repo
> root for the triage run, but the fix itself must be made in `servoy/master/servoy-client`
> and `servoy/master/servoy-eclipse`.

## Reported problem

A form-component property (e.g. a `servoycore-listformcomponent`'s `json.containedForm`)
whose value is written as a **bare form-name/UUID string** instead of the canonical
`{ "svy_form": "<UUID>" }` object throws a raw
`java.lang.ClassCastException: class java.lang.String cannot be cast to class org.json.JSONObject`.
In the NG client the reproduced path is `ChildrenJSONGenerator.visit` (reached from
`AngularFormGenerator.generateJS`) and the whole page stays blank. The designer/outline/
ghost-handler sites are in the IDE, so the same value can also break *editing* a form.

The ticket proposes a solution: at each of nine sites, call
`FormComponentPropertyType.asFormComponentValue(...)` before the cast and skip the property
when it returns `null`, exactly as SVY-21667 did for the sites it covered.

## Root-cause assessment

Every one of the nine named sites follows the identical defect shape, confirmed by reading
the current `master` source in both repos:

```java
Object propertyValue = fe.getPropertyValue(pd.getName());
Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs); // tolerant lookup
if (frm == null) continue;                                                // null guard
... getFormComponentCache(fe, pd, (JSONObject)propertyValue, frm, fs);    // UNGUARDED cast
```

`FormComponentPropertyType.getForm(...)`
(`servoy-client/servoy_ngclient/.../property/types/FormComponentPropertyType.java:230`)
accepts a `String`, a `UUID`, a `JSForm` **or** a `JSONObject` — so it happily resolves a
bare-string `containedForm` to a real `Form`. The `if (frm == null) continue;` guard therefore
does **not** protect the following `(JSONObject)propertyValue` cast: `frm` is non-null while
`propertyValue` is still the original `String`, so the cast throws. The guard added by
SVY-21667 — `asFormComponentValue(value, formName, elementName, propertyName)` at
`FormComponentPropertyType.java:268` — returns `null` (after logging one clear, actionable
`Debug.error`) precisely for this case, and the already-fixed sites `continue` when it does.
The nine sites below simply never had that guard applied.

Confirmed sites (line numbers drift slightly from the ticket; the pattern matches at each):

**servoy-client** (`servoy_ngclient/src/com/servoy/j2db/server/ngclient/...`):
- `ChildrenJSONGenerator.java` ~244 (`getFormComponentCache(fe, pd, (JSONObject)propertyValue, frm, ...)`) — reproduced
- `utils/FormComponentUtils.java` ~93 (`(JSONObject)rawPropertyValueOfFEsFCProperty`)
- `property/types/FormComponentSabloValue.java` ~130 (`(JSONObject)propertyValue`)

**servoy-eclipse**:
- `com.servoy.eclipse.designer/.../outline/FormOutlineContentProvider.java` ~444 (`(JSONObject)fcPropertyValue`)
- `com.servoy.eclipse.designer/.../rfb/DesignerWebsocketSession.java` ~980, ~1035, ~1109 (`(JSONObject)propertyValue`)
- `com.servoy.eclipse.designer/.../rfb/actions/handlers/GhostHandler.java` ~650 (`(JSONObject)propertyValue`)
- `com.servoy.eclipse.model/.../util/WebFormComponentChildType.java` ~407 (`(JSONObject)currentNgFormElementOfParentFC.getPropertyValue(...)`)

The bug is real, in Servoy's own code, and user-reachable from a hand-written or
AI-generated `.frm` that compiles clean. It is not a misconfiguration, not expected
behaviour, and not in a third-party dependency.

## Ticket premise check

The premise holds completely, and unusually so — this ticket is the explicitly-planned
follow-up to work that was already done:

- The three **servoy-client** sites were discovered *during* SVY-21667 (spec §3.3c,
  "Three further unguarded sites found, left unfixed by explicit decision (2026-11-02)")
  and left unfixed only because the user chose **"Document only, fix later."** SVY-21714 is
  that "fix later."
- The six **servoy-eclipse** sites are additional instances of the same shape, found later
  while testing SVY-21668.
- The proposed fix (`asFormComponentValue` + `continue` on null) is not speculative — it is
  the exact, already-shipped pattern from SVY-21667, and §3.3c of that spec even pre-writes
  the guidance for the three servoy-client sites (noting they need no `recursionCheck`
  rebalancing, unlike the primary `FormElementHelper`/`FormWrapper` sites).

One consistency note on the ticket's own "Not verified" section: it worries that
`ServoyFormBuilder.java:410` looked unguarded with no `asFormComponentValue` caller. Git
shows commit `1ae8b12c` *"SVY-21667 Guard form-component cast in Servoy Project Builder [ai]"*
(2026-11-03) already fixed that tenth site. The reporter's running Developer (build of
2026-11-04, but evidently predating/not including that commit on its branch) still logged
the old message — so `ServoyFormBuilder` is already handled and is correctly **excluded**
from the nine. No action is needed there.

## Approaches considered

1. **Per-site guard (ticket's primary proposal)** — at each of the nine sites, call
   `asFormComponentValue(value, formName, elementName, propertyName)` with whatever
   form/element context is in scope, and `continue`/skip on `null`.
   *Pros:* minimal, surgical, matches the proven SVY-21667 pattern exactly; each site logs
   one clear message naming the form/element/property; no behaviour change for valid values.
   *Cons:* nine near-identical edits across two repos; the same malformed value can be logged
   once per site it passes through (acceptable — each message is still clear).

2. **Single shared helper that both resolves the form and validates the cast** (ticket's
   optional suggestion) — route all sites through one method returning the `JSONObject` (or
   `null`) so a rejected value is logged once per operation, not once per site.
   *Pros:* single choke point; less duplication; one log line per bad value.
   *Cons:* larger refactor touching the shared `FormComponentPropertyType` contract and every
   caller's control flow; higher regression surface for a Minor bug; the per-site context
   (form/element names) still has to be threaded in. Better as a later cleanup than as the fix.

3. **No code change** — rely on the existing `getForm` tolerance and treat the blank page as
   a data-authoring error.
   *Pros:* zero code risk.
   *Cons:* unacceptable — the symptom is a context-free `ClassCastException` and a blank page
   with no indication of *which* form/element/property is at fault; the whole point of
   SVY-21667 was to replace exactly this failure with a clear, survivable error, and these
   nine sites defeat that for the most common authoring mistake. Rejected.

## Recommendation

**PROCEED with Approach 1** (per-site `asFormComponentValue` guard + skip-on-null), applied
to all nine sites in `servoy-client` and `servoy-eclipse`. It is the smallest change that
restores the SVY-21667 guarantee (one clear error, rest of the form still renders) on every
remaining path, reuses an already-shipped and tested helper, and carries low regression risk
for valid `{ "svy_form": ... }` values (the guard is a pass-through for real `JSONObject`s).

Keep **Approach 2** (shared resolve-and-validate helper) in mind as an optional later
consolidation — it is the ticket's own "optionally route all sites through one helper" note —
but it should not gate this fix.

**Important for whoever implements this:** work in the `servoy/master/servoy-client` and
`servoy/master/servoy-eclipse` checkouts, not Servoy-Copilot. Mirror the SVY-21667 test
style (there are existing `FormComponentPropertyTypeAsFormComponentValueTest` and
`TestServoyFormBuilderFormComponentGuard` tests to follow). `WebFormComponentChildType` and
the `DesignerWebsocketSession`/`GhostHandler`/`FormOutlineContentProvider` sites each already
have a `Form`/`FormElement`/`IFormElement` in scope to supply the form and element names for
the message.

## Git history findings

- `servoy-client@488c6adf1` (2026-11-02) *"SVY-21667 Guard form-component value cast, report
  clear error [ai]"* — introduced `asFormComponentValue` and fixed the first batch of sites;
  committed alongside `docs/SVY-21667-form-component-cast-guard.spec.md`, whose **§3.3c**
  explicitly records the three servoy-client sites as found-but-deliberately-deferred
  ("Document only, fix later"). This is the direct provenance of SVY-21714.
- `servoy-eclipse@1ae8b12c` / `servoy-client@2375e818` (2026-11-03) *"SVY-21667 Guard
  form-component cast in Servoy Project Builder [ai]"* — fixed the tenth site
  (`ServoyFormBuilder.addFormMarkers`, spec §3.3d), which is why the ticket's "Not verified"
  note about `ServoyFormBuilder` is already resolved and that file is not among the nine.
- No regression / dependency-version angle: this is pre-existing code that SVY-21667 began
  hardening and SVY-21714 finishes. The nine sites were never guarded, not broken by an
  upgrade.

# Triage Report — SVY-21548

**Verdict:** PROCEED

## Reported problem

List view and table view forms in the TiNG (Angular) NG client still run a server-side
generation path built around a `servoycore-portal` web component that no longer ships.
Observed symptom: a `NullPointerException` in `FormElementHelper.getFormElement()` at
`ConcurrentHashMap.putIfAbsent(formElement, null)`, hit once a plain NG client E2E run
re-initialised a list/table-view form through the *cached* `getFormElement()` branch.

An immediate null guard was already committed (`27e965926`, "Guard against null portal
FormElement so list/table-view forms don't NPE"). The ticket asks to go further and remove
the dead portal / list-view / table-view server-side generation path that depends on the
removed component.

**Proposed solution in the ticket (noted, not assumed correct):**
- Remove / rework `BodyPortal` and `FormElementHelper.createBodyPortalFormElement()`.
- Review `WebListFormUI` and the list/table-view branch of `WebFormController.initFormUI()`.
- Clean up remaining `servoycore-portal` references in `FormElement`,
  `FormTemplateGenerator`, `FormLayoutGenerator`, and the leftover
  `servoy_ngclient/war/servoycore/portal/` resources.
- Reconsider the null guard once the dead path is gone.

## Root-cause assessment

The portal web component was removed from the shipped core on **2026-06-10** by commit
`e2c57e928` ("remove the portal now from the core" — *"TiNG never used that, has no impl
for that at all"*). That commit deleted only the manifest registration:

```
servoy_ngclient/war/servoycore/META-INF/MANIFEST.MF
-Name: portal/portal.spec
-Web-Component: True
```

The spec file and all its assets are still physically present under
`servoy_ngclient/war/servoycore/portal/` (`portal.spec`, `portal.js`, `portal_server.js`,
ui-grid bundles, etc.), but because the manifest no longer declares them, the
`servoycore-portal` web component is no longer registered. Confirmed: the live Angular
client has **no** `servoycore-portal` consumer — only the unrelated
`servoycore-listformcomponent` exists in `com.servoy.eclipse.ngclient.ui/node/src`.

The server side was left fully intact. The chain is:

1. `WebFormController.initFormUI()` (`component/WebFormController.java:102-122`) still
   instantiates `WebListFormUI` for any non-record-view form that has a body part.
2. `WebListFormUI.getFormElements()` (`WebListFormUI.java:55-80`) removes the body
   elements and appends a `BodyPortal` (`new BodyPortal(form)`), then asks
   `FormElementHelper` to turn the persists into `FormElement`s.
3. `FormElementHelper.createBodyPortalFormElement()`
   (`FormElementHelper.java:514-697`) builds a `FormElement("servoycore-portal", …)`.
   Because the spec is unregistered, the `FormElement` falls back to the error bean, its
   `"childElements"` property is absent, `pd == null`, and the method returns `null`
   (lines 589-593, 696).
4. That `null` used to flow only through non-caching paths and was tolerated. The NPE
   surfaced once the **cached** branch (`getFormElement(...)` lines 454-479) fed the null
   into `persistWrappers.putIfAbsent(formElement, null)` — `ConcurrentHashMap` throws NPE
   on a null value.

`BodyPortal` is also constructed in two template-side wrappers that participate in the
same list/table-view path: `FormWrapper.getBaseComponents()`
(`template/FormWrapper.java:194`) and `PartWrapper` (`template/PartWrapper.java:162`), both
gated on `isListView`/`isTableView`. The type name `"servoycore-portal"` is produced by
`FormTemplateGenerator.getComponentTypeName()` for any `Portal` persist
(`template/FormTemplateGenerator.java:195-198`) and specially styled in
`FormLayoutGenerator` (`template/FormLayoutGenerator.java:419-425`) and tagged in
`FormElement` (`FormElement.java:887-889`).

So the ticket's "dead path" claim is accurate: the whole list/table-view server-side
rendering projection targets a component that no longer exists in TiNG. The error-bean
fallback means these forms currently render nothing meaningful for their body part in a
plain NG client regardless — the NPE was just the first hard failure.

## Ticket premise check

The premise holds up. This is genuinely dead/broken code, not a user-side issue and not a
third-party dependency problem. The portal was deliberately removed from the shipped core
(not accidentally), TiNG has no implementation, and the server generation that feeds it was
simply never cleaned up. The null guard (`27e965926`) is a correct stop-gap but, as the
ticket says, it only masks the symptom.

One caveat the ticket itself flags as a question and that the implementer must resolve:
**what should list-view and table-view forms actually render in TiNG now?** The removal
commit says TiNG "never used" the portal — but `WebFormController.initFormUI()` still
routes every body-part form to `WebListFormUI`, and `WebFormUI.java:266-299` /
`JSEventType.java:176-178` still carry list/table-view legacy-component branches. Two
readings are possible:

- **(a)** List/table-view is already effectively unsupported in TiNG and these forms are
  meant to degrade to a plain `WebFormUI` render (record-view-like) — in which case the
  portal path, `BodyPortal`, `WebListFormUI`, and the `isListView/isTableView` branches are
  all removable.
- **(b)** List/table-view is still a supported projection that *should* be re-pointed at
  `servoycore-listformcomponent` (which does exist in the Angular client) rather than the
  dead portal — a larger rework, not a pure deletion.

This is a scope decision, not an un-investigable ambiguity: the deletion is well-defined
and the symptom is single-rooted, so this is PROCEED, with the design question raised for
the implementer/author to confirm before the rework portion.

## Approaches considered

1. **Remove the dead portal generation path entirely (ticket's approach).**
   Delete `BodyPortal`, `createBodyPortalFormElement()`, the `WebListFormUI` portal
   construction, the `isListView/isTableView` `BodyPortal` additions in `FormWrapper`/
   `PartWrapper`, the `servoycore-portal` branches in `FormTemplateGenerator`/
   `FormLayoutGenerator`/`FormElement`, and the `servoy_ngclient/war/servoycore/portal/`
   assets; then route body-part forms to a plain `WebFormUI`.
   - *Pros:* removes genuinely dead code and the latent-null class of bugs; the null guard
     can then be reverted; smaller, cleaner server surface.
   - *Cons:* only correct if reading **(a)** is right — i.e. list/table-view really is
     meant to collapse to record-view in TiNG. If **(b)** is intended this silently drops
     list/table-view rendering. Needs author confirmation of intended behaviour. Touches
     several files across the `servoy-client` repo (note: the symptom code lives in
     `servoy-client`, not `servoy-eclipse`).

2. **Re-point list/table-view generation at `servoycore-listformcomponent`.**
   Keep the list/table-view concept but replace the dead `servoycore-portal` target with
   the component the Angular client actually ships.
   - *Pros:* preserves list/table-view functionality if that is still a requirement.
   - *Cons:* substantially larger; `listformcomponent` has a different model than the old
     portal; may be unnecessary if the product direction is to drop legacy list/table-view
     in TiNG. Contradicts the removal commit's "TiNG never used that" rationale.

3. **Keep only the null guard, do nothing else (status quo).**
   - *Pros:* zero risk now; NPE already fixed.
   - *Cons:* leaves dead code, dead assets, and a confusing error-bean render for
     list/table-view forms; the exact tech-debt the ticket is filed to remove. Explicitly
     rejected by the ticket.

4. **No code change needed.**
   - *Pros:* none beyond "no work."
   - *Cons:* incorrect — there is demonstrably dead code tied to a removed component, plus
     unused shipped assets. The ticket is a legitimate tech-debt cleanup (labels:
     `tech-debt`, `ai`). Not appropriate.

## Recommendation

**PROCEED with Approach 1** (remove the dead portal / list-view / table-view generation
path), **gated on the author confirming reading (a)** — that list/table-view forms are
intended to degrade to a plain `WebFormUI` record-style render in TiNG and are not meant to
be re-pointed at `servoycore-listformcomponent`. Given commit `e2c57e928`'s explicit "TiNG
never used that, has no impl for that at all," (a) is the strongly favoured reading and the
one the ticket's own scope assumes.

Concretely, Approach 1 means, in `servoy-client/servoy_ngclient`:
- Make `WebFormController.initFormUI()` stop routing body-part forms to `WebListFormUI`
  (collapse to `WebFormUI`), then remove `WebListFormUI` and `BodyPortal`.
- Remove `FormElementHelper.createBodyPortalFormElement()` and its `BodyPortal` branches in
  `getFormElement()`; once the null source is gone, revert the `27e965926` null guard (keep
  only if other paths can still legitimately produce null).
- Drop the `BodyPortal`/`isListView`/`isTableView` additions in
  `template/FormWrapper.java` and `template/PartWrapper.java`, and the `servoycore-portal`
  branches in `template/FormTemplateGenerator.java`, `template/FormLayoutGenerator.java`,
  and `FormElement.java`.
- Delete the orphaned `servoy_ngclient/war/servoycore/portal/` resources and the stale
  `client_tests/test/portal.test.js`.

Keep Approach 2 as the fallback **only** if the author says list/table-view must keep
rendering in TiNG.

**Repo note for the implementer:** the affected production code is in the **`servoy-client`**
repository (`servoy_ngclient`), not `servoy-eclipse`. Only incidental references
(`CreateComponentCommand.java`, the `ComponentTemplateGenerator` skip-list +
`ComponentTemplateGeneratorTest`) live in `servoy-eclipse`; the `servoycore-portal` skip in
`ComponentTemplateGenerator` can stay harmlessly or be cleaned up alongside.

## Git history findings

- `e2c57e928` (2026-06-10, Johan Compagner) — "remove the portal now from the core": removed
  the `portal/portal.spec` + `Web-Component: True` lines from
  `servoy_ngclient/war/servoycore/META-INF/MANIFEST.MF`. Message: *"TiNG never used that has
  no impl for that at all. And angular now breaks on that because of 'onXxx' events that are
  not implemented."* This is the point the component became unregistered while the server
  path stayed. **Not a change to revert** — removing the server path is the correct
  follow-through, not undoing this commit.
- `27e965926` (2026-10-06, Johan Compagner) — "Guard against null portal FormElement so
  list/table-view forms don't NPE [ai]": the immediate stop-gap. Added the `if
  (persistWrapper != null)` guard around `persistWrappers.putIfAbsent` plus the null-skip in
  `getFormElements(Iterator, context)` (`FormElementHelper.java:135-138`, `454-479`). This
  is the "null guard" the ticket says can be reconsidered once the dead path is gone.
- `BodyPortal.java` / `WebListFormUI.java` core logic dates to 2015-2016 (George Ganea);
  the generation projection predates TiNG by years, confirming it is legacy that TiNG never
  carried forward.
- No prior `docs/SVY-21548-*.spec.md` exists. No dependency/target-platform version bump is
  implicated — this is a deliberate component removal, not a regression from an upgrade.

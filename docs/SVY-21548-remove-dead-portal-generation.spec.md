# Spec: SVY-21548 — Remove dead servoycore-portal / BodyPortal list & table view generation

> **Approved approach (triage gate):** Approach 1 — remove the dead portal / list-view /
> table-view server-side generation path entirely and route body-part forms to a plain
> `WebFormUI` (list/table-view degrade to a record-view-style render in TiNG). Do **not**
> re-point at `servoycore-listformcomponent`. See `docs/SVY-21548-triage.md`.
>
> **Repo scope:** The bulk of this work is in the sibling **`servoy-client`** repository
> (`servoy_ngclient`), checked out at `C:\Users\vosti\git_master\servoy-client`. Only two
> incidental references live in this **`servoy-eclipse`** checkout
> (`CreateComponentCommand.java`, `ComponentTemplateGenerator` + its test). Both repos must
> be edited and built; keep the two commits separate (one per repo).

## 1. Goal

The `servoycore-portal` web component was removed from the shipped core in June 2026
(commit `e2c57e928`, "remove the portal now from the core" — *"TiNG never used that, has no
impl for that at all"*), but the server-side generation that targets it was left intact.
List-view and table-view forms still build a synthetic `BodyPortal` and ask
`FormElementHelper.createBodyPortalFormElement()` to produce a `"servoycore-portal"`
`FormElement`. Because the spec is no longer registered, that `FormElement` falls back to
the error bean, has no `childElements` property, and the method returns `null` — latent dead
code that surfaced as an NPE (`ConcurrentHashMap.putIfAbsent(formElement, null)`) once a
plain NG client E2E run hit the *cached* `getFormElement()` branch (stop-gap guard added in
`27e965926`). This change removes the entire dead portal / list-view / table-view
server-side generation path so that body-part forms route to a plain `WebFormUI` (a
record-view-style render in TiNG), deletes the orphaned `servoy_ngclient/war/servoycore/portal/`
assets and the stale `client_tests/test/portal.test.js`, and reverts the now-unnecessary
null guard.

## 2. Background

### 2.1 How the portal is unregistered but the server path lives on

The portal was unregistered by deleting only its manifest entry in
`servoy_ngclient/war/servoycore/META-INF/MANIFEST.MF`:

```
-Name: portal/portal.spec
-Web-Component: True
```

The current manifest registers `errorbean`, `navigator`, `slider`, `formcomponent`,
`defaultloadingindicator`, `formcontainer`, `listformcomponent`, `responsivecontainer` —
**no `portal`**. The spec file and all assets (`portal.spec`, `portal.js`,
`portal_server.js`, ui-grid bundles, etc.) are still physically present under
`servoy_ngclient/war/servoycore/portal/` but are dead. The live Angular TiNG client has no
`servoycore-portal` consumer (only the unrelated `servoycore-listformcomponent` exists).

### 2.2 The server generation chain (all in `servoy_ngclient`)

1. **`component/WebFormController.initFormUI()`** (lines 102–122) routes any non-record-view
   form that has a BODY part to `new WebListFormUI(this)`; record-view and body-less forms
   get a plain `WebFormUI`.
2. **`WebListFormUI.getFormElements()`** (lines 54–80) strips the body elements and appends
   `getPortal()` → `new BodyPortal(form)`, then turns the persists into `FormElement`s.
3. **`FormElementHelper.createBodyPortalFormElement()`** (lines 514–697) builds a
   `FormElement("servoycore-portal", …)`. The spec being unregistered means the error-bean
   fallback kicks in, `pd == null` at line 589, and it returns `null` (also line 696).
4. **`FormElementHelper.getFormElement()`** has `BodyPortal` branches in both the non-cached
   path (line 450) and the cached path (lines 462–477). The `27e965926` guard added the
   `if (persistWrapper != null)` wrapper around `putIfAbsent` here, plus a null-skip in
   `getFormElements(Iterator, context)` (lines 135–138).

### 2.3 Other `BodyPortal` / `servoycore-portal` producers

- **`template/FormWrapper.java`** — fields `isTableView`/`isListView` (lines 80–81, 102–103),
  and `getBaseComponents()` excludes body components then `components.add(new BodyPortal(form))`
  for list/table view (lines 178–195). `getBodyComponents()` (266–286) and the
  `isTableView` label-for skip at line 278 feed this.
- **`template/PartWrapper.java`** — `getBaseComponents(...)` returns a `BodyPortal` for a BODY
  part in TABLE/LIST view (lines 154–164).
- **`template/FormTemplateGenerator.getComponentTypeName()`** — maps **any** `instanceof Portal`
  persist to `"servoycore-portal"` (lines 195–198). See §3.5 for the real-Portal nuance.
- **`template/FormLayoutGenerator.java`** — a `servoycore-portal` list-view min-width style
  branch (lines 419–425) and `isNotSelectable()` (lines 666–669) keyed on the type name.
- **`FormElement.getForbiddenComponentNames()`** (lines 882–894) — special-cases
  `"servoycore-portal"`; called from `FormLayoutGenerator` (lines 457, 579).
- **`NGRuntimeWindow.getNavigatorProperties()`** (line 716) — `instanceof WebListFormUI`
  forces the default navigator to NONE for list forms.
- **`WebFormUI.contributeComponentToElementsScope()`** (lines 270–272) — a legacy-component
  branch gated on LIST/TABLE view and `servoydefault-` type names.

### 2.4 Incidental references in `servoy-eclipse`

- `com.servoy.eclipse.designer/.../CreateComponentCommand.java:403` — a
  `"servoycore-portal".equals(name)` drop handler (`createNewPortal`). Dead because the
  palette no longer offers the portal, but harmless; clean up alongside.
- `com.servoy.eclipse.ngclient.ui/.../ComponentTemplateGenerator.java:111-112` — the skip-list
  for `servoycore-portal` in template generation, and its test
  `ComponentTemplateGeneratorTest.java` (`PORTAL_SPEC`, line 161 assertion). Harmless, but
  the spec no longer ships, so the skip is moot; clean up alongside.

### 2.5 Git history (carried from triage)

- `e2c57e928` (2026-06-10, Johan Compagner) — unregistered the portal in the core manifest.
  **Not a change to revert**; removing the server path is the correct follow-through.
- `27e965926` (2026-10-06, Johan Compagner) — the null guard (`if (persistWrapper != null)`
  around `putIfAbsent` + the `getFormElements` null-skip). To be reverted once the null
  source (`createBodyPortalFormElement` → null) is gone.

## 3. Design

The portal / list-view / table-view server projection is removed wholesale. After removal,
`WebFormController.initFormUI()` creates a plain `WebFormUI` for every form (record,
body-less, list, and table), so list/table-view forms render their body elements in a
record-view-style layout in TiNG, consistent with the "TiNG never used the portal" rationale.

### 3.1 Collapse `WebFormController.initFormUI()` to `WebFormUI`

Replace the three-branch `if/else` with a single `formUI = new WebFormUI(this)`. The
body-less-form `reportJSWarning` can be dropped (it only distinguished a case that no longer
behaves differently) — confirm whether to keep the warning for user clarity; default is to
drop it since all forms now render the same way. Remove the `WebListFormUI` import.

### 3.2 Delete `WebListFormUI` and `BodyPortal`

Delete `WebListFormUI.java` and `BodyPortal.java` entirely. Fix the one remaining
`WebListFormUI` reference in `NGRuntimeWindow.getNavigatorProperties()` (line 716): with
`WebListFormUI` gone, the `instanceof WebListFormUI` guard that forced the default navigator
to NONE for list forms disappears. **Decision:** the special "no default navigator for list
forms" behaviour is part of the legacy list-view projection and is dropped — remove the
whole `if (… instanceof WebListFormUI …)` block so list/table forms get the normal navigator
treatment like any other `WebFormUI` form.

### 3.3 Remove `createBodyPortalFormElement()` and the cached/non-cached `BodyPortal` branches

In `FormElementHelper`:
- Delete `createBodyPortalFormElement(...)` (lines 514–697) and any now-unused private
  helpers it alone uses (e.g. `getRowHeight`, `fillsWidth` — **verify** each is not called
  elsewhere before deleting).
- Delete the `BodyPortal` branch in the non-cached path (line 450) and the cached path
  (lines 462–463).
- **Revert the `27e965926` null guard** now that the null source is gone: restore the direct
  `persistWrappers.putIfAbsent(formElement, persistWrapper)` (no `if (persistWrapper != null)`
  wrapper) and remove the `if (formElement != null)` null-skip in
  `getFormElements(Iterator, context)` (lines 135–138), including the SVY-21548 comments —
  **unless** code review finds another legitimate null-returning path through
  `getFormElement`. The `new FormElement(...)` constructor path does not return null, so with
  `createBodyPortalFormElement` gone the guard is dead. Keep the guard only if review proves
  otherwise, and note why.

### 3.4 Remove the `BodyPortal` / list-view / table-view branches in the template wrappers

- **`FormWrapper.java`** — remove the `isTableView`/`isListView` fields, their initialisation,
  the body-component exclusion (lines 178–181), and `components.add(new BodyPortal(form))`
  (192–195); remove the `BodyPortal` import. `getBaseComponents()` then simply adds all
  security-visible persists plus the default navigator. **Verify** whether `getBodyComponents()`
  and the `isTableView` label-for skip (line 278) are still referenced after the exclusion is
  gone; delete if orphaned.
- **`PartWrapper.java`** — remove the BODY-part TABLE/LIST switch that returns a `BodyPortal`
  (lines 154–164) and the `BodyPortal` import; the method falls through to the normal
  component collection for the body part.

### 3.5 Remove the `servoycore-portal` type-name mapping and its dependents

- **`FormTemplateGenerator.getComponentTypeName()`** — remove the `if (persist instanceof Portal)
  return "servoycore-portal";` branch (lines 195–198). **Nuance to confirm in review:** this
  branch maps *any* `Portal` persist (including a real user-placed portal) to the removed
  component. Since the component no longer ships in TiNG, a real `Portal` already renders as
  the error bean; dropping the branch lets it fall through to `FormElement.ERROR_BEAN` (line
  205) — same observable result, no regression versus today. Confirm there is no other TiNG
  rendering for real `Portal` persists (triage confirmed none).
- **`FormLayoutGenerator.java`** — remove the `servoycore-portal` list-view min-width style
  branch (419–425) and `isNotSelectable()` (666–669); update the caller at the
  `isNotSelectable` usage site accordingly (fold into `isSelectable`/remove the call).
- **`FormElement.getForbiddenComponentNames()`** — remove the `"servoycore-portal"`
  special-case (lines 887–892). If the method becomes an empty-list-only method, keep it
  returning an empty list (callers at `FormLayoutGenerator` lines 457, 579 still invoke it)
  unless review decides to simplify the callers too.

### 3.6 Legacy LIST/TABLE-view behaviour in `WebFormUI`

Review the LIST/TABLE-view legacy-component branch in
`WebFormUI.contributeComponentToElementsScope()` (lines 270–272) and the equivalent in
`JSEventType.java:176-178` (flagged by triage). These apply `RuntimeLegacyComponent`
behaviour to `servoydefault-` components on list/table forms. **Decision:** these are *not*
portal-specific — they drive runtime scripting compatibility (legacy element API) for
components placed on list/table forms, which still exist as plain `WebFormUI` forms after
this change. **Leave them in place** (out of scope — see §6) unless review shows they are
only reachable through the deleted portal path. The implementer must verify, not assume.

### 3.7 Delete orphaned assets and stale test

- Delete the directory `servoy_ngclient/war/servoycore/portal/` and all its contents
  (`portal.spec`, `portal.js`, `portal_server.js`, `portal.css`, `portal.html`, `portal.png`,
  `portal_doc.js`, and the `css/`, `js/` ui-grid bundles). The manifest already does not
  reference it, so no manifest edit is needed — **confirm** with a repo-wide search that
  nothing else references `war/servoycore/portal/`.
- Delete the stale `servoy_ngclient/client_tests/test/portal.test.js`.

### 3.8 Clean up `servoy-eclipse` incidental references

- `CreateComponentCommand.java` — remove the `"servoycore-portal".equals(name)` branch
  (line 403 and its block) and any now-unused imports (`Portal`, `createNewPortal`-only
  helpers). **Verify** no other palette/drop path depends on it.
- `ComponentTemplateGenerator.java` — remove `servoycore-portal` from the skip-list
  (lines 111–112), leaving `servoycore-formcomponent`/`servoycore-navigator`. Update
  `ComponentTemplateGeneratorTest.java` accordingly: drop `PORTAL_SPEC` and the
  `assertFalse(template.contains("<servoycore-portal"))` assertion (line 161) — the spec no
  longer exists so the test input is meaningless. **Keep** the SVY-21380 errorbean exemption
  tests untouched.

## 4. Implementation plan

> Two commits — one in `servoy-client`, one in `servoy-eclipse`. Both subject lines include
> `SVY-21548` and end with `[ai]`.

### Part A — `servoy-client` / `servoy_ngclient` (bulk)

1. `component/WebFormController.java` — collapse `initFormUI()` to always
   `new WebFormUI(this)`; remove the `WebListFormUI` import (§3.1).
2. Delete `WebListFormUI.java` and `BodyPortal.java` (§3.2).
3. `NGRuntimeWindow.java` — remove the `instanceof WebListFormUI` navigator-NONE block
   (line 716) and the now-unused import (§3.2).
4. `FormElementHelper.java` — delete `createBodyPortalFormElement()` and its sole-use
   private helpers; delete the `BodyPortal` branches at lines 450, 462–463; revert the
   `27e965926` null guard (lines 135–138, 470–477) back to the direct `putIfAbsent` and
   un-skipped `getFormElements` (§3.3).
5. `template/FormWrapper.java` — remove `isTableView`/`isListView` fields + init, the body
   exclusion, the `BodyPortal` add, the `BodyPortal` import, and (if orphaned)
   `getBodyComponents()` / the label-for skip (§3.4).
6. `template/PartWrapper.java` — remove the BODY-part TABLE/LIST `BodyPortal` switch and the
   `BodyPortal` import (§3.4).
7. `template/FormTemplateGenerator.java` — remove the `instanceof Portal → "servoycore-portal"`
   branch (§3.5).
8. `template/FormLayoutGenerator.java` — remove the `servoycore-portal` min-width style branch
   and `isNotSelectable()`; fix its caller (§3.5).
9. `FormElement.java` — remove the `"servoycore-portal"` case from `getForbiddenComponentNames()`
   (§3.5).
10. Delete `war/servoycore/portal/` (whole directory) and
    `client_tests/test/portal.test.js` (§3.7).
11. Review (do **not** remove unless proven portal-only) the LIST/TABLE legacy branches in
    `WebFormUI.contributeComponentToElementsScope()` and `JSEventType.java:176-178` (§3.6).
12. Build `servoy_ngclient` (and dependents) in Eclipse; run
    `getCompilationErrors` on every touched class; `organizeImports`; resolve all errors.

### Part B — `servoy-eclipse` (incidental)

13. `com.servoy.eclipse.designer/.../CreateComponentCommand.java` — remove the
    `"servoycore-portal"` drop branch and unused imports (§3.8).
14. `com.servoy.eclipse.ngclient.ui/.../ComponentTemplateGenerator.java` — drop
    `servoycore-portal` from the skip-list (§3.8).
15. `com.servoy.eclipse.ngclient.ui.tests/.../ComponentTemplateGeneratorTest.java` — remove
    `PORTAL_SPEC` + the `<servoycore-portal` assertion; keep the SVY-21380 tests (§3.8).
16. `getCompilationErrors` on the three touched `servoy-eclipse` classes; `organizeImports`;
    run `ComponentTemplateGeneratorTest` to confirm it still passes.

### Verification

17. In `servoy_ngclient`, run the client JUnit/plugin tests that touch form-element /
    template generation if present; at minimum compile-clean. Manually confirm (or add a
    regression test — see §5) that a list-view and a table-view form initialise through
    `WebFormController` without an NPE and render their body components via `WebFormUI`.
18. `grep` both repos for any residual `BodyPortal`, `WebListFormUI`, `servoycore-portal`,
    `war/servoycore/portal` after the edits — expect zero (apart from `docs/` history files).

## 5. Acceptance criteria

- [ ] `WebFormController.initFormUI()` creates a `WebFormUI` for every form view type; no
      `WebListFormUI` is instantiated anywhere.
- [ ] `BodyPortal.java` and `WebListFormUI.java` are deleted; no references remain in
      `servoy_ngclient`.
- [ ] `FormElementHelper.createBodyPortalFormElement()` and both its `BodyPortal` branches in
      `getFormElement()` are removed.
- [ ] The `27e965926` null guard is reverted (direct `putIfAbsent`, un-skipped
      `getFormElements`) — or, if a legitimate null path remains, the guard is kept with a
      documented reason and the SVY-21548 comment updated.
- [ ] The `isListView`/`isTableView` `BodyPortal` additions are removed from `FormWrapper` and
      the `BodyPortal` switch is removed from `PartWrapper`.
- [ ] The `servoycore-portal` branches are removed from `FormTemplateGenerator`,
      `FormLayoutGenerator`, and `FormElement.getForbiddenComponentNames()`.
- [ ] `war/servoycore/portal/` and `client_tests/test/portal.test.js` are deleted; the core
      manifest is unchanged (it already omits portal).
- [ ] The `servoy-eclipse` incidental references are cleaned up: `CreateComponentCommand`
      portal drop branch removed, `ComponentTemplateGenerator` skip-list entry removed, and
      `ComponentTemplateGeneratorTest` updated — the test suite still passes (including the
      SVY-21380 errorbean tests).
- [ ] A list-view form and a table-view form initialise and render in a plain NG client
      without the `ConcurrentHashMap.putIfAbsent(..., null)` NPE; their body components appear
      in a record-view-style render.
- [ ] Both `servoy_ngclient` and the touched `servoy-eclipse` plugins compile with no new
      errors; no residual `BodyPortal` / `WebListFormUI` / `servoycore-portal` /
      `war/servoycore/portal` references remain (outside `docs/`).

## 6. Out of scope

- Re-pointing list/table-view generation at `servoycore-listformcomponent` (explicitly
  rejected by the approved approach).
- Adding a new TiNG rendering/visual parity for the old list/table-view look beyond the plain
  `WebFormUI` record-style render.
- The LIST/TABLE-view legacy-scripting-compatibility branches in
  `WebFormUI.contributeComponentToElementsScope()` and `JSEventType.java` — reviewed but left
  in place unless proven portal-only (§3.6); any change there is a separate follow-up.
- Removing the `Portal` persistence type itself or the Smart-client/legacy portal rendering —
  only the TiNG `servoycore-portal` server projection is removed.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Keep or drop the "shown in record view because it has no body part" `reportJSWarning` in `initFormUI()` once all body-part forms render the same way? Default: drop. | author/reviewer | open |
| Is the `instanceof WebListFormUI` → default-navigator-NONE behaviour (NGRuntimeWindow:716) safe to drop, or must list/table forms keep suppressing the default navigator? Default: drop. | author/reviewer | open |
| Are `WebFormUI.contributeComponentToElementsScope()` (270–272) and `JSEventType.java:176-178` reachable only via the deleted portal path, or do they still serve list/table `WebFormUI` forms? If portal-only, remove them too. | implementer (verify) | open |
| Can the `27e965926` null guard be fully reverted, or does another `getFormElement` path legitimately return null? | implementer (verify) | open |
| After dropping the `instanceof Portal → "servoycore-portal"` mapping, is a real user-placed `Portal` persist's error-bean fallback acceptable (no TiNG regression vs today)? Triage says yes. | reviewer confirm | open |

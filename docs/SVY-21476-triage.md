# Triage Report — SVY-21476 (focused re-triage with reproduction forms)

**Verdict:** PROCEED

## Reported problem

Five symptoms were reported for the responsive form `projViewEpic` (deep inheritance chain). The ticket lists symptoms but proposes **no concrete solution** — it is a bug report, not a design.

| # | Symptom (observed) | Implied "fix" in ticket |
|---|--------------------|--------------------------|
| 1 | Outline does not show the `[extended formName]` inherited-form label; extended tabs appear empty | none proposed |
| 2 | Extended tabs / inherited elements not rendered correctly | none proposed |
| 3 | Keyboard **Delete** silently fails on the component; mouse / outline Delete "works" | none proposed |
| 4 | Deletions on the extended form appear to succeed in the UI (parent even reflects the change) but do **not** persist to the `.frm`; elements return after restart | none proposed |
| 5 | Copy/paste recovery produced wrong result (panel replaced by old-panel-plus-pasted-tab) | none proposed |

## Reproduction artifacts (ground truth)

Inheritance chain (root → leaf), all responsive:

```
abstractCCC     A70F6520-6BA5-4328-876C-2F3140A0F7EE   (root)
 └ baseNav      316AA798-8DD3-48EB-84A4-A48C3AEFC3D2
   └ projHeaderTicket 098A263E-AA86-46FF-AB83-E0152BE3FFA5
     └ projViewTicket D060265E-67F6-420D-9B07-7CA0969271F0  (large, responsive)
       └ projViewEpic A8BA5A9A-C4F2-4164-8261-9A7B7C62351A  ← PROBLEM FORM
```

`projViewEpic.frm` contains **4 override persists placed as direct top-level `items` of the form root**, each carrying only an `extendsID` plus a few overridden properties:

1. `226A1DB4-…` typeid 46 (layout container) → `extendsID B9662D52-…`
2. `302D125B-…` typeid 47 (label "Edit epic") → `extendsID DE45CEBF-…`
3. `C0438EAB-…` typeid 46 (layout container) → `extendsID C8296EA1-…`
4. `E5D0FF20-…` typeid 47 (breadcrumbs) → `extendsID CEA1BB7E-…`

All 4 `extendsID` targets **resolve** (none dangling): `B9662D52`, `DE45CEBF`, `C8296EA1` are defined **deeply nested (~12–14 levels)** inside responsive 12-grid containers in `projViewTicket.frm`; `CEA1BB7E` (breadcrumbs) lives in `baseNav.frm` under nav → col-md-12.

**Anomaly:** in the parent chain these elements live many levels deep; in `projViewEpic` their overrides sit **flat at the form root**, not nested under an override of their parent container.

## Root-cause assessment

### Is a flat-at-root override with a deep `extendsID` legal? — YES, it is a supported serialization.

The flattening layer re-parents an override purely by following its `extendsID`; it never assumes the override's own on-disk parent mirrors the inherited element's parent. Evidence, from source I read:

- `FlattenedForm.fill()` (`servoy_shared/src/com/servoy/j2db/persistence/FlattenedForm.java:133-283`) builds `extendsMap` keyed by **the super-persist's UUID → the override**:
  `IPersist p = PersistHelper.getSuperPersist(persist); … extendsMap.put(p.getUUID(), persist);`
  Then, for a responsive override whose super's parent is **not** the form, it walks up to the top ancestor and does `if (!(topPersist.getParent() instanceof Form)) continue;` — i.e. it **deliberately skips placing the override at form level**, delegating placement to the nested container.
- `FlattenedLayoutContainer.fill()` (`…/FlattenedLayoutContainer.java:33-65`) then, for each hierarchy child, calls `getOverridePersist(child, extendsMap)` (`:67-74`) which swaps the inherited child for its override when `extendsMap.containsKey(child.getUUID())`. This is exactly what re-parents a flat-at-root override into its correct deep position. Note the comment there: *"not all overrides are on form level so extendsMap may be incomplete — add overrides from current container to the map"*.
- This branch was introduced intentionally by commit **`34629f1ff` "SVY-13405 Double inheritance of UI in responsive forms breaks form"** (servoy-client), which added the `topPersist` walk + `!(topPersist.getParent() instanceof Form) continue` skip. So flat-at-root overrides for deeply-inherited responsive elements are a **known, intended** shape.

**Conclusion:** The serialization in `projViewEpic.frm` is legal. The runtime/flattening layer resolves it correctly. The defects are in the **editor operations**, which assume the override's own parent mirrors the inherited element's parent (or treat the override as a plain inherited element).

### Symptom → code-path mapping

- **Symptom 3 (keyboard Delete silently fails) — SHARES the root cause; this is the single most crisp, fixable defect.**
  `KeyPressedHandler.executeMethod()` (case 46, `…/editor/rfb/actions/handlers/KeyPressedHandler.java:75-225`) guards with `if (selection.size() > 0 && !containsInheritedElements(selection))`. `containsInheritedElements()` (`:65-73`) calls `Utils.isInheritedFormElement(persist, form)`. For an override persist, `Utils.isInheritedFormElement` (`servoy_shared/…/util/Utils.java:165-193`) returns `true` because `PersistHelper.isOverrideElement()` (`PersistHelper.java:1018-1026`) is true (it has an `extendsID` + valid UUID). So the whole selection is judged "inherited" and **the delete is dropped** with no feedback. The mouse/outline path (`DeleteAction.createDeleteCommand` → `FormElementDeleteCommand`, `…/editor/rfb/actions/DeleteAction.java:76-108`) has **no such guard**, which is why mouse/outline Delete "works". → Root cause: the keyboard guard conflates *override persist* with *pure inherited (read-only) persist*.

- **Symptom 4 (delete not persisting; returns after restart) — SHARES the root cause.**
  `FormElementDeleteCommand.redo()` (`…/editor/commands/FormElementDeleteCommand.java:309-339`) deletes the persist that is selected in the editor and then saves. In this form the editor works on the **flattened** tree, where the deep override has been re-parented into a nested `FlattenedLayoutContainer`. The delete removes the child from the *flattened* container / super element (so the UI, and even a parent redraw, reflect the change), but the object actually written to `projViewEpic.frm` is the **flat-at-root override persist**, whose parent is the form root — the delete never removes/rewrites that root-level override, so on reload `FlattenedForm.fill()` re-materialises it. Same underlying mismatch (flattened re-parented view vs. real root-level override persist).

- **Symptoms 1 & 2 (outline missing `[extended form]` label / extended tabs empty / not rendered) — SHARE the root cause.**
  `FormOutlineContentProvider.getChildrenNonGrouped()` (`…/outline/FormOutlineContentProvider.java:93-167`) enumerates `flattenedForm.getAllObjectsAsList()` and builds children by real `persist.getParent()` (`getParentNonGrouped`, `:254-287`: `if (persist.getParent() == form) return ELEMENTS`). Because the deep overrides are re-parented in the flattened tree but their **real** parent is the form root, the outline's parent/child bookkeeping is inconsistent for exactly these persists — the nested override appears detached, so the inherited-form grouping/label and the tab contents come out empty/wrong. Same mismatch between the re-parented flattened node and the root-level persist's real parent.

- **Symptom 5 (copy/paste recovery wrong) — LIKELY an independent side effect, not the primary target.**
  This is a user recovery workaround, not a core operation; its wrong result is a *consequence* of the form already being in the flat-override state plus paste-into-container logic. It should resolve or become moot once the primary delete/flatten mismatch is fixed, but it is not the crisp root-cause path and should not drive the fix.

**Single root cause:** Editor operations treat a **flat-at-root override persist whose `extendsID` targets a deeply-nested inherited element** as if it were a plain inherited (read-only) element and/or assume its own parent mirrors the inherited element's parent. The flattening layer (correctly) re-parents it by `extendsID`, but the delete guard (`KeyPressedHandler.containsInheritedElements` / `Utils.isInheritedFormElement`) and the delete-persist selection (`FormElementDeleteCommand`) do not account for the divergence between the override's real root parent and its flattened deep position.

## Ticket premise check

The ticket proposed no solution — it only enumerated symptoms. That premise is fine: there is nothing to refute. The reframing risk flagged in the first pass (five unrelated bugs) is resolved by the reproduction forms: symptoms 1–4 collapse to one root cause; symptom 5 is a downstream recovery artifact.

## Approaches considered

1. **Fix the keyboard-delete guard to distinguish override persists from pure inherited elements** (primary).
   - Change `KeyPressedHandler.containsInheritedElements` (and/or the shared `DesignerUtil.containsInheritedElement`) so an **override persist** (`PersistHelper.isOverrideElement` true, i.e. has its own `extendsID` in the current form's hierarchy and is thus editable/deletable) is **not** blocked, matching the mouse/outline path.
   - Pros: smallest, targeted; directly fixes symptom 3; aligns keyboard and mouse behaviour; low blast radius.
   - Cons: alone it does not fix the persistence mismatch (symptom 4) — the deletion still needs to remove the real root-level override; must be paired with the delete-target fix.

2. **Fix `FormElementDeleteCommand` to resolve the real (root-level) override persist before `deleteObject`** so deletions on re-parented flat overrides are written to the `.frm`.
   - Pros: fixes symptom 4 (and by extension 1/2 once the stale root override is gone); addresses the true root cause where it manifests destructively.
   - Cons: touches shared delete logic used by all forms; needs careful handling of override vs. base persist and of the "override the whole inherited subtree" semantics; higher risk — must be test-guarded (`com.servoy.eclipse.designer.tests` / `com.servoy.eclipse.ui.tests`).

3. **Normalise the serialization: rewrite flat-at-root deep overrides into nested override containers on save** (defensive migration).
   - Pros: makes on-disk shape match the nested inherited structure, so *all* editor bookkeeping that assumes parent-mirroring becomes correct at once.
   - Cons: contradicts the intentional `SVY-13405` design that expects flat-at-root overrides to be legal and re-parented by the flattener; risks regressing double-inheritance responsive forms; largest blast radius. Not recommended.

4. **No code change.**
   - Pros: zero risk of regressing responsive double-inheritance.
   - Cons: leaves a data-loss-class bug (deletions silently not persisted, elements resurrect on restart) and broken outline on a real customer form. Not acceptable.

## Recommendation

**PROCEED** with **Approach 1 + Approach 2 together**, treating them as one fix for one root cause.

- **Single root cause:** editor delete/outline logic conflates a *flat-at-root override persist with a deep `extendsID`* with a *pure inherited read-only element*, and does not reconcile the override's real (form-root) parent with its flattened deep position.
- **Exact files/methods to change:**
  - `com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/editor/rfb/actions/handlers/KeyPressedHandler.java` → `containsInheritedElements(List<IPersist>)` (lines 65-73): stop blocking override persists so keyboard Delete matches the mouse/outline path.
  - `com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/editor/commands/FormElementDeleteCommand.java` → `redo()` (lines 309-339) and/or `execute()` (227-307): resolve and delete the **real root-level override persist** (not just the flattened re-parented node) so the change is written to the `.frm` and does not resurrect on reload.
  - Shared helper to reuse for the "is this truly read-only inherited vs. an editable override" decision: `com.servoy.j2db.util.Utils.isInheritedFormElement` (Utils.java:165-193) / `PersistHelper.isOverrideElement` (PersistHelper.java:1018-1026) — do not change their semantics; call them correctly from the guard.
- **Do NOT change** the flattening layer (`FlattenedForm.fill`, `FlattenedLayoutContainer.fill/getOverridePersist`) — it is correct and intentional (`SVY-13405`). Approach 3 is explicitly rejected to avoid reverting that design.
- Outline symptoms (1/2) should be re-verified after the delete fix; if the empty-tab / missing-`[extended form]`-label persists, the follow-up is in `FormOutlineContentProvider.getParentNonGrouped` (:254-287) parent resolution for re-parented overrides — same root cause, adjacent method.
- Symptom 5 (copy/paste) is a downstream recovery artifact; verify it is resolved by the primary fix rather than chasing it separately.

## Git history findings

- **`34629f1ff` "SVY-13405 Double inheritance of UI in responsive forms breaks form"** (servoy-client, `FlattenedForm.java`) introduced the responsive `topPersist` walk and the `if (!(topPersist.getParent() instanceof Form)) continue;` skip — establishing that **flat-at-root overrides re-parented by `extendsID` are intended**. A fix must therefore live in the editor operations, **not** by "correcting" the serialization; changing the flattener would revert this deliberate decision.
- **`702fb787a` "SVY-13405 …"** also touched `FlattenedLayoutContainer.fill()` to top up `extendsMap` from the current container ("*not all overrides are on form level*") — same design intent, reinforcing that deep overrides need not be nested on disk.
- `KeyPressedHandler` history (SVY-17615, SVY-15645, SVY-15066) shows the delete case-46 guard predates responsive deep-inheritance concerns; the inherited-element guard was never revisited for override-vs-inherited nuance — consistent with this being a genuine gap rather than an intentional constraint.
- `FormElementDeleteCommand` / `ElementUtil.getOverridePersist` last meaningfully touched by **`SVY-20784` refactor the flattened stuff for WebCustomType** and the reverted **SVY-20749** work — no commit specifically targets deleting a deep-inherited responsive override, confirming this path is untested for the reproduction scenario.

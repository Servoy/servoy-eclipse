# Spec: SVY-21476 — Keyboard delete of flat-at-root override persists on responsive extended forms

## 1. Goal

Make the keyboard **Delete** key behave the same as the mouse/outline Delete on a responsive extended form, and make either deletion actually persist to the `.frm`. Today, on a form that carries *override persists* placed flat at the form root but whose `extendsID` targets a deeply-nested inherited element, keyboard Delete silently does nothing, and even when the deletion appears to work in the UI the override is re-materialised on reload. This is a data-loss-class defect on a real customer form (`projViewEpic`): the developer thinks elements are gone, they come back after restart. The fix distinguishes an editable *override persist* from a truly read-only *inherited element* in the keyboard-delete guard, and ensures the delete targets the real root-level override persist so the change is written to disk.

## 2. Background

### 2.1 The inheritance chain and the reproduction form

The reporter supplied the actual forms (`C:\Users\jcomp\Downloads\sample frm\sample frm\`). Chain, root → leaf, all responsive:

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

All 4 `extendsID` targets **resolve** (none dangling): `B9662D52`, `DE45CEBF`, `C8296EA1` are defined **deeply nested (~12–14 levels)** inside responsive 12-grid containers in `projViewTicket.frm`; `CEA1BB7E` (breadcrumbs) lives in `baseNav.frm`. In the parent chain these elements live many levels deep; in `projViewEpic` their overrides sit **flat at the form root**.

### 2.2 The flat-at-root override is a legal, intentional serialization (SVY-13405)

The flattening layer re-parents an override purely by following its `extendsID`; it never assumes the override's own on-disk parent mirrors the inherited element's parent.

- `FlattenedForm.fill()` builds `extendsMap` keyed by the super-persist's UUID → the override, then for a responsive override whose super's parent is not the form walks up to the top ancestor and does `if (!(topPersist.getParent() instanceof Form)) continue;` — deliberately skipping placement at form level and delegating placement to the nested container.
- `FlattenedLayoutContainer.fill()` calls `getOverridePersist(child, extendsMap)` which swaps the inherited child for its override when `extendsMap.containsKey(child.getUUID())` — this re-parents a flat-at-root override into its correct deep position. Comment there: *"not all overrides are on form level so extendsMap may be incomplete."*
- Introduced intentionally by commit **`34629f1ff` "SVY-13405 Double inheritance of UI in responsive forms breaks form"** (servoy-client). Reinforced by **`702fb787a`** which topped up `extendsMap` from the current container.

**The serialization is legal and the runtime/flattening layer resolves it correctly.** The defects are in the **editor operations**.

### 2.3 The two delete paths and the exact defect

**Keyboard Delete (broken):** `KeyPressedHandler.executeMethod()` case 46 (`com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/editor/rfb/actions/handlers/KeyPressedHandler.java:141-190`) guards the delete with:

```java
if (selection.size() > 0 && !containsInheritedElements(selection))
{
    editorPart.getCommandStack().execute(new FormElementDeleteCommand(...));
    ...
}
```

`containsInheritedElements()` (`KeyPressedHandler.java:65-73`) calls `Utils.isInheritedFormElement(iPersist, form)`. For a flat-at-root **override persist**, `Utils.isInheritedFormElement` (`servoy_shared/src/com/servoy/j2db/util/Utils.java:165-193`) returns `true` because its last line is `return PersistHelper.isOverrideElement(element);`, and `PersistHelper.isOverrideElement` (`servoy-client/servoy_shared/.../util/PersistHelper.java:1018-1026`) returns `true` when the element has an `extendsID` property with a valid UUID. So the whole selection is judged "inherited", the guard fails, and the delete is **silently dropped** with no feedback.

**Mouse / outline Delete (works):** `DeleteAction.createDeleteCommand()` (`com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/editor/rfb/actions/DeleteAction.java:76-108`) builds the same `FormElementDeleteCommand` **with no inherited-element guard** — that is precisely why the mouse/outline path "works" for these override persists.

**Persistence defect:** `FormElementDeleteCommand`'s constructor (`:104-132`) unwraps `IFlattenedPersistWrapper` to its wrapped persist, then `redo()` (`:309-339`) calls `deleteObject(child)` on the persist and saves. For a flat-at-root override, the persist selected in the flattened editor is the override itself (its real parent is the form root), so deleting it *should* rewrite the `.frm`. However the reporter observes deletions that "appear to succeed … but do not persist … return after restart". This indicates the object actually removed in some scenarios is the **flattened re-parented node / super element**, not the real root-level override persist — so `FlattenedForm.fill()` re-materialises it on reload. The delete must resolve and remove the **real root-level override persist**.

## 3. Design

### 3.1 Keyboard delete guard fix (KeyPressedHandler)

The guard must **not** block an *override persist* (which is editable / deletable — the developer explicitly created it on the extended form), only a truly-inherited read-only element (a plain child of a super-form that has *not* been overridden in the current form).

`Utils.isInheritedFormElement` conflates the two cases: for a super-form child that is *not* an override it returns `true` (via the `getAncestor(IRepository.FORMS) != context` check — correct, that element is read-only), **and** for an override persist it *also* returns `true` (via `PersistHelper.isOverrideElement` — wrong for the delete-guard purpose, because an override is editable).

**Do not change `Utils.isInheritedFormElement` or `PersistHelper.isOverrideElement` semantics** (they are used broadly and correctly elsewhere). Instead, refine the guard in `KeyPressedHandler.containsInheritedElements(List<IPersist>)` so that an element which is an *override persist in the current form* is treated as deletable (not "inherited"):

- For each selected persist, block **only** when the element is inherited/read-only **and not** an override persist that lives (as an override) in the current form. Concretely: `Utils.isInheritedFormElement(iPersist, form) && !PersistHelper.isOverrideElement(iPersist)` → treat as blocking-inherited. When `PersistHelper.isOverrideElement(iPersist)` is `true` (has its own `extendsID` + valid UUID, i.e. an editable override that the current form owns), it is **not** blocked.

This makes keyboard Delete match the mouse/outline path (which has no guard at all) while still blocking Delete on genuinely read-only inherited elements that were never overridden.

Note: the parallel shared helper `DesignerUtil.containsInheritedElement(List)` (`com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/util/DesignerUtil.java:132-169`) has the same `Utils.isInheritedFormElement` conflation. It is **not** on the keyboard-delete path (it guards other operations). Do **not** change it as part of this fix unless a regression surfaces — keep the blast radius to the keyboard-delete guard.

### 3.2 Delete-target fix (FormElementDeleteCommand — resolve the real root-level override)

`FormElementDeleteCommand` must delete the **real root-level override persist** as it exists on `projViewEpic.frm`, not a flattened re-parented node or the inherited super element.

- The constructor already unwraps `IFlattenedPersistWrapper` (`:118-121`). Extend the resolution so that, for a selected persist that is an override (`PersistHelper.isOverrideElement`), the command operates on the actual override persist owned by the editing form's real (non-flattened) persist tree — i.e. the persist whose `getParent()` is the form root in `projViewEpic.frm` — rather than any node produced by the flattening re-parenting (`FlattenedLayoutContainer.getOverridePersist`).
- In `redo()` (`:309-339`), `deleteObject(children[i])` and the subsequent `saveEditingSolutionNodes(...)` must act on that real root-level override persist so the change is written to the `.frm`. Verify `parents[i] = children[i].getParent()` resolves to the real form root (not a flattened container) for these overrides; if the selected persist is a flattened wrapper/re-parented node, resolve back to the underlying real override before `deleteObject`.
- `undo()` (`:341-356`) restores via `undeleteObject(parents[i], children[i], childIndexes[i])`; ensure `parents[i]` captured in `redo()` is the real root parent so undo re-inserts at the correct place.
- Preserve the existing override-in-subforms confirmation flow (`getOverridingPersists` / `ConfirmDeleteDialog` in `execute()`, `:227-307`) and the `WebCustomType` guard (`:77-79`); only change **which** persist is resolved as the delete target for flat-at-root responsive overrides.

Constraint: do not alter `ElementUtil.getOverridePersist` semantics (`com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/util/ElementUtil.java:324`) — reuse the existing helpers (`PersistHelper.getBasePersist`, `PersistHelper.getSuperPersist`, `PersistHelper.isOverrideElement`) to resolve, do not rewrite override-creation logic.

### 3.3 Outline label / empty-tabs (conditional secondary — FormOutlineContentProvider)

Symptoms 1 & 2 (outline missing the `[extended formName]` inherited-form label; extended tabs appear empty) share the same root cause: `FormOutlineContentProvider.getParentNonGrouped()` (`com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/outline/FormOutlineContentProvider.java:254-287`) resolves a child's parent by its **real** `persist.getParent()` (`if (persist.getParent() == form) return ELEMENTS;` at `:267`), but a flat-at-root override is re-parented deep in the flattened tree — so the outline's parent/child bookkeeping is inconsistent for exactly these persists and the nested override appears detached.

**Re-verify symptoms 1 & 2 after the delete fixes (3.1 + 3.2).** They may resolve once the stale flat-at-root overrides can be removed. **Only if they persist**, apply a follow-up in `getParentNonGrouped` so that a re-parented override resolves its parent to its **flattened** position (via `getFlattenedWhenForm(persist.getParent())` / the override's re-parented container) rather than assuming the real form-root parent. Treat this as conditional/secondary — do not change it pre-emptively.

### 3.4 Git history / constraints

- **`34629f1ff` (SVY-13405)** established flat-at-root responsive overrides as intentional; **`702fb787a`** reinforced it. The flattening layer (`FlattenedForm.fill`, `FlattenedLayoutContainer.fill` / `getOverridePersist`) is **off-limits** — fixing this by "normalising" the serialization would revert that design and regress double-inheritance responsive forms.
- `KeyPressedHandler` case-46 guard history (SVY-17615, SVY-15645, SVY-15066) predates responsive deep-inheritance; the inherited-vs-override nuance was never added — this is a genuine gap, not an intentional constraint.
- `FormElementDeleteCommand` / `ElementUtil.getOverridePersist` last meaningfully touched by **SVY-20784** (flattened stuff for WebCustomType); no commit targets deleting a deep-inherited responsive override — this path is untested for the reproduction scenario.

## 4. Implementation plan

Ordered, concrete changes:

1. **`com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/editor/rfb/actions/handlers/KeyPressedHandler.java`** → `containsInheritedElements(List<IPersist>)` (`:65-73`): change the per-persist test from `Utils.isInheritedFormElement(iPersist, form)` to block only when the element is inherited **and not** an override persist — i.e. `Utils.isInheritedFormElement(iPersist, form) && !PersistHelper.isOverrideElement(iPersist)`. Add the `PersistHelper` import if not present.
2. **`com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/editor/commands/FormElementDeleteCommand.java`** → constructor (`:104-132`) and/or `redo()` (`:309-339`): resolve the selected persist to the **real root-level override persist** (unwrap flattened wrappers, and for an override resolve to the persist owned by the editing form's real persist tree) so `deleteObject` + `saveEditingSolutionNodes` rewrite the `.frm`. Ensure `parents[i]`/`childIndexes[i]` captured for `undo()` reference the real root parent. Reuse `PersistHelper.isOverrideElement` / `PersistHelper.getBasePersist` / `PersistHelper.getSuperPersist`; do not change their semantics or the flattening layer. Preserve the existing subform-override confirmation flow and `WebCustomType` guard.
3. **Verify** symptoms 1 & 2 with the reproduction form. **Only if still broken**, apply the conditional secondary fix in **`com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/outline/FormOutlineContentProvider.java`** → `getParentNonGrouped()` (`:254-287`) to resolve re-parented overrides to their flattened parent.
4. After edits: organize imports, format, `eclipse-ide_getCompilationErrors`, fix any SpotBugs of the two highest severities in the changed code.
5. Add/extend tests (see §5) in `com.servoy.eclipse.designer.tests` and/or `com.servoy.eclipse.ui.tests`.

## 5. Acceptance criteria

Testable against the reproduction form (`projViewEpic`, the 5-level responsive chain with 4 flat-at-root overrides — 2 layout containers typeid 46, a "Edit epic" label, a breadcrumbs label — all `extendsID` targets resolving deep in the parent chain):

- [ ] **Keyboard Delete removes a flat-at-root override persist** on `projViewEpic` (e.g. select the "Edit epic" label override `302D125B-…` and press Delete) — the element is removed, matching the mouse/outline Delete result. Before the fix, keyboard Delete does nothing.
- [ ] **`containsInheritedElements` unit behaviour:** for a selection containing a persist for which `PersistHelper.isOverrideElement` is `true`, `KeyPressedHandler.containsInheritedElements` returns `false` (not blocking); for a genuinely inherited, non-overridden super-form child it still returns `true` (still blocking). Cover both keyboard-delete guard branches.
- [ ] **The deletion persists to the `.frm`:** after deleting a flat-at-root override (via keyboard or mouse) and saving, `projViewEpic.frm` no longer contains the override persist (its top-level `items` entry with that `extendsID` is gone), and after a form reload / editor reopen the element does **not** return.
- [ ] **Undo restores** the deleted flat-at-root override at its original root-level position with the correct index.
- [ ] **No regression on truly-inherited elements:** keyboard Delete on a plain inherited (never-overridden) element from a super-form remains blocked (no delete), exactly as before.
- [ ] **Delete on a normal (non-inherited) element** on a plain form is unaffected — keyboard and mouse Delete both work as before.
- [ ] **Symptoms 1 & 2 re-verified:** after the delete fixes, the outline shows the `[extended formName]` inherited-form label and extended tabs render their inherited content on `projViewEpic`. If they still fail, the §3.3 secondary fix is applied and this criterion is met with it.
- [ ] **Flattening layer untouched:** double-inheritance responsive forms (SVY-13405 scenario) still flatten/render correctly — no change to `FlattenedForm` / `FlattenedLayoutContainer`.

## 6. Out of scope

- Any change to the flattening layer (`FlattenedForm.fill`, `FlattenedLayoutContainer.fill` / `getOverridePersist`) — intentional per SVY-13405.
- Any change to the semantics of `Utils.isInheritedFormElement` or `PersistHelper.isOverrideElement` (they must be called correctly, not redefined).
- Any change to `DesignerUtil.containsInheritedElement` unless a regression surfaces (it is not on the keyboard-delete path).
- **Symptom 5** (copy/paste recovery producing old-panel-plus-pasted-tab) as a separate fix — it is a downstream recovery artifact of the form being stuck in the flat-override state. Verify it is resolved / moot once the primary delete fix lands; do not chase it independently.
- Migrating / normalising the on-disk serialization of flat-at-root overrides.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Does `FormElementDeleteCommand.redo()` already delete the real root override for these forms, or does it in some scenarios remove the flattened/super node (needs confirmation with a save-and-reload test on `projViewEpic`)? | Coder | Open — resolve by test in §5 (persistence criterion) |
| Are symptoms 1 & 2 fully resolved by the delete fix alone, or is the §3.3 `FormOutlineContentProvider.getParentNonGrouped` follow-up required? | Coder | Open — resolve by re-verification in §5 |
| Should the refined guard live in `KeyPressedHandler.containsInheritedElements` only, or be extracted to a shared helper for reuse by future callers? | Reviewer | Open — default: keep local to minimise blast radius |

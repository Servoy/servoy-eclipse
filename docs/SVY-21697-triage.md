# SVY-21697 Triage: Deleted component reappears after adding a calendar field

## Verdict

**PROCEED.** The root cause is a gap in the SVY-21249 own-file guard in `ServoyModel.handleChangedFiles`. A captured live stack trace confirms it. Recommended fix: extend the guard from composites to composite items (persists serialized inside their parent's `.frm`/`.tbl`/`.rel`/`.menu`), in `com.servoy.eclipse.core` only.

## Reported problem

From the Jira description of SVY-21697 (no comments on the issue):

1. Open a form in the form editor and add any component.
2. Delete the sidenav component with the context menu. Do not save.
3. Add a calendar and assign it a form variable created with the Data Provider dialog's "Create" button.
4. When the dataprovider is assigned, the deleted sidenav reappears on the form.

The bug is not specific to sidenav or calendar. The trigger is any action that writes the form's `.js` to disk while the form editor has unsaved structural changes. Here that action is `NewVariableAction.createNewVariable`.

## Root-cause assessment

### The code path that re-adds the component (captured, authoritative)

```
AbstractBase.internalAddChild:610            this = EDITING form 'abc', obj = sidenav_2
AbstractRepository.copyPersistIntoSolution:304   no editing copy -> createObject + addChild
ServoyProject$1.visit:321
ServoyProject.copyNodeToEditingSolution:303
ServoyProject.updateEditingPersist:524
ServoyModel$22.visit:2706                    handleChangedFiles visitor, ELSE branch
ServoyModel.handleChangedFiles:2678          solution.acceptVisitor over the REAL solution
ServoyModel.handleChangedFilesInSolutionProject:2476
... resource change broadcast ...
WorkspaceFileAccess.setContents:76
NewVariableAction.createNewVariable:301      writes forms/abc.js
DataProviderDialog.createVarButtonPressed:174
```

Frame values at `ServoyModel$22.visit`: `persist = sidenav_2` (WebComponent, parent `abc`), `persist.isChanged() == true`, `compositeWithItems == false`, `ownFileChanged == true`.

### Step by step

1. `FormElementDeleteCommand` removes `sidenav_2` from the **editing** form only. The **real** solution and `abc.frm` on disk still contain it.
2. `NewVariableAction` writes `forms/abc.js`. That is the only changed file.
3. `handleChangedFilesInSolutionProject` calls `handleChangedFiles`, which calls `SolutionDeserializer.updateSolution(..., readAll=false, useFilesForDirtyMark=false, shouldReset=true, testExisting=true, ...)` (ServoyModel.java:2642).
4. In `readObjectFilesFromSolutionDir` (SolutionDeserializer.java:407-408), `abc.frm` is not changed. It is still read because `hasRelatedEntries(abc.frm, changedFiles)` returns true when `abc.js` changed (SolutionDeserializer.java:870-877). The form JSON gets `changed=false`. Its items, including `sidenav_2`, are deserialized again into the existing **real** persists.
5. The real `Form abc` is flagged changed, because a new `ScriptVariable` child came from `abc.js`. The real `sidenav_2` is also flagged changed (see the next section).
6. The visitor in `handleChangedFiles` (ServoyModel.java:2678-2732) runs:
   - **Form `abc`:** `compositeWithItems == true` and `isOwnCompositeFileChanged == false`, so it takes the SVY-21249 skip branch. The editing form is not overwritten, which is correct. The visitor then returns `CONTINUE_TRAVERSAL` (Form is `isCompositeWithIndependentSerializationOfSubItems`), so it descends into the form's children.
   - **Child `sidenav_2`:** `isChanged() == true` and `compositeWithItems == false`. Line 2693 sets `ownFileChanged = !compositeWithItems || ...`, which is forced to `true`. That sends the item to the else branch, `servoyProject.updateEditingPersist(sidenav_2, false)`.
7. `updateEditingPersist` finds the item in the real solution but not in the editing solution, because it was deleted there. `copyNodeToEditingSolution` → `copyPersistIntoSolution` → `createObject` + `addChild` puts it back on the editing form. `copyNodeToEditingSolution` also calls `editingSolution.clearEditingState(dest)` (ServoyProject.java:343). That removes `sidenav_2` from the editing solution's `removedObjects`, which explains why `getRegisteredRemovedObjects()` was empty afterwards. The pending delete is lost completely, so a later Save would not delete it either.
8. The RFB designer renders the editing form, which now contains `sidenav_2` again.

### Why the real `sidenav_2` is flagged changed when only `abc.js` changed

It is **not** the `changed` JSON attribute or `handleChanged`:

- `handleChangedFiles` passes `useFilesForDirtyMark=false`, so `handleChanged` (SolutionDeserializer.java:2016-2031, the only code that propagates `CHANGED_JSON_ATTRIBUTE` to items) is never called on this path. In any case the `.frm` JSON carries `changed=false` (line 419) and the propagation at line 1946 only runs when it is `true`.

The flag comes from **re-applying the `.frm` values to the existing real persist**, with the setters' own change detection:

- `deserializePersist` (testExisting=true) reuses the existing `sidenav_2` (lines 1764, 1917) and records it in `persist_json_map`.
- `completePersist` then calls `setPersistValues(repository, retval, obj)` (line 2167) → `AbstractRepository.updatePersistWithValueMap(persist, values, overwrite=true)` → `AbstractBase.copyPropertiesMap(values, true)` (AbstractBase.java:145-182). This can raise `isChanged` in several ways, even when the file content equals the in-memory state:
  - `clearProperty(key)` for every in-memory property that is not in the re-read map sets `isChanged = true` (AbstractBase.java:116-118).
  - `setPropertyInternal` for a key not yet in `propertiesMap` sets `isChanged = true`. It only reverts when the value equals the content-spec default (AbstractBase.java:218-241).
  - `checkForChange` does an `equals` comparison of old and new values (AbstractBase.java:743-760). Values are rebuilt from strings by `convertArgumentStringToObject`, and complex values such as `ServoyJSONObject`/`CSSPosition` are compared through `equals`/`toString`, so representation differences count as changes.
  - **WebComponent specifically:** `WebComponent.setPropertyInternal` (WebComponent.java:126-130) always also calls `PersistHelper.setWebComponentProperty`. For every property that has a spec `PropertyDescription`, that method calls `webComponent.flagChanged()` **unconditionally**, whether or not the value changed (PersistHelper.java:1605-1664, line 1661). `clearWebComponentProperty` similarly flags on removal (line 1554).

So any composite item that a sibling re-read touches can end up `isChanged() == true` without any real content change. Web components are the most likely to, because of the unconditional `flagChanged()`. The `ServoyModel` visitor then treats "flagged changed" as "must be pushed to the editing solution". That assumption fails here: the item's containing file (`abc.frm`) did not change.

I did not determine (it needs the debugger, which I did not touch) which property re-application sets the flag on `sidenav_2`. To pin it down, put a breakpoint on `AbstractBase.flagChanged` and on the `isChanged = true` assignments in `AbstractBase` with the condition `uuid == 04a04d9d-...`. The fix below does not depend on which property it is.

### Which persists are "stored inside the parent's file"

- `SolutionSerializer.getFileName` (SolutionSerializer.java:1394-1398) returns the parent's file name when `isCompositeWithItems(persist.getParent()) || persist instanceof IChildWebObject`. `getRelativePath` (1447-1451) likewise recurses to the parent. For such an item, `getRelativeFilePath(persist, false)` is the containing `.frm` (or `.tbl`/`.rel`/`.menu`).
- `isCompositeWithItems`: Relation, TableNode, Form, TabPanel, Portal, LayoutContainer, Menu, MenuItem.
- `IScriptElement` is handled first in `getFileName` and resolves to the `.js`, not the parent's file. `isCompositeItem` also excludes it explicitly.
- Containers nested in a form (LayoutContainer, TabPanel, Portal) are themselves `compositeWithItems`, so they **already** go through `isOwnCompositeFileChanged`. `getRelativeFilePath` for them already resolves to the form's `.frm`, so they are protected today. Only **leaf items** are exposed: WebComponent, GraphicalComponent, Field, Bean, Part, Tab, Portal columns, WebCustomType (`IChildWebObject`), RelationItem, TableNode/Menu leaf items.

### Note on the two previous, reverted approaches

- **Snapshot/restore guard:** this tried to preserve or restore the editing form's state around the change round. It was wrong because the re-add does not come from overwriting the composite. SVY-21249 already prevents that, and the Form takes the skip branch. The re-add is a separate, item-level `updateEditingPersist(sidenav_2, false)` call. It creates the item and also clears its removed-object registration, so a form-level snapshot cannot express or undo it correctly.
- **Approach A** (stop re-firing the composite to real-solution listeners): this was wrong because listeners and the RFB preview only render the editing form. The editing model is mutated directly in `updateEditingPersist` → `copyPersistIntoSolution`, whatever is fired.

## Ticket premise check

The premise is correct. Delete without save, then "Create variable" in the Data Provider dialog, brings the deleted component back. The cause is model-level, not a designer rendering issue, and it is not component-specific. It is broader than the ticket says: any synchronous `.js` write for a form with unsaved item-level edits can push stale real-solution items over the editing ones. For example, a variable or method created from the editor or Solution Explorer while the form editor is dirty. That can resurrect deleted items or revert unsaved property edits on items whose real copy gets flagged. The same mechanism applies to `.tbl` items when `_calculations.js`/`_foundset.js` changes.

## Approaches considered

### 1. Extend the SVY-21249 guard to composite items (recommended)

In the `handleChangedFiles` visitor, decide "own file changed" for composite items by whether their **containing** file changed, instead of forcing `true`:

```java
boolean compositeWithItems = SolutionSerializer.isCompositeWithItems(persist);
boolean storedInContainerFile = !(persist instanceof IScriptElement) &&
    (SolutionSerializer.isCompositeWithItems(persist.getParent()) || persist instanceof IChildWebObject);
boolean ownFileChanged = !(compositeWithItems || storedInContainerFile) ||
    isOwnCompositeFileChanged(persist, solutionLocationDir.getParentFile(), changedFilePaths);
if ((compositeWithItems || storedInContainerFile) && !ownFileChanged)
{
    persist.clearChanged();
    changed.put(persist.getUUID(), persist);
}
else { /* unchanged: updateEditingPersist(...) */ }
```

`isOwnCompositeFileChanged` already resolves the path via `SolutionSerializer.getRelativeFilePath(persist, false)`. For an item, that is the parent's `.frm`, so the helper works unchanged. Its `.less` branch is Form-only and does not apply to items, which is correct. The `updateEditingPersist` `recursive` argument stays `compositeWithItems`.

How this behaves case by case:

- **Repro (only `abc.js` changed):** `abc.frm` is not in `changedFilePaths`, so the item is skipped and `sidenav_2` stays deleted in the editing form. Its removed-object registration is kept (no `clearEditingState`), so a later Save still deletes it from disk.
- **The new variable still reaches the editing form:** `ScriptVariable` is an `IScriptElement`, so it is excluded from the new predicate and still goes to `updateEditingPersist`. It is also still collected in `changedScriptElements`. This is the SVY-21249 behaviour.
- **Genuine external/team change to `abc.frm`:** the `.frm` is in `changedFilePaths`, so the Form takes the full recursive update (as today) and each item's containing file also counts as changed. Items are still copied.
- **New item added on disk:** this always means the `.frm` changed, so it is copied.
- **Item removed on disk:** handled through `strayCats` (ServoyModel.java:2669-2676 and later code), not this branch. Unaffected.
- **`.less` change:** the Form takes the full recursive update (SVY-21576), which already copies all children. Items in the same round see the `.frm` as unchanged and skip their own second copy. That is harmless, because the recursive form copy already pushed them.
- **Nested items** (component in a LayoutContainer, Tab in a TabPanel, WebCustomType in a WebComponent): `getRelativeFilePath` recurses to the top-level `.frm`. Correct.
- **TableNode items:** same logic with `.tbl`. A calculation `.js`-only change no longer overwrites unsaved table-node item edits. Calculations are `IScriptElement`, so they are still copied.
- **Real listeners:** the skip branch still puts the persist into `changed`, so real-solution listeners are still notified. That matches SVY-21249's skip branch.

Correctness boundaries:

- The guard relies on `changedFilePaths` being the real set of changed files. That was fixed by a9a21061c1 (path doubling) and is covered by `ServoyModelOwnFileChangedTest`.
- If something changes a real composite item **in memory** without writing its file, and relies on this branch to push it to editing, that push stops. I found no such caller on this path: real-solution persists are read-only for editors (`EclipseRepository.updateNode` rejects writes to the real solution), and real items only change through deserialization.
- `IChildWebObject` already makes `getFileName` resolve to the parent, so including it is consistent with the serializer.

Blast radius: one visitor in `ServoyModel.handleChangedFiles`. The `isOwnCompositeFileChanged` helper can be reused unchanged, or gain a sibling predicate helper so it can be unit tested. Extend `ServoyModelOwnFileChangedTest` with item cases: item with `.frm` changed → true, item with only `.js` changed → false, nested item, `IChildWebObject`, and `IScriptElement` not covered by the predicate.

### 2. Prevent the real item from being flagged changed in `SolutionDeserializer`

Possible variants:

- skip `setPersistValues` for persists that come from a file that is only read as a related entry (`changed=false`);
- save and restore `isChanged` around `completePersist` for such persists;
- make `PersistHelper.setWebComponentProperty` flag only on a real value change.

Assessment: this fixes the symptom at its source but has a much larger blast radius:

- The deserializer is shared by solution load, import, team sync, revert, and the war/solution exporters.
- `hasRelatedEntries` exists so the `.frm` is resolved as the script parent. Skipping value re-application changes long-standing reconciliation semantics.
- The `PersistHelper` change is in `servoy_shared` (servoy-client repo), which is also used at runtime and by the solution model.
- It does not make `handleChangedFiles` robust: any future spurious item flag would bring the bug back. The visitor would still equate "flagged" with "content changed on disk".

Worth a separate cleanup case (the unconditional `flagChanged()` is questionable on its own), but not as the fix for this bug.

### 3. No code change

Not acceptable. The default "create variable" workflow silently brings back deleted components, and also silently cancels the pending delete (the removed-object registration is cleared), so saving does not complete the user's delete. There is no reasonable workaround other than "save before creating variables".

## Recommendation

**Approach 1.** Extend the SVY-21249 own-file guard in `ServoyModel.handleChangedFiles` to composite items: persists that are not `IScriptElement` and whose parent `isCompositeWithItems`, or that are `IChildWebObject`. Decide "own file changed" with the existing `isOwnCompositeFileChanged` (the `getRelativeFilePath` → containing `.frm`). Skip `updateEditingPersist` when that file did not change. Keep `clearChanged()` and the `changed.put(...)` notification in the skip branch. Correctness boundaries are listed under approach 1. Add plain-JUnit cases to `com.servoy.eclipse.core.tests.plainjunit.ServoyModelOwnFileChangedTest`. Manual retest: the Jira repro (sidenav stays deleted, the new variable appears and is assignable, Save removes sidenav from `abc.frm`), plus an external edit to `abc.frm` while the editor is open (the change must still show up).

## Git history findings

- `8dddf28a6f` (SVY-21249, 2026-08-06) introduced the guard. Its intent (commit message and code comment): when a **composite** (e.g. Form) is flagged changed only because a child script `.js` changed, do not overwrite the editing composite, so unsaved editor state is preserved. Only composites (`isCompositeWithItems`) were considered. The `!compositeWithItems ||` term keeps the pre-existing behaviour for everything else. Nothing in the commit, its tests, or the follow-ups routes composite items through the else branch on purpose. The item case was simply not covered.
- `a9a21061c1` (SVY-21249 follow-up, 2026-09-01) fixed the path doubling (`solutionLocationDir.getParentFile()` as base) that made `ownFileChanged` always false for composites.
- `3e88c32d45` (SVY-21576, 2026-10-16) added the form `.less` own-file case and extracted `isOwnCompositeFileChanged`, with `ServoyModelOwnFileChangedTest` (only Form/composite cases).
- Before SVY-21249, the visitor already sent every flagged persist (composites and items) to `updateEditingPersist`. In the repro, the Form would have been fully overwritten recursively, also bringing the sidenav back. So this bug predates SVY-21249. SVY-21249 fixed the composite half and left the item half open.
- The visitor's `CONTINUE_TRAVERSAL` for `isCompositeWithIndependentSerializationOfSubItems` (Form, etc.) is long-standing. It is what makes the visitor reach the form's items after the Form takes the skip branch.
- Recent `SolutionDeserializer` history (`62ae7701ca` SVY-21431, duplicate-UUID runtime properties) does not touch change flagging.

## Questions for the reporter

None. The verdict is PROCEED.

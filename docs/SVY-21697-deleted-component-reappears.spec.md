# Spec: SVY-21697 — Deleted component reappears after adding a calendar field

## 1. Goal

When the form editor has unsaved structural edits, such as a deleted component, writing only the form's `.js` to disk must not bring stale real-solution items back into the editing form. In the reported case the `.js` is written by "Create" variable in the Data Provider dialog. The fix extends the SVY-21249 own-file guard in `ServoyModel.handleChangedFiles` from composites to composite items. Composite items are persists serialized inside their parent's `.frm`/`.tbl`/`.rel`/`.menu`. An item is pushed to the editing solution only when its containing file actually changed on disk.

## 2. Background

### 2.1 Reproduction (Jira)

1. Import the attached solution and open form `abc` in the form editor. Add any component.
2. Delete the sidenav component with the context menu. Do not save.
3. Add a calendar and assign it a form variable created with the Data Provider dialog's "Create" button.
4. When the dataprovider is assigned, the deleted sidenav reappears on the form.

The bug is not specific to sidenav or calendar. Any synchronous write of the form's `.js` while the form editor has unsaved item-level edits can trigger it. The same applies to `.tbl` items when `_calculations.js`/`_foundset.js` is written.

### 2.2 Editing solution vs real solution

`ServoyProject` holds two copies of each solution:

- The **real** solution mirrors what is on disk.
- The **editing** solution is what editors and the RFB designer work on and render.

`FormElementDeleteCommand` removes `sidenav_2` from the editing form only. It records it in the editing solution's removed objects, so Save deletes it from `abc.frm`. Until Save, the real solution and `abc.frm` on disk still contain `sidenav_2`.

`ServoyProject.updateEditingPersist(persist, recursive)` (ServoyProject.java ~494-527) copies a real persist into the editing solution through `copyNodeToEditingSolution`. When the editing copy does not exist, `copyPersistIntoSolution` creates it and adds it to the editing parent. `copyNodeToEditingSolution` also calls `editingSolution.clearEditingState(dest)`, which drops the persist from the removed-objects registration.

### 2.3 Root cause (captured live stack trace)

```
AbstractBase.internalAddChild:610                this = EDITING form 'abc', obj = sidenav_2
AbstractRepository.copyPersistIntoSolution:304   no editing copy -> createObject + addChild
ServoyProject$1.visit:321
ServoyProject.copyNodeToEditingSolution:303
ServoyProject.updateEditingPersist:524
ServoyModel$22.visit:2706                        handleChangedFiles visitor, ELSE branch
ServoyModel.handleChangedFiles:2678              solution.acceptVisitor over the REAL solution
ServoyModel.handleChangedFilesInSolutionProject:2476
... resource change broadcast ...
WorkspaceFileAccess.setContents:76
NewVariableAction.createNewVariable:301          writes forms/abc.js
DataProviderDialog.createVarButtonPressed:174
```

Frame values at `ServoyModel$22.visit`: `persist = sidenav_2` (WebComponent, parent `abc`), `persist.isChanged() == true`, `compositeWithItems == false`, `ownFileChanged == true`.

The sequence:

1. `NewVariableAction` writes `forms/abc.js`. That is the only changed file.
2. `handleChangedFiles` (ServoyModel.java ~2634) calls `sd.updateSolution(..., readAll=false, useFilesForDirtyMark=false, shouldReset=true, testExisting=true, ...)`. `abc.frm` is unchanged but is read anyway as a related entry of `abc.js` (`SolutionDeserializer.hasRelatedEntries`). Its items are re-applied onto the existing real persists.
3. The real Form `abc` is flagged changed, because a new `ScriptVariable` came from `abc.js`. The real `sidenav_2` is also flagged changed (§2.4).
4. The visitor in `handleChangedFiles` (ServoyModel.java ~2678-2732) runs:
   - **Form `abc`:** `compositeWithItems == true` and `isOwnCompositeFileChanged(...) == false`, so it takes the SVY-21249 skip branch (`clearChanged()` + `changed.put`). This is correct. `isCompositeWithIndependentSerializationOfSubItems(form)` is true, so the visitor returns `CONTINUE_TRAVERSAL` and descends into the form's children.
   - **Item `sidenav_2`:** `compositeWithItems == false`, so line ~2693 `ownFileChanged = !compositeWithItems || ...` is forced to `true`. The item goes to the else branch, `servoyProject.updateEditingPersist(sidenav_2, false)`. That re-creates it on the editing form and clears its removed-object registration.
5. The RFB designer renders the editing form, so the sidenav is back. The pending delete is also lost: a later Save would not remove it from `abc.frm`.

### 2.4 Why the real item is flagged changed when only `abc.js` changed

The flag does not come from the `changed` JSON attribute. With `useFilesForDirtyMark=false`, `handleChanged` is not called, and the `.frm` JSON carries `changed=false`. It comes from re-applying the `.frm` values onto the existing real persist: `completePersist` → `setPersistValues` → `AbstractRepository.updatePersistWithValueMap(..., overwrite=true)` → `AbstractBase.copyPropertiesMap`. That path can raise `isChanged` even when the content is equal:

- `clearProperty` sets the flag for every in-memory property that is missing from the re-read map.
- `setPropertyInternal` sets it for new keys.
- `checkForChange` compares values rebuilt from strings using `equals`.

Specifically for `WebComponent`, `WebComponent.setPropertyInternal` always calls `PersistHelper.setWebComponentProperty`. For spec-described properties, that method calls `flagChanged()` **unconditionally**.

Which exact property flags `sidenav_2` was not pinned down. The fix does not depend on it. The visitor's flaw is that it treats "flagged changed" as "content changed on disk".

### 2.5 Why the two previous approaches were wrong (both reverted, do not reintroduce)

- **Snapshot/restore guard over the real solution / editing form:** the re-add does not come from overwriting the composite. SVY-21249 already skips the Form. It comes from a separate item-level `updateEditingPersist(sidenav_2, false)`, which also clears the removed-object registration. A form-level snapshot can neither express nor correctly undo that.
- **"Approach A", stop re-firing the composite to real-solution listeners:** the editing model is mutated directly in `updateEditingPersist` → `copyPersistIntoSolution`, regardless of which listeners fire. Listeners and the designer only render the editing form, so suppressing notifications changes nothing.

### 2.6 Git history

- `8dddf28a6f` (SVY-21249, 2026-08-06) introduced the own-file guard. When a **composite** is flagged changed only because a child script `.js` changed, the editing composite is not overwritten. Only `isCompositeWithItems` persists were considered. `!compositeWithItems ||` keeps the old behaviour for everything else. No commit or test routes items through the else branch on purpose. The item case was simply not covered.
- `a9a21061c1` (SVY-21249 follow-up, 2026-09-01) fixed path doubling by using `solutionLocationDir.getParentFile()` as the base. Before that, `ownFileChanged` was always false for composites. The guard in this spec relies on that fix.
- `3e88c32d45` (SVY-21576, 2026-10-16) added the Form `.less` own-file case, extracted `isOwnCompositeFileChanged`, and added `ServoyModelOwnFileChangedTest` (Form/Relation cases only).
- **The bug predates SVY-21249.** Before it, the visitor sent every flagged persist to `updateEditingPersist`, so the Form was fully and recursively overwritten, which also restored the sidenav. SVY-21249 fixed the composite half and left the item half open.

## 3. Design

### 3.1 The change

In the `handleChangedFiles` visitor, a persist that is stored in its container's file is treated like a composite for the own-file decision:

```java
boolean compositeWithItems = SolutionSerializer.isCompositeWithItems(persist);
boolean storedInContainerFile = isStoredInContainerFile(persist);
boolean guarded = compositeWithItems || storedInContainerFile;
boolean ownFileChanged = !guarded ||
    isOwnCompositeFileChanged(persist, solutionLocationDir.getParentFile(), changedFilePaths);
if (guarded && !ownFileChanged)
{
    persist.clearChanged();
    changed.put(persist.getUUID(), persist);
}
else
{
    IPersist editingPersist = servoyProject.updateEditingPersist(persist, compositeWithItems); // recursive arg unchanged
    persist.clearChanged();
    changed.put(persist.getUUID(), persist);
    changedEditing.put(persist.getUUID(), editingPersist);
}
```

The `IScriptElement` collection into `changedScriptElements`, the `TableNode` in-mem reload, and the traversal return value are unchanged.

`isOwnCompositeFileChanged` is reused unchanged. It compares `new File(solutionParentDir, SolutionSerializer.getRelativeFilePath(persist, false))` against `changedFilePaths`. For an item, `getRelativeFilePath` already resolves to the containing file (§3.2). Its `.less` branch is `instanceof Form`-only and does not apply to items. Its Javadoc must be widened to say it also covers items stored in a container's file.

### 3.2 The item predicate

`isStoredInContainerFile(IPersist)` is a new `static` helper in `ServoyModel`. It is `public` (like `isOwnCompositeFileChanged`) so the plain-JUnit test in another bundle can call it.

```java
public static boolean isStoredInContainerFile(IPersist persist)
{
    return !(persist instanceof IScriptElement) &&
        (SolutionSerializer.isCompositeWithItems(persist.getParent()) || persist instanceof IChildWebObject);
}
```

This mirrors `SolutionSerializer.getFileName` (SolutionSerializer.java ~1380-1407):

- `IScriptElement` is handled first and resolves to the `.js`.
- `Solution` resolves to its settings file. Its parent is the repository/null, so the predicate is false.
- Otherwise, `isCompositeWithItems(persist.getParent()) || persist instanceof IChildWebObject` returns the parent's file name.

`getRelativePath` (~1443-1499) recurses to the parent when the parent `isCompositeWithItems`. For an `IChildWebObject` under a `WebComponent`, the `WebComponent` contributes an empty path segment and the recursion reaches the form. So `getRelativeFilePath(item, false)` is the top-level `<solution>/forms/<form>.frm` (or `.tbl`/`.rel`/`.menu`), including for nested items.

`isCompositeWithItems` covers Relation, TableNode, Form, TabPanel, Portal, LayoutContainer, Menu and MenuItem. For solutions with file version `< VERSION_38` it covers Relation only. In that legacy layout, form elements have their own files, so the predicate is false for them and today's behaviour is kept.

The implementer must re-verify the predicate against the live `SolutionSerializer.getFileName`/`getRelativePath` before committing. If they diverge, follow the serializer.

Nested containers (LayoutContainer, TabPanel, Portal) are already `compositeWithItems` and already guarded. They are also `storedInContainerFile`, which changes nothing for them. The change affects leaf items: WebComponent, GraphicalComponent, Field, Bean, Part, Tab, portal columns, `IChildWebObject`/WebCustomType, RelationItem, and leaf items of TableNode/Menu.

### 3.3 Correctness boundaries

- **Repro (only `abc.js` changed):** `abc.frm` is not in `changedFilePaths`, so `sidenav_2` takes the skip branch. It stays deleted in the editing form and its removed-object registration is kept (no `clearEditingState`). A later Save removes it from `abc.frm`.
- **New variable/method still reaches the editing form:** `ScriptVariable`/`ScriptMethod` are `IScriptElement`, so they are excluded from the predicate and still take `updateEditingPersist`. They are still added to `changedScriptElements`.
- **Genuine external or team change to `abc.frm`:** the `.frm` is in `changedFilePaths`, so the Form takes the full recursive update and each item also sees its containing file as changed. Items are still copied.
- **New item added on disk:** this implies the containing `.frm` changed, so it is copied.
- **Item removed on disk:** handled via `strayCats` (ServoyModel.java ~2669-2676 and later). Unaffected.
- **Form `.less` change (SVY-21576):** the Form takes the full recursive update, which already copies all its children. The items then see the `.frm` as unchanged and skip a redundant second copy. Harmless.
- **Nested items** (component in a LayoutContainer, Tab in a TabPanel, WebCustomType in a WebComponent): these resolve to the top-level `.frm`.
- **TableNode items:** same logic with `.tbl`. A `_calculations.js`/`_foundset.js`-only change no longer overwrites unsaved table-node item edits. Calculations and foundset methods are `IScriptElement`, so they are still copied.
- **Real-solution listeners:** the skip branch still does `changed.put(...)`, so listeners are notified as in the SVY-21249 skip branch. Only `changedEditing` is not populated for the skipped item.
- **Dependency on `changedFilePaths`:** the guard relies on it being the accurate set of changed absolute paths (fixed by `a9a21061c1`, covered by `ServoyModelOwnFileChangedTest`).
- **In-memory-only real item changes:** if something changed a real item in memory without writing its file and relied on this branch to push it to editing, that push stops. The triage found no such caller: real-solution persists are not written by editors, and real items change only through deserialization.

## 4. Implementation plan

1. **`com.servoy.eclipse.core/src/com/servoy/eclipse/core/ServoyModel.java`**, through `tools["eclipse-coder"]` (never the built-in edit):
   1. Add `public static boolean isStoredInContainerFile(IPersist persist)` next to `isOwnCompositeFileChanged` (~2588-2622), with a short Javadoc referencing SVY-21697 and `SolutionSerializer.getFileName`. Import `com.servoy.j2db.persistence.IChildWebObject` if it is not imported yet. `IScriptElement` is already used in the visitor.
   2. In the `handleChangedFiles` visitor (~2687-2707), replace the `ownFileChanged`/skip condition as in §3.1. Introduce the `guarded` local (or an equivalent name). Keep `updateEditingPersist(persist, compositeWithItems)` unchanged. Update the inline comment so it says the guard also covers items stored in a container's file (SVY-21697).
   3. Widen the Javadoc of `isOwnCompositeFileChanged` to mention items. No signature change.
2. Organize imports, format the file, then run `tools["eclipse-ide"].getCompilationErrors` on `com.servoy.eclipse.core` and fix any errors. No new Spotbugs high-severity findings.
3. **Tests**: extend `com.servoy.eclipse.core.tests/src/test/java/com/servoy/eclipse/core/tests/plainjunit/ServoyModelOwnFileChangedTest.java` with new `@Nested` classes, or add a sibling class in the same `plainjunit` package. Use the existing conventions: reflective `Solution`/`Form` fabrication, `VERSION_38` metadata, and `SOLUTION_PARENT_DIR` + `getRelativeFilePath` path composition. Fabricate `WebComponent`, `LayoutContainer`, `WebCustomType` (or another `IChildWebObject`) and `ScriptVariable` the same way. Use `doReturn` mocking only if reflective construction is impossible. Cases are listed in §5.
4. Run the tests with `tools["eclipse-ide"].runJUnitTests` (`projectName=com.servoy.eclipse.core.tests`, `packageName=com.servoy.eclipse.core.tests.plainjunit`, `junitVersion: "6"`).
5. No changes to `MANIFEST.MF`, `pom.xml` or `build.properties`. `com.servoy.eclipse.core` already depends on `servoy_shared` and `com.servoy.eclipse.model`. If the test bundle cannot see a needed type, stop and report instead of editing the MANIFEST.

## 5. Acceptance criteria

- [ ] In `handleChangedFiles`, a flagged-changed persist for which `isStoredInContainerFile` is true does **not** reach `updateEditingPersist` when its containing file is not in `changedFilePaths`. It is `clearChanged()` and put into `changed`.
- [ ] Composites keep their SVY-21249/SVY-21576 behaviour. `updateEditingPersist`'s `recursive` argument is still `compositeWithItems`.
- [ ] `IScriptElement` persists are never covered by `isStoredInContainerFile` and are still copied and collected in `changedScriptElements`.
- [ ] Unit test: a WebComponent in a Form with the form `.frm` in the changed set → `isOwnCompositeFileChanged` is true.
- [ ] Unit test: the same WebComponent with only the form `.js` in the changed set → false.
- [ ] Unit test: a WebComponent nested in a LayoutContainer resolves to `<solution>/forms/<form>.frm`: true with the `.frm`, false with the `.js` only.
- [ ] Unit test: an `IChildWebObject` (e.g. WebCustomType under a WebComponent) → `isStoredInContainerFile` is true and resolves to the form `.frm`.
- [ ] Unit test: a `ScriptVariable`/`ScriptMethod` child of a Form → `isStoredInContainerFile` is false.
- [ ] Unit test: a WebComponent → `isStoredInContainerFile` is true, and a Form (whose parent is the Solution) → false.
- [ ] Existing `ServoyModelOwnFileChangedTest` cases still pass. `com.servoy.eclipse.core` has no compilation errors.
- [ ] Manual (Jira repro): after Delete sidenav, Add calendar, Create variable and assign it, the sidenav stays deleted, the new variable is visible and assigned, and Save removes the sidenav from `abc.frm` on disk.
- [ ] Manual: with the form editor open, an external edit to `abc.frm`, for example changing a component property, still shows up in the editor and designer.
- [ ] Manual: creating a variable or method while the form editor has an unsaved property edit on a component keeps that edit.

## 6. Out of scope

- Fixing the spurious change flag at its source: `SolutionDeserializer` related-entry re-application, or `PersistHelper.setWebComponentProperty`'s unconditional `flagChanged()` (triage Approach 2). It has a larger blast radius, shared with load, import, export, team sync and runtime (`servoy_shared`). It can be a separate cleanup case.
- Any change to `FormElementDeleteCommand`, undo/redo, save, `SolutionDeserializer`, `ServoyProject` (`updateEditingPersist`, `copyNodeToEditingSolution`, `clearEditingState`) or `SolutionSerializer`.
- The other `updateEditingPersist(child, false)` call sites in `ServoyModel` (~2860, ~2897).
- `MANIFEST.MF`, `pom.xml` and `build.properties` changes.
- The two reverted approaches (§2.5): the snapshot/restore guard and suppressing real-solution listener notification for the composite.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should an `integrationpde` test reproduce the full flow (open the editing form, delete an item, write the form `.js`, assert the item stays absent and its removed-object registration is kept)? `com.servoy.eclipse.core.tests` has no `integrationpde` package yet, and adding one needs the full `integration` profile and Jenkinsfile wiring (see AGENTS.md). | Test phase / human | Open |
| Which property re-application flags `sidenav_2` changed (debugger: `AbstractBase.flagChanged` conditioned on its UUID)? It is not needed for this fix, only for a possible Approach 2 cleanup case. | Human | Open |
| Should a follow-up case be filed for `PersistHelper.setWebComponentProperty`'s unconditional `flagChanged()`? | Human | Open |

# Spec: SVY-21150 — Ctrl+drag for ghost columns (guard + duplication)

## 1. Goal

Two related improvements to Ctrl+drag behavior for ghost elements (aggrid columns, tab panel tabs) in the form designer:

1. **Guard (implemented):** Prevent the form editor from duplicating an entire aggrid component when the user Ctrl+drags after multi-selecting aggrid column ghosts. The drag-copy logic was incorrectly adding the parent aggrid element to the selection when none of the selected ghost column UUIDs resolve to content iframe elements, resulting in the aggrid being cloned instead of the columns. The fix guards against this by detecting ghost-only selections and aborting the drag-copy.

2. **Ctrl+drag duplication (new):** Enable Ctrl+drag to duplicate ghost columns within the same parent container. When the user holds Ctrl during a ghost drag in `ghostscontainer.component.ts`, the original ghost stays in place and the dragged ghost becomes a clone. On drop, a new `duplicateGhosts` websocket call creates server-side copies of the selected ghosts with new UUIDs, inserted at the drop position.

## 2. Background

### 2.1 Ghost elements vs content elements

In the form designer, some child components (e.g. aggrid columns, tab panel tabs) are rendered as "ghost" overlays by `ghostscontainer.component.ts` rather than as real elements in the content iframe. These ghosts have UUIDs tracked in the selection, but calling `EditorContentService.getContentElement(uuid)` returns `null` for them because no matching `[svy-id='uuid']` element exists in the content iframe DOM.

### 2.2 The Ctrl+drag-copy flow

When the user Ctrl+drags in absolute layout, `dragselection.component.ts` handles the copy:

1. **`onMouseDown`** (line 94–118) — stores `this.dragNode` (the element under the mouse, resolved via `getNode(event)`). For a click over an aggrid, this is the aggrid's `.svy-wrapper` element.

2. **`onMouseMove`** (line 186–193) — on the first move with Ctrl held:
   - Sets `dragCopy = true`
   - Gets the current selection (ghost column UUIDs from prior Ctrl+clicks)
   - If `dragNode`'s `svy-id` is not already in the selection, **pushes it** (line 190)
   - Calls `initSelectionToDrag(selection)`

3. **`initSelectionToDrag`** (line 238–260) — for each UUID in the selection, calls `getContentElement(uuid)`. Ghost UUIDs return `null` and are silently skipped (`continue` at line 242–243). Only the aggrid UUID (injected at line 190) resolves, so only the aggrid gets cloned.

4. **`sendChanges`** / **`createComponents`** (line 139–171) — sends the cloned aggrid to the server, which creates a duplicate aggrid component.

### 2.3 Git history

- Lines 186–191: Ctrl+drag logic predates the git history visible in this file. Last substantive change was `b9e91abb52f` (2024-07-03, bulk re-indent by `lvostinar`). The `!` non-null assertions were added by `a6e8005ce13` (strictNullChecks migration) — a mechanical change, not a logic change.
- `initSelectionToDrag` line 242–243: The `if (!node) { continue; }` guard was added by `6988edaeaa8` (`Johan Compagner`, 2022-12-07) as a null-safety improvement. It inadvertently hides the ghost-element mismatch that causes this bug.

## 3. Design

### 3.1 Guard before adding dragNode to selection

In `onMouseMove` (line 186–193), after retrieving the selection, check whether any selection item resolves to a content element via `getContentElement()`. If none do (i.e. every selected item is a ghost), the selection is ghost-only and drag-copy cannot work. In this case:
- Do **not** add `dragNode` to the selection
- Do **not** set `dragCopy = true`
- Do **not** call `initSelectionToDrag`
- Set `selectionToDrag = []` so the subsequent non-Ctrl path (line 195–201) is also skipped

This prevents the wrong element from being injected into the selection.

### 3.2 Guard after initSelectionToDrag for empty result

As defense-in-depth, after each call to `initSelectionToDrag` (lines 192 and 200), check whether `selectionToDrag` is empty. If it is:
- Reset `dragCopy = false`
- Reset `selectionToDrag = null`
- Return early from `onMouseMove`

This catches any future scenario where the selection contains UUIDs that don't resolve, regardless of how `dragNode` was handled.

### 3.3 Scope of guard changes

Only `dragselection.component.ts` is modified. No backend changes are needed. The `ghostscontainer.component.ts` and `EditorContentService` are unchanged.

### 3.4 Ghost duplication — frontend (`ghostscontainer.component.ts`)

Add Ctrl+drag-to-duplicate support for `GHOST_TYPE_CONFIGURATION` ghosts (aggrid columns, custom type children). The pattern mirrors how `dragselection.component.ts` detects copy mode: copy activates on first `mousemove` with Ctrl held, not on `mousedown`.

#### 3.4.1 Ctrl detection and `dragCopy` flag

- In the existing `onMouseMove` handler for `GHOST_TYPE_CONFIGURATION` reorder drag:
  - On the first move, if `event.ctrlKey` (or `event.metaKey` on macOS) is true, set a `dragCopy` flag.
  - When `dragCopy` is true, keep the original ghost element in its original position (do not move it). Instead, show the dragged element as a visual clone at the cursor position.
  - `GHOST_TYPE_PART` must be excluded — parts cannot be duplicated.

#### 3.4.2 Visual feedback during copy-drag

- When `dragCopy` is active, the original ghost remains at its start position.
- The drag preview (the element being moved with the cursor) represents the clone.
- If the user releases Ctrl before dropping (detected via `keyup` listener), cancel copy mode: revert to normal reorder behavior (move the original ghost to cursor position, clear clone preview).

#### 3.4.3 Drop handling

- In `onMouseUp`, if `dragCopy` is true:
  - Do **not** call the existing `sendChanges()` (which reorders).
  - Instead, call a new `editorSession.duplicateGhosts()` method with:
    - `uuids`: the UUIDs of the selected ghosts to duplicate
    - `parentUuid`: the UUID of the parent component (the aggrid / container)
    - `dropIndex`: the target position index for the cloned ghosts
  - Reset `dragCopy` to `false`.

#### 3.4.4 Inherited ghost guard

- Before activating copy mode, check whether the ghost being dragged belongs to an inherited form element. Inherited ghosts cannot be duplicated (the parent form owns them). If inherited, do not set `dragCopy` — fall through to normal reorder behavior.

### 3.5 Ghost duplication — frontend (`editorsession.service.ts`)

Add a new method:

```typescript
duplicateGhosts(args: { uuids: string[], parentUuid: string, dropIndex: number }): Promise<any>
```

This sends a websocket call to the backend handler `duplicateGhosts` with the provided arguments.

### 3.6 Ghost duplication — backend (`EditorServiceHandler.java`)

Register a new handler name `"duplicateGhosts"` in the handler map, routing to `DuplicateGhostsHandler`.

### 3.7 Ghost duplication — backend (`DuplicateGhostsHandler.java`)

New handler class in `com.servoy.eclipse.designer.rfb.endpoint`.

#### 3.7.1 Input

- `uuids`: JSON array of ghost UUID strings to duplicate
- `parentUuid`: UUID string of the parent component
- `dropIndex`: integer position for insertion

#### 3.7.2 Processing

1. Resolve the parent persist from `parentUuid`.
2. For each UUID in `uuids`:
   a. Resolve the ghost persist (custom type child) from UUID.
   b. Validate it belongs to the given parent (same-parent constraint).
   c. Validate it is not inherited (skip inherited ghosts).
   d. Clone the persist. The clone must get a **new UUID** — use the same mechanism that SVY-21257 fixed (`WebComponent.cloneObj()` regenerates UUIDs for custom type children).
   e. Insert the clone at `dropIndex` (incrementing for each successive clone).
3. Wrap all mutations in a `BaseRestorableCommand` for undo/redo support.
4. Fire `IPersistChangeListener.persistsChanged()` to refresh the designer.
5. Return the UUIDs of the newly created ghosts so the frontend can update the selection.

#### 3.7.3 Ghost type handling

- **`GHOST_TYPE_CONFIGURATION`** (custom type children, e.g. aggrid columns): clone via the custom type property mechanism. These are JSON property children, not full persists — use `AddContainerCommand.addCustomType()` or equivalent.
- **`GHOST_TYPE_COMPONENT`** (nested components, e.g. tab panel tabs): clone via `IPersist.cloneObj()`. These are full persists with their own UUIDs.
- **`GHOST_TYPE_PART`**: rejected — parts cannot be duplicated.

### 3.8 Constraints

- **Same parent only** — cross-parent duplication is not supported. All selected ghosts must belong to the same parent.
- **No inherited ghosts** — ghosts belonging to an inherited form element are skipped.
- **Undo/redo required** — the duplication must be wrapped in a command for the undo stack.
- **Ctrl+click remains multi-select** — copy mode only activates on the first `mousemove` with Ctrl held, not on `mousedown` or `mouseup`.

## 4. Implementation plan

1. **`dragselection.component.ts` — guard the Ctrl+drag path (lines 186–193):**
   - After `const selection = this.editorSession.getSelection()` (line 188), add a check: `const hasContentElements = selection.some(id => this.editorContentService.getContentElement(id) != null)`.
   - If `!hasContentElements`, set `this.selectionToDrag = []` and skip the rest of the Ctrl+drag block (do not set `dragCopy`, do not push `dragNode`, do not call `initSelectionToDrag`).
   - If `hasContentElements`, proceed with the existing logic (add `dragNode` if needed, call `initSelectionToDrag`).

2. **`dragselection.component.ts` — guard after initSelectionToDrag (Ctrl path, after line 192):**
   - After `this.initSelectionToDrag(selection)`, add: if `this.selectionToDrag!.length === 0`, set `this.dragCopy = false`, `this.selectionToDrag = null`, and `return`.

3. **`dragselection.component.ts` — guard after initSelectionToDrag (non-Ctrl path, after line 200):**
   - After `this.initSelectionToDrag(selection)`, add: if `this.selectionToDrag!.length === 0`, set `this.selectionToDrag = null` and `return`.

4. **Update existing tests in `dragselection.component.spec.ts`:**
   - Add a test case: Ctrl+drag with ghost-only selection does not initiate drag-copy (verifies `dragCopy` remains `false` and `createComponents` is not called).
   - Add a test case: Ctrl+drag with a valid content-element selection still works as before.

5. **Verify guard:** Run `npm run lint`, `npm run build_debug_nowatch`, and `npm test` in `com.servoy.eclipse.designer.rfb/node/`.

6. **`ghostscontainer.component.ts` — add Ctrl detection to ghost drag:**
   - In the `onMouseMove` handler for `GHOST_TYPE_CONFIGURATION`, detect `event.ctrlKey`/`event.metaKey` on first move.
   - Set `dragCopy = true` if Ctrl is held and the ghost is not inherited and not a `GHOST_TYPE_PART`.
   - When `dragCopy` is true, keep the original ghost in place and show the drag preview as a clone.
   - Add a `keyup` listener to cancel copy mode if Ctrl is released before drop.

7. **`ghostscontainer.component.ts` — modify `onMouseUp` for copy-drag:**
   - If `dragCopy` is true, call `this.editorSession.duplicateGhosts(...)` instead of `sendChanges()`.
   - Pass the selected ghost UUIDs, parent UUID, and computed drop index.
   - Reset `dragCopy` to `false`.

8. **`editorsession.service.ts` — add `duplicateGhosts` method:**
   - Add `duplicateGhosts(args: { uuids: string[], parentUuid: string, dropIndex: number }): Promise<any>` that sends a websocket call to `"duplicateGhosts"`.

9. **`EditorServiceHandler.java` — register new handler:**
   - Add `"duplicateGhosts"` to the handler map, routing to `DuplicateGhostsHandler`.

10. **Create `DuplicateGhostsHandler.java`:**
    - Implement the handler in `com.servoy.eclipse.designer.rfb.endpoint`.
    - Resolve parent persist, validate each ghost UUID belongs to the parent and is not inherited.
    - Clone each ghost with new UUIDs (using the SVY-21257-safe cloning mechanism).
    - Insert clones at the specified drop index.
    - Wrap in `BaseRestorableCommand` for undo/redo.
    - Fire `persistsChanged()` and return new ghost UUIDs.

11. **Add tests for ghost duplication:**
    - `ghostscontainer.component.spec.ts`: test that Ctrl+drag activates copy mode, creates clones, and calls `duplicateGhosts`.
    - `ghostscontainer.component.spec.ts`: test that Ctrl+drag on `GHOST_TYPE_PART` does not activate copy mode.
    - `ghostscontainer.component.spec.ts`: test that releasing Ctrl before drop cancels copy mode.

12. **Verify duplication:** Run `npm run lint`, `npm run build_debug_nowatch`, and `npm test` in `com.servoy.eclipse.designer.rfb/node/`.

## 5. Acceptance criteria

- [ ] Ctrl+selecting multiple aggrid column ghosts then Ctrl+dragging does **not** duplicate the aggrid component
- [ ] Ctrl+selecting multiple aggrid column ghosts then Ctrl+dragging is a no-op (nothing is created or moved)
- [ ] Ctrl+drag of regular content elements (buttons, labels, etc.) still duplicates them correctly
- [ ] Mixed selection containing both ghost UUIDs and real content UUIDs: only the real content elements are duplicated on Ctrl+drag
- [ ] Non-Ctrl drag of regular elements still moves them correctly
- [ ] Non-Ctrl drag when selection is ghost-only is a no-op (no crash, no incorrect move)
- [ ] All existing `dragselection.component.spec.ts` tests pass
- [ ] New test cases cover the ghost-only guard

### Ghost duplication
- [ ] Ctrl+dragging a ghost column (aggrid column) within the same parent duplicates the column at the drop position
- [ ] The original ghost stays in place during the Ctrl+drag (visual clone follows the cursor)
- [ ] Releasing Ctrl before drop cancels copy mode and reverts to normal reorder
- [ ] The duplicated ghost gets a new UUID (not a reference to the original)
- [ ] Ctrl+drag duplication is undoable/redoable via the standard undo stack
- [ ] `GHOST_TYPE_PART` (form parts) cannot be duplicated via Ctrl+drag
- [ ] Inherited ghost elements cannot be duplicated via Ctrl+drag
- [ ] Cross-parent duplication is not supported (ghosts stay within the same parent)
- [ ] ~~Ctrl+drag duplication works for `GHOST_TYPE_COMPONENT` (nested components like tab panel tabs)~~ — **moved to out of scope** (GHOST_TYPE_COMPONENT uses position-based drag, not reorder; requires separate implementation)
- [ ] After duplication, the newly created ghost(s) are selected
- [ ] New frontend tests cover Ctrl+drag copy mode activation, clone creation, and cancellation

## 6. Out of scope

- Cross-parent ghost duplication (dragging a ghost column from one aggrid to another).
- Showing a user-visible warning or toast when drag-copy is aborted due to ghost-only selection.
- Duplication of `GHOST_TYPE_PART` (form parts).
- `GHOST_TYPE_COMPONENT` Ctrl+drag duplication (nested components like tab panel tabs use position-based drag, not reorder — requires a different frontend mechanism).

## 7. Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| UUID handling in cloned ghosts | Duplicate UUIDs would corrupt the solution | Use SVY-21257-safe cloning (`WebComponent.cloneObj()` regenerates UUIDs for custom type children) |
| Ghost type differences | `GHOST_TYPE_CONFIGURATION` (custom type children) and `GHOST_TYPE_COMPONENT` (nested components) need different cloning paths | Handle each type separately in `DuplicateGhostsHandler` — custom type via `addCustomType()`, nested component via `cloneObj()` |
| Ctrl+click conflict | Ctrl+click is already used for multi-select | Copy mode activates on first `mousemove` with Ctrl held, same pattern as `dragselection.component.ts` |

## 8. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should Ctrl+drag of ghost columns be a supported feature in the future? | Product | resolved — yes, included in this spec |

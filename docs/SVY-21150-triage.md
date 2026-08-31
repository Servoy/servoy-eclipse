# Triage Report — SVY-21150

**Verdict:** PROCEED

## Reported problem

In the form editor, when holding Ctrl and clicking on multiple aggrid column ghosts to multi-select them, then performing drag-and-drop (Ctrl+drag), the entire aggrid component is duplicated instead of the selected columns.

## Root-cause assessment

The bug is a real interaction conflict between the ghost selection system and the absolute-layout drag-copy system (`dragselection.component.ts`). The root cause is in `dragselection.component.ts:186–193`.

**Detailed trace:**

1. User Ctrl+clicks on aggrid column ghosts. `ghostscontainer.component.ts:262` handles this correctly — each ghost UUID is toggled into the selection via `editorSession.setSelection()`.

2. User then Ctrl+drags from the content area (glasspane, over the aggrid visual). Two handlers fire:
   - `mouseselection.component.ts:176` — Ctrl is held, so the non-Ctrl path (`setSelection`) is skipped. No selection change.
   - `dragselection.component.ts:94–110` (via contentArea bubbling) — `getNode(event)` finds the aggrid element under the mouse, stores it as `this.dragNode`. Sets `this.dragStartEvent`.

3. On `mousemove`, `dragselection.component.ts:186–193` activates:
   ```typescript
   if ((event.ctrlKey || event.metaKey) && this.selectionToDrag == null) {
       this.dragCopy = true;
       const selection = this.editorSession.getSelection();
       if (this.dragNode && !selection.includes(this.dragNode.getAttribute('svy-id')!)) {
           selection.push(this.dragNode.getAttribute('svy-id')!);  // ← BUG HERE
       }
       this.initSelectionToDrag(selection);
   }
   ```
   The selection contains ghost column UUIDs (e.g., `col1-uuid`, `col2-uuid`). The aggrid's `svy-id` is different, so **line 190 adds the aggrid UUID to the selection**.

4. In `initSelectionToDrag` (line 238–260):
   - For each ghost column UUID: `getContentElement(colUUID)` queries the iframe for `[svy-id='colUUID']` — returns `null` because columns don't exist as separate elements in the content iframe (they're "ghosts" precisely for this reason). The `continue` at line 243 **silently skips** them.
   - For the aggrid UUID (just added at line 190): `getContentElement(aggridUUID)` returns the aggrid HTML element → it gets **cloned** for drag-copy.

5. On `mouseup`, `sendChanges` (line 139–171) calls `createComponents` with only the aggrid clone. **Result: the aggrid is duplicated, not the columns.**

**Key file:** `com.servoy.eclipse.designer.rfb/node/src/designer/dragselection/dragselection.component.ts:186–193`

## Ticket premise check

The ticket correctly identifies the symptom (aggrid duplicated instead of columns) and does not propose a specific solution. The premise is valid — this is a Servoy form editor bug.

## Approaches considered

1. **Guard in `dragselection.component.ts`** — Before adding `dragNode` to the selection (line 189–190), check whether any current selection items resolve to content elements. If none do (i.e., all are ghosts), skip adding `dragNode` and abort the drag-copy. This prevents the wrong element from being duplicated.
   - Pros: Minimal, targeted fix. Prevents the incorrect behavior. Low regression risk.
   - Cons: Ctrl+drag of ghost-selected items becomes a no-op (nothing happens). Doesn't add ghost column duplication as a feature.

2. **Abort after empty `initSelectionToDrag`** — After calling `initSelectionToDrag`, if `selectionToDrag` is empty despite having a non-empty selection, reset the drag state and bail out.
   - Pros: Defense-in-depth, catches the problem regardless of how `dragNode` was added. Also protects the non-Ctrl drag path.
   - Cons: Same as approach 1 — no column duplication feature.

3. **Implement ghost column Ctrl+drag duplication in `ghostscontainer`** — Add Ctrl+drag-copy support for `GHOST_TYPE_COMPONENT` ghosts, similar to how `dragselection-responsive.component.ts:188–216` handles Ctrl+drag with `createComponents`. The ghostscontainer would detect Ctrl being held during drag, and on mouseup send a `createComponents` call with the selected ghost UUIDs.
   - Pros: Actually delivers the expected user experience (columns get duplicated). Consistent with how Ctrl+drag works for regular components.
   - Cons: Larger change spanning frontend (`ghostscontainer`) and possibly backend (`CreateComponentsHandler`). Needs to verify that `CreateComponentCommand.createComponent` can clone ghost children (columns) given just their UUID. Higher risk.

4. **No code change** — Ghost elements (aggrid columns) are internal sub-objects, and Ctrl+drag duplication was never designed for them. The user should use copy/paste or right-click context menu instead.
   - Pros: Zero effort, zero risk.
   - Cons: The current behavior is actively harmful — it duplicates the entire aggrid, which is destructive/confusing. Even if ghost duplication isn't supported, the wrong-element duplication must be prevented.

## Recommendation

**Approach 1 + 2 combined** (guard + empty-result abort).

The immediate priority is preventing the wrong element (aggrid) from being duplicated. This is a small, safe fix in `dragselection.component.ts`:

1. At line 186–193: before adding `dragNode`, check if selection items are ghost-only (no content elements found). If so, don't add `dragNode` and skip the drag.
2. After `initSelectionToDrag`, if `selectionToDrag` is empty, reset drag state and return early.

Whether to implement approach 3 (actual ghost column duplication) is a separate feature decision that can be tracked in a follow-up ticket if desired. The fix here focuses on preventing the incorrect, destructive behavior.

## Git history findings

- `dragselection.component.ts:186–191` — The Ctrl+drag logic was introduced by `lvostinar` in commit `b9e91abb52f` (2024-07-03, "adjust indentation" — a bulk re-indent). The actual logic predates that commit. Lines 189–190 were later touched by `a6e8005ce13` (strictNullChecks migration, `[ai]`) — this was a mechanical `!` non-null assertion addition, not a logic change.
- `initSelectionToDrag` (line 238–260) — core logic dates to `96e18f742f3` (`emera`, 2022-02-01). The `if (!node) { continue; }` skip at line 242–243 was added by `6988edaeaa8` (`Johan Compagner`, 2022-12-07) as a null-safety improvement but inadvertently hides the ghost-element mismatch.
- `ghostscontainer.component.ts` — no Ctrl+drag-copy logic has ever existed for `GHOST_TYPE_COMPONENT`. The ghost drag for this type only handles visual repositioning (for dropping onto the content iframe), not duplication.

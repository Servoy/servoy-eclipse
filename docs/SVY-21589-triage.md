# Triage Report — SVY-21589

**Verdict:** NO_ACTION (most likely already fixed by `85f5e36992` / SVY-21625; confirm with the attached sample before closing)

## Reported problem
Sample `move_Row.servoy`, form `employees` (responsive, 12grid). The structure, read from the export's `solution.xml`, matches the screenshot:

```
container-fluid (4776FCD9)
  row (A388121A)          > col-md-12 (empty)
  row (79D4BEC1)          > col-md-12 > label_10 ("Test")
```

Steps in the ticket (reported 2026-10-12, Minor, fix version 2026.12):
1. In the form editor, drag the row that holds the label out of the container, so it lands at the form root.
2. Drag it back into the container. "This doesn't work the first" time.
3. "It matters where the mouse is over the row when the dnd happens."

The ticket only describes symptoms. It does not propose a fix. There are no comments or linked issues.

## Root-cause assessment
This is the same responsive drag-and-drop path as SVY-21625, seen from the other direction. That path is now fixed on master. All of this comes from reading the code. I did not run the designer.

After step 1 the DOM is `.svy-form > [container-fluid > wrapper > row1 > col], row2`. Top-level items are not wrapped. Children of a layout container are wrapped in `.svy-responsive-wrapper`, which has no `svy-id` (SVY-21248, `18f0c91473`). When row2 is dragged back, `DesignerUtilsService.getDropNode` (`com.servoy.eclipse.designer.rfb/node/src/designer/services/designerutils.service.ts`) resolves the target based on the element under the pointer:

| Pointer over | Before `85f5e36992` | On master now |
|---|---|---|
| row1's own area (padding/margin, `rowDesign`) | `getParent(row1)`: the row rejects `12grid.row` and climbs to the wrapper. The wrapper's `topContainer` fallback accepts the row, so the target is the wrapper (no `svy-id`). The preview and drop go to the form root, so row2 stays where it was. That is the "doesn't work" symptom. | The wrapper is skipped, the container accepts the row, and row2 goes before or after row1 inside the container. |
| lower/right 30% of row1 | Same wrapper problem. | Container, with `beforeChild` set to the next sibling, or `append`. |
| the container's own padding | The "drop directly on node" branch already drilled through wrappers (`querySelector('[svy-id]')`), so this worked. It explains "works on another try / depends on where the mouse is". | Unchanged, works. |
| the empty `col-md-12` inside row1 | The column accepts the row (`12grid/column.spec` only `excludes` container/column/flexitem), so row2 is nested inside the column. | Unchanged. This is a nested Bootstrap row, which is valid. SVY-21625 lists it as known existing behaviour. |
| the form area outside the container | Form-root branch, row stays at root. | Unchanged, correct. |

Both of the reporter's symptoms match the first two rows of the table: the first attempt fails, and the outcome depends on pointer position. Commit `85f5e36992` changed exactly those rows (the wrapper skip in `getParent`, plus not using a plain component as a parent for a layout drag). Step 1 (moving the row out to the root) used the form-root branch, which was correct before and after the commit.

"It matters where the mouse is" can also mean the mousedown point. `DragselectionResponsiveComponent.onMouseDown` drags the element under the pointer (`getNode(event)`), not the current selection. So pressing on the column or label inside a selected row drags that child, not the row. That is long-standing designer behaviour, not part of this regression.

Residual risk I could not rule out by reading alone: when row2 moves between the form root and the container during the preview, the two places are different `@for` blocks in `designform_component.component.ts`. Angular probably re-creates the row's DOM, which would leave `dragNode` detached. The follow-up code compares by `svy-id` (`isSameElement`, `getNode`'s `skipNodeId`), and `getNextElementSibling` on a detached node returns `null`, which `onMouseUp` handles as "no right sibling". I don't see a failure path from that, but only a live run can confirm it.

## Ticket premise check
No solution was proposed. The symptom is real and lives in this project's RFB designer code. It is the SVY-21248 wrapper regression that SVY-21625 already fixed. It is not user error and not a 12grid package problem.

## Approaches considered
1. **No code change (recommended).** Verify the ticket's steps with `move_Row.servoy` on a build that contains `85f5e36992`, then resolve as fixed by SVY-21625.
   - Pros: the code path that causes both symptoms is already fixed and covered by Vitest regression tests (`designerutils.service.spec.ts`).
   - Cons: depends on a manual check. If it still fails, this needs a new investigation, probably with runtime logging (for example around a detached `dragNode`).
2. **Add a Vitest regression test for the "back into the container" direction.** The dragged row is a top-level `.svy-form` child, and the pointer is over a wrapped row1. Assert that `getDropNode` returns the container.
   - Pros: cheap, and pins the reverse direction that the SVY-21625 tests don't cover explicitly.
   - Cons: exercises the same `getParent` logic that is already tested. Small added value.
3. **Stop columns from accepting rows as drop targets.**
   - Pros: dropping on an empty column would then reach the container.
   - Cons: nested rows in columns are valid Bootstrap and allowed by the 12grid spec. That would be a behaviour or package change, out of scope here, as already decided in SVY-21625.

## Recommendation
NO_ACTION as a code change. Treat SVY-21589 as fixed by SVY-21625 (`85f5e36992`, master, 2026-11-19), after checking by hand with the attached sample:
- Drag row2 below the container, so it lands at the form root.
- Drag it back over row1's upper part, so it goes before row1 inside the container.
- Drag it over row1's lower 30%, so it goes after row1.
- Drag it over the container padding.
- On the first attempt, check that the row never stays at the root or jumps there.

Dropping onto the empty column nests the row in that column. That is expected.

If the manual check still fails, reopen the investigation with runtime logging in `onMouseMove`/`onMouseUp`. The logs should show `canDrop`, whether `dragNode` is connected, and its `svy-id`. Optionally add approach 2's test while closing.

## Git history findings
- `18f0c91473` (emera, 2026-07-28, SVY-21248): added `.svy-responsive-wrapper` around every child of a layout container. This is the regression source, shipped in 2026.9, and matches the report date of 2026-10-12.
- `85f5e36992` (2026-11-19, SVY-21625, on master only, not in a tag yet): the `getParent` wrapper skip, the "component is not a parent for a layout" skip, and the drag-time `min-height`. This covers the failing rows above.
- `getDropNode`'s form-root branch was last changed in `a6e8005ce1` (Gabi Boros, 2026-07-28) and `6a95cafbc1`/`72f081d27e` (Johan Compagner, 2026-06/08). Those changes don't affect this case.
- No dependency or target-platform change is involved. No earlier commit mentions SVY-21589.

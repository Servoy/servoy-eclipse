# Triage Report — SVY-21625

**Verdict:** PROCEED

## Reported problem
In a responsive (12grid) form with `container-fluid > row1, row2, row3` (each row holding one `col-md-12`), you cannot drag `row3` in the RFB form editor and drop it between `row1` and `row2`. The drop either puts the row "on top of the container", meaning outside it at the form root, or leaves it where it was. The attached sample (`row_reorder.servoy`, form `resp`) has exactly that structure. Row 3 holds an `aggrid-groupingtable` (`aggrid1`) with one column.

The ticket only describes the problem. It does not propose a fix.

Side note: the user's screenshot shows the `columns` ghost strip of `aggrid1` overlapping its "property 'columns'" label. That is a separate cosmetic issue in the ghost rendering. It is not mentioned in the ticket, and nothing in the drop-target code depends on it.

## Root-cause assessment
This is a regression in the RFB responsive drag-and-drop. It was introduced by SVY-21248 (commit `18f0c91473`, 2026-07-28, first shipped in 2026.9.0), and the analysis below is based on reading the code.

**1. SVY-21248 added a wrapper div around every child of a layout container.**
In `com.servoy.eclipse.ngclient.ui/node/src/designer/designform_component.component.ts` (template `#svyResponsiveDiv`, lines 96-104), each child of a layout container is now rendered inside `<div class="svy-responsive-wrapper">`, which is `display: contents` (in `designform.css`). The wrapper has no `svy-id` and no `svy-layoutname`. So the DOM for the sample is:

```
.svy-form
  div.svy-layoutcontainer [svy-id=container, svy-layoutname=12grid.container]
    div.svy-responsive-wrapper            <- no svy-id
      div.svy-layoutcontainer [svy-id=row1, svy-layoutname=12grid.row]
    div.svy-responsive-wrapper
      div.svy-layoutcontainer [svy-id=row2 ...]
    div.svy-responsive-wrapper
      div.svy-layoutcontainer [svy-id=row3 ...]
```

**2. `DesignerUtilsService.getParent` stops at the wrapper instead of the container.**
`com.servoy.eclipse.designer.rfb/node/src/designer/services/designerutils.service.ts`, `getParent` (lines 256-273), is called from `getDropNode` (line 130) when `row3` is dragged over `row2`:
- `row2` (`12grid.row`) does not accept `12grid.row` children, so `getParent` recurses on `row2.parentElement`.
- Before SVY-21248, that parent was the container, which accepts rows, so the drop target was the container.
- Now that parent is the `.svy-responsive-wrapper`. It has no `svy-layoutname`, so `editorSession.getAllowedChildrenForContainer(null)` falls back to the `'topContainer'` list (`editorsession.service.ts` line 384). That list contains `12grid.row` (`row.spec` has `"topContainer": true`), so `getParent` returns the wrapper as the drop target.

**3. The wrapper has no `svy-id`, so the move goes to the form root.**
In `getDropNode`, the branch where `realDropTarget != dropTarget` (lines 135-174) returns `{ dropTarget: wrapper, beforeChild: row2 }`, or `beforeChild = row3` when the pointer is in the bottom/right 30% band.
- Preview (`dragselection-responsive.component.ts` line 164): it sends `dropTarget: wrapper.getAttribute('svy-id')`, which is `null`. The iframe (`designform_component.component.ts` lines 406-417) then uses `formCache.mainStructure` (the form root) as the parent. `row2` is not a child of the form root, so `indexOf` returns -1 and the preview row is spliced in next to the container: "on top of the container".
- Drop (`onMouseUp` lines 199-213): `dropTargetUUID` comes out `null` and is left out of the payload, while `rightSibling` is still `row2`. On the server, `MoveInResponsiveLayoutHandler` (lines 87-108) can't resolve a drop target, logs `drop target ... not found`, and uses the form as the parent. `ChangeParentCommand` then moves `row3` out of the container to the form root and appends it, because `row2` is not among the form's children. So the editor really does move the row to the form root.
- When the pointer is in the bottom/right 30% of `row2`, `getNextElementSibling(row2)` reads through the wrapper and returns `row3`, which is the dragged node. `onMouseMove` (line 153) ignores that case, so the row stays where it was. That is the reporter's second symptom.

The wrapper is not new to this kind of bug. SVY-21578 (commit `c2a38d6ffa`, 2026-10-12) fixed the same problem in `MouseSelectionComponent.insertACopyAction`, where code that read `parentNode`/`nextElementSibling` hit the new wrapper. That fix was local to `insertACopyAction` and did not touch `getParent`/`getDropNode`. `getNextElementSibling` and the "drop directly on the node" branch (lines 183-189, which drills into wrappers with `querySelector('[svy-id]')`) already handle the wrapper. `getParent` does not.

**Blast radius of the same bug:** `getDropNode` is also used by `palette.component.ts` (lines 343 and 495). So dragging a layout container from the palette onto a nested container that rejects that child type can also resolve to a wrapper and drop at the form root. Any drag where `getParent` has to climb out of an element nested in a layout container is affected.

## Ticket premise check
The ticket only reports a symptom, and the symptom is real. It comes from this project's designer code, not from user misuse or the `12grid` package. The `aggrid1` in row 3 is incidental: the bug depends only on where the drop target resolves, not on what the rows contain.

## Approaches considered
1. **Make `getParent` skip wrapper elements** (recommended). When `getParent` meets a `.svy-responsive-wrapper` (or more generally an element with no `svy-id` that is not the `.svy-form` root), go on to its `parentElement` before checking allowed children.
   - Pros: a small change in the one function that broke, matching how SVY-21578 handled the wrapper. It fixes both the move and the palette drop paths. Behaviour at the form root is unchanged: top-level items are not wrapped (template lines 80-82), and `getParent(.svy-form)` still returns the form. It is easy to cover with a Vitest regression test in `designerutils.service.spec.ts` using a real wrapped DOM.
   - Cons: other wrapper-blind DOM walks may still exist elsewhere and need a separate audit.
2. **Stop `getAllowedChildrenForContainer` from falling back to `'topContainer'` for elements without a layout name** (only when they also have no `svy-id`).
   - Pros: removes the specific wrong "the wrapper accepts top containers" decision.
   - Cons: that fallback is also how the form root and components are handled today. Changing it is broader and riskier, and the wrapper would still be the starting point of the climb.
3. **Remove the wrapper and implement SVY-21248's invisible indicator another way** (for example `ngClass` on the child template's root element).
   - Pros: removes the root of the whole class of bugs (SVY-21578, SVY-21625, and any not found yet).
   - Cons: much larger change. Every child template (components, form components, CSS-position containers) would need to apply the class itself. It reworks a feature that just shipped, and SVY-21578 already adapted code to the wrapper.
4. **No code change.**
   - Pros: none worth having.
   - Cons: reordering rows or containers by drag-and-drop in responsive forms has been broken since 2026.9 and silently moves elements to the form root, a structural change the user didn't ask for.

## Recommendation
**PROCEED with approach 1.** In `DesignerUtilsService.getParent`, step over `.svy-responsive-wrapper` (or any non-form element with no `svy-id`) to its `parentElement` before resolving allowed children.

Add a Vitest regression test with the real SVY-21248 wrapped DOM: `container > wrapper > row1/row2/row3`, dragging `row3` over `row2`. Assert that `getDropNode` returns the container as `dropTarget` and `row2` as `beforeChild`; this test should fail on the current code. Then check by hand with the attached `row_reorder.servoy`: drag row3 between row1 and row2, drop above row1, drop after row2, and drag a Row from the palette onto a column and onto a row.

Approach 3 is the more thorough long-term option. It is worth raising with the SVY-21248 author, but it is too big for a minor-priority regression fix. Separately, consider a quick audit for other `parentElement` walks in the RFB code that assume a layout container's child is its direct DOM child.

## Git history findings
- `18f0c91473` (emera, 2026-07-28, "SVY-21248 add invisible_element indicator for responsive layout in designer [ai]") added the `.svy-responsive-wrapper` div (no `svy-id`) around every child in `#svyResponsiveDiv` and gave it `display: contents`. It is in tags `2026.9_RC1`, `2026.9_RC2` and `2026.9.0`, which matches a regression reported on 2026-10-15.
- `c2a38d6ffa` (emera, 2026-10-12, SVY-21578) fixed the same wrapper bug in `insertACopyAction` only. Its commit message names the wrapper explicitly.
- `getParent` was last changed functionally in `e8929eed4b` (SVY-20282, 2025-06-24) and `0708bf8146` (SVY-19767, 2024-12-04). Neither knew about the wrapper. `getDropNode`'s relevant branch was last changed in `c3856052fc` (SVY-18740/SVY-18732, 2023-12) and `2309fcc01b` (SVY-20621, scroll offset only).
- No target-platform or dependency change is involved. The regression is purely a frontend DOM-structure change.

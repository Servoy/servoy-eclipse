# Triage Report — SVY-21578

**Verdict:** PROCEED

## Reported problem
In a responsive form editor, when a column (grid column / layout container) is
selected it shows an inline toolbar with two buttons: "Insert a &lt;container&gt;
before" and "Insert a &lt;container&gt; after". Clicking either button inserts the
new column **outside** its parent container instead of next to the selected column
inside the same container. The reporter notes this is a **regression from 2026.06**
(worked in `.06`, broken in `.09`). Attachment `responsive.jpg` shows the selected
column's toolbar with the "Insert a 12grid.column before" tooltip.

The ticket reports the symptom only; it proposes no solution.

## Root-cause assessment

The insert action is handled front-end in
`com.servoy.eclipse.designer.rfb/node/src/designer/mouseselection/mouseselection.component.ts`,
`insertACopyAction(event, node, before)` (around lines 501-520). It computes the
drop target from the selected element's DOM parent:

```ts
const htmlNode = this.editorContentService.getContentElement(node.svyid);
...
const droptarget = (htmlNode.parentNode as HTMLElement).getAttribute('svy-id');
if (droptarget) component['dropTargetUUID'] = droptarget;
if (before) {
    component['rightSibling'] = node.svyid;
} else if (htmlNode.nextElementSibling) {
    component['rightSibling'] = htmlNode.nextElementSibling.getAttribute('svy-id');
}
```

`htmlNode` is the selected column's content element — the `.svy-layoutcontainer`
`<div>` that carries the `svy-id` attribute (rendered in the content iframe by
`designform_component.component.ts` via `[svyContainerAttributes]="state.attributes"`).

The regression was introduced by **SVY-21248** (commit `18f0c91473`, "add
invisible_element indicator for responsive layout in designer", 2026-07-28), which
is present in `2026.9_RC2` but **not** in `2026.06.0`. That change wrapped every
child of a responsive container in an extra intermediate div inside the
`#svyResponsiveDiv` template of
`com.servoy.eclipse.ngclient.ui/node/src/designer/designform_component.component.ts`:

```html
@for (item of state.items; track item) {
  <div class="svy-responsive-wrapper" [ngClass]="{ invisible_element: item.model?.svyVisible === false }">
    <ng-template [ngTemplateOutlet]="getTemplate(item)" .../>
  </div>
}
```

That wrapper `<div class="svy-responsive-wrapper">` has **no `svy-id` attribute**.

Consequence for `insertACopyAction`:
- `htmlNode.parentNode` is now the `.svy-responsive-wrapper` div (no `svy-id`),
  not the real parent container. `(...).getAttribute('svy-id')` returns `null`,
  so `dropTargetUUID` is never set.
- For the "after" case, `htmlNode.nextElementSibling` is now the *next wrapper* div
  (also no `svy-id`), so `rightSibling` is `null` too.

On the Java side, `CreateComponentCommand.createComponent` (…/editor/rfb/actions/
handlers/CreateComponentCommand.java) only descends into the selected container when
a `dropTargetUUID` is supplied: with `args.getDropTarget()` null it leaves
`parentSupportingElements = form` (lines 234-257) and creates the new layout
container on the form root (lines 550-634) — i.e. **outside** the intended
container. That is exactly the reported symptom.

The responsive drag-and-drop path is unaffected because it already resolves the
owning container with a wrapper-safe lookup —
`this.dragNode.parentElement?.closest('[svy-id]')`
(dragselection-responsive.component.ts:158) — rather than a raw `parentNode`. The
inline insert-before/after action is the only place still using the bare
`parentNode`/`nextElementSibling`, which is why only that path regressed.

The two 2026.06→2026.09 changes that touched `CreateComponentCommand.java`
(`23ffcbda1c` designsize read; `e22c704b62` SVY-21386, which is on master but **not**
in `2026.9_RC2`) are unrelated to this symptom — they affect sizing and
palette-bottom-drop ordering, not parent resolution.

## Ticket premise check
The ticket proposes no solution, so there is no premise to overturn — but it does
assert "regression from .06", and that assertion holds up: the breaking change
(SVY-21248) landed between `2026.06.0` and `2026.9_RC2`. The natural assumption that
the bug is in the Java insert command is **not** where the fix belongs: the Java
command behaves correctly for the arguments it receives; it is the front-end that
stopped sending `dropTargetUUID`/`rightSibling` because the DOM gained an
unidentified wrapper div. The fix is front-end, in the designer RFB project.

## Approaches considered

1. **Fix `insertACopyAction` to resolve the real container via `closest('[svy-id]')`
   (recommended).** Replace `htmlNode.parentNode` with
   `htmlNode.parentElement?.closest('[svy-id]')` for the drop target, and use a
   wrapper-safe sibling lookup for the "after" case (e.g. the existing
   `DesignerUtilsService.getNextElementSibling(...)` already used by the drag path,
   or walk up to the wrapper then take its `nextElementSibling`'s `[svy-id]`
   descendant). Mirrors the proven drag-and-drop logic.
   - Pros: tiny, localized, consistent with the existing drag path; no server
     change; low blast radius; the `rightSibling`/`dropTargetUUID` contract to Java
     is unchanged.
   - Cons: must handle the "after" sibling correctly through the wrapper (the
     `nextElementSibling` is a wrapper, so its `svy-id` child must be read).

2. **Give the `.svy-responsive-wrapper` div the child's `svy-id` (or drop the extra
   wrapper and move `invisible_element` onto the existing node).** Remove the DOM
   ambiguity at the source introduced by SVY-21248.
   - Pros: fixes the root structural change; any other code relying on `parentNode`
     having an `svy-id` benefits.
   - Cons: higher risk — the wrapper exists specifically to carry the
     `invisible_element` visual indicator (SVY-21248) and selection relies on only
     real nodes carrying `svy-id`; duplicating `svy-id` onto the wrapper could make
     the wrapper selectable/queryable and regress SVY-21248 or selection logic.
     Broader blast radius across both Angular projects.

3. **Make the Java `CreateComponentCommand` fall back to the selected persist's
   container when `dropTargetUUID` is absent.** Infer the parent server-side.
   - Pros: resilient to future DOM changes.
   - Cons: wrong layer — the command legitimately treats "no drop target" as
     "drop on form"; changing that would alter palette/other callers that rely on
     the form-root fallback. Masks the real front-end bug.

4. **No code change.**
   - Pros: none.
   - Cons: a real, reproducible functional regression; inserting a column via the
     documented toolbar button places it in the wrong parent. Not acceptable.

## Recommendation
**PROCEED** with **Approach 1** — fix `insertACopyAction` in
`com.servoy.eclipse.designer.rfb/node/src/designer/mouseselection/mouseselection.component.ts`
to resolve the drop-target container with a wrapper-safe
`parentElement?.closest('[svy-id]')` lookup (and a wrapper-safe next-sibling lookup
for the "after" branch), matching the existing responsive drag path. It is the
smallest, lowest-risk change, keeps SVY-21248's `.svy-responsive-wrapper` indicator
intact, and requires no server-side change.

Alternatives: Approach 2 (remove/annotate the wrapper) is a legitimate but
higher-risk structural fix that risks regressing SVY-21248 and selection; Approach 3
(server-side fallback) is the wrong layer. Both are inferior to Approach 1.

A frontend Vitest unit test for `insertACopyAction` should accompany the fix: assert
that with a selected column whose content element sits inside a
`.svy-responsive-wrapper` (no `svy-id`) inside a parent `.svy-layoutcontainer` (with
`svy-id`), the `createComponent` payload carries the parent's `svy-id` as
`dropTargetUUID` and the correct `rightSibling` for both before and after. The test
should fail against the raw-`parentNode` implementation.

## Git history findings
- **Regression introducer:** `18f0c91473` — "SVY-21248 add invisible_element
  indicator for responsive layout in designer [ai]" (emera, 2026-07-28). Added the
  `<div class="svy-responsive-wrapper">` child wrapper (with no `svy-id`) in the
  `#svyResponsiveDiv` template of
  `com.servoy.eclipse.ngclient.ui/node/src/designer/designform_component.component.ts`.
  Confirmed present in `2026.9_RC2`, absent in `2026.06.0`
  (`git merge-base --is-ancestor`).
- `insertACopyAction` itself was last functionally touched by `a152102b08`
  (SVY-16451) and is otherwise unchanged between `2026.06.0` and `2026.9_RC2` apart
  from the zoneless/signals migration (`node.svyid` typing); the raw
  `parentNode`/`nextElementSibling` logic is identical in both tags, so the behavior
  change comes entirely from the DOM structure change in SVY-21248.
- `CreateComponentCommand.java` diffs between the two tags (`23ffcbda1c` designsize,
  and `e22c704b62`/SVY-21386 which is on master only) are unrelated to parent
  resolution.
- No prior `docs/SVY-21578*` spec exists.

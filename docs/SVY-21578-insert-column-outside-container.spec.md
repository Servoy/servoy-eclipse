# Spec: SVY-21578 — Insert column is done outside the container

## 1. Goal
In the responsive form editor, when a column (grid column / layout container) is selected it
shows an inline toolbar with "Insert a &lt;container&gt; before" and "Insert a
&lt;container&gt; after" buttons. Clicking either button must insert the new column **next to
the selected column, inside the same parent container** — not on the form root. Today the new
column lands outside the parent container. This is a functional regression from 2026.06 (worked
in `.06`, broken from `.09`). The fix restores the correct placement with a small, frontend-only
change.

## 2. Background

### 2.1 Where the action lives
The inline insert-before/after action is handled entirely in the RFB designer Angular frontend,
in `com.servoy.eclipse.designer.rfb/node/src/designer/mouseselection/mouseselection.component.ts`,
method `insertACopyAction(event, node, before)` (currently lines 501–520). It builds a
`component` payload and calls `this.editorSession.createComponent(component)`. The two fields that
decide placement are:

- `dropTargetUUID` — the `svy-id` of the parent container the new element should go **into**.
- `rightSibling` — the `svy-id` of the element the new one should be inserted **before**.

Current implementation:

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

`htmlNode` is the selected column's content element — the `.svy-layoutcontainer` `<div>` carrying
the `svy-id` attribute, rendered in the content iframe by
`com.servoy.eclipse.ngclient.ui/node/src/designer/designform_component.component.ts`.

### 2.2 Root cause (regression from SVY-21248)
SVY-21248 (commit `18f0c91473`, "add invisible_element indicator for responsive layout in
designer", present in `2026.9_RC2`, absent in `2026.06.0`) wrapped every child of a responsive
container in an extra intermediate `<div class="svy-responsive-wrapper">` inside the
`#svyResponsiveDiv` template of `designform_component.component.ts`:

```html
@for (item of state.items; track item) {
  <div class="svy-responsive-wrapper" [ngClass]="{ invisible_element: item.model?.svyVisible === false }">
    <ng-template [ngTemplateOutlet]="getTemplate(item)" .../>
  </div>
}
```

That wrapper `<div>` has **no `svy-id` attribute**. As a result, in `insertACopyAction`:

- `htmlNode.parentNode` is now the `.svy-responsive-wrapper` div (no `svy-id`), not the real
  parent container, so `(...).getAttribute('svy-id')` returns `null` and `dropTargetUUID` is never
  set.
- For the "after" case, `htmlNode.nextElementSibling` is now the *next wrapper* div (also no
  `svy-id`), so `rightSibling` resolves to `null`.

On the Java side, `CreateComponentCommand.createComponent`
(`com.servoy.eclipse.designer.rfb/.../editor/rfb/actions/handlers/CreateComponentCommand.java`)
only descends into the selected container when a `dropTargetUUID` is supplied. With no drop
target it leaves `parentSupportingElements = form` and creates the new container on the form
root — i.e. **outside** the intended container. That is exactly the reported symptom. The Java
command is behaving correctly for the (now-wrong) arguments it receives; the bug is that the
frontend stopped sending `dropTargetUUID`/`rightSibling`.

### 2.3 The proven, wrapper-safe pattern already exists
The responsive **drag-and-drop** path never regressed because it already resolves the owning
container with a wrapper-safe lookup instead of a bare `parentNode`:

- Drop target: `this.dragNode.parentElement?.closest('[svy-id]')`
  (`dragselection-responsive.component.ts:158`).
- Next sibling: `DesignerUtilsService.getNextElementSibling(element)`
  (`designerutils.service.ts:379`), which walks up through wrapper divs that lack an `svy-id`,
  takes the next element sibling, and — if that sibling is itself a wrapper — reads its
  `svy-id`-bearing descendant.

The inline insert-before/after action is the only remaining place using the raw
`parentNode`/`nextElementSibling`, which is why only it regressed. The fix is to make
`insertACopyAction` use the same two helpers.

## 3. Design

### 3.1 Resolve the drop-target container wrapper-safely
Replace the raw-parent lookup with a `closest('[svy-id]')` walk that skips the
`.svy-responsive-wrapper` div and lands on the real parent container:

```ts
const droptarget = htmlNode.parentElement?.closest('[svy-id]')?.getAttribute('svy-id');
if (droptarget) component['dropTargetUUID'] = droptarget;
```

This mirrors `dragselection-responsive.component.ts:158`. When the selected column already sits
directly under an `svy-id` parent (non-wrapper case, e.g. older/absolute layouts), `closest`
still returns that parent, so behavior is unchanged there.

### 3.2 Resolve the "after" sibling wrapper-safely
For the `before === false` branch, replace `htmlNode.nextElementSibling` with the existing
`DesignerUtilsService.getNextElementSibling(htmlNode)`, which already handles the wrapper layer:

```ts
if (before) {
    component['rightSibling'] = node.svyid;
} else {
    const nextSibling = this.designerUtilsService.getNextElementSibling(htmlNode);
    if (nextSibling) {
        component['rightSibling'] = nextSibling.getAttribute('svy-id');
    }
}
```

`this.designerUtilsService` is already injected in the component (field at line 43), and
`getNextElementSibling` is already used by the drag path, so no new dependency or helper is
introduced. When there is no following sibling (last child), `rightSibling` stays unset, which is
correct — Java appends to the end of the container.

### 3.3 No server-side change
The `dropTargetUUID`/`rightSibling` contract to `CreateComponentCommand.java` is unchanged; the
Java command already does the right thing when those values are populated. SVY-21248's
`.svy-responsive-wrapper` indicator is left fully intact. The change is confined to one method in
one TypeScript file.

## 4. Implementation plan

1. **Edit** `com.servoy.eclipse.designer.rfb/node/src/designer/mouseselection/mouseselection.component.ts`,
   method `insertACopyAction` (lines ~501–520):
   - Change the drop-target resolution from
     `(htmlNode.parentNode as HTMLElement).getAttribute('svy-id')` to
     `htmlNode.parentElement?.closest('[svy-id]')?.getAttribute('svy-id')`.
   - Change the "after" sibling resolution from `htmlNode.nextElementSibling.getAttribute('svy-id')`
     to `this.designerUtilsService.getNextElementSibling(htmlNode)?.getAttribute('svy-id')`
     (guarding for a missing sibling as today).
   - Keep `before` → `rightSibling = node.svyid` and `keepOldSelection = true` unchanged.

2. **Add a Vitest unit test** for `insertACopyAction` in
   `com.servoy.eclipse.designer.rfb/node/src/designer/mouseselection/mouseselection.component.spec.ts`
   (file already exists):
   - Build a DOM where the selected column's content element (`.svy-layoutcontainer`, carrying
     `svy-id` + `svy-layoutname`) sits inside a `.svy-responsive-wrapper` div (no `svy-id`) inside a
     parent `.svy-layoutcontainer` that **does** carry an `svy-id`; include a following sibling
     column wrapped the same way.
   - Instantiate the component with the project's unit-test pattern
     (`Object.create(MouseSelectionComponent.prototype)`), inject a stub
     `editorContentService.getContentElement(svyid)` returning the selected node, the real
     `DesignerUtilsService` (or a minimal stub exposing `getNextElementSibling`), and a stub
     `editorSession.createComponent` spy.
   - Assert that calling `insertACopyAction(event, node, true)` produces a payload with
     `dropTargetUUID === <parent svy-id>` and `rightSibling === node.svyid`.
   - Assert that calling `insertACopyAction(event, node, false)` produces a payload with
     `dropTargetUUID === <parent svy-id>` and `rightSibling === <next column svy-id>` (read through
     the wrapper).
   - The test must **fail** against the raw-`parentNode`/`nextElementSibling` implementation
     (where `dropTargetUUID`/`rightSibling` would be `null`/undefined) and **pass** after the fix.

3. **Verify** per `com.servoy.eclipse.designer.rfb/node/AGENTS.md`:
   - `npm run lint` — zero warnings.
   - `npm run build_debug_nowatch` — compiles.
   - `npm test` — all unit tests pass, including the new one.

## 5. Acceptance criteria
- [ ] In a responsive form editor, selecting a column and clicking "Insert a &lt;container&gt;
      before" inserts the new column **inside the same parent container**, immediately before the
      selected column.
- [ ] Clicking "Insert a &lt;container&gt; after" inserts the new column **inside the same parent
      container**, immediately after the selected column.
- [ ] When the selected column is the last child, "after" appends the new column at the end of the
      same parent container (not on the form root).
- [ ] `insertACopyAction` sends a non-null `dropTargetUUID` (the parent container's `svy-id`) in the
      responsive case where the content element is wrapped in `.svy-responsive-wrapper`.
- [ ] SVY-21248's `.svy-responsive-wrapper` invisible-element indicator continues to work (no change
      to `designform_component.component.ts`).
- [ ] A Vitest unit test for `insertACopyAction` covers before/after through the wrapper and fails on
      the pre-fix implementation.
- [ ] `npm run lint`, `npm run build_debug_nowatch`, and `npm test` all pass.

## 6. Out of scope
- Removing or re-annotating the `.svy-responsive-wrapper` div (Approach 2 from triage) — higher risk,
  could regress SVY-21248 and selection logic.
- Any server-side change to `CreateComponentCommand.java` or a Java-side parent fallback
  (Approach 3) — wrong layer; the command legitimately treats "no drop target" as "drop on form".
- The absolute-layout insert path (not affected by the responsive wrapper).
- Changes to the drag-and-drop path (already correct).

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| None — approach, root cause, and fix location are confirmed by triage. | — | closed |

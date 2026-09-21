# Spec: SVY-21483 — Responsive drag preview sized from model instead of rendered element

## 1. Goal

In the responsive form editor, when a component (e.g. a button) is dragged from one
row to another, the drag "hover" preview must render at the **same size the element
actually has on the form**, not at its (often much larger) persisted design `model.size`
or the `200 × 100` fallback. This is achieved by measuring the real rendered size of the
dragged element in the responsive drag source and passing it through the
`createDraggedComponent` iframe message so the ngclient content iframe sizes the preview
to match — mirroring what the already-correct absolute-layout drag path does with
`getBoundingClientRect()`. The existing `model.size` / `200 × 100` fallback is preserved
for the case where no measured size is supplied.

## 2. Background

### 2.1 Architecture of the responsive drag preview

The form designer is a two-layer Angular system communicating over `postMessage`:

- **RFB designer** (`com.servoy.eclipse.designer.rfb/node/`) runs the drag interaction.
  `DragselectionResponsiveComponent.onMouseMove()`
  (`dragselection-responsive.component.ts:113`) detects the start of a responsive drag
  and posts a `createDraggedComponent` message to the content iframe carrying only the
  element's `svy-id` (`uuid`) and the `dragCopy` flag — **no size information**.
- **ngclient content iframe** (`com.servoy.eclipse.ngclient.ui/node/src/designer/`)
  handles that message in `DesignFormComponent`
  (`designform_component.component.ts:329`). For a non-layout component it builds the
  preview `draggedElementItem` (a `ComponentCache`) whose `layout` width/height come from
  the persisted `model.size`, falling back to `200 × 100`
  (`designform_component.component.ts:352-356, 372-379`):

  ```ts
  const elWidth  = (this.insertedClone as ComponentCache).model.size ? ...size!.width  : 200;
  const elHeight = (this.insertedClone as ComponentCache).model.size ? ...size!.height : 100;
  (this.insertedClone as ComponentCache).layout = { width: elWidth + 'px', height: elHeight + 'px' };
  ```

- The preview is rendered by the responsive template block as an absolutely-positioned
  `#svy_draggedelement` wrapper whose width/height come from that `layout`
  (`designform_component.component.ts:75-79`), applied by the `svyContainerLayout`
  handling in `AddAttributeDirective.ngOnChanges`
  (`addattribute.directive.ts:36-47`).

### 2.2 Root cause

In a responsive (flex) layout the actual rendered size of a button is determined by the
flex container and CSS, and is usually far smaller than its design-time `model.size`
(or the `200 × 100` fallback when `model.size` is unset). Because the preview is sized
from the model rather than from the element **as actually rendered**, it appears "huge"
compared to the original — exactly the reported symptom (`hover.jpg`, reproducible with
the attached `respSmp.servoy`).

The **absolute-layout** drag path
(`com.servoy.eclipse.designer.rfb/node/src/designer/dragselection/dragselection.component.ts`)
does not have this problem because it drags the real DOM node (or a `cloneNode(true)` of
it) and measures it with `getBoundingClientRect()` (`dragselection.component.ts:229`), so
its preview always matches the rendered size. Only the responsive path constructs a
preview sized independently of the live element.

### 2.3 "Sudden regression" investigation (Angular 22 / SVY-19023) — evidence

The reporter believes this appeared "all of a sudden" and suspects the recent Angular 22
migration (SVY-19023). **This is correct — the migration is the regression trigger.** The
preview-sizing *code* is old, but the migration removed the data it depended on, which is
what changed the behaviour.

**Root of the regression — `size` removed from the client model (`872f86e`, SVY-19023).**
As part of the Angular 22 migration, commit
`872f86e SVY-19023 remove size/location from component specs - handled as internal by
framework` deleted the explicit `size` model property from 21 bootstrap component specs.
For the button (`bootstrapcomponents/components/button/button.spec`) it removed:

```json
"size" : {"type" :"dimension", "default" : {"width":80, "height":30}},
```

and replaced it with a **server-only** design size (`f9d70be` / `9033faf`):

```json
"designsize" : {"type":"dimension", "tags": {"serveronly": true, "scope": "private"}, "default" : {"width":80, "height":30}},
```

`designsize` carries the `"serveronly": true` tag, so it is consumed only by Java
(`DesignerFilter`, `CreateComponentCommand`, `ElementFactory` via
`IContentSpecConstants.PROPERTY_DESIGN_SIZE`) and is **never sent to the browser client
model**. Consequently, at runtime in the content iframe, `model.size` is now `undefined`
for these components.

**Effect on the preview.** The responsive preview code has always done
(`designform_component.component.ts:353-354`):

```ts
const elWidth  = model.size ? model.size.width  : 200;
const elHeight = model.size ? model.size.height : 100;
```

- **Before the migration:** the button model had `size` defaulting to `80 × 30`, so the
  preview was sized `80 × 30` — a close match to how a small button renders, so the bug
  was not visible.
- **After the migration:** `model.size` is `undefined`, so every migrated component now
  falls into the **`200 × 100` fallback** — producing the oversized "hover" preview in
  `hover.jpg`.

So the reporter's instinct is right: the Angular 22 migration (`872f86e`) is the trigger.
The pre-existing `200 × 100` fallback was latent and only became reachable once `size`
disappeared from the client model.

**Why still fix by measuring the rendered element (not by restoring `size`).** Restoring a
client `size` model property would re-add design-time size data that the framework now
deliberately keeps server-only, and it would still be the *design* size, not the size the
element actually renders at inside a responsive flex row (which is what must be previewed).
Measuring the live element (as the absolute-layout path already does with
`getBoundingClientRect()`) fixes the root cause robustly regardless of whether `model.size`
is present, and does not depend on per-component spec data.

**Corroborating history (the code path itself is old):**
- The `model.size` → `layout` sizing with `200 × 100` fallback dates to
  `c0234dcf3f SVY-16453 [ng2 designer] implement dnd from palette in responsive layout` and
  was carried through `33ac41ff48 SVY-20562`, `d1794cf2fd` / `48c3906039` SVY-19023.
- The responsive `#svy_draggedelement` template block and the
  `AddAttributeDirective.svyContainerLayout` handling were only reformatted / made
  standalone by SVY-19023 (`ac7d94206b`, `d1794cf2fd`); their behaviour is unchanged.
  Signal inputs in Angular 22 still fire `ngOnChanges`, so the layout is applied the same
  way. What changed is the *input value* — `model.size` went from `80 × 30` to `undefined`.

**Conclusion:** genuine regression introduced by the Angular 22 migration commit
`872f86e` (removal of `size` from the client model), which unmasked the pre-existing
`200 × 100` responsive-preview fallback. The fix below addresses it at the correct layer by
sizing the preview from the element as actually rendered.

## 3. Design

Mirror the absolute-layout path: measure the dragged element's real rendered size at drag
start (the `dragNode` is already available), send it in the `createDraggedComponent`
message, and prefer it over `model.size` when building the preview `layout`. Preserve the
existing `model.size` / `200 × 100` fallback for when no measured size is supplied.

### 3.1 RFB designer — measure and send the rendered size

In `dragselection-responsive.component.ts`, in `onMouseMove()` at the point the drag is
started and `createDraggedComponent` is posted (currently line 113):

- Compute the rendered size of the dragged element. The element node is `this.dragNode`.
  The code already accounts for `dragNode.clientWidth == 0 && dragNode.clientHeight == 0`
  when choosing a highlight clone (lines 87-93); apply the same intent when measuring:
  - Prefer `this.dragNode.getBoundingClientRect()` → `{ width, height }`.
  - If that yields a zero width/height (a wrapper with no box of its own), fall back to
    the first element child's / parent's `getBoundingClientRect()` consistent with the
    existing highlight-element selection at lines 87-93, so the measured size matches the
    visible element.
- Only include the size in the message when it is a positive, finite width **and** height
  (do not send `0 × 0` — that must fall through to the model/`200 × 100` fallback on the
  iframe side).
- Add the size to the `createDraggedComponent` payload as a new **optional** field, e.g.
  `size: { width: number, height: number }` (numbers, in px).

The message becomes:
```ts
this.editorContentService.sendMessageToIframe({
  id: 'createDraggedComponent',
  uuid: this.dragNode.getAttribute('svy-id'),
  dragCopy: this.dragCopy,
  size: measuredSize // optional; omitted / undefined when not measurable
});
```

### 3.2 ngclient content iframe — prefer the measured size

In `designform_component.component.ts`, in the `createDraggedComponent` handler, in the
non-layout (component) branch (currently lines 352-356), change the width/height source to
prefer the incoming measured size, then fall back to `model.size`, then to `200 × 100`:

- If `event.data.size` is present with positive width/height, use those numbers.
- Else if `model.size` is present, use it (existing behaviour).
- Else use `200 × 100` (existing fallback).

The resulting `layout = { width: w + 'px', height: h + 'px' }` continues to feed
`draggedElementItem.layout`, so the rest of the flow (template `#svy_draggedelement`,
`svyContainerLayout`) is unchanged.

### 3.3 Scope of the size preference

- Apply the measured-size preference **only** to the component (non-layout) branch — the
  branch that currently uses `model.size`. The layout-container branch
  (`designform_component.component.ts:342-348`) does not size from `model.size` and must
  not be altered.
- The absolute-layout drag path (`dragselection.component.ts`) is already correct and is
  **not** touched.
- The `createElement` (palette-drop) path, which also derives size from
  `event.data.model.size` with a `200 × 100` fallback, is **not** in scope — this fix is
  about dragging an existing, already-rendered element within a responsive form. (See
  SVY-21294 precedent: the *variant palette* case correctly uses the model's pre-computed
  size; the present case is the opposite — a live component whose rendered size is the
  correct source.)

## 4. Implementation plan

Ordered, concrete changes across the two known files:

1. **`com.servoy.eclipse.designer.rfb/node/src/designer/dragselection-responsive/dragselection-responsive.component.ts`**
   - In `onMouseMove()`, just before/at the existing `sendMessageToIframe({ id:
     'createDraggedComponent', ... })` call (line 113), measure the dragged element's
     rendered size via `this.dragNode.getBoundingClientRect()`, applying the same
     zero-size fallback intent already used for `highlightEl` (lines 87-93: firstElementChild
     or parentElement when `dragNode.clientWidth/clientHeight` are 0).
   - Build an optional `size: { width, height }` only when both are positive/finite.
   - Add `size` to the `createDraggedComponent` message payload.
   - Keep the change small and typed (no `any` where avoidable, per RFB AGENTS.md;
     numbers in px).

2. **`com.servoy.eclipse.ngclient.ui/node/src/designer/designform_component.component.ts`**
   - In the `createDraggedComponent` handler, non-layout (component) branch (lines
     352-356), replace the `elWidth`/`elHeight` derivation so it prefers
     `event.data.size` (positive width/height) over `model.size`, keeping the `model.size`
     then `200 × 100` fallback chain intact.
   - Do not change the layout-container branch (342-348) or the template.

3. **Verification** (per project AGENTS.md):
   - RFB: `npm run lint` (zero warnings), `npm run build_debug_nowatch`, `npm test`,
     and `npm run test:browser` if a browser test is added.
   - ngclient.ui: `npx tsc --noEmit -p src/tsconfig.app.json`, `npx ng lint`,
     and a development build.

## 5. Acceptance criteria

- [ ] When dragging an existing component between rows in a **responsive** form, the drag
      preview (`#svy_draggedelement`) renders at the same size the element has on the form
      (matches the original, per `hover.jpg` scenario, using `respSmp.servoy`), not at the
      larger `model.size` or `200 × 100`.
- [ ] `dragselection-responsive.component.ts` measures the dragged element's rendered size
      (via `getBoundingClientRect()`, with the existing zero-size child/parent fallback)
      and includes it as an optional `size` field in the `createDraggedComponent` message.
- [ ] `designform_component.component.ts` prefers `event.data.size` (when it has positive
      width/height) over `model.size` when building the component preview `layout`.
- [ ] When no measured size is supplied (field absent or non-positive), the existing
      behaviour is preserved: `model.size` if present, else `200 × 100`.
- [ ] The absolute-layout drag preview and the layout-container drag preview are unchanged.
- [ ] RFB: `npm run lint` passes with zero warnings; `npm run build_debug_nowatch`
      compiles; existing `npm test` suite passes.
- [ ] ngclient.ui: TypeScript typecheck, lint, and development build pass.

## 6. Out of scope

- Re-adding a client-side `size` model property to the bootstrap component specs (the
  framework deliberately keeps design size server-only via `designsize`; see §2.3). The fix
  is at the preview layer, not the spec layer.
- The palette-drop `createElement` sizing path (new element from palette).
- The absolute-layout drag path (`dragselection.component.ts`) — already correct.
- The variant palette drag preview (SVY-21294 territory).
- Any change to the `svyContainerLayout` directive behaviour or the preview template
  structure.
- A broader signal migration of `DesignFormComponent` / `AddAttributeDirective`
  (unrelated to this defect).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| For a wrapper `dragNode` that reports `0 × 0`, is the firstElementChild/parentElement `getBoundingClientRect()` (mirroring the existing highlight-clone logic at lines 87-93) the right rendered size to send, or should the child component element be measured directly? | Coder / Deian | Open — default: mirror the existing highlight-clone fallback |
| Should the measured size also be applied to a `dragCopy` (Ctrl-drag) preview? | Coder | **Resolved (second-pass investigation):** yes, automatically. The `dragCopy` branch (`designform_component.component.ts:358-370`) rebuilds `insertedClone` reusing `(this.insertedClone as ComponentCache).layout` — the same `layout` field computed just above it in the non-copy branch (lines 352-356). Since the fix only changes how that `layout` is computed (preferring `event.data.size`), the copy path is covered with no extra code. |

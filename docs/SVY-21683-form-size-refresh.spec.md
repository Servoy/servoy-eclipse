# Spec: SVY-21683 — Form resize by changing width in properties view needs refresh

## 1. Goal

On a CSS-position (absolute-layout) form open in the Servoy form editor, editing the
form's **width**/**height** (shown under the "minWidth"/"minHeight" labels in the
Properties view) must update the editor's rendered form size **immediately**, without
needing a manual editor refresh. The new size already reaches the Angular RFB designer;
the fix makes the `EditorContentComponent` re-render its `[ngStyle]` size binding when the
`updateFormSize` message arrives under zoneless change detection. Scope is isolated to the
RFB Angular frontend — no Java-side change.

## 2. Background

### 2.1 The size-change pipeline (confirmed by triage)

When the form's `width`/`height` is edited in the Properties view, the new size travels end
to end correctly up to the point of the bug:

1. The property write goes through `BasePropertyHandler.setValue(...)` and fires a
   persist-change (`ServoyModel.firePersistChanged`).
2. `BaseVisualFormEditor.persistChanges(...)` detects the form itself changed and calls
   `refresh(changedChildren, false)`.
3. `RfbVisualFormEditorDesignPage$PartListener.refreshPersists(...)` — because the persist
   list contains the form — issues `updateForm(uuid, parentUuid, width, height)` to the
   content service with the new form size.
4. In the content iframe, `EditorContentService.updateForm(...)` →
   `ServoyDesignerComponent.updateForm(...)` posts
   `{ id: 'updateFormSize', width, height }` to the parent window (the RFB designer shell).
5. In the RFB shell, the RFB-side `EditorContentService` receives that `message` event (via
   a raw `window.addEventListener('message', ...)`) and dispatches it to all
   `IContentMessageListener`s, including
   `EditorContentComponent.contentMessageReceived('updateFormSize', { width, height })`.

The Java side and the content-iframe side already deliver the new size on every edit. The
defect is entirely in step 5's Angular handler.

### 2.2 The zoneless change-detection gap (root cause)

`EditorContentComponent`
(`com.servoy.eclipse.designer.rfb/node/src/designer/editorcontent/editorcontent.component.ts`)
is `ChangeDetectionStrategy.OnPush`, and the RFB app runs **zoneless**
(`provideZonelessChangeDetection()` in `main.ts`, migrated in commit `856267353e`,
2026-08-10).

The `updateFormSize` branch of `contentMessageReceived` mutates a **plain mutable object
field** that is bound in the template:

```ts
// editorcontent.component.ts lines 80-83
if (id === 'updateFormSize' && this.urlParser.isAbsoluteFormLayout()) {
    this.contentStyle['width'] = data.width + 'px';
    this.contentStyle['height'] = data.height + 'px';
}
```

```html
<!-- editorcontent.component.html line 1 -->
<div class="content" keyboardlayout [ngStyle]="contentStyle" #element> ... </div>
```

The handler runs from a raw `window` `message` callback — **outside** any Angular
zone/event. Under zoneless OnPush, mutating a plain field there does **not** notify Angular,
so `[ngStyle]` is never re-evaluated and the editor keeps the old size until some unrelated
interaction triggers change detection (the "needs a refresh" symptom). `EditorContentComponent`
injects **no** `ChangeDetectorRef` and calls no `markForCheck()`, and `contentStyle` was not
migrated to a signal during the zoneless migration — AGENTS.md explicitly lists it among the
components still using mutable template-bound state.

Corroborating evidence this is purely a CD-notification gap, not a logic bug:

- The toolbar "Desktop/Tablet/Phone/custom width" path mutates the same `contentStyle`
  (`setContentSize`/`setContentSizeFull`) and updates live — because those run from
  Angular-handled toolbar click events, which schedule change detection.
- The sibling `GhostsContainerComponent` handles the same `updateFormSize` message but reads
  its `formWidth/formHeight` imperatively inside `renderGhosts()` (not via a template
  binding), so it is unaffected.

### 2.3 All mutators of `contentStyle`

Any fix must keep every existing writer of `contentStyle` working. The field is mutated in:

| Location | What it does |
|----------|--------------|
| field initializer (lines 22–26) | initial `{ position, top, left }` |
| `ngOnInit` (lines 54–61) | adds `width`/`height` (absolute) or `bottom`/`right`/`minWidth` (responsive) |
| `contentMessageReceived` / `updateFormSize` (lines 80–83) | sets `width`/`height` — **the broken path** |
| `setContentSizeFull` (lines 150–159) | reassigns the whole object, then `delete`s `width`/`height` |
| `setContentSize` (lines 183–195) | sets `width`/`height`, `delete`s `top`/`left`/`position`/`minWidth`/`bottom`/`right` |

`setContentSize`/`setContentSizeFull`/`getFormInitialWidth` are also called externally from
`DesignSizeService` (`setEditor` registers the component). The public method signatures must
not change.

## 3. Design

### 3.1 Chosen approach — convert `contentStyle` to a signal

Follow the triage's recommended Approach 1 and the RFB zoneless/signals direction (AGENTS.md:
"Template-bound state must be a signal for Angular to detect changes from non-Angular
contexts"). Convert `contentStyle` from a plain field to a `signal<Record<string, string>>`
and route every mutator through `.set(...)` with a fresh object (never mutate the signal's
current value in place — a new reference is what makes the signal fire and `[ngStyle]`
re-render).

- Declaration:
  ```ts
  contentStyle = signal<Record<string, string>>({
      position: 'absolute',
      top: '20px',
      left: '20px'
  });
  ```
- Template binding stays readable via the signal call:
  ```html
  <div class="content" keyboardlayout [ngStyle]="contentStyle()" #element> ... </div>
  ```
- Each mutator reads the current map, produces a **new** object with the change (spread +
  add/remove keys), and calls `this.contentStyle.set(newMap)`. For removals, build the new
  object without the deleted keys (object destructuring rest, or spread then `delete` on the
  copy) rather than `delete this.contentStyle()['x']`.

This fixes the root cause for the `updateFormSize` path (the signal write schedules CD even
from the out-of-Angular `message` callback) and keeps the toolbar paths working.

The lighter alternative — inject `ChangeDetectorRef` and call `markForCheck()` after the
`updateFormSize` mutation — is acceptable only if a full signal migration proves out of
scope; it is the smaller fix but does not advance the signals migration. Prefer the signal.

### 3.2 Why not a heavier refresh

Making `ServoyDesignerComponent.updateForm` trigger a full `contentRefresh()`/re-navigation
was considered and rejected (triage Approach 2): wrong layer, over-broad, slower, can
flicker, and still leaves the underlying CD gap for the binding. Not pursued.

### 3.3 No Java change

The persist-change → `refreshPersists` → `updateForm` pipeline already sends the new size on
every edit. No Java/content-iframe change is in scope.

## 4. Implementation plan

Edit only
`com.servoy.eclipse.designer.rfb/node/src/designer/editorcontent/editorcontent.component.ts`
and its template/spec. Use the eclipse-coder tools for the TS/HTML files (per repo AGENTS.md).

1. **Import `signal`** from `@angular/core` in `editorcontent.component.ts`.
2. **Change the field** `contentStyle: Record<string, string> = {...}` to
   `contentStyle = signal<Record<string, string>>({...})` with the same initial map.
3. **`ngOnInit`** — replace the in-place additions with a single `.set(...)`: start from the
   initial map and add `width`/`height` (absolute branch) or `bottom`/`right`/`minWidth`
   (responsive branch).
4. **`contentMessageReceived` / `updateFormSize` branch** — replace the two assignments with
   `this.contentStyle.set({ ...this.contentStyle(), width: data.width + 'px', height: data.height + 'px' });`.
5. **`setContentSizeFull`** — build the new full map
   (`{ position, top, left, right, bottom }`, no `width`/`height`) and `.set(...)` it
   instead of reassigning then `delete`-ing.
6. **`setContentSize`** — build a new map from the current one: set `width` (and `height`
   when not `auto`), omit `top`/`left`/`position`/`minWidth`/`bottom`/`right`, then
   `.set(...)`. Keep the `auto` → `adjustFromContentSize()` branch and the `setFormWidth`
   call unchanged.
7. **Template** `editorcontent.component.html` — change `[ngStyle]="contentStyle"` to
   `[ngStyle]="contentStyle()"`.
8. **Update the existing unit spec**
   `editorcontent.component.spec.ts`:
   - The mock sets `(component as any).contentStyle = {...}` (line 45) — change it to a real
     `signal({...})` (import `signal`), since the methods now call `.set()`/`()`.
   - Update the assertions that read `contentStyle['width']` to read
     `component.contentStyle()['width']` (the signal getter).
   - Keep/extend the `updateFormSize` test so it still asserts the absolute-layout path sets
     `1024px`/`768px` and the non-absolute path leaves it unchanged.
9. **Add a browser spec** `editorcontent.component.browser.spec.ts` (TestBed + real DOM,
   following `toolbarbutton.browser.spec.ts` as the SVY-21405 precedent) that:
   - creates the component with absolute layout and mocked providers,
   - calls `contentMessageReceived('updateFormSize', { width, height })` from **outside** an
     Angular event (simulating the out-of-Angular `message` callback — do **not** wrap it in
     a fixture event),
   - runs `fixture.detectChanges()` + `await fixture.whenStable()`,
   - asserts the content `div`'s actual `style.width`/`style.height` updated to the new size.
     This is the regression guard: it must fail against the pre-fix plain-field version.
   - Honor the AGENTS.md mocking rules — never `vi.stubGlobal('document'/'window', ...)`.
10. **Verify:** from `com.servoy.eclipse.designer.rfb/node/` run `npm run lint` (zero
    warnings), `npm run build_debug_nowatch`, `npm test`, and `npm run test:browser`.

## 5. Acceptance criteria

- [ ] Changing a CSS-position form's width/height ("minWidth"/"minHeight") in the Properties
      view updates the editor's rendered form size immediately, with no manual refresh
      (reproduced with the attached `smpFrmSize.servoy`).
- [ ] `EditorContentComponent.contentStyle` is a signal; `[ngStyle]` binds `contentStyle()`.
- [ ] All existing `contentStyle` mutators (`ngOnInit`, `contentMessageReceived`,
      `setContentSize`, `setContentSizeFull`) go through `.set(...)` with a new object
      reference and still produce the same resulting style maps.
- [ ] Toolbar Desktop/Tablet/Phone/custom-width/height sizing still updates the editor live
      (no regression via `DesignSizeService`).
- [ ] Public method signatures of `setContentSize`, `setContentSizeFull`,
      `getFormInitialWidth` are unchanged (still callable from `DesignSizeService`).
- [ ] Unit spec updated to the signal API and passes.
- [ ] New browser spec asserts the content div's width/height update after an out-of-Angular
      `updateFormSize` message, and fails against the unfixed version.
- [ ] `npm run lint` passes with zero warnings; build, unit tests and browser tests pass.
- [ ] No Java-side or content-iframe (`com.servoy.eclipse.ngclient.ui`) change.

## 6. Out of scope

- Any Java-side change (property handling, persist refresh, `RfbVisualFormEditorDesignPage`,
  `BaseVisualFormEditor`) — the size is already delivered correctly.
- The content-iframe side (`com.servoy.eclipse.ngclient.ui/.../designer/`).
- Migrating the other still-mutable RFB components to signals (`contextmenu`, `palette`,
  `toolbar`) — tracked separately in AGENTS.md.
- Responsive-layout `contentSizeChanged`/`adjustFromContentSize` behavior beyond making its
  `contentStyle` reads/writes consistent with the signal.
- Changing the websocket message shape or the `IContentMessageListener` contract.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Confirm `[ngStyle]="contentStyle()"` keeps identical rendering for the responsive branch (`bottom`/`right`/`minWidth`) as the old plain-object binding. | Implementer | open |
| Whether a browser spec can faithfully simulate the "outside Angular" `message` dispatch, or whether asserting the signal write + one `detectChanges` is a sufficient regression guard. | Implementer | open |

# Triage Report — SVY-21683

**Verdict:** PROCEED

## Reported problem

On a CSS-position form opened in the form editor, changing **minWidth** or **minHeight**
in the Properties view does not update the editor's rendered form size. A manual refresh
(reopen/refresh the editor) is needed before the new size shows. Reproduced with the
attached solution `smpFrmSize.servoy` (a CSS-position form `frm`, size `560,480`, with a
few absolute-positioned components).

Note on terminology: for a `Form` the Properties view **labels** the `width`/`height`
persist properties as "minWidth"/"minHeight"
(`RepositoryHelper.getDisplayName(...)` in `servoy_shared`, lines 772–779 — `height` →
`minHeight`, `width` → `minWidth`). So "change minWidth/minHeight in the properties view"
means editing the form's **`width` / `height`** (its design size). This is distinct from
the boolean `useMinWidth`/`useMinHeight` form flags (`IContentSpecConstants.PROPERTY_USE_MIN_WIDTH`).
The ticket is about the form size not refreshing in the editor.

## Root-cause assessment

The size change *does* reach the Angular editor; the browser frame just isn't told to
re-render because of a **zoneless change-detection gap** introduced during the RFB
Angular-22 modernization.

Flow when the form `width`/`height` is edited in the Properties view:

1. The property write goes through `BasePropertyHandler.setValue(...)`
   (`com.servoy.eclipse.ui/.../property/BasePropertyHandler.java`), then a persist-change
   is fired (`ServoyModel.firePersistChanged` → `firePersistsChanged`).
2. `BaseVisualFormEditor.persistChanges(...)`
   (`com.servoy.eclipse.designer/.../editor/BaseVisualFormEditor.java`, lines 541–749)
   detects the form itself changed, sets `form_refresh`, adds the form to
   `changedChildren`, and calls `refresh(changedChildren, false)`.
3. `RfbVisualFormEditorDesignPage$PartListener.refreshPersists(...)`
   (`com.servoy.eclipse.designer/.../editor/rfb/RfbVisualFormEditorDesignPage.java`,
   lines 709–770) — because the persist list contains the form — issues two websocket
   service calls to the content service: `updateFormData` (component data) **and**
   `updateForm(uuid, parentUuid, width, height)` with the new form size (line 748–750).
4. In the content iframe, `EditorContentService.updateForm(...)`
   (`com.servoy.eclipse.ngclient.ui/.../designer/editorcontent.service.ts`, line 495)
   calls `designFormCallback.updateForm(width, height)`, i.e.
   `ServoyDesignerComponent.updateForm(...)`
   (`.../designer/servoydesigner.component.ts`, lines 132–142), which posts
   `{ id: 'updateFormSize', width, height }` to the **parent** window (the RFB designer
   shell) and schedules `renderGhosts` / `redrawDecorators`.
5. In the RFB designer shell, `EditorContentService` (the RFB-side one,
   `com.servoy.eclipse.designer.rfb/node/src/designer/services/editorcontent.service.ts`,
   line 36) dispatches the message to all `IContentMessageListener`s, including
   `EditorContentComponent.contentMessageReceived(...)`
   (`.../designer/editorcontent/editorcontent.component.ts`, lines 79–83):

   ```ts
   if (id === 'updateFormSize' && this.urlParser.isAbsoluteFormLayout()) {
       this.contentStyle['width']  = data.width  + 'px';
       this.contentStyle['height'] = data.height + 'px';
   }
   ```

   `contentStyle` is a **plain mutable object field** bound in the template via
   `<div ... [ngStyle]="contentStyle" #element>` (`editorcontent.component.html`, line 1).

This is where it breaks. The RFB app runs **zoneless**
(`provideZonelessChangeDetection()` in `com.servoy.eclipse.designer.rfb/node/src/main.ts`,
line 18; migrated in commit `856267353e` "migrate RFB to zoneless Angular", 2026-08-10),
and `EditorContentComponent` is `ChangeDetectionStrategy.OnPush`
(`editorcontent.component.ts`, line 16). The `updateFormSize` handler runs from a raw
`window.addEventListener('message', ...)` callback (RFB
`editorcontent.service.ts`, line 25) — **outside** any Angular zone/event. Mutating a
plain object field in that callback does **not** notify Angular, so `[ngStyle]` is not
re-evaluated and the editor keeps the old size until some other interaction (or a manual
refresh) triggers change detection. That "needs a refresh" is exactly the symptom.

Corroborating evidence that the handler itself is correct and this is purely a CD-
notification gap:
- The toolbar "Desktop/Tablet/Phone/custom width" path mutates the same `contentStyle`
  (`setContentSize`/`setContentSizeFull`, lines 149–204) and *does* update live — because
  those run from Angular-handled toolbar click events, which schedule change detection.
- The sibling `GhostsContainerComponent` handles the same `updateFormSize` message
  (`ghostscontainer.component.ts`, lines 88–92) but stores `formWidth/formHeight` as
  fields read **imperatively** inside `renderGhosts()`, not through a template binding, so
  it is not affected by the missing CD tick.
- `EditorContentComponent` injects **no** `ChangeDetectorRef` and calls no `markForCheck()`
  (confirmed by search), and `contentStyle` was **not** converted to a signal during the
  zoneless migration.

So the Java side (persist change → `refreshPersists` → `updateForm`) is behaving correctly
and already sends the new size on every edit. The defect is isolated to the Angular RFB
`EditorContentComponent` not reacting to the `updateFormSize` message under zoneless CD.

## Ticket premise check

The ticket reports only the symptom ("a refresh is needed to show the new size") and
proposes no implementation. The symptom is a real bug. The naive reading ("min-width/
min-height form flags don't refresh") is a red herring — the affected properties are the
form's `width`/`height` (design size), shown under the "minWidth"/"minHeight" labels, and
the actual fault is a zoneless change-detection miss in the RFB editor content component,
not anything in the property-handling or persist-refresh Java code.

## Approaches considered

1. **Make the RFB `EditorContentComponent` notify Angular when `updateFormSize` arrives.**
   Convert `contentStyle` to a signal (`signal<Record<string,string>>({...})`, update via
   `.set({...})`) so the `[ngStyle]` binding reacts under zoneless CD; or, minimally,
   inject `ChangeDetectorRef` and call `markForCheck()` after mutating `contentStyle` in
   the `updateFormSize` branch.
   - Pros: targeted, matches the documented RFB zoneless pattern ("use signals or
     `markForCheck()` to notify Angular of state changes in async callbacks"); fixes the
     root cause; the signal route is the direction the project is moving (AGENTS.md lists
     `EditorContentComponent` state among remaining signal migrations). No Java change.
   - Cons: must ensure the other `contentStyle` mutators (`setContentSize`,
     `setContentSizeFull`, `ngOnInit`) go through the same signal/`markForCheck()` so they
     keep working; add/extend the existing `editorcontent.component.spec.ts` (and ideally a
     `*.browser.spec.ts`) to prove the DOM updates after an out-of-Angular
     `updateFormSize`.
2. **Have `ServoyDesignerComponent.updateForm` trigger a heavier `contentRefresh()`/full
   editor refresh instead of just posting `updateFormSize`.**
   - Pros: no component-state change needed.
   - Cons: wrong layer and over-broad — it reloads form data / re-navigates for a pure size
     change, is slower, can flicker, and still doesn't fix the underlying CD gap for the
     `contentStyle` binding; treats the symptom.
3. **No code change.**
   - Pros: none.
   - Cons: the bug is real and reproducible with the attached solution; the editor shows a
     stale size until a manual refresh. Not acceptable.

## Recommendation

**PROCEED with Approach 1.** Fix the zoneless change-detection gap in
`EditorContentComponent` (`com.servoy.eclipse.designer.rfb/node/src/designer/editorcontent/editorcontent.component.ts`)
so that the `updateFormSize` message re-renders the `[ngStyle]="contentStyle"` binding.
Prefer converting `contentStyle` to a signal (updating all mutators —
`ngOnInit`, `contentMessageReceived`, `setContentSize`, `setContentSizeFull` — to
`.set(...)`), which aligns with the RFB zoneless/signals direction; the lighter
`ChangeDetectorRef.markForCheck()` after the mutation is an acceptable smaller fix if a
full signal migration of the component is out of scope. Cover it with a unit spec and,
ideally, a browser spec asserting the content `div`'s width/height actually update after an
out-of-Angular `updateFormSize` message. No Java-side change is needed — the persist-change
→ `refreshPersists` → `updateForm` pipeline already delivers the new size on every edit.

Alternatives: Approach 2 (heavier refresh in `ServoyDesignerComponent.updateForm`) only if,
unexpectedly, the signal/`markForCheck()` fix proves insufficient for some CSS-position
nuance; it is less precise and slower.

## Git history findings

- RFB was migrated to zoneless Angular in commit `856267353e`
  ("migrate RFB to zoneless Angular: signals, remove zone.js [ai]", 2026-08-10) and
  `provideZonelessChangeDetection()` is active in `main.ts` (commit `62b5a51f7c`). This is
  the regression window: before zoneless, zone.js would have caught the out-of-Angular
  `message` callback mutation and re-rendered; after it, it does not. `contentStyle` was
  left as a plain mutable field (not migrated to a signal), and AGENTS.md explicitly lists
  `EditorContentComponent` among components still using mutable template-bound state.
- The `updateForm`/`updateFormSize` wiring in
  `RfbVisualFormEditorDesignPage.refreshPersists` dates to `f9b9a34d32`
  ("SVY-17788 Autorefresh for less, css and variant.json files"); the Angular `contentMessageReceived`
  size branch predates the zoneless migration (introduced around the
  `SVY-17793`/pre-Vitest era, commit `72f081d27e` modernization kept it). The handler logic
  itself is unchanged and correct — only the surrounding change-detection model changed.
- Related recent fix in the same file family: `477aa5f32d`
  ("SVY-21255 fix column size not updated in form editor") touched the content update path
  for responsive container sizing; SVY-21683 is a different, CD-notification cause on the
  absolute/CSS-position form-size path.

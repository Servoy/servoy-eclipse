# Triage Report — SVY-21255 (corrected: canvas decorator label, not editor header)

> **This report supersedes the earlier triage of SVY-21255.** The previous analysis
> targeted the Java editor-header text `RfbVisualFormEditorDesignPage.setContentDescription`
> ("Showing container: …"). That is **NOT** the reported symptom. The real symptom is the
> stale decorator label rendered on the selection decorator in the responsive form designer
> canvas (e.g. `md-2` staying `md-2` after the column becomes `col-md-3`). Everything below
> replaces the `setContentDescription` analysis.

**Verdict:** PROCEED

## Reported problem

In the responsive form designer canvas, a bootstrap layout container (a column) draws a
small wireframe decorator label at its top-left showing its column class — e.g. `md-2`.
When the user resizes / changes the column so the Properties view now shows
`class = col-md-3`, the decorator label on the canvas **still reads `md-2`** (stale). The
resize itself and the content refresh work correctly; only the decorator text is not
updated.

- Jira status: **Reopened**. The original commit `477aa5f32d` ("fix column size not updated
  in form editor") fixed the *resize/size* update and a frontend crash guard, plus wired the
  Java editor-header content description. A reviewer (Edit Mera, 2026-08-31) found no bugs in
  that commit. The reporter (Deian Mihutoni, 2026-09-01) then reopened with:
  *"The resize is done now but the value that is on top of the column is not updated."*
- Attachment `form_editor.jpg` matches the `md-2` decorator symptom. Symptom confirmed by the
  user on reopen, so it was not re-derived from the screenshot here.

## Root-cause assessment

The decorator label is CSS-generated text:
`mouseselection.component.css:8-9` → `.decorationOverlay.showWireframe::before { content: attr(svytitle); }`.
The `svytitle` attribute on the decorator node is written in exactly one place —
`MouseSelectionComponent.applyWireframeForNode()` at `mouseselection.component.ts:410`:

```ts
this.renderer.setAttribute(selectedNode.nativeElement, 'svytitle', node.getAttribute('svy-title')!);
```

i.e. it copies the content element's `svy-title` attribute onto the decorator. This copy
runs from `applyWireframe()` (line 398-402), which is invoked from:
- `onMouseUp()` (line 386) — a fresh click/selection, and
- the constructor `effect()` (lines 49-54), which re-runs **only when `selectedRef()` (the
  `viewChildren` list of decorator nodes) changes** — that is, when decorators are
  added/removed, not when a selected element's properties change.

On a property/class change the refresh path is different and does **not** touch `svytitle`:

- server pushes an incremental update → `EditorContentService.updateFormData()`
  (`ngclient.ui/node/src/designer/editorcontent.service.ts`), which on an existing container
  sets `container.attributes = elem.attributes` (line 80) and finally calls
  `designFormCallback.redrawDecorators()` (line 445) when `redrawDecorators` is true.
- `redrawDecorators` is relayed as a `postMessage({id:'redrawDecorators'})`
  (`servoydesigner.component.ts:128-129`) → `MouseSelectionComponent.contentMessageReceived`
  (line 108-112) → `selectionChanged(sel, true)` (line 110) → `redrawDecorators()` (line 104).
- `MouseSelectionComponent.redrawDecorators()` (lines 78-97) **only recomputes each node's
  position/size style**. It never calls `applyWireframe()`/`applyWireframeForNode()`, so the
  decorator's `svytitle` attribute is never re-copied.

Because the `viewChildren` list does not change on a property update, the constructor
`effect()` does not fire either. **Net effect: the decorator keeps its old `svytitle`
("md-2") indefinitely** — the confirmed hypothesis.

### The important nuance: is the *content* element's `svy-title` even refreshed?

Yes. This was verified end-to-end and the content side is fine, so a frontend-only fix is
viable:

- The incremental update array `ng2containers` is produced by
  `ChildrenJSONGenerator.writeLayoutContainer(...)` (called from
  `DesignerWebsocketSession.java:790`). That method **does** put `svy-title` into the emitted
  `attributes` object — confirmed at
  `servoy-client/.../ChildrenJSONGenerator.java:693`:
  `attributes.put("svy-title", FormLayoutStructureGenerator.getLayouContainerTitle(layoutContainer));`
  (The `svy-title` at `DesignerWebsocketSession.java:772-773` is a *different* array — the
  first `containers` array used for full render — but `ng2containers` carries it too via
  `writeLayoutContainer`.)
- `getLayouContainerTitle` (`FormLayoutStructureGenerator.java:443-463`) strips the leading
  `col-`, so `col-md-3` → `md-3` (matching the `md-2` form seen on the label; the reporter's
  older `md-*` collapsing is only in the legacy `DesignerFilter` full-render path).
- On the client, `container.attributes = elem.attributes` assigns a **new object reference**,
  so `AddAttributeDirective.ngOnChanges` fires
  (`ngclient.ui/.../addattribute.directive.ts:48-54`) and re-applies every attribute,
  including the fresh `svy-title`, onto the content DOM element via `renderer.setAttribute`.

So after a class change the **content element's `svy-title` is correctly `md-3`**; it is only
the **decorator's copied `svytitle`** that stays stale. Re-copying it on the redraw path is
therefore sufficient.

## Ticket premise check

Premise holds and is now correctly scoped. The reopen is a genuine, still-present defect: the
decorator/wireframe label is not refreshed on a property change. It is **narrower** than the
original ticket (which conflated size, a crash guard, and the editor header) — this remaining
piece is purely the wireframe title on the selection decorator.

Regression-vs-residual: this is a **residual (pre-existing) defect**, not a regression from
the zoneless/signals migration. The migration commit `228632ce71` replaced
`selectedRef.changes.subscribe(() => applyWireframe())` with the equivalent
`effect(() => { selectedRef(); … applyWireframe(); })`. Both fire only when the decorator
**list** changes, never on a property update, so `redrawDecorators()` never re-applied the
title before or after the migration. The prior SVY-21255 fix (`477aa5f32d`) addressed size,
not the wireframe title, so it did not touch this path.

## Approaches considered

1. **No code change.** Rejected — the ticket is Reopened with a reproducible stale-label
   symptom and the content-side data is already correct, so this is a real, fixable bug.

2. **Re-apply the wireframe title in the redraw path (recommended).** In
   `MouseSelectionComponent.redrawDecorators()` (or when handling the `redrawDecorators`
   content message), after recomputing each node's position, also re-copy the fresh
   `svy-title` from the content element onto the decorator's `svytitle` attribute — i.e. reuse
   the logic already in `applyWireframeForNode()`. Smallest, most targeted change; the content
   element already carries the correct `svy-title`. Frontend-only (RFB).

3. **Re-run `applyWireframe()` from the `redrawDecorators` message handler.** Broader than #2
   — re-applies wireframe class, background color, maxLevelDesign, etc. Simpler to reason about
   but does more work than needed and slightly widens blast radius. Acceptable fallback.

4. **Server/iframe-side refresh.** Rejected as unnecessary. Verified the content element's
   `svy-title` is already refreshed correctly on the incremental update path; the gap is only
   in the decorator copy, so a server change would not address the actual defect.

## Recommendation

Approach **2** — re-apply the container title in `MouseSelectionComponent.redrawDecorators()`
by re-copying `svy-title` from the (already-fresh) content element onto the decorator's
`svytitle` attribute, reusing the guard already in `applyWireframeForNode()`
(`svy-layoutcontainer`, not `data-maincontainer`, not `svy-responsivecontainer`, non-zero
size). Keep it frontend-only in `com.servoy.eclipse.designer.rfb`.

Add a unit test alongside the existing `describe('redrawDecorators')` in
`mouseselection.component.spec.ts`: a selected layout container whose content element's
`svy-title` changes from `md-2` to `md-3` should leave the decorator node with
`svytitle === 'md-3'` after `redrawDecorators()`. This regresses cleanly against the current
code (which never sets `svytitle` in `redrawDecorators`).

## Git history findings

- `mouseselection.component.ts` `redrawDecorators` / `applyWireframeForNode` / constructor
  `effect` — last functional touches:
  - `228632ce71` "migrate last ViewChildren to signal viewChildren + effect in
    MouseSelectionComponent [ai]" — converted `@ViewChildren('selected')` +
    `selectedRef.changes.subscribe(() => applyWireframe())` into the constructor `effect()`.
    **Semantically equivalent**: both trigger only on decorator-list changes. Not the cause.
  - `856267353e`, `a65221abed` — zoneless/signal state migrations; no change to when the
    title is applied.
  - Original wireframe-title logic dates to `3494a3bf4d` "SVY-16294 [ng2 designer] implement
    responsive mode wireframe" — the `redrawDecorators`-doesn't-re-apply-title gap has existed
    since the wireframe feature was introduced.
- Prior SVY-21255 fix `477aa5f32d` — touched `editorcontent.service.ts` (null guards) and
  `RfbVisualFormEditorDesignPage.java` (editor-header content description) + a helper JUnit
  test. It did **not** touch the wireframe title copy path, consistent with the reopen.
- `DesignerWebsocketSession.java` svy-title emission (lines 772-773) and
  `ChildrenJSONGenerator.writeLayoutContainer` (`servoy-client`, line 693) — both current;
  the server side already sends the correct fresh title. No server regression.

## Questions for the reporter

None — verdict is PROCEED. (Symptom confirmed by the reporter's reopen comment and screenshot;
content-side data flow verified in code.)

# Peer-review summary — SVY-21483: Hover size for buttons during dnd is huge

**Risk: LOW.** A tightly-scoped, designer-only visual fix — the responsive drag preview now
measures the live element with `getBoundingClientRect()` instead of the now-`undefined`
`model.size` (unmasked by SVY-19023), keeping the old `model.size`/`200×100` chain as a
guarded fallback. No runtime, security, persistence, or external-API surface is touched.

**Reviewed scope:** `servoy-eclipse` commit `f2cebd0fc8` on branch `release` (6 files,
+745/-4). No sibling-repo commits.

## Manual test plan

### Verifying the fix
1. Open `respSmp.servoy` (attached to the ticket) or any responsive form with buttons in flex rows.
2. Drag a button from one row to another. The drag preview must match the button size, not the oversized ~200×100 box.
3. Repeat for a label, a text field, and a wide/tall component — each preview should match its source element.

### Layout containers (extra test — confirms the intended scope)
Layout containers stay on the old sizing path (`getMeasuredDragSize` returns `undefined` for a
zero-box element carrying `svy-layoutname`). This test verifies the behaviour and decides
whether the oversized-preview symptom is in scope for containers too.
1. Drag a whole flex row / layout container between positions in a responsive form.
2. Observe the drag preview size — confirm it is acceptable, and note whether it also looks oversized like the button symptom did before the fix.
3. Repeat with a nested container (container inside a container) to cover a more complex layout.

### Regression checks
- **Absolute-layout form:** drag a component around — preview must be unchanged (untouched code path).
- **Variant palette drag:** drag a variant onto a form — unchanged (`onVariantsMouseDown` path untouched).
- **NG client runtime:** open a deployed solution — no visual/behavioural change (the handler is designer-mode only).

### Automated checks worth running
- `com.servoy.eclipse.designer.rfb/node`: `npm run lint` and `npm test` (includes the 9 new `dragselection-responsive` cases).
- `com.servoy.eclipse.ngclient.ui/node`: the designer spec run covering `designform_component.component.spec.ts` (6 new cases).
- Both require the shared libs built first (`npm run build_libs` in `ngclient.ui/node`).

## Possible improvements / follow-ups
- **Container previews:** if the layout-container test above shows the preview is still
  oversized for containers, decide whether measuring should be extended to them (currently
  deliberately left on the old path).
- **Wire typing (non-blocking):** the new `size` field crosses the RFB→iframe `postMessage`
  boundary where both `sendMessageToIframe(message)` and `event.data` are typed `any`, so a
  `{ width, height }` shape mismatch would fail silently. Reviewer considered this acceptable
  (pre-existing project pattern for the whole designer message channel); a shared type could
  harden it later.

# SVY-21255 — Peer Review Summary

**Risk: MODERATE** — the code change is small, designer-only, and has no security relevance; the risk sits entirely in a branch mismatch: the reopen fix (`a4a725dcd8`) is on `master` only, so `release` still ships the reopened bug unless it is carried forward.

**Jira outcome:** Resolved (Fixed) on the understanding that the implementation itself is sound; the carry-forward to `release` is tracked separately (see follow-ups below) rather than blocking this review.

## Scope reviewed
- `servoy-eclipse` `f96b406781` — spec doc only (release+master)
- `servoy-eclipse` `477aa5f32d` — original fix: two null guards in `editorcontent.service.ts` + Java `RfbVisualFormEditorDesignPage.setContentDescription` + tests (release+master)
- `servoy-eclipse` `a4a725dcd8` — reopen fix: `mouseselection.component.ts` `applyWireframeTitle` helper re-copies the decorator `svytitle` on the property-change redraw path + Vitest + docs (**master only**)

## Manual test plan

### Verifying the fix
1. Open the attached `tstResizeColumn.servoy` solution, open a responsive form in the designer, turn **wireframe mode on**.
2. Zoom into a column; confirm the corner label shows its current class (e.g. `md-2`).
3. In the Properties view change the bootstrap class `col-md-2` → `col-md-3`, save the form.
4. **Expect:** the corner label updates to `md-3` **without reselecting**; the rendered content width changes; the editor header "Showing container: …" reflects the new class.
5. (Original-fix guard) Confirm no console `TypeError` and the content refreshes on resize/save.

### Regression checks
- **Wireframe off:** turn wireframe mode off, select a layout container, confirm selection/decorators behave normally and nothing visually depends on `svytitle` while off.
- **Multi-select / nested:** select two or more nested layout containers, change a class, confirm **every** selected container's label updates, not just the first (`id === svyid` matching — author's open question).
- **Background / `maxLevelDesign`:** change a property that affects a container's background on a `maxLevelDesign` container and confirm the decorator still renders correctly after redraw (the shared helper re-copies title only).
- **Zoom in/out header:** zoom in and out of containers repeatedly; confirm the header text is correct and not flickering/wrong.
- **Non-layout and responsive containers:** confirm `data-maincontainer` and `svy-responsivecontainer` elements still get no title.

### Automated checks
- RFB designer Vitest: `cd com.servoy.eclipse.designer.rfb/node && npm test` (four new `redrawDecorators` cases) + `npm run lint` + build (zero-warning policy).
- Content-iframe Vitest: `editorcontent.service.spec.ts` crash-guard case.
- Java: `com.servoy.eclipse.designer.tests` → `DesignerUtilGetLayoutContainerAsStringTest` — covers only the util, **not** the new `RfbVisualFormEditorDesignPage` header wiring.
- Run Spotbugs before merge (blocking per AGENTS.md; not run during review).

## Possible improvements / follow-ups
- **Carry-forward (the whole risk) — confirmed still needed.** Diffed `release`'s current `mouseselection.component.ts` directly against the fix: `release`'s `redrawDecorators()` still only recomputes position/size and never re-copies `svytitle`, and `applyWireframeForNode()` still writes the title unconditionally inline (the pre-fix shape). No other commit on `release` touches this path. The reopened symptom (stale `md-2` label after a class change) will reproduce on `release` today. Schedule and track the merge/cherry-pick of `a4a725dcd8` to the intended release line (ticket fix versions 2026.9.0 and 2026.12) — this is a branch/release-management follow-up, not a defect in the reviewed change.
- Confirm with the author whether dropping the `svytitle` write when wireframe is off was intended for the selection path, or only for the new redraw path (effectively a no-op on the existing path, since both selection callers already gate on `showWireframe()`).
- Resolve the two spec open questions: whether the redraw path should also re-apply background colour / `maxLevelDesign`, and whether `id === svyid` decorator matching is guaranteed 1:1 for multi-select / nested containers.
- The null guards in `editorcontent.service.ts` swallow a cache-missing entry with no logging — benign given the content refreshes by other means, but a null `newParent`/`container` could in principle hint at an upstream desync. The reviewer's noted orphaning quirk (`StructureCache.removeChild` not clearing `child.parent`) is a possible separate follow-up ticket.

## Security
No security relevance. Developer-only in-IDE tooling, no untrusted-input boundary. Both new sinks land in non-executing text (`svytitle` consumed only by CSS `attr()`, not HTML-parsed; `getLayoutContainerAsString` builds a passive Java string for an editor header).

## Reviewed scope
servoy-eclipse @ `f96b406781`, `477aa5f32d` (release+master), `a4a725dcd8` (master only).

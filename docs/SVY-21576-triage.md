# Triage Report — SVY-21576

**Verdict:** PROCEED

## Reported problem
The `getFormLayout` MCP tool returns the correct template (HTML with the right
style classes) but its computed styles and screenshot omit the rules defined in a
form's own `forms/<form>.less` (the `Form.getFormCss()` property). Solution-level
stylesheet rules (`demo.less`) resolve correctly; form-level rules do not. Because
an AI agent judges UI fidelity from this render, it reviews a layout that differs
from the running NG client — layout rules (grid, flex, padding) are exactly the
ones that go missing, which is the fidelity the agent is judged on.

The ticket frames this as a possible defect in the `getFormLayout` MCP tool and
asks (as a *suggested fix*) that the tool include the form's compiled `.less` in
the render and emit a `styleResolution` warning. It also lists secondary cosmetic
issues (one-line 79 KB JSON output, literal "DataLabel" text, opt-in screenshot
not mentioned).

## Root-cause assessment
The symptom is **not** in the MCP tool and nothing in the Servoy-Copilot bundle
needs changing. The tool renders through the same `/formtemplate` /
`AngularFormGenerator.parseLess(form.getFormCss())` path as the NG form designer's
own live preview, so it faithfully mirrors whatever the designer shows. The real
bug reproduces in the form designer itself and lives in the model layer of
servoy-eclipse.

Root cause is the SVY-21249 guard in `ServoyModel.handleChangedFiles`
(`com.servoy.eclipse.core/src/com/servoy/eclipse/core/ServoyModel.java`, the
`persist.acceptVisitor` block around lines 2841–2865):

```java
boolean ownFileChanged = true;
if (compositeWithItems)
{
    String ownFilePath = new File(solutionLocationDir.getParentFile(),
        SolutionSerializer.getRelativeFilePath(persist, false)).getAbsolutePath(); // the .frm path
    ownFileChanged = changedFilePaths.contains(ownFilePath);
}
if (compositeWithItems && !ownFileChanged)
{
    // only child script element(s) changed; do NOT update the editing composite
    persist.clearChanged();
    changed.put(persist.getUUID(), persist);
}
else
{
    IPersist editingPersist = servoyProject.updateEditingPersist(persist, compositeWithItems);
    persist.clearChanged();
    changed.put(persist.getUUID(), persist);
    changedEditing.put(persist.getUUID(), editingPersist);
}
```

The `ownFileChanged` check only recognises the composite's **own** file — the
form's `.frm`. A form's `.less` is neither the `.frm` nor a child script element.
When the LESS tab is saved, `forms/<form>.less` changes on disk;
`SolutionDeserializer` (lines 454–459 and 510–519 of `SolutionDeserializer.java`)
maps that `.less` back to the `.frm`/Form and calls `form.setFormCss(css)`, which
flags the **Form** changed — but the Form's own `.frm` did not change. The guard
therefore takes the `!ownFileChanged` skip branch: it does `persist.clearChanged()`
without calling `updateEditingPersist(...)`, so the editing Form's `formCss` is
never refreshed and the entry is never added to `changedEditing`. The designer's
`refreshPersists` (in `RfbVisualFormEditorDesignPage`,
`com.servoy.eclipse.designer`) consequently never re-pushes the recompiled form
CSS, so the preview stays stale until a reopen/restart. The running NG client is
unaffected because it reads the `.less` live from the repository blob.

The link to SVY-21249 and the regression nature are both confirmed by git history
(see below). The ticket is already linked to SVY-21249 ("relates to").

## Ticket premise check
The ticket's suggested fix (change the MCP tool to compile and inject the form
`.less`, add a resolution warning) is **the wrong target**. The MCP tool is a
faithful mirror of the designer's own preview path; "fixing" the tool would make
it diverge from the designer and mask a genuine designer bug that any user editing
a form `.less` hits. Johan's comments on the ticket already point in this direction
("this should really work fine… maybe this is more of a caching issue, the `.less`
file of a form is not picked up right away… but then it also shouldn't show in a
normal nclient launch") — which matches exactly: runtime is fine, the designer
editing-model refresh is not.

The correct fix is in `ServoyModel.handleChangedFiles` in servoy-eclipse. The
secondary cosmetic items (JSON formatting, "DataLabel" placeholder, screenshot
mention) are separate, low-value MCP-tool ergonomics and out of scope for the
regression; they can be split off if wanted.

## Approaches considered
1. **Narrow the SVY-21249 guard so a changed form `.less`/`.css` forces the full
   `updateEditingPersist`** (recommended). Before taking the `!ownFileChanged`
   skip, also treat the composite's own `.less` file as an "own file change". The
   form `.less` path is available via `SolutionSerializer.getFormLESSFile(form)`
   (returns `forms/<form>.less`); check whether that path is in `changedFilePaths`
   and, if so, fall through to the `else` branch. Keep the skip for the pure
   "only a child `.js` changed" case.
   - Pros: minimal, targeted; restores both the designer preview and
     `getFormLayout`; keeps SVY-21249's original fix intact; model-layer fix
     benefits every consumer, not just the MCP tool.
   - Cons: must be careful the `.less` path comparison uses the same base-dir
     convention as the existing `ownFilePath` (note the SVY-21249 follow-up commit
     `a9a21061c1` already fixed a path-doubling bug here — reuse
     `solutionLocationDir.getParentFile()`).
2. **Keep the guard, but in the skip branch still refresh only the editing form's
   `formCss`** (re-read + `setFormCss` on the editing persist) and add it to
   `changedEditing` so `refreshPersists` fires.
   - Pros: even more surgical about not overwriting unsaved editor state.
   - Cons: more code; duplicates part of `updateEditingPersist`; easy to drift.
3. **Fix the MCP tool to inject the compiled form `.less`** (the ticket's
   suggestion).
   - Pros: localized to the Copilot bundle.
   - Cons: wrong layer — masks a real designer regression; makes the tool diverge
     from the designer's own preview; every manual designer user still sees stale
     CSS.
4. **No code change.**
   - Pros: none.
   - Cons: a confirmed regression (works on `lts_2026`, broken on `release`) that
     breaks the form-LESS live preview in the designer for all users; rejected.

## Recommendation
**PROCEED** with Approach 1: narrow the SVY-21249 guard in
`ServoyModel.handleChangedFiles` so a changed form `.less` (not just the `.frm`)
counts as an own-file change and takes the full `updateEditingPersist` branch,
refreshing `formCss` and re-pushing the preview. Approach 2 is a viable more
surgical alternative. Do **not** change `RfbVisualFormEditorDesignPage` (byte-
identical to LTS and already correct) and do **not** change the MCP tool for the
primary issue.

Add a regression test under `com.servoy.eclipse.core.tests` (the SVY-21249 commit
`8dddf28a6f` removed its editor-side tests and the follow-up added none, so there
is currently no `handleChangedFiles` guard test): assert that a form-`.less`
change refreshes the editing form's `formCss`, while an `onAction`-only `.js`
change still does **not** overwrite unsaved editor state (SVY-21249 stays fixed).

The secondary cosmetic MCP-tool items (one-line JSON, "DataLabel" placeholder,
screenshot not surfaced) are out of scope for this regression and should be a
separate ticket if pursued.

## Git history findings
- `8dddf28a6f` (SVY-21249, "Save All button doesn't save the changes on form
  editor", 2026-08-06) introduced the `ownFileChanged` guard in
  `ServoyModel.handleChangedFiles`. Its intent was narrow: when a form gains a
  child `ScriptMethod` (`.js` written by "OK & Show") the form is flagged changed
  even though its `.frm` never changed, and the old code's full
  `updateEditingPersist` discarded the unsaved handler. The guard skips the full
  overwrite in that case.
- `a9a21061c1` (SVY-21249 follow-up, 2026-09-01) fixed a path-doubling bug in the
  same guard: `ownFilePath` must be built from `solutionLocationDir.getParentFile()`
  because `SolutionSerializer.getRelativeFilePath()` already includes the solution
  name. Any new `.less` path comparison must use the same base.
- The guard **does not exist on `lts_2026`** (`git grep ownFileChanged lts_2026`
  returns nothing), which confirms the ticket's regression claim: the edit-and-save
  LESS preview updates correctly on 2026.3 LTS and is broken on `release` purely
  because of this model-layer change. The designer refresh code is unchanged
  between the branches.
- Supporting code: `SolutionDeserializer` maps `forms/<form>.less` back to the
  Form and calls `form.setFormCss(...)` (`SolutionDeserializer.java` lines 454–459,
  510–519); `SolutionSerializer.getFormLESSFile(Form)` (line 1920) and
  `FORM_LESS_FILE_EXTENSION = ".less"` (line 132) give the canonical `.less` path.

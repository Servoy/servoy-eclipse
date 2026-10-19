# Spec: SVY-21576 — Form `.less` edit does not refresh the designer preview (regression from SVY-21249)

## 1. Goal
When a developer edits a form's own stylesheet in the form editor's **Less** tab and
saves, the form designer's live preview must repaint with the new rules immediately —
as it did before SVY-21249 and as the running NG client already does. Today the preview
stays stale until the editor is reopened or the IDE restarted. The same stale render is
what the `getFormLayout` MCP tool returns (it renders through the designer's preview
path), so fixing the designer refresh also fixes the reported MCP-tool fidelity gap.
The fix narrows the SVY-21249 "own file changed" guard in
`ServoyModel.handleChangedFiles` so a changed form `.less` counts as an own-file change
and takes the full editing-persist refresh branch, while keeping SVY-21249's protection
for the "only a child `.js` changed" case intact.

## 2. Background

### 2.1 How a form stylesheet is stored and loaded
A form's own CSS lives in the `Form` persist property `Form.getFormCss()` and is
serialized to disk as `forms/<form>.less` (constant
`SolutionSerializer.FORM_LESS_FILE_EXTENSION = ".less"`, path helper
`SolutionSerializer.getFormLESSFile(Form)` →
`<relativeFormPath>/<formName>.less`). The form's structural persist is serialized
separately as `forms/<form>.frm` (`SolutionSerializer.FORM_FILE_EXTENSION = ".frm"`).
When the `.less` file changes on disk, `SolutionDeserializer` maps it back to the owning
`Form` and calls `form.setFormCss(css)`, which flags the **Form** persist changed even
though the Form's own `.frm` file did not change.

### 2.2 The SVY-21249 guard
`ServoyModel.handleChangedFiles` (`com.servoy.eclipse.core`,
`com.servoy.eclipse.core/src/com/servoy/eclipse/core/ServoyModel.java`) visits every
changed persist. For a composite-with-items persist (e.g. a `Form`) it decides between
two branches (current code, ~lines 2841–2865):

```java
boolean compositeWithItems = SolutionSerializer.isCompositeWithItems(persist);
boolean ownFileChanged = true;
if (compositeWithItems)
{
    String ownFilePath = new File(solutionLocationDir.getParentFile(),
        SolutionSerializer.getRelativeFilePath(persist, false)).getAbsolutePath(); // the .frm path
    ownFileChanged = changedFilePaths.contains(ownFilePath);
}
if (compositeWithItems && !ownFileChanged)
{
    // only child script element(s) changed; do NOT overwrite the editing composite
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

SVY-21249 (commit `8dddf28a6f`) introduced this guard so that when a child
`ScriptMethod` (`.js`) is written by the form editor's "OK & Show" and the `Form` is
flagged changed even though its `.frm` never changed on disk, the full
`updateEditingPersist(...)` is skipped — otherwise it discarded the just-assigned,
still-unsaved event handler. The guard's intent is narrow: skip only the "only a child
`.js` changed" case. Follow-up commit `a9a21061c1` fixed a path-doubling bug: `ownFilePath`
must be built from `solutionLocationDir.getParentFile()` because
`SolutionSerializer.getRelativeFilePath()` already includes the solution name. Any new
path comparison must use the same base directory.

### 2.3 Why form CSS regressed
A form's `.less` is neither the `.frm` nor a child script element. Saving the Less tab
changes `forms/<form>.less`, which flags the `Form` changed — but `ownFileChanged` only
tests the `.frm` path, so `ownFileChanged` is `false`, the guard takes the skip branch,
calls `persist.clearChanged()` without `updateEditingPersist(...)`, and never puts the
Form into `changedEditing`. The designer's `refreshPersists` (in
`RfbVisualFormEditorDesignPage`, `com.servoy.eclipse.designer`) only re-pushes persists
that reach `changedEditing`, so the recompiled form CSS is never re-pushed and the
preview stays stale. The guard does not exist on `lts_2026`, which is why the LESS
preview updates correctly on 2026.3 LTS and is broken on `release`; the designer refresh
code is byte-identical between the branches. The running NG client is unaffected because
it reads the `.less` live from the repository blob.

### 2.4 Why the MCP tool is not the right target
The `getFormLayout` MCP tool renders through the same
`/formtemplate` / `AngularFormGenerator.parseLess(form.getFormCss())` path as the
designer's live preview, so it faithfully mirrors whatever the designer shows. Changing
the tool to compile and inject the form `.less` would make it diverge from the designer
and mask a genuine designer regression that every user editing a form `.less` hits.
Johan Compagner's ticket comments ("this should really work fine… maybe this is more of
a caching issue, the `.less` file of a form is not picked up right away… but then it also
shouldn't show in a normal nclient launch") match the diagnosis exactly: runtime is fine,
the designer editing-model refresh is not. The fix therefore belongs in
`ServoyModel.handleChangedFiles` in servoy-eclipse. The ticket's secondary cosmetic
MCP-tool items (one-line 79 KB JSON output, literal "DataLabel" text, screenshot not
surfaced) are separate, low-value ergonomics and are out of scope here.

## 3. Design

### 3.1 Narrow the guard so a changed form `.less` is an own-file change
In the `compositeWithItems` branch, before deciding `ownFileChanged`, also treat the
form's own `.less` file as an own-file change. Concretely: when `ownFileChanged` is still
`false` after the existing `.frm` check and the persist is a `Form`, compute the form's
`.less` path with the **same base-dir convention** as `ownFilePath` and test it against
`changedFilePaths`. If the `.less` path is present, set `ownFileChanged = true` so the
code falls through to the `else` branch (full `updateEditingPersist` → `formCss` refresh
→ entry added to `changedEditing` → designer re-push). The pure "only a child `.js`
changed" case is unaffected: no `.less` is in `changedFilePaths`, so `ownFileChanged`
stays `false` and the skip branch still protects the unsaved handler.

The `.less` comparison path must be built the same way as the existing `.frm`
`ownFilePath`:

```java
new File(solutionLocationDir.getParentFile(),
    SolutionSerializer.getRelativeFilePath(persist, false))   // → .../forms/<form>.frm
```

For the `.less` sibling, derive the relative path from the same
`SolutionSerializer.getRelativePath(persist, false)` + `<formName>` + `.less` (do not use
`getFormLESSFile(Form)` directly here — that returns an `IFile`/workspace path, whereas
this comparison is against absolute `java.io.File` paths rooted at
`solutionLocationDir.getParentFile()`; build the comparison path with the same `File`
convention as `ownFilePath` so it matches `changedFilePaths`). Guard the cast to `Form`
so non-form composites are untouched.

### 3.2 Extract the own-file decision into a testable helper
The current comparison is an anonymous-visitor inline block that requires a live
`ServoyModel`, OSGi runtime and workspace to exercise — not unit-testable. To make the
new behaviour verifiable without the full IDE, extract the decision into a small
package-visible static helper on `ServoyModel` (or a nearby util class in
`com.servoy.eclipse.core`), e.g.:

```java
/**
 * Returns true when the composite persist's OWN serialized file (its .frm, or — for a
 * Form — its form-level .less stylesheet) is among the changed files, meaning the full
 * editing persist must be refreshed. Returns false when only child files (e.g. a child
 * script .js) changed, in which case the editing composite must not be overwritten
 * (SVY-21249).
 */
static boolean isOwnCompositeFileChanged(IPersist persist, File solutionParentDir, Set<String> changedFilePaths)
```

The helper encapsulates: build the `.frm` `ownFilePath` from
`solutionParentDir`+`getRelativeFilePath(persist, false)`; if present → `true`; else if
`persist` is a `Form`, build the `.less` sibling path the same way and if present →
`true`; else `false`. The visitor then calls:

```java
boolean ownFileChanged = !compositeWithItems
    || isOwnCompositeFileChanged(persist, solutionLocationDir.getParentFile(), changedFilePaths);
```

Keep the existing branch structure (`if (compositeWithItems && !ownFileChanged) { skip }
else { full refresh }`) unchanged — only the computation of `ownFileChanged` moves into
the helper. This keeps the diff minimal and preserves the SVY-21249 behaviour exactly for
the `.js`-only case while adding the `.less` case.

### 3.3 Test project
The triage calls for a test under `com.servoy.eclipse.core.tests`. That project does not
exist in the repo today (the SVY-21249 commit `8dddf28a6f` removed its editor-side tests
and the follow-up added none, so there is no existing `handleChangedFiles` guard test).
Two options:

1. **Preferred — pure unit test, no new bundle:** add the helper from §3.2 and test it
   with a plain JUnit test placed in the existing `com.servoy.eclipse.model.tests`
   bundle only if the helper lives in a class reachable from there; since
   `ServoyModel` lives in `com.servoy.eclipse.core`, the natural home is a new
   `com.servoy.eclipse.core.tests` bundle. Create it as a standard `eclipse-test-plugin`
   (mirroring `com.servoy.eclipse.model.tests`: same `pom.xml` packaging, `.project`,
   `META-INF/MANIFEST.MF`, `build.properties`, `src/test/java` layout) and register it in
   the parent reactor `pom.xml` module list. The helper from §3.2 must be at least
   package-visible / `public` so the test can call it. The test drives the helper with
   fabricated `changedFilePaths` sets and a stub/mock `Form` (reflected or Mockito,
   matching the model-tests conventions) — no OSGi/workspace needed because the helper is
   pure path logic. This matches the "pure-logic helper + unit test" pattern already used
   for `AddContainerCommand.computeNextLayoutContainerIndex` (see AGENTS.md SVY-21386).
2. **Fallback — integration test** in `com.servoy.eclipse.model.tests` or
   `com.servoy.eclipse.ui.tests` (both `eclipse-test-plugin`) that builds a synthetic
   solution, writes a form `.less`, triggers `handleChangedFiles`, and asserts the
   editing form's `formCss` is refreshed. Heavier and slower; use only if extracting the
   helper proves impractical.

Prefer option 1. The helper extraction is the enabling change.

## 4. Implementation plan

1. **`com.servoy.eclipse.core/src/com/servoy/eclipse/core/ServoyModel.java`** — add the
   package-visible static helper `isOwnCompositeFileChanged(IPersist, File, Set<String>)`
   described in §3.2:
   - build the `.frm` path with `new File(solutionParentDir,
     SolutionSerializer.getRelativeFilePath(persist, false)).getAbsolutePath()`; if in
     `changedFilePaths` → return `true`;
   - if `persist instanceof Form`, build the `.less` sibling path with the same base
     (`new File(solutionParentDir, SolutionSerializer.getRelativePath(persist, false) +
     ((Form)persist).getName() + SolutionSerializer.FORM_LESS_FILE_EXTENSION)
     .getAbsolutePath()`); if in `changedFilePaths` → return `true`;
   - otherwise return `false`.
   Verify the exact `getRelativePath` / `getFileName` composition against
   `getRelativeFilePath` so the `.less` path matches what `changedFiles` actually
   contains for a form `.less`.
2. **`ServoyModel.handleChangedFiles` visitor (~lines 2841–2865)** — replace the inline
   `ownFileChanged` computation with a call to the new helper:
   `boolean ownFileChanged = !compositeWithItems ||
   isOwnCompositeFileChanged(persist, solutionLocationDir.getParentFile(), changedFilePaths);`
   Leave the `if (compositeWithItems && !ownFileChanged) { … } else { … }` branches and
   the `IScriptElement` / `TableNode` handling unchanged.
3. **Create `com.servoy.eclipse.core.tests`** `eclipse-test-plugin` bundle (if not using
   the fallback): `.project`, `META-INF/MANIFEST.MF` (depend on `com.servoy.eclipse.core`,
   `com.servoy.eclipse.model`, JUnit), `build.properties`, `pom.xml`
   (`<packaging>eclipse-test-plugin</packaging>`, parented to the reactor), and
   `src/test/java/com/servoy/eclipse/core/ServoyModelOwnFileChangedTest.java`. Add the new
   module to the root reactor `pom.xml`.
4. **Add the regression test** `ServoyModelOwnFileChangedTest` asserting:
   - a form `.less` path present in `changedFilePaths` (and no `.frm`) →
     `isOwnCompositeFileChanged` returns `true` (full refresh branch);
   - a child `.js`-only change (neither `.frm` nor `.less` present) → returns `false`
     (SVY-21249 skip branch preserved);
   - a form `.frm` change still → returns `true`;
   - a non-`Form` composite with only a child file changed → returns `false`.
5. **Post-edit checklist (AGENTS.md):** `eclipse-coder_organizeImports`,
   `eclipse-coder_formatFile`, `eclipse-ide_getCompilationErrors`, fix any Spotbugs of the
   two highest severities, then run the test class.

## 5. Acceptance criteria
- [ ] Editing a color in a form's **Less** tab and saving repaints the designer preview
      immediately with the new rules (no reopen/restart needed), matching the running NG
      client.
- [ ] `getFormLayout` (MCP tool), reading the same preview path, returns the new form
      CSS after the Less-tab save — without any change to the Servoy-Copilot bundle or the
      MCP tool.
- [ ] SVY-21249 stays fixed: assigning an `onAction` via "OK & Show" (which writes a child
      `.js` but does not change the `.frm`) and then Save All still saves the handler; the
      unsaved editor state is not overwritten.
- [ ] `ServoyModel.handleChangedFiles`'s own-file decision is extracted into a
      package-visible/static helper and the designer-side `RfbVisualFormEditorDesignPage`
      is unchanged.
- [ ] A regression test (`com.servoy.eclipse.core.tests` →
      `ServoyModelOwnFileChangedTest`, or the fallback integration test) asserts: a form
      `.less` change → own-file-changed `true` (formCss refreshed); a child-`.js`-only
      change → `false` (skip branch preserved); a `.frm` change → `true`.
- [ ] Compilation is clean, imports organized, file formatted, no top-two-severity
      Spotbugs issues in the changed code, and the test passes.

## 6. Out of scope
- Any change to the `getFormLayout` MCP tool or the Servoy-Copilot bundle.
- Any change to `RfbVisualFormEditorDesignPage` or the designer refresh path (byte-
  identical to LTS and already correct once the Form reaches `changedEditing`).
- The ticket's secondary cosmetic MCP-tool items: one-line 79 KB JSON output, literal
  "DataLabel" placeholder text, and the opt-in screenshot not being surfaced. These are a
  separate ticket if pursued.
- Any `.css` (non-LESS) form-stylesheet handling beyond the existing `.less` path, unless
  verification shows forms can also persist a `.css` sibling that follows the same model
  flow (confirm during implementation; if so, treat it symmetrically in the helper).

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| Does a form stylesheet ever serialize as `.css` rather than `.less` on disk? If so, the helper should match both extensions. Confirm during implementation. | implementer | open |
| Preferred test home: create a new `com.servoy.eclipse.core.tests` bundle (clean unit test of the extracted helper) vs. an integration test in an existing `*.tests` bundle. Spec recommends the new bundle + helper; confirm the new reactor module is acceptable. | reviewer | open |

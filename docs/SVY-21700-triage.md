# Triage Report — SVY-21700

**Verdict:** PROCEED

## Reported problem

Since **2026.03.2**, four build-time markers appear in the Problems view for the abstract
base forms of the `svyImportDialog` module:

```
Form "svyImportDialog_abstractChooseFile"    uses CSS position layout but has no body part; it will be shown in record view at runtime.
Form "svyImportDialog_abstractFieldMapping"  uses CSS position layout but has no body part; it will be shown in record view at runtime.
Form "svyImportDialog_abstractMain"          uses CSS position layout but has no body part; it will be shown in record view at runtime.
Form "svyImportDialog_abstractPerformImport" uses CSS position layout but has no body part; it will be shown in record view at runtime.
```

These are abstract base forms (meant to be extended, not instantiated directly) whose
persisted `.frm` carries `useCssPosition:true`. The reporter found the only way to silence
the markers is to hand-edit each `.frm` to set `useCssPosition:false`, and noted this
"seems to be a 2026.03.2 only problem so far." No solution beyond that manual workaround was
proposed in the ticket.

## Root-cause assessment

This is a **false-positive marker** introduced on the 2026.03 line — the check is correct in
intent but has no way to silence a legitimately body-less form, and no one-click fix.

The marker `MarkerMessages.FormCssPositionNoBodyPart`
(`com.servoy.eclipse.model/.../builder/MarkerMessages.java:314`) is raised from
`ServoyFormBuilder.addFormMarkers`
(`com.servoy.eclipse.model/.../builder/ServoyFormBuilder.java:734-742`) whenever a form is
non-responsive, has `useCssPosition==true`, and its flattened form has no `Part.BODY`. The
severity `ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART` (`ServoyBuilder.java:442`) is
`ProblemSeverity.ERROR`.

An abstract base form (an extendable layout with no body of its own, whose body is supplied
by the concrete subforms that extend it) legitimately has `useCssPosition==true` and no body
part, so it trips the check even though it is never shown on its own and never actually
falls back to record view at runtime. `Form.isAbstractForm()` treats any
`useCssPosition==true` form as *not* abstract, so there is no structural flag that would let
the check exclude it, and the user is left hand-editing `.frm` files.

## Ticket premise check

The ticket only reports the symptom and a manual workaround (set `useCssPosition:false` by
hand). That workaround is the *right action* — for an abstract form the layout flag is
meaningless, and clearing it removes the marker and aligns the form with a clean abstract
form — but doing it by hand on every base `.frm`, with no tooling, is the problem. The
correct productization of that workaround is a **marker quick-fix** that performs exactly
that change on one click.

## Approaches considered

1. **Add a quick-fix to the marker that clears the form's layout info (recommended).**
   Offer a Servoy quick-fix on the `FormCssPositionNoBodyPart` marker whose action is
   `form.setUseCssPosition(null)` (+ save). An abstract form should carry no layout
   information; this is the same change the reporter does by hand, as a one-click fix.
   - Pros: productizes the exact, correct manual workaround; keeps the builder check firing
     for genuinely broken forms; minimal, well-scoped; follows the existing
     `ServoyQuickFixGenerator` pattern.
   - Cons: the marker currently uses the shared `PROJECT_FORM_MARKER_TYPE` and sets no
     `Uuid`, so a small builder change is needed to carry the form UUID and let the quick-fix
     target the exact form (see Git/Code findings). The marker still fires until the user
     applies the fix (by design here).

2. **Narrow the builder predicate so extended base forms never fire.** (Automatic
   suppression.)
   - Pros: no user action needed.
   - Cons: the user explicitly chose the quick-fix route for this branch; automatic
     suppression is a larger behavioural change and risks masking genuinely broken forms.
     Out of scope here.

3. **Lower the default severity ERROR → WARNING.**
   - Pros: one line; non-blocking.
   - Cons: doesn't remove the false positives; treats the symptom. Separate release-owner
     call.

4. **No code change.**
   - Cons: leaves a confirmed false-positive ERROR on every valid CSS-position abstract base
     form, forcing manual `.frm` edits. Not acceptable.

## Recommendation

**PROCEED with Approach 1** — add a marker quick-fix that clears the form's `useCssPosition`
layout flag (the "an abstract form shouldn't have layout information" action, matching the
New Form wizard's notion of a clean abstract form). Keep the builder check firing as-is
(the user's explicit choice); the quick-fix is the resolution the user applies.

To let the quick-fix target the exact form, the builder must attach the form's `Uuid` to
this marker (today `PROJECT_FORM_MARKER_TYPE` markers only carry `SolutionName`), and the
quick-fix must recognise specifically the css-no-body case (by message text) so it is not
offered on every `formProblem` marker.

## Git history findings

- The marker/check is on the 2026.03 line: `ServoyFormBuilder.addFormMarkers` raises
  `FormCssPositionNoBodyPart` with severity `FORM_CSS_POSITION_NO_BODY_PART` =
  `ProblemSeverity.ERROR` (`ServoyBuilder.java:442`). Confirmed present in the current
  `lts_2026` checkout (`C:\Users\vosti\git_2026.3\servoy-eclipse`), which is where the fix is
  to be made (the reporter observed it in 2026.03.2).
- `com.servoy.eclipse.core.quickfix.ServoyQuickFixGenerator` is the registered
  `IMarkerResolutionGenerator`; `com.servoy.eclipse.core/plugin.xml` already registers it for
  `com.servoy.eclipse.core.formProblem` (the `PROJECT_FORM_MARKER_TYPE`), but there is no code
  branch for that type, so no fix is offered today.
- `ServoyBuilder.addMarker(...)` only sets `SolutionName` for `PROJECT_FORM_MARKER_TYPE`
  (`ServoyBuilder.java:~3424`); it does not set `Uuid`. The quick-fix therefore needs the
  builder to carry the form UUID for this specific marker.

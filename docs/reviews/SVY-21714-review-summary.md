# Peer review summary: SVY-21714

**Risk: MODERATE.** `WebFormComponentChildType.getJson(true, ...)` now returns a detached `JSONObject` when the parent's form-component value is malformed. Writes through it are dropped silently, but the form is still flagged changed. Whether the Properties UI can reach this state was not confirmed.

Scope reviewed:
- servoy-eclipse `41833b388`
- servoy-client `83e4bf2e9`

Both commits are on `master`.

Release target: MATCH. Fix Version 2026.12 is unreleased, and servoy-client `master` reads 2026.12.0. Both commits are on origin/master.

## Manual test plan

**Verifying the fix**
1. Create a CSS-position form containing a `servoycore-listformcomponent`. Set `json.containedForm` to a plain form name instead of `{"svy_form": ...}`. Open the form in the NG client. It should render, with the list form component empty and one error in the log naming the form, element and property.
2. Open the same form in the designer. The page should load, the outline should show no children for that component, and ghosts and template refresh should work. No `ClassCastException` should appear in the Developer error log.
3. Move a sibling component on that form. The designer should still update.

**Regression checks**
1. Open a form with a valid form component, including a nested one. Outline children, ghosts, designer templates and NG rendering should be unchanged.
2. Select a form-component child in the designer, edit a property in the Properties view, and save. The change should persist.
3. Open the tab-sequence page on a valid form-component form. The children should be listed.
4. On the bad-string form, select a child through the tab-sequence page and edit it. Note whether the form is marked dirty and whether the change is written.
5. Open a form with a recursive form-component reference. It should be reported once, with no entry left stuck in the recursion set.

Automated: `WebFormComponentChildTypeTest` in `com.servoy.eclipse.model.tests` (JUnit 6). It was not run during the review.

## Possible improvements / follow-ups

- `WebFormComponentChildType.setProperty` and `removeProperty` could fail or log instead of writing into a detached object and calling `flagChanged()` when the parent value is malformed (`WebFormComponentChildType.java:133-149, 495-523`).
- `asFormComponentValue` logs on every call with no dedupe, from render, refresh and outline paths. One bad `.frm` repeats the same error. Consider a single shared resolve-and-validate helper, or rate-limiting the log. The logged value is raw solution data.
- The scope of `WebFormComponentChildType` is wider than the nine sites in the ticket: the `initializeElement` reorder, `getJSONObject` to `optJSONObject`, and about 40 re-indented lines. Confirm this was intended.
- A `WebFormComponentChildType` built on a bad parent keeps `propertyDescription == null` and `element == null` for its lifetime. The consumers checked are null-safe, but the reference search was grep-based and capped at 30 results.
- Only `WebFormComponentChildType` has new tests. The other designer sites and the servoy-client sites have none.
- `docs/SVY-21714-triage.md` still refers to Servoy-Copilot paths (`C:\Users\...`). Adapt or trim it for this repo.
- Not verified: Spotbugs, a running Developer instance, and casts that the grep pattern would not match (servoy-extensions, sablo).

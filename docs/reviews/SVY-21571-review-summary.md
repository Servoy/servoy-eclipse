# Review Summary — SVY-21571: servoycore-errorbean is marked deprecated but is still actively used in form editor

**Risk: LOW.** The change is contained to two design-time consumers (template generator and builder). The one risk the review raised, a stale sablo or servoy_ngclient paired with the new servoy-eclipse, was dismissed by the reviewer: the repos are always in sync in the workspace and ship in sync in installers and Developer updates.

**Reviewed scope** (all `master`, Fix Version 2026.12, author Gabi Boros):
- servoy-eclipse `cfb9420e33`
- sablo `4e5e121d`
- servoy-client `dad1102ab`

## Manual test plan

**Verifying the fix**
1. Build sablo, servoy-client and servoy-eclipse from the same master window and start the Developer.
2. Open a solution with a form that uses a component from a package you then remove or disable. Open the form in the editor. Expected: the error bean renders and the editor is not blank.
3. Check the Problems view for that form. Expected: a MissingSpecification marker for the missing component, and no DeprecatedSpecification marker about `servoycore-errorbean`.
4. Inspect the generated Angular output for the servoycore package. Expected: it contains `servoycore-errorbean`.

**Regression checks**
1. Open the component palette and the "convert component" dialog. Expected: errorbean does not appear (`isDeprecated()` is unchanged).
2. Check another deprecated component, e.g. `servoycore-defaultLoadingIndicator`. Expected: no template is generated, and a DeprecatedSpecification marker is raised when it is placed on a form.
3. Open a few forms using ordinary third-party components (aggrid, bootstrap). Expected: no new markers and the specs still load.
4. WAR export: quick check that the errorbean template is still included.

**Automated checks:** `com.servoy.eclipse.ngclient.ui.tests` (`ComponentTemplateGeneratorTest`, `WebObjectSpecificationAlwaysGenerateTemplateTagTest`), e.g. `mvn clean verify -pl com.servoy.eclipse.ngclient.ui.tests`. These were not run during the review.

## Possible improvements / follow-ups
- `ServoyBuilder.checkSpecs` (~`:959-988`): a tagged and deprecated spec now falls through the `else if` into the final `else`, which runs `checkMissingProperties(...)`. Before, a deprecated spec never reached it. Only matters for a persist literally typed `servoycore-errorbean`, which normal flow never creates. The explanatory comment sits in the `else` branch, not at the changed condition.
- `PropertyDescription.hasTag` is presence-only, so `"alwaysGenerateTemplate": false` also opts out. Consider `Boolean.TRUE.equals(getTag(...))`. Any package can use the tag.
- Spec-root `tags` are now stored on every spec. `PropertyDescription.java:134` runs `setDescription((String)getTag("doc"))`, so a root `tags.doc` that is not a string would throw `ClassCastException` at parse time. No shipped spec has root `tags` (by grep), so it is theoretical.
- Test strength: `WebObjectSpecificationAlwaysGenerateTemplateTagTest` re-evaluates `isDeprecated() && !hasTag(...)` inline, so it only pins the sablo parse contract. `ServoyBuilder.checkSpecs` has no test.
- Open on the issue: the tag name `alwaysGenerateTemplate` differs from the `serveronly`/`skiptemplate` suggested in the issue. Johan Compagner's question on what the error bean shows in ngclient and the designer has no reply.
- The Jira closing comment cites the servoy-eclipse commit as `cf3092a`. The actual SHA is `cfb9420e33`.

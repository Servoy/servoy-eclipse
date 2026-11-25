# SVY-21460 peer-review summary (lts_2026)

**Risk: MODERATE, same as the master review.** The only real finding carries over unchanged: `HeadlessFormTemplateContent.getFormDataJS` (`:118-155`) toggles the JVM-wide `Settings.TESTING_MODE` per request without a lock. Overlapping `/formtemplate` renders can leave testing mode on for the whole Developer, and `Settings.save()` writes it to servoy.properties on shutdown. Developer-only, needs concurrent requests. Accepted as-is at review time.

This is a delta review against the earlier master review (master summary: `docs/reviews/SVY-21460-review-summary.md` on master). It covers only what differs on lts_2026.

Reviewed scope (all on `origin/lts_2026`):
- servoy-eclipse: `adb0aed22`, `92dd5850c`, `245930384`, `a7a705218`, `292983d68`, `02c15909d`
- servoy-client: `80661cc30` (same commit as on master/release)
- Not on lts by design: servoy-eclipse `84ccf5648` (fixes a release/master-only provider bug), and the Servoy-Copilot `getFormLayout` MCP tool (no lts_2026 branch in that repo).

Release target: Fix Version is only `2026.12` (unreleased), but lts_2026 reads as 2026.3.3 LTS and the code already shipped in the `2026.3.2` and `2026.9.0` tags. The Fix Version does not reflect the LTS releases that carry the fix.

## lts-specific findings

- **ServoyPublicService provider is correct on lts.** `formtemplate.module.ts:15-16` provides `ServoyPublicServiceFormTemplateImpl` on the lazy module injector, above the whole `svy-formtemplate` subtree, including nested forms and `LFCModule` components. It does not leak into the NG client or designer injectors, and no root singleton injects `ServoyPublicService`. This confirms the `84ccf5648` commit message: lts does not need that fix.
- **Low: the formtemplate component ships in the shared chunk.** `servoycore/addattribute.directive.ts:3,37` and `servoycore/listformcomponent/listformcomponent.ts:11,223` import `FormTemplateComponent` as a class token to find the parent form. Because of that, the formtemplate copy of the generated component templates is probably bundled into the common chunk that every NG client page loads. No functional change. Master avoids this with an `AbstractFormComponent` DI token. Inferred from imports, not confirmed with build stats.
- **Low: a different error when no parent form is found.** When a `[svyContainerStyle]`/LFC has no form host at all, the error now names `FormTemplateComponent` instead of `DesignFormComponent`. No such case exists in the lts sources.
- **Security: LOW, same as master.** The Java endpoint is identical apart from one comment. The Angular side has no `innerHTML`/`bypassSecurityTrust*`. Note (SVY-21670 `136f013279`, not this case): the HTTP 500 message includes exception messages, the root cause's top stack frame and possibly a local file path.

## Test results at review time

- Jenkins `lts_2026/servoy-eclipse` #379: SUCCESS, 1032 tests, 0 failed. The formtemplate Karma specs were in that run.
- Jenkins `lts_2026/servoy-client` #73: SUCCESS, 420 tests, 0 failed.
- No local render run on lts. The render path was only exercised locally on master.

## Manual test plan (lts Developer)

Run the whole plan with testing mode **off** on the admin page first. Check that it is still off after that round, then switch it **on** and run the plan again.

**Verifying the feature**
1. With an lts Developer running and a solution active, open `http://localhost:<port>/formtemplate/<cssPositionForm>.html`. Components should render in position with `data-svy-name`/`data-svy-id`, and the solution stylesheets should load as separate `<link>` tags.
2. Repeat for a responsive form, a tabpanel form and a datagrid form. Tabs and columns should be present, nested tab forms render through `<svy-formtemplate>`, and text fields with formats render (no NG0200/NG0201 in the console).
3. Open `/formtemplate/doesNotExist.html`. It should return 404.

**Regression checks**
4. With testing mode off, run two parallel loops of 20 requests against `/formtemplate/<form>.html`. Then open a debug NG client: unnamed elements should not get UUID-based `svy_` names. Exit the Developer: servoy.properties should not contain `servoy.ngclient.testingMode=true`.
5. Open forms with tabpanel/datagrid (custom-array properties) and list form components in a debug NG client, in the form designer, and in an exported WAR. They should behave exactly as before, with no injector errors in the browser console.
6. Export a WAR. The build should succeed, and requesting `/formtemplate/x.html` on the deployed server should be harmless.

## Possible improvements / follow-ups

- **Testing-mode toggle:** if it ever causes trouble, pass testing mode per call instead of mutating the global setting, or serialise save/set/restore.
- **Fix Version:** consider adding 2026.3.2 (LTS) and 2026.9.0.
- **Merge-forward:** release/master deleted `formtemplate.module.ts` and `formtemplate-routing.module.ts`. Any future lts edit to them, or to the NgModule-style formtemplate/addattribute code, has to be re-applied by hand to the standalone code on merge. lts uses `hasSampleData()`, master `isInSampleDataMode()`.

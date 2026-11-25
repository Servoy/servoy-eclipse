# SVY-21460 peer-review summary

**Risk: MODERATE.** The new `/formtemplate` route toggles the JVM-wide `Settings.TESTING_MODE` per request without a lock. Overlapping renders can leave testing mode on for the whole Developer session, and `Settings.save()` writes it to servoy.properties on shutdown. The impact is Developer-only and needs concurrent requests.

Reviewed scope (all on `origin/master`):
- servoy-eclipse: `adb0aed22`, `92dd5850c`, `245930384`, `a7a705218`, `84ccf5648`, `292983d68`, `02c15909d`
- servoy-client: `80661cc30`
- Servoy-Copilot: `b3da2d4e`, `253932eb`, `f79e74bd`

Fix Version 2026.12: the release-target check is MATCH. The servoy-eclipse and servoy-client commits also shipped in 2026.3.2 and 2026.9.0 (via lts_2026).

## Test results at review time

- Jenkins `master/servoy-eclipse` #546: formtemplate Vitest specs (37) and `ChromiumWrapperScreenshotTest` (4) passed.
- Jenkins `master/servoy-client` #148: `PersistFieldInstanceTest` (7) passed, including the form-template case.
- Jenkins `developer_ai_plugin` #476: `McpToolResultTest` and the `FormLayout*` plain tests passed. The `FormLayout*IntegrationTest` render tests are skipped on CI because the agent has no browser.
- Local PDE run of the three `FormLayout*IntegrationTest` classes in a Developer: 23 tests, 12 passed, 2 failed, 9 skipped.
  - The SVY-21460 render path passed: CSS form DOM, `data-svy-*` stamping, deterministic re-render, viewport handling, error cases.
  - The 9 skips were HTTP 503 or a timeout while the Developer was still building its Angular resources.
  - The 2 failures are SVY-21610 (sample data) and SVY-21614 (disabled/readonly) tests, out of scope here.

## Manual test plan

Run the whole plan with testing mode **off** on the admin page first. Check that testing mode is still off after that round, then switch it **on** and run the plan again.

**Verifying the feature**
1. With a Developer running and a solution active, open `http://localhost:<port>/formtemplate/<cssPositionForm>.html` in a browser. Components should render in position with `data-svy-name`/`data-svy-id` attributes, and the solution stylesheets should load as separate `<link>` tags.
2. Repeat for a responsive form, a form with a tabpanel and a form with a datagrid. Tabs and columns should be present, and nested tab forms should render through `<svy-formtemplate>`.
3. Open `/formtemplate/doesNotExist.html`. It should return 404.
4. From an AI agent, call `servoy-form` / `getFormLayout` for a form that lives in a module. On the Chromium backend you should get the DOM, an appearance map and a PNG. On the SWT backend you should get the DOM and styles with no image.

**Regression checks**
5. Make sure `servoy.ngclient.testingMode` is absent or false in servoy.properties. Run two parallel loops of 20 requests against `/formtemplate/<form>.html`. Then open a debug NG client: unnamed elements should not get UUID-based `svy_` names. Exit the Developer: servoy.properties should not contain `servoy.ngclient.testingMode=true`.
6. While an agent repeatedly calls `getFormLayout`, open a form in a debug NG client and in the form designer. Element names and attributes should be unchanged.
7. Open forms with tabpanel/datagrid (custom-array properties) in a debug NG client and in an exported WAR. They should behave exactly as before.
8. Open the same forms in the form designer. Palette, ghosts and table columns should be unchanged.
9. In a deployed client, open a form with container-style wrappers and a list form component. There should be no NG0201 errors in the browser console.
10. Export a WAR. The build should succeed, and requesting `/formtemplate/x.html` on the deployed server should be harmless (404 or the normal index, no form data).
11. Start a Cypress run from the Developer while `/formtemplate` is being requested. The Cypress run should keep working in testing mode.

## Possible improvements / follow-ups

- **Testing-mode toggle** (`HeadlessFormTemplateContent.getFormDataJS`). Accepted as-is at review time: it is Developer-only and needs overlapping renders. If it ever causes trouble, pass testing mode per call (for example through the converter context) instead of mutating the global setting, or at least serialise the save, set and restore. The tester covers the off/on behaviour, as described in the test plan above.
- **Shared form-component cache** (servoy-client `FormElementHelper`). A form-template render can fill the shared `formComponentElements` cache while testing mode is on. It is unclear whether TiNG clients read that legacy `template` string. If they don't, this has no effect.
- **Network exposure.** `/formtemplate/*` has no loopback or auth check. The bundled Developer connector has no `address` attribute, so it may bind to all interfaces. This is the same exposure the existing `/designer/*` filters already have. Consider a loopback check for all of these endpoints.
- **Tests.** There is no automated test for `HeadlessFormTemplateContent` or `FormTemplateHttpEndpoint`, although the spec planned a Java test.
- **Scope.** The ticket said "no client-runtime changes", but servoy-client `80661cc30` changes `servoy_ngclient`. The default path is unchanged; confirm this scope was agreed with the lead.
- **Fix Version.** Consider adding 2026.3.2 and 2026.9.0, since the code already shipped there.
- **Documentation drift.** The `HeadlessFormTemplateContent` Javadoc still refers to the removed `getSolutionStyleSheet()`. The spec still describes the replaced `/formtemplate/stylesheet.css` endpoint.

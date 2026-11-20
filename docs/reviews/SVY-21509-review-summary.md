# Review Summary: SVY-21509 Form opened in browser does not refresh after editor change

**Risk: MODERATE.** The refresh path was never run against a live preview and neither repo has a test for it. The fix also spans two repos that must ship together.

**Scope reviewed:** servoy-eclipse `cc92fcc8c8` and servoy-client `3e463a627a`, both on master (2026.12.0-SNAPSHOT).

**Release-target check:** MATCH. Both masters carry the fix for Fix Version 2026.12, which is unreleased.

## Manual test plan

**Verifying the fix**
1. Start Developer on a solution, open a form in the form editor and use Show Form in Browser.
2. Change a component property, save, and check that the browser updates without a manual reload.
3. Move or add a component, save, and check the same.
4. Edit the solution `.less` or `.css` and check for a page reload.
5. Start Developer fresh, make the preview the very first NG client request, and check that a preview client is created (the early-activation defect).

**Regression checks**
1. Save an unrelated persist and check that the preview does not reload.
2. Save a script method and a form variable with the preview open, and check the log for exceptions.
3. Save a relation, valuelist, menu and style that the previewed form uses. Check that the form reloads with no exceptions.
4. Open a debug NG client and a preview together, save a form, and check that both refresh.
5. Open `?formpreview=<unknown form>` and a second preview on the same session (retarget).
6. Run a headless Cypress form test and check that sessions still start.

Automated: `com.servoy.eclipse.ngclient.tests` (`NGClientWebsocketSessionFactoryTest`) and `com.servoy.eclipse.cypress.tests` plainjunit, both with JUnit 6.

## Possible improvements / follow-ups
- Add a test for the refresh path. A cheap one is a plain test for the `ScriptCalculation` `instanceof` skip in `DebugUtils`.
- `refreshFormPreview` returns silently when the preview has no websocket session yet. Decide whether it should log.
- `FormPreviewNGClient` passes an unknown form name straight to `showFormInMainPanel`. That path was not traced.
- The unconditional `CLIENT_ENDPOINT` factory registration is a plain `put`. Confirm headless Cypress still works, because the supplier wiring differs from `forHeadless()`.
- If the embedded app server can bind to a non-loopback interface, the auth-free `?formpreview=` exposure is worth a separate look. That design predates this change.
- The spec `docs/SVY-21509-form-preview-refresh-on-editor-change.spec.md` §3.1 names `FormPreviewNGClient.refreshPreview`, but the code has `refreshFormPreview` in core `Activator` plus the `runPreviewRefresh` seam.
- Spotbugs was not run.

# SVY-21610 review summary: sample data in the getFormLayout form-template render

**Outcome: Resolved (Fixed) on 2026-11-27, after peer review. 1h 30m logged on the transition.**

**Risk: MODERATE.** The real NG client runtime is well protected: the server-side code only runs when a flag is set, and only the Developer-only `HeadlessFormTemplateContent` sets it. What remains are the `?sampleData=false` wording and the unguarded LFC fallback (see follow-ups).

The API name differs across branches: master has `ServoyApi.isInSampleDataMode()`, while the `release`/`lts_2026` backports keep `hasSampleData()`. Answer given during the review: this is intended and needs no backport. Only the master component packages (SVY-21687) read the API, and they only use it on 2026.12. Their helper checks optionally whether the method exists (`isInSampleDataMode?.()`), so on 2026.9/2026.3 it just returns `false` and is ignored. On those lines getFormLayout shows a few components (imagemedia, accordion, treeview…) without placeholders. That is a weaker render in the AI tool, not an error.

Reviewed scope (master):
- servoy-eclipse `13b3c60f8`, `912c865a5`, `9ae512df5`, `4ebb5d4ba`, `56ed55e76`, `0ff600379`
- servoy-client `089e243cb`
- Servoy-Copilot `3726aee9`, `a9a48609`

The backports (servoy-eclipse `5a4763832`, `c81be7c61`, `a14a7df65`, `32f272891`, `09038c1c1`; servoy-client `939d2f940`) were not reviewed directly. Release-target check: MATCH (Fix Version 2026.12 ships from master).

## Manual test plan

Posted on the Jira case as comment 235208.

**Verifying the fix (Developer, master)**
1. Build a form with:
   - a bootstrap textbox bound to a form variable that has an initialiser (`var vFilled = 'Value';`);
   - a textbox bound to a text column;
   - a datalabel;
   - an nggrids datagrid on a table.

   Call getFormLayout with default parameters. Expected: the first textbox shows `Value`, the column textbox shows a sample word, and the grid shows 3 rows.
2. Call it again. Expected: identical output.
3. Bind a textbox to a variable that is assigned only in `onLoad`. Expected: it shows a type-based sample value, not the `onLoad` value.
4. Add a tabpanel with a contained form. Expected: the tab content renders, with sample data.
5. Add an LFC over a form with a few fields. Expected: rows render and the console shows no NG0100.
6. Call with `sampleData=false`. Expected: no data values.

**Regression checks**
1. Run a real NG client, both the developer run-client and an exported WAR, on forms with:
   - an LFC whose contained form has a security-hidden element;
   - a datagrid;
   - a servoyextra-table;
   - a tabpanel with nested forms.

   Expected: unchanged behaviour, the hidden element stays hidden, and no "no model found for cell" warnings.
2. Open the same forms in the RFB designer. Expected: unchanged ("DataLabel" placeholder and the LFC designer rows still show).
3. Optional: on a 2026.9 `release` Developer with the current master bootstrap, extra and nggrids packages, run getFormLayout on a form with an imagemedia, an accordion and a treeview. Expected: no errors (the missing `isInSampleDataMode` gets ignored); missing placeholders are accepted.
4. With devtools open, run getFormLayout on forms with a broad component mix. Expected: no console errors, and no network requests besides resource loads.
5. Run two getFormLayout calls concurrently on forms with nested tabs. Afterwards a debug client in the same Developer should behave normally (`Settings.TESTING_MODE` gets restored).

## Possible improvements / follow-ups
- **`?sampleData=false` changes the DOM.** `FormTemplateServoyApi` passes `isDesigner=false` unconditionally (`formtemplate_component.component.ts:340`), so with `?sampleData=false` the HTML still differs from the pre-change render: design placeholders are gone and runtime init runs on empty models. Either correct the "byte for byte" claim in the commit message, spec and endpoint comment, or tie the flag to `!sampleData`.
- **Unguarded `getRowItemState` change.** The runtime LFC `getRowItemState` (`listformcomponent.ts:913-939`) now falls back to the FormCache model, or to an empty `Cell` plus a warning, where it used to throw. This path runs in all deployed clients. Confirm that security-hidden children can't render through this fallback.
- **Sticky `browserUnavailable`.** The static flag in Servoy-Copilot `FormLayoutInspectionService` is never reset, so one transient failure disables getFormLayout until Developer restarts.
- **Missing test.** The nested-form collection in `HeadlessFormTemplateContent` has no Java test. Render-dependent Copilot PDE tests skip on headless CI.
- **Stale doc.** `com.servoy.eclipse.ngclient.ui/node/AGENTS.md` still describes the formtemplate spec as covering the in-designer `ServoyApi`.
- **Skills guidance deferred.** The reporter's other ask was skills guidance: prefer a variable initialiser over `onLoad`. It is deferred and not tracked in a linked case.

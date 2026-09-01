# Triage Report — SVY-21341

**Verdict:** NEEDS_INPUT

## Reported problem

As part of SVY-19023 (standalone-component migration), a `"serveronly": true` tag was
added to component spec properties that had no corresponding `@Input` in the migrated
Angular component. Those properties are excluded from template generation and websocket
communication. The concern is that some of these tagged properties may not be
intentionally server-only but rather dead code that should be removed (or client-side
properties that lost their binding and need it restored).

The ticket lists ~25 properties across three component repositories and asks that each
be categorized as **keep (serveronly)**, **add @Input (client needed)**, or **remove
(unused)**.

## Root-cause assessment

I audited every property in the list across the four relevant git repositories
(`bootstrapcomponents`, `aggridcomponents`, `servoy-extra-components`, and the platform
`servoy-client`). Usage was checked in `*_server.js`, legacy AngularJS `.js`, migrated
Angular `.ts`/`.html`, and the platform Java code.

The audit shows the ticket's premise is **only partially valid**, and applying it
literally would produce wrong results for most of the listed properties:

**1. The entire `servoy-extra-components` section rests on a false premise.**
Every listed component there is still **legacy AngularJS**, not migrated to Angular. Their
`.spec` `definition` points at a plain `.js` file
(`servoy-extra-components/components/*/​*.spec:7` — e.g. `"servoyextra/slider/slider.js"`,
`"servoyextra/table/table.js"`, `"servoyextra/dbtreeview/dbtreeview.js"`,
`"servoyextra/splitpane/splitpane.js"`, `"servoyextra/select2tokenizer/select2tokenizer.js"`).
The "no corresponding `@Input`" heuristic does not apply — these components read the
properties from `$scope.model`. Usage counts confirm they are live, not dead:
`roots` (dbtreeview) 38 refs, `panes` (splitpane) 23, `sortColumnIndex` (table) 12,
`valueSeparator` (select2tokenizer) 9, `stepsArray`/`text` (slider/spinner) 7 each,
`readOnly`/`ticksArray`/`searchingText`/`displaysTags`/`findmode` all >0. **None are
removal candidates.** The `serveronly` tag here just means "not pushed to the browser as
a bound attribute," which is correct for AngularJS components.

**2. The `errorShow` properties (5 floatlabel components) are false positives for the
heuristic.** In the migrated Angular components they are declared as an internal `signal`
plus an `output` (`errorShowChange`), not as `input()` —
`floatlabelcalendar.ts:18` `readonly errorShow = signal<...>(undefined)` and
`:19 errorShowChange = output<boolean>()`, written at `:36`/`:41`. They are used
client-side (6 references each in `.ts`/`.html`) and pushed back to the server
(`pushToServer: "allow"`). They are correctly server-only for the *down* direction and
should be **kept**. The "no `@Input`" signal is misleading because the migration uses
signals/outputs rather than `@Input`.

**3. aggrid `groupingtable` — mixed:**
- `_internalVisible` — used 3× in `groupingtable_server.js` → **keep (server logic)**.
- `keepAppliedFilterOnHide` — used in `groupingtable_server.js` (+ doc) → **keep**.
- `hashedColumns` — introduced at `groupingtable.spec:45` by commit `7ec67ff5`
  ("add designsize property to grid component specs [ai]", 2026-08-18). A full-repo and
  platform search finds **zero** references anywhere except the spec declaration itself.
  This is the one clear **remove** candidate.

**4. bootstrap — mixed:**
- `containerStyleClass` (accordion) — a genuine **migration regression**, not simple dead
  code. The old AngularJS template still binds it (`accordion/accordionpanel.html:8`
  `ng-class='model.containerStyleClass'`), but accordion has been migrated to Angular
  (`projects/bootstrapcomponents/src/accordion/accordion.html` + `accordion.ts`) and the
  new template **never binds it**. Sample solutions still set it
  (`bootstrapComponentsSample/forms/accordionForm.frm`/`.js`). So the property was
  client-facing and the binding was lost in migration. Correct action is most likely
  **add the binding back**, not remove.
- `valuelistConfig` (typeahead, floatlabeltypeahead) — this is the config target of the
  `valuelistID` property (`typeahead.spec:22` `"config": "valuelistConfig"`). It is a
  valuelist-mechanism config object consumed by the platform's valuelist handling, not a
  bound `@Input`. It appears only in `*_doc.js` otherwise. Very likely **keep**, but
  confirmation of the valuelist-config contract is warranted before touching it.

## Ticket premise check

The ticket proposes a per-property audit with three outcomes. The audit *activity* is
reasonable, but the premise that these are candidates for removal because they lack an
`@Input` **does not hold** for the majority of the list:

- `servoy-extra-components` entries are not migrated at all — the heuristic is inapplicable.
- `errorShow` uses signals/outputs — inapplicable.
- Server-side-used properties (`_internalVisible`, `keepAppliedFilterOnHide`) are correct.
- Only `hashedColumns` is clearly removable; `containerStyleClass` is a *restore* case,
  not a *remove* case.

So the list conflates several different situations under one heuristic, and a literal
"remove unused serveronly props" pass would incorrectly delete live properties.

## Approaches considered

1. **Scope the fix to the two real findings only** (remove `hashedColumns`; restore the
   `containerStyleClass` binding in the migrated accordion) and mark everything else
   keep-as-is. — Pros: small, safe, evidence-backed, no risk to live components. Cons:
   narrower than the ticket's stated list; needs sign-off that the broad audit is
   considered "done" with these conclusions.
2. **Full audit deliverable** — categorize all ~25 properties in a doc/comment and only
   change `hashedColumns` + `containerStyleClass`. — Pros: satisfies the acceptance
   criteria (each property categorized). Cons: mostly documentation; the "changes" are
   limited to the two findings above.
3. **Split by repository** — the three repos are independent git repos with their own
   release cycles; do three separate PRs. — Pros: matches repo ownership. Cons: more
   overhead for two tiny code changes.
4. **No code change** — treat the tags as correct and close. — Pros: nothing breaks.
   Cons: leaves the `hashedColumns` dead spec entry and the accordion
   `containerStyleClass` regression unaddressed.

## Recommendation

This needs a human decision because the ticket spans **three separate component
repositories** (each with its own release process) and its central premise is largely
incorrect, so the "right" outcome depends on what the reporter actually wants out of the
audit. The concrete code work is tiny (one removal, one binding restore); the rest is
categorization the reporter may want captured as documentation. Before writing a spec I
need direction on scope, on the `containerStyleClass` regression, and on the `valuelist
Config` contract.

## Git history findings

- `hashedColumns` — `aggridcomponents` commit `7ec67ff5` ("add designsize property to
  grid component specs [ai]", 2026-08-18). Added with `serveronly` from the start; no
  usage ever added. `git log -S hashedColumns` on the grouping table shows no
  functional commit introducing a reader.
- `containerStyleClass` — still bound in legacy `accordion/accordionpanel.html:8`; absent
  from the migrated `projects/bootstrapcomponents/src/accordion/accordion.html`,
  confirming the binding was dropped during the Angular migration rather than being
  intentionally server-only.

## Questions for the reporter

1. This audit covers three independent component repositories (`bootstrapcomponents`,
   `aggridcomponents`, `servoy-extra-components`). Do you want a single tracking effort
   with separate PRs per repo, or should each repo be handled under its own case?

2. The `servoy-extra-components` components in the list (dbtreeview, slider, table,
   splitpane, select2tokenizer, htmlarea, spinner) are still legacy AngularJS — they read
   these properties from `$scope.model`, and all of the listed properties are actually in
   use. The "no `@Input`" premise doesn't apply to them. Can we mark this whole group as
   **keep (correctly server-only)** and drop it from the audit?

3. The five `errorShow` properties (floatlabel components) are implemented in the migrated
   Angular code as an internal `signal` plus an `errorShowChange` `output`, and are pushed
   back to the server. They are genuinely server-only in the down direction. Confirm we
   keep these as-is?

4. For `groupingtable`, only `hashedColumns` appears genuinely unused (zero references
   anywhere). `_internalVisible` and `keepAppliedFilterOnHide` are used in
   `groupingtable_server.js`. Are you OK with removing only `hashedColumns` and keeping
   the other two?

5. `containerStyleClass` on the accordion looks like a **migration regression**, not dead
   code: the old AngularJS template bound it (`accordionpanel.html`), but the migrated
   Angular template dropped the binding, and sample solutions still set it. Do you want
   the client binding restored (recommended) rather than the property removed?

6. `valuelistConfig` (typeahead / floatlabeltypeahead) is the `config` object referenced
   by the `valuelistID` property and consumed by the platform valuelist mechanism, not a
   plain `@Input`. Can we confirm it stays as **keep**?

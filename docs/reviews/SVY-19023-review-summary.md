# SVY-19023 — Peer Review Summary

**Risk: MODERATE.** No unresolved code defect remains: every regression this review
surfaced is filed as its own case, accepted as intended behaviour, or already fixed. What
is left is reach that cannot be verified from a single checkout — `@servoy/public`'s
`ServoyBaseComponent` changed shape for every external component package — plus one
provisional `// commented out for verification` state that is still provisional.

**Reviewed:** the merged 2026 implementation across four repositories —
`servoy-eclipse` `577f557943~1..e1671a439d` (47 commits, 236 files, +24981/−23047),
`sablo` `c06ffdde`+`ba469cec`, `servoy-client` `1de9ae7a0`/`b7dfa0662`/`599d9a2db`/`8502a3250`/`db5618a69`,
`bootstrapcomponents` `872f86e`/`1e51250`/`2ae30c3`/`257205b`. All merged to `master` and
`release`; `lts_2026` does not contain them.

**Acted on during the review** (2026-10-08, all on `release`): `e4e605b0df` restored the 16
emptied `servoydefault` specs and pointed the Maven test execution at a new `test_all`
script covering every Angular project; `7f7f591c0e` made Jenkins collect all the vitest
reports; `c62dac798a` prefixed vitest classnames with `angular.`. Net effect — the suite is
green and visible (`release » servoy-eclipse #195`: 1307 tests, 0 failed, 11 pre-existing
skips), so this area can no longer lose coverage silently. The three items below are
decisions, not defects.

---

## What the change does

TiNG's internal components all became `standalone: true`, `AppModule`/`bootstrapModule`
was replaced with `bootstrapApplication()`, a new `NG2-Components:` MANIFEST key lets a
package without an NgModule be registered, `@servoy/public`'s `ServoyBaseComponent` moved
to signal inputs, and a `serveronly` spec tag stops `ComponentTemplateGenerator` from
emitting `[prop]` bindings for properties the Angular class cannot accept (which is what
allowed `CUSTOM_ELEMENTS_SCHEMA` to be removed).

The two mechanisms that carry the blast radius, for anyone touching this area later:

- **`@servoy/public` is a published library.** Changing an exported base class breaks every
  external component package that extends it. `257205b` shows the per-package cost:
  46 files for bootstrapcomponents alone.
- **`.spec` files are the server↔client contract, and the generator runs at developer
  time.** A spec-level removal surfaces as a blank render or an Angular compile error in
  generated code, far from its cause and with no compile-time link. Three filed regressions
  came out of exactly this.

---

## Manual test plan

Steps a human still has to run; the automated suites do not reach these.

1. **Element-group geometry.** Absolute-layout form, two grouped servoydefault buttons,
   then from script: `elements.myGroup.width` and `elements.myGroup.setSize(200,100)`.
   `RuntimeWebGroup.getBounds()` dereferences `getProperty("size")` and
   `getProperty("location")` with no null check, and neither is declared in most specs nor
   injected any more. Contrast with a group of `rectangle`/`splitpane`/`tabpanel`, which
   kept their spec `size`. (Low priority — the file's substantive history stops in 2021 and
   grouping is absolute-layout only, so this is a dormant corner.)
2. **Third-party component default sizing.** Drop a component whose spec declares neither
   `size` nor `designsize`; check the palette preview, the drop default size, and
   `state.model.size` in devtools. In-house, the bootstrap `accordion`, `tabpanel`,
   `tablesspanel`, `formcomponent` and `progressbar` specs are in that position.
3. **Legacy geometry scripting.** `elements.myButton.getWidth()` / `.setSize()` on a
   servoydefault button, whose spec no longer declares `size`. Should work via the
   `cssPosition` fallback added to `RuntimeLegacyComponent` after this range — confirms
   SVY-21467's class of symptom is closed.
4. **Legacy MANIFEST fallback.** Install or hand-edit a package declaring only
   `NG2-Module:` and confirm it still generates the old-shaped import and renders. This is
   what protects every third-party package in the wild;
   `AllComponentsModuleGeneratorTest` covers it as a unit but not end to end.
5. **The originally reported symptom**, never exercised anywhere in the change: build a
   trivial package whose Angular classes are standalone with no NgModule, declare it with
   `NG2-Components`, install it, render a component.
6. **Missing-spec fallback.** Put a component on a form and uninstall its package.
   **Confirmed working on lts_2026, release and master** (reviewer-verified in the
   Developer): the Error Bean is substituted and the Properties view shows
   `Error Bean (Servoy Core)` with `error = Specification not found.`. That path does not
   depend on the generated Angular template — `FormElement.java:191` and
   `GhostHandler.java:207` substitute `FormElement.ERROR_BEAN` independently, and the
   message text is the spec's own `"error"` default. SVY-21380 was the narrower case of the
   form-editor canvas blanking, fixed on `master`; `release` keeps `"deprecated": "true"` on
   `errorbean.spec` by decision.

**Automated:** `cd com.servoy.eclipse.ngclient.ui/node` → `npm run test_all` (added by
`e4e605b0df`; covers ngclient2, public, servoydefault, dialogs, window, ngclientutils with
per-project JUnit XML). All of it runs in Jenkins now and is green — 1307 tests, 0 failed,
11 pre-existing skips — so a regression in this area will be visible rather than silent,
which was not true while this case was in review. `npm run lint` plus `npm run lint:slow` —
the local `lint` script no longer covers `*.html`, so template and accessibility rules only
run via `lint:ci`. Java: `ComponentTemplateGeneratorTest` and
`AllComponentsModuleGeneratorTest` in `com.servoy.eclipse.ngclient.ui.tests`
(PDE launcher ≈ 15 min here).

**Surfaces:** NG client runtime, the form-designer content iframe, the developer-time
generation path, and **solution scripting against element geometry** — that last one is
easy to forget because it is reached from JavaScript, not the UI. No coverage needed for
the RFB/WPM Angular apps (only a dead dependency removed) or the legacy AngularJS layer
(not a deployable target for 2026.9).

---

## Possible improvements / follow-ups

1. **`DefaultComponentPropertiesProvider` is still provisional.** The `location`, `size`,
   `anchors` and `formIndex` blocks are commented out with "commented out for verification"
   — unchanged on `release` and `master` two months on. Decide whether to delete them or
   restore them. Third-party packages receive neither these nor the later compensating
   `designsize`, so they fall through to hard-coded 80×80 / 100×100 defaults; if that is
   the intended end state for external packages, it is worth saying so explicitly. (The
   `formIndex` documentation string from SVY-20651 lives inside the commented block and
   goes away with it, which is correct if the property is really gone.)
2. **servoydefault MANIFEST is 10 characters from breaking.** `war/servoydefault/META-INF/MANIFEST.MF`'s
   `NG2-Components:` line is **501 bytes** (501 chars, LF-terminated — the repo is
   `* text=auto eol=lf`), against the 512-byte buffer `java.util.jar.Manifest` reads a
   header line into. Measured by appending padding and re-parsing with the JDK's own
   `Manifest`: `+10` chars still parses, `+11` throws `IOException: line too long`. Since the
   existing class names are 18–26 chars, **one more component cannot be added** — a comma
   plus a name overshoots by ~13. The failure is what matters more than the margin:
   `DirPackageReader.getManifest()` propagates the `IOException` to
   `WebSpecReader.cacheWebObjectSpecs`, which swallows it into a single
   "Cannot read web component/service or layout specs from package" log line, so **every
   servoydefault component silently disappears from the workspace** with no visible link to
   the manifest. `bootstrapcomponents/components/META-INF/MANIFEST.MF` already wraps its
   equivalent value across continuation lines at 72 columns and parses to the identical
   value, so aligning servoydefault removes the limit entirely. Worth doing before someone
   hits it.
3. **`isDeprecated()` now carries two meanings.** Across ~20 designer call sites
   (`AddContainerCommand:152`, `CreateComponentHandler:120`, `ConversionComponentDialog:98`,
   `DesignerUtil:434,478,508`, `SolutionExplorerListContentProvider:2253`) it means *hide
   this from developers*. `ComponentTemplateGenerator:115` borrowed it to mean *do not
   generate client code*. Those coincide for most components and diverge for
   framework-substituted ones — which is how SVY-21380 happened. The same line was removed
   deliberately in 2021 (`4322314c8f`: "we can't skip deprecated components … else we need
   an extra spec property that they can be ignored for ng2"), and that spec property now
   exists as `serveronly` / `skiptemplate`. Driving the skip from an explicit tag would stop
   this recurring with a different component. Low urgency — the `deprecated` flags currently
   in place are all wanted (reviewer decision: `errorbean` and `tablesspanel` stay
   deprecated), so this is about making the generator's intent explicit rather than fixing
   anything broken.
   Related: `WebObjectSpecification.isDeprecated()` returns true for any spec declaring a
   `replacement` key regardless of the `deprecated` flag — `!"".equals("replacement")`
   compares a literal to `""`. Pre-existing since 2019 (`dd03571c`), but the generator is
   the first caller to act on it.
4. **`serveronly` tag selection.** The tags were chosen by "the Angular component has no
   matching `@Input`". SVY-21341 (still open) already reverted two of six bootstrapcomponents
   tags because those properties did have client bindings. Worth finishing that audit.
   Two leftovers for whoever does:
   - `tabpanel.spec` has `activeTabIndex` tagged `serveronly` *and* `"pushToServer": "shallow"`
     — pushable by the client, never sent to it.
   - `servoycore-slider.enabled` is a case where the tag is *accurate* but points at a gap in
     the component: `ServoyCoreSlider` has never had an `enabled` input and `slider.html` has
     never had a `[disabled]` binding (verified at the range base and across the file's whole
     history), so a security-disabled slider stays interactive — the server still rejects the
     push, so nothing is written. The tag only stopped emitting a binding the component was
     already ignoring, i.e. **behaviour is unchanged by this work and nothing has ever been
     reported**. No separate case needed; if it is ever reported, the fix is an `enabled`
     input plus `[disabled]="!enabled()"`, as `calendar.html:21`, `combobox.html:14` and
     `tabpanel.html:6` already do.
5. **A base-class breaking change shipped as a patch bump** (`@servoy/public`
   `2026.9.1 → 2026.9.2`). Accepted — external and customer packages convert — but a
   minor/major bump would surface a clear dependency-resolution error instead of a
   confusing compile error inside `target/<solution>/`.
6. **`prefer-on-push-component-change-detection` is disabled** with "re-enable when
   migrating to zoneless (Phase 7)", while `provideZonelessChangeDetection()` is already
   active. Low impact — the 19 `Eager` components are deliberate opt-ins and
   `provideCheckNoChangesConfig` is in place — but a new component could slip through on
   the default strategy unnoticed.
7. **Documentation drift worth a sweep** while this is fresh: `node/AGENTS.md` §11 claims
   "Standalone components: 100%" beside a new "Phase 6b" listing the NgModules that remain;
   §2 still lists the deleted `karma*.conf.js` files and §10 a `test_edge` script that no
   longer exists; §11 still files "`@Input()` → signals" under *Architecturally Blocked*
   although `aec6cd8b6e` (inside this range) established that signal inputs do fire
   `ngOnChanges`. Also `8502a3250`'s message says `errorbean` has "no Angular
   implementation" — true of `defaultloadingindicator`, not of `errorbean`.

---

## Resolved during the review

| Item | Outcome |
|---|---|
| `servoycore-errorbean` template no longer generated | **SVY-21380**, fixed on `master`. Narrower than it first looked: the missing-spec *diagnostic* still works on all three branches (reviewer-verified — Error Bean is substituted and the Properties view shows `error = Specification not found.`, via `FormElement.java:191` / `GhostHandler.java:207`, neither of which needs the generated template). What SVY-21380 fixed was the form-editor canvas blanking in one scenario. `release` keeps `"deprecated": "true"` on `errorbean.spec` by decision — no action wanted |
| `model.size` undefined after `size` left the specs | **SVY-21483** |
| Legacy `getWidth`/`getHeight` warnings | **SVY-21467** |
| `serveronly` tags on properties with client bindings | **SVY-21341** (open) |
| `bootstrapcomponents-tablesspanel` stops rendering | Accepted — deprecated may stop rendering on upgrade |
| Properties withheld from the legacy AngularJS client | Dropped — NG1 is not a deployable target for 2026.9 |
| 16 emptied `servoydefault` `*.spec.ts` files | **Fixed and CI-verified** (2026-10-08). `e4e605b0df` restored all 16 with the intended signal migration and added `test_all` so Maven runs every Angular project again — the narrowed test execution is why the loss went unnoticed for two months. `7f7f591c0e` then made Jenkins collect every vitest report (per-project plus the RFB browser suite) and `c62dac798a` prefixed vitest classnames with `angular.` so they do not collide with the Java packages in the Jenkins view. Confirmed green on `release » servoy-eclipse #195`: **1307 tests, 0 failed, 11 skipped, 49 sec**, with all six Angular projects reporting separately. The 11 skips are pre-existing `it.skip`/`describe.skip` markers, not artifacts of the restore — verified against the range base `9f19aa017a`: checkgroup 2, radio 1, spinner 4, form_component 1 `describe.skip` (3 tests), identical counts before and after |
| `viewChild('element')` missing `{ read: ElementRef }` | Already repaired by `0d90957d8a` |
| `cssPosition` tagged `serveronly` | Already repaired to `skiptemplate` by `8246c4f01` + `1961be795d` |

---

## Notes for the next reader

- `servoy_ngclient/war/` holds the legacy AngularJS assets **and** the `.spec` files and
  MANIFESTs that drive TiNG. The `NG2-Components` / `NPM-PackageName` / `Entry-Point` /
  `NG2-CSS-ClientLibs` keys this case edited all live there, so dropping NG1 removes the
  `.js`/`.html` consumers in that tree but not the spec metadata.
- `origin/release` is a strict **ancestor** of `origin/master` (2026.9 vs 2026.12) and the
  merge flow is `lts_2026 → release → master`. A fix present only on `master` is a
  later-line fix, not a pending forward merge.
- `d1794cf2fd` (prettier, 110 files, ±21k lines) is ~88% of the line churn, and
  `c2c2f0a4a0` is a 578-warning `eslint --fix` over 51 files. The change is far smaller
  than its diffstat suggests.
- The author's own `docs/SVY-19023-triage.md` and
  `docs/SVY-19023-standalone-components-migration.spec.md` are committed inside the range
  and are the best starting point. §6 of the spec lists the signal-input migration as out
  of scope; it shipped anyway, unblocked mid-stream by `aec6cd8b6e`.

**Reviewed scope:** `servoy-eclipse` `577f557943~1..e1671a439d` (+ `sablo`, `servoy-client`,
`bootstrapcomponents` as listed above), on `release`.

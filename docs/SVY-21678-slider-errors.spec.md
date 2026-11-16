# Spec: SVY-21678 — Errors when a form contains the slider component

## 1. Goal

A form containing the `servoyextra-slider` web component must render at runtime **without
flooding the console with errors**. The sample (`Sample2.servoy`) currently produces two
distinct, independent slider error families on every pointer/refresh:

- **(A)** a hard `TypeError: patchedFormat.indexOf is not a function` thrown out of the
  slider's `formatValue()` → `FormattingService.format` path whenever the slider's
  `numberFormat.display` is not a usable string; and
- **(B)** an `NG0100 ExpressionChangedAfterItHasBeenCheckedError` flood on the ngx-slider
  `animate` host binding, driven by the Servoy side replacing the whole `Options` object on
  every `svyOnChanges` under zoneless OnPush.

This spec fixes both families. (A) is fixed primarily in the slider component
(`servoy-extra-components`) with a secondary defensive guard in this repo's `@servoy/public`
`FormattingService`. (B) is stabilized in the slider component by not replacing/re-applying
the `Options` object when nothing actually changed. The two defects are independent; both
must be fixed (fixing only one leaves the other symptom live). The OpenAI
`ClassNotFoundException` in the same log is unrelated noise and is explicitly out of scope.

## 2. Background

### 2.1 The three log error families & which are in scope

The attached `servoy_log.txt` contains three unrelated error families (confirmed by the
triage, `docs/SVY-21678-triage.md`):

| Count | Error | Origin | In scope? |
|------:|-------|--------|-----------|
| 650 | `NG0100 ExpressionChangedAfterItHasBeenCheckedError` — `'animate'` flips (plus some `NgbNavOutlet -1 → 0`) | `SliderComponent_HostBindings` inside the third-party `@angular-slider/ngx-slider` (the `NgbNavOutlet` ones are a bootstrap tabpanel, unrelated) | **Yes — family (B)**, slider part only |
| 15 | `TypeError: patchedFormat.indexOf is not a function` | `FormattingService.formatNumbers` ← `ServoyExtraSlider.formatValue` ← ngx-slider `Options.translate` | **Yes — family (A)** |
| few | `ClassNotFoundException: com.openai.models.ReasoningEffort$Known` in `XMLScriptObjectAdapterLoader` | Servoy Developer AI/OpenAI plugin startup | **No — out of scope (§6)** |

The ticket itself is a pure "lots of errors, please fix" report (description: *"i get a lot
of errors in the Dev console, tried different dp types for the slider and it still
reproduces"*), `Minor`, component `NGClient`, fixVersion `2026.12`, currently In Progress. No
proposed solution or comments; the attachments are the sample and the log. The triage already
analyzed both — this spec reuses that analysis and does not re-dig the log.

### 2.2 The TypeError path (family A)

**Slider component** — `components/projects/servoyextracomponents/src/slider/slider.ts`
`formatValue()` (lines 237–249):

```ts
formatValue(value: number, label: LabelType): string {
    if (this.formattingFunctionParsed && typeof this.formattingFunctionParsed === 'function') {
        return this.formattingFunctionParsed(value, label);
    }
    const numberFormat = this.numberFormat();
    if (numberFormat && numberFormat.type === 'NUMBER') {
        const format = new Format();
        format.display = numberFormat.display;   // <-- display can be non-string / null / undefined
        format.type = 'NUMBER';
        return this.formatService.format(value, format, false);
    }
    return value.toString();
}
```

`formatValue` is the slider's ngx-slider `Options.translate` callback (set in the `options`
signal initializer, slider.ts:72–74), so it runs for every label/aria render. The
`numberFormat` input is the Servoy `format`-typed spec property (slider.spec:25:
`"numberFormat": { "type": "format", "for":["dataProvider"], ... }`). The component copies
`numberFormat.display` straight onto a hand-rolled `Format` **without checking it is a
string**.

**`@servoy/public` FormattingService** (this repo) —
`com.servoy.eclipse.ngclient.ui/node/projects/servoy-public/src/lib/format/formatting.service.ts`:

`format(...)` (lines 45–66) guards only the obvious cases:

```ts
public format(data: any, format: Format, useEditFormat: boolean): string {
    if ((!format) || (!format.type) || ((typeof data === 'number') && isNaN(data))) {
        ...
    }
    const formatString = useEditFormat ? format.edit : format.display;
    ...
    if ((format.type === 'NUMBER') || (format.type === 'INTEGER')) {
        return this.formatNumbers(data, formatString);   // formatString === format.display here
    }
    ...
}
```

`formatNumbers(data, servoyFormat)` (lines 389–...) then does, after the `if (!servoyFormat)
return data;` / empty-string guards, the first string operation at line 417:

```ts
if (patchedFormat.indexOf(';') > 0) { ... }   // patchedFormat = servoyFormat = format.display
```

The existing `format(...)` guard (`!format || !format.type`) and the `formatNumbers`
`!servoyFormat` guard both **pass** when `format.type === 'NUMBER'` but `format.display` is a
non-string (e.g. an object, or a truthy non-string). `.indexOf` does not exist on that value
and it throws `TypeError: patchedFormat.indexOf is not a function`. (`Format.display` is typed
`string` — see `Format` class, formatting.service.ts:13–26 — but the slider assigns from an
untyped `numberFormat.display: any`, so the type system does not catch it.)

Default components never hit this because they bind the server `Format` straight through
`[svyFormat]`/`FormatDirective`; only the slider hand-rolls a `Format` and copies
`numberFormat.display` unchecked, slipping past every existing guard. This is a
deterministic component bug — it fires on every slider label/aria render whenever
`numberFormat` is set with a non-string/missing `display` (which is why the reporter sees it
"with different dp types").

### 2.3 The NG0100 `animate` trigger (family B)

The `NG0100 ExpressionChangedAfterItHasBeenCheckedError` on the `'animate'` expression is
emitted from `SliderComponent_HostBindings` inside the third-party `@angular-slider/ngx-slider`,
not from Servoy template code. The real cause (found by reading the library source) is a
**zoneless change-detection notification gap in ngx-slider itself**:

- ngx-slider defaults `animate = true` and binds `class.animate` to its own
  `sliderElementAnimateClass` host field (initial value `false`).
- It flips that field to `true` **asynchronously**, inside a `setTimeout(...)` queued from its
  own `ngAfterViewInit`/`applyOptions()`
  (`angular-slider-ngx-slider.mjs`: `if (this.sliderElementAnimateClass !== this.viewOptions.animate) { setTimeout(() => { this.sliderElementAnimateClass = this.viewOptions.animate; }); }`),
  and does **not** call `markForCheck()` afterwards.
- Under this app's zoneless change detection (`provideZonelessChangeDetection()`) nothing
  re-renders the host binding after that timeout, so the DOM keeps the stale `false`. The
  app's dev-mode exhaustive no-changes check
  (`provideCheckNoChangesConfig({ exhaustive: true, interval: 500 })` in
  `com.servoy.eclipse.ngclient.ui/node/src/main.ts`) then runs a verification pass, reads the
  component's now-`true` value against the rendered `false`, and throws NG0100.

This is a notification gap (the host binding changes outside any Angular-scheduled CD), not a
Servoy options-churn bug. Earlier experiments that tried to tame the Servoy side — a
per-property equality guard and batching/deferring `setNewOptions`, or forcing `animate:false`
— were investigated and **rejected**: equality/batching only reduced a separate, larger
console flood but left the intrinsic init-time flip (the browser still showed the errors), and
`animate:false` removed the error only by permanently disabling the slider's CSS animation
(an unacceptable behaviour change; re-enabling it brought the error straight back because the
`false→true` transition re-triggers the same ngx-slider flip). The per-property churn is **not**
the root cause and `setNewOptions` is left as-is.

The `NgbNavOutlet -1 → 0` NG0100 entries in the log are from a bootstrap tabpanel in the
sample, not the slider, and are not addressed here.

### 2.4 Git history (reused from triage)

`servoy-extra-components` slider history (sibling repo, branch `master`, HEAD `14d7199`
2026-11-12):

- `ed387c9` SVY-21889 "convert template-bound properties to signals" (2026-08-20) — changed
  `[options]="options"` → `[options]="options()"` and made `options` a `signal`. Most
  consistent with the `animate` NG0100 instability surfacing.
- `8e2e88c` SVY-21889 convert to standalone; `e3cfa47` constructor-DI → `inject()`.
- `14d7199` last touched `formatValue()`; the unchecked `format.display = numberFormat.display`
  assignment predates these commits and is the latent `TypeError` bug.

`@servoy/public` `FormattingService.formatNumbers` (this repo) has always assumed a string
`display` (`patchedFormat.indexOf(';')` at line 417) with no non-string guard. No prior
`docs/SVY-21678-*.spec.md` exists in either repo.

### 2.5 The two-repo layout

| What | Repo | File |
|------|------|------|
| Slider component (fix (A) primary + fix (B)) | **`servoy-extra-components`** (sibling checkout, own git + Angular/Vitest node tooling) | `components/projects/servoyextracomponents/src/slider/slider.ts`, `.html`, `.spec.ts`; `components/slider/slider.spec` |
| `@servoy/public` FormattingService (fix (A) defensive guard) | **this `servoy-eclipse` repo** | `com.servoy.eclipse.ngclient.ui/node/projects/servoy-public/src/lib/format/formatting.service.ts` |

The slider component depends on `@servoy/public` `2026.9.4` (package.json:33) — a **published**
npm version, not a `file:` link to this checkout. So the `@servoy/public` guard is a separate,
independent robustness fix shipped from this repo; the slider fix does not need to wait for it,
and the slider's own guard is what removes the error in the reported sample.

**Tooling per repo:**
- `servoy-extra-components` (from `components/`): build `npm run build` /
  `npm run build_debug_nowatch`, lint `npm run lint` (ESLint 10, all rules warn via
  `eslint-plugin-only-warn` but warnings should be fixed), test `npm run test` (Vitest, single
  run) or scoped `npx ng test @servoy/servoyextracomponents --no-watch --include "projects/servoyextracomponents/src/slider/slider.spec.ts"`.
  Components here are `standalone: true` and use signal inputs.
- `servoy-eclipse` `@servoy/public` (from `com.servoy.eclipse.ngclient.ui/node/`): typecheck
  `npx tsc --noEmit -p projects/servoy-public/tsconfig.lib.json`, lint `npm run lint`, rebuild
  the lib `npm run build_lib`, test `npm run test_public` (Vitest).

## 3. Design

### 3.1 TypeError fix (family A) — slider guard (primary) + `@servoy/public` guard (defensive)

**Primary — `slider.ts` `formatValue()` [servoy-extra-components].** Only build a `Format`
and delegate to `FormattingService.format` when `numberFormat.display` is a usable string;
otherwise fall back. This removes the TypeError at the exact site that produces it in the
reported sample:

```ts
const numberFormat = this.numberFormat();
if (numberFormat && numberFormat.type === 'NUMBER' && typeof numberFormat.display === 'string') {
    const format = new Format();
    format.display = numberFormat.display;
    format.type = 'NUMBER';
    return this.formatService.format(value, format, false);
}
return value?.toString() ?? '';
```

- The guard is `typeof numberFormat.display === 'string'` — the minimal, exact condition that
  distinguishes the working case from the throwing case. An empty-string `display` still goes
  through `format`; `formatNumbers` already returns the data unchanged for a falsy
  `servoyFormat`, so behavior for valid formats is unchanged.
- The fallback mirrors the existing `return value.toString()` at the end of the method, made
  null-safe (`value?.toString() ?? ''`) because `translate` can be invoked with edge values;
  the exact fallback string is an open question (§7) but `value?.toString()` matches today's
  non-format behavior.

**Defensive (secondary) — `FormattingService.formatNumbers` [servoy-eclipse @servoy/public].**
Harden the shared service so **no** component can trigger
`patchedFormat.indexOf is not a function` again, even if it feeds a bad `display`. Add a
non-string guard at the top of `formatNumbers` (after the existing `if (!servoyFormat) return
data;` / `if (data === '') return data;` guards, before `let nmbr = Number(data);` and the
`patchedFormat.indexOf(';')` at line 417):

```ts
if (typeof servoyFormat !== 'string') {
    // no usable format string -> return the data unformatted rather than throwing
    return data;
}
```

- **Bail out, do not coerce.** Returning `data` unformatted is consistent with the method's
  existing early returns (`if (!servoyFormat) return data;`) and does not risk producing a
  wrong formatted string from a garbage format. (Coercion — `String(servoyFormat)` — is
  rejected: it would feed a meaningless string like `"[object Object]"` into the formatter.)
- This changes no valid-format behavior: when `servoyFormat` is the normal string `display`,
  the new branch is skipped entirely. It only intercepts the previously-throwing non-string
  case.

**Which is primary:** the slider guard is primary (it fixes the component that reaches the
service badly, and is what clears the sample's errors against the published `@servoy/public`).
The `@servoy/public` guard is a defense-in-depth change in this repo that protects every
consumer and is shipped independently.

### 3.2 NG0100 fix (family B) — mark the view for check after ngx-slider's async animate flip

The NG0100 is a zoneless change-detection notification gap in ngx-slider (see §2.3): the
library flips its `class.animate` host binding in a `setTimeout` queued from its own
`ngAfterViewInit`, without calling `markForCheck()`, so under `provideZonelessChangeDetection()`
the rendered binding stays stale until the dev-mode exhaustive no-changes check throws.

The fix schedules a Servoy-side `markForCheck()` right after ngx-slider's own timeout, so a
real change-detection pass re-renders the host binding before the verification check reads it:

```ts
ngAfterViewInit() {
    super.ngAfterViewInit();
    // ngx-slider turns on its class.animate host binding in a setTimeout queued from its own
    // ngAfterViewInit, without notifying Angular. Under zoneless CD nothing re-renders that
    // binding, so the dev-mode no-changes check reports NG0100 (SVY-21678). The child's timeout
    // is queued before this one, so marking this view for check here lets a real CD pass pick
    // up the new class value.
    setTimeout(() => this.cdRef.markForCheck());
}
```

- The slider's own `setTimeout` (from ngx-slider's `ngAfterViewInit`, which runs **before** the
  parent's `ngAfterViewInit`) is queued ahead of this one, so by the time our callback runs the
  library has already set its `animate` field; `markForCheck()` then schedules the CD pass that
  renders it.
- `animate` keeps ngx-slider's **default** (`true`) — the slider still animates. No `animate`
  option is set by Servoy, so there is no `false→true` transition to re-trigger the flip.
- `setNewOptions` is left exactly as it was (`Object.assign({}, this.options())` +
  `this.options.set(...)`). The per-property churn was investigated and is **not** the root
  cause (see §2.3), so it is not changed.

### 3.3 Rejected family-B approaches (investigated, not shipped)

These were tried and discarded, recorded so the decision is not re-litigated:

1. **Per-property equality guard in `setNewOptions`** (skip `options.set` when the value is
   unchanged) — reduced a separate large console flood but left the intrinsic init-time
   `animate` flip; the browser still showed the errors. Not the root cause.
2. **Batching + microtask-deferring the option flush per change cycle** — same outcome: the
   remaining errors are from ngx-slider's own async host-binding flip, independent of when
   Servoy sets `[options]`.
3. **`animate: false` (permanent)** — removed the error only by disabling the slider's CSS
   animation (an unacceptable behaviour change). Re-enabling it later (`false→true`) brought the
   error straight back, since the transition re-triggers the ngx-slider flip.

The shipped fix (§3.2) keeps the animation on and addresses the actual notification gap.

### 3.4 What is explicitly NOT changed

- **ngx-slider internals** (`@angular-slider/ngx-slider`) — not patched or forked; the fix is a
  Servoy-side `markForCheck()` plus the `formatValue` format guard.
- **`setNewOptions` / the options-churn path** — left as-is (not the root cause).
- **The OpenAI `ClassNotFoundException`** — unrelated Developer AI/OpenAI startup noise, out of
  scope (§6).
- **The `NgbNavOutlet` NG0100** — a bootstrap tabpanel in the sample, not the slider; out of
  scope.
- **Valid number-format rendering** — the slider guard only adds a `typeof === 'string'`
  condition and the service guard only intercepts non-strings, so a real `display` string
  formats exactly as today.

## 4. Implementation plan

Ordered. Each step is tagged with the repo it lives in. Do (A) before (B).

1. **[servoy-extra-components] `formatValue` string guard (fix A, primary).** In
   `components/projects/servoyextracomponents/src/slider/slider.ts`, change the
   `if (numberFormat && numberFormat.type === 'NUMBER')` branch in `formatValue()` (lines
   242–247) to also require `typeof numberFormat.display === 'string'`, and make the final
   fallback null-safe (`return value?.toString() ?? '';`). No other method changes.

2. **[servoy-extra-components] NG0100 `markForCheck` fix (fix B).** In the same file, add an
   `ngAfterViewInit()` that calls `super.ngAfterViewInit()` then
   `setTimeout(() => this.cdRef.markForCheck())` (see §3.2), so Angular re-renders ngx-slider's
   async `class.animate` host-binding flip under zoneless CD. Leave `setNewOptions` and the
   `[options]="options()"` binding unchanged; do not set an `animate` option (keep the library
   default).

3. **[servoy-extra-components] extend the slider unit tests.** In
   `components/projects/servoyextracomponents/src/slider/slider.spec.ts` (Vitest; already has
   `formatValue`/`numberFormat`/`setNewOptions` tests):
   - Add a test: `formatValue` with `numberFormat = { type: 'NUMBER', display: undefined }`
     (and one with a non-string object `display`) must **not** throw and must return the plain
     value string — and must **not** call `formatService.format` (spy asserts not called),
     proving the guard short-circuits. This fails against the pre-fix version (which throws /
     calls `format`).
   - Keep the existing `formatValue delegates to the FormattingService for a NUMBER format`
     test (string `display: '#,##0.00'`) green — proves valid formatting is unchanged.
   - Add a test that `animate` keeps the ngx-slider default (not set to `false` by Servoy), and
     a test (with `vi.useFakeTimers()`) that `cdRef.markForCheck` is called after the
     `ngAfterViewInit` timeout fires — the regression guard for (B).
   - Follow this repo's AGENTS.md test conventions (`TestBed.createComponent`,
     `fixture.componentRef.setInput(...)`, `NO_ERRORS_SCHEMA`, never
     `vi.stubGlobal('document'/'window', ...)`).
   - **Build/test:** from `components/` run `npm run build` (ng-packagr full typecheck),
     `npm run lint` (address warnings), and
     `npx ng test @servoy/servoyextracomponents --no-watch --include "projects/servoyextracomponents/src/slider/slider.spec.ts"`.

4. **[servoy-eclipse @servoy/public] defensive non-string guard (fix A, secondary).** In
   `com.servoy.eclipse.ngclient.ui/node/projects/servoy-public/src/lib/format/formatting.service.ts`,
   `formatNumbers(data, servoyFormat)` (line 389), add
   `if (typeof servoyFormat !== 'string') { return data; }` immediately after the existing
   `if (!servoyFormat) return data;` / `if (data === '') return data;` guards and before
   `let nmbr = Number(data);` (so it is in place before the `patchedFormat.indexOf(';')` at
   line 417). Use the eclipse-coder tools for this file (repo AGENTS.md — it is inside an
   Eclipse workspace project; never built-in edit/write). Do not change `format(...)` or any
   valid-format behavior.

5. **[servoy-eclipse @servoy/public] add/extend the FormattingService spec.** There is
   currently **no** `formatting.service.spec.ts` next to the service. Create one under
   `com.servoy.eclipse.ngclient.ui/node/projects/servoy-public/src/lib/format/` (Vitest, this
   repo's `@servoy/public` conventions — see TiNG AGENTS.md §4: explicit `import { describe, it,
   expect } from 'vitest'`, TestBed-provided `FormattingService`, no `NO_ERRORS_SCHEMA`,
   individual providers). Assert:
   - `format(5, { type: 'NUMBER', display: undefined } as any, false)` does **not** throw and
     returns the value unformatted (`'5'` / the data) — fails against the pre-fix service
     (which throws `patchedFormat.indexOf is not a function`).
   - a valid `{ type: 'NUMBER', display: '#,##0.00' }` still formats correctly (no regression).
   - **Build/test:** from `com.servoy.eclipse.ngclient.ui/node/` run
     `npx tsc --noEmit -p projects/servoy-public/tsconfig.lib.json`, `npm run lint`, and
     `npm run test_public`. If the library artifact is consumed downstream in this checkout,
     `npm run build_lib` to rebuild `@servoy/public`.

6. **Verify end-to-end.** Reproduce with `Sample2.servoy`: confirm the
   `patchedFormat.indexOf is not a function` TypeError no longer appears, and the NG0100
   `animate` error count drops substantially from the ~650 baseline. Confirm the slider still
   renders and formats labels for a valid `numberFormat`. Lint/build/tests green in **both**
   repos.

## 5. Acceptance criteria

- [ ] **(A) slider:** `formatValue` on a slider whose `numberFormat.display` is non-string,
      `undefined`, or whose `numberFormat` is a non-`{type:'NUMBER',display:string}` shape, no
      longer throws `TypeError: patchedFormat.indexOf is not a function`; it returns the plain
      value string and does not call `FormattingService.format`. Unit-tested in
      `slider.spec.ts` (fails against the pre-fix version).
- [ ] **(A) `@servoy/public`:** `FormattingService.format`/`formatNumbers` returns the data
      unformatted (does not throw) when the format's `display` is not a string, and a valid
      string `display` formats exactly as before. Unit-tested in a new/extended
      `formatting.service` spec (fails against the pre-fix service).
- [ ] **(B):** the NG0100 `animate` `ExpressionChangedAfterItHasBeenCheckedError` no longer
      occurs when `Sample2.servoy` runs, while the slider keeps its CSS move animation
      (ngx-slider default `animate`), achieved by the `ngAfterViewInit`
      `setTimeout(() => cdRef.markForCheck())` that re-renders the library's async
      `class.animate` flip under zoneless CD. Unit-tested: `markForCheck` is called after the
      timeout; `animate` is not forced off. Confirmed in-browser (no console NG0100, slider
      still animates).
- [ ] No regression to valid number-format rendering (slider labels/aria for a real
      `numberFormat` display string) and no regression to normal slider option updates
      (changed options still apply).
- [ ] `npm run lint` (zero/addressed warnings), build, and the relevant Vitest tests pass in
      **both** repos — `servoy-extra-components` (`components/`) and
      `servoy-eclipse` `@servoy/public` (`com.servoy.eclipse.ngclient.ui/node/`).
- [ ] The OpenAI `ClassNotFoundException`, ngx-slider internals, and the `NgbNavOutlet`
      tabpanel NG0100 are left untouched (out of scope).

## 6. Out of scope

- The OpenAI `ClassNotFoundException: com.openai.models.ReasoningEffort$Known` in
  `XMLScriptObjectAdapterLoader` — unrelated Developer AI/OpenAI startup noise; split off
  separately if it is a real packaging regression.
- Patching or forking `@angular-slider/ngx-slider` internals — the fix stays on the Servoy
  side.
- The unrelated `NgbNavOutlet` (bootstrap tabpanel) NG0100 entries in the same log.
- Migrating the slider's `@Input`/`svyOnChanges` model to `effect()`/`computed()` (broader
  architectural change, not needed for this fix).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Exact non-string fallback for `formatValue` — `value?.toString() ?? ''` (matches today's non-format path) vs. returning `''` vs. `String(value)`. Implementer decision, not a blocker. | Implementer | open |
| Whether the per-property value-equality check in `setNewOptions` is sufficient for (B), or whether a narrower per-property option update / also folding the `ngOnInit` `ceil`/`floor` into the initial `options` value is needed to get the NG0100 count down far enough. Baseline: the equality guard. Implementer decision, measured against the sample. | Implementer | open |
| Whether `@servoy/public` `formatNumbers` should additionally guard the `INTEGER`/edit-format path, or only the `display`/`NUMBER` path that the slider exercises. The reported case is `NUMBER`/`display`; broader hardening is optional. | Implementer | open |

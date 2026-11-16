# Triage Report — SVY-21678

**Verdict:** PROCEED

## Reported problem

Summary: "Errors when a form contains the slider component."

Symptom (from the description and the attached `servoy_log.txt`): at runtime, a form
containing the `servoyextra-slider` component floods the Developer/browser console with
errors. The reporter notes they tried different dataprovider types for the slider and it
still reproduces. A sample solution (`Sample2.servoy`) and a ~900 KB log are attached.

No solution is proposed in the ticket — it is a pure "lots of errors, please fix" report,
so there is little premise to challenge beyond "which errors actually matter and where do
they come from."

The attached log actually contains **three distinct error families**; they must be
separated because they do not share a root cause:

| Count | Error | Origin |
|------:|-------|--------|
| 650 | `NG0100 ExpressionChangedAfterItHasBeenCheckedError` — `'animate'` flips `false → true` (348×) / `true → false` (2×), plus some `NgbNavOutlet` `-1 → 0` | `SliderComponent_HostBindings` inside the **third-party `@angular-slider/ngx-slider`** (and, separately, `ng-bootstrap`'s `NgbNavOutlet` — a tabpanel, unrelated to the slider) |
| 15 | `TypeError: patchedFormat.indexOf is not a function` | `FormattingService.formatNumbers` ← `ServoyExtraSlider.formatValue` ← ngx-slider `Options.translate` |
| few | `ClassNotFoundException: com.openai.models.ReasoningEffort$Known` in `XMLScriptObjectAdapterLoader` | Servoy Developer AI/OpenAI plugin startup — **completely unrelated** to the slider; noise in the same log |

## Root-cause assessment

**The slider source WAS found on disk**, in the sibling repo (NOT in this `servoy-eclipse`
checkout):

- Web component: `C:\Users\vosti\git_master\servoy-extra-components\components\projects\servoyextracomponents\src\slider\slider.ts` (+ `.html`, `.spec.ts`)
- Spec/packaging: `C:\Users\vosti\git_master\servoy-extra-components\components\slider\slider.spec`
- The shared `FormattingService` it calls lives in THIS repo at
  `com.servoy.eclipse.ngclient.ui\node\projects\servoy-public\src\lib\format\formatting.service.ts`
  (shipped as `@servoy/public`, which `servoy-extra-components` consumes as version `2026.9.4`).

So the bug spans **two repos**: the slider component (`servoy-extra-components`) is the
primary source of both slider-specific error families; the `@servoy/public` FormattingService
in this repo is the secondary site of the `TypeError`.

### Error family 1 — `patchedFormat.indexOf is not a function` (the real, fixable slider bug)

`slider.ts` `formatValue()` (lines 237-249):

```ts
const numberFormat = this.numberFormat();
if (numberFormat && numberFormat.type === 'NUMBER') {
    const format = new Format();
    format.display = numberFormat.display;   // <-- numberFormat.display can be non-string
    format.type = 'NUMBER';
    return this.formatService.format(value, format, false);
}
```

`FormattingService.format` → `formatNumbers(data, formatString)` where `formatString =
format.display`, and the first thing `formatNumbers` does is `patchedFormat.indexOf(';')`
(formatting.service.ts:417). If `format.display` is **not a string** (undefined, or an
object — e.g. the `format` spec property delivered by the server for this slider instance
is a format whose `display` is null/undefined, or the whole `numberFormat` object shape is
not `{type, display:string}`), `.indexOf` does not exist and it throws.

The default components never hit this because they bind the server-provided `Format` object
straight through `[svyFormat]` / the `FormatDirective`, and `FormattingService.format`
itself guards `if ((!format) || (!format.type) ...)` — but it does **not** guard against
`format.type === 'NUMBER'` with a non-string `display`. The slider hand-rolls a `Format`
with `type='NUMBER'` and copies `numberFormat.display` without checking it is a string, so
it slips past every existing guard and reaches `formatNumbers`, which assumes a string.

This is a genuine, deterministic component bug (why the reporter sees it "with different dp
types"): whenever the slider's `numberFormat` is set but its `display` isn't a usable string,
the `translate`/aria path crashes on every pointer. Fixable with a type guard in `slider.ts`
(`typeof numberFormat.display === 'string'`) and/or a defensive guard in the shared
`FormattingService.formatNumbers` (bail out / coerce when `servoyFormat` is not a string).

### Error family 2 — `NG0100` on `animate` (650 occurrences; the loudest noise)

Stack: `SliderComponent_HostBindings` (ngx-slider's own `@HostBinding` for its
`ngx-slider-animate` class) → `bindingUpdated`/`checkStylingProperty`. ngx-slider toggles
its internal `animate` flag while Angular's **single-pass zoneless OnPush** change detection
is verifying expressions, so Angular reports the value changed after it was checked.

The trigger on the Servoy side: `setNewOptions()` (slider.ts:231-235) does
`Object.assign({}, this.options())` and `this.options.set(newOptions)` on **every**
`svyOnChanges` for a long list of properties, and `ngOnInit` sets two options before the
view exists. Each call produces a **new `Options` object reference** bound via
`[options]="options()"`, forcing ngx-slider to re-apply/re-animate. Combined with the first
error throwing out of `ngAfterViewInit`, the component is left in an inconsistent state that
keeps re-triggering. The `NgbNavOutlet -1 → 0` NG0100 entries are from a bootstrap tabpanel
in the sample, not the slider.

Git history shows this area was recently churned: `ed387c9` (SVY-21889, 2026-08-20)
converted `options` from a plain field to a `signal` and changed the template to
`[options]="options()"`; `f108647`/`8e2e88c` migrated the component to the `@servoy/public`
signal-based base class and standalone. The ExpressionChanged/animate instability is
consistent with that signals migration against a third-party library (ngx-slider 22) that
does its own host-binding mutations and was not written for zoneless OnPush.

### Error family 3 — OpenAI `ClassNotFoundException`

`XMLScriptObjectAdapterLoader.loadDocumentationForPlugins` →
`ClassNotFoundException: com.openai.models.ReasoningEffort$Known`. This is a Servoy
Developer plugin-documentation/AI startup problem and has nothing to do with the slider or
the form. It should be ignored for this ticket (or split off separately if it is a real
packaging regression).

## Ticket premise check

The ticket's only premise is "the slider causes errors" — which holds. What it does NOT say,
and what matters for implementation, is that there are **two independent slider defects**
(a hard `TypeError` in number formatting, and a flood of cosmetic `NG0100` animate warnings),
plus unrelated log noise. A fix that only silences the NG0100 flood would leave the real
`TypeError` (broken label/aria formatting) in place, and vice-versa. Both slider families
live in `servoy-extra-components`, with the `TypeError` also touchable in this repo's
`@servoy/public` FormattingService.

## Approaches considered

1. **Fix the `formatValue` number-format path in `slider.ts` (+ optionally harden
   `FormattingService.formatNumbers`)** — pros: directly removes the real `TypeError`;
   small, well-scoped, testable (slider.spec.ts already has `formatValue`/`numberFormat`
   tests to extend). Cons: primary edit is in the `servoy-extra-components` repo, which this
   checkout can build/test only via its own `node/` tooling; the `@servoy/public` guard is in
   this repo but requires rebuilding the lib.
2. **Stabilize the `[options]` / `animate` NG0100 flood** — e.g. avoid replacing the whole
   `Options` object on every change, batch option updates, or guard against re-setting
   identical options, so ngx-slider stops re-animating during CD. Pros: removes 650 console
   errors, the most visible symptom. Cons: NG0100 here is largely emitted by the third-party
   ngx-slider host binding under zoneless OnPush; fully eliminating it may need care (and the
   first TypeError must be fixed first, since throwing out of `ngAfterViewInit` aggravates it).
   Lives entirely in `servoy-extra-components`.
3. **Fix only in `servoy-extra-components`, treat this repo as not involved** — valid for the
   component logic, but the `FormattingService.formatNumbers` non-string guard is a legitimate
   robustness fix for THIS repo's `@servoy/public` and would protect any other component that
   feeds it a bad format.
4. **No code change** — pros: none meaningful. Cons: the `TypeError` is a real, reproducible
   defect that breaks slider label/aria formatting and spams the console; the ticket is a
   valid `Minor` bug with fixVersion 2026.12 already set. Rejected.

## Recommendation

**PROCEED.** Recommended approach: **#1 combined with #2**, both implemented primarily in the
`servoy-extra-components` repo, with an optional defensive guard in this repo's
`@servoy/public` `FormattingService`.

Concretely:
- In `servoy-extra-components/.../slider/slider.ts` `formatValue()`, only build a `Format` and
  call `formatService.format` when `numberFormat.display` is a usable string (guard
  `typeof ... === 'string'`); otherwise fall back to `value.toString()`. Extend the existing
  `formatValue`/`numberFormat` unit tests to cover a non-string / missing `display`.
- Address the `[options]`/`animate` NG0100 flood in `slider.ts` (avoid needlessly replacing
  the whole `Options` object / re-applying identical options every `svyOnChanges`), verifying
  the error count drops with the sample.
- Optionally, in THIS repo, harden
  `com.servoy.eclipse.ngclient.ui/node/projects/servoy-public/src/lib/format/formatting.service.ts`
  `formatNumbers` to bail out or coerce when `servoyFormat` is not a string, so no component
  can trigger `patchedFormat.indexOf is not a function` again.

Important build-scope caveat for the implementer: the primary slider fix is **outside** this
`servoy-eclipse` checkout. This repo can build/test the `@servoy/public` part, but the
component change (and its Vitest tests) must be built and run in the `servoy-extra-components`
repo. The OpenAI `ClassNotFoundException` in the log is unrelated and out of scope for
SVY-21678.

Alternatives if the team prefers a narrower cut: do only #1 (kill the hard `TypeError`) now
and file the NG0100 animate noise as a follow-up, since the two are independent.

## Git history findings

- `servoy-extra-components` slider history (sibling repo, branch `master`, HEAD
  `14d7199` 2026-11-12): recent commits are a signals/standalone/base-class migration, not a
  functional slider change —
  - `ed387c9` SVY-21889 "convert template-bound properties to signals" (2026-08-20): changed
    `[options]="options"` → `[options]="options()"` and made `options` a `signal`. This is the
    change most consistent with the `animate` NG0100 instability surfacing.
  - `f108647` SVY-19023 migrate to `@servoy/public` 2026.9.2 signal-based base class;
    `8e2e88c` SVY-21889 convert to standalone; `4fcef36` upgraded `@servoy/public` to 2026.9.3
    and removed zone.js from test polyfills (component is now exercised zoneless).
  - `formatValue()` itself (lines 237-249) was last touched by `14d7199`; the unchecked
    `format.display = numberFormat.display` assignment predates these and is the latent bug.
- `@servoy/public` `FormattingService.formatNumbers` (this repo) assumes a string `display`
  (`patchedFormat.indexOf(';')` at line 417) with no non-string guard.
- No prior `docs/SVY-21678-*.spec.md` exists in either repo; this is a fresh case. Component
  is `NGClient`, priority Minor, fixVersion 2026.12, currently In Progress.

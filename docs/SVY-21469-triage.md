# Triage Report — SVY-21469

**Verdict:** PROCEED (with a corrected root cause — the previous theory is disproven)

## Reported problem

> "data-target is no longer pushed to the function when set using the attributes field."
> "After upgrading to 2026.09, the data-target value isn't pushed anymore to the onAction when it is set through the attributes on a component."

Priority: Blocker, reported as a 2026.09 regression.

**Critical new evidence from the user.** Two browser screenshots of a plain `bootstrapcomponents-label` with `data-target: pec` set via the designer `attributes` property show the rendered DOM element is **correct**:

```html
<span class="bts-label default-align" data-target="pec" id="s9024ca6dae74024fe6ae63efe05d4a40" tabindex="-1">
```

So on a simple label the attribute **does** reach the DOM and the value **does** reach `onAction`. The user's own words: **"on simple label is fine, so why on them is different?"** The failing case in their svyCloud app is a label used as a clickable **tile** inside a contained form / form component (property panel: `app_pipelines.containedForm.border`, handler `onActionTile(event, dataTarget) (OVERRIDE)`, styleClass `svycloud-ui-tile-frame clickable`, `data-target` set via attributes).

The bug is therefore **not** "attribute missing from the DOM" in the general case. It is a **works-here / fails-there split** between a stand-alone label and a label rendered as a tile inside a form component.

## Root-cause assessment

### The prior "attribute never reaches DOM" theory is DISPROVEN

The previous report claimed the SVY-19023 signal migration broke `addAttributes()` in `svyOnInit`, so `servoyAttributes` was "never written to the DOM". The user's browser DOM screenshot directly contradicts this: `data-target="pec"` is present on the `<span class="bts-label">` host of a simple label. `addAttributes()` (`basecomponent.ts:208-213`) runs correctly and writes the initial map. The `firstChange` guard in `svyOnChanges` (`basecomponent.ts:109`) is irrelevant because the initial write is done by `addAttributes()`, not by `svyOnChanges`. **This layer works.** The theory must be abandoned.

### The real differentiator: DOM traversal in `getDataTarget`, not the DOM write

The value is read at click time by `getDataTarget(event)` and passed as the second handler argument:

- `bootstrapcomponents/projects/bootstrapcomponents/src/bts_baselabel.ts:62-68`
  ```ts
  protected getDataTarget(event: any): any {
      const dataTarget = event.target.closest('[data-target]');
      if (dataTarget) return dataTarget.getAttribute('data-target');
      return null;
  }
  ```
- Passed at `bts_baselabel.ts:22` (keydown/Enter), `:36` (single-click when double-click also bound), `:43` (plain click), `:49` (contextmenu).

The click listener is attached to `getFocusElement()`, which is `getNativeElement()` → the `<span #element class="bts-label">` host (`bts_basecomp.ts:58-60`, `basecomponent.ts:133-135`). `data-target` is written by `addAttributes()` onto that **same** host span.

**Why the simple label works:** the click lands somewhere inside `<span class="bts-label" data-target="pec">`; `event.target.closest('[data-target]')` walks up from the clicked node and immediately finds the host span. Confirmed by the screenshot — the attribute is on the host and the listener is on the host, so `closest` always resolves. (Confirmed.)

**Why the tile can fail — three concrete, code-backed mechanisms, in order of likelihood:**

1. **The clicked element is not a DOM descendant of the `[data-target]` host (most likely).**
   `event.target.closest(...)` only walks **ancestors** of the physically clicked node. For it to find `data-target`, the clicked node must be a descendant of the host span that carries the attribute. In the tile the label renders HTML content (`showAs: html`, `[innerHTML]` in `label.html:13-18` / `datalabel.html:11-17`) and is wrapped by form-component / `containedForm` framing (`svycloud-ui-tile-frame`). If the actual click target at runtime is an element that is **not** inside the attribute-bearing span — e.g. an overlay/wrapper element that receives the click, or the `data-target` ends up on a different element than the one the click bubbles from — `closest('[data-target]')` returns null and the handler receives `null`. This is exactly the "simple label fine, tile different" split: same code, different DOM nesting/click origin. (Suspected — needs one runtime `console.log(event.target, event.currentTarget, event.target.closest('[data-target]'))` in the tile to confirm; per AGENTS "log first".)

2. **A different `getDataTarget` implementation is in play for the rendering component.**
   Not every label subclass reads the attribute off the host. Enumerated implementations:
   - `bts_baselabel.ts:62` — `event.target.closest('[data-target]')` (label, datalabel, imagemedia). **Returns the attribute.**
   - `checkbox.ts:93-99` — `event.target.closest('label').querySelector('[data-target]')` — walks to the nearest `<label>` then **down** into it. **Different traversal.** If the tile is a checkbox-style component (or one that inherits this override) the value comes from a nested `[data-target]`, not the host, and can be null when the attribute is on the host.
   - `bts_basefield.ts:162-164` — `getDataTarget(_event) { return null; }` — **ignores the event and always returns null.** Any field-derived component that surfaces `onAction` gets `null` for `dataTarget` by design.
   The property panel said "Label (Bootstrap Components)", but confirm the *actual* rendered selector of the failing tile (`bootstrapcomponents-label` vs `-datalabel` vs a field/checkbox variant). If it resolves to `bts_basefield`'s stub or checkbox's `querySelector` path, `null` is expected regardless of the DOM write. (Suspected — depends on the exact failing component; must be confirmed against the running form's element type.)

3. **`showAs: html` inner-HTML breaks the ancestor chain.**
   A special case of (1): with `[innerHTML]` the label text is rendered inside `<span class="bts-label-text">`, itself inside the host span that carries `data-target`. A click on inner HTML there **still** resolves via `closest` (the text span is a descendant of the host). So plain inner HTML alone does **not** break it — this matches the simple-label screenshot working. It only breaks when the tile framing places the click-receiving element **outside** the attribute-bearing host (mechanism 1). (Confirmed that inner HTML by itself is not the cause; the tile wrapping is.)

### What is NOT the cause (ruled out with evidence)

- **Server-side arg truncation is ruled out.** The handler spec (`label.spec` `handlers` block) declares two parameters — `event` and `dataTarget` (`{ "name": "dataTarget", "type": "string" }`). Client-side `getEventArgs` (`converter.service.ts:173-219`) iterates **all** `args.length` entries; the second arg (the `data-target` string) is not an `Event`, so it goes through `convertFromClientToServer(arg, handlerSpecification?.getArgumentType(i), ...)` (`:214`). For index 1 the spec's `getArgumentType(1)` is the `string` type (`types_registry.ts:405-407`); a plain string round-trips unchanged. It is pushed to `newargs` (`:216`) and sent. Server `BaseWebObject.executeEvent` → `doExecuteEvent` → `handler.executeEvent(args)` (`BaseWebObject.java:335,383-392`) passes the full `args` array through; there is **no** truncation to the declared parameter count. The same path is used for form-component/list cells via `component_converter.ts:485` (identical `getEventArgs` call). So if the client passes a non-null `data-target`, the server delivers it. The failure is that the **client passes `null`** — i.e. `getDataTarget` returned null — which points back to the DOM-traversal split above. (Confirmed by reading the full arg chain.)

- **`getEventArgs` did not change to drop the second arg.** Its only history is `SVY-19023` formatting/eslint/strictNullChecks commits (`d1794cf2fd`, `c2c2f0a4a0`, `a6e8005ce1`); the loop over all args and the per-index `getArgumentType` conversion are long-standing and unchanged in behaviour. (Confirmed via `git log -L 173,219`.)

### Convergent conclusion

The regression is **not** the initial attribute write (that works — proven by the browser DOM). The value reaches `onAction` only when `event.target.closest('[data-target]')` can walk from the clicked node up to the host span that carries `data-target`. On a stand-alone label that always holds. On the svyCloud **tile** (label inside a form-component/`containedForm` with `svycloud-ui-tile-frame clickable` framing and `showAs: html` content) the click-receiving element is not an ancestor-chain descendant of the attribute-bearing host, OR the tile is rendered by a component whose `getDataTarget` reads from a different element (checkbox `querySelector('label')`) or returns null (`bts_basefield`). In both variants `getDataTarget` returns `null`, so `onAction`'s `dataTarget` is empty.

**Confidence:** High that the mechanism is `getDataTarget`/`closest` DOM traversal and that server + `getEventArgs` + initial DOM write are all correct. Medium on *which* of the three sub-mechanisms applies to this specific tile — that requires one runtime log in the failing form (see Recommendation). This is genuinely investigable; it is not blocked on a missing fact, but the fix must be validated against the exact failing element.

## Ticket premise check

Partially holds, with a correction. The user-facing symptom ("data-target no longer pushed to onAction") is real. But the premise implied in the previous triage ("attribute never reaches the DOM after the signal migration") is **false** — the attribute is on the DOM for a simple label. The correct premise is: `getDataTarget` returns `null` for the tile because of where the click originates / which element carries the attribute / which `getDataTarget` override runs, not because the attribute is absent. Whether this is strictly a 2026.09 regression in `getDataTarget` itself is **not** established — the `getDataTarget`/`closest` logic is unchanged since `ae70cc8` (SVY-20819) and `8531754` (SVY-21251, which only added the keydown/Enter listener). If the tile worked before 2026.09, the behavioural change is more likely in the surrounding DOM structure (form-component/contained-form rendering or event target) than in `getDataTarget`. This must be pinned down before claiming a specific regressing commit.

## Approaches considered

1. **Confirm the failing element and its DOM, then make `getDataTarget` robust (recommended).**
   Reproduce the tile, log `event.target`, `event.currentTarget`, and `event.target.closest('[data-target]')`. Then, depending on findings, make `getDataTarget` resilient: fall back to `getFocusElement()`/`event.currentTarget`'s own `[data-target]` when `closest` from `event.target` misses (e.g. `event.currentTarget.closest('[data-target]') ?? event.target.closest('[data-target]')`, or read the attribute directly off `getNativeElement()`). Pros: fixes the actual reading bug at the right layer; low blast radius (bootstrapcomponents only); does not touch the (working) server or base-class attribute write. Cons: needs the runtime evidence first to avoid guessing which of the three sub-mechanisms applies.

2. **Align/override `getDataTarget` per component.**
   If the tile resolves to a checkbox/field variant, fix that component's `getDataTarget` (or the `bts_basefield` stub) to also read the host `[data-target]`. Pros: targeted. Cons: only correct if mechanism 2 is the cause; verify first.

3. **Change the base-class attribute write (the previous recommendation).**
   Rejected. The DOM already has the attribute (screenshot). Removing the `firstChange` guard or re-running `addAttributes()` would not change `getDataTarget`'s return value on the tile and would not fix the reported symptom.

4. **No code change.**
   Rejected. Blocker-priority regression with a reproducible symptom in the customer app.

## Recommendation

PROCEED with **Approach 1**. Before writing any fix:

1. **Log first (per AGENTS).** In `bts_baselabel.ts:getDataTarget`, temporarily log `event.type`, `event.target.outerHTML?.slice(0,120)`, `event.currentTarget?.outerHTML?.slice(0,120)`, `!!event.target.closest('[data-target]')`, and `this.getNativeElement().getAttribute('data-target')`. Reproduce the svyCloud tile click. This single log resolves which of the three sub-mechanisms is in play.
2. **Confirm the rendered component/selector** of the failing tile (`bootstrapcomponents-label` vs `-datalabel` vs field/checkbox) so the fix targets the right `getDataTarget`.
3. **Fix the reader, not the writer.** Most robust general fix: read `data-target` from the listener's own element as a fallback — the attribute is guaranteed on `getNativeElement()` (the host `#element`), which is `event.currentTarget` for these listeners. e.g.:
   ```ts
   protected getDataTarget(event: any): any {
       const el = event.target?.closest?.('[data-target]')
           ?? (event.currentTarget as HTMLElement)?.closest?.('[data-target]')
           ?? this.getNativeElement();
       return el?.getAttribute?.('data-target') ?? null;
   }
   ```
   (Confirm the final form against the runtime log before committing; keep checkbox's nested-`querySelector` semantics if that component genuinely needs the inner element.)
4. **Add a regression test** (Vitest, bootstrapcomponents) that renders a label with `servoyAttributes = { 'data-target': 'pec' }`, dispatches a click on an **inner** child element (simulating the tile's inner HTML), and asserts `getDataTarget` returns `'pec'` and `onAction` receives it as the second argument.
5. Rebuild the component package and verify the value now reaches `onActionTile(event, dataTarget)` on the tile.

## Git history findings

- **`getDataTarget` / `closest` reader is unchanged by the migration** (bootstrapcomponents `projects/bootstrapcomponents/src/bts_baselabel.ts`):
  - `ae70cc8` (SVY-20819 "use signals instead of @input") converted `this.onActionMethodID` → `this.onActionMethodID()` calls but left `getDataTarget`/`closest('[data-target]')` intact.
  - `8531754` (SVY-21251 "datalabel onAction does not trigger if entered") only **added** the keydown/Enter listener (`bts_baselabel.ts:19-25`); it did not touch `getDataTarget`.
  - `0185c86` (SVY-21298 strict-mode) only added `!`/`as any` non-null assertions.
  - `257205b` (SVY-19023 "migrate to @servoy/public 2026.9.2 signal-based base class") is where the package picked up the new base class — but the base class change affects the (working) attribute **write**, not the **read**.
  - Conclusion: the `getDataTarget` reader itself did not regress. (Confirmed via `git log -L 16,53:bts_baselabel.ts`.)
- **`getEventArgs` unchanged in behaviour** (`converter.service.ts:173-219`): only SVY-19023 formatting/eslint/strictNullChecks commits (`d1794cf2fd`, `c2c2f0a4a0`, `a6e8005ce1`). The all-args loop and per-index `getArgumentType` conversion are long-standing. No arg-dropping introduced. (Confirmed via `git log -L 173,219`.)
- **`svyOnChanges` `firstChange` guard is old** (`basecomponent.ts:108-121`): present since `209cf48d38` (SVY-18890 Angular 17 upgrade), unchanged by SVY-19023. It never governed the initial write (that is `addAttributes` in `svyOnInit`), so it is not implicated. (Confirmed via `git log -L 108,121`.)
- **Server side unchanged and non-truncating:** `BaseWebObject.executeEvent`/`doExecuteEvent` pass the full `args` array to `handler.executeEvent(args)` (`BaseWebObject.java:335,383-392`); `DataAdapterList.executeEvent` (`DataAdapterList.java:207-219`) only converts the return value. No mapping to the declared parameter count. (Confirmed.)

**Conclusion:** The 2026.09 symptom is real, but the previous "initial `servoyAttributes` never reaches the DOM" root cause is **disproven** by the customer's browser DOM (a simple label renders `data-target="pec"` correctly). The real differentiator is client-side **read** time: `getDataTarget`'s `event.target.closest('[data-target]')` succeeds on a stand-alone label (attribute on the host, click bubbles from a descendant) but returns `null` on the svyCloud **tile** because the clicked element is outside the attribute-bearing host's ancestor chain and/or the tile is rendered by a component with a different or stubbed `getDataTarget`. The server, `getEventArgs`, and the base-class attribute write are all correct and unchanged. Fix the reader (`getDataTarget`) after one runtime log confirms which sub-mechanism applies.

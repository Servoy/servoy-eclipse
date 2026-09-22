# Spec: SVY-21380 — Missing component spec must render a visible inline error (not blank the form editor)

> Note: the Jira ticket was read via the approved Triage report (`docs/SVY-21380-triage.md`);
> the live Jira API was not re-queried for this spec. All ticket facts below are carried
> from that triage.

## 1. Goal

When a form contains a component whose `typeName` points to a spec that is **not installed**,
the missing component must be replaced by a **visible inline error** that names the missing
spec — and, critically, the surrounding valid components must keep rendering. Today the NG
client only leaves an empty gap while the **form editor goes entirely blank** (all valid
components are lost), a regression introduced by SVY-19023. This fix hardens both
`getTemplate()` implementations so a missing template ref can never throw, adds an inline
fallback template, and re-includes `servoycore-errorbean` in template generation so the
designated fallback bean renders its "Specification not found." message.

## 2. Background

### 2.1 Reported behaviour (per-surface blast radius)

| Surface | Current result | Expected result |
|---------|----------------|-----------------|
| NG client | valid components render; broken one leaves an empty gap | inline error where the broken component is |
| Form editor | **entirely blank canvas** — valid components lost too | valid components render + inline error where the broken one is |
| Screenshot / MCP tool | image with the same empty gap; console error surfaced in text | image shows the inline error (console capture still reports it) |

All three surfaces log the same line:

```
ERROR FormComponent - Template for servoycoreErrorbean was not found,
please check form_component template.          sablo.service.ts:99
```

Reproduction attachment: `test3_errors.servoy`.

### 2.2 How the substitution and templates work

The failure is a two-layer story; the substitution logic is correct, but the substitute's
Angular template no longer exists and one of the two template consumers is unguarded.

1. **Fallback selection (server side, correct).** When a spec is missing,
   `FormElement.getWebComponentSpec()` (servoy_ngclient) falls back to
   `FormElement.ERROR_BEAN = "servoycore-errorbean"`. Both the editor and the NG client use
   the same `FormElement`, which is why the console message is identical on both surfaces.
   The editor ghost path does the same substitution in
   `com.servoy.eclipse.designer/.../rfb/actions/handlers/GhostHandler.java:207`.

2. **The errorbean template is no longer generated.** The per-component
   `<ng-template #servoycoreErrorbean …>` and its `viewChild('servoycoreErrorbean')` are
   emitted at build/refresh time by
   `com.servoy.eclipse.ngclient.ui/.../ComponentTemplateGenerator.java`, into the
   `<!-- component template generate start/end -->` blocks of
   `designform_component.component.ts` and `form_component.component.ts` (written by
   `WebPackagesListener.java:491-497`). That generator skips deprecated specs
   (`ComponentTemplateGenerator.java:92` → `if (spec.isDeprecated()) continue;`), and
   `servoycore-errorbean` is now marked `"deprecated": "true"` in
   `servoy-client/servoy_ngclient/war/servoycore/errorbean/errorbean.spec`. So there is no
   `servoycoreErrorbean` template ref in the generated forms, and
   `this['servoycoreErrorbean']` is `undefined`.

3. **The editor path is unguarded; the NG-client path is not.**
   - Editor `designform_component.component.ts:571` ends `getTemplate()` with
     `return (this as any)[item.type]();` → `undefined()` → **TypeError thrown inside the
     `@for` template evaluation** → whole form render aborts → blank canvas.
   - Client `form_component.component.ts:290` is defensive:
     `return typeof componentRef === 'function' ? componentRef() : componentRef;` → returns
     `undefined`, renders the rest of the form, only leaves a gap.

The `ErrorBean` Angular class itself still exists and is imported
(`com.servoy.eclipse.ngclient.ui/node/src/servoycore/servoycore.components.ts:9`,
`.../servoycore/error-bean/error-bean.ts`) — only the generated per-instance `<ng-template>`
disappeared. The errorbean model ships an `error` string (default `"Specification not
found."`) and a `toolTipText` string.

## 3. Design

### 3.1 Harden both `getTemplate()` methods

Neither `getTemplate()` may ever evaluate `undefined()`. The client is already safe; the
editor is not. Make the editor’s component branch mirror the client’s defensive resolution,
and route a missing ref to the inline fallback template (3.2) rather than returning
`undefined`.

- **Editor** `com.servoy.eclipse.ngclient.ui/node/src/designer/designform_component.component.ts`,
  `getTemplate()` (~560-573): resolve the ref to a local variable, log the existing
  "Template … was not found" error when it is missing, and when the resolved ref is not a
  callable template, return the inline fallback template instead of calling `(this as any)[item.type]()`.
- **Client** `com.servoy.eclipse.ngclient.ui/node/src/ngclient/form/form_component.component.ts`,
  `getTemplate()` (~280-291): keep the existing defensive check but, instead of returning a
  bare `undefined` for a missing ref, return the same inline fallback template so the gap is
  replaced by a visible inline error. Preserve the existing `injectedComponentRefs` test hook.

Both methods must be safe for `item.type === 'menu'` (which intentionally returns
`undefined!` in the editor today) and for the structure/form-component branches, which are
unaffected.

### 3.2 Re-include `servoycore-errorbean` in generation (the primary fix)

Exempt `servoycore-errorbean` from the deprecated-skip at
`ComponentTemplateGenerator.java:92` so the designated fallback bean’s template is always
generated even though the spec is flagged deprecated for user placement. Once the
`servoycoreErrorbean` template is generated again, the server-side substitution (which already
replaces a missing spec with `FormElement.ERROR_BEAN = "servoycore-errorbean"`) renders the
errorbean's model `error` ("Specification not found.") with a tooltip naming the missing spec.
This is the visible inline error on every surface — the errorbean IS the error element.

**Mandatory:** the exemption MUST carry a code comment explaining *why* errorbean is special
(it is the designated fallback bean referenced by `FormElement.ERROR_BEAN`, so it must always
be generated), so a future cleanup does not re-drop it as "just another deprecated spec".

### 3.3 Harden `getTemplate()` — never render a redundant inline fallback

An earlier draft added an inline `#svyMissingSpec` `<ng-template>` to both components as a
belt-and-braces fallback. That was **dropped**: it was redundant with the always-generated
errorbean substitute (§3.2), and returning it via a lazily-resolving `viewChild` signal could
be `undefined` on the first render pass, so it did not reliably render anyway. The errorbean
substitute is the single source of the visible error.

The editor `getTemplate()` hardening (§3.1) remains the guard that stops the blank-canvas
regression: it must never evaluate `undefined()`. Both methods therefore resolve the ref to a
local variable and return `typeof componentRef === 'function' ? componentRef() : componentRef`
(a harmless `undefined` for a genuinely-missing ref, which Angular's `[ngTemplateOutlet]`
tolerates by rendering nothing). In practice the ref is never missing for the missing-spec
case once §3.2 regenerates the errorbean template — the guard only protects against an
unexpected/uninstalled template ref.

### 3.4 Scope boundary — cross-repo file (errorbean.spec)

`servoycore-errorbean`’s `"deprecated": "true"` flag lives in
`servoy-client/servoy_ngclient/war/servoycore/errorbean/errorbean.spec`, in a **separate repo
checkout** (`../servoy-client`), not in this repo. This fix deliberately does **not** un-deprecate
the spec (that would re-expose it for user placement, defeating SVY-19023). Instead, 1a keeps
the deprecated flag intact and exempts errorbean at the generator level. No change to the
`servoy-client` repo is required by this spec. See Open Questions.

### 3.5 Git history

- **`b9a2efea186`** (servoy-eclipse, Johan Compagner, 2026-08-13) — *"SVY-19023 skip
  deprecated components in template gen …"* added `ComponentTemplateGenerator.java:92`
  `if (spec.isDeprecated()) continue;` to prevent NG8001/NG8103 errors for deprecated
  components. Side effect: errorbean fallback template no longer generated. (Confirmed via
  `git blame` on line 92.)
- **`8502a32506`** (servoy-client, Johan Compagner, 2026-08-13) — *"SVY-19023 mark unused
  servoycore components deprecated …"* set `"deprecated": "true"` on `errorbean.spec`.
  (Confirmed: the spec at `../servoy-client/.../errorbean.spec` currently reads
  `"deprecated": "true"`.)
- **`d1794cf2fdb`** (servoy-eclipse, 2026-08-12) — prettier reformat of `src/`; the editor
  `getTemplate()` unguarded `(this as any)[item.type]()` was only reformatted here, not
  introduced. (Confirmed via `git blame` on line 571.)
- **`f975d74c102`** (Andrei Costescu, 2024-11-05) — introduced the guarded client variant
  (`typeof componentRef === 'function' ? …`); i.e. the client was hardened long ago and the
  editor never was.
- No prior `docs/SVY-21380*` spec exists; SVY-21195 (Closed) is the direct predecessor.

## 4. Implementation plan

1. **`ComponentTemplateGenerator.java`** (`com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/`):
   at line ~92, change `if (spec.isDeprecated()) continue;` to skip deprecated specs **except**
   `servoycore-errorbean`. Add a comment explaining errorbean is the designated
   `FormElement.ERROR_BEAN` fallback and must always be generated.
2. **`designform_component.component.ts`** (editor,
   `com.servoy.eclipse.ngclient.ui/node/src/designer/`): harden `getTemplate()` (~560-573) so it
   resolves the ref to a local variable, logs the existing "Template … was not found" error, and
   returns `typeof componentRef === 'function' ? componentRef() : componentRef` instead of calling
   `(this as any)[item.type]()` (which becomes `undefined()` and throws). Preserve the intentional
   `menu` branch (`return undefined!`) and the structure/form-component branches. Do NOT add an
   inline fallback template.
3. **`form_component.component.ts`** (NG client,
   `com.servoy.eclipse.ngclient.ui/node/src/ngclient/form/`): `getTemplate()` (~280-291) already
   returns `typeof componentRef === 'function' ? componentRef() : componentRef` and preserves the
   `injectedComponentRefs` test hook — leave this defensive behaviour intact (no change needed
   beyond keeping it). Do NOT add an inline fallback template.
4. **Regenerate templates** so `servoycoreErrorbean` reappears in the generated blocks: build
   / trigger `WebPackagesListener` (or `npm run build_libs` + IDE refresh) and verify the
   `<!-- component template generate start/end -->` blocks in both `.component.ts` files now
   contain `servoycoreErrorbean`.
4b. **`error-bean.ts`** (`com.servoy.eclipse.ngclient.ui/node/src/servoycore/error-bean/`): the
   regenerated errorbean template binds all four model properties from `errorbean.spec`
   (`error`, `toolTipText`, `location`, `size`), but the `ErrorBean` component only declared
   `error` and `toolTipText`, causing NG8002 ("Can't bind to 'location'/'size'") at build time.
   Add the missing `location` and `size` signal inputs (`input<any>(undefined)`, matching the
   pattern used by `splitpane`/`basetabpanel` for the same point/dimension model types).
5. **Tests** (see §5 anchors): Vitest unit tests on both `getTemplate()` methods; a Java test
   that `ComponentTemplateGenerator` still emits `servoycoreErrorbean`.
6. **Verify on all three surfaces** with `test3_errors.servoy`: editor (valid components render
   + inline error), NG client (gap replaced by inline error), screenshot/MCP (image shows the
   inline error, console capture still reports the missing-spec line).

## 5. Acceptance criteria

- [ ] Opening a form containing a component with a missing spec in the **form editor** renders
      all valid components **and** the errorbean substitute where the broken component is (no blank canvas).
- [ ] The **NG client** replaces the previous empty gap with the errorbean substitute for the same form.
- [ ] The errorbean substitute names the missing spec (tooltip) and shows its "Specification not
      found." message.
- [ ] `getTemplate()` in **both** `designform_component.component.ts` and
      `form_component.component.ts` never evaluates `undefined()` / never throws for a missing ref.
- [ ] `ComponentTemplateGenerator` again emits a `servoycoreErrorbean` template/viewChild into
      the generated blocks of both components, and the exemption carries an explanatory comment.
- [ ] The screenshot / MCP tool captures an image showing the errorbean substitute; the SVY-21195
      console capture still reports the missing-spec line. (Per SVY-21195 this does **not** gate
      `screenshotForm`.)
- [ ] A Vitest unit test asserts the editor `getTemplate()` never throws for a missing ref; a Java
      test asserts `servoycoreErrorbean` is still generated (exempted from the deprecated-skip).
- [ ] `npm run lint` passes with zero warnings and the Angular build succeeds; no new
      compilation errors in the Java module.

## 6. Out of scope

- Un-deprecating `servoycore-errorbean` in `errorbean.spec` (would re-expose it for user
  placement — contradicts SVY-19023). The deprecated flag stays; only generation is exempted.
- Any change to the `servoy-client` repo (§3.4).
- Gating `screenshotForm` on the presence/absence of the error (explicitly excluded by SVY-21195).
- The heavier "error div/iframe in the editor" design (Triage Approach 2).
- A dedicated inline `#svyMissingSpec` fallback template (dropped as redundant with the
  always-generated errorbean substitute — see §3.3).
- Reworking `FormElement.getWebComponentSpec()` / `ERROR_BEAN` substitution logic (it is correct).
- Populating the errorbean `error`/`toolTipText` with the missing component's type name on the
  persist-based `FormElement` path. The errorbean renders the spec default `"Specification not
  found."` on both surfaces; this matches long-standing behaviour in older releases and is
  accepted as-is. Naming the missing type would be a `servoy-client` change (see §7) and is a
  possible future follow-up, not part of this fix.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Confirm no shipped install path relies on `servoycore-errorbean` being absent from generation (i.e. re-including it causes no NG8001/NG8103 regression that SVY-19023 was fixing). | Dev | open |
| Cross-repo boundary: is any coordinated `servoy-client` change desired (e.g. a dedicated non-deprecated fallback spec) instead of the generator-level exemption? Assumed no for this spec. | Dev/Lead | open |
| Should the errorbean name the missing component type (via its `error`/`toolTipText`) instead of the generic "Specification not found."? Persist-based `FormElement` (servoy-client) does not set a descriptive `error` for the plain missing-spec case, so the spec default shows on both surfaces. Decision: keep "Specification not found." (matches older releases); a name-carrying message would be a servoy-client follow-up. | Dev/Lead | resolved (keep default) |
| Ticket was read via the triage report (Jira token may be expired) — confirm no acceptance criteria in the live ticket contradict §5. | PM | open |
```
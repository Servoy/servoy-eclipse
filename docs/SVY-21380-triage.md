# Triage Report — SVY-21380

**Verdict:** PROCEED

## Reported problem
When a form contains a component whose `typeName` points to a spec that is not
installed, the component silently fails to render. The observed symptoms differ by
surface:

| Surface | Result |
|---------|--------|
| NG client | valid components still render; the broken one leaves an empty gap |
| Form editor | **entirely blank canvas** — even the valid components are lost |
| Screenshot / MCP tool | image captured with the same empty gap as NG client, and the browser-console error is surfaced in text alongside it |

All three log the same line:

```
ERROR FormComponent - Template for servoycoreErrorbean was not found,
please check form_component template.          sablo.service.ts:99
```

Expected: a visible inline error where the broken component should be, naming the
missing spec, and in the editor the valid components must still render.

> Note: the ticket's *proposed solution* — "show an error div/iframe inside the
> editor" (a review suggestion carried over from SVY-21195) — is noted, but is only
> one of several ways to fix this and is not assumed correct.

## Root-cause assessment
The failure is a two-layer story. The substitution logic is fine; what breaks is that
the **substitute's Angular template no longer exists** and one of the two consumers of
that template is **unguarded**.

1. **Fallback selection (server side, correct).** When a spec is missing,
   `FormElement.getWebComponentSpec()` (servoy_ngclient, cited in the reporter's
   comment) falls back to `FormElement.ERROR_BEAN` = `"servoycore-errorbean"`. Both the
   editor and the NG client use the same `FormElement`, which is why the console
   message is identical on both surfaces. The editor's ghost path does the same
   substitution in
   `com.servoy.eclipse.designer/.../rfb/actions/handlers/GhostHandler.java:207`.

2. **The errorbean template is no longer generated.** The per-component
   `<ng-template #servoycoreErrorbean …>` and its `viewChild('servoycoreErrorbean')`
   are emitted at build/refresh time by
   `com.servoy.eclipse.ngclient.ui/.../ComponentTemplateGenerator.java`, into the
   `<!-- component template generate start/end -->` blocks of
   `designform_component.component.ts` and `form_component.component.ts` (written by
   `WebPackagesListener.java:491-497`). That generator now **skips deprecated specs**:

   `ComponentTemplateGenerator.java:92` → `if (spec.isDeprecated()) continue;`

   And `servoycore-errorbean` was **marked deprecated** in
   `servoy-client/servoy_ngclient/war/servoycore/errorbean/errorbean.spec`
   (`"deprecated": "true"`). Both changes are the same case, same day:
   - servoy-eclipse `b9a2efea186` — *"SVY-19023 skip deprecated components in template
     gen …"* (Johan Compagner, 2026-08-13) — commit message even says its purpose is to
     *"Prevent NG8001/NG8103 errors for deprecated components"*.
   - servoy-client `8502a32506` — *"SVY-19023 mark unused servoycore components
     deprecated …"* (same author, same day).

   So after SVY-19023 there is **no** `servoycoreErrorbean` template ref in the
   generated forms. `this['servoycoreErrorbean']` is `undefined`.

3. **The editor path is unguarded; the NG-client path is not.** In
   `designform_component.component.ts:560-573` `getTemplate()` ends with:

   ```ts
   return (this as any)[item.type]();   // undefined()  -> TypeError
   ```

   With `item.type === 'servoycoreErrorbean'` and the ref undefined, this is
   `undefined()`, which **throws inside the `@for` template evaluation** and aborts the
   whole form render → blank canvas (even valid components are lost). The NG-client
   twin at `form_component.component.ts:280-291` is defensive:

   ```ts
   return typeof componentRef === 'function' ? componentRef() : componentRef; // undefined, no throw
   ```

   so it renders the rest of the form and only leaves a gap. This exactly matches the
   per-surface blast radius in the ticket.

**Net:** the "blank canvas in the editor" is a **regression introduced by SVY-19023**:
deprecating the errorbean spec removed its generated template, and the editor's
`getTemplate()` was never hardened against a missing ref the way the client's was. The
`ErrorBean` Angular class itself still exists and is still imported
(`servoycore.components.ts:9`) — only the generated per-instance `<ng-template>`
disappeared.

## Ticket premise check
- **Is it a real Servoy bug?** Yes. Reproducible from the described `.frm` edit;
  attachment `test3_errors.servoy` is a ready reproduction.
- **Is the ticket's framing right?** Mostly. It correctly says "the substitution works;
  what fails is rendering the substitute." What the ticket flags as *"not yet checked —
  whether the servoycore package ships an error bean template at all"* is exactly the
  crux, and the answer is now established: **the spec ships but is deprecated, so the
  template generator deliberately skips it as of SVY-19023.** This is a shipped-install
  regression, not a dev-setup artifact.
- **Is the proposed "error div/iframe in the editor" the right fix?** It is heavier than
  necessary. The minimal, correct fix is to (a) restore a rendered substitute for the
  missing-spec case and (b) harden the editor's `getTemplate()` so a missing ref can
  never blank the canvas. See approaches below.

## Approaches considered

1. **Harden the editor `getTemplate()` + guarantee an errorbean template.**
   Make `designform_component.getTemplate()` defensive like the client's (never call
   `undefined()`), and ensure the errorbean substitute still has a template to render.
   Two sub-options for the template:
   - *1a. Exempt `servoycore-errorbean` from the deprecated-skip* in
     `ComponentTemplateGenerator.java:92` (it is the designated fallback bean, so it
     must always be generated even though it is "deprecated" for user placement).
   - *1b. Give both `getTemplate()` methods an inline fallback template* (a small
     `#svyMissingSpec` error div) used whenever the resolved ref is missing, decoupling
     the fix from the errorbean spec entirely.
   Pros: fixes the editor blank-canvas at the true root; small, localized; matches the
   already-defensive client behaviour; makes the inline error visible on all surfaces.
   Cons: 1a re-includes one "deprecated" spec (needs a clear comment on why errorbean is
   special); 1b adds a bit of template to two components.

2. **Inline error div/iframe in the editor (the ticket's suggestion).**
   Render a dedicated error element in the editor when a component cannot be resolved.
   Pros: explicit, obvious error surface; addresses the reviewer's request directly.
   Cons: larger; if it doesn't also harden `getTemplate()`, a stray `undefined()` can
   still throw; risks diverging editor vs client behaviour again.

3. **Stop substituting `ERROR_BEAN` and instead skip/omit the broken component.**
   Pros: no template needed. Cons: hides the problem from the user — the opposite of the
   ticket's "visible inline error"; and still needs the editor guard to be safe.

4. **No code change.**
   Pros: none beyond zero effort. Cons: the editor blank-canvas is a real regression
   from SVY-19023 that loses *all* editing ability for any form referencing a missing
   spec; the console error is invisible to users. Not acceptable.
   Honest evaluation: **rejected** — this is a genuine regression with a concrete cause.

## Recommendation
**PROCEED** with **Approach 1**, favouring **1b (inline fallback template) combined with
hardening both `getTemplate()` methods**, and additionally **1a (exempt errorbean from
the deprecated skip)** so the intended `servoycore-errorbean` substitute renders with
its "Specification not found." message and a tooltip naming the missing spec.

Justification:
- The editor blank-canvas must be fixed at its root: `getTemplate()` in
  `designform_component.component.ts:571` must never evaluate `undefined()`. This alone
  stops the "valid components lost" symptom and brings the editor in line with the
  already-safe client (`form_component.component.ts:290`).
- The user-visible inline error the ticket asks for is best delivered by making the
  errorbean substitute actually render (1a) and/or a generic missing-spec fallback
  template (1b). Prefer wiring the error text to name the missing spec (the errorbean
  model already has an `error` string, default *"Specification not found."*).
- Confirm the fix on all three surfaces: editor (valid components render + inline
  error), NG client (gap replaced by inline error), and `screenshotForm`/MCP (image
  shows the inline error; SVY-21195 console capture still reports it). Per SVY-21195,
  this does **not** need to gate `screenshotForm`.

Alternatives if the above is rejected: Approach 2 is acceptable *only if* it also
hardens `getTemplate()`; Approach 3 contradicts the ticket's intent and should not be
chosen.

Suggested test anchors (for the downstream spec): a Vitest unit test on
`designform_component`/`form_component` `getTemplate()` asserting a missing ref does not
throw and yields the fallback; and a Java test that `ComponentTemplateGenerator` still
emits `servoycoreErrorbean` if 1a is taken.

## Git history findings
- **`b9a2efea186`** (servoy-eclipse, Johan Compagner, 2026-08-13) — *"SVY-19023 skip
  deprecated components in template gen …"* added
  `ComponentTemplateGenerator.java:92` `if (spec.isDeprecated()) continue;`. Its stated
  goal was to prevent NG8001/NG8103 template errors for deprecated components. Side
  effect: the errorbean fallback template is no longer generated.
- **`8502a32506`** (servoy-client, Johan Compagner, 2026-08-13) — *"SVY-19023 mark
  unused servoycore components deprecated …"* set `"deprecated": "true"` on
  `errorbean.spec`. Combined with the above, this is what removed the substitute
  template.
- **`d1794cf2fdb`** (servoy-eclipse, 2026-08-12) — prettier reformat of `src/`; the
  editor `getTemplate()` unguarded `(this as any)[item.type]()` predates this and was
  only reformatted, not introduced, here. The guarded client variant
  (`typeof componentRef === 'function' ? …`) dates to `f975d74c102` (Andrei Costescu,
  2024-11-05) — i.e. the client was hardened long ago and the editor never was.
- No prior `docs/SVY-21380*` spec exists. `docs/` has no SVY-21195 spec checked in
  either, though SVY-21195 (Closed) is the direct predecessor.

A fix that re-includes errorbean in generation (1a) must add a comment explaining the
exemption so it is not "cleaned up" again as a deprecated-spec skip.

# Triage Report — SVY-21460

**Verdict:** NEEDS_INPUT (scope divergence between the ticket and the requested direction)

> The ticket text was fetched from Jira. The Angular/Java code investigation below was
> done by an isolated read-only agent and reconciled against the ticket. The core
> divergence is not about *where the code lives* — it is about **how far to go**: the
> ticket deliberately scopes a **non-rendering** projection, while the user's direction
> asks to actually **render the real DOM**. A human must pick the target before a spec.

## Reported problem

**Ticket SVY-21460** — "MCP endpoint: return the generated Angular template + JSON model
for a form (read-only, AI form-awareness)". Status: In Progress. Labels: `ai`,
`form-designer`, `mcp`. Fix version: 2026.12. Relates to SVY-21459 (closed research spike).

The problem: a Servoy `.frm` is an opaque Servoy-specific JSON persist that LLMs have no
training signal for, so authoring/editing it blind is unreliable. LLMs *are* strong at
HTML/Angular templates. The AI needs an **AI-readable, verifiable projection** of a form
so it can self-check that a `.frm` it just wrote matches its intent (a generate-then-check
loop), the way a developer eyeballs compiled output.

**What the ticket explicitly asks for (and explicitly rules out):**
- Add **one read-only MCP endpoint** that, given a form, returns:
  - the **component tag tree** — component tags wrapped in `svy-wrapper`/CSS-position divs
    for CSS-position forms; nested `svy-layoutcontainer` divs for responsive forms, and
  - the **JSON model** normally fed into the form (`{formName:{responsive, formCss, size,
    children:[…]}}`), e.g. embedded in a `<script type="application/json">` block.
- **"This is NOT meant to render (that needs the Angular runtime to construct the tags);
  it is meant as an AI-readable, verifiable projection of a form."** ← the ticket draws
  this line on purpose.
- Reuse existing building blocks; **no persist / client-runtime changes**.
- One-way (generate only); nothing parses HTML back → avoids all SVY-21459 round-trip risk.
- Open design decision in the ticket: **stateless** (endpoint takes the `.frm` content the
  LLM just wrote) vs **stateful** (endpoint takes a form name in the open solution).
  Ticket says start with whichever is simplest.
- Exit criterion: evaluate how useful the output is to the LLM.

**Building blocks the ticket names (verified in code):**
`FormTemplateGenerator.getPersistComponentTypeName(IFormElement)` (persist → component
type), `FormTemplateGenerator.getTagName(componentType)` (→ `data-<componentType>`
selector), `AngularFormGenerator.generateJS()` + `ChildrenJSONGenerator`
(`writeFormElement` / `writeLayoutContainer` / `writeCSSPosition`) producing the JSON
form-state model, and `form_component.component.ts` as the reference for the target tag
shape.

**Johan's comment on the ticket (2026-09-28):** "this is also a bit similar or working
together with really rendering a form. Maybe we should also do a bit more in the
FormPreview and render this inside a browser (just like openchamber can do) so it has full
access to the developer tools / browser DOM, so it can kind of see it through there."
→ This comment *points toward* the user's expanded direction, but as a "maybe", not the
ticket's committed scope.

## The user's requested direction (authoritative supplementary context)

The human who launched this pipeline asked for materially **more** than the ticket's
committed scope:

- Not just the *template projection*, but the **actual resulting HTML DOM that Angular
  renders** — "I really think we should generate the resulting HTML DOM that Angular will
  use."
- Achieve it by **reusing the NG-client renderer** (`node/src/ngclient/form/
  form_component.component.ts`) to *actually render* the form, **without data and without
  a running client** (no Servoy script executed).
- Likely a **new `node/src/formtemplate/` route** alongside `node/src/designer/`, with its
  own route (like the designer's), that uses far less machinery, injects different stuff,
  but drives the real form component; the **form cache is injected with designer-style
  form state**.
- Follow-up (explicitly out of scope for the core of this case): expose a URL that renders
  any form, embed a browser in the Servoy AI view, and hook it so the AI can inspect the
  DOM / click / screenshot.

## Root-cause assessment (code investigation)

This is a feature, so "root cause" = where the capability already exists and what is
genuinely missing. Two very different capabilities are in play depending on scope:

### For the ticket's *non-rendering projection* (Java-only, no Angular runtime)
Almost everything already exists on the Java side:
- `AngularFormGenerator.generateJS()` produces the exact JSON form-state model, and it
  **already runs headlessly** — `MobileExporter` loops all forms and calls it with no open
  editor and no live client. This is direct proof the JSON can be generated standalone.
- `FormTemplateGenerator` already maps persists → component types → `data-<type>` tag
  names.
- What is missing is only the **assembly**: walk the form-state children, emit the tag
  tree (`svy-wrapper` + CSS-position divs for anchored forms; nested
  `svy-layoutcontainer` divs for responsive), attach the JSON model, and expose it as an
  MCP endpoint. **No Angular route, no browser, no rendering.** This matches the ticket to
  the letter and is genuinely low-risk / "mostly assembly."

### For the user's *actual rendered DOM* (Angular runtime required)
A "render a form with no data and no client" pipeline **already exists** — it is the RFB
designer content route:
- Route `designer/solution/:solutionname/form/:formname/clientnr/:clientnr` →
  lazy-loaded `ServoyDesignerModule`; `ServoyDesignerComponent` connects to
  `/rfb/angular/content/`, calls `$editor.getData` (`ng2:true`), gets form-state JSON,
  does `formService.createFormCache(formName, formState, null)` and renders
  `<svy-designform>`.
- `DesignFormComponent` (`svy-designform`) extends the **same** `AbstractFormComponent`
  base as the runtime `FormComponent` (`svy-form`) and its template is a near-fork of the
  runtime template.
- No-data / no-client already holds: `FormService.setDesignerMode()` (`isInDesigner=true`),
  `ServoyPublicServiceDesignerImpl` stubs out every client call
  (`executeInlineScript`, `callServiceServerSideApi`, `showForm`, data push …),
  `ComponentCache.initForDesigner(...)` seeds models from static design values.
- The **runtime** `FormComponent` itself is client-coupled in its lifecycle
  (`resolveComponentCache` → `formService.formLoaded` service call, `onResize` →
  `sendChanges`, handler execution → `executeEvent`). So "reuse
  `form_component.component.ts` directly" is misleading: on a no-client route those calls
  would fire. The designer solves this by using the sibling `DesignFormComponent`, not the
  runtime component.
- The form-state JSON is produced server-side by `AngularFormGenerator` inside
  `DesignerWebsocketSession.executeMethod("getData", …)`, which is currently **bound to an
  open `BaseVisualFormEditor`** (styles/zoom come from the editor). A URL-addressable
  "render any form" path would need this decoupled into a headless provider.

So for the *rendered-DOM* interpretation, ~90% of the Angular machinery exists (designer
route), but it (a) requires an open editor, (b) emits designer-only DOM
(`svy-wrapper`/design attributes/wireframe), and (c) is not a clean addressable URL.

## Ticket premise check

- The **ticket's own premise is sound and minimal**: the Java building blocks exist, it is
  mostly assembly, and it explicitly avoids rendering to stay low-risk. If the goal is
  literally what the ticket says, a small Java MCP endpoint is the right, proportional
  answer — no new Angular route needed at all.
- The **user's premise** ("generate the resulting DOM by reusing `form_component.component.
  ts` in a new `formtemplate` route") is a *different, larger* feature. It is technically
  reasonable but: (1) it contradicts the ticket's "NOT meant to render"; (2) "reuse
  `form_component.component.ts`" really means "reuse an `AbstractFormComponent`-based
  renderer" — the designer already provides one (`DesignFormComponent`), so a brand-new
  route risks becoming a *third* fork of the form template to keep in sync; (3) the real
  effort is server-side (decouple `AngularFormGenerator`/styles from the open editor into a
  headless, URL-addressable provider), not the Angular route.

These are not two solutions to one problem — they are **two different problems at two
different ambition levels**. That is why this is `NEEDS_INPUT`.

## Approaches considered

1. **Ticket-faithful: Java-only non-rendering projection MCP endpoint.**
   Assemble tag tree + JSON model from `AngularFormGenerator`/`FormTemplateGenerator`;
   expose as one read-only MCP tool. Stateful (open solution form) first, optionally
   stateless (raw `.frm`) later.
   - Pros: exactly the ticket; low-risk; no client/persist changes; small; testable in
     `com.servoy.eclipse.tests`; the generator already runs headlessly (MobileExporter).
   - Cons: the AI sees a *static projection*, not the true rendered DOM — layout emergent
     from CSS/flex/responsive breakpoints, computed sizes, and any runtime template logic
     are NOT reflected. May be "good enough" for tag-tree verification but not for "see how
     it actually looks."

2. **User-faithful: new `node/src/formtemplate/` Angular route that renders real DOM
   (no data / no client).**
   New thin route/component reusing `AbstractFormComponent` rendering (designer-style
   `ServoyPublicService` stub + `setDesignerMode()`), fed by a **headless** form-state
   provider factored out of `DesignerWebsocketSession` (no open editor). Optionally strip
   designer-only chrome (ghosts/drag/decorators/variants/wrappers) so the DOM is close to
   true runtime.
   - Pros: gives the AI the *actual* rendered DOM; sets up the follow-up (URL + embedded
     browser + AI DOM access) cleanly; single shared server generator.
   - Cons: much larger; contradicts the ticket's stated non-rendering scope; real work is
     the server-side editor-decoupling + deciding runtime-vs-designer template; risk of a
     third form-template fork; needs verification that no client-coupled lifecycle call
     fires.

3. **Reuse the existing designer content route as-is / lightly extended.**
   Point the AI at the designer URL, or add a "clean render" flag to it.
   - Pros: least new code; proven pipeline.
   - Cons: requires an open editor; emits designer-only DOM; not a clean "render any form"
     URL; doesn't satisfy either the ticket (still renders, still designer chrome) or the
     full user direction (still editor-bound).

4. **No code change.**
   - Pros: none beyond zero effort.
   - Cons: neither the ticket's AI-projection need nor the user's rendered-DOM goal is met.
     Rejected.

## Recommendation

**Get a human decision on scope before writing a spec**, because the ticket and the
requested direction diverge on the single most important axis — *project vs render*:

- If the goal is the **ticket as written** (low-risk, non-rendering, "acid test" first
  step), go with **Approach 1** — a Java-only MCP endpoint. This is the recommended
  *first* deliverable: it directly satisfies SVY-21460, is small and safe, and its
  usefulness to the LLM is exactly the ticket's stated exit criterion. If it proves
  insufficient, that is the signal to invest in rendering.
- If the goal is the **user's rendered-DOM direction**, go with **Approach 2** — the new
  `formtemplate` route — understanding it is a substantially larger effort whose center of
  gravity is the server-side headless form-state/style provider, and that it should reuse
  an `AbstractFormComponent`-based renderer rather than the client-coupled runtime
  `FormComponent` verbatim.

A defensible sequencing is **Approach 1 now (this case), Approach 2 as the follow-up**
that also unlocks the embedded-browser AI-DOM-access idea — which is roughly how the
ticket frames its relationship to SVY-21459 and how Johan's comment reads.

## Git history findings

Not gathered in this run (the read-only investigation agent had no git tooling; the
orchestrator fetched only the ticket). Recommended before implementation of Approach 2:
- `git log --oneline -- com.servoy.eclipse.ngclient.ui/node/src/designer/` — how/why
  `DesignFormComponent` diverged from `FormComponent`.
- `git blame` on `DesignerWebsocketSession.getData` / `getStyleSheets` — the editor
  coupling and any prior headless attempts.
- History of `AngularFormGenerator` usage in `MobileExporter` — the established headless
  pattern to mirror.

## Questions for the reporter

1. **Project vs render — which is the target for THIS case?**
   (a) The ticket as written: a read-only MCP endpoint returning the **tag tree + JSON
   model**, explicitly **not** rendered (Java-only, low-risk); or
   (b) the expanded direction: actually **render the real HTML DOM** via a new Angular
   `formtemplate` route reusing the form renderer with no data / no client.
2. If (a): **stateful** (endpoint takes a form name in the open solution) or **stateless**
   (endpoint takes the raw `.frm` content the LLM just wrote), or both — and which first?
3. If (b): should the AI-facing DOM be **true runtime `svy-form` DOM** (cleaner, closer to
   production) or is **designer `svy-designform` DOM** (already wired, but with
   `svy-wrapper` wrappers + design attributes) acceptable? And must it render **arbitrary
   forms with no editor open** (headless), or is "the currently edited form" enough for the
   first iteration?
4. Is the **embedded-browser + AI-DOM-access** follow-up firmly **out of scope** for this
   case (a separate future case), as the user indicated?

# Spec: SVY-21360 — Toggle exploded view button works in reverse

## 1. Goal

Fix the "Exploded view" toolbar toggle in the RFB form designer so it reliably
applies the state the user clicked, instead of intermittently appearing to
apply the opposite state. The same fix resolves the duplicate ticket
SVY-21361 ("Dynamic guides toggle works in reverse"), since both symptoms are
caused by the same underlying defect: an uncaught `NG0951` error thrown by
`GhostsContainerComponent` that aborts a shared message-dispatch loop in
`EditorContentService`, silently dropping the message before decorator
components (dynamic guides, selection decorators, etc.) can react to it.

## 2. Background

### 2.1 Reproduction (no rename needed)

The ticket's original description says the bug appears "after a solution
rename." Investigation (see triage report, `docs/SVY-21360-triage.md`) found
the rename is not required to reproduce it. Simply opening a solution that
contains a **responsive form** built from a container/row/column layout plus
an AG Grid component — with **no ghosts** (no parts/config ghosts to render)
— and pressing the **Exploded view** toolbar button
(`com.servoy.eclipse.designer.rfb/node/src/designer/toolbar/toolbar.component.ts`)
reproduces the crash and the inverted-toggle symptom every time.

### 2.2 The NG0951 crash

`com.servoy.eclipse.designer.rfb/node/src/designer/ghostscontainer/ghostscontainer.component.ts`
declares:

```ts
readonly elementRef = viewChild.required<ElementRef<Element>>('element');
```

and uses it in `hideShowGhosts()`:

```ts
hideShowGhosts(visibility: string) {
    const elementRef = this.elementRef();   // throws NG0951 here
    if (elementRef) {
        ...
    }
}
```

The `#element` template reference target only exists inside a `@for` loop in
`ghostscontainer.component.html`:

```html
@for (container of ghostContainers(); track container) {
  <div #element class="ghostcontainer {{container.class}}" ...>
```

When `ghostContainers()` is empty (an entirely normal case — e.g. the
responsive form described above, with no parts/config ghosts to show), no
`#element` node is ever created. `viewChild.required(...)` throws
(`NG0951`, https://angular.dev/errors/NG0951) whenever its query has no
match, instead of returning `undefined` the way an optional `viewChild`
query (or the pre-migration `@ViewChild`) would. So `this.elementRef()`
throws every time `hideShowGhosts()` runs for such a form.

### 2.3 Why the throw causes an *inverted* toggle, not just a crash

`hideShowGhosts()` is invoked from `contentMessageReceived()` for nearly
every inbound message:

```ts
contentMessageReceived(id: string, data: {...}) {
    ...
    if (id !== 'hideGhostContainer' && id !== 'positionClick') {
        this.hideShowGhosts('visible');   // runs (and throws) for most message ids
    }
    if (id === 'hideGhostContainer') {
        this.hideShowGhosts('hidden');
    }
}
```

`contentMessageReceived` is called from a bare, unguarded `forEach` in
`com.servoy.eclipse.designer.rfb/node/src/designer/services/editorcontent.service.ts`:

```ts
this.contentMessageListeners.forEach(listener => listener.contentMessageReceived(event.data.id, event.data));
```

When `GhostsContainerComponent.contentMessageReceived` throws, the exception
propagates out of `forEach` and **aborts the iteration**. Every listener
registered *after* `GhostsContainerComponent` in `contentMessageListeners`
(template/creation order in `designer.component.html`: selection decorators
→ highlight → **ghosts container** → same-size indicator → anchoring
indicator → editor content) silently does not receive that particular
message.

Because the "Exploded view" toggle and the dynamic-guides toggle both rely on
a message reaching the content iframe and having the relevant decorator
components re-render in response, a message dropped mid-toggle looks exactly
like "the toggle applied the opposite of what I clicked" — this matches both
the SVY-21360 symptom and the SVY-21361 symptom ("dynamic guides appear when
disabled"), since both toggles go through this same broadcast mechanism and
the same failure point.

### 2.4 Git history

Introduced by:

```
7e854931ac (Johan Compagner, 2026-08-06 17:41:11 +0200)
"migrate RFB ViewChild/ViewChildren to signal queries (13/14 migrated) [ai]"
```

This commit mechanically converted
`@ViewChild('element', { static: false }) elementRef!: ElementRef<Element>;`
to `readonly elementRef = viewChild.required<ElementRef<Element>>('element');`,
and updated the call site from `if (this.elementRef)` to
`const elementRef = this.elementRef(); if (elementRef) {...}`.

Before the migration, an unmatched `@ViewChild` simply left `elementRef` as
`undefined`, and the existing `if (this.elementRef)` guard handled that
gracefully. `viewChild.required()` has no such fallback — it throws instead.
The commit message itself flags this was a semi-automated migration ("13/14
migrated"); this is the one case where applying `.required()` was incorrect,
because the target element is conditionally rendered (`@for` over a
possibly-empty array) rather than always present. No other
`viewChild.required(...)` call site in the RFB module sits inside a
conditional (`@if`/`@for`) template region.

There is no earlier spec in `docs/` covering this behavior; it was
accidentally introduced by an unrelated, structural, non-behavioral
migration commit, not by a deliberate design change.

## 3. Design

### 3.1 Revert `elementRef` to an optional signal query

In `ghostscontainer.component.ts`, change:

```ts
readonly elementRef = viewChild.required<ElementRef<Element>>('element');
```

back to:

```ts
readonly elementRef = viewChild<ElementRef<Element>>('element');
```

This restores the exact pre-migration semantics: when the `@for` loop
produces no `#element` node (no ghosts), `this.elementRef()` returns
`undefined` instead of throwing.

No other change is needed in `hideShowGhosts()`:

```ts
hideShowGhosts(visibility: string) {
    const elementRef = this.elementRef();
    if (elementRef) {
        ...
    }
}
```

The existing `if (elementRef) {...}` guard already handles the `undefined`
case correctly — it simply skips the hide/show logic when there is no ghosts
container element, which is the desired behavior for a form with no ghosts.

### 3.2 Scope boundary

This is a minimal, targeted revert of the one call site where the
ViewChild-to-signal migration was semantically incorrect. It does not touch:

- Any other `viewChild.required(...)` call sites in the RFB module (all 13
  other migrated sites target always-present template elements and are
  unaffected).
- `EditorContentService`'s listener-dispatch `forEach` loop. Hardening that
  loop with a per-listener try/catch was considered during triage (see
  "Out of scope" below) but is explicitly not part of this fix.

### 3.3 Why this fixes both tickets

Since the NG0951 throw is the sole cause of the aborted
`contentMessageReceived` dispatch loop, and both SVY-21360's "Exploded view"
toggle and SVY-21361's "Dynamic guides" toggle depend on messages reaching
listeners registered after `GhostsContainerComponent` in that same loop,
removing the throw resolves both symptoms without any additional
toggle-specific code changes.

## 4. Implementation plan

1. In `com.servoy.eclipse.designer.rfb/node/src/designer/ghostscontainer/ghostscontainer.component.ts`,
   change the `elementRef` declaration from
   `viewChild.required<ElementRef<Element>>('element')` to
   `viewChild<ElementRef<Element>>('element')`.
2. Leave `hideShowGhosts()` unchanged — its existing `if (elementRef) {...}`
   guard already handles the `undefined` case.
3. Run `npm run lint` and `npm run build_debug_nowatch` in
   `com.servoy.eclipse.designer.rfb/node/` to verify the change compiles and
   passes lint.
4. Run the existing unit test
   `src/designer/ghostscontainer/ghostscontainer.component.spec.ts` (`npm
   test`) to confirm no regression, and add/adjust a test case covering
   `hideShowGhosts()` on a component with an empty `ghostContainers()` (no
   `#element` match) to assert it no longer throws.
5. Manually verify (or via browser test) that toggling "Exploded view" on a
   responsive form with a container/row/column layout + AG Grid and no
   ghosts no longer throws `NG0951` in the console and applies the clicked
   state correctly.
6. Manually verify the dynamic guides toggle (SVY-21361 reproduction: CSS
   position form editor, enable/disable dynamic guides) no longer shows
   guides in the disabled state.

## 5. Acceptance criteria

- [ ] Opening a solution with a responsive form (container/row/column +
      AG Grid, no ghosts) and pressing "Exploded view" no longer throws
      `NG0951` in the designer iframe console.
- [ ] The "Exploded view" toggle applies the state the user clicked (enabled
      shows exploded view, disabled hides it) on such a form.
- [ ] The dynamic guides toggle on a CSS position form no longer shows
      guides after being disabled (SVY-21361 symptom resolved).
- [ ] `ghostscontainer.component.ts`'s `hideShowGhosts()` and
      `contentMessageReceived()` are otherwise unchanged.
- [ ] No other `viewChild.required(...)` call site in the RFB module is
      modified.
- [ ] `npm run lint` and `npm run build_debug_nowatch` pass with zero errors
      in `com.servoy.eclipse.designer.rfb/node/`.
- [ ] Existing and new unit tests for `GhostsContainerComponent` pass.

## 6. Out of scope

- Hardening `EditorContentService`'s `contentMessageListeners.forEach`
  dispatch loop with a per-listener try/catch. This was considered during
  triage as a defensive follow-up that would prevent any future
  single-listener bug from silently starving other decorator components of
  messages, but it is explicitly excluded from this fix — the fix here is
  the minimal, targeted revert of the incorrect `viewChild.required()`
  usage only.
- Any other component touched by the "migrate RFB ViewChild/ViewChildren to
  signal queries" commit (`7e854931ac`) — only `GhostsContainerComponent`'s
  `elementRef` is affected by this defect.

## 7. Open questions

None — triage verdict was PROCEED with a single, well-understood root cause
and approved approach.

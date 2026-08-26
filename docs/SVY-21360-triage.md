# Triage Report — SVY-21360

**Verdict:** PROCEED

## Reported problem

Ticket summary: "Toggle exploded view button works in reverse". Description: after a
solution rename, for a responsive form with a container/row/column layout and an
AG Grid component, the "Exploded view" enable/disable toggle behaves inverted from
what the user clicked.

User-supplied context adds a concrete technical symptom that reproduces the same
button: pressing **Exploded view** (toolbar button in
`com.servoy.eclipse.designer.rfb/node/src/designer/toolbar/toolbar.component.ts`)
throws in the designer iframe's browser console:

```
Uncaught Error: NG0951
    at ... elementRef (...)
    at t.hideShowGhosts (...)
    at t.contentMessageReceived (...)
```

This was only observed by remote-attaching a debugger to an installed/packaged
Developer, because that is the only way to see the embedded designer iframe's
browser console in a production build; it is not evidence that the bug itself is
build-mode-specific.

## Root-cause assessment

The stack trace maps 1:1 onto
`com.servoy.eclipse.designer.rfb/node/src/designer/ghostscontainer/ghostscontainer.component.ts`:

```ts
readonly elementRef = viewChild.required<ElementRef<Element>>('element');
...
hideShowGhosts(visibility: string) {
    const elementRef = this.elementRef();   // <-- throws NG0951 here
    if (elementRef) {
        ...
    }
}
```

`elementRef` is declared with `viewChild.required(...)`, which by Angular's contract
(NG0951, https://angular.dev/errors/NG0951) **throws** if the query has no match,
instead of returning `undefined`. The `#element` template reference it targets only
exists inside a `@for` loop in `ghostscontainer.component.html`:

```html
@for (container of ghostContainers(); track container) {
  <div #element class="ghostcontainer {{container.class}}" ...>
```

When a form has **no ghosts** (`ghostContainers()` is an empty array — a completely
ordinary case, e.g. a responsive form made only of a container/row/column layout
and an AG Grid, with no parts/config ghosts to show), no `#element` node is ever
created, so `this.elementRef()` throws NG0951 every time `hideShowGhosts()` runs.

`hideShowGhosts()` is called from `contentMessageReceived()` for almost any inbound
message:

```ts
contentMessageReceived(id: string, data: {...}) {
    ...
    if (id !== 'hideGhostContainer' && id !== 'positionClick') {
        this.hideShowGhosts('visible');   // <-- runs, and throws, for most message ids
    }
    if (id === 'hideGhostContainer') {
        this.hideShowGhosts('hidden');
    }
}
```

`contentMessageReceived` is invoked from
`com.servoy.eclipse.designer.rfb/node/src/designer/services/editorcontent.service.ts`:

```ts
this.contentMessageListeners.forEach(listener => listener.contentMessageReceived(event.data.id, event.data));
```

This is a bare, un-guarded `forEach`. If one listener's `contentMessageReceived`
throws, the exception propagates out of `forEach` and **aborts the iteration**, so
every listener registered *after* `GhostsContainerComponent` in
`contentMessageListeners` silently does not receive that message.

In `designer.component.html`, template/creation order registers listeners roughly as:
`selection-decorators` (MouseSelectionComponent) → `designer-highlight`
(HighlightComponent) → `designer-ghostscontainer` (GhostsContainerComponent, the
one that throws) → `designer-samesize-indicator` → `designer-anchoring-indicator` →
`designer-editorcontent` (EditorContentComponent, which itself also handles
`updateFormSize`/`contentSizeChanged`). All of these later listeners intermittently
miss messages whenever a ghost-less form triggers the NG0951 throw. Because the
"Exploded view" toggle relies on a `showWireframe` message reaching the content
iframe and the various decorator components re-rendering in response, a message
silently dropped mid-toggle looks exactly like "the toggle applied the opposite of
what I expected" — matching the reported symptom, and its still-open Jira duplicate,
SVY-21361 ("Dynamic guides toggle works in reverse"), which shares the same
message-broadcast mechanism and same failure point.

### Git history

```
7e854931ac (Johan Compagner, 2026-08-06 17:41:11 +0200)
"migrate RFB ViewChild/ViewChildren to signal queries (13/14 migrated) [ai]"
```

This commit mechanically converted
`@ViewChild('element', { static: false }) elementRef!: ElementRef<Element>;`
to `readonly elementRef = viewChild.required<ElementRef<Element>>('element');` and
updated the call site from `if (this.elementRef)` to
`const elementRef = this.elementRef(); if (elementRef) {...}`.

Before the migration, an unmatched `@ViewChild` simply left `elementRef` as
`undefined`, and the existing `if (this.elementRef)` guard handled that gracefully.
`viewChild.required()` does not have that fallback — it throws. The commit message
itself flags this was a semi-automated migration ("13/14 migrated"), and this is the
one case where blindly applying `.required()` was incorrect, because the target
element is conditionally rendered (`@for` over a possibly-empty array) rather than
always present. No other `viewChild.required(...)` call site in the RFB module sits
inside a conditional (`@if`/`@for`) template region — this one is the exception,
which is exactly why it was missed by an otherwise mechanical migration.

There is no earlier spec in `docs/` for this behavior; it was accidentally
introduced by an unrelated, structural, non-behavioral migration commit.

## Ticket premise check

The ticket describes only the symptom ("works in reverse") and proposes no fix
approach of its own — there is nothing to challenge there. The user-supplied
technical context (NG0951 stack trace) does correctly point at the real defect
location (`hideShowGhosts`/`contentMessageReceived` in
`ghostscontainer.component.ts`), and investigation confirms it precisely.

## Approaches considered

1. **Change `elementRef` back to an optional `viewChild('element')` signal query**
   (not `.required`). Pros: minimal, one-line change; restores the exact pre-migration
   semantics (`elementRef()` returns `undefined` instead of throwing when there are no
   ghosts, and the existing `if (elementRef)` guard already handles that). No behavior
   change for forms that do have ghosts. Cons: none identified — this is a pure
   revert-of-regression.
2. **Wrap `EditorContentService`'s listener-dispatch `forEach` in a
   try/catch per listener** (log and continue instead of aborting the loop). Pros:
   makes the whole content-message pipeline resilient to any single listener's bug,
   preventing this entire class of "later listeners silently stop receiving messages"
   failure in the future. Cons: it is a defensive hardening, not a fix for the actual
   defect; applying it alone would suppress the NG0951 crash's side effects but leave
   the underlying incorrect `viewChild.required()` usage (and its console errors) in
   place.
3. **Both 1 and 2 together.** Pros: fixes the concrete bug and hardens the dispatch
   loop against a repeat of this failure mode elsewhere. Cons: slightly larger diff,
   touches a shared service used by every designer decorator component (low risk,
   but worth flagging since it's shared infrastructure).
4. **No code change.** Not viable — this is a genuine, reproducible product regression
   with a console error and a matching, plausible causal chain to the reported
   symptom, introduced by an identifiable recent commit.

## Recommendation

**PROCEED** with approach 1 as the primary, minimal fix: change
`ghostscontainer.component.ts`'s `elementRef` from `viewChild.required<...>('element')`
to the optional `viewChild<...>('element')`, keeping the existing
`if (elementRef) {...}` guard in `hideShowGhosts()` untouched (it already handles
`undefined` correctly). This directly removes the NG0951 throw and, by extension,
should resolve the "works in reverse" symptom for both SVY-21360 and its duplicate
SVY-21361, since both stem from the same aborted message-dispatch loop.

Approach 2 (hardening `EditorContentService`'s dispatch loop with a per-listener
try/catch) is worth doing as a follow-up in the same change or a small separate one,
since it prevents any future single-listener bug from silently starving other
decorator components of messages — but it should not be treated as a substitute for
fixing the actual `viewChild.required()` misuse.

## Questions for the reporter

Not applicable — verdict is PROCEED, not NEEDS_INPUT.

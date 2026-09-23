# Triage Report — SVY-21386

**Verdict:** PROCEED

## Reported problem
On a responsive form (`resp`), a newly added **empty row at the bottom** of the form does
not stay at the bottom — after reopening Developer it is misordered (renders at/near the
top).

### Decisive experiment (authoritative)
The reporter ran a controlled A/B test that isolates *which code path* introduces the
misorder:

1. **Add a new row AND column via the OUTLINE view** at the bottom of the form, then
   **reopen Developer** → the new row/column **REMAIN at the bottom (correct).**
2. **Drag-and-drop a row from the PALETTE** at the bottom → after reopening Developer it
   is **misordered.**

Reporter's conclusion (verified below): *"it is added weirdly when dragging and dropping
from the palette."*

This is decisive because **both** paths persist and reload through the **same**
sort/serialization pipeline. If the reload/sort were the cause, the Outline-added row would
be misordered *too*. It is not. Therefore the differentiator is **not** the reload — it is
the `location` value each creation path stores.

A reproduction solution (`tst_respScrollbars2.servoy`) is attached. Its responsive form
`resp` contains a container with three `row` children whose stored `location`
(content_id 414, an `x,y` string) values are:

| row (col child)            | location x,y |
|----------------------------|--------------|
| row with `table_12`        | `400,364`    |
| row with `textboxgroup_1`  | `384,248`    |
| row with `fileupload_3`    | `392,98`     |

The ticket describes only the symptom; it proposes no solution.

## Root-cause assessment

Responsive layout containers (rows/columns) have no meaningful visual X coordinate — they
stack vertically, so their intended order is driven by an **ordering counter** stored in
`location`. The two creation paths assign that counter very differently, and *that* is the
bug the reporter isolated.

### Primary — palette bottom-drop stores the raw drop coordinate instead of a `max+1` counter

`CreateComponentCommand.createComponent`, layout-container branch
(`com.servoy.eclipse.designer/.../CreateComponentCommand.java:575-578`):

```java
List<IPersist> res = createLayoutContainer(form, parentSupportingElements, layoutSpec, sameTypeChildContainer, config,
    args.getRightSibling() != null
        ? getLocationAndShiftSiblings(form, parentSupportingElements, args, extraChangedPersists)
        : args.getLocation(),                               // <-- bottom drop (no right sibling)
    specifications, args.getPackageName());
```

- **Right-sibling drop** (drop between/above an existing row): `getLocationAndShiftSiblings`
  (`CreateComponentCommand.java:822-900`) renumbers all siblings with a running counter and
  returns a sane `(counter, counter)`. This path is fine.
- **No-right-sibling drop** (the "add at the bottom" case): the code passes
  `args.getLocation()` **verbatim**. That value comes straight from the Angular palette:
  `palette.component.ts:277-288` sets `component.x = event.pageX` / `component.y = event.pageY`
  (adjusted only for scroll/iframe offset) and `CreateComponentOptions.fromJson`
  (`CreateComponentCommand.java:1179`) turns it into `new Point(args.optInt("x"), args.optInt("y"))`.
  For a bottom drop no `beforeChild`/`rightSibling` is sent (`palette.component.ts:305-315`
  only set `rightSibling` when `beforeChild` exists), so the `: args.getLocation()` branch is taken.
- `createLayoutContainer` then stores that raw pixel point directly:
  `container.setLocation(new Point(location.x, location.y))` (line 732), or for a
  CSS-position container `setCssPosition(... top=location.y ... left=location.x ...)` (line 728).

So the new row's `x,y` is a **screen pixel coordinate**, not an ordering counter, and is
**not guaranteed to be greater than the existing rows' counters**. In the repro the
existing rows carry counters `x` = 384/392/400 (and `y` = 98/248/364). A raw pixel drop
whose `x` (or `y`) is smaller than those lands the new row **before** them in the sort,
which is exactly the "new bottom row jumps to the top" symptom. This is the value that
differs between the palette path and the Outline path.

### Why the Outline-add path is correct (the control case)

Adding a row via the Outline goes through `AddContainerCommand`:

- `computeNextLayoutContainerIndex` (`AddContainerCommand.java:511-534`) returns
  `max(over all sibling x and y) + 1`.
- `addLayoutComponent` (`AddContainerCommand.java:455`) then stores
  `container.setLocation(new Point(index, index))` — i.e. **x == y == max+1**, strictly
  greater than every sibling on *both* axes.

A child whose `x` and `y` both exceed every sibling sorts **last under either ordering**
(X-first *or* Y-first). That is precisely why the Outline-added row stays at the bottom
across a restart. The palette path lacks this `max+1` guarantee, so it is not immune.

## Ticket premise check
The ticket proposes no fix, so there is no premise to overturn — only a symptom to
explain. The symptom is a genuine Servoy Developer bug (not user misconfiguration, not
third-party, not expected behavior): the palette drag-drop create path stores a raw drop
coordinate for a bottom-dropped responsive container instead of a `max+1` ordering counter,
so the new row is not guaranteed to sort last and appears misordered after reload.

## Approaches considered

1. **Fix the palette bottom-drop location in `CreateComponentCommand`** (recommended) —
   in the layout-container branch, when there is **no right sibling**, assign the new
   responsive/CSS-position container a `max(sibling)+1` ordering counter (the same value
   the Outline path and the right-sibling `getLocationAndShiftSiblings` path already
   produce) instead of the raw `args.getLocation()`. Concretely, route the no-right-sibling
   case through the sibling-aware counter logic (reuse the `computeNextLayoutContainerIndex`
   helper) so it stores `new Point(max+1, max+1)`.
   - Pros: fixes the exact path the reporter isolated; makes "drop at the bottom"
     deterministic regardless of the pixel drop coordinate; aligns the palette path with
     the already-correct Outline path; unit/integration testable at the command level.
   - Cons: must scope the change to responsive / CSS-position-child containers so it does
     not disturb absolute-layout drops (which legitimately use `args.getLocation()` as a
     real pixel position); needs care not to regress the drop-preview positioning.
2. **No code change.**
   - Pros: none.
   - Cons: the reported bug persists; a palette-dropped bottom row is misordered after
     restart, with an attached repro and a clean A/B isolation. Not acceptable.

## Recommendation
**PROCEED** with **Approach 1**: fix the **palette drag-drop bottom-drop location
assignment** in `CreateComponentCommand.createComponent`
(`com.servoy.eclipse.designer/.../CreateComponentCommand.java:575-578`, the
`: args.getLocation()` branch). For a responsive form / CSS-position-child container drop
with no right sibling, compute and store `new Point(max(sibling)+1, max(sibling)+1)` — the
same monotonic counter the Outline-add path (`AddContainerCommand.computeNextLayoutContainerIndex`
+ `setLocation(new Point(index, index))`) and the right-sibling renumbering path already
produce — instead of the raw Angular drop coordinate. This makes a palette bottom-drop
behave exactly like the (already-correct) Outline add, resolving the reported symptom.

Accompany the fix with a test that exercises the shared
`AddContainerCommand.computeNextLayoutContainerIndex` helper: given a container holding the
repro's three rows, assert the returned counter exceeds every sibling's `x` and `y` (so a
new container placed at `new Point(index, index)` sorts last), mirroring the Outline-add
behavior.

## Git history findings
- **Palette bottom-drop location (`: args.getLocation()`)** — the layout-container branch in
  `CreateComponentCommand` that takes `args.getLocation()` verbatim when there is no right
  sibling predates the recent churn; the file's latest touch (`23ffcbd`,
  *"read designsize property for component palette sizing and drop [ai]"*) changed sizing,
  not this location branch. This raw-coordinate assignment is the code the reporter's
  experiment implicates.
- **Outline-add counter** — `AddContainerCommand.computeNextLayoutContainerIndex` returns
  `max(x,y)+1` and `addLayoutComponent` stores `new Point(index, index)`; this `max+1`,
  equal-x-y value is why the Outline path sorts last under any ordering and survives a
  restart, i.e. the control case.
- `SVY-20153` "Row drop place inconsistent with preview in responsive form" (`34a5a74`) and
  the surrounding responsive-drop history confirm the drop-location logic in this command has
  been iterated before — reinforcing the value of a regression test on the bottom-drop counter.

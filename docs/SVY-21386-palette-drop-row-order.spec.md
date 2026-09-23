# Spec: SVY-21386 — Palette bottom-drop responsive row order not preserved after reload

## 1. Goal

Make a responsive layout container (row/column) dragged from the **palette** and dropped
at the **bottom** of a responsive form (no right sibling) stay at the bottom — both in the
Outline immediately and after Developer is reopened — by assigning it a monotonic
`max(sibling)+1` ordering counter instead of the raw Angular pixel drop coordinate, exactly
as the already-correct Outline-add path does.

## 2. Background

### 2.1 The symptom

On a responsive form (`resp`), a new empty row added at the bottom does not stay at the
bottom after reopening Developer — it renders at/near the top. The reporter isolated the
cause with a decisive A/B experiment:

- **Outline add** a new row/column at the bottom, reopen Developer → row stays at the bottom (correct).
- **Palette drag-drop** a row at the bottom, reopen Developer → row is misordered (bug).

Both paths persist and reload through the same sort/serialization pipeline. Because the
Outline-added row orders correctly and the palette-dropped one does not, the differentiator
is **not** the reload/sort — it is the `location` value each creation path stores at
creation time.

The attached repro solution `tst_respScrollbars2.servoy` (form `resp`) contains a container
with three `row` children whose stored `location` (`x,y`) values are `400,364`, `384,248`,
`392,98`. These are ordering counters, not visual coordinates.

### 2.2 Responsive containers order by a stored counter, not a real coordinate

Responsive layout containers (rows/columns) have no meaningful visual X coordinate — they
stack vertically, so their intended order is driven by an **ordering counter** stored in the
persist's `location` (an `x,y` pair). Correct ordering therefore depends on that counter
being strictly greater than every sibling's when a new container is meant to sort last.

### 2.3 The two creation paths

**Palette drag-drop (buggy path)** —
`com.servoy.eclipse.designer/.../editor/rfb/actions/handlers/CreateComponentCommand.java`.
In `createComponent`, the layout-container branch (lines 575–578) decides the location to
hand to `createLayoutContainer`:

```java
List<IPersist> res = createLayoutContainer(form, parentSupportingElements, layoutSpec, sameTypeChildContainer, config,
    args.getRightSibling() != null
        ? getLocationAndShiftSiblings(form, parentSupportingElements, args, extraChangedPersists) : args.getLocation(),
    specifications, args.getPackageName());
```

- **Right-sibling drop** (`args.getRightSibling() != null`): `getLocationAndShiftSiblings`
  (lines 822–900) renumbers all siblings with a running counter and returns a sane
  `(counter, counter)`. This path is fine and must not change.
- **No-right-sibling drop** (the "add at the bottom" case): the code passes
  `args.getLocation()` **verbatim**. That value originates in the Angular palette
  (`palette.component.ts` sets `component.x = event.pageX` / `component.y = event.pageY`,
  adjusted only for scroll/iframe offset) and `CreateComponentOptions.fromJson`
  (line 1179) turns it into `new Point(args.optInt("x"), args.optInt("y"))`. For a bottom
  drop no `beforeChild`/`rightSibling` is sent, so the `: args.getLocation()` branch is
  taken.

`createLayoutContainer` (lines 712–791) then stores that point directly. For a
CSS-position container (`CSSPositionLayoutContainer`):

```java
((CSSPositionLayoutContainer)container)
    .setCssPosition(new CSSPosition(Integer.toString(location.y), "-1", "-1", Integer.toString(location.x), "200", "200"));
```

and for a plain layout container:

```java
container.setLocation(new Point(location.x, location.y));
if (CSSPositionUtils.isCSSPositionContainer(layoutSpec)) container.setSize(new Dimension(200, 200));
```

So a bottom-dropped responsive container's `x,y` is a screen pixel coordinate, not an
ordering counter, and is **not guaranteed** to exceed the existing siblings' counters. In
the repro the siblings carry counters 384/392/400 (x) and 98/248/364 (y); a raw pixel drop
smaller than those lands the new row **before** them in the sort — the "new bottom row jumps
to the top" symptom.

**Outline add (correct control path)** —
`com.servoy.eclipse.designer/.../editor/commands/AddContainerCommand.java`.

- `computeNextLayoutContainerIndex` (lines 511–534) returns
  `max(over all sibling x and y) + 1` (for a CSS-position ancestor it returns the child
  count instead).
- `addLayoutComponent` (line 455) stores `container.setLocation(new Point(index, index))` —
  i.e. `x == y == max+1`, strictly greater than every sibling on both axes.

A child whose `x` and `y` both exceed every sibling sorts **last under either ordering**
(X-first or Y-first). That is why the Outline-added row survives a restart. The palette path
lacks this `max+1` guarantee, so it is not immune. This is the pattern the palette
bottom-drop fix must mirror.

## 3. Design

### 3.1 Palette bottom-drop location fix

Change the layout-container branch in `CreateComponentCommand.createComponent` so that, for
a **responsive / CSS-position-child** drop with **no right sibling**, the new container
receives a `max(sibling)+1` ordering counter (`new Point(max+1, max+1)`) instead of the raw
`args.getLocation()`.

Requirements:

- **Scope strictly to responsive / layout-container drops.** Absolute-layout drops (a plain
  layout container on a non-responsive, non-CSS-position parent) legitimately use
  `args.getLocation()` as a real pixel position and must continue to do so. Use the same
  responsive/CSS-position guard `getLocationAndShiftSiblings` itself already uses so the two
  paths agree on what "responsive" means:

  ```java
  form.isResponsiveLayout() || parentSupportingElements instanceof CSSPositionLayoutContainer
  ```

  (In this branch `parentSupportingElements` is the drop parent `ISupportFormElements`; a
  `CSSPositionLayoutContainer` parent means a CSS-position-child drop even on a
  non-responsive form.)

- **Compute the counter the same way the Outline path does** — `max(x,y)+1` over the
  siblings — so the palette bottom-drop and the Outline add produce identical values.
  Prefer a single shared helper over duplicating the loop:
  - Reuse the logic of `AddContainerCommand.computeNextLayoutContainerIndex` (lines 511–534),
    including its CSS-position-ancestor special case (`return child count` when
    `parent.getAncestor(IRepository.CSSPOS_LAYOUTCONTAINERS) != null`). Extract it into a
    static, reusable form (see §4) and call it from both commands so they cannot drift, OR
    replicate the exact same computation locally in `CreateComponentCommand` if extraction is
    impractical. Extraction is preferred.

- **Preserve drop-preview behavior.** The change only affects the persisted ordering counter,
  not the transient Angular drag preview. The right-sibling path
  (`getLocationAndShiftSiblings`) is untouched.

Resulting shape of the location argument (conceptual):

```java
Point location;
if (args.getRightSibling() != null)
{
    location = getLocationAndShiftSiblings(form, parentSupportingElements, args, extraChangedPersists);
}
else if (form.isResponsiveLayout() || parentSupportingElements instanceof CSSPositionLayoutContainer)
{
    int index = computeNextLayoutContainerIndex(parentSupportingElements); // max(x,y)+1 (or child count for CSS-pos ancestor)
    location = new Point(index, index);
}
else
{
    location = args.getLocation(); // absolute layout: real pixel position, unchanged
}
List<IPersist> res = createLayoutContainer(form, parentSupportingElements, layoutSpec, sameTypeChildContainer, config,
    location, specifications, args.getPackageName());
```

The exact structuring (inline vs. helper) is the coder's choice as long as: (a) the
responsive/CSS-position no-right-sibling case stores `new Point(max+1, max+1)` computed
identically to the Outline path, (b) the right-sibling path is unchanged, and (c) the
absolute-layout path still uses `args.getLocation()`.

### 3.2 Git history

- The `: args.getLocation()` layout-container branch predates recent churn; the file's latest
  touch (`23ffcbd`, *"read designsize property for component palette sizing and drop [ai]"*)
  changed sizing, not this location branch. This raw-coordinate assignment is the code the
  reporter's experiment implicates.
- `AddContainerCommand.computeNextLayoutContainerIndex` (`max(x,y)+1`) +
  `addLayoutComponent` (`new Point(index, index)`) is the established correct pattern to
  mirror.
- `SVY-20153` ("Row drop place inconsistent with preview in responsive form", `34a5a74`) and
  the surrounding responsive-drop history show this command's drop-location logic has been
  iterated before, reinforcing the value of a regression test on the bottom-drop counter.

## 4. Implementation plan

Files: `com.servoy.eclipse.designer` (this workspace).

1. **Extract the ordering-counter helper.** In
   `com.servoy.eclipse.designer/.../editor/commands/AddContainerCommand.java`, promote
   `computeNextLayoutContainerIndex(IPersist parent)` (lines 511–534) to a `public static`
   method (keeping its exact behaviour, including the
   `getAncestor(IRepository.CSSPOS_LAYOUTCONTAINERS)` child-count special case and the
   `max(x,y)+1` loop). Update its existing caller inside `AddContainerCommand` accordingly.

2. **Primary fix in `CreateComponentCommand.createComponent`** (file
   `com.servoy.eclipse.designer/.../editor/rfb/actions/handlers/CreateComponentCommand.java`,
   lines 575–578). Replace the inline ternary location argument with the three-way decision
   from §3.1:
   - right sibling present → `getLocationAndShiftSiblings(...)` (unchanged);
   - else if `form.isResponsiveLayout() || parentSupportingElements instanceof CSSPositionLayoutContainer`
     → `new Point(index, index)` where `index = AddContainerCommand.computeNextLayoutContainerIndex(parentSupportingElements)`
     (the extracted helper from step 1);
   - else → `args.getLocation()` (unchanged, absolute layout).
   Ensure imports resolve (`CSSPositionLayoutContainer` is already used in this file;
   `AddContainerCommand` is already referenced, e.g. `AddContainerCommand.showDataproviderDialog`).

3. **Verify, organize imports, format** each changed Java file; run
   `eclipse-ide_getCompilationErrors` on `com.servoy.eclipse.designer` and resolve any issues.

## 5. Acceptance criteria

- [ ] Dragging a row from the palette to the **bottom** of a responsive form (no right
      sibling) stores a `location` whose `x` and `y` both equal `max(sibling x,y)+1`, so it
      sorts **last** and stays at the bottom in the Outline and after reopening Developer.
- [ ] For a bottom drop into a container holding the repro's three rows (counters
      384/392/400 x, 98/248/364 y), the new container's stored counter (`x == y`) is strictly
      greater than every sibling's `x` and `y`, regardless of the raw pixel
      `args.getLocation()` supplied.
- [ ] Dropping a row **between/above** existing rows (right-sibling path,
      `args.getRightSibling() != null`) still routes through `getLocationAndShiftSiblings` and
      is unchanged (no regression).
- [ ] **Absolute-layout / non-responsive** drops (plain layout container, non-responsive
      form, non-`CSSPositionLayoutContainer` parent) still store the pixel
      `args.getLocation()` (no regression).
- [ ] A CSS-position-child drop on a non-responsive form (parent is a
      `CSSPositionLayoutContainer`) also gets the `max+1`/child-count counter, matching
      `computeNextLayoutContainerIndex`'s CSS-position-ancestor branch.
- [ ] The palette bottom-drop counter equals what the Outline-add path
      (`AddContainerCommand`) would produce for the same parent (both use the same helper /
      identical computation).
- [ ] `com.servoy.eclipse.designer` compiles with no new errors, and the two
      highest-severity SpotBugs categories report no new findings in the changed code.

## 6. Out of scope

- Any change to the Angular palette/drag-preview code
  (`palette.component.ts`, `ghostscontainer.component.ts`, `designform_component.component.ts`).
- Changing `getLocationAndShiftSiblings` or the right-sibling renumbering behaviour.
- Migrating or rewriting existing forms' stored `location` counters (the repro solution's
  already-wrong values are not retroactively "fixed"; only newly dropped rows are correct).
- **Any change to `PositionComparator` / `RESPONSIVE_PERSIST_COMPARATOR`.** The comparator
  contains a separate dead-code smell (its `YX` layout-container branch is overwritten, so
  containers effectively sort X-first). This is *not* the differentiator for SVY-21386 — the
  `max+1` counter sorts last under either ordering — and flipping the long-standing X-first
  runtime behaviour to Y-first would change ordering for existing responsive forms with
  disagreeing x/y (wider blast radius, historically churny). It should be addressed under its
  own ticket with dedicated QA, not here.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should `computeNextLayoutContainerIndex` be extracted to a shared util (e.g. on `CSSPositionUtils`) rather than made `public static` on `AddContainerCommand`, to avoid a designer→command coupling? Coder may choose the least-invasive option that keeps a single source of truth. | dev | open |

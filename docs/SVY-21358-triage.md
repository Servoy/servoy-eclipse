# Triage Report — SVY-21358

**Verdict:** NEEDS_INPUT

## Reported problem

Running a **responsive form containing an aggrid/datagrid** in the Titanium client
produces console errors. The ticket quotes one server-side error:

```
ERROR [Executor,uuid:e510185:2] org.sablo.specification.property.types.ObjectPropertyType
  - [default or 'object' type toJSONValue] unsupported value type:null for value:
    java.awt.Dimension[width=140,height=20] current json:  [ ]
java.lang.IllegalArgumentException: unsupported value type; see value in log entry
```

The attached `servoy_log.txt` actually contains **two unrelated errors**, and the
architect comments make clear the ticket is an umbrella to "track down (E2E) failures
like this" (Johan Compagner) and to "check for more similar e2e problems" (Andrei
Costescu). No single fix is proposed.

## Root-cause assessment

The log holds two independent failures at two different times, in two different
processes:

### Error 1 — server side, form designer (10:34:05, no clientid)

Stack (log lines 140–169):

```
ObjectPropertyType.getJSONAndClientSideType(ObjectPropertyType.java:281)
ObjectPropertyType.toJSONValueImpl(ObjectPropertyType.java:85)
JSONUtils.defaultToJSONValue(JSONUtils.java:165)
NGConversions$FormElementToJSON.toJSONValue(NGConversions.java:349)
ChildrenJSONGenerator.writeFormElement(ChildrenJSONGenerator.java:362)
...
AngularFormGenerator.generateJS(AngularFormGenerator.java:270)
DesignerWebsocketSession.executeMethod(DesignerWebsocketSession.java:229)
```

The `DesignerWebsocketSession` frame and the missing `clientid` show this is the
**form designer** generating the form's Angular JSON, not the runtime client. A
`java.awt.Dimension[140,20]` (the default component size from
`WebComponent.getSize()` → `new java.awt.Dimension(140, 20)`,
`servoy-client/servoy_shared/.../WebComponent.java:447`) reaches
`ObjectPropertyType.getJSONAndClientSideType`, whose `else` branch
(`sablo/.../ObjectPropertyType.java:274-282`) has no handling for `Dimension` and logs
the error. `AngularFormGenerator` itself *does* have an explicit `Dimension` branch
(`AngularFormGenerator.java:241-249`) — sablo's generic object serializer does not.

This error path is the designer branch of
`ChildrenJSONGenerator.writeFormElement` (`ChildrenJSONGenerator.java:342-377`).
**Git history strongly suggests this is already fixed:** commit `6261bcd67a6`
("remove size/location for designer (css pos is written)", Johan Compagner,
**2026-08-20 11:36**) added, to exactly that designer branch:

```java
// remove the size and location properties, should not be used anymore in the client code
templateProperties.content.remove(IContentSpecConstantsBase.PROPERTY_SIZE);     // line 359
templateProperties.content.remove(IContentSpecConstantsBase.PROPERTY_LOCATION); // line 360
```

The log was captured at **10:34 the same day** — roughly one hour *before* that
commit. The equivalent removal already existed in the non-designer branch
(`ChildrenJSONGenerator.java:415-416`, commit `ebcb1765130`). The commit is an
ancestor of current `master` (`git merge-base --is-ancestor` confirms). Johan's
comment "i think this should be fixed" matches this: the `Dimension[140,20]` was the
persist `size` property leaking into the object serializer, and it is now stripped
before serialization on the designer path.

### Error 2 — client side, Titanium runtime (10:34:44, clientid aggrid_tst)

Browser console error relayed to the server log (lines 170–344):

```
setHeight@.../chunk-34TYITQ3.js:66879:11
svyOnChanges@.../chunk-34TYITQ3.js:201852:18
ngOnChanges@...
...
throwErrorIfNoChangesMode@.../chunk-V3Q2W6AK.js:9402:9
checkNoChangesInternal@...
DataGrid_Template@.../chunk-34TYITQ3.js:204992:29
```

This is an Angular **NG0100 `ExpressionChangedAfterItHasBeenCheckedError`**: the
`DataGrid` component calls `setHeight(...)` from inside `svyOnChanges`/`ngOnChanges`,
mutating a bound value during change detection. Under the app's zoneless /
`checkNoChanges` regime this throws. This is a defect in the **DataGrid (ag-grid)
component**, which lives in the external **`servoy-nggrids`** package — it is not in
this `servoy-eclipse` repo, nor in `servoy-client`/`sablo`. I searched all local
repos and found no nggrids/datagrid source checked out here.

## Ticket premise check

The ticket proposes no concrete fix; it is a Minor, open-ended tracking ticket
("use this case to track down E2E failures like this", "check for more similar e2e
problems"). The single error it quotes (Error 1) appears to have already been
addressed by commit `6261bcd67a6`, which is in `master`. The real remaining defect
(Error 2, the NG0100 in DataGrid) is not mentioned in the ticket body at all and
lives in a different repository.

## Approaches considered

1. **Treat Error 1 as the target and add a `Dimension` branch to sablo's
   `ObjectPropertyType`** — pros: robust safety net for any `Dimension` that leaks
   into generic object serialization. Cons: `java.awt.Dimension` is a Servoy/AWT
   concept leaking into a generic sablo type; likely papers over the real cause;
   Error 1 already appears fixed at source by `6261bcd67a6`, so this may be redundant.
   Also a `sablo` repo change, outside this `servoy-eclipse` pipeline.

2. **Fix Error 2 (NG0100) in the DataGrid component** — move the `setHeight` state
   mutation out of `svyOnChanges` (e.g. defer, use a signal/`markForCheck`, or guard
   against re-entrant CD). Pros: fixes the real runtime error. Cons: the code is in
   the external `servoy-nggrids` package, not present in this workspace; cannot be
   specced/implemented from here without that source.

3. **No code change** — pros: Error 1 is very likely already resolved in `master`
   (commit `6261bcd67a6` + Johan's "i think this should be fixed"); the ticket may
   simply need verification/closure. Cons: Error 2 (NG0100) is a genuine,
   still-open runtime defect that no one has explicitly scoped or fixed.

4. **Broad E2E error sweep** — honour Andrei's "check for more similar e2e problems"
   by auditing all components for `svyOnChanges`-during-CD mutations and `Dimension`/
   object-type serialization leaks. Pros: matches the architects' stated intent.
   Cons: unbounded scope; not a single specifiable fix; spans multiple repos.

## Recommendation

**NEEDS_INPUT.** The divergence test trips: the one attachment contains two
materially different root causes, in two different codebases, and the ticket lacks
the detail to tell which the reporters actually want fixed here:

- Error 1 (server `Dimension` → `ObjectPropertyType`, designer path) appears
  **already fixed** by `6261bcd67a6` in `master`, consistent with Johan's comment.
- Error 2 (client NG0100 in `DataGrid.setHeight` during `svyOnChanges`) is a real,
  open defect but lives in the **`servoy-nggrids`** package, which is **not present in
  this workspace** and outside the `servoy-eclipse` repo this pipeline operates on.

A spec cannot be written that produces a fix in this repository without first
resolving scope. A human decision is required.

## Git history findings

- `6261bcd67a6` — "remove size/location for designer (css pos is written)" (Johan
  Compagner, 2026-08-20 11:36). Added `PROPERTY_SIZE`/`PROPERTY_LOCATION` removal to
  the designer branch of `ChildrenJSONGenerator.writeFormElement`
  (`ChildrenJSONGenerator.java:358-360`). This is the code path in Error 1's stack and
  was committed ~1h after the attached log was captured. In current `master`.
- `ebcb1765130` — earlier commit; already had the same removal in the non-designer
  branch (`ChildrenJSONGenerator.java:415-416`), and added the `Dimension` handling in
  `AngularFormGenerator.java:241-249`.
- `sablo` `ObjectPropertyType.java:274-282` (the `else`/error branch) is long-standing
  and has no `Dimension` case.

## Questions for the reporter

1. **Server-side `ObjectPropertyType` / `Dimension[140,20]` error:** this looks
   already fixed by commit `6261bcd67a6` ("remove size/location for designer"), which
   was committed about an hour after the attached `servoy_log.txt` was captured and is
   now in `master`. Can you confirm this specific error no longer reproduces on a
   current build? If it does still reproduce, please attach a fresh log so we can see
   which property still carries the `Dimension`.

2. **Client-side NG0100 (`ExpressionChangedAfterItHasBeenCheckedError`):** the second
   error in the log is the `DataGrid` component calling `setHeight(...)` from inside
   `svyOnChanges` during change detection. That code lives in the **`servoy-nggrids`**
   component package, not in `servoy-eclipse` or `servoy-client`. Should this be split
   into a separate ticket against the nggrids/DataGrid component, or tracked here? (We
   cannot fix it from this repository.)

3. **Scope of this ticket:** Andrei's comment asks to "check for more similar e2e
   problems." Is SVY-21358 meant to be (a) verification/closure of the specific
   `Dimension` error, (b) a fix for the DataGrid NG0100, or (c) a broader audit of E2E
   console errors? Each implies very different, non-overlapping work.

# Spec: SVY-21358 — E2E object-serialization hardening (Dimension/Point safety-net + generator audit)

## 1. Goal

Harden the server-side form-JSON generation pipeline so that a leaked
`java.awt.Dimension` / `java.awt.Point` (persist `size` / `location`) value can never
again produce a hard `ObjectPropertyType` "unsupported value type" error, and audit the
NG-client form generators (`ChildrenJSONGenerator`, `AngularFormGenerator`) for any
remaining code paths where such AWT values could leak into the generic `object`
serializer. This is a defensive robustness pass — a safety net plus a targeted audit —
in response to the architects' request to "track down E2E failures like this" and
"check for more similar e2e problems". It is explicitly not a rewrite.

## 2. Background

The attached `servoy_log.txt` on SVY-21358 contains **two independent, unrelated
errors**. Only Error 1 is in scope for this repo/servoy-client/sablo; Error 2 is out of
scope (see §6).

### Error 1 — server-side `ObjectPropertyType` "unsupported value type" (IN SCOPE)

```
ERROR org.sablo.specification.property.types.ObjectPropertyType
  - [default or 'object' type toJSONValue] unsupported value type:null for value:
    java.awt.Dimension[width=140,height=20] current json: [ ]
java.lang.IllegalArgumentException: unsupported value type; see value in log entry
```

Stack (designer path, no `clientid`):
`ObjectPropertyType.getJSONAndClientSideType` → `toJSONValueImpl` →
`JSONUtils.defaultToJSONValue` → `NGConversions$FormElementToJSON.toJSONValue` →
`ChildrenJSONGenerator.writeFormElement` → `AngularFormGenerator.generateJS` →
`DesignerWebsocketSession.executeMethod`.

Root cause: the default component size `new java.awt.Dimension(140, 20)`
(`servoy-client/servoy_shared/src/com/servoy/j2db/persistence/WebComponent.java:447`)
reached the generic `object`-type serializer. `ObjectPropertyType.toJSONValueImpl`'s
final `else` branch (`sablo/.../ObjectPropertyType.java:274-282`) knows how to write
numbers, booleans, strings, char sequences and dates, but has **no case for
`Dimension`/`Point`**, so it logs an error and writes nothing.

Prior art / where it is already handled:
- `AngularFormGenerator` writes `Dimension` explicitly for the form's own properties
  (`AngularFormGenerator.java:241-249`) but has no `Point` case there.
- `ChildrenJSONGenerator.writeFormElement` strips the persist `size`/`location`
  properties before serialization on **both** branches:
  - non-designer branch: lines 415-416 (long-standing, commit `ebcb1765130`)
  - designer branch: lines 358-360 (commit `6261bcd67a6`, "remove size/location for
    designer (css pos is written)", Johan Compagner, 2026-08-20 11:36)
- Confirmed: `6261bcd67a6` is an ancestor of current `servoy-client` `master`
  (`git merge-base --is-ancestor` = true). The attached log was captured ~1h before
  that commit, so the specific designer leak Error 1 shows **is already fixed at
  source**. Johan's comment "i think this should be fixed" matches.

So the concrete leak is fixed, but the generic `object` serializer remains a single
point of failure: any future path (a new form-element variant, a component property
typed `object` that ends up holding an AWT value, etc.) that lets a `Dimension`/`Point`
through will again hit a hard error instead of degrading gracefully. This spec adds that
missing safety net and audits the generator paths so we can be confident no other known
leak exists.

### Error 2 — client-side NG0100 in DataGrid (OUT OF SCOPE)

An Angular `ExpressionChangedAfterItHasBeenCheckedError` (`NG0100`): the `DataGrid`
component calls `setHeight(...)` from inside `svyOnChanges`/`ngOnChanges` during change
detection. This lives in the external **`servoy-nggrids`** package, which is not present
in this workspace. The user is handling it in a separate session. See §6.

## 3. Design

### 3.1 Safety-net for `Dimension`/`Point` in the generic object serializer (sablo)

Add explicit handling for `java.awt.Dimension` and `java.awt.Point` to the value-type
dispatch in `ObjectPropertyType.toJSONValueImpl`, in the block ending at the current
`else` error branch (`sablo/.../ObjectPropertyType.java:256-282`).

- `Dimension` → write `{ "width": <w>, "height": <h> }` (mirror
  `AngularFormGenerator.java:241-249`, using `getWidth()`/`getHeight()`).
- `Point` → write `{ "x": <x>, "y": <y> }` (mirror the shape used by
  `NGPointPropertyType`).

Design considerations / constraints:
- `java.awt.Dimension`/`Point` are AWT/Servoy concepts leaking into a generic sablo
  type. To keep sablo free of an AWT compile dependency where possible, detect by
  fully-qualified class name via reflection **or** by importing `java.awt.Dimension` /
  `java.awt.Point` (both are in the JDK, so no new dependency — `AngularFormGenerator`
  already imports them). Prefer the direct `instanceof` import for readability, matching
  the existing `AngularFormGenerator` style, unless sablo's build must stay strictly
  headless (verify: sablo is plain Java/AWT-available, so `instanceof` is fine).
- Keep the existing `else` error branch as the true fallback for genuinely unknown
  types — do not weaken the diagnostic for real unsupported values.
- The written value has no client-side type (like the number/boolean/string cases): the
  returned `JSONStringWithClientSideType` keeps `type` unchanged (plain `object`).

### 3.2 Generator-path audit (servoy-client)

Read-and-verify pass (no functional change expected; add a `Point` case only if a real
gap is found):

1. `ChildrenJSONGenerator.writeFormElement` (lines 342-423): confirm **both** the
   designer branch (358-360) and the non-designer branch (415-416) strip
   `PROPERTY_SIZE` and `PROPERTY_LOCATION`. Confirm there is no third branch or
   form-element variant (e.g. form-component sub-elements, inherited elements) that
   serializes template properties without the same removal.
2. `AngularFormGenerator.generateJS` / property-writing loop (lines 225-255): the form's
   own property loop handles `Dimension` but **not** `Point`. Add a `Point` case for
   symmetry so a `location`-typed form property cannot hit the generic `writer.value`
   fallback with an AWT `Point`.
3. Grep for other `object`-typed / template-property serialization entry points in
   `servoy_ngclient/.../ngclient` that could receive a persist `size`/`location` and do
   not strip them; record findings in the PR description. If a genuine leak is found,
   strip `PROPERTY_SIZE`/`PROPERTY_LOCATION` there too (same pattern as
   `ChildrenJSONGenerator`).

The §3.1 sablo safety-net is the backstop; §3.2 keeps the leaks from reaching it in the
first place.

### 3.3 Regression test

Add a focused unit test for the sablo change (the audit changes are covered indirectly):
- In sablo (`org.sablo.specification.property.types`), a test that calls the object-type
  serialization on a `java.awt.Dimension` and a `java.awt.Point` and asserts the JSON is
  `{"width":...,"height":...}` / `{"x":...,"y":...}` and that **no** `IllegalArgumentException`
  is logged / thrown. Follow the existing sablo test conventions for `ObjectPropertyType`
  (locate an existing `*ObjectPropertyType*`/property-type test to mirror harness setup).

## 4. Implementation plan

1. **sablo — add Dimension/Point cases** (safety net, the core fix)
   File: `/home/gabi/github_master/sablo/sablo/src/main/java/org/sablo/specification/property/types/ObjectPropertyType.java`
   - Add `else if (converted instanceof java.awt.Dimension)` and
     `else if (converted instanceof java.awt.Point)` cases before the final `else`
     error branch (currently lines 274-282), writing the object shapes from §3.1.
   - Add `import java.awt.Dimension;` and `import java.awt.Point;` if using direct
     `instanceof`.

2. **sablo — regression test**
   Add a test (mirroring existing property-type tests under
   `/home/gabi/github_master/sablo/sablo/src/test/...`) asserting Dimension/Point
   serialize without error. Verify the exact test source root and harness first.

3. **servoy-client — AngularFormGenerator Point symmetry**
   File: `/home/gabi/github_master/servoy-client/servoy_ngclient/src/com/servoy/j2db/server/ngclient/AngularFormGenerator.java`
   - In the property-writing loop (around lines 241-253), add an
     `else if (value instanceof Point)` case writing `{ "x", "y" }`, next to the
     existing `Dimension` case. (`java.awt.Point` already imported at line 21.)

4. **servoy-client — audit ChildrenJSONGenerator / generator paths**
   File: `/home/gabi/github_master/servoy-client/servoy_ngclient/src/com/servoy/j2db/server/ngclient/ChildrenJSONGenerator.java`
   - Verify both branches strip `PROPERTY_SIZE`/`PROPERTY_LOCATION` (lines 358-360 and
     415-416). Only add code if the audit finds an unstripped path. Record findings in
     the PR/commit body.

5. **Build & verify**
   - sablo: `mvn -q -pl sablo verify` (or the module's standard build) — compile + run
     the new test.
   - servoy-client: compile the `servoy_ngclient` project (Eclipse/JDT) and confirm no
     compilation errors after the `Point` addition.

## 5. Acceptance criteria

- [ ] `ObjectPropertyType` serializes a `java.awt.Dimension` as
      `{"width":w,"height":h}` and a `java.awt.Point` as `{"x":x,"y":y}` without logging
      an error or throwing `IllegalArgumentException`.
- [ ] The existing generic `else` error branch still fires (unchanged diagnostic) for a
      genuinely unsupported type (e.g. an arbitrary POJO).
- [ ] A sablo unit test covers both the Dimension and Point cases and passes.
- [ ] `AngularFormGenerator` handles `Point` in its form-property write loop, symmetric
      with the existing `Dimension` handling.
- [ ] Audit of `ChildrenJSONGenerator` (and any adjacent generator serialization paths)
      is documented in the PR body; both existing size/location strips are confirmed, and
      any newly-found leak is fixed with the same removal pattern.
- [ ] sablo module builds and tests pass; `servoy_ngclient` compiles with no errors.

## 6. Out of scope

- **Error 2 — DataGrid NG0100 (`ExpressionChangedAfterItHasBeenCheckedError`) in
  `servoy-nggrids`.** Not present in this workspace; the user is handling it in a
  separate session. If desired, split into its own ticket against the nggrids/DataGrid
  component.
- Re-fixing Error 1's specific designer leak — already fixed by `6261bcd67a6` in
  `master`. This spec adds a backstop, it does not revert or duplicate that fix.
- Any broader "audit every component for `svyOnChanges`-during-CD mutations" sweep
  (Andrei's wider ask) — unbounded and cross-repo; not attempted here beyond the DataGrid
  note above.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should sablo detect Dimension/Point by `instanceof` (AWT is JDK, no new dep) or stay strictly reflection-based to avoid coupling? | dev/architect | Proposed: `instanceof` (JDK types, mirrors AngularFormGenerator). Confirm during impl. |
| Is there an existing sablo `ObjectPropertyType` test to mirror, and what is the correct test source root/harness? | dev | To confirm at impl step 2. |
| Does the audit (step 4) find any generator path that serializes persist size/location without stripping? | dev | To be answered by the audit; findings go in PR body. |
| Should Error 2 be split into a dedicated `servoy-nggrids` ticket, or tracked under SVY-21358? | reporter/architect | Open (user handling separately). |

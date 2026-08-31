# Triage Report — SVY-21357

**Verdict:** PROCEED

## Reported problem
When a TiNG (NG2) form is put into Find Mode, a `combobox` bound to a value list
displays the raw dataprovider value (e.g. the real integer/UUID) instead of the
value list's configured display value (e.g. `2` instead of `Chang`). The same
scenario works correctly in NG1. Reproduced with a minimal demo solution
(`VLFindMode.servoy`, attached to the ticket) on Servoy 2024.3.9 and 2025.3.5.
No fix approach is proposed in the ticket — it is a plain bug report.

## Root-cause assessment
The TiNG combobox component is
`com.servoy.eclipse.ngclient.ui/node/projects/servoydefault/src/lib/combobox/combobox.ts`.
Its `svyOnChanges` contains:

```ts
svyOnChanges(changes: SimpleChanges) {
    this.valueComparator = ...;
    if (changes['dataProviderID'] && this.findmode) {
        this.formattedValue = this.dataProviderID;          // <-- bypasses the valuelist entirely
    } else if ( (changes['dataProviderID'] || changes['valuelistID']) && this.valuelistID) {
        const valueListElem = this.valuelistID.find(this.valueComparator);
        if (valueListElem) this.formattedValue = this.formatService.format(valueListElem.displayValue, this.format, false);
        else { ... resolve via valuelist.getDisplayValue(...) ... }
    }
    ...
}
```
(`combobox.ts:162-192`)

When `findmode` is true, the code short-circuits and assigns the raw
`dataProviderID` to `formattedValue` unconditionally, skipping the normal
value-list lookup/`getDisplayValue()` resolution path that runs in every other
mode. This is exactly the symptom reported: the combobox shows the real value
instead of the display value while in Find Mode.

This branch was introduced by:
```
092542a5327 (cPecican, 2023-11-29) SVY-18673 All fields should be editable in find mode - update
e68e95c9c5 / 58f50cf4d9 (Gabi Boros, 2023-11-16) SVY-18673 All fields should be editable in find mode
```
SVY-18673's goal ("All fields should be editable in find mode") was that
fields which are normally read-only/disabled become editable once a form
enters Find Mode, so the user can type search criteria into them. For
`calendar`, `check`, `checkgroup` and `typeahead` — components with an actual
text-input element — that translates into removing `readonly`/`disabled`
attributes (`basefield.ts:92-98`, `check.ts:54`, `typeahead.ts:88-90`). Those
changes are legitimate: the widget is genuinely editable and a user could type
a raw search expression (`>100`, `!2`, etc.) that would not resolve through a
value list, so bypassing value-list formatting is defensible there.

The `combobox` component, however, has no free-text input on its main
display element at all — `combobox.html` renders a plain
`<button>...<span>{{formattedValue}}</span></button>` with a `ngbDropdown`
menu of value-list items (`combobox.html:2-16`); the *only* way to change the
value is picking an item from the dropdown, which always yields a value from
the value list (`updateValue(value.realValue)` at `combobox.html:27`). There
is no scenario in which `dataProviderID` in Find Mode holds a raw, free-typed
search expression that could break value-list formatting — so the
justification that applies to `calendar`/`typeahead` does not apply to
`combobox`. The `findmode` special case was copy-pasted into `combobox.ts`
without recognizing this component doesn't need it, and it broke value-list
resolution for the one case that does need it: showing the current record's
value using its display value when the form switches into Find Mode.

**NG1 comparison** confirms this: NG1's combobox
(`servoy-client/servoy_ngclient/war/servoydefault/combobox/combobox.html:14`)
always runs its value through the `showDisplayValue` filter
(`combobox.js:220-256`) regardless of find mode — there is no `findMode`
special case in the NG1 implementation at all. That is why NG1 "just works"
and TiNG doesn't: TiNG added a find-mode bypass that NG1 never had.

## Ticket premise check
The ticket proposes no solution, so there is no premise to validate beyond
"this is a Servoy bug, not user error." That premise holds: the demo project
reproduces the issue cleanly with a standard value-list-backed combobox and
default Find Mode usage, and the regression is traceable to a specific,
identifiable code change (SVY-18673) rather than any misconfiguration.

## Approaches considered
1. **Remove the `findmode` short-circuit from `combobox.ts`** so it always
   goes through the existing value-list resolution branch (`else if` block),
   just like in normal (non-find) mode. — Pros: minimal, surgical, matches
   NG1 behavior exactly, no regression risk to the calendar/typeahead/check
   fixes from SVY-18673 (untouched). Cons: none identified; the combobox
   never accepts free-typed values so there's no case this could break.
2. **Keep the find-mode bypass but only as a fallback** when the value-list
   lookup finds no matching entry (merge the two branches: try value-list
   resolution first, fall back to raw value only if unresolved). — Pros:
   more defensive in case some future combobox variant does allow typed
   entry. Cons: unnecessary complexity for a component that structurally
   cannot receive typed values today; adds a code path that can never be
   exercised.
3. **No code change** — Rejected. This is a real, reproducible regression
   with clear origin and no configuration workaround: the user cannot make
   the value list resolve through any client-side property.

**Recommended:** Approach 1. It is the simplest fix, has an identical
precedent (this is literally what the code did before SVY-18673 touched this
file), and the review of `combobox.html` shows no code path where a free-typed
raw value could reach `dataProviderID` in find mode, so there's nothing to
protect against.

## Recommendation
Proceed with Approach 1: remove the special `this.findmode` branch in
`ServoyDefaultCombobox.svyOnChanges` (`combobox.ts:164-165`) so that
`dataProviderID` changes always go through the value-list lookup /
`getDisplayValue()` resolution path, exactly as they do outside Find Mode.
This restores parity with NG1 and fixes the reported symptom without
affecting the other components (`calendar`, `check`, `checkgroup`,
`typeahead`) that legitimately need the find-mode editability behavior from
SVY-18673.

## Git history findings
- `092542a5327` / `e68e95c9c5` / `58f50cf4d9` / `4f273196ee` (SVY-18673, Nov
  2023): introduced the `findmode` bypass across `basefield.ts`,
  `calendar.ts`, `check.ts`, `typeahead.ts`, and `combobox.ts`. Only the
  `combobox.ts` bypass is a regression; the others operate on components with
  genuine text-input editability.
- `6fa62cdb96` (SVY-19157, Nov 2024): unrelated later fix in the same method
  (recompute `formattedValue` when `valuelistID` itself changes) — confirms
  the file is actively maintained and the `findmode` branch was not revisited
  since its introduction.
- No prior spec found in `docs/` for SVY-18673 or SVY-21357.

## Additional verification (servoy_client repo)
Per request, verified the server-side find-mode plumbing in the `servoy-client`
repo (`com.servoy.j2db.dataprocessing.FoundSet`) to make sure the regression
isn't rooted there instead of in the TiNG client code:

- `FoundSet.find()` / `FoundSet.setFindMode()`
  (`servoy_shared/src/com/servoy/j2db/dataprocessing/FoundSet.java:5567-5619`)
  only flip the internal `findMode` boolean and fire `fireFindModeChange()`.
  They don't touch value formatting or display in any way - this is pure
  state management (also runs the `onFind`/`onAfterFind` triggers and clears
  the foundset to a single new find state).
- That event propagates through `DataAdapterList.setFindMode()`
  (`servoy_ngclient/src/com/servoy/j2db/server/ngclient/DataAdapterList.java:1278`)
  to every registered `IFindModeAwarePropertyValue`, including
  `DataproviderTypeSabloValue.findModeChanged()`
  (`servoy_ngclient/.../property/types/DataproviderTypeSabloValue.java:283`),
  which just flips its own local `findMode` flag and asks for a repaint.
- `DataproviderTypeSabloValue.getValueForToJSON()`
  (same file, lines 738-745) has its own `if (findMode)` branch that
  **always** serializes the raw `uiValue` to the client as a plain string,
  for every component type, by design: in find mode a user can type a raw
  search expression (`>100`, `!2`, ranges, `%x%`, etc.) that cannot be
  resolved through any value list, so the server intentionally does not try
  to resolve/format it - it hands the client component the raw value and
  leaves the display decision to the component.

Conclusion: the server-side (`servoy-client`) find-mode plumbing behaves as
designed and is **not** the source of the regression - it correctly defers
the display decision to the client component rather than trying to resolve a
value list itself. The bug is confined to the TiNG `combobox.ts` client-side
bypass identified above, which is the one component that fails to make that
display decision correctly even though it (unlike `calendar`/`check`/
`typeahead`) structurally never accepts free-typed input in find mode. This
does not change the recommendation.

## Additional verification (all four find-mode-capable valuelist components)

Per request, investigated the equivalent components in the `bootstrapcomponents`
repo (Bootstrap combobox, Bootstrap typeahead) and the servoydefault typeahead
(TiNG), to see whether the same regression exists elsewhere.

**Bootstrap combobox** (`bootstrapcomponents/projects/bootstrapcomponents/src/combobox/combobox.ts`,
`svyOnChanges`, around line 222) has the **identical bug**:
```ts
if (changes['dataProviderID'] && this.findmode()) {
    this.formattedValue.set(this._dataProviderID());
} else if (...) { /* valuelist resolution */ }
```
Git history traces this to the same commit family as the TiNG bug: `be743c2`
("SVY-18673 All fields should be editable in find mode - update"), the exact
same ticket that introduced the TiNG regression. The bypass was copy-pasted
into the Bootstrap combobox for the same unjustified reason - this component's
only value-entry path is picking from the dropdown (`updateValue(realValue)`),
so it never receives a free-typed search expression either. Same root cause,
same fix: remove the `findmode()` bypass so it always goes through valuelist
resolution.

**servoydefault (TiNG) typeahead** (`servoydefault/src/lib/typeahead/typeahead.ts`,
`inputFormatter`) has **no find-mode guard at all** - it unconditionally runs
`valuelistID.find(...)` and, if unresolved, `valuelistID.getDisplayValue(...)`,
regardless of find mode. This is why it "just works": it was never touched by
SVY-18673's find-mode bypass in the first place (the bypass was only ever
applied to `basefield`/`calendar`/`check`/`typeahead`'s *editability* handling,
not to typeahead's display-value formatting).

**Bootstrap typeahead** (`bootstrapcomponents/projects/bootstrapcomponents/src/typeahead/typeahead.ts`,
`inputFormatter`) *does* have a find-mode guard:
```ts
else if (!this.findmode() && valuelistID?.hasRealValues()) { ... }
```
However, this did **not** originate from SVY-18673. It was added by a separate,
later commit `0221bfa` ("SVY-19974 No results after searching for datetime in
listformcomponent results in errors") - a real crash fix. SVY-19974's stack
trace (`TypeError: Cannot read properties of undefined (reading
'getInternalState')` in `ValuelistType.fromServerToClient`) shows a race: a
foundset-linked valuelist inside a list form component had its client-side
valuelist reference reset by a find-mode transition before an in-flight
`getDisplayValue()` server round-trip response could be applied. The fix as
written is blunt: it disables *all* display-value resolution in find mode,
including the synchronous, already-in-memory `valuelistID.find(...)` lookup
that carries no such race (no server round-trip, nothing to arrive late). That
over-broad guard is what reproduces the SVY-21357 symptom for Bootstrap
typeahead too, for the common case of a small/static valuelist.

**Targeted fix for Bootstrap typeahead:** split the guard so only the async
`getDisplayValue()` fallback (the actual source of the SVY-19974 race) stays
guarded by `!findmode()`, while the synchronous `valuelistID.find(...)` lookup
runs unconditionally, in find mode or not:
```ts
inputFormatter = (result: any) => {
    ...
    else if (valuelistID?.hasRealValues()) {
        const value = valuelistID.find((item) => { ... });
        if (value) {
            result = value.displayValue;
        } else if (!this.findmode()) {
            // async getDisplayValue() round-trip - keep SVY-19974 guard here only
            ...
        } else {
            return '';
        }
    }
    ...
};
```
This resolves the reported symptom for values already present in the loaded
valuelist (the common case, matching the reported scenario) without
reintroducing the SVY-19974 race for DB-backed valuelists that require an
async server round-trip.

### Updated recommendation (scope)
Proceed with Approach 1 across all three affected spots:
1. TiNG `combobox.ts` - remove the `findmode` bypass entirely (as originally recommended).
2. Bootstrap `combobox.ts` - remove the `findmode()` bypass entirely (same fix, same justification).
3. Bootstrap `typeahead.ts` - split the `inputFormatter` guard so only the async `getDisplayValue()` path remains guarded by `!findmode()`; the synchronous `.find()` lookup runs unconditionally.

No change needed for servoydefault (TiNG) typeahead - it is already correct.

### Additional git history findings
- `bootstrapcomponents` `be743c2` (SVY-18673, same ticket as the servoy-eclipse.ngclient.ui `092542a5327`/`e68e95c9c5` commits): introduced the identical `findmode` bypass into Bootstrap `combobox.ts`.
- `bootstrapcomponents` `0221bfa` (SVY-19974, Feb 2025): introduced the `!this.findmode` guard into Bootstrap `typeahead.ts`'s `inputFormatter`, fixing a real client crash (`getInternalState` on undefined) caused by a find-mode valuelist-reference reset racing an in-flight async `getDisplayValue()` response. This guard is broader than necessary.

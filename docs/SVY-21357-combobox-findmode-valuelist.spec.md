# Spec: SVY-21357 — Value List Display Value Not Resolved in Combo Box During Find Mode in TiNG

## 1. Goal
When a TiNG (NG2) form enters Find Mode, a `combobox` bound to a value list must keep
showing the value list's configured display value (e.g. `Chang`) for the record's
current dataprovider value (e.g. `2`), exactly as it does outside Find Mode and exactly
as NG1 already does. Today it shows the raw dataprovider value instead. The same class
of regression (loss of value-list-resolved display value while in Find Mode) exists in
the equivalent components of the separate `bootstrapcomponents` package: `combobox.ts`
has the identical bug, and `typeahead.ts` has an over-broad guard that produces the same
symptom for valuelists that are already loaded client-side.

## 2. Background

### 2.1 Cross-repository scope
This fix touches **two separate git repositories**:

1. `servoy-eclipse` (this repository) — `com.servoy.eclipse.ngclient.ui` project, TiNG
   `combobox.ts`.
2. `bootstrapcomponents` — a standalone repository checked out locally at
   `D:\GitSourcesComponents\bootstrapcomponents`, **not** a submodule or subfolder of
   `servoy-eclipse`. It contains `combobox.ts` and `typeahead.ts` for the Bootstrap
   component package.

The implementation and commit phases must operate on both repositories independently:
each gets its own commit(s), its own build/lint/test cycle, and its own review. Do not
attempt to combine changes from both repos into a single commit or a single build step.
The `servoy-eclipse` `AGENTS.md`/Eclipse MCP tooling (Maven, Eclipse project model,
`eclipse-git` tools) applies only to repo 1. Repo 2 is a plain Angular workspace outside
the Eclipse workspace and must be edited/built/committed using its own tooling (its own
`package.json`, its own git repo at its own root) — plain file edit tools and terminal
`git`/`npm` commands, not the Eclipse MCP `eclipse-*` tools, which only see the Eclipse
workspace.

### 2.2 The regression's origin (SVY-18673)
In November 2023, SVY-18673 ("All fields should be editable in find mode") added a
`findmode` bypass to several TiNG components' `svyOnChanges` so that fields become
editable in Find Mode. For components with a genuine free-text input element
(`calendar`, `check`, `checkgroup`, `typeahead`'s editability), this bypass is
justified: a user might type a raw search expression (`>100`, `!2`, `%x%`) that cannot
resolve through a value list, so the server-side find-mode plumbing
(`DataproviderTypeSabloValue.getValueForToJSON()`) intentionally sends the raw value and
leaves formatting to the client.

The same bypass was copy-pasted into `combobox.ts`, but `combobox` has no free-text
input at all — it is a button that opens a dropdown of value-list items
(`combobox.html`), and the only way to change its value is
`updateValue(value.realValue)`, which always yields a value list-backed real value.
There is no code path by which `dataProviderID` could hold a raw, free-typed search
expression in Find Mode for this component. The bypass is therefore an unjustified
regression for `combobox` specifically, even though it is correct for the other
components SVY-18673 touched (which are unaffected by this fix and must remain
untouched).

The identical bypass, from the same commit family (`be743c2`, same ticket), was
copy-pasted into the Bootstrap `combobox.ts` for the same unjustified reason.

### 2.3 A second, unrelated over-broad guard (SVY-19974) in Bootstrap typeahead
Bootstrap `typeahead.ts`'s `inputFormatter` has a separate `!this.findmode()` guard that
did **not** come from SVY-18673. It was added later by SVY-19974, a real crash fix for a
race between an in-flight async `getDisplayValue()` server round-trip and a find-mode
valuelist-reference reset. That fix, as written, disables *all* display-value
resolution in Find Mode — including the synchronous, already-in-memory
`valuelistID.find(...)` lookup, which has no such race (no server round-trip is
involved). This over-broad guard reproduces the SVY-21357 symptom for Bootstrap
typeahead too, for any valuelist whose values are already loaded client-side (the common
case, and the one this ticket's demo project exercises).

servoydefault (TiNG) `typeahead.ts`'s `inputFormatter` has no find-mode guard at all —
it was never touched by either SVY-18673 or an equivalent of SVY-19974 — and already
behaves correctly. **No change is needed there.**

### 2.4 Server-side plumbing is not the problem
Verified in the `servoy-client` repo: `FoundSet.setFindMode()` and
`DataAdapterList.setFindMode()` only flip internal state and notify listeners; they do
not touch formatting. `DataproviderTypeSabloValue.getValueForToJSON()` deliberately
sends the raw `uiValue` to the client in Find Mode, by design, for every component type,
and leaves the display decision to the client component. This is correct and must not
change — the fix is entirely client-side, in the three files listed below.

## 3. Design

### 3.1 TiNG `combobox.ts` — remove the findmode bypass
File: `com.servoy.eclipse.ngclient.ui/node/projects/servoydefault/src/lib/combobox/combobox.ts`

Current `svyOnChanges` (lines 164–194):
```ts
svyOnChanges(changes: SimpleChanges) {
    this.valueComparator = ...;
    if (changes['dataProviderID'] && this.findmode()) {
        this.formattedValue = this.dataProviderID();
    } else if ( (changes['dataProviderID'] || changes['valuelistID']) && this.valuelistID()) {
        // ... valuelist resolution ...
    }
    else if (changes['dataProviderID'] && !this.valuelistID()) {
        this.formattedValue = this.dataProviderID();
    }
    ...
}
```

Remove the `if (changes['dataProviderID'] && this.findmode()) { ... }` branch entirely,
so that a `dataProviderID` change always falls into the `else if` valuelist-resolution
branch when a valuelist is present (regardless of find mode), and into the final
`else if (changes['dataProviderID'] && !this.valuelistID())` branch only when there is
genuinely no valuelist. This restores exact parity with NG1's `combobox.js`, which has
no find-mode special case at all.

No other logic in the method changes. The `calendar`/`check`/`checkgroup`/`typeahead`
editability bypasses added by SVY-18673 live in different files (`basefield.ts`,
`calendar.ts`, `check.ts`) and are untouched.

### 3.2 Bootstrap `combobox.ts` — remove the findmode bypass (same fix)
File (separate repo): `bootstrapcomponents/components/projects/bootstrapcomponents/src/combobox/combobox.ts`

Current `svyOnChanges` (lines 223–247) has the identical structure using signals:
```ts
svyOnChanges(changes: SimpleChanges) {
    super.svyOnChanges(changes);
    const valuelistIDValue = this.valuelistID();
    if (changes['dataProviderID'] && this.findmode()) {
        this.formattedValue.set(this._dataProviderID());
    } else if ((changes['dataProviderID'] || changes['valuelistID']) && valuelistIDValue) {
        // ... valuelist resolution ...
    }
    ...
}
```

Remove the `if (changes['dataProviderID'] && this.findmode()) { ... }` branch entirely,
so the `else if` becomes the first (and only) condition guarding valuelist resolution.
Note this component has no third `else if (!valuelistID)` fallback branch (unlike TiNG's
combobox) — when there's no valuelist, `formattedValue` simply keeps its previous value
via the placeholder-text handling further down in the method, which is unaffected by
this change and must not be altered.

### 3.3 Bootstrap `typeahead.ts` — narrow the inputFormatter guard
File (separate repo): `bootstrapcomponents/components/projects/bootstrapcomponents/src/typeahead/typeahead.ts`

Current `inputFormatter` (lines 235–274):
```ts
inputFormatter = (result: any) => {
    if (result === this.NULL_VALUE) {
        result = null;
    }
    const valuelistID = this.valuelistID();
    if (result?.displayValue !== undefined) result = result.displayValue;
    else if (!this.findmode() && valuelistID?.hasRealValues()) {
        const value = valuelistID.find((item) => { ... });
        if (value) {
            result = value.displayValue;
        } else {
            // async getDisplayValue() round-trip fallback
            ...
        }
    }
    return this.formatService.format(result, this.format()!, false);
};
```

The `!this.findmode()` guard currently wraps the entire `else if` block, disabling both
the synchronous `.find()` lookup and the async `getDisplayValue()` fallback in Find
Mode. Narrow it so only the async fallback stays guarded, and the synchronous lookup
always runs when a valuelist with real values is present:

```ts
inputFormatter = (result: any) => {
    if (result === this.NULL_VALUE) {
        result = null;
    }
    const valuelistID = this.valuelistID();
    if (result?.displayValue !== undefined) result = result.displayValue;
    else if (valuelistID?.hasRealValues()) {
        const value = valuelistID.find((item) => {
            if (item.realValue == result) {
                return true;
            }
            if (item.realValue instanceof Date && result instanceof Date) {
                return item.realValue.getTime() === result.getTime();
            }
            return false;
        });
        if (value) {
            result = value.displayValue;
        } else if (!this.findmode()) {
            // async getDisplayValue() round-trip — keep the SVY-19974 race guard here only
            let display = this.realToDisplay.get(result);
            if (display === null || display === undefined) {
                valuelistID.getDisplayValue(result).subscribe(val => {
                    if (val) {
                        this.realToDisplay.set(result, val);
                        if (result == this._dataProviderID()) this.instance()!.writeValue(result);
                    }
                });
                display = this.realToDisplay.get(result);
                if (display === null || display === undefined) return '';
                else result = display;
            } else {
                result = display;
            }
        } else {
            return '';
        }
    }
    return this.formatService.format(result, this.format()!, false);
};
```

Net effect: the top-level condition drops `!this.findmode()`; the removed guard is
reinserted one level deeper, in front of only the async `getDisplayValue()` branch
(replacing its implicit `else`). When the synchronous lookup finds no match and find
mode is active, the async round-trip is skipped and `''` is returned, preserving the
SVY-19974 fix for the actual race it targeted (DB-backed valuelists needing a server
round-trip) while fixing the common case (value already present in the loaded
valuelist) reported in this ticket.

No other line in `inputFormatter`, and no other method in the file, changes.

### 3.4 Git history
Documented in the triage report (`docs/SVY-21357-triage.md`), carried forward here:
- `092542a5327` / `e68e95c9c5` / `58f50cf4d9` (servoy-eclipse, SVY-18673, Nov 2023):
  introduced the TiNG `combobox.ts` findmode bypass.
- `be743c2` (bootstrapcomponents, SVY-18673, same ticket): introduced the identical
  Bootstrap `combobox.ts` bypass.
- `0221bfa` (bootstrapcomponents, SVY-19974, Feb 2025): introduced the over-broad
  `!this.findmode()` guard in Bootstrap `typeahead.ts`'s `inputFormatter`, fixing a real
  crash but with an unnecessarily wide blast radius.

## 4. Implementation plan

**Repo 1 — `servoy-eclipse`** (Eclipse workspace project `com.servoy.eclipse.ngclient.ui`):
1. Edit `com.servoy.eclipse.ngclient.ui/node/projects/servoydefault/src/lib/combobox/combobox.ts`:
   remove the `if (changes['dataProviderID'] && this.findmode()) { this.formattedValue = this.dataProviderID(); }`
   branch from `svyOnChanges`.
2. Validate per the project's `AGENTS.md` workflow: `npx tsc --noEmit -p src/tsconfig.app.json`
   (or the servoydefault library equivalent), `npm run lint`, then a build.
3. Add/update a unit test for `ServoyDefaultCombobox` (`combobox.component.spec.ts` or
   equivalent) covering: dataProviderID change while `findmode()` is true and a matching
   valuelist entry exists → `formattedValue` resolves to the display value, not the raw
   dataprovider value.
4. Commit in the `servoy-eclipse` git repo (Eclipse `eclipse-git` tools), subject line
   including `SVY-21357` and ending in `[ai]`.

**Repo 2 — `bootstrapcomponents`** (standalone repo at `D:\GitSourcesComponents\bootstrapcomponents`):
5. Edit `components/projects/bootstrapcomponents/src/combobox/combobox.ts`: remove the
   `if (changes['dataProviderID'] && this.findmode()) { this.formattedValue.set(this._dataProviderID()); }`
   branch from `svyOnChanges`.
6. Edit `components/projects/bootstrapcomponents/src/typeahead/typeahead.ts`: narrow the
   `inputFormatter` guard as described in section 3.3 — drop `!this.findmode()` from the
   outer `else if`, keep it only in front of the async `getDisplayValue()` fallback, and
   return `''` when find mode is active and no synchronous match is found.
7. Add/update unit tests in this repo for both components covering the find-mode +
   valuelist-resolved-display-value scenario (combobox: dataProviderID change in find
   mode resolves via valuelist; typeahead: `inputFormatter` in find mode resolves a value
   present in the loaded valuelist via `.find()`, and does not attempt the async
   `getDisplayValue()` round-trip when no match is found in find mode).
8. Run this repo's own lint/build/test commands (its own `package.json` scripts — not
   the `servoy-eclipse` Angular sub-project commands).
9. Commit in the `bootstrapcomponents` git repo separately from repo 1's commit, subject
   line including `SVY-21357` and ending in `[ai]`.

## 5. Acceptance criteria
- [ ] TiNG combobox: in Find Mode, a `dataProviderID` value that matches a value-list
      entry displays that entry's display value, not the raw dataprovider value.
- [ ] Bootstrap combobox: same behavior as above, for the Bootstrap component.
- [ ] Bootstrap typeahead: in Find Mode, `inputFormatter` resolves a value already
      present in the loaded valuelist (synchronous `.find()`) to its display value.
- [ ] Bootstrap typeahead: the SVY-19974 fix remains intact — when the synchronous
      lookup finds no match in Find Mode, the async `getDisplayValue()` round-trip is
      still skipped (no crash, no race), and `inputFormatter` returns `''`.
- [ ] servoydefault (TiNG) typeahead is unchanged — confirmed no code edit was made to
      it.
- [ ] The SVY-18673 editability behavior for `calendar`, `check`, `checkgroup`, and
      basefield remains unaffected — no edits are made outside the three files listed in
      section 3.
- [ ] Both repositories build, lint (zero warnings for the Angular sub-project rule),
      and pass their existing/new unit tests.
- [ ] Two separate commits exist, one per repository, each following that repository's
      commit message convention (`SVY-21357 ... [ai]`).

## 6. Out of scope
- Any change to server-side find-mode plumbing (`FoundSet`, `DataAdapterList`,
  `DataproviderTypeSabloValue`) in the `servoy-client` repository — verified not to be
  the source of the regression.
- Any change to `basefield.ts`, `calendar.ts`, `check.ts`, or servoydefault (TiNG)
  `typeahead.ts` — these either implement legitimate find-mode editability (unrelated to
  display-value formatting) or are already correct.
- Introducing a fallback/merge strategy (triage's "Approach 2") instead of removing the
  bypass outright — rejected as unnecessary complexity for components that structurally
  cannot receive free-typed input in Find Mode.
- Any change to the `bootstrapcomponents` package version number or release process.

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| Does the `bootstrapcomponents` repo have its own CI pipeline that must be triggered/verified separately from `servoy-eclipse` CI? | dev | open |
| Should a regression test be added at the `VLFindMode.servoy` demo-project level (functional/e2e), in addition to unit tests? | dev | open |
</content>
</invoke>

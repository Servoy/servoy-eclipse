# SVY-21299 — Peer Review Summary

**Issue:** SVY-21299 "Remove strict: false from tsconfig compilerOptions and fix all type errors" · Task · Minor · NGClient · fix version 2026.9.0
**Reviewed scope:** three sequential commits on `release` (servoy-eclipse), author Gabi Boros, Jul 28–31 2026:
`05fd3a1f00`, `a6e8005ce1`, `87bcdcad493007d523525c989b1e5971ca6765c7`

## Risk verdict

**LOW.** A mechanical TypeScript strict-mode sweep across the two Angular workspaces
(`com.servoy.eclipse.ngclient.ui/node` runtime, `com.servoy.eclipse.designer.rfb/node`
designer) plus a tsconfig-only touch to `designer.wpm`: strict flags enabled, then the
annotations needed to compile added (`!`, `?.`, `??`, `| null`/`| undefined`, explicit
param types). No logic change, no dependency change (no `package.json`/lockfile touched),
no auth/validation/sanitization change — **security relevance NONE**, blast radius
**contained**. The overwhelming majority of hunks are type-only and emit identical
JavaScript.

The one flagged non-equivalent edit (M1 below) was verified against the latest `release`
HEAD and found behaviourally equivalent. The only residual soft spot is that ~1600 new
runtime non-null assumptions landed with **no tests added** and only `tsc` as the gate — a
wrong `!`/`null!` would surface as a latent `TypeError`, not a build failure (same failure
mode as the pre-change code, which would also have thrown).

### M1 — foundset `delete X` → `X = undefined!` — verified equivalent

In `src/ngclient/converters/foundset_converter.ts`, three `delete selectionUpdateDefer`
statements became `= undefined!` (current HEAD lines 227, 438/449, 596). In every spot the
write is immediately overwritten (438 → 441) or read only through a truthiness/identity
guard (line 435 `if (...selectionUpdateDefer)`, line 225 `defer === ...selectionUpdateDefer`),
none of which can distinguish "key absent" from "key present = undefined". The symbol is
file-local only (10 matches, whole `node/` workspace searched); no external
`in`/`hasOwnProperty`/`Object.keys` consumer exists. Foundset selection round-trips behave
exactly as before.

## Manual test plan

**Automated gate (run first, per affected workspace):**
1. `com.servoy.eclipse.ngclient.ui/node`: `npx tsc --noEmit -p src/tsconfig.app.json`; `npm run lint` (zero warnings); build libs then `npm run test_public` / `test_default` / `test_dialogs` / `test_window` / `test_ngclientutils`.
2. `com.servoy.eclipse.designer.rfb/node`: `npm run lint`; `npm test` (Vitest) — design-time only.
3. `com.servoy.eclipse.designer.wpm/node`: tsconfig-only — a `tsc`/lint pass is sufficient.

**Runtime regression smoke checks (NG Client, cheap insurance — no behavioural change expected):**
1. **Foundset selection:** open a form with a grid/table backed by a foundset; select rows, change selection, page/scroll; confirm selection round-trips with no console error.
2. **Dialogs:** trigger error / info / question / warning and an input dialog; confirm each opens with correct OK/Cancel buttons and labels. If feasible, test a locale where `servoy.button.ok`/`servoy.button.cancel` are absent (exercises the `?? ''` path).
3. **Calendar:** open a calendar field, pick a date, clear it; confirm open/close and value set/clear.
4. **Popup menus:** open a popup/context menu, click an item, dismiss; confirm no null-deref on the menu DOM path.

**Design-time (lower priority):** a short Form Designer session (drag/drop, select, resize) smoke-tests the ~813 designer.rfb `!` DOM assertions — they throw visibly at design time if wrong, nothing ships.

## Possible improvements / follow-ups (non-blocking)

- No tests were added for the ~1600 new runtime non-null assumptions; `tsc --noEmit` is the only gate exercised so far. Consider confirming the per-library Vitest suites pass against the strict build.
- The three commit subjects omit the `SVY-21299` key (they carry only `[ai]`), which breaks `git log --grep`/release-notes tracing. Commits are already pushed and cannot be altered — record the three shas explicitly in any merge-forward/release note. (Accepted as annoying-but-unfixable.)
- On `release`: this ~250-file sweep is a high merge-conflict surface for later `lts_*` forward-merges — forward-merge the three commits as a block, soon.
- The four `com.servoy.eclipse.knowledgebase(.mcp)` `.project` files (incl. two `bin/.project`) committed in `05fd3a1f00` were reviewed and accepted as deliberate cleanup.

---
*Peer review via case-review skill: change narrative, regression/blast-radius, security, and manual test plan. Reviewed scope: servoy-eclipse `release` commits 05fd3a1f00, a6e8005ce1, 87bcdcad493007d523525c989b1e5971ca6765c7.*

# SVY-21648 — BaseTabpanel signal writes during change detection

Handoff brief for applying this fix on the **release** branch.

Target: `com.servoy.eclipse.ngclient.ui/node/projects/servoydefault/src/lib/` in the Servoy
Developer Eclipse repo. Jira: SVY-21648 (Task, Minor, fixVersion 2026.9.0). Follow-up from the
zoneless migration (SVY-20855); the regression came in with commit `ba083f5ea9`.

Verify the hazards still exist on release before changing anything — `ba083f5ea9` may or may not
be on that branch, and the line numbers below are from master.

## Hazard A — `tabIndex` write-back lands inside the change-detection pass

`basetabpanel.ts` `setFormVisible()` calls `this.tabIndex.set(this.getTabIndex(tab))`. `tabIndex`
is a `model()`, so this emits `tabIndexChange`. `ComponentTemplateGenerator.java` (~line 204-222,
in `com.servoy.eclipse.ngclient.ui/src/.../ComponentTemplateGenerator.java`) emits
`(tabIndexChange)="callback.datachange(state,'tabIndex',$event)"` for every pushToServer
property, so that write mutates the parent's `state.model.tabIndex` synchronously — inside the
same pass that delivered the `tabIndex` input. `checkNoChanges` flags it as NG0100.

`ba083f5ea9` removed the `Promise.resolve().then()` that used to keep this out of the pass.

Reached from `svyOnChanges`: the `changes['tabIndex']` branch calls `select()` synchronously, and
the `changes['tabs']` branch calls `setFormVisible()` directly. Note `select()` only reaches
`setFormVisible()` synchronously on the *first* selection (its `else` branch); once a tab is
selected it goes through the async `hideForm()` promise, so the exposure is first-selection and
the `tabs` branch.

## Hazard B — signal write from a template read path

`getRealTabIndex()` calls `this.tabIndex.set(i + 1)` while resolving a tab *name* to an index. It
is reached from `getSelectedTabId()`, and `accordion.html` line 3 reads that on every pass:
`[collapsed]="getSelectedTabId() !== tab._id"`. On first render `svyOnChanges` has not run yet
(`basecomponent.ts` `ngOnChanges` stashes the changes until `elementRef` resolves), so
`selectedTab()` is undefined and the name branch runs *during rendering* — NG0600, plus an
unintended server push mid-render.

Only the accordion reaches B. Tabpanel reads `selectedTabID()` and tablesspanel reads
`getForm()`, so they're clear of it. All three are exposed to A.

## Constraint: no behavior change

The server must still receive the same `tabIndex` push with the same value. Do **not** suppress
the echo by comparing before setting — only change *when* the write happens.

## Fix 1 — make `getRealTabIndex()` pure

Delete the `this.tabIndex.set(i + 1);` line from the name-resolution loop; return `i` only. No
caching needed — `setFormVisible()` already sets the authoritative numeric value once a tab is
selected, so the end state is unchanged. What disappears is an intermediate push during render.

```ts
    /**
     * Pure resolve of the configured tabIndex (a 1-based number or a tab name) to a
     * 0-based index into tabs(). Must NOT write the tabIndex signal: it is reached from
     * getSelectedTabId(), which the accordion template reads on every change detection
     * pass, and writing a model() signal during rendering is an NG0600 and also pushes
     * the value to the server mid-render. setFormVisible() is what normalizes tabIndex
     * to its numeric form once a tab is actually selected.
     */
    protected getRealTabIndex(): number {
        if (this.tabIndex()) {
            if (isNaN(this.tabIndex())) {
                if (!this.tabs()) return -1;
                for (let i = 0; i < this.tabs().length; i++) {
                    if (this.tabs()[i].name === this.tabIndex()) {
                        return i;
                    }
                }
                return -1;
            }
            return this.tabIndex() - 1;
        }
        if (this.tabs() && this.tabs().length > 0) return 0;
        return -1;
    }
```

## Fix 2 — defer only the `tabIndex` write in `setFormVisible()`

Keep `select()` synchronous in `svyOnChanges` — do not restore a blanket `Promise.resolve()`
around it. `ba083f5ea9` removed that for a reason (the old deferral let `getForm()` run before any
tab was selected, which is the NG0100 it was fixing).

**A microtask is not sufficient.** zone.js is still loaded as the zoneless-migration safety net
and drains the microtask queue synchronously inside change detection, so
`Promise.resolve().then()` still lands in the same pass and the NG0100 persists. This was
confirmed empirically — it must be a macrotask.

```ts
        const oldSelected = this.selectedTab();
        this.selectedTab.set(tab);
        this.selectedTabID.set(tab._id);
        // tabIndex is a model(): setting it emits tabIndexChange, which the generated form
        // template routes into callback.datachange() and so writes back into the parent's
        // model in the same change detection pass that delivered the tabIndex input.
        // Defer the write out of the pass (NG0100). A microtask is not enough: zone.js
        // drains those synchronously inside change detection, so this has to be a task.
        const newTabIndex = this.getTabIndex(tab);
        setTimeout(() => {
            if (this.selectedTab() === tab) {
                this.tabIndex.set(newTabIndex);
                this.cdRef.markForCheck();
            }
        }, 0);
```

The `selectedTab() === tab` guard only drops a write whose tab was already superseded (that value
was stale anyway). `markForCheck()` is required because the app is zoneless.

## Fix 3 — tests

Three specs: `tabpanel/tabpanel.spec.ts`, `tabpanel/tablesspanel.spec.ts`,
`accordion/accordion.spec.ts`.

Essential points, each learned the hard way:

1. **Add `provideCheckNoChangesConfig({ exhaustive: true })`** to every TestBed alongside the
   existing `provideZonelessChangeDetection()`. Without it NG0100 cannot surface in a spec at all
   — which is why `ba083f5ea9`'s existing "should not throw NG0100" test in
   `tablesspanel.spec.ts` passes while the hazard is live. This single addition is what makes the
   suite able to catch the bug.

2. **Host must drive `tabIndex` through a signal**, bound as `[tabIndex]="tabIndex()"`. Mutating a
   plain host field and calling `detectChanges()` makes exhaustive `checkNoChanges` report the
   *host's own* binding as changed, producing a confusing false NG0100 unrelated to the component.

3. **Host must wire the write-back** the way the generated template does, or the hazard isn't
   reproduced at all:

```ts
    readonly tabIndex = signal<any>(1);
    emissions: any[] = [];

    /** mirrors the generated template's callback.datachange(state,'tabIndex',$event) write-back */
    onTabIndexChange(value: any) {
        this.emissions.push(value);
        this.tabIndex.set(value);
    }
```
   bound as `(tabIndexChange)="onTabIndexChange($event)"`.

4. **Assert `emissions` synchronously right after `detectChanges()` returns** — that is the only
   thing that distinguishes a mid-render write from a correctly deferred one. Then flush and
   assert the push did still arrive:

```ts
    host.tabIndex.set('tab2');
    fixture.detectChanges();
    expect(host.emissions).toEqual([]);        // nothing written during the pass
    await flushDeferredWrites(fixture);
    expect(host.emissions).toEqual([2]);       // normalized push still happens
    expect(host.tabIndex()).toBe(2);
```

5. **`whenStable()` does not await a `setTimeout`.** Needed helper:

```ts
/** setFormVisible() defers the tabIndex write with setTimeout, which whenStable() does not await */
const flushDeferredWrites = async (fixture: ComponentFixture<unknown>) => {
    await fixture.whenStable();
    await new Promise(resolve => setTimeout(resolve, 0));
    await fixture.whenStable();
};
```

6. **The mocked `WindowRefService` needs a `console`:**
   `{ provide: WindowRefService, useValue: { nativeWindow: { event: null, console } } }`.
   `LoggerService` captures `win.console` at construction, and the tabpanel's `updateNavpane()`
   legitimately calls `log.warn()` after its 10 retries — without it you get
   `Cannot read properties of undefined (reading 'warn')` instead of a warning, and it surfaces as
   a bogus test failure.

7. `tabpanel.spec.ts` currently only covers `DefaultTabpanelActiveTabVisibilityListener` (the
   MutationObserver helper). It needs a real `ServoyDefaultTabpanel` host added.

8. Watch one expectation: with a numeric `tabIndex` that already matches the resolved selection,
   the model never changes value, so `emissions` stays `[]` even after flushing. Assert that, not
   a push.

Coverage to add per spec: numeric `tabIndex` change after init, name-based change after init,
name-based on first render, the normalized push arriving after the pass, and a loop driving
several values (`[2, 'tab1', 1, 'tab2']`).

## Verify the tests actually discriminate

Revert each hazard separately and confirm the new tests fail. On master this gave:

| State | Failing tabpanel/accordion tests |
|---|---|
| both hazards restored | 4 |
| only the `getRealTabIndex` write | 1 (accordion — the one path that reaches it) |
| only the synchronous `tabIndex.set` | 3 (tabpanel ×2, tablesspanel ×1) |
| fix in place | 0 |

## Build / test commands

From `com.servoy.eclipse.ngclient.ui/node`:

- `npx tsc --noEmit -p projects/servoydefault/tsconfig.lib.json`
- `npm run test_default`
- `npx eslint "projects/servoydefault/src/lib/tabpanel/**/*.ts" "projects/servoydefault/src/lib/accordion/**/*.ts"`

Two pre-existing failures on master, unrelated — confirm they also pre-exist on release before
worrying about them:

- `calendar.spec.ts` ×3: `tempus-dominus` rejects a `display.keyboardNavigation` option.
- `npx ng build @servoy/servoydefault` fails at "Writing package manifest" with
  `@angular/cdk must be explicitly allowed using the "allowedNonPeerDependencies" option`. Also
  fails with the changes stashed, so don't chase it.

Note `npm run lint` is broken in this project (`eslint-formatter-unix` not installed) — use
`npx eslint` with explicit paths.

## Commit convention

Subject must end with ` [ai]` and include `SVY-21648`. Suggested subject:

`SVY-21648 fix BaseTabpanel signal writes during change detection [ai]`

Ask before committing, and separately before pushing — `release` is a shared branch.

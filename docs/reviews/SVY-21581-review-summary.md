# Review Summary — SVY-21581: Context menu is not completely visible in form editor

**Risk: LOW.** The only surviving finding is a Low failure-mode difference: a throw inside the new `detectChanges()` is handled by the existing `.catch` instead of Angular's `ErrorHandler`. The fix has not been seen working in a live designer.

**Scope reviewed:** `servoy-eclipse`, commit `fe8b36fd7` on `master` (also on `origin/master`).
Production change: two `this.cdr.detectChanges()` calls in `contextmenu.component.ts`, before `adjustMenuPosition(...)` in both `hasCypressFormTest()` branches.

**Release-target check: MATCH.** Fix Version 2026.12, and `servoy-client` `origin/master` reads 2026.12.0.

## Manual test plan

**Verifying the fix**
1. Open the RFB form editor and load a form tall enough to reach the bottom of the canvas.
2. Right-click near the bottom of the form as the first right-click after the designer loads. The menu must be fully visible and flipped upward, not hidden behind the status bar.
3. Right-click again at the same position. It must look the same as the first time.

**Regression checks**
1. Right-click in the middle of the form: the menu opens at the cursor with no jump.
2. Right-click near the right edge: the menu flips left and stays clear of the palette.
3. Hover submenu items near the bottom: submenus stay visible.
4. On a form with a Cypress test, "Run Cypress Form Test" appears, also on the first right-click. Without one, it is absent.
5. With an empty clipboard, Paste is disabled on the first open.
6. Right-click on a part ghost: it behaves as before.
7. Press Esc and left-click to close the menu.

Automated checks in `com.servoy.eclipse.designer.rfb/node`: `npm test`, `npm run lint`, `npm run build_debug_nowatch`. These were not run during the review.

## Possible improvements / follow-ups
- Confirm the fix in a running designer. The unit tests stub `offsetHeight`, so they cannot prove real DOM measurement.
- Check that the "`detectChanges` before measure" ordering tests fail when the two added lines are removed.
- `show()` still calls `markForCheck()` right before `detectChanges()`. It is redundant but harmless.
- A throw in a template getter (for example `getItemClass` for Convert Component, which uses `node.parentElement!`) now happens inside the promise callback. The `.catch` re-runs `show()` and `detectChanges()`, so a second throw would be an unhandled rejection. This needs an unusual state, such as a detached selected node.
- Longer term: convert `menuItems` to signals, as `node/AGENTS.md` already lists.

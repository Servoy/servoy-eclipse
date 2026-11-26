# Triage Report — SVY-21789

**Verdict:** PROCEED (scope narrowed to Esc on form popups after debugging; see "Update")

## Update after implementation and debugging
The first version of this report recommended two fixes: close the popup on any click outside it, and add Esc handling. Debugging the sample in the browser changed that:
- **Click outside is not broken in the sample.** A log in `formPopupBodyListener` showed a click on `div.svy-body` outside the popup does close it with the existing code, and a click on an input inside the popup correctly keeps it open. The hypothesis below (a click beside a narrow main form is ignored) was not confirmed, and no click position was found where the popup fails to close. The click handling was left unchanged.
- **Esc is confirmed missing** in the browser: with the old code, Esc does nothing for a form popup.
- **Implemented:** only the Esc handling (approach 2). The click-outside rewrite (approach 1) was dropped: it needed a skip list for overlays rendered on `<body>` (combobox, typeahead and date picker lists) and fixed nothing reproducible.
- **Verified:** with a `closeOnEscape` dialog under the popup, the first Esc closes only the popup and the second closes the dialog (checked by hand and in unit tests). See `docs/SVY-21789-form-popup-close-esc-click.spec.md` for the final design and open questions.

## Reported problem
Jira: "Form popup doesn't close on Esc or click" (Minor, NGClient, fix version 2026.12, In Progress).

With the attached sample (`closePopup_.servoy`), at runtime:
- a popup created with `plugins.window.createFormPopup(forms.f2).x(100).y(100).width(400).height(500).show()` does not close on Esc.
- it does not close on a click "if the mouse position is on the right side of the dialog".
- "other dialogs in the sample also don't close on Esc".

The ticket proposes no solution. Form `main` of the sample has buttons that open a form popup (`f2`, 400x500 at 100,100), a modal dialog and a non-modal dialog (both via `application.createWindow(...)` + `win.show(forms.f2)`), a popup menu, and the plugins.dialogs info/warning/question/input/select dialogs. No `closeOnEscape` is set anywhere, and `f2` is only 234x328 while the popup is 400x500.

The reporter confirmed the Esc symptom by running the sample. The click symptom was not reproduced (see Update).

## Root-cause assessment

Three symptoms with three different code paths, all in the TiNG NG client (`com.servoy.eclipse.ngclient.ui/node/src/ngclient/services`).

### 1. Form popup never handled Esc (confirmed)
- `popupform.service.ts` (`PopupFormService`) closed a popup only through `formPopupBodyListener`, a `mouseup` listener on `document.body`. No keyboard listener existed in `popupform.service.ts`, `popupform/popupform.ts` or `popupform.html`.
- `git log -S"Escape"` on those files returns nothing. Esc support was never implemented for form popups, so this is a missing feature and not a regression.
- The only Esc handling in the client is `WindowService.handleEscapeKey` (`window.service.ts:269`). It is registered only for dialog windows with `instance.closeOnEscape` true, and it ignores Esc while `PopupStateService.isAnyPopupActive()` is true.
- At the time of triage, `PopupStateService.activatePopup`/`deactivatePopup` were not called from anywhere in this repo (only from the spec). The form popup never registered itself, so Esc and popup state were not coordinated for it.

### 2. Dialogs not closing on Esc (works as designed)
- `SvyWindow.closeOnEscape` defaults to `false` (`window.service.ts:592`). It is set only when the server calls `setCloseOnEscape`, which comes from `JSWindow.setCloseOnEscape`.
- The sample never calls it. Esc handling for dialogs was added in SVY-20377 (commit 44de45b02, July 2025) as an opt-in.
- Whether `plugins.dialogs.*` dialogs should close on Esc was not checked and is a product decision.

### 3. Click does not close the popup in some areas (not confirmed)
- `formPopupBodyListener` (`popupform.service.ts`) closes the popup only if the click target is inside `.formpopup-backdrop`, `.svy-main-window-container` (the `#mainForm` div) or `.svy-dialog`. This is why the original hypothesis expected a click outside those containers to be ignored.
- In the sample, a click on `div.svy-body` outside the popup did close it, and no failing click position was found.
- The wording "right side of the dialog" could also mean the empty part of the popup itself (400 wide vs. a 234-wide form), where not closing is correct.

## Ticket premise check
There is no proposed solution. The title says "doesn't close on Esc or click", but the symptoms have different causes: Esc on the form popup is a missing feature, Esc on dialogs is opt-in behaviour, and the click symptom could not be reproduced.

## Approaches considered
1. Fix the click-outside check so that a `mouseup` outside the popup element (`#formpopup`) closes the popup.
   - Pros: would work regardless of main form width.
   - Cons: risks closing popups when clicking elements that belong to them but render on `<body>`, such as combobox, typeahead and date picker lists, and needs care around nested popups and the `clickedComponentId` reopen trick. **Not done**: the problem was not reproduced.
2. Add an Esc `keyup` listener while a form popup is shown: register it in `showPopup`, remove it in `cancelFormPopup`, honour `doNotCloseOnClickOutside`, and register with `PopupStateService` so `WindowService.handleEscapeKey` does not also close a dialog underneath. **Done.**
   - Pros: small, local, consistent with the dialog Esc mechanism.
   - Cons: depends on the 200 ms `deactivatePopup` delay. Nested popups close as a whole chain.
3. Make dialogs close on Esc by default (change the `closeOnEscape` default or the server-side default).
   - Pros: matches the reporter's expectation.
   - Cons: a behaviour change for existing solutions, outside the scope of this bug without a product decision. **Not done.**
4. No code change.
   - Pros: dialogs not closing on Esc is opt-in behaviour.
   - Cons: the form popup Esc gap is a real user-visible issue.

## Recommendation
PROCEED with approach 2 for form popups only. Treat the dialog Esc part as "works as designed" unless the product owner decides otherwise (approach 3 stays a separate decision). Ask the reporter for the exact click position if the click symptom is still seen.

## Git history findings
- `popupform.service.ts` click handling dates from a1ad55c41 (2021, "VY-15746 [NG2] window plugin: implement formpopup"). The allowlist of containers has been unchanged since the original port.
- Esc handling for dialogs came with 44de45b02 (SVY-20377, July 2025, "Is there a way to use keyboard button ESC to close modal dialogs"). It added `PopupStateService` and `closeOnEscape`. 8dd7b0405 (SVY-20377) later raised the `deactivatePopup` timeout "for ng grids".
- Recent commits on these files (Angular 22 / signals / lazy-import refactors) do not touch the close logic. No target-platform bump is involved.
- No prior spec in `docs/` for this area was found.

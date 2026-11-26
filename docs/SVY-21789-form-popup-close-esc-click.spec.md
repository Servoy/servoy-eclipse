# Spec: SVY-21789 — Form popup doesn't close on Esc or click

## 1. Goal
A form popup shown with `plugins.window.createFormPopup(...).show()` must close when the user presses Esc, unless the popup was created with `doNotCloseOnClickOutside`. When the popup is shown over a dialog that closes on Esc, the first Esc closes only the popup and a later Esc closes the dialog. Today Esc is ignored for form popups.

The click part of the ticket was investigated and not reproduced (see 2.2), so click handling is unchanged.

## 2. Background

### 2.1 Where the code lives
All changes are in the TiNG NG client, `com.servoy.eclipse.ngclient.ui/node/src/ngclient/services/`:
- `popupform.service.ts` (`PopupFormService`): shows and closes form popups. It registers `formPopupBodyListener` as a `mouseup` listener on `document.body` 300 ms after a popup is created (`showPopup`), and removes it in `cancelFormPopup(boolean)`.
- `window.service.ts` (`WindowService.handleEscapeKey`): closes a dialog on Esc only when `instance.closeOnEscape` is true and `PopupStateService.isAnyPopupActive()` is false. It listens on `window` for `keyup` in the capture phase.
- `projects/servoy-public/src/lib/services/popup-state.service.ts` (`PopupStateService`): `activatePopup(id)` adds an id to a set, `deactivatePopup(id)` removes it after a 200 ms delay, so the `WindowService` key listener still sees the popup as active for the same Esc event.

### 2.2 Behaviour before the change
- Esc: no keyboard handling existed for form popups (never implemented, confirmed in the browser with the sample). The form popup also never registered with `PopupStateService`.
- Click outside: debugging the sample `closePopup_.servoy` with a log in `formPopupBodyListener` showed that a click on `div.svy-body` outside the popup closes it with the existing code, and a click on an input inside the popup keeps it open, as intended. No click position was found where the popup fails to close, so the click handling stays as it is. The ticket's "right side of the dialog" may refer to the empty area inside the popup (400 px wide popup, 234 px wide form), where staying open is expected.
- Dialogs: `closeOnEscape` defaults to false and the sample never sets it, so "other dialogs don't close on Esc" is the existing opt-in behaviour.

### 2.3 Git history
- `popupform.service.ts` click handling dates from a1ad55c41 (2021, VY-15746).
- Dialog Esc handling and `PopupStateService` came with 44de45b02 and 8dd7b0405 (SVY-20377, including the 200 ms delay "for ng grids").

## 3. Design

### 3.1 Esc closes the popup
- `PopupFormService` injects `PopupStateService` and uses the id `'formpopup'`.
- `formPopupKeyListener`: on `event.key === 'Escape'`, when a popup is shown and `doNotCloseOnClickOutside` is not set, call `cancelFormPopup(false)`. Nested popups close as a whole chain, like an outside click.
- Registered in `showPopup` right after the component is created: `activatePopup('formpopup')` and a capture-phase `keyup` listener on `doc.defaultView ?? window` (same event and phase as `WindowService.handleEscapeKey`). Adding the same function reference again for nested popups is a no-op.
- Removed in `cancelFormPopup(boolean)` (always, also with no popup) and in `cancelFormPopup(string)` when no popup remains: remove the listener and `deactivatePopup('formpopup')`.

### 3.2 Popup over a dialog
`activatePopup` makes `isAnyPopupActive()` true, so `WindowService.handleEscapeKey` skips the dialog while the popup is open. Because `deactivatePopup` removes the id only after 200 ms, the dialog's listener, which runs for the same Esc event, still sees the popup as active. So:
- The first Esc closes only the popup. The order in which the two `keyup` listeners were registered does not matter.
- An Esc more than 200 ms later closes the dialog (if it has `closeOnEscape`).
- With `doNotCloseOnClickOutside`, neither the popup nor the dialog closes on Esc.

### 3.3 Unchanged
- The click-outside allowlist (backdrop, `.svy-main-window-container`, `.svy-dialog`) and the `clickedComponentId`/`sequencePopup` reopen trick.
- Dialog Esc handling (`closeOnEscape`, opt-in, default false).

## 4. Implementation plan
1. `popupform.service.ts`: inject `PopupStateService`, add `formPopupKeyListener`, register it with `activatePopup` in `showPopup`, clean up in both `cancelFormPopup` branches (about 20 lines).
2. `popupform.service.spec.ts` (Vitest, 47 tests in the file):
   - Esc: closes the popup, other keys and `keydown` do nothing, `doNotCloseOnClickOutside`, listener removal, popup state activate/deactivate with the 200 ms delay, nested popups, show again after close, cleanup with no popup, `cancelFormPopup(string)`.
   - Popup over a dialog: the real `WindowService.handleEscapeKey` registered on `window` with a mocked dialog `cancel()`. Esc with no popup closes the dialog, the first Esc closes only the popup, a second Esc within 200 ms leaves the dialog and one after that closes it, listener order does not matter, `doNotCloseOnClickOutside`.
   - Existing click behaviour: a click inside an open dialog, recording `clickedComponentId`, `x` and `y`, `doNotCloseOnClickOutside`, and the real `mouseup` listener including the 300 ms registration delay and the `sequencePopup` reopen trick.

## 5. Acceptance criteria
- [x] With the sample solution, Esc closes the form popup (verified manually).
- [x] A popup with `doNotCloseOnClickOutside` ignores Esc (unit test).
- [x] Nested popups close as a chain on one Esc (unit test).
- [x] With a `closeOnEscape` dialog under the popup, the first Esc closes only the popup and the second closes the dialog (unit tests, and verified manually in the browser).
- [x] No key listener remains after the popup closes (unit test).
- [x] Click-outside behaviour and the reopen trick are unchanged (unit tests; outside click verified in the browser).

## 6. Out of scope
- Changing the click-outside logic (not reproduced). An earlier plan to close on any click outside `#formpopup` was dropped: it needed a skip list for overlays rendered on `<body>` (combobox, typeahead, date picker lists), and it fixed nothing that was reproduced.
- Dialog Esc defaults, `plugins.dialogs.*`, popup menus, server-side changes, the `PopupStateService` delay.

## 7. Verification notes
- The sample `closePopup` was used with `closeOnEscape = true` added temporarily to `onShowNonModalDialog` (a local change in the Developer workspace, not part of the repo).
- Manual test of popup over dialog: Esc closed the popup, a second Esc closed the dialog.
- Automated browser runs against the same sample were inconsistent: in one batch of 6, four runs saw no Esc effect at all and two passed; a later batch of 4 passed. The cause was not found. The code did not change between those runs.

## 8. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| The ticket's "doesn't close on a click on the right side of the dialog" was not reproduced; a click on the page outside the popup closes it. It may refer to the empty area inside the popup, where staying open is expected. | Reporter | open |
| If a popup is re-shown within 200 ms of closing (reopen trick), the pending `deactivatePopup` can leave it marked inactive, so an Esc could also close a `closeOnEscape` dialog underneath. | Developer | open |
| Why did four automated browser runs show no Esc effect? | Developer | open |

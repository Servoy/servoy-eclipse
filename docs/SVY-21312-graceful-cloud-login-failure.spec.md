# Spec: SVY-21312 — Developing blocked without svyCloud check

## 1. Goal

When the Servoy Cloud authentication service is unreachable or returns a server error (5xx / connection timeout), Servoy Developer must allow the developer to continue working in a degraded mode rather than blocking startup entirely. A non-blocking informational dialog should explain which features are unavailable, replacing the current behaviour where the IDE either loops re-prompting for credentials or refuses to proceed.

## 2. Background

### 2.1 Current login flow

The login is triggered at startup from `Activator.showLoginAndStart()` (line 449 in `com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/Activator.java`). It calls `new ServoyLoginDialog(shell).doLogin(onLogin)`.

`ServoyLoginDialog.doLogin()` (lines 93–169 in `ServoyLoginDialog.java`) works as follows:

1. Reads stored credentials from Eclipse secure storage.
2. If no stored credentials → opens the login dialog; sets `firstLogin = true`.
3. Calls the private `getLoginToken(username, password)` which POSTs to the Servoy middleware API (`SERVOY_API_BASE + "developer_auth/getAuthToken"`).
4. Handles the `LoginTokenResponse`:
   - `OK` → stores credentials + token, continues.
   - `LOGIN_ERROR` (4xx) → clears stored info, re-opens dialog.
   - `ERROR` (5xx / connection failure) → if `firstLogin`, same as LOGIN_ERROR (loops); if not `firstLogin`, passes `null` token and continues.

### 2.2 Problem

- **First-time login with cloud down:** The developer enters valid credentials, the cloud returns 5xx or is unreachable. Because `firstLogin == true`, the code clears stored credentials and re-opens the dialog, creating an infinite loop with no way to proceed.
- **Returning user with cloud down (stored credentials):** The code already falls through (passes `null` token). However, no informational message is shown explaining that cloud-dependent features are unavailable.

### 2.3 Two touch points (per architect Johan Compagner)

1. **The actual login** — user-facing dialog flow when no stored credentials exist.
2. **The quick refresh under the hood** — silent background credential validation when stored credentials exist.

## 3. Design

### 3.1 Distinguish server errors from authentication errors

In the `doLogin()` response handler, when `LoginTokenResponse.Status.ERROR` is returned (regardless of `firstLogin`):

- Do **not** clear stored credentials (the credentials may be perfectly valid; the server is just unreachable).
- Do **not** re-open the login dialog in a loop.
- Store the credentials if it was a first login (they were entered by the user and may be valid).
- Pass `null` as the login token to the `onLogin` consumer (same as the current behaviour for the non-firstLogin ERROR path).
- Notify login listeners with the username so the IDE continues startup.

### 3.2 Show a non-blocking informational dialog

When the cloud is unreachable (ERROR status), display a non-blocking `MessageDialog` (information type) on the UI thread explaining:

> **Servoy Cloud is currently unreachable**
>
> You can continue working, but the following features require a cloud connection and are temporarily unavailable:
> - AI Assistant (Servoy Pilot)
> - Cloud-based printing
> - Start page / tutorials
> - NG Desktop export (cloud build)
> - Pipeline setup
>
> Your credentials have been saved. The connection will be retried automatically.

The dialog must be non-modal (or quickly dismissible) so it does not itself become a blocker.

### 3.3 HTTP response code handling refinement

In `getLoginToken(String username, String password)` (lines 198–255):

- `200` → `Status.OK` (no change)
- `>= 500` → `Status.ERROR` (no change — server-side problem)
- Connection failure / timeout (caught in the `error != null` branch) → `Status.ERROR` (no change)
- `401`, `403` → `Status.LOGIN_ERROR` (invalid credentials — no change)
- Other 4xx → Consider treating as `ERROR` rather than `LOGIN_ERROR` if the endpoint itself is misbehaving (optional improvement).

### 3.4 Background silent refresh

When stored credentials exist and the background validation returns `ERROR`:
- Keep the stored credentials intact.
- Show a subtle status bar indication (via `ServoyLoginStatus`) that the cloud connection is degraded, rather than a popup.
- Optionally schedule a retry (e.g., every 5 minutes) until the cloud comes back online, at which point the token is refreshed silently.

### 3.5 State tracking

Add a static field to `ServoyLoginDialog`:
- `private static volatile boolean cloudReachable = true;`
- Updated on each login attempt result.
- Consumers (AI assistant, exporters, etc.) can check this before attempting cloud operations and show a targeted message if the cloud is down.

## 4. Implementation plan

1. **`ServoyLoginDialog.java`** — Modify `doLogin()` response handler (lines 146–158):
   - When `status == ERROR` and `firstLogin == true`: store credentials, pass `null` token, notify listeners, show info dialog. Do NOT loop.
   - When `status == ERROR` and `firstLogin == false`: keep current pass-through behaviour, add info log.

2. **`ServoyLoginDialog.java`** — Add `cloudReachable` static field and getter `isCloudReachable()`. Set to `false` on ERROR, `true` on OK.

3. **`ServoyLoginDialog.java`** — Add a private helper method `showCloudUnavailableDialog()` that displays the informational message on the UI thread.

4. **`ServoyLoginStatus.java`** — Update the status bar widget to reflect degraded state (e.g., show "Cloud unavailable" with a warning icon instead of the logged-in username).

5. **`Activator.showLoginAndStart()`** — After `doLogin` completes with a `null` token, still proceed with the start page / workspace initialization. The start page itself should degrade gracefully (show local content or skip).

6. **Consumers** (`OpenCodeView`, `TutorialView`, `ExportNGDesktopWizard`, `SetupPipelineDetailsPage`, etc.) — When requesting a login token via `ServoyLoginDialog.getLoginToken(onLogin)`, handle a `null` token gracefully by checking `ServoyLoginDialog.isCloudReachable()` and showing an appropriate message.

7. **Optional: Retry mechanism** — Add a scheduled executor in `ServoyLoginDialog` that retries the auth call every 5 minutes when `cloudReachable == false`. On success, update the token, set `cloudReachable = true`, and notify listeners.

## 5. Acceptance criteria

- [ ] When the Servoy Cloud middleware returns HTTP 5xx during first login, the IDE starts normally in degraded mode (no infinite loop).
- [ ] When the Servoy Cloud middleware is completely unreachable (connection timeout/refused) during first login, the IDE starts normally in degraded mode.
- [ ] A non-blocking informational dialog is shown explaining which features are unavailable.
- [ ] Credentials entered by the user are saved even when the cloud is unreachable (so they don't have to re-enter on next startup).
- [ ] When stored credentials exist and the background refresh fails due to cloud being down, the IDE continues without showing a login dialog.
- [ ] The status bar reflects the degraded cloud connection state.
- [ ] When the cloud comes back online (either via retry or next manual login), full functionality is restored seamlessly.
- [ ] Authentication errors (invalid credentials, 401/403) still behave as before: clear stored info and re-prompt.

## 6. Out of scope

- Changing the login flow for successful authentication scenarios.
- Offline mode for NG Client runtime (this is about the Developer IDE only).
- Removing the login requirement entirely — login is still required, but server unavailability is handled gracefully.
- Changes to the Servoy Cloud middleware itself.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should the retry interval be configurable via preferences? | Dev team | open |
| What specific HTTP status code does the cloud return when it is in maintenance mode vs. fully down? Need to confirm with cloud team. | Johan Compagner | open |
| Should credentials from a first login attempt (cloud down) be stored in secure storage or only in memory until validated? | Dev team | open |
| Should the degraded-mode info dialog have a "Retry now" button? | UX/Dev team | open |

# Spec: SVY-21125 — AI Spend Usage in Status Bar

## 1. Goal

Add a third entry to the `ServoyLoginStatus` popup menu that shows the team's
AI spend as a percentage of the monthly budget. When the user's team has consumed
more than 90 % of its budget, the entry includes an "(upgrade?)" prompt. Clicking
the entry opens the ServoyCloud portal — the same destination as the existing
"Go to ServoyCloud" item.

## 2. Background

### 2.1 Existing status-bar contribution

`com.servoy.eclipse.ui.ServoyLoginStatus` is an Eclipse
`WorkbenchWindowControlContribution` that renders a small icon in the workbench
status bar. Clicking the icon when the user is logged in shows a popup menu with:

1. **Go to ServoyCloud** — opens `https://admin.servoy-cloud.eu/…?loginToken=…` in the system browser.
2. **Logout** — clears saved credentials and re-opens the login dialog.

### 2.2 GenAI API key

During login, `ServoyLoginDialog` receives `svy_ai_key` from the Servoy API and
stores it as a JVM system property:

```java
System.setProperty("GENAI_API_KEY", svyAiKey);
```

This property is subsequently read by `ProviderConfigWriter.buildProviderEnvVars()`
to inject the key into the opencode child process. It is accessible to any plugin
in the same JVM without introducing an OSGi bundle dependency.

### 2.3 Usage API

The GenAI platform exposes a user-info endpoint:

```
GET https://genai.servoy-cloud.eu/user/info
Authorization: Bearer <GENAI_API_KEY>
Content-Type: application/json
```

The response contains a `teams` array. A user belongs to exactly one team, so
`teams[0]` is used. Relevant fields:

| Field        | Type   | Meaning                        |
|--------------|--------|--------------------------------|
| `spend`      | number | Amount spent this billing cycle|
| `max_budget` | number | Maximum allowed budget         |

## 3. Design

### 3.1 New menu item

Extend `showPopUp()` in `ServoyLoginStatus` to add a third menu item **between**
"Go to ServoyCloud" and "Logout":

| Condition              | Menu item text              |
|------------------------|-----------------------------|
| `GENAI_API_KEY` absent | item not shown              |
| Fetch pending / failed | `"AI usage: loading…"`      |
| spend ≤ 90 %           | `"spend: X%"`               |
| spend > 90 %           | `"spend: X% (upgrade?)"`    |

where `X = Math.round(spend / max_budget * 100)`.

Clicking the item navigates to the same ServoyCloud URL as "Go to ServoyCloud".

### 3.2 Asynchronous fetch with short-lived cache

Fetching on the UI thread would block the workbench. Instead:

* A companion class `GenaiUsageCache` holds a cached `SpendInfo` (spend, maxBudget)
  and the time it was last fetched.
* **Cache TTL**: 5 minutes. A cached value younger than 5 minutes is used as-is.
* **Trigger**: `showPopUp()` calls `GenaiUsageCache.refreshIfStale()`, which
  starts a daemon background thread if the cache is absent or older than 5 minutes.
  The method returns immediately (non-blocking).
* **Display**: The menu item shows whatever is in the cache at the moment the menu
  is built. If the cache is empty (first open after login), it shows "loading…".
  On the *next* popup open (typically seconds later), the refreshed value appears.
* **Error handling**: Any `IOException` or JSON parse error is logged via
  `ServoyLog.logError` and the cached value is left unchanged (stale-on-error
  strategy). If the key is revoked or the response is non-200, the item text
  reverts to "loading…" until a successful refresh.

### 3.3 Cache invalidation on logout

`ServoyLoginDialog.clearSavedInfo()` is called on logout. After logout the
`GENAI_API_KEY` property is no longer valid. `GenaiUsageCache` must expose a
`clear()` method; `ServoyLoginStatus`'s Logout handler calls it after
`clearSavedInfo()`.

### 3.4 JSON parsing

The existing `ServoyLoginDialog` uses `org.json.JSONObject`. The same library
is available in `com.servoy.eclipse.ui`. Use it to parse the `/user/info`
response (parse `teams[0].spend` and `teams[0].max_budget`).

If `org.json` is not accessible, fall back to a minimal manual extraction using
the existing pattern from `ProviderConfigWriter`.

### 3.5 HTTP client

Use `java.net.http.HttpClient` (available since Java 11, already used in
`ServoyLoginDialog`) with a 5-second connect+read timeout to avoid hanging.

## 4. Implementation plan

1. **Create `GenaiUsageCache.java`** in
   `com.servoy.eclipse.ui/src/com/servoy/eclipse/ui/`:
   - Inner record `SpendInfo(double spend, double maxBudget)`.
   - Static `AtomicReference<SpendInfo> cached` and `AtomicLong fetchedAt`.
   - `static SpendInfo getCached()` — returns the cached value, or `null`.
   - `static void refreshIfStale()` — if the cache is absent or older than
     5 minutes, spawns a daemon thread that calls `/user/info` and updates
     `cached` + `fetchedAt`. No-op if the `GENAI_API_KEY` property is blank.
   - `static void clear()` — sets `cached` to `null` and `fetchedAt` to `0`.
   - `static String formatSpend(SpendInfo info)` — returns `"spend: X%"` or
     `"spend: X% (upgrade?)"`.
   - All HTTP and JSON logic lives here; `ServoyLoginStatus` only calls these
     three public methods.

2. **Modify `ServoyLoginStatus.showPopUp()`**:
   - Call `GenaiUsageCache.refreshIfStale()` at the top of the method.
   - After the "Go to ServoyCloud" item, add the spend `MenuItem`:
     - Only add it when `System.getProperty("GENAI_API_KEY")` is non-blank.
     - Text: `GenaiUsageCache.getCached() != null ? GenaiUsageCache.formatSpend(...) : "AI usage: loading…"`.
     - `SelectionListener` opens the same ServoyCloud URL as the first item.

3. **Modify the Logout `SelectionListener`** in `ServoyLoginStatus.showPopUp()`:
   - After `ServoyLoginDialog.clearSavedInfo()`, call `GenaiUsageCache.clear()`.

4. **Post-edit workflow** for both files:
   - `eclipse-coder_organizeImports`
   - `eclipse-coder_formatFile`
   - `eclipse-ide_getCompilationErrors` (fix all errors)
   - `eclipse-ide_executeQuickFix` if quick fixes are available

## 5. Acceptance criteria

- [ ] When `GENAI_API_KEY` is not set, the spend menu item is absent from the popup.
- [ ] When `GENAI_API_KEY` is set and the cache is empty (first open), the item text is `"AI usage: loading…"`.
- [ ] After the background fetch completes, reopening the popup shows the formatted spend percentage.
- [ ] When spend ≤ 90 %, the item shows `"spend: X%"` (integer, no decimal).
- [ ] When spend > 90 %, the item shows `"spend: X% (upgrade?)"`.
- [ ] Clicking the spend item opens the same ServoyCloud URL as "Go to ServoyCloud".
- [ ] After logout and re-login, the cache is cleared; the item shows "loading…" again on the first popup.
- [ ] A failed HTTP call (network error, non-200) is logged but does not crash the popup.
- [ ] The UI thread is never blocked (fetch always happens on a daemon thread).

## 6. Out of scope

- Showing the absolute monetary spend or max_budget values (percentage only).
- A dedicated "upgrade plan" flow or dialog — the "upgrade?" text simply links to ServoyCloud.
- Auto-refreshing the menu item text while the menu is open.
- Showing usage for teams other than `teams[0]`.
- Any change to the opencode plugin or the opencode server startup.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should X be a rounded integer or show one decimal place (e.g. 75 vs 75.3)? | Johan | open — spec assumes integer |
| Is the "X/Y%" format in the ticket intended to show both the spend value and max_budget as raw numbers (e.g. "spend: 75/100") rather than a percentage? | Johan | open — spec uses percentage only |
| Should a cache miss result in a synchronous fetch with a short timeout rather than showing "loading…"? | Johan | open — spec uses async-only approach |

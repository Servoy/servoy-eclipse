# Spec: SVY-21122 — Get skills zip and GenAI token from Servoy Cloud login

## 1. Goal

After a successful Servoy Cloud login, extract `skill_endpoint` and `svy_ai_key`
from the login JSON response and set them as JVM system properties so the opencode
plugin can use them. The skills zip URL is constructed from a shared API base
constant and streamed directly — `SERVOY_SKILLS_ZIP` is a URL, not a file path.
The opencode server is only started after both login and an active solution are
present.

---

## 2. Background

### 2.1 Existing login HTTP call

`ServoyLoginDialog.getLoginToken(String, String)` parses the server 200 response
to extract `"token"`. The server now also returns:

| JSON field | Value |
|-----------|-------|
| `skill_endpoint` | Relative path (e.g. `"developer/downloadSkill4Servoy"`) appended to `SERVOY_API_BASE` |
| `svy_ai_key` | The `GENAI_API_KEY` for the Servoy GenAI gateway |

### 2.2 System properties consumed by the opencode plugin

- `GENAI_API_KEY` — read by `ProviderConfigWriter.buildProviderEnvVars()` and
  `OpencodeFolderCreatorJob` (via `isServoyAiConfigured()`).
- `SERVOY_SKILLS_ZIP` — read by `SkillsZipExtractor.getSkillsZipSource()`, now
  supports both HTTP/HTTPS URLs and local file paths.

### 2.3 No persistence needed

`getLoginToken()` is called on every Eclipse startup, so both properties are
always set fresh — no secure-preference storage required.

### 2.4 Deferred opencode startup

`OpencodeFolderCreatorJob` is no longer scheduled at plugin activation. It is
triggered only after all three preconditions are met: login complete, Servoy AI
configured (both system properties set), and an active solution present.

---

## 3. Design

### 3.1 `ServoyLoginDialog` — URL base constant + login events

**New constant:**
```java
public static final String SERVOY_API_BASE =
    System.getProperty("servoy.api.url", "https://middleware-prod.unifiedui.servoy-cloud.eu")
    + "/servoy-service/rest_ws/api/";
```
`CROWD_URL` is refactored to `SERVOY_API_BASE + "developer_auth/getAuthToken"`.

**Property setting in `getLoginToken(String, String)` 200-response branch:**
```java
String skillEndpoint = loginTokenJSON.optString("skill_endpoint", null);
String svyAiKey      = loginTokenJSON.optString("svy_ai_key", null);

if (svyAiKey != null && !svyAiKey.isBlank())
    System.setProperty("GENAI_API_KEY", svyAiKey);
if (skillEndpoint != null && !skillEndpoint.isBlank())
    System.setProperty("SERVOY_SKILLS_ZIP",
        SERVOY_API_BASE + skillEndpoint + "?loginToken=" + loginToken);
```

**Bug fix — stored-token path**: `getLoginToken(Consumer<String>)` previously
called `onLogin.accept(token)` for stored tokens without calling
`notifyLoginListener`. A new private static `notifyLoginListeners(String)` is
extracted and called from both paths so `loginComplete` is always set.

**Login-complete flag:**
```java
private static volatile boolean loginComplete = false;
public static boolean isLoginComplete() { return loginComplete; }
```
Set to `true` in `notifyLoginListeners()`.

**Multi-listener support:** `servoyLoginListener` single field replaced by
`CopyOnWriteArrayList<IServoyLoginListener> loginListeners`. `addLoginListener()`
adds to the list; `notifyLoginListeners()` iterates, fires all, then clears the
list (one-shot semantics — no manual deregistration needed).

### 3.2 `SkillsZipExtractor` — URL support

`SERVOY_SKILLS_ZIP` accepts either an HTTP/HTTPS URL or a local file path.

- `getSkillsZipSource()` (renamed from `getSkillsZipPath()`) returns `String`;
  returns URLs as-is, verifies existence for file paths.
- `openZipStream(String source)` opens an `InputStream` from URL or file path.
- `extractToConfigDir`, `writeOrUpdateAgentsMd`, `readZipEntry` now accept
  `InputStream` and use `ZipInputStream` instead of `ZipFile`.
- `OpencodeFolderCreatorJob` downloads bytes once via `openZipStream` and wraps
  with `ByteArrayInputStream` for each extraction call.

### 3.3 `Activator` — deferred startup

`OpencodeFolderCreatorJob` is no longer scheduled in `Activator.start()`.
Instead, `Activator.ensureServerStarting()` (idempotent) is called by
`OpenCodeView` when all preconditions are met:

```java
void ensureServerStarting() {
    if (urlOverride != null) return; // external server
    if (setupStarted) return;
    setupStarted = true;
    new OpencodeFolderCreatorJob().schedule();
}
```

### 3.4 `OpenCodeView.initUrl()` — state machine

Five ordered states, re-entered whenever conditions change:

1. **Login not complete** → show loading page; register login listener that calls `initUrl()` again.
2. **Login done, `isServoyAiConfigured()` false** → show "activate Servoy AI in Servoy Cloud" page.
3. **Dev/external-server override** → navigate to override URL.
4. **No active solution** → show "no solution" page; register project listener.
5. **All conditions met** → call `activator.ensureServerStarting()`; navigate or show loading + start URL switcher.

`onActiveSolutionAvailable()` simply calls `PlatformUI.getWorkbench().getDisplay().asyncExec(this::initUrl)`.

---

## 4. Implementation plan

1. **`ServoyLoginDialog.java`** (`com.servoy.eclipse.ui`):
   - Add `SERVOY_API_BASE` constant; refactor `CROWD_URL`.
   - Parse `skill_endpoint` + `svy_ai_key` via `optString`; set system properties using `SERVOY_API_BASE + skillEndpoint + "?loginToken=..."`.
   - Extract `notifyLoginListeners(String)` static helper; call from both `doLogin` and `getLoginToken(Consumer)`.
   - Add `loginComplete` flag + `isLoginComplete()`.
   - Replace single `servoyLoginListener` field with `CopyOnWriteArrayList`; `notifyLoginListeners` clears list after firing.

2. **`SkillsZipExtractor.java`** (`com.servoy.eclipse.opencode`):
   - Rename `getSkillsZipPath()` → `getSkillsZipSource()`.
   - Add `openZipStream(String source)`.
   - Change `extractToConfigDir`, `writeOrUpdateAgentsMd`, `readZipEntry` to `InputStream` / `ZipInputStream`.

3. **`OpencodeFolderCreatorJob.java`** (`com.servoy.eclipse.opencode`):
   Update to use `getSkillsZipSource()` + `openZipStream()` + `ByteArrayInputStream`.

4. **`Activator.java`** (`com.servoy.eclipse.opencode`):
   Remove immediate job scheduling from `start()`; add `ensureServerStarting()`.

5. **`OpenCodeView.java`** (`com.servoy.eclipse.opencode`):
   Rewrite `initUrl()` as the 5-state machine; simplify `onActiveSolutionAvailable()`.

6. **`SkillsZipExtractorTest.java`** (`com.servoy.eclipse.opencode.tests`):
   Update tests to use `InputStream` API; add URL-source tests.

---

## 5. Acceptance criteria

- [ ] After login, `System.getProperty("GENAI_API_KEY")` returns `svy_ai_key`.
- [ ] After login, `System.getProperty("SERVOY_SKILLS_ZIP")` equals `SERVOY_API_BASE + skill_endpoint + "?loginToken=..."`.
- [ ] `SkillsZipExtractor` can stream a zip from both an HTTPS URL and a local file path.
- [ ] Both properties are JVM-only — no secure-preference storage.
- [ ] If the server omits `skill_endpoint` or `svy_ai_key`, login succeeds unchanged.
- [ ] A stream failure is logged but does not prevent login or startup.
- [ ] The Servoy AI view shows the loading page while login is in progress.
- [ ] After login, if properties are not set, the view shows "activate Servoy AI in Servoy Cloud".
- [ ] Opencode is only started after login is complete AND an active solution is present.
- [ ] No compilation errors in `com.servoy.eclipse.ui` or `com.servoy.eclipse.opencode`.

---

## 6. Out of scope

- Zip freshness/caching — the URL is opened fresh on every startup.
- UI feedback during the zip stream download.

---

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| For `openZipStream` with HTTPS: `new URL(source).openStream()` is used (no custom headers). Sufficient since the token is in the query string. | Developer | resolved |
| `extractToConfigDir` overwrites `.opencode/` on every startup. Could be skipped if the zip URL hasn't changed, but overwriting is simpler for now. | Developer | open |

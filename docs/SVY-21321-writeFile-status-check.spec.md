# Spec: SVY-21321 — NGdesktopfile writeFile: check HTTP status and always fire callback

## 1. Goal

When `plugins.ngdesktopfile.writeFile` receives a non-200 HTTP response (e.g. 404 after session expiry), the client must **not** write the error page body to disk and must **always** invoke the user's callback with an error indication. This prevents silent data corruption and gives the caller a way to detect and handle the failure.

## 2. Background

### 2.1 Current behaviour

The `writeFile` flow is:
1. Server-side (`ngdesktopfile_server.js`) stores the callback/passThru, generates a media URL via `servoyApi.getMediaUrl(bytes)`, and calls the client-side `writeFileImpl`.
2. Client-side (`saveUrlToPath`) issues an Electron `net.request` to the media URL (using the session cookies), then streams the response body to a file.
3. Completion is detected when `writeSize === parseInt(response.headers['content-length'], 10)`.

When the HTTP session has expired, the server returns **HTTP 404** (Tomcat error page). Because the code never checks `response.statusCode`, there are two failure modes:
- **If the 404 response HAS a `content-length` header:** the callback fires with `path` (success), but the file contains the HTML error page — silent data corruption.
- **If the 404 response LACKS `content-length`:** `parseInt(undefined, 10)` → `NaN`, so `writeSize === NaN` is always `false`, and the callback is never invoked (hang).

### 2.2 Affected files

Both the AngularJS (NG1) and Angular (NG2) client implementations contain the same defect:

| Version | File (relative to repo root) | Lines |
|---------|------------------------------|-------|
| NG1 | `ngdesktopfile/ngdesktopfile/ngdesktopfile.js` | 262–284 |
| NG2 | `ngdesktopfile/projects/ngdesktopfile/src/lib/ngdesktopfile.service.ts` | 1050–1098 |

### 2.3 Server-side callback contract

`$scope.writeCallback(message, key)` — when `message === 'error'`, the server forwards the error to the user's callback via `record.callback('error', record.passThru)`. No server-side changes are needed.

### 2.4 Git history

- `saveUrlToPath` introduced by `vidmarian` in `576ead15` (2021-11-26, SVYX-344) — status code check was never present.
- NG2 port by `jdejong` in `0b693878` (2023-03-17) — carried the same pattern.
- `resolve()` refactor by `lvostinar` in `8495f409` (2026-03-23, SVY-20785) — no status code check added.

## 3. Design

### 3.1 Status code guard in response handler

At the top of `request.on('response', (response) => { ... })`, add a guard:

```
if (response.statusCode !== 200) {
    // Do NOT create a writer or write any data
    // Signal error to caller
    // Resolve the defer so subsequent waitForDefered calls are not blocked
    return;
}
```

The error signaling path is:
- If `syncDefer` exists: `syncDefer.resolve('error')`
- Otherwise: call server-side `writeCallback` with `['error', key]`
- Then: `defer.resolve(false); defer = null;`

This matches the existing `request.on('error')` handler pattern.

### 3.2 No changes to the success path

The existing `content-length`-based completion logic for 200 responses remains unchanged. This is the smallest fix per the approved approach (Approach 1 from the triage).

## 4. Implementation plan

1. **`ngdesktopfile/ngdesktopfile/ngdesktopfile.js`** — In `request.on('response', ...)` (line 262), add status code check before `fileSize` assignment. On non-200: call `writeCallback` with error, resolve defer, return early.

2. **`ngdesktopfile/projects/ngdesktopfile/src/lib/ngdesktopfile.service.ts`** — In `request.on('response', ...)` (line 1050), add the same status code check before the `resolve` function definition. On non-200: call the error path (matching the reject helper pattern), resolve defer, return early.

3. **Manual QA** — Test by:
   - Pointing the media URL to a non-existent path (simulating 404) and verifying the callback fires with `'error'`.
   - Verifying that a normal `writeFile` with a valid session still works correctly.
   - Verifying that the written file is NOT created/corrupted on a non-200 response.

## 5. Acceptance criteria

- [ ] `writeFile` callback fires with `'error'` as the first argument when the server returns a non-200 status code (both NG1 and NG2).
- [ ] No file is written (or an empty/partial file is not left on disk) when the server returns a non-200 status code.
- [ ] The internal defer is resolved so that subsequent `writeFile` / `waitForDefered` calls are not permanently blocked.
- [ ] Normal `writeFile` with a valid session and 200 response continues to work as before.


## 6. Out of scope

- Fixing the root cause of session expiry (deferred to SVY-21322 — static server-side files).
- Switching completion detection from `content-length` to `response.on('end')` (Approach 2/3 from triage — can be done as a follow-up for robustness).
- Adding retry logic or automatic session refresh.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should the callback receive additional error detail (e.g. the HTTP status code) beyond `'error'`? | Johan Compagner | open |
| Should a partially-written file be deleted on error, or is not creating it sufficient? | — | resolved (don't create writer at all on non-200) |

# Triage Report — SVY-21321

**Verdict:** PROCEED

## Reported problem

`plugins.ngdesktopfile.writeFile` stops working after the HTTP session expires (e.g. after hours/days of inactivity). When the session is dead, the server returns HTTP 404 for the media URL. The client-side Electron code:
1. Writes the 404 HTML error page content to the target file instead of the actual data.
2. Never fires the user's callback, leaving the caller with no way to detect the failure.

## Root-cause assessment

The bug is in the client-side `saveUrlToPath` function in both:
- **AngularJS version:** `ngdesktopfile/ngdesktopfile/ngdesktopfile.js:262-284`
- **Angular/NG2 version:** `ngdesktopfile/projects/ngdesktopfile/src/lib/ngdesktopfile.service.ts:1050-1078`

Two defects:

1. **No HTTP status code check.** The `request.on('response', ...)` handler never inspects `response.statusCode`. An HTTP 404 is a valid HTTP response (not a network error), so the `request.on('error')` handler is not triggered. The code proceeds to write whatever body it receives — in this case, the Tomcat 404 HTML page.

2. **Callback never fires when `content-length` header is missing.** Completion detection relies on `writeSize === fileSize` where `fileSize = parseInt(response.headers['content-length'], 10)`. When the 404 response lacks a `content-length` header (e.g. uses chunked transfer encoding), `parseInt(undefined, 10)` returns `NaN`. Since `writeSize === NaN` is always `false`, the callback is never invoked, the writer is never closed, and the `defer` is never resolved (blocking all subsequent `waitForDefered` calls too).

Additionally, there is a pre-existing typo in the NG2 error handler at line 1088 — it calls `'ngdesktop'` instead of `'ngdesktopfile'`, meaning even network-error callbacks would not reach the server-side handler.

## Ticket premise check

The ticket's proposed approach — check the response and always fire the callback (with a success/failure indication) — is correct and directly aligns with Johan Compagner's comment. The root cause of the *session expiring* is explicitly deferred to SVY-21322 (static server-side files). This ticket's scope is correctly limited to making the client-side code resilient to non-200 responses and ensuring the callback always fires.

## Approaches considered

1. **Check `response.statusCode`; only write on 200; always fire callback** — Add a status code check at the top of the `response` handler. On non-200: don't create the writer, call `writeCallback` with `'error'`, resolve the defer. On 200: proceed as today. Also add a `response.on('end')` handler as a safety net for the case where `content-length` is missing or doesn't match.
   - Pros: Minimal change, directly addresses both symptoms, matches Johan's guidance.
   - Cons: None significant.

2. **Replace content-length completion detection with `response.on('end')`** — Instead of comparing `writeSize === fileSize`, use the `end` event as the primary signal that all data has been received.
   - Pros: More robust for all responses (not just error cases). Handles chunked transfers correctly.
   - Cons: Slightly larger change; changes the success path behavior. The `content-length` check acts as an integrity verification that data wasn't truncated — losing this is a tradeoff.

3. **Combination of approaches 1 and 2** — Check status code AND use `end` event for completion, keeping `content-length` as an integrity check rather than sole completion trigger.
   - Pros: Most robust solution overall.
   - Cons: Larger change surface; harder to backport to LTS.

4. **No code change** — Not viable. This is a confirmed client-side bug causing data corruption and silent failures.

## Recommendation

**Approach 1** is recommended for the LTS backport (2026.3.2 LTS as requested by the reporter). It's the smallest change that fixes both symptoms:
- Add `if (response.statusCode !== 200)` guard in the response handler
- On non-200: call `writeCallback` with `'error'` + resolve defer (don't write anything)
- Fix the `'ngdesktop'` → `'ngdesktopfile'` typo in the NG2 error handler (line 1088)

For the main branch, **Approach 3** (combination) would provide the most robust long-term fix, but is not required for the immediate LTS fix.

Both `ngdesktopfile.js` (AngularJS/NG1) and `ngdesktopfile.service.ts` (Angular/NG2) need the same fix. The server-side code (`ngdesktopfile_server.js`) does not need changes — it already forwards the callback correctly when `writeCallback` is called with `'error'`.

## Git history findings

- The `saveUrlToPath` function was introduced by `vidmarian` in commit `576ead15` (2021-11-26) for SVYX-344 (certificate error fix) — switching from a different download mechanism to `net.request` with session cookies. The status code check was never added from the beginning.
- The NG2 port was done by `jdejong` in commit `0b693878` (2023-03-17) — carried over the same pattern without adding status code validation.
- The `resolve()` refactor and `fileSize === 0` handling was added by `lvostinar` in commit `8495f409` (2026-03-23) for SVY-20785 — still no status code check was added.

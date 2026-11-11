# Spec: SVY-21606 — Client doesn't refresh after build is done

## 1. Goal

When a change in SPM (the Web Package Manager / "WPM") triggers a Titanium NG client
rebuild, the browser showing the solution displays the static "building" page while the
build runs, but does not reliably load the freshly built client once the build finishes —
it stays stuck on the building page. This spec makes the building page reload itself on a
fixed interval so that, once the build is done, the reloaded request is served the freshly
built client.

## 2. Background

### 2.1 How the building page is served

- `com.servoy.eclipse.ngclient.ui/src/com/servoy/eclipse/ngclient/ui/IndexPageFilter.java`
  (around lines 101–111): on any `/index.html` request for `rfb/angular2` or `/solution/`,
  while `WebPackagesListener.isBuildRunning()` is `true`, the filter serves
  `resources/loadingclient.html` instead of the real client.
- Once `isBuildRunning()` is `false` and `dist/app/browser/index.html` exists, the same
  filter serves the real built client for that request.

So the handoff from "building" to "built client" is driven entirely by the browser
**re-requesting the page**: each re-request re-enters `IndexPageFilter`, which serves the
building page while the build runs and the real client once it is done.

### 2.2 Why the page is the only thing that can drive the reload

The building page (`loadingclient.html`) is a plain static HTML page. It has **no
websocket and no Angular/sablo client** — confirmed by inspecting the browser's Network
tab (no socket connection while the building page is shown). Because there is no live
client connection, the server has **no way to push** a reload to this page: there is no
`IWindow`/websocket endpoint and no running NG client in `getActiveDebugClients()` for that
browser tab. The page must therefore drive its own reload.

### 2.3 Prior mechanism

`loadingclient.html` previously self-refreshed with `<meta http-equiv="refresh"
content="3.5">`. The replacement keeps the same 3.5 s cadence but uses an explicit
`setTimeout(() => location.reload(), 3500)` in a `<script>` block, which is the more
reliable/standard way to reload and keeps the behaviour in one obvious place.

## 3. Design

### 3.1 Self-reloading building page

In `com.servoy.eclipse.ngclient.ui/resources/loadingclient.html`:

- Remove the `<meta http-equiv="refresh" content="3.5">` tag from `<head>`.
- Add a small `<script>` at the end of `<body>` that reloads the page after 3.5 s:

  ```html
  <script>
    // SVY-21606: this static building page has no websocket, so the server cannot push a reload to it.
    // Reload every 3.5s; once the build is done the reloaded request serves the freshly built client
    // (IndexPageFilter stops serving this page when the build is no longer running).
    setTimeout(function () { location.reload(); }, 3500);
  </script>
  ```

Each reload re-enters `IndexPageFilter`: while the build is still running it is served the
building page again (and reloads again 3.5 s later); once the build is done it is served the
freshly built client and the loop ends naturally.

### 3.2 No server-side change

`IndexPageFilter` and `WebPackagesListener` are unchanged. A server→client push was
considered and rejected (see §2.2): the building page has no connection to push to.

### 3.3 Git history

Per the triage report: the `IndexPageFilter` `isBuildRunning()` → `loadingclient.html`
branch and the `loadingclient.html` self-refresh mechanism are long-standing (only
EOL-normalization and unrelated edits in history). No dependency/target-platform version
change is involved; this is not a version regression.

## 4. Implementation plan

1. In `com.servoy.eclipse.ngclient.ui/resources/loadingclient.html`:
   - Remove `<meta http-equiv="refresh" content="3.5">` from `<head>`.
   - Add the `setTimeout(() => location.reload(), 3500)` `<script>` block at the end of
     `<body>`.

## 5. Acceptance criteria

- [ ] While a TiNG build triggered by an SPM/WPM change is running, the browser shows the
      building page.
- [ ] Once the build finishes, the building page reloads (within ~3.5 s) and the browser is
      served the freshly built client — no manual refresh needed. This reproduces and fixes
      the reported scenario.
- [ ] The reload cadence matches the previous behaviour (~3.5 s).
- [ ] No server-side (`IndexPageFilter` / `WebPackagesListener`) and no `node/` Angular
      change is required.

## 5a. Testing decision

No automated test is added. The change is a single self-reload line in a static HTML page
whose behaviour (re-request → `IndexPageFilter` serves building page vs. built client) can
only be meaningfully exercised end-to-end with a running Developer, an activated solution
and a real TiNG build — a heavyweight integration run disproportionate to a one-line HTML
change. Verification is by the manual test plan below.

### Manual test plan
1. Start Developer, activate a solution, open the solution in a browser.
2. In SPM/WPM, install or change a web package so a TiNG rebuild is triggered — the browser
   shows the building page.
3. When the build finishes, the building page reloads within ~3.5 s into the freshly built
   client, with no manual refresh. ✔ the reported bug is fixed.

## 6. Out of scope

- A server→client reload push over the websocket (rejected: the building page has no
  websocket/client connection to receive it).
- A build-status poll endpoint (not needed; a fixed-interval reload matches the prior
  behaviour and is sufficient).
- Changing the `IndexPageFilter` building-page vs. built-client serving logic.
- Changing the SPM/WPM install trigger (`InstallWebPackageHandler`).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| None. | — | — |

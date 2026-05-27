# Spec: SVY-21080 — Embed & Auto-Start Opencode in the Servoy Product

## 1. Goal

When a user opens the **Servoy AI** perspective (`com.servoy.eclipse.opencode`) in the shipped
Servoy developer product, opencode must start automatically without any prior manual installation.
The shipped Node.js runtime (already bundled for the Angular build) is reused to install and
launch opencode from within the plugin's state directory. The `BrowserEditor` is then pointed at
the running server as it does today, but now the server is managed by Eclipse instead of being
an external prerequisite.

---

## 2. Background

### 2.1 How Node.js is already shipped

`com.servoy.eclipse.ngclient.ui` defines an extension point `com.servoy.eclipse.ngclient.ui.nodejs`.
Platform-specific plugins (e.g. `com.servoy.eclipse.nodejs.win32.win32.aarch64`) contribute to this
extension point by providing a zip archive containing a full Node.js distribution.

`com.servoy.eclipse.ngclient.ui.Activator.extractNode()` picks up that contribution via
`Platform.getExtensionRegistry()`, extracts the zip into its own state location
(`.metadata/.plugins/com.servoy.eclipse.ngclient.ui/`), and exposes `nodePath` and `npmPath`
as `File` references. A `CountDownLatch` (`nodeReady`) ensures callers can wait for the
extraction to finish before using node/npm.

`Activator.createNPMCommand(File folder, List<String> args)` returns a pre-configured
`RunNPMCommand` (a `WorkspaceJob`) that uses those paths.

### 2.2 How the Angular folder is maintained

`NodeFolderCreatorJob` copies the bundled `/node` folder from the plugin JAR into a working
directory under `.metadata/.plugins/com.servoy.eclipse.ngclient.ui/target/`, then runs
`npm install` there. A `package_copy.json` sentinel is used to detect when the version shipped
in the bundle differs from the installed version, triggering a clean re-install.

### 2.3 Current `com.servoy.eclipse.opencode` state

* `OpencodePerspective` opens a `BrowserEditor` pointing at
  `http://127.0.0.1:4096/` (overridable via system property `opencode.url`).
* No `Activator`, no lifecycle management, no process management.
* Opencode must be running externally today.

---

## 3. Design

### 3.1 State directory layout

The opencode plugin manages its own state location:

```
.metadata/.plugins/com.servoy.eclipse.opencode/
  opencode/                  ← working directory (created by OpencodeFolderCreatorJob)
    package.json             ← the file shipped inside the plugin bundle
    package_copy.json        ← last-installed copy (version sentinel)
    node_modules/
      .bin/
        opencode             (Linux/macOS symlink)
        opencode.cmd         (Windows batch)
      opencode/
        ...
  .fullygenerated            ← marker written after successful npm install
```

No `src/`, `projects/`, or Angular specifics — this is a much simpler layout than the
NGClient UI because opencode is a single npm package rather than a compiled Angular workspace.

### 3.2 New class: `Activator` in `com.servoy.eclipse.opencode`

```java
package com.servoy.eclipse.opencode;

public class Activator extends Plugin {
    public static final String PLUGIN_ID = "com.servoy.eclipse.opencode";

    private static Activator instance;
    private Process opencodeProcess;          // the running opencode server
    private final CountDownLatch serverReady = new CountDownLatch(1);
    private int serverPort = 4096;            // matches DEFAULT_URL in OpencodePerspective

    @Override
    public void start(BundleContext context) throws Exception {
        instance = this;
        new OpencodeFolderCreatorJob().schedule();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        stopOpencodeProcess();
        instance = null;
    }

    public static Activator getInstance() { return instance; }

    /** Called by RunOpencodeCommand once the server is listening. */
    void serverStarted() { serverReady.countDown(); }

    /** Blocks the calling thread until the server is up, or the timeout expires. */
    public boolean waitForServer(long timeoutMs) throws InterruptedException {
        return serverReady.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public int getServerPort() { return serverPort; }

    void setOpencodeProcess(Process p) { this.opencodeProcess = p; }

    public void stopOpencodeProcess() {
        if (opencodeProcess != null && opencodeProcess.isAlive()) {
            opencodeProcess.destroy();
            opencodeProcess = null;
        }
    }
}
```

`Bundle-Activator` must be added to `META-INF/MANIFEST.MF`.

### 3.3 New class: `OpencodeFolderCreatorJob`

Analogous to `NodeFolderCreatorJob`, but much simpler because we only need to install a
single package — not copy an entire Angular source tree.

**Algorithm:**

```
1. Wait for Node.js extraction:
       com.servoy.eclipse.ngclient.ui.Activator.getInstance().waitForNodeExtraction()

2. Resolve working dir:
       stateLocation = Activator.getInstance().getStateLocation().toFile()
       opencodeDir   = new File(stateLocation, "opencode")
       markerFile    = new File(stateLocation, ".fullygenerated")
       bundlePkgUrl  = Activator.getInstance().getBundle().getEntry("/opencode/package.json")

3. Version check (same pattern as NodeFolderCreatorJob):
       sentinelFile = new File(opencodeDir, "package_copy.json")
       needsInstall = !markerFile.exists()
                   || !sentinelFile.exists()
                   || !FileUtils.readFileToString(sentinelFile, UTF-8)
                        .equals(Utils.getURLContent(bundlePkgUrl))

4. If needsInstall:
       a. delete opencodeDir (clean slate)
       b. opencodeDir.mkdirs()
       c. Copy bundle's /opencode/package.json  → opencodeDir/package.json
       d. Copy bundle's /opencode/package.json  → opencodeDir/package_copy.json
       e. markerFile.delete()

5. If needsInstall OR !markerFile.exists():
       RunNPMCommand install = ngclientActivator.createNPMCommand(
               opencodeDir, List.of("install", "--prefix", ".", "--no-save"))
       install.runCommand(monitor)
       if install.getExitCode() == 0: markerFile.createNewFile()
       else: log error and return WARNING status

6. Schedule RunOpencodeCommand (only if not already running):
       new RunOpencodeCommand(opencodeDir).schedule()
```

### 3.4 Bundle resource: `/opencode/package.json`

The plugin bundle ships a minimal `package.json` that pins the opencode version:

```json
{
  "name": "servoy-opencode-host",
  "version": "1.0.0",
  "private": true,
  "dependencies": {
    "opencode-ai": "^1.15.11"
  }
}
```

Package name is **`opencode-ai`**, version pinned at `^1.15.11` (latest as of 2026-05-27).
Bump the floor version in this file whenever a new opencode release needs to be shipped.

This file lives at `com.servoy.eclipse.opencode/opencode/package.json` in the plugin project
and is included in the JAR via `build.properties`:
```
bin.includes = ...,\
               opencode/
```

### 3.5 New class: `RunOpencodeCommand` extends `Job`

Starts the opencode server process and keeps it alive.  It mirrors `RunNPMCommand` in structure
but is a persistent (long-running) job, not a one-shot npm command.

```
nodePath  = ngclientActivator.nodePath   (accessed via package-visible getter or exposed API)
opencodeDir already has node_modules/.bin/opencode[.cmd]

Platform.OS_WIN32 → binary = new File(opencodeDir, "node_modules/.bin/opencode.cmd")
otherwise          → binary = new File(opencodeDir, "node_modules/.bin/opencode")

ProcessBuilder:
  command = [binary.getAbsolutePath()]   (or [node, opencode-main.js] if needed)
  environment: PATH prepended with nodePath.getParent()
  directory = opencodeDir
  redirectErrorStream = true
```

**Server-ready detection:**

opencode prints this line to stdout when the server is listening:
```
opencode server listening on http://127.0.0.1:4096
```
`RunOpencodeCommand` reads stdout line-by-line and calls `Activator.getInstance().serverStarted()`
when a line contains **`"opencode server listening on"`**.  The actual URL on that line is
parsed to extract the real port (for the Activator to expose via `getServerPort()`).
As a belt-and-suspenders fallback, a watchdog thread polls `http://127.0.0.1:<port>/` every
500 ms (up to 120 s) and also calls `serverStarted()` on first successful HTTP response.

**Process lifecycle:**

* Job does not complete until the process exits.
* If the process exits unexpectedly (non-zero exit code), log the error and schedule a
  restart after 5 seconds (up to 3 retries).
* `Activator.stop()` calls `stopOpencodeProcess()` which kills the process; the job then
  exits naturally.

### 3.6 Update `OpencodePerspective`

Two changes:

**a) Trigger server startup on perspective open (defensive — Activator already started the
job but the server may not be up yet):**

```java
@Override
public void createInitialLayout(IPageLayout layout) {
    layout.setEditorAreaVisible(true);
    layout.setFixed(true);

    PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();
        if (page == null) return;
        try {
            String url = System.getProperty(URL_PROPERTY, buildServerUrl());
            page.openEditor(new BrowserEditorInput(url, "Servoy AI"), BrowserEditor.EDITOR_ID);
        } catch (PartInitException e) {
            ServoyLog.logError(e);
        }
    });
}

private static String buildServerUrl() {
    Activator a = Activator.getInstance();
    int port = (a != null) ? a.getServerPort() : 4096;
    return "http://127.0.0.1:" + port + "/";
}
```

The browser editor will show a connection-refused / loading page until the server is up;
Equo Chromium will retry automatically.  If a nicer experience is desired (see §7.5), a
bundled `loading.html` can be shown first and the URL swapped once `serverReady` fires.

**b) Optional: Stop server when perspective is closed** (low priority, can defer):  
Listen to `IPerspectiveListener` and call `Activator.getInstance().stopOpencodeProcess()` when
the last window of this perspective closes.

### 3.7 MANIFEST.MF changes for `com.servoy.eclipse.opencode`

```
Bundle-Activator: com.servoy.eclipse.opencode.Activator
Bundle-ActivationPolicy: lazy

Require-Bundle: org.eclipse.ui,
 org.eclipse.core.runtime,
 com.servoy.eclipse.ui,
 com.servoy.eclipse.model,
 com.servoy.eclipse.ngclient.ui
```

`com.servoy.eclipse.ngclient.ui` is added so we can call
`com.servoy.eclipse.ngclient.ui.Activator.getInstance().createNPMCommand(...)` and
`waitForNodeExtraction()`.  It also ensures node has been extracted before we run npm.

> **Alternative:** expose a service / API from `ngclient.ui` rather than a direct package
> dependency, keeping `opencode` decoupled. This is a trade-off; the direct dependency is
> simpler and both plugins are always co-installed.

### 3.8 Build / pom.xml

`com.servoy.eclipse.opencode` currently has no `pom.xml`. To build the `opencode/` folder
into the JAR, either:

* Add a minimal `pom.xml` (Tycho `eclipse-plugin` packaging) and add the project to the
  parent reactor in the root `pom.xml`.
* OR include the project as-is if it is already managed by the feature's `build.properties`.

Check whether `com.servoy.eclipse.feature` already references `com.servoy.eclipse.opencode`
and whether the existing Tycho build picks it up.

---

## 4. File structure in the plugin project after this change

```
com.servoy.eclipse.opencode/
  META-INF/
    MANIFEST.MF            ← add Bundle-Activator + ngclient.ui dependency
  opencode/
    package.json           ← shipped dependency spec (pinned opencode version)
  src/com/servoy/eclipse/opencode/
    Activator.java          ← NEW
    OpencodeFolderCreatorJob.java  ← NEW
    RunOpencodeCommand.java        ← NEW
    OpencodePerspective.java       ← update buildServerUrl + minor tweaks
    editors/
      BrowserEditor.java
      BrowserEditorInput.java
  build.properties          ← add opencode/ to bin.includes
  plugin.xml
  pom.xml                   ← NEW (if needed for reactor)
```

---

## 5. Startup sequence end-to-end

```
Eclipse starts
  └─ com.servoy.eclipse.ngclient.ui.Activator.start()
       └─ extractNode() → schedules "extracting nodejs" Job
  └─ com.servoy.eclipse.opencode.Activator.start()
       └─ schedules OpencodeFolderCreatorJob

"extracting nodejs" Job runs
  └─ unzips node.zip into .metadata/.plugins/com.servoy.eclipse.ngclient.ui/
  └─ nodeReady.countDown()

OpencodeFolderCreatorJob runs
  └─ waitForNodeExtraction()            ← blocks until nodeReady
  └─ checks package.json version
  └─ [if outdated] npm install          ← installs opencode into state/opencode/
  └─ schedules RunOpencodeCommand

RunOpencodeCommand runs (persistent job)
  └─ starts  opencode  process
  └─ reads stdout → detects "ready" → serverReady.countDown()

User opens "Servoy AI" perspective
  └─ OpencodePerspective.createInitialLayout()
       └─ opens BrowserEditor at http://127.0.0.1:4096/
            └─ Chromium shows loading page until server ready
               (or swaps URL once serverReady fires — optional enhancement)
```

---

## 6. Error handling

| Scenario | Behaviour |
|---|---|
| Node.js plugin not installed (non-product launch from source) | `waitForNodeExtraction()` blocks indefinitely; add a timeout and log a clear error. |
| `npm install` fails (network issue, disk full) | Log to console + error log; do not start process; show error in BrowserEditor area. |
| opencode process exits unexpectedly | `RunOpencodeCommand` schedules a restart (up to 3 retries, 5 s apart); logs each restart. |
| Port 4096 already in use | opencode itself will fail to start; the retry logic catches this. For the future, consider scanning for a free port and passing `--port` to opencode. |
| Eclipse closed while opencode running | `Activator.stop()` destroys the process cleanly. |

---

## 7. Open Questions — ✅ Resolved

> All answers sourced from the opencode GitHub repository (sst/opencode, branch `dev`) and
> npmjs.com on 2026-05-27.

### 7.1 npm package name ✅
**`opencode-ai`** (not `opencode`).  
Install: `npm install opencode-ai`  
Binary placed at `node_modules/.bin/opencode` (or `opencode.cmd` on Windows).

Update `package.json` in the bundle accordingly:
```json
{
  "name": "servoy-opencode-host",
  "version": "1.0.0",
  "private": true,
  "dependencies": {
    "opencode-ai": "^1.15.11"
  }
}
```

### 7.2 Version to pin ✅
Use a **caret range `^1.15.11`** (current latest as of 2026-05-27, released today).  
This allows non-breaking minor/patch upgrades automatically on re-install, while the
`package_copy.json` sentinel still triggers a clean re-install whenever we intentionally
bump the version in the bundle.  Ship a new bundle with a higher floor version for major
upgrades that require deliberate testing.

### 7.3 Server start command ✅
Use the **`serve` subcommand** — not the bare `opencode` command (which launches the TUI)
and not `web` (which tries to open the system browser, defeating our `BrowserEditor`).

```
opencode serve --port 4096 --hostname 127.0.0.1
```

On Windows the actual invocation through node is:
```
node.exe  node_modules/opencode-ai/dist/index.js  serve  --port 4096  --hostname 127.0.0.1
```
or via the cmd shim:
```
node_modules\.bin\opencode.cmd  serve  --port 4096  --hostname 127.0.0.1
```

The `serve` command starts a **headless HTTP server** (no TUI, no browser launch).  
It loads project instances per-request via HTTP headers, so no project path is needed
at startup — the client passes context with each request.

The `--port` flag accepts an explicit port number.  When `0` is passed opencode itself
prefers 4096 then falls back to any free port, but we pass the explicit `4096` directly
so the actual port is always known before the process starts.

### 7.4 Ready signal string ✅
The `serve` command logs exactly this line to stdout when the server is listening:

```
opencode server listening on http://127.0.0.1:4096
```

(Pattern: `"opencode server listening on http://{hostname}:{port}"`)

`RunOpencodeCommand` should match on the substring **`"opencode server listening on"`** so it
is robust to hostname/port variation if the port ever changes.  The actual URL logged can
also be parsed to extract the real port (important for the port-conflict fallback case).

### 7.5 Loading UX ✅ (decision)
**Show a bundled `loading.html`** first, then redirect to the server URL once
`serverReady` fires.  Rationale: on first launch `npm install` can take 30–60 seconds;
showing a Chromium "ERR_CONNECTION_REFUSED" page for that long is a poor user experience.

Implementation:
- Bundle `resources/opencode-loading.html` in the plugin (minimal HTML with a spinner and
  "Starting Servoy AI…" message).
- `BrowserEditorInput` is opened with a `file://` URL to that HTML.
- A background thread waits on `Activator.getInstance().waitForServer(120_000)` then calls
  `Display.asyncExec(() -> editor.setUrl("http://127.0.0.1:4096/"))`.
- If `waitForServer` times out, update the page to show an error message instead.

### 7.6 Port configuration ✅ (decision)
**Always pass `--port 4096` explicitly** and keep the existing `opencode.url` system property
as a **dev/override escape hatch**:

- If `opencode.url` system property is set → open `BrowserEditor` at that URL and **skip
  launching** the managed process entirely (dev mode, external server assumed).
- If not set → launch the managed server on port 4096, open browser at
  `http://127.0.0.1:4096/`.

This preserves the existing dev workflow and keeps production behaviour deterministic.

If port 4096 is already in use, `opencode serve` will fail to start (it does **not** auto-
fallback when an explicit port is given).  The error will be visible in the Opencode Console
and the browser will remain on the loading page with an eventual timeout error.
Future work: scan for a free port and pass it dynamically.

### 7.7 Process isolation ✅ (decision: document + detect)
Two Eclipse instances on the same machine will both try port 4096 and the second will fail.

For MVP:
- Log a clear error in the Opencode Console if the process exits immediately with a non-zero
  code (port already bound).
- Show an actionable message in the browser area: *"Could not start the Servoy AI server —
  port 4096 is already in use. Close other Servoy developer instances and restart, or set
  the system property `-Dopencode.url=http://127.0.0.1:4096/` to share the existing server."*

Future improvement: detect a running instance and reuse it.

### 7.8 Console view ✅ (yes, add it)
Add a dedicated **"Servoy AI Console"** in the Eclipse Console view, registered via the
`org.eclipse.ui.console.consoleFactories` extension point (same pattern as the
"Titanium NG Build Console" in `com.servoy.eclipse.ngclient.ui`).

Both `OpencodeFolderCreatorJob` and `RunOpencodeCommand` write to this console so the user
can see npm install progress and the live opencode server output.

### 7.9 pom.xml ✅
`com.servoy.eclipse.opencode` **does need a `pom.xml`** — it is currently an untracked new
project (`?? com.servoy.eclipse.opencode/` in git status) with no Maven descriptor.

Add a minimal Tycho `eclipse-plugin` pom and register it in the parent reactor:
- Create `com.servoy.eclipse.opencode/pom.xml`
- Add `<module>com.servoy.eclipse.opencode</module>` to the root `pom.xml`
- Add the plugin to `com.servoy.eclipse.feature/feature.xml` so it is included in the product

---

## 8. Out of scope for this ticket

* Upgrading or managing multiple versions of opencode.
* Passing workspace/solution context into opencode automatically.
* Any opencode configuration UI.
* Supporting opencode on aarch64 macOS (depends on npm package supporting that arch).

---

## 9. Testing

### 9.1 Test strategy

Most of the logic is either OSGi-lifecycle glue (untestable without a running Eclipse) or
pure-Java coordination logic (fully testable in isolation). The approach:

| Layer | Testable? | How |
|---|---|---|
| `OpencodeServerState` — latch + port tracking | ✅ pure Java | Instantiate directly, no OSGi |
| `OpencodeFolderCreatorJob` — version-sentinel, file helpers | ✅ pure Java | Temp dirs, no OSGi |
| `Activator.start/stop` — OSGi lifecycle | ❌ needs OSGi | Manual / integration test |
| `RunOpencodeCommand` — npm exec, watchdog | ❌ needs real Node.js | Manual / integration test |
| `OpencodePerspective` — browser + UI thread | ❌ needs Eclipse UI | Manual |

### 9.2 Production refactoring for testability

Two minimal changes were made to enable pure-unit tests without touching the logic:

1. **`OpencodeServerState`** (new class) — extracts the `CountDownLatch`/port fields from
   `Activator` into a standalone, OSGi-free class. `Activator` now delegates all latch
   operations to it. Tests create independent `OpencodeServerState` instances.

2. **`OpencodeFolderCreatorJob` helper visibility** — the three helper methods are now
   package-visible (not `private`) so the test fragment can call them directly:
   - `static boolean needsInstall(File markerFile, File sentinelFile, String bundleContent)`
   - `static String readUrlContent(URL url) throws IOException`
   - `static void deleteDirectory(Path dir) throws IOException`

### 9.3 Test project: `com.servoy.eclipse.opencode.tests`

A new Eclipse fragment plugin (same pattern as `com.servoy.eclipse.tests` which fragments
`com.servoy.eclipse.designer`):

```
com.servoy.eclipse.opencode.tests/
  META-INF/MANIFEST.MF         Fragment-Host: com.servoy.eclipse.opencode
  build.properties
  pom.xml                      eclipse-test-plugin packaging
  src/test/java/com/servoy/eclipse/opencode/
    OpencodeServerStateTest.java
    OpencodeFolderCreatorJobTest.java
```

Being a fragment of `com.servoy.eclipse.opencode`, the test classes share the same package
(`com.servoy.eclipse.opencode`) and can access package-visible members without any reflection.

### 9.4 `OpencodeServerStateTest` — 6 tests

Covers the `CountDownLatch`-based server-ready coordination:

| Test | Verifies |
|---|---|
| `initiallyNotReady` | `isReady()` = false, `getServerPort()` = default |
| `serverStarted_signalsReadyAndSetsPort` | `isReady()` = true, port updated |
| `waitForServer_returnsTrueWhenAlreadyReady` | immediate return when latch already at 0 |
| `waitForServer_timesOutWhenNotStarted` | returns false within ≤ 80 ms |
| `waitForServer_returnsTrueWhenStartedConcurrently` | latch released by background thread |
| `serverStarted_multipleCallsAreSafe` | second `serverStarted()` doesn't throw |

### 9.5 `OpencodeFolderCreatorJobTest` — 9 tests

Covers the version-sentinel decision logic and file-system helpers using `@Rule TemporaryFolder`:

**`needsInstall`:**

| Test | Scenario | Expected |
|---|---|---|
| `needsInstall_markerAbsent_returnsTrue` | `.fullygenerated` missing | `true` |
| `needsInstall_sentinelAbsent_returnsTrue` | `package_copy.json` missing | `true` |
| `needsInstall_contentMatches_returnsFalse` | sentinel == bundle | `false` |
| `needsInstall_contentDiffers_returnsTrue` | sentinel ≠ bundle | `true` |
| `needsInstall_bothPresent_emptyBundle_returnsFalse` | empty == empty | `false` |

**`readUrlContent`:**

| Test | Scenario |
|---|---|
| `readUrlContent_readsFileUrl` | round-trip via `file://` URL |
| `readUrlContent_handlesUnicode` | UTF-8 multibyte characters preserved |

**`deleteDirectory`:**

| Test | Scenario |
|---|---|
| `deleteDirectory_removesNestedStructure` | nested dirs + files all gone |
| `deleteDirectory_emptyDir` | empty directory removed |

### 9.6 Running the tests

**In Eclipse IDE:**
1. Import `com.servoy.eclipse.opencode.tests` into the workspace.
2. Right-click → *Run As → JUnit Test* (standard JUnit 4, no plug-in runner needed because
   no OSGi APIs are called — the fragment just provides package access).

**Via Maven (Tycho):**
```bash
mvn clean verify -pl com.servoy.eclipse.opencode.tests
```
(Requires the module to be added to the root `pom.xml`; currently it mirrors the pattern of
`com.servoy.eclipse.tests` which is also not in the default reactor.)

### 9.7 What is not covered (manual / future)

* `Activator.start()` scheduling `OpencodeFolderCreatorJob` → verify via manual launch
* Full npm install end-to-end → verified by running Servoy Developer and opening the
  Servoy AI perspective for the first time
* Server-ready URL switch in `OpencodePerspective` → verified manually in the running IDE
* Retry logic in `RunOpencodeCommand` → tested by temporarily making opencode exit immediately

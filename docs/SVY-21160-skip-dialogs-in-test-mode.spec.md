# Spec: SVY-21160 â Skip blocking dialogs and unnecessary npm builds in test mode

## 1. Goal

When Servoy Developer is launched in test mode (PDE JUnit plugin tests), the main thread hangs because the ServoyLoginDialog opens a modal event loop. Additionally, the startup page dialog and Node.js npm build attempts are pointless in headless test environments. This change ensures that login, the startup page, and npm builds are gracefully skipped when running under the PDE test harness.

## 2. Background

### 2.1 The hang

The PDE test runner uses `E4Testable` to drive the workbench event loop. During startup, `com.servoy.eclipse.ui.Activator.showLoginAndStart()` is invoked asynchronously on the display thread. This method creates a `ServoyLoginDialog` and calls `doLogin()`, which may call `open()` â opening a modal `TitleAreaDialog` that starts its own nested event loop (`Window.runEventLoop()`). In a test environment with no stored credentials, this dialog blocks the UI thread indefinitely.

Stack trace from the issue:
```
at org.eclipse.jface.window.Window.runEventLoop(Window.java:820)
at org.eclipse.jface.window.Window.open(Window.java:799)
at com.servoy.eclipse.ui.dialogs.ServoyLoginDialog.doLogin(ServoyLoginDialog.java:114)
at com.servoy.eclipse.ui.Activator$2.run(Activator.java:500)
```

### 2.2 The startup page

After login completes, `showLoginAndStart()` also opens a `BrowserDialog` for the tutorials/start page. This is similarly meaningless in test mode and can also block.

### 2.3 Node.js extraction and npm commands

`com.servoy.eclipse.ngclient.ui.Activator.extractNode()` looks for the `nodejs` extension point. In a minimal test target platform, the Node.js platform bundle (e.g. `com.servoy.eclipse.nodejs.win32`) may not be present. When this happens:
- `nodePath` and `npmPath` remain `null` after extraction
- The latch is still counted down (the extraction job runs with `cf.length == 0`)
- `createNPMCommand()` then creates a `RunNPMCommand` with null paths, which will fail or produce confusing errors

The ticket requests that when Node is not available, `createNPMCommand()` should return a no-op job and log a warning, rather than attempting to run an npm command with null paths.

### 2.4 Relevant code locations

| File | Line | Description |
|------|------|-------------|
| `com.servoy.eclipse.ui/Activator.java` | 449 | `showLoginAndStart()` â triggers login + start page |
| `com.servoy.eclipse.ui/dialogs/ServoyLoginDialog.java` | 93 | `doLogin()` â opens modal dialog |
| `com.servoy.eclipse.ngclient.ui/Activator.java` | 129 | `extractNode()` â extracts Node from extension registry |
| `com.servoy.eclipse.ngclient.ui/Activator.java` | 252 | `createNPMCommand()` â creates npm WorkspaceJob |

## 3. Design

### 3.1 Test mode detection

Add a utility method `ModelUtils.isTestRunning()` in `com.servoy.eclipse.model` that detects whether the Eclipse instance is running under a test harness. Detection strategy:

- Check if the `org.eclipse.pde.junit.runtime` bundle is resolved/active via `Platform.getBundle("org.eclipse.pde.junit.runtime")`. This covers PDE JUnit plugin tests launched from the IDE.
- Check if the `org.eclipse.tycho.surefire.osgibooter` bundle is present via `Platform.getBundle("org.eclipse.tycho.surefire.osgibooter")`. This covers Tycho/Maven test runs (e.g. `mvn verify`), which use their own test bootstrapper instead of PDE's runtime.
- If either bundle is present, the method returns `true` and logs which mode was detected for diagnostics.

This is secure against bypass because:
- A simple system property (like `eclipse.pde.launch`) could be trivially set by end users to skip login, which is unacceptable since login is mandatory in production.
- The PDE JUnit runtime bundle is only present when the PDE test harness is actually installed and running. It is not part of the Servoy Developer product and cannot be injected by users without modifying the installation.
- The Tycho surefire osgibooter bundle is only present when running under Tycho's Maven test harness. It is not part of the Servoy Developer product.

This method belongs in `ModelUtils` because that class is in the `com.servoy.eclipse.model` bundle, which is already a dependency of both `com.servoy.eclipse.ui` and `com.servoy.eclipse.ngclient.ui`.

### 3.2 Skip login dialog in test mode

In `com.servoy.eclipse.ui.Activator.showLoginAndStart()`:
- At the top of the method, check `ModelUtils.isTestRunning()`
- If true, log an info message ("Skipping login dialog in test mode") and call `ServoyLoginDialog.notifyLoginListeners(null)` so that any listeners get unblocked, then return without showing the dialog or the start page

### 3.3 Skip startup page in test mode

This is implicitly handled by skipping `showLoginAndStart()` entirely (section 3.2). The startup page is opened inside the `onLogin` callback, so if login is skipped, the page is never shown.

### 3.4 Graceful handling of missing Node.js in createNPMCommand

**Observation:** All callers of `createNPMCommand()` use `RunNPMCommand`-specific methods (`runCommand(monitor)` and `getExitCode()`). Nobody calls `.schedule()` on the result.

**Design:** Introduce an `IRunNPMCommand` interface that declares the methods callers actually use, then provide a no-op implementation for when Node.js is unavailable.

1. **`IRunNPMCommand` interface** (new, in `com.servoy.eclipse.ngclient.ui`):
   ```java
   public interface IRunNPMCommand {
       void runCommand(IProgressMonitor monitor) throws IOException, InterruptedException;
       int getExitCode();
       void setUser(boolean b);
       void schedule();
       void join() throws InterruptedException;
       void setExtraEnvironment(Map<String, String> unmodifiableMap);
       Process getProcess();
       boolean cancel();
   }
   ```
   The interface includes `setUser`, `schedule`, `join`, `setExtraEnvironment`, `getProcess`, and `cancel` because an external project (Servoy-Copilot) uses the returned object as a `Job` and calls these methods.

2. **`RunNPMCommand`** implements `IRunNPMCommand` (no behavioral change — these methods already exist on `WorkspaceJob`/`RunNPMCommand`).

3. **`NoOpNPMCommand`** (new, package-private) implements `IRunNPMCommand`:
   - `runCommand()` logs a warning ("Node.js is not available, skipping npm command: [args]") and returns immediately.
   - `getExitCode()` returns `0`.
   - `setUser()`, `schedule()`, `join()`, `setExtraEnvironment()` are no-ops.
   - `getProcess()` returns `null`.
   - `cancel()` returns `true`.

4. **`Activator.createNPMCommand()`**: change return type from `RunNPMCommand` to `IRunNPMCommand`. After `waitForNodeExtraction()`, check if `nodePath == null`. If so, return a `NoOpNPMCommand`. No extra boolean needed — the null check on the already-existing `nodePath` field is sufficient.

5. **`extractNode()`**: if `cf.length == 0` (no nodejs extension found), log a warning: "No Node.js plugin found, npm commands will be unavailable". The latch still counts down as before.

6. **`Activator.countDown()`**: Add a guard so the `WebPackagesListener` is only registered when Node.js is actually available. Currently the code registers the listener as soon as the latch reaches 0, which implies "node is ready" — but if node extraction failed (nodePath is null), registering the listener is pointless since all npm commands would be no-ops anyway. Add a `nodePath != null` check before registering:
   ```java
   if (nodeReady.getCount() == 0 && nodePath != null && ServoyModelFinder.getServoyModel() != null && ...)
   ```

7. **Callers** (`WebPackagesListener`, `NodeFolderCreatorJob`): change their local variable type from `RunNPMCommand` to `IRunNPMCommand`. No other changes needed since they only use `runCommand()` and `getExitCode()`.

## 4. Implementation plan

1. **`com.servoy.eclipse.model/src/.../util/ModelUtils.java`** — Add `public static boolean isTestRunning()` method that checks for both `org.eclipse.pde.junit.runtime` (PDE tests) and `org.eclipse.tycho.surefire.osgibooter` (Tycho/Maven tests). Logs which mode was detected.

2. **`com.servoy.eclipse.ngclient.ui/src/.../IRunNPMCommand.java`** (new) — Interface declaring `runCommand(IProgressMonitor)` and `getExitCode()`.

3. **`com.servoy.eclipse.ngclient.ui/src/.../RunNPMCommand.java`** — Add `implements IRunNPMCommand` to the class declaration. No other changes needed (methods already exist).

4. **`com.servoy.eclipse.ngclient.ui/src/.../NoOpNPMCommand.java`** (new, package-private) — Implements `IRunNPMCommand`. `runCommand()` logs a warning and returns. `getExitCode()` returns `0`.

5. **`com.servoy.eclipse.ngclient.ui/src/.../Activator.java`** —
   - In `extractNode()`: if `cf.length == 0`, log a warning.
   - Change `createNPMCommand()` return type to `IRunNPMCommand`. After `waitForNodeExtraction()`, if `nodePath == null`, return `new NoOpNPMCommand(commandArguments)`.

6. **`com.servoy.eclipse.ngclient.ui/src/.../WebPackagesListener.java`** — In `countDown()`, add a `nodePath != null` check so the `WebPackagesListener` is not registered when Node.js is unavailable. Change local variable types from `RunNPMCommand` to `IRunNPMCommand` at all 6 call sites.

7. **`com.servoy.eclipse.ngclient.ui/src/.../NodeFolderCreatorJob.java`** — Change local variable type from `RunNPMCommand` to `IRunNPMCommand` at both call sites.

8. **`com.servoy.eclipse.ui/src/.../Activator.java`** — In `showLoginAndStart()`, add a guard at the top:
   ```java
   if (ModelUtils.isTestRunning()) {
       ServoyLog.logInfo("Skipping login and start page in test mode");
       ServoyLoginDialog.notifyLoginListeners(null);
       return;
   }
   ```

9. **Verify** that `ServoyLoginDialog.notifyLoginListeners(null)` is accessible (make package-private or add a public `skipLogin()` method if needed).

## 5. Acceptance criteria

- [ ] PDE JUnit plugin tests no longer hang on the login dialog
- [ ] The startup page (BrowserDialog) is not shown in test mode
- [ ] When the Node.js plugin is not present, `createNPMCommand()` returns a no-op job that completes immediately with `OK_STATUS`
- [ ] When the Node.js plugin is not present, the `WebPackagesListener` is not registered (no pointless listener reacting to package changes)
- [ ] When Node.js IS present in the test target, npm builds proceed normally (no regression)
- [ ] Normal (non-test) developer startup still shows the login dialog and start page as before
- [ ] No compilation errors introduced

## 6. Out of scope

- Refactoring the login flow itself (e.g., making it non-blocking by design)
- Handling other potential modal dialogs that may appear during tests (e.g., the "missing packages" `ListSelectionDialog` in `activeProjectChanged`)
- Making the Node.js bundle optional in production â it is only optional in test target platforms

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should `notifyLoginListeners(null)` be exposed publicly, or should a new `skipLogin()` method be added to `ServoyLoginDialog`? | Developer | open |
| Should the missing-packages `ListSelectionDialog` in `activeProjectChanged` also be guarded by `isTestRunning()`? (It uses `Display.getDefault().syncExec` which could also block.) | Architect | open |
| Are there external consumers of `createNPMCommand()` (e.g. Servoy-Copilot) that would need updating for the `IRunNPMCommand` return type change? | Developer | open |

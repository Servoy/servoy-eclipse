# Spec: SVY-21089 — Opencode started with Servoy-isolated home directory

## 1. Goal

When the Servoy product launches the embedded opencode server it should use
Servoy-specific configuration, data, state, and cache directories (under
`~/.servoy/`) instead of the default XDG locations (`~/.config/opencode`,
`~/.local/state/opencode`, `~/.local/share/opencode`, `~/.cache/opencode`).
This prevents the Servoy-managed opencode instance from reading or writing the
user's personal opencode configuration and avoids conflicts when the user also
runs opencode standalone from the command line.

---

## 2. Background

### 2.1 Current behaviour

`RunOpencodeCommand` launches opencode via `npm exec -- opencode serve …` using
`ngActivator.createNPMCommand()`. The `RunNPMCommand` class builds a
`ProcessBuilder`, copies the current JVM environment, and starts Node.js. No
XDG overrides are applied, so opencode inherits the XDG Base Directory values
from the parent Eclipse process — which are the user's real XDG directories.

opencode (npm package `opencode-ai`) resolves its four key directories by
appending `"opencode"` to the XDG base directory values from the `@opencode-ai/core`
`Global` module:

| Purpose | Default path (Linux/macOS) | Windows default | Environment variable |
|---------|---------------------------|-----------------|----------------------|
| Config  | `~/.config/opencode/`      | `%APPDATA%\opencode\` | `XDG_CONFIG_HOME` (or `OPENCODE_CONFIG_DIR`) |
| Data    | `~/.local/share/opencode/` | `%LOCALAPPDATA%\opencode\` | `XDG_DATA_HOME` |
| State   | `~/.local/state/opencode/` | `%LOCALAPPDATA%\opencode\` | `XDG_STATE_HOME` |
| Cache   | `~/.cache/opencode/`       | `C:\Users\<user>\.cache\opencode\` | `XDG_CACHE_HOME` |

The cache directory is used for runtime plugin downloads (e.g. `opencode-codebase-index`).
On Windows the XDG library does **not** map `XDG_CACHE_HOME` to a standard Windows
folder — it defaults to `~/.cache` as a literal path. Setting `XDG_CACHE_HOME`
explicitly redirects it on all platforms.

### 2.2 `RunNPMCommand` environment injection

`RunNPMCommand.runCommand()` builds the child-process environment by calling
`builder.environment()` (which starts with the current JVM environment) and
then adds `PATH`, `NODE_OPTIONS`, and `NG_PERSISTENT_BUILD_CACHE`. There is
no facility to inject additional variables from the outside.

### 2.3 Isolation requirement

The change must be isolated to the opencode server launch only. Other usages
of `RunNPMCommand` (NG build, npm install for Angular workspaces, etc.) must
not be affected.

---

## 3. Design

### 3.1 Approach: set XDG environment variables on the child process

Setting `XDG_CONFIG_HOME`, `XDG_DATA_HOME`, `XDG_STATE_HOME`, and `XDG_CACHE_HOME`
to `{user.home}/.servoy` on the opencode child process causes opencode to use:

- Config : `~/.servoy/opencode/`
- Data   : `~/.servoy/opencode/`
- State  : `~/.servoy/opencode/`
- Cache  : `~/.servoy/opencode/` (runtime plugin downloads, e.g. `opencode-codebase-index`)

Because all four XDG bases point to the same parent directory and opencode
appends `"opencode"` to each, all directories converge to `~/.servoy/opencode/`.
opencode manages its own subdirectory structure within that root.

`~/.servoy/` is already the conventional Servoy-specific user directory on all
platforms. Using it keeps opencode configuration alongside other Servoy user
files and makes them easy to locate and back up.

### 3.2 `OPENCODE_CONFIG_DIR` vs XDG variables

opencode exposes `OPENCODE_CONFIG_DIR` (a direct override just for the config
directory), but no equivalent for `data`, `state`, or `cache`. Using the four
XDG environment variables is the only way to redirect **all** opencode directories
with a single consistent base path, so XDG variables are the right mechanism.

### 3.3 `RunNPMCommand` — add `setExtraEnvironment`

A new package-visible setter is added to `RunNPMCommand`:

```java
/**
 * Extra environment variables to inject into the child process environment
 * before the command is started. Called before {@link #runCommand}.
 */
public void setExtraEnvironment(Map<String, String> extra) {
    this.extraEnvironment = Map.copyOf(extra);
}
```

A corresponding field:
```java
private Map<String, String> extraEnvironment = Map.of();
```

In `runCommand()`, after the initial environment setup:
```java
if (!extraEnvironment.isEmpty()) {
    environment.putAll(extraEnvironment);
}
```

This change is backward-compatible: existing callers that never call
`setExtraEnvironment` see no behaviour change.

### 3.4 `RunOpencodeCommand` — build and inject XDG environment

After creating `serverCommand` via `createNPMCommand()`, call:

```java
serverCommand.setExtraEnvironment(buildServoyXdgEnv());
```

Where:

```java
static Map<String, String> buildServoyXdgEnv() {
    String servoyHome = System.getProperty("user.home") +
                        File.separator + ".servoy";
    return Map.of(
        "XDG_CONFIG_HOME", servoyHome,
        "XDG_DATA_HOME",   servoyHome,
        "XDG_STATE_HOME",  servoyHome,
        "XDG_CACHE_HOME",  servoyHome   // redirects runtime plugin downloads
    );
}
```

This only applies to the opencode server process. The dev-mode override
(`opencode.url` system property) already bypasses `RunOpencodeCommand`
entirely, so no further guarding is needed.

### 3.5 Directory creation

The `.servoy` directory will be created by opencode itself (it calls
`fs.mkdir(..., { recursive: true })` on startup for all four paths). No
pre-creation step is required in Java.

### 3.6 Escape hatch

If for some reason the user needs to preserve the default XDG paths (e.g. they
already have an opencode config in the standard location that they want to
reuse), they can set the system property `opencode.use.default.xdg=true` in
their Servoy Developer JVM arguments. When this property is set,
`buildServoyXdgEnv()` returns an empty map and the XDG override is skipped.

---

## 4. Implementation plan

1. **`RunNPMCommand`** (`com.servoy.eclipse.ngclient.ui`):
   - Add field `private Map<String, String> extraEnvironment = Map.of();`
   - Add method `public void setExtraEnvironment(Map<String, String> extra)`
   - In `runCommand()`, after setting PATH/NODE_OPTIONS/NG_PERSISTENT_BUILD_CACHE,
     add `environment.putAll(extraEnvironment);`

2. **`RunOpencodeCommand`** (`com.servoy.eclipse.opencode`):
   - Add private static helper `buildServoyXdgEnv()` (see §3.4)
   - After `ngActivator.createNPMCommand(...)` returns `serverCommand`,
     call `serverCommand.setExtraEnvironment(buildServoyXdgEnv())`
   - Honour `opencode.use.default.xdg` escape hatch

3. **Verify** (manual): Launch Servoy Developer, open the Servoy AI perspective,
   and confirm opencode creates/uses `~/.servoy/opencode/` instead of
   `~/.config/opencode/`. On Linux this is straightforward to check with `ls`.

---

## 5. Acceptance criteria

- [ ] When Servoy Developer starts the embedded opencode server, opencode
      writes its config to `{user.home}/.servoy/opencode/` (or subdirectory
      thereof) instead of `~/.config/opencode/`.
- [ ] When Servoy Developer starts the embedded opencode server, opencode
      writes its state/session data to `{user.home}/.servoy/opencode/` instead
      of `~/.local/state/opencode/` or `~/.local/share/opencode/`.
- [ ] When Servoy Developer starts the embedded opencode server, opencode
      downloads runtime plugins (e.g. `opencode-codebase-index`) to
      `{user.home}/.servoy/opencode/` instead of `~/.cache/opencode/`
      (or `C:\Users\<user>\.cache\opencode\` on Windows).
- [ ] A standalone `opencode` CLI invocation (not via Servoy) continues to
      use the standard XDG directories and is unaffected by the Servoy change.
- [ ] Existing `RunNPMCommand` callers (NG build, npm install jobs) are
      unaffected — they do not set `extraEnvironment`, so behaviour is
      unchanged.
- [ ] Setting the JVM property `-Dopencode.use.default.xdg=true` disables the
      XDG override and opencode uses its default directories.
- [ ] No compilation errors introduced in `com.servoy.eclipse.ngclient.ui` or
      `com.servoy.eclipse.opencode`.

---

## 6. Out of scope

- Migrating existing opencode config from `~/.config/opencode/` to the new
  location (users who had a prior install keep their old config in its original
  location; the Servoy-embedded instance simply starts fresh in `.servoy/`).
- Supporting a fully custom path via UI or preferences.
- Windows-specific path handling beyond what `System.getProperty("user.home")`
  and the XDG library already provide.

---

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Does `opencode-ai ^1.15.11` actually respect all four XDG env vars on Windows? `XDG_CONFIG_HOME`, `XDG_DATA_HOME`, `XDG_STATE_HOME` confirmed working (tested). `XDG_CACHE_HOME` added after observing `C:\Users\<user>\.cache\opencode\` still being written — believed resolved. | Developer | open |
| Should `~/.servoy` be created proactively (e.g. by `OpencodeFolderCreatorJob`) to avoid any edge case where opencode fails before creating it? opencode uses `recursive: true` mkdir, so this should not be needed, but worth confirming. | Developer | open |

# Spec: SVY-21091 — Eclipse extension point for MCP endpoints

## 1. Goal

Provide an Eclipse extension point that lets plugins contribute MCP (Model Context
Protocol) endpoint URLs and an auth token to the embedded opencode server. Before
launching opencode, the framework collects all contributions, rewrites each URL to
use `{env:MCP_PORT}` in place of the hardcoded port, and merges the resulting
entries into `{user.home}/.servoy/opencode/opencode.json`. The actual port and auth
token are passed to the opencode child process as environment variables `MCP_PORT`
and `MCP_AUTH_TOKEN`.

Using environment variables for the port and token keeps the config file stable
across different Servoy Developer installations (which may run the internal Tomcat
server on different ports) and prevents the auth token from being stored in the
config file in plaintext.

---

## 2. Background

### 2.1 opencode.json format and env-var support

opencode reads its MCP server list from `opencode.json` in its config directory
(redirected to `{user.home}/.servoy/opencode/` via the XDG env vars set in
SVY-21089). opencode supports `{env:VAR_NAME}` syntax inside any string field to
substitute environment variable values at startup. The existing dev-time config
shows the pattern:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "eclipse-ide": {
      "type": "remote",
      "url": "http://localhost:8085/mcp/eclipse-ide"
    },
    "atlassian": {
      "type": "remote",
      "url": "https://mcp.atlassian.com/v1/mcp",
      "headers": {
        "Authorization": "Basic {env:ATLASSIAN_AUTH_BASIC}"
      }
    }
  }
}
```

### 2.2 Why a dynamic config is needed

Currently no `opencode.json` is written to `~/.servoy/opencode/`. The embedded
opencode server therefore has no MCP tools configured. For Servoy Developer to
expose its Eclipse tools to the AI assistant, the Eclipse MCP bridge endpoints must
be added to opencode's config before the server starts.

The MCP bridge runs on the internal Tomcat server whose port is determined at
startup (typically 8085, but can differ across installations). Hard-coding the port
in the JSON would break if the port is different. Using `{env:MCP_PORT}` lets the
same config file work for any port — each Eclipse instance passes its own value via
the opencode child process environment.

### 2.3 Existing startup flow

```
Activator.start()
  → OpencodeFolderCreatorJob.run()
      → npm install (if needed)
      → RunOpencodeCommand.schedule()
          → buildServoyXdgEnv()        ← XDG isolation (SVY-21089)
          → serverCommand.setExtraEnvironment(…)
          → npm exec opencode serve
```

The new code fits just before `new RunOpencodeCommand(opencodeDir).schedule()` in
`OpencodeFolderCreatorJob`, and in `RunOpencodeCommand.run()` to merge env var sets.

---

## 3. Design

### 3.1 Extension point: `com.servoy.eclipse.opencode.mcpEndpoint`

Declared in `com.servoy.eclipse.opencode/plugin.xml`:

```xml
<extension-point
    id="mcpEndpoint"
    name="Opencode MCP Endpoint Provider"
    schema="schema/mcpEndpoint.exsd"/>
```

A contributor (in any plugin) registers one provider per bundle:

```xml
<extension point="com.servoy.eclipse.opencode.mcpEndpoint">
    <endpoint class="com.example.MyMcpEndpointProvider"/>
</extension>
```

`class` must implement `com.servoy.eclipse.opencode.IMcpEndpointProvider`.

### 3.2 `IMcpEndpointProvider` interface

```java
package com.servoy.eclipse.opencode;

import java.util.List;

/**
 * Extension point interface for {@code com.servoy.eclipse.opencode.mcpEndpoint}.
 * <p>
 * Implementors return the list of MCP endpoint URLs served by the local Tomcat
 * MCP bridge, plus the auth token required to call those endpoints. The framework
 * rewrites each URL to use {@code {env:MCP_PORT}} in place of the actual port and
 * merges the entries into {@code opencode.json}.
 * </p>
 *
 * <h4>URL contract</h4>
 * Each URL must be of the form {@code http://localhost:<port>/<path>}, e.g.
 * {@code http://localhost:8085/mcp/eclipse-ide}. The MCP server name written into
 * {@code opencode.json} is derived from the last path segment of the URL
 * (e.g. {@code eclipse-ide}).
 *
 * <h4>Port contract</h4>
 * All URLs returned by all registered providers must use the same port (the
 * Tomcat MCP bridge port). The framework extracts it from the first URL it
 * encounters and sets it as {@code MCP_PORT}.
 */
public interface IMcpEndpointProvider {

    /**
     * Returns the fully-qualified MCP endpoint URLs exposed by this provider.
     * An empty list means this provider has no active endpoints right now.
     *
     * @return list of URLs, e.g. {@code ["http://localhost:8085/mcp/eclipse-ide"]}
     */
    List<String> getUrls();

    /**
     * Returns the Basic auth token used in the {@code Authorization} header for
     * these endpoints, or {@code null} if no authentication is needed.
     *
     * @return the raw token value (placed after "Basic " in the header), or
     *         {@code null}
     */
    String getAuthToken();
}
```

### 3.3 URL transformation and env-var naming

For every contributed URL:

1. **Extract** the port from the URL (e.g., `8085` from `http://localhost:8085/mcp/eclipse-ide`).
2. **Replace** the port with `{env:MCP_PORT}` to produce the template URL:
   `http://localhost:{env:MCP_PORT}/mcp/eclipse-ide`
3. **Derive** the server name from the last URL path segment:
   `/mcp/eclipse-ide` → `eclipse-ide`

Two environment variables are produced (regardless of how many endpoints are contributed):

| Variable | Value | Source |
|----------|-------|--------|
| `MCP_PORT` | e.g. `8085` | port extracted from contributed URLs |
| `MCP_AUTH_TOKEN` | the auth token | `IMcpEndpointProvider.getAuthToken()` |

`MCP_AUTH_TOKEN` is only added to the env map when at least one provider returns a
non-null auth token. All providers are assumed to share the same token (they all
serve the same Tomcat instance).

### 3.4 Generated opencode.json structure

For contributed URLs `http://localhost:8085/mcp/eclipse-ide` and
`http://localhost:8085/mcp/eclipse-coder` with an auth token, the generated entries
look like:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "eclipse-ide": {
      "type": "remote",
      "url": "http://localhost:{env:MCP_PORT}/mcp/eclipse-ide",
      "headers": {
        "Authorization": "Basic {env:MCP_AUTH_TOKEN}"
      }
    },
    "eclipse-coder": {
      "type": "remote",
      "url": "http://localhost:{env:MCP_PORT}/mcp/eclipse-coder",
      "headers": {
        "Authorization": "Basic {env:MCP_AUTH_TOKEN}"
      }
    }
  }
}
```

If a provider returns no auth token, the `"headers"` block is omitted for its
entries.

### 3.5 opencode.json merge strategy

The file is located at `{user.home}/.servoy/opencode/opencode.json`.

**If the file does not exist**: generate it fresh from the contributed endpoints.
Create the parent directory if needed.

**If the file already exists**: read and parse it, then for each contributed
endpoint check whether:
- the server name key is present in the `mcp` object, **and**
- the existing `url` value already uses the `{env:MCP_PORT}` template (correct form), **and**
- the existing `headers.Authorization` value matches `Basic {env:MCP_AUTH_TOKEN}` (or
  is absent when no auth token is provided).

If all three conditions hold for an entry, leave it unchanged. Otherwise (the entry
is missing, the URL still has a hardcoded port, or the auth value has changed),
write the up-to-date entry for that server name.

Entries in the existing file that were **not contributed** by any provider (e.g.
a user-added Atlassian entry) are left untouched.

The `$schema` field is added when creating a new file; it is also added if the file
exists but lacks the field.

A failure to read the existing file is treated the same as the file not existing
(log the error, generate fresh).

### 3.6 `McpConfigWriter` class (package-private)

```java
package com.servoy.eclipse.opencode;

/**
 * Builds, merges, and writes {@code opencode.json} from MCP endpoint contributions.
 * <p>
 * All methods are package-private static so they can be exercised from
 * {@code com.servoy.eclipse.opencode.tests} without an OSGi runtime.
 * </p>
 */
class McpConfigWriter {

    static final String ENV_PORT  = "MCP_PORT";
    static final String ENV_TOKEN = "MCP_AUTH_TOKEN";
    static final String SCHEMA_URL = "https://opencode.ai/config.json";

    /** Collects all registered providers via the extension point registry. */
    static List<IMcpEndpointProvider> collectProviders() { … }

    /**
     * Extracts the port number from a URL such as
     * {@code http://localhost:8085/mcp/eclipse-ide}.
     *
     * @throws IllegalArgumentException if the URL has no explicit port
     */
    static int extractPort(String url) { … }

    /**
     * Derives the MCP server name from a URL by taking the last non-empty
     * path segment. {@code ".../mcp/eclipse-ide"} → {@code "eclipse-ide"}.
     */
    static String serverNameFromUrl(String url) { … }

    /**
     * Builds the template URL by replacing the port with {@code {env:MCP_PORT}}.
     * {@code "http://localhost:8085/mcp/eclipse-ide"}
     * → {@code "http://localhost:{env:MCP_PORT}/mcp/eclipse-ide"}
     */
    static String templateUrl(String url) { … }

    /**
     * Builds the environment variable map for the given providers.
     * Returns a map containing {@code MCP_PORT} and, if any provider returns
     * a non-null auth token, {@code MCP_AUTH_TOKEN}.
     *
     * @return unmodifiable map; empty if {@code providers} is empty or all
     *         return empty URL lists
     */
    static Map<String, String> buildEnvVars(List<IMcpEndpointProvider> providers) { … }

    /**
     * Merges contributed endpoints into the opencode.json at {@code configFile}.
     * Creates the file (and parent directories) if absent; merges into the
     * existing content if present.
     *
     * @param providers  collected extension point contributions
     * @param configFile target path, e.g.
     *                   {@code ~/.servoy/opencode/opencode.json}
     */
    static void mergeConfig(List<IMcpEndpointProvider> providers, Path configFile)
            throws IOException { … }
}
```

JSON is built and parsed using `org.json` if available on the bundle class path, or
with `com.google.gson` — whichever is already a transitive dependency of
`com.servoy.eclipse.opencode`. If neither is available, a minimal hand-rolled JSON
reader/writer is acceptable for this fixed schema.

### 3.7 Integration in `OpencodeFolderCreatorJob`

Just before `new RunOpencodeCommand(opencodeDir).schedule()`:

```java
List<IMcpEndpointProvider> providers = McpConfigWriter.collectProviders();
Path servoyOpencodeCfgDir = Path.of(System.getProperty("user.home"), ".servoy", "opencode");
try {
    McpConfigWriter.mergeConfig(providers, servoyOpencodeCfgDir.resolve("opencode.json"));
} catch (IOException e) {
    ServoyLog.logError("OpencodeFolderCreatorJob: failed to write opencode.json", e);
    // non-fatal: opencode starts without the Servoy MCP endpoints configured
}
Map<String, String> mcpEnvVars = McpConfigWriter.buildEnvVars(providers);
new RunOpencodeCommand(opencodeDir, mcpEnvVars).schedule();
```

### 3.8 Integration in `RunOpencodeCommand`

Add an `additionalEnvVars` field and constructor overloads:

```java
private final Map<String, String> additionalEnvVars;  // Map.of() by default

public RunOpencodeCommand(File opencodeDir) {
    this(opencodeDir, Map.of());
}

public RunOpencodeCommand(File opencodeDir, Map<String, String> additionalEnvVars) {
    this(opencodeDir, 0, additionalEnvVars);
}

private RunOpencodeCommand(File opencodeDir, int retryCount,
                           Map<String, String> additionalEnvVars) {
    …
    this.additionalEnvVars = Map.copyOf(additionalEnvVars);
}
```

In `run()`, merge all env var sources before calling `setExtraEnvironment`:

```java
Map<String, String> env = new HashMap<>(buildServoyXdgEnv());
env.putAll(additionalEnvVars);
serverCommand.setExtraEnvironment(Collections.unmodifiableMap(env));
```

Retry scheduling preserves `additionalEnvVars`:

```java
new RunOpencodeCommand(opencodeDir, retryCount + 1, additionalEnvVars).schedule(RETRY_DELAY_MS);
```

### 3.9 Public API exports

`IMcpEndpointProvider` is the only type contributors need. Add it to
`Export-Package` in `MANIFEST.MF`. `McpConfigWriter` and (if needed) any internal
value types remain package-private and unexported.

---

## 4. Implementation plan

1. **Extension point declaration** (`com.servoy.eclipse.opencode`):
   - Add `<extension-point id="mcpEndpoint" …>` to `plugin.xml`.
   - Create `schema/mcpEndpoint.exsd` with the `endpoint` element and required
     `class` attribute typed to `IMcpEndpointProvider`.

2. **`IMcpEndpointProvider`** (`com.servoy.eclipse.opencode`):
   - Create `src/com/servoy/eclipse/opencode/IMcpEndpointProvider.java`.
   - `List<String> getUrls()` and `String getAuthToken()`.
   - Export from `MANIFEST.MF`.

3. **`McpConfigWriter`** (`com.servoy.eclipse.opencode`):
   - Create `src/com/servoy/eclipse/opencode/McpConfigWriter.java`.
   - Implement: `collectProviders`, `extractPort`, `serverNameFromUrl`,
     `templateUrl`, `buildEnvVars`, `mergeConfig`.
   - Package-private; not exported.

4. **`RunOpencodeCommand`** (`com.servoy.eclipse.opencode`):
   - Add `additionalEnvVars` field + constructor overloads (§3.8).
   - Merge env var sets in `run()`.
   - Pass `additionalEnvVars` forward in retry scheduling.

5. **`OpencodeFolderCreatorJob`** (`com.servoy.eclipse.opencode`):
   - Call `McpConfigWriter.collectProviders()`, `mergeConfig()`, `buildEnvVars()`
     before scheduling `RunOpencodeCommand` (§3.7).

6. **Compilation check** after each file: `eclipse-ide_getCompilationErrors` →
   `eclipse-coder_organizeImports` → `eclipse-coder_formatFile`.

---

## 5. Acceptance criteria

- [ ] The `com.servoy.eclipse.opencode.mcpEndpoint` extension point exists in
      `plugin.xml` with a valid `mcpEndpoint.exsd` schema.
- [ ] `IMcpEndpointProvider` is exported from `com.servoy.eclipse.opencode` and
      callable from other plugins without accessing internals.
- [ ] When at least one contributor provides URLs, `OpencodeFolderCreatorJob`
      writes (or merges into) `{user.home}/.servoy/opencode/opencode.json`
      containing an `mcp` entry for each contributed URL, with:
      - `"url": "http://localhost:{env:MCP_PORT}/mcp/<name>"` (port replaced)
      - `"headers": {"Authorization": "Basic {env:MCP_AUTH_TOKEN}"}` (when a token
        is provided)
- [ ] `MCP_PORT` is set to the actual port extracted from the contributed URLs and
      passed to the opencode child process.
- [ ] `MCP_AUTH_TOKEN` is set to the provider's auth token and passed to the
      opencode child process (only when a non-null token is provided).
- [ ] If `opencode.json` already exists and all contributed entries are already
      present and correct, the file is not overwritten (or is rewritten with
      identical content — no functional change).
- [ ] If `opencode.json` already exists and contains user-added entries not
      contributed by any provider, those entries are preserved unchanged.
- [ ] If `opencode.json` already exists and a contributed entry is missing or
      outdated (wrong URL template or auth header), only that entry is updated.
- [ ] A failure to read or write `opencode.json` is logged but does not prevent
      opencode from starting.
- [ ] `XDG_*` env vars (SVY-21089) and `MCP_PORT` / `MCP_AUTH_TOKEN` are all
      present in the opencode child process — neither set overwrites the other.
- [ ] Retry scheduling in `RunOpencodeCommand` preserves `MCP_PORT` /
      `MCP_AUTH_TOKEN` in subsequent attempts.
- [ ] When no contributors are registered, no `opencode.json` is written and no
      MCP env vars are added.
- [ ] No compilation errors in `com.servoy.eclipse.opencode`.

---

## 6. Out of scope

- Actual contributors to the extension point (those live in the Eclipse MCP bridge
  plugin and are a separate ticket).
- Dynamic reconfiguration while opencode is running (a restart is required to pick
  up new endpoint registrations or URL changes).
- Supporting multiple MCP bridge ports across different providers in the same
  Eclipse instance.
- UI for managing or inspecting MCP endpoint contributions.
- Migration of a manually-maintained `opencode.json` with hardcoded ports to the
  env-var template format.

---

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Does opencode resolve `{env:…}` inside the `"url"` field? It is confirmed working in header values; the `url` field should work too (same resolver), but worth a quick smoke-test before coding. | Developer | open |
| Which JSON library is available on the `com.servoy.eclipse.opencode` bundle classpath? (`org.json`, Gson, Jackson, or none) — determines the JSON merge implementation. | Developer | open |
| If a contributed URL has no explicit port (e.g. uses port 80/443 implicitly), should `extractPort` fail or fall back to the default? Assumption: all Tomcat MCP bridge URLs always have an explicit port. | Developer | open |

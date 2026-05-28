# Spec: SVY-21108 — Extract skills zip into user.home and project root

## 1. Goal

At Servoy Developer startup, read a zip file supplied via the system property
`SERVOY_SKILLS_ZIP` (path to the zip on disk). Extract its contents into two
destinations:

1. **`{user.home}/.servoy/opencode/`** — the `.opencode/` subdirectory and
   `opencode.json` from the zip root.
2. **Project root** (git root of the active solution, or project folder) — the
   `AGENTS.MD` file from the zip root, created if absent or its YAML block
   updated if it already exists.

This replaces the hardcoded `ProviderConfigWriter.PROVIDER_CONFIG_JSON` for the
model configuration: when the zip is present and contains `opencode.json`, that
file is used as the base config (before MCP endpoints are merged). The existing
`McpConfigWriter` merge always runs on top.

---

## 2. Background

### 2.1 Current opencode.json flow

`OpencodeFolderCreatorJob` calls:
1. `McpConfigWriter.mergeConfig()` — writes MCP endpoint entries.
2. `ProviderConfigWriter.mergeProviderConfig()` — writes the hardcoded LiteLLM
   provider block.

Both target `{user.home}/.servoy/opencode/opencode.json`.

### 2.2 Zip file structure

```
skills.zip
├── AGENTS.MD          → project root (create or update YAML block)
├── opencode.json      → {user.home}/.servoy/opencode/opencode.json
└── .opencode/         → {user.home}/.servoy/opencode/ (fully replaced)
    └── ...
```

The zip is currently obtained from the system property `SERVOY_SKILLS_ZIP`
(value is an absolute path to the file). When the property is absent or the
file does not exist, the feature is silently skipped.

### 2.3 AGENTS.MD YAML block

The `AGENTS.MD` file contains a fenced YAML block that agents use for
repo-specific configuration:

```yaml
servoy_version: "<servoy_version>"
postgres_version: "<postgres_version>"
databases:
  active: ["<database>"]
  ignore: []
```

If `AGENTS.MD` already exists (user may have customised it), the file must not
be overwritten wholesale. Only the above YAML fields are updated in-place.

### 2.4 Project root

`OpenCodeView.getActiveProjectPath()` already computes the project root: it
walks up from the active solution's folder to find the `.git` directory. The
same logic is reused here as a static utility.

---

## 3. Design

### 3.1 `SkillsZipExtractor` class

New package-private class in `com.servoy.eclipse.opencode`, alongside
`McpConfigWriter` and `ProviderConfigWriter`.

**Constants:**

```java
static final String SKILLS_ZIP_PROPERTY = "SERVOY_SKILLS_ZIP";
static final String POSTGRES_VERSION    = "17.6";
```

**Methods:**

```java
/** Returns the zip Path from the system property, or null if absent/missing. */
static Path getSkillsZipPath()

/**
 * Extracts the zip into the opencode config directory:
 * - .opencode/ subdirectory → fully deleted then re-extracted into configDir
 * - opencode.json            → written into configDir as-is
 *
 * @return true if opencode.json was written from the zip (caller skips ProviderConfigWriter)
 */
static boolean extractToConfigDir(Path zipFile, Path configDir) throws IOException

/**
 * Writes AGENTS.MD to projectRoot:
 * - If absent: create from zip content as-is.
 * - If present: update only the YAML block fields
 *   (servoy_version, postgres_version, databases.active).
 */
static void writeOrUpdateAgentsMd(Path zipFile, Path projectRoot) throws IOException

/**
 * Updates the YAML block inside an existing AGENTS.MD string.
 * Fields updated: servoy_version, postgres_version, databases.active.
 * Everything else is left unchanged.
 * Package-private for testing.
 */
static String updateAgentsYaml(String content, String servoyVersion,
                                String postgresVersion, List<String> databases)

/** Reads a named entry from the zip, or returns null if the entry is absent. */
static String readZipEntry(Path zipFile, String entryName) throws IOException
```

### 3.2 AGENTS.MD YAML update strategy

`updateAgentsYaml()` uses line-by-line string replacement (same hand-rolled
style as `McpConfigWriter` — no external YAML library). For each of the three
fields:

- `servoy_version:` — find the line, replace everything after `:` with the new
  value quoted.
- `postgres_version:` — same.
- `databases: / active:` — find the `active:` line inside the `databases:` block,
  replace the array value.

If a field is missing from the existing file, it is **not** inserted (assumption:
the zip template always contains all three fields, and removing them is a
deliberate user action).

**Value sources:**

| Field | Value |
|-------|-------|
| `servoy_version` | `ClientVersion.getMajorVersion() + "." + String.format("%02d", ClientVersion.getMiddleVersion())` → e.g. `"2026.06"` |
| `postgres_version` | `SkillsZipExtractor.POSTGRES_VERSION` (`"17.6"`) |
| `databases.active` | `ApplicationServerRegistry.get().getServerManager().getServerNames(true, false, false, false)` — enabled non-system servers |

### 3.3 Updated `OpencodeFolderCreatorJob`

After npm install, before `McpConfigWriter.mergeConfig()`:

```java
Path servoyOpencodeCfgDir = Path.of(System.getProperty("user.home"), ".servoy", "opencode");

// 1. Extract skills zip (if available)
boolean providerFromZip = false;
Path skillsZip = SkillsZipExtractor.getSkillsZipPath();
if (skillsZip != null) {
    try {
        providerFromZip = SkillsZipExtractor.extractToConfigDir(skillsZip, servoyOpencodeCfgDir);
    } catch (IOException e) {
        ServoyLog.logError("OpencodeFolderCreatorJob: failed to extract skills zip", e);
    }

    String projectRoot = getActiveProjectPath();
    if (projectRoot != null) {
        try {
            SkillsZipExtractor.writeOrUpdateAgentsMd(skillsZip, Path.of(projectRoot));
        } catch (IOException e) {
            ServoyLog.logError("OpencodeFolderCreatorJob: failed to write AGENTS.MD", e);
        }
    } else {
        ServoyLog.logInfo("OpencodeFolderCreatorJob: no active project, AGENTS.MD skipped.");
    }
}

// 2. Merge MCP endpoints (always)
List<IMcpEndpointProvider> providers = McpConfigWriter.collectProviders();
try {
    McpConfigWriter.mergeConfig(providers, servoyOpencodeCfgDir.resolve("opencode.json"));
} catch (IOException e) { ... }
Map<String, String> mcpEnvVars = McpConfigWriter.buildEnvVars(providers);

// 3. Provider config fallback (only if zip did not supply opencode.json)
if (!providerFromZip) {
    try {
        ProviderConfigWriter.mergeProviderConfig(servoyOpencodeCfgDir.resolve("opencode.json"));
    } catch (IOException e) { ... }
}
```

`getActiveProjectPath()` is extracted from `OpenCodeView` into a new package-private
static utility method `OpenCodeUtil.getActiveProjectPath()` so both `OpenCodeView`
and `OpencodeFolderCreatorJob` can call it without duplication.

### 3.4 `.opencode/` subdirectory extraction

The `.opencode/` directory inside the zip is extracted as follows:
1. Delete `configDir/.opencode/` completely if it exists.
2. Walk all entries in the zip whose names start with `.opencode/`.
3. For each entry: create parent dirs, write bytes.

This ensures the directory is always in sync with the zip on every restart.

---

## 4. Implementation plan

1. **`OpenCodeUtil.java`** (`com.servoy.eclipse.opencode`):
   Create `src/com/servoy/eclipse/opencode/OpenCodeUtil.java` with one static
   method `getActiveProjectPath()` extracted from `OpenCodeView`. Update
   `OpenCodeView` to call `OpenCodeUtil.getActiveProjectPath()`.

2. **`SkillsZipExtractor.java`** (`com.servoy.eclipse.opencode`):
   Create `src/com/servoy/eclipse/opencode/SkillsZipExtractor.java` with all
   methods from §3.1. Package-private; not exported.

3. **`OpencodeFolderCreatorJob.java`** (`com.servoy.eclipse.opencode`):
   Insert the zip extraction block (§3.3) before the existing
   `McpConfigWriter.collectProviders()` call. Gate `ProviderConfigWriter` call
   on `!providerFromZip`.

4. **Compilation check**: `eclipse-ide_getCompilationErrors` on
   `com.servoy.eclipse.opencode` — zero errors required.

---

## 5. Acceptance criteria

- [ ] When `SERVOY_SKILLS_ZIP` is not set, the feature is silently skipped and
      existing behaviour is unchanged.
- [ ] When `SERVOY_SKILLS_ZIP` points to a valid zip, `.opencode/` is fully
      extracted into `{user.home}/.servoy/opencode/` on every restart (old
      `.opencode/` directory deleted first).
- [ ] `opencode.json` from the zip is written to
      `{user.home}/.servoy/opencode/opencode.json` and MCP endpoints are merged
      on top by `McpConfigWriter`.
- [ ] When the zip provides `opencode.json`, `ProviderConfigWriter` is not called
      (its hardcoded block does not overwrite the zip-provided config).
- [ ] When `AGENTS.MD` does not exist at the project root, it is created from the
      zip content as-is.
- [ ] When `AGENTS.MD` already exists at the project root, only
      `servoy_version`, `postgres_version`, and `databases.active` are updated;
      all other content is preserved.
- [ ] `servoy_version` is set to `"<major>.<middle-zero-padded>"` (e.g.
      `"2026.06"`).
- [ ] `postgres_version` is set to `"17.6"`.
- [ ] `databases.active` contains the names of enabled registered servers from
      `ApplicationServerRegistry`.
- [ ] A failure in any extraction step is logged as an error but does not prevent
      opencode from starting.
- [ ] No compilation errors in `com.servoy.eclipse.opencode`.

---

## 6. Out of scope

- Fetching the zip from Servoy Cloud (future ticket — currently always from
  local file via system property).
- Re-running zip extraction when the active project changes after startup.
- Validating or sanitising the zip contents (e.g. zip-slip protection is assumed
  handled by extracting only known entry names).
- Watching the zip file for changes at runtime.

---

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| What are the exact `getServerNames` boolean parameters to get only user-visible servers (enabled, non-internal, non-system)? Assumption: `getServerNames(true, false, false, false)` per `BackgroundTableLoader` usage. | Developer | open |
| If no active project is open when the job runs, should AGENTS.MD extraction be deferred (e.g. via `IActiveProjectListener`) or simply skipped? Assumption: skip and log; the user reopens the AI view after activating a project. | Product | open |
| Should `updateAgentsYaml` insert missing YAML fields (if the user deleted them) or leave them absent? Assumption: leave absent — the user's edit is intentional. | Developer | open |

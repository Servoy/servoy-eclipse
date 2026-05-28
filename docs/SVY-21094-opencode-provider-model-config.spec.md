# Spec: SVY-21094 — Inject Servoy GenAI gateway into opencode.json

## 1. Goal

Before launching opencode, inject the Servoy GenAI gateway provider configuration
(`provider.litellm`, `model`, `small_model`) into
`{user.home}/.servoy/opencode/opencode.json` alongside the MCP endpoint entries
already written by `McpConfigWriter`. This gives opencode a working LLM backend
pointing at `genai.servoy-cloud.eu` without manual configuration.

The provider config is hardcoded for this ticket. In a future ticket it will come
dynamically from the Servoy Cloud login flow.

---

## 2. Background

### 2.1 Current opencode.json flow (SVY-21091 / SVY-21093)

`OpencodeFolderCreatorJob` calls `McpConfigWriter.mergeConfig()` to write MCP
endpoint entries into `opencode.json`, then passes `MCP_PORT` and
`MCP_AUTH_TOKEN` as environment variables to the opencode child process via
`RunOpencodeCommand`. The file is located at
`{user.home}/.servoy/opencode/opencode.json`.

### 2.2 opencode provider/model config format

opencode reads an `opencode.json` with an optional `provider` block (custom LLM
providers) and `model` / `small_model` strings that select the active model:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "provider": {
    "litellm": {
      "npm": "@ai-sdk/openai-compatible",
      "name": "LiteLLM",
      "options": {
        "apiKey": "{env:GENAI_API_KEY}",
        "baseURL": "https://genai.servoy-cloud.eu/v1"
      },
      "models": {
        "eu.anthropic.claude-sonnet-4-6":          { "name": "Claude Sonnet 4.6" },
        "eu.anthropic.claude-haiku-4-5-20251001-v1:0": { "name": "Claude Haiku 4.5" },
        "eu.anthropic.claude-opus-4-7":            { "name": "Claude Opus 4.7" }
      }
    }
  },
  "model": "litellm/eu.anthropic.claude-sonnet-4-6",
  "small_model": "litellm/eu.anthropic.claude-haiku-4-5-20251001-v1:0"
}
```

The `apiKey` is passed via environment variable `GENAI_API_KEY` (keeping the
secret off disk, consistent with how `MCP_AUTH_TOKEN` is handled). The hardcoded
value for this ticket is `"your-virtual-key"`.

### 2.3 Coexistence constraint

`McpConfigWriter` owns the `mcp` block; `ProviderConfigWriter` owns `provider`,
`model`, and `small_model`. Neither must disturb the other's fields or any
user-added entries.

---

## 3. Design

### 3.1 `ProviderConfigWriter` class

New package-private class in `com.servoy.eclipse.opencode`, parallel to
`McpConfigWriter`.

**Constants — only the API key is a named field:**

```java
static final String ENV_API_KEY     = "GENAI_API_KEY";   // env var name
static final String DEFAULT_API_KEY = "your-virtual-key"; // hardcoded for now
```

Everything else — the provider block structure, base URL, model IDs, model
names, `model`, `small_model` — lives in a single hardcoded JSON string:

```java
// The full config fragment (minus the apiKey value, which is an env-var reference).
// In a future ticket this string is replaced by whatever Servoy Cloud returns.
static final String PROVIDER_CONFIG_JSON =
    """
    {
      "provider": {
        "litellm": {
          "npm": "@ai-sdk/openai-compatible",
          "name": "LiteLLM",
          "options": {
            "apiKey": "{env:GENAI_API_KEY}",
            "baseURL": "https://genai.servoy-cloud.eu/v1"
          },
          "models": {
            "eu.anthropic.claude-sonnet-4-6": { "name": "Claude Sonnet 4.6" },
            "eu.anthropic.claude-haiku-4-5-20251001-v1:0": { "name": "Claude Haiku 4.5" },
            "eu.anthropic.claude-opus-4-7": { "name": "Claude Opus 4.7" }
          }
        }
      },
      "model": "litellm/eu.anthropic.claude-sonnet-4-6",
      "small_model": "litellm/eu.anthropic.claude-haiku-4-5-20251001-v1:0"
    }
    """;
```

When Servoy Cloud provides a config blob in the future, the code simply passes
that string instead of `PROVIDER_CONFIG_JSON` — no Java constant changes needed.

**`buildProviderEnvVars()` → `Map<String, String>`**

Returns `{GENAI_API_KEY: "your-virtual-key"}`. Same shape as
`McpConfigWriter.buildEnvVars()` so `OpencodeFolderCreatorJob` can merge both
maps before passing to `RunOpencodeCommand`.

**`mergeProviderConfig(Path configFile)` → void, throws `IOException`**

1. If `configFile` does not exist: create parent dirs and write a fresh JSON
   containing only `$schema`, `provider`, `model`, `small_model`.
2. If `configFile` exists: read it, then:
   - Add `$schema` if missing (same as `McpConfigWriter`).
   - Replace or insert `provider` as a top-level key (entire object replaced —
     it is fully Servoy-managed).
   - Replace or insert `model` string.
   - Replace or insert `small_model` string.
   - Leave all other keys (`mcp`, any user-added keys) untouched.
3. Write the result back to `configFile`.

**JSON manipulation approach:** Same hand-rolled string manipulation style as
`McpConfigWriter` — no additional JSON library dependency. For replacing a
top-level string value: locate the key, find the quoted value, replace it. For
replacing a top-level object value: locate the key, brace-count to find the
closing `}`, replace the entire block.

**Skip-if-unchanged optimisation:** Before writing, compare the result string to
the existing file content; skip the write if identical.

### 3.2 Updated `OpencodeFolderCreatorJob`

After the existing `McpConfigWriter` calls, add:

```java
try {
    ProviderConfigWriter.mergeProviderConfig(servoyOpencodeCfgDir.resolve("opencode.json"));
} catch (IOException e) {
    ServoyLog.logError("OpencodeFolderCreatorJob: failed to write provider config", e);
    // non-fatal: opencode starts without Servoy GenAI provider configured
}

Map<String, String> allEnvVars = new HashMap<>(mcpEnvVars);
allEnvVars.putAll(ProviderConfigWriter.buildProviderEnvVars());
new RunOpencodeCommand(opencodeDir, Collections.unmodifiableMap(allEnvVars)).schedule();
```

(The existing `new RunOpencodeCommand(opencodeDir, mcpEnvVars).schedule()` line
is replaced by the block above.)

---

## 4. Implementation plan

1. **`ProviderConfigWriter.java`** (`com.servoy.eclipse.opencode`):
   Create `src/com/servoy/eclipse/opencode/ProviderConfigWriter.java`.
   - Constants: `ENV_API_KEY`, `DEFAULT_API_KEY`, `PROVIDER_CONFIG_JSON` (the full
     JSON blob as a text block — §3.1).
   - `static Map<String, String> buildProviderEnvVars()`.
   - `static void mergeProviderConfig(Path configFile) throws IOException` — parses
     the top-level keys out of `PROVIDER_CONFIG_JSON` and merges them into the file.
   - Private helpers: `upsertTopLevelString(json, key, value)`,
     `upsertTopLevelObject(json, key, objectJson)`, `buildFreshJson()`.
   - Package-private; not exported.

2. **`OpencodeFolderCreatorJob.java`** (`com.servoy.eclipse.opencode`):
   - After the `McpConfigWriter.mergeConfig()` try/catch block, add the
     `ProviderConfigWriter.mergeProviderConfig()` call (§3.2).
   - Replace the final `new RunOpencodeCommand(…, mcpEnvVars).schedule()` line
     with the combined env-var map version (§3.2).

3. **`ProviderConfigWriterTest.java`** (`com.servoy.eclipse.opencode.tests`):
   Create `src/test/java/com/servoy/eclipse/opencode/ProviderConfigWriterTest.java`
   following the same pattern as `McpConfigWriterTest` — JUnit 4,
   `@Rule TemporaryFolder`, declared in package `com.servoy.eclipse.opencode` to
   access package-private methods. No OSGi runtime required. Cover the cases in §8.

4. **Compilation check**: `eclipse-ide_getCompilationErrors` on both
   `com.servoy.eclipse.opencode` and `com.servoy.eclipse.opencode.tests` —
   zero errors required.

---

## 5. Acceptance criteria

- [ ] `ProviderConfigWriter.buildProviderEnvVars()` returns a map containing
      `GENAI_API_KEY` → `"your-virtual-key"`.
- [ ] When `opencode.json` does not exist, `mergeProviderConfig()` creates it
      with `$schema`, `provider.litellm` (with `{env:GENAI_API_KEY}` as
      `apiKey`), `model`, and `small_model`.
- [ ] When `opencode.json` already exists (e.g. from `McpConfigWriter`),
      `mergeProviderConfig()` adds `provider`, `model`, and `small_model` without
      touching the `mcp` block or any user-added entries.
- [ ] When `provider`, `model`, and `small_model` are already present and
      correct, `mergeProviderConfig()` does not rewrite the file.
- [ ] `OpencodeFolderCreatorJob` passes `GENAI_API_KEY` to the opencode child
      process environment alongside `MCP_PORT` and `MCP_AUTH_TOKEN`.
- [ ] A failure in `mergeProviderConfig()` is logged but does not prevent
      opencode from starting.
- [ ] No compilation errors in `com.servoy.eclipse.opencode` or
      `com.servoy.eclipse.opencode.tests`.
- [ ] The generated `opencode.json` contains the `litellm` provider block and
      selects `litellm/eu.anthropic.claude-sonnet-4-6` as the active model.
- [ ] All 7 test cases in `ProviderConfigWriterTest` pass (see §7).

---

## 6. Out of scope

- Dynamic provider config from Servoy Cloud login (future ticket).
- User-facing UI to change the model selection.
- Supporting multiple providers or model switching at runtime.
- Migrating existing `opencode.json` files that have a manually configured
  provider block.

---

## 7. Test cases

`ProviderConfigWriterTest` must cover the following scenarios:

| # | Test | What is asserted |
|---|------|-----------------|
| 1 | `buildProviderEnvVars_returnsApiKey` | Map contains `GENAI_API_KEY` → `DEFAULT_API_KEY`; no other keys |
| 2 | `mergeProviderConfig_freshFile_createsAllFields` | Non-existent file is created; result contains `$schema`, `provider`, `model`, `small_model` with the correct values from `PROVIDER_CONFIG_JSON` |
| 3 | `mergeProviderConfig_existingMcpFile_addsProviderFields` | File that already has an `mcp` block gets `provider`, `model`, `small_model` added without disturbing the `mcp` content |
| 4 | `mergeProviderConfig_alreadyCorrect_fileUnchanged` | Calling `mergeProviderConfig()` twice does not change the file content on the second call (skip-if-unchanged) |
| 5 | `mergeProviderConfig_staleProviderBlock_replaced` | File with an outdated `provider` block (different baseURL or models) gets the provider block replaced with the current `PROVIDER_CONFIG_JSON` values |
| 6 | `mergeProviderConfig_userAddedKeys_preserved` | User-added top-level keys (e.g. a custom `theme` key) survive a `mergeProviderConfig()` call untouched |
| 7 | `mergeProviderConfig_mcpBlockUnchanged` | An `mcp` block present before the call is byte-for-byte identical after the call |

---

## 8. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| The `apiKey` is currently `"your-virtual-key"`. Is this the real shared key for all Servoy developers, or a placeholder that needs a real value before shipping? | Product | open |
| Should `mergeProviderConfig()` be called before or after `McpConfigWriter.mergeConfig()`? Order should not matter since they touch different keys, but confirm. | Developer | open |

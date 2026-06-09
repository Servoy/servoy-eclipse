# Spec: Custom Skills Zip and Overwrite Control

## 1. Goal

Allow developers to start Servoy Developer with a custom skills zip file and control whether
skills are overwritten on startup. This is achieved via two system properties:
- `SERVOY_SKILLS_ZIP` — if already set before login, the login dialog should not overwrite it.
- `SERVOY_SKILL_OVERWRITE` — when set to `false`, skip extracting skills to `~/.servoy/opencode`.

## 2. Background

### 2.1 Current behaviour

During Servoy login (`ServoyLoginDialog`), the server returns a `skill_endpoint` URL. The login
code unconditionally sets `System.setProperty("SERVOY_SKILLS_ZIP", ...)` with this URL. This
means any pre-existing value (e.g. set via `-DSERVOY_SKILLS_ZIP=/path/to/custom.zip` on the
command line) is always overwritten.

Later, `OpencodeFolderCreatorJob.run()` extracts the skills zip into `~/.servoy/opencode/` via
`SkillsZipExtractor.extractToConfigDir()`. This always deletes and re-extracts the `.opencode/`
subdirectory, making it impossible to use locally-modified skill files during development.

### 2.2 Desired behaviour

1. **Preserve custom `SERVOY_SKILLS_ZIP`:** If the system property is already set when the login
   response arrives, do not overwrite it. This lets developers launch with
   `-DSERVOY_SKILLS_ZIP=/path/to/my-skills.zip` and keep that value through login.

2. **New `SERVOY_SKILL_OVERWRITE` property:** When set to `"false"`, `OpencodeFolderCreatorJob`
   skips the skills extraction step (but still does everything else — npm install, MCP config,
   provider config, and starting the opencode server). Default is `true` (overwrite/extract as
   today).

## 3. Design

### 3.1 ServoyLoginDialog — guard SERVOY_SKILLS_ZIP

In `ServoyLoginDialog` (line ~232-236), wrap the `System.setProperty("SERVOY_SKILLS_ZIP", ...)`
call with a check: only set the property if it is not already set (i.e.
`System.getProperty("SERVOY_SKILLS_ZIP")` is `null` or blank).

### 3.2 OpencodeFolderCreatorJob — SERVOY_SKILL_OVERWRITE

After the comment `// 1. Extract skills zip (if available)` in `OpencodeFolderCreatorJob.run()`,
check the new system property `SERVOY_SKILL_OVERWRITE`. If its value is `"false"`
(case-insensitive), skip the entire skills extraction block (zip download, `extractToConfigDir`,
and `writeOrUpdateAgentsMd`). The MCP config merge, provider config, and opencode server launch
must still proceed.

The constant for the property name should be defined in `SkillsZipExtractor` alongside
`SKILLS_ZIP_PROPERTY` for consistency.

### 3.3 Documentation in skill4servoy README.md

Add a new section documenting both system properties, explaining their purpose, default values,
and usage examples (e.g. Eclipse launch configuration VM arguments).

## 4. Implementation plan

1. **`com.servoy.eclipse.ui/src/.../ServoyLoginDialog.java`** — Guard the `SERVOY_SKILLS_ZIP`
   `setProperty` call: only set when not already present.

2. **`com.servoy.eclipse.opencode/src/.../SkillsZipExtractor.java`** — Add a new constant:
   `static final String SKILL_OVERWRITE_PROPERTY = "SERVOY_SKILL_OVERWRITE";`

3. **`com.servoy.eclipse.opencode/src/.../OpencodeFolderCreatorJob.java`** — In the `run()`
   method, after the `// 1. Extract skills zip (if available)` comment, check
   `SkillsZipExtractor.SKILL_OVERWRITE_PROPERTY`. If `"false"` (case-insensitive), skip the
   entire skills extraction block but still proceed with MCP config merge and provider config.

4. **`/home/gabi/github_master/skill4servoy/README.md`** — Add a "System Properties" section
   documenting `SERVOY_SKILLS_ZIP` and `SERVOY_SKILL_OVERWRITE`.

## 5. Acceptance criteria

- [ ] Starting the developer with `-DSERVOY_SKILLS_ZIP=/custom/path.zip` preserves that value
      after login (login does not overwrite it).
- [ ] Starting without `-DSERVOY_SKILLS_ZIP` still gets the property set from the login response
      as before.
- [ ] Starting with `-DSERVOY_SKILL_OVERWRITE=false` skips skills extraction entirely (no
      delete + re-extract of `~/.servoy/opencode/.opencode/`).
- [ ] Starting without `SERVOY_SKILL_OVERWRITE` (or with `true`) extracts skills as before.
- [ ] The opencode server, MCP config, and provider config still work correctly when
      `SERVOY_SKILL_OVERWRITE=false`.
- [ ] The README.md in skill4servoy documents both properties with examples.

## 6. Out of scope

- Changing the `GENAI_API_KEY` behaviour (it is always overwritten by login, no guard needed).
- Any UI for configuring these properties (they are developer-only VM args).
- Partial overwrite (e.g. overwrite some files but not others within the zip).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should `SERVOY_SKILL_OVERWRITE=false` also skip `writeOrUpdateAgentsMd`? | — | assumed yes (skip entire extraction block) |

# Spec: SVY-21247 â WAR Exporter from Servoy Developer forgets -skipDatabaseViewsUpdate

## 1. Goal

The "Skip database views update" checkbox in the WAR export wizard loses its value between exports. Users must re-check it every time they build a WAR from the Developer IDE. The fix ensures this setting is persisted to and restored from `IDialogSettings`, matching the behaviour of all other options on the same wizard page.

## 2. Background

### 2.1 How the WAR export wizard persists settings

The WAR export wizard stores all user-selected options in Eclipse `IDialogSettings` so they survive between wizard invocations within a session and across IDE restarts. The model class `ExportWarModel` (in `com.servoy.eclipse.exporter.war`) is responsible for:

- **Loading** settings in its constructor from an `IDialogSettings` instance.
- **Saving** settings in `saveSettings(IDialogSettings)` after a successful export.

### 2.2 The skipDatabaseViewsUpdate option

The `skipDatabaseViewsUpdate` field was introduced in commit `c37e54132` (SVY-13670) to add command-line WAR exporter support for skipping database view updates. The field and its getter/setter live in `AbstractWarExportModel` (line 101, 657â667).

The GUI wizard page `DatabaseImportPropertiesPage` correctly:
- Initializes the checkbox from `exportModel.isSkipDatabaseViewsUpdate()` (line 137)
- Updates the model on selection change via `exportModel.setSkipDatabaseViewsUpdate(...)` (line 143)

However, `ExportWarModel` **never loads** this setting from `IDialogSettings` in its constructor, and **never saves** it in `saveSettings()`. This means the field always starts at its default value (`false`) on every wizard invocation.

### 2.3 Git history

The field was added in SVY-13670 for the command-line exporter (`WarArgumentChest`). The GUI persistence in `ExportWarModel` was overlooked at that time. Every other boolean option on the same wizard page (`allowSQLKeywords`, `updateSequences`, `overrideSequenceTypes`, `overrideDefaultValues`, `overwriteGroups`) is properly loaded and saved â only `skipDatabaseViewsUpdate` is missing.

## 3. Design

### 3.1 Add load/save of skipDatabaseViewsUpdate to ExportWarModel

Follow the exact same pattern used for the neighboring settings (e.g., `updateSequences` at lines 209/426):

- **Load** (constructor, after line 215): read the setting key `"export.skipDatabaseViewsUpdate"` via `Utils.getAsBoolean()` and call `setSkipDatabaseViewsUpdate()`.
- **Save** (`saveSettings`, after line 432): write the setting key using `settings.put("export.skipDatabaseViewsUpdate", isSkipDatabaseViewsUpdate())`.

### 3.2 Settings key

Use `"export.skipDatabaseViewsUpdate"` to be consistent with the naming pattern of other export settings (e.g., `"export.updateSequences"`, `"export.overrideSequenceTypes"`).

## 4. Implementation plan

1. **`com.servoy.eclipse.exporter.war/src/com/servoy/eclipse/warexporter/export/ExportWarModel.java`**
   - In the constructor (after line 215 where `upgradeRepository` is loaded), add:
     ```java
     setSkipDatabaseViewsUpdate(Utils.getAsBoolean(settings.get("export.skipDatabaseViewsUpdate")));
     ```
   - In `saveSettings()` (after line 432 where `upgradeRepository` is saved), add:
     ```java
     settings.put("export.skipDatabaseViewsUpdate", isSkipDatabaseViewsUpdate());
     ```

2. **Verify** no compilation errors and that the checkbox now retains its value across wizard invocations.

## 5. Acceptance criteria

- [ ] When the user checks "Skip database views update" and completes a WAR export, the setting is persisted.
- [ ] When the user re-opens the WAR export wizard, the "Skip database views update" checkbox reflects the previously saved value.
- [ ] The "Restore Defaults" button still correctly resets the checkbox to unchecked.
- [ ] The command-line WAR exporter (`WarArgumentChest`) is unaffected by this change.

## 6. Out of scope

- Changes to the command-line exporter persistence (it uses its own argument map, not `IDialogSettings`).
- Changes to other wizard pages or settings.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| None     |       |        |

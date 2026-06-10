# Spec: SVY-21168 — Angular update forbids assets outside working dir

## 1. Goal

After upgrading Angular, the CLI now rejects asset paths that reference directories outside the workspace root. The existing `angular.json` uses `"input": "../node_modules/@angular/common/locales"` which produces the error:

> The ../node_modules/@angular/common/locales asset path must be within the workspace root.

The fix copies the Angular locale files into a local directory inside the workspace root before the build runs, and updates `angular.json` to reference that local path.

## 2. Background

### 2.1 Angular workspace layout

The Angular project lives in a working directory (`projectFolder`) that is created per-solution inside the IDE's state location (e.g., `<mainTargetFolder>/<solutionName>/`). The source files from `com.servoy.eclipse.ngclient.ui/node/` are copied there by `NodeFolderCreatorJob`. The `angular.json` is part of the copied sources.

### 2.2 node_modules dedup structure

After `npm install` and `npm dedup`, shared packages (including `@angular/common`) are hoisted to a parent-level `node_modules` directory (`projectFolder/../node_modules`). The project-level `node_modules` only contains packages that cannot be deduped. This is why the existing `angular.json` uses `../node_modules/@angular/common/locales` — after dedup, the `@angular/common` package only exists in the parent.

### 2.3 Build entry point

The build is triggered in `WebPackagesListener.PackageCheckerJob.run()` at line 831:

```java
npmCommand = Activator.getInstance().createNPMCommand(this.projectFolder, Arrays.asList("run", whatToRun));
```

This same method is used for both IDE debug builds and WAR production export builds.

### 2.4 Git history

The locale asset entry was introduced in commit `12d0aaff1a7` (SVY-18890 — upgrade to Angular 17.x, switch to application/esbuild builder). The commit explains that angular locales are copied as assets and loaded via dynamic ESM imports at runtime.

## 3. Design

### 3.1 Change angular.json to use a local path

Update the static asset entry in `com.servoy.eclipse.ngclient.ui/node/angular.json` from:

```json
{
    "glob": "*.js",
    "input": "../node_modules/@angular/common/locales",
    "output": "/locales/angular"
}
```

to:

```json
{
    "glob": "*.js",
    "input": "locales/angular",
    "output": "/locales/angular"
}
```

The directory `locales/angular/` is relative to the workspace root (i.e., `projectFolder`) and will be populated by the Java code before the build.

### 3.2 Copy locale files in PackageCheckerJob

In `WebPackagesListener.PackageCheckerJob.run()`, just before the build command is executed (line 831), add a step to copy `*.js` files from `@angular/common/locales` into the local `locales/angular/` directory.

The source directory should be resolved by checking:
1. `new File(projectFolder.getParentFile(), "node_modules/@angular/common/locales")` (parent/root node_modules — the common case after dedup)
2. `new File(projectFolder, "node_modules/@angular/common/locales")` (local node_modules — fallback if dedup did not hoist it)

The copy should:
- Create `projectFolder/locales/angular/` if it does not exist
- Copy only `*.js` files (matching the glob in angular.json)
- Overwrite existing files (they may be stale from a previous Angular version)
- Log to the Titanium NGClient console on success/failure

### 3.3 WAR export compatibility

The same `PackageCheckerJob.run()` method is used for WAR exports (called from `exportNG2ToWar`). Since the locale-copy step is added before the build command, it works for both paths without any additional changes.

### 3.4 .gitignore consideration

Add `locales/` to `com.servoy.eclipse.ngclient.ui/node/.gitignore` (if one exists) so the copied files are not accidentally committed. Since the `locales/angular/` directory is created at build-time in the working target folder (not in the source project), this may not be necessary — but it's a safety measure for developers running builds from the source tree.

## 4. Implementation plan

1. **Modify `com.servoy.eclipse.ngclient.ui/node/angular.json`** (line 39): Change `"input": "../node_modules/@angular/common/locales"` to `"input": "locales/angular"`.

2. **Modify `WebPackagesListener.java`** — in `PackageCheckerJob.run()`, insert a new block before the build command (before line 831). The block should:
   - Resolve the locale source directory (parent node_modules first, then local)
   - Create `new File(projectFolder, "locales/angular")`
   - Use `FileUtils.copyDirectory()` with a `FileFilter` that accepts only `*.js` files
   - Log to the console: `"- copying Angular locale files to locales/angular"`
   - Wrap in try/catch and log errors without failing the build (locale files are not strictly required for the build to succeed)

3. **Add `locales/` to `.gitignore`** in `com.servoy.eclipse.ngclient.ui/node/` if developers could inadvertently run builds in the source tree.

## 5. Acceptance criteria

- [ ] IDE debug build (`npm run build_debug_nowatch`) succeeds without "asset path must be within the workspace root" error
- [ ] WAR production export build (`npm run build`) succeeds without the error
- [ ] Angular locale files (`*.js`) are present in `dist/app/browser/locales/angular/` after a successful build
- [ ] Runtime i18n/locale loading continues to work (e.g., `nl-NL` locale loads correctly in NG client)
- [ ] The fix works correctly after npm dedup (when `@angular/common` is only in the parent node_modules)
- [ ] The fix works correctly without dedup (when `@angular/common` is in the local node_modules)

## 6. Out of scope

- Changing the Angular locale loading mechanism (dynamic ESM import) itself
- Addressing other potential `../node_modules` references that may exist in component package assets (these use a different mechanism via `assetsToAdd`)
- Upgrading or changing the npm dedup strategy

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should the locale copy also filter by global locales (e.g., only copy locales that are actually used)? | Dev | open — current approach copies all ~600 locale files (~2MB), same as before |
| Is there a `.gitignore` file in `node/` that needs updating? | Dev | open — verify during implementation |

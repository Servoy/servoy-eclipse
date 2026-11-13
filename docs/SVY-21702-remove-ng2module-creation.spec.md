# Spec: SVY-21702 — Stop creating NG2-Module entries in the Java tooling / wizards / package template (produce standalone NG2-Components instead)

## 1. Goal

Make every Servoy Developer flow that **creates or upgrades** a web-component package, or **adds a component/service** to one, produce the modern standalone **`NG2-Components`** syntax and stop touching any `*.module.ts` file. Today these flows still write an `NG2-Module: <packageName>Module` manifest entry and still read/edit a package `*.module.ts` that the modernized package template no longer ships (deleted in commit `15790ffd91`), so a newly created package is broken: its manifest references a non-existent module, the template rename is a silent no-op, and a newly added component is never registered anywhere. This change is intentionally **one-directional**: tooling emits the new syntax, but Developer and the runtime must keep reading and honoring existing packages that still declare `NG2-Module` (e.g. the marketplace `svykanban` package, plus unknown customer packages).

## 2. Background

### 2.1 How packages declare their NG2 content

A web-component package's `META-INF/MANIFEST.MF` declares its Angular 2+ content in one of two ways:

- **Legacy module-based:** `NG2-Module: <SomeModule>` — the package ships a `*.module.ts` `@NgModule` that declares/exports its components, and the runtime imports that module.
- **Modern standalone:** `NG2-Components: <Class1>,<Class2>,...` — a comma-separated list of standalone component class names; there is no `*.module.ts`. Components are `standalone` (`standalone: true` or, per the current Angular default, standalone without the flag) and are exported from `project/src/public-api.ts`. Services are standalone `@Injectable({ providedIn: 'root' })` and are **not** listed in `NG2-Components` (that attribute is components only) — they are only exported via `public-api.ts` and become available through root DI.

`org.sablo.specification.PackageSpecification` (sablo repo,
`sablo/src/main/java/org/sablo/specification/PackageSpecification.java`) parses both attributes into `ng2Module` / `ng2Components` and exposes `getNg2Module()`, `getNg2Components()`, and `hasStandaloneComponents()` (true when `NG2-Components` is non-empty).

### 2.2 Current state of the component packages (verified)

Every first-party component package now declares **only** `NG2-Components` and ships no `*.module.ts`:
`servoydefault` (servoy-client), `aggrid`, `bootstrapcomponents`, `bootstrapextracomponents`, `servoy-extra-components`, `custom-rendered-components`. Marketplace/extra packages checked are also `NG2-Components`-only: `svyGMaps`, `svyChartJS`, `svycanvas`, `svyPDFViewer`, `smartDocumentEditor`, `12grid`, `fullcalendarcomponent2`.

The **one confirmed exception** is `svykanban` (`svykanban/svykanban/META-INF/MANIFEST.MF`), which declares **both** `NG2-Module: SvyKanbanModule` and `NG2-Components: SvyKanban`. This is the concrete real-world package proving `NG2-Module` entries still exist in the wild, so its read path must keep working. (Because `hasStandaloneComponents()` is true for it, the runtime generator already prefers its standalone branch — but the `NG2-Module` read path must not be removed.)

### 2.3 SVY-21388 context (git history)

- SVY-21388 (Resolved) removed the last `NG2-Module` entries and `*.module.ts` files from the first-party component packages and added the `NG2-Components` spec-level registration mechanism. SVY-21702 is the follow-up tooling cleanup it left behind.
- Commit `15790ffd91` ("modernize packagetemplate: Angular 22, Vitest, standalone, no peerDeps [ai]", 2026-08-27) **deleted** `com.servoy.eclipse.ngclient.ui/packagetemplate/project/src/ng2package.module.ts` and reduced `project/src/public-api.ts` to a commented stub (`//export * from './mycomponent/mycomponent';`). The wizard Java was not updated in the same commit — that is the direct cause of today's breakage.

### 2.4 The broken write paths (verified against source)

**`com.servoy.eclipse.ui` — `src/com/servoy/eclipse/ui/views/solutionexplorer/actions/`**

- **`NewResourcesComponentsOrServicesPackageAction.createManifest(...)`** — for a `WEB_COMPONENT` package it writes:
  ```java
  manifest.getMainAttributes().put(new Attributes.Name("NPM-PackageName"), packageName);
  manifest.getMainAttributes().put(new Attributes.Name("NG2-Module"), packageName + "Module");
  manifest.getMainAttributes().put(new Attributes.Name("Entry-Point"), "dist");
  ```
  There is no such module and no module file → the new package is wrong on creation.

- **`WebPackageUpgradeAction.run()`** — on upgrade it writes the same `NG2-Module: <project>Module` into the manifest, then for every existing component in the package calls `NewWebObjectAction.addNG2Code(name, packageType, project)`.

- **`NewWebObjectAction.addNG2Code(componentOrServiceName, elementType, packageRoot)`** — for a component it writes `<name>.html/.ts/.spec.ts`, appends the export to `public-api.ts`, edits `scripts/build.js` (via `modifyBuildFile`), then opens `project/src/<moduleName>.module.ts` with `moduleFile.getContents(true)` and injects the class into the module's `declarations:`/`exports:` arrays. For a service it does the analogous `providers:` injection. Since the module file no longer exists in a modern package, `getContents` throws; the `catch` only `ServoyLog.logError`s it. Net effect: the component's files and its `public-api.ts` export are written, `build.js` is edited, but the component is **never added to the `NG2-Components` manifest attribute** — which is now the only registration mechanism. Hence "created but not registered". (`addToModuleFileContent(...)` is the helper that does the array injection and becomes dead once module editing is removed.)

- **`NewPackageProjectWizard.copyAndRenameFiles(IProject project)`** (`src/com/servoy/eclipse/ui/wizards/NewPackageProjectWizard.java`) — still contains:
  ```java
  replaceTagInFile(new File(packageFile, "project/src/ng2package.module.ts"), PROJECT_NAME_TAG, projectName);
  ...
  new File(packageFile, "project/src/ng2package.module.ts").renameTo(new File(packageFile, "project/src/" + projectName + ".module.ts"));
  ```
  The template file was deleted, so `replaceTagInFile` fails and logs an exception and the `renameTo` returns false against a missing file. Both lines are dead and must be removed. The `replaceTagInFile(.../public-api.ts ...)` call just above them is still valid and stays.

### 2.5 The read/consume paths (must stay — back-compat)

These are NOT to be changed; they are the runtime/Developer paths that honor existing packages (including `svykanban` and customer packages that still declare `NG2-Module`):

- **`com.servoy.eclipse.ngclient.ui` — `WebPackagesListener.generateAllComponentsModuleContent(...)`** — builds `AllComponentsModule`. It already prefers `spec.hasStandaloneComponents()` and falls back to `else if (spec.getNg2Module() != null)` in all three loops (import / imports / exports). Leave the `getNg2Module()` branches in place.
- **`WebPackagesListener` package-collection loops** — gate on `(!stringIsEmpty(module) || entry.hasStandaloneComponents())`. Leave unchanged.
- **`ComponentTemplateGenerator`** — uses `getNg2Module() != null` as one OR-condition in its NG2-compatibility check. Leave unchanged (it is already subordinate to the `getNpmPackageName()`/`getEntryPoint()` checks that cover standalone packages).
- **sablo `PackageSpecification.getNg2Module()`** (separate repo) — leave in place; it is the read API the above paths use.

### 2.6 Modern standalone templates (verified references)

- Component template `component-templates/component.ts` already produces a standalone-compatible component (`ServoyBaseComponent`, `OnPush`, `standalone: false` — note: it is currently NOT marked standalone; see §3.4). Exported from `public-api.ts`.
- Service template `component-templates/service.ts` is a plain `@Injectable()` with no `providedIn`. Modern first-party standalone services use `@Injectable({ providedIn: 'root' })` (verified: `aggrid` `datagrid.service.ts`/`powergrid.service.ts`, and others) and are exported from `public-api.ts`; they are not in `NG2-Components`.
- A `NG2-Components`-only package's `public-api.ts` lists each component with `export * from './<name>/<name>';` (verified: `servoy-extra-components` `projects/servoyextracomponents/src/public-api.ts`).

## 3. Design

The change replaces "write `NG2-Module` + edit `*.module.ts`" with "write/maintain `NG2-Components` + export via `public-api.ts`, no module file" across the three write paths, and removes the dead template handling from the wizard. All read paths are untouched. A small shared manifest helper keeps the `NG2-Components` edit logic in one place.

### 3.1 New-package creation — `NewResourcesComponentsOrServicesPackageAction.createManifest`

For a `WEB_COMPONENT` package, replace the `NG2-Module` attribute with a **`NG2-Components`** attribute. A brand-new package has no components yet, so emit an **empty** `NG2-Components` value (an empty component list), e.g.:
```java
manifest.getMainAttributes().put(new Attributes.Name("NPM-PackageName"), packageName);
manifest.getMainAttributes().put(new Attributes.Name("NG2-Components"), ""); // populated as components are added
manifest.getMainAttributes().put(new Attributes.Name("Entry-Point"), "dist");
```
`hasStandaloneComponents()` returns false for an empty value, which is correct for an empty package and keeps the generator's standalone branch dormant until a component is added. Do not write any module attribute and do not create any `*.module.ts`.

> Note the `Entry-Point: dist` value is unchanged by this spec — it is the existing behavior for a freshly created package before it is built. Only the `NG2-Module` → `NG2-Components` switch is in scope here.

### 3.2 Add component/service — `NewWebObjectAction.addNG2Code`

Rework so it **never** opens, creates, or edits a `*.module.ts`:

- **Component branch:** keep writing `<name>.html`, `<name>.ts`, `<name>.spec.ts` from the templates; keep the `public-api.ts` append; keep `modifyBuildFile(...)`. **Remove** the entire `IFile moduleFile = ...; moduleFile.getContents(...); ... addToModuleFileContent(...); moduleFile.setContents(...)` block. **Add**: append the new component's class name to the package manifest's `NG2-Components` attribute (see §3.3). The class name is the existing `className` (`componentOrServiceName` with first letter upper-cased) — the same value previously injected into the module's `declarations`/`exports`.
- **Service branch:** keep writing `<name>.service.ts` from the template and keep the `public-api.ts` append and `modifyBuildFile(...)`. **Remove** the `providers:` module-injection block. Do **not** add the service to `NG2-Components` (that attribute is for components only). Standalone services are made available via `@Injectable({ providedIn: 'root' })` + the `public-api.ts` export, so the service template must emit `providedIn: 'root'` (see §3.4). No module edit, no manifest component entry.
- `addToModuleFileContent(...)` becomes unused after both branches drop module editing — remove it.
- Keep the method `static` and the signature unchanged (it is called from both `createComponentOrService` and `WebPackageUpgradeAction.run`).

**Future-proofing (per the ticket):** the add-path must work for both package kinds. Before editing the manifest, read the existing manifest: if the package declares `NG2-Module` (and not `NG2-Components`), it is a legacy module package — for such a package `addNG2Code` should keep the old module-editing behavior **only if** a `*.module.ts` is actually present, otherwise skip module editing and log a clear warning (do not crash). In practice modern creation always produces `NG2-Components`, so the common path is the standalone one; the legacy branch is a defensive back-compat path, not the default. Decide which attribute to maintain by inspecting the manifest at runtime, exactly as the ticket asks ("any action must check if it is still using NG2-Module or NG2-Components to see what it has to do").

### 3.3 Shared `NG2-Components` manifest helper

Add a small helper (static method in `NewWebObjectAction`, or a shared util reachable from both `NewWebObjectAction` and `NewResourcesComponentsOrServicesPackageAction`) that:

1. Reads `META-INF/MANIFEST.MF` of the package root (`IFolder`/`IProject`).
2. Parses the current `NG2-Components` value into an ordered, de-duplicated list of class names (empty when the attribute is absent or blank).
3. Adds the new component class name if not already present.
4. Writes the attribute back (comma-separated, no spaces, matching the existing manifests, e.g. `DataGrid,PowerGrid`), preserving the rest of the manifest and using the same `Manifest`-based read/write already used by `addToManifest(...)` (note: `addToManifest` already round-trips the manifest for the per-component `Name:` sub-sections, so reuse that pattern; be careful that `java.util.jar.Manifest` ordering/line-wrapping stays valid — the `Name:` entries added by `addToManifest` must not be lost).

This helper is also what `WebPackageUpgradeAction.run` relies on indirectly (it calls `addNG2Code` per component, which calls the helper). Keep the helper idempotent so re-running upgrade does not duplicate entries.

### 3.4 Templates — `component-templates/service.ts` (and verify `component.ts`)

- **`service.ts`:** change `@Injectable()` to `@Injectable({ providedIn: 'root' })` so an added service is a standalone, root-provided service with no module registration. Keep the rest of the template as-is.
- **`component.ts`:** it currently has `standalone: false`. Verify against a freshly-created package build whether a `NG2-Components` package expects `standalone: true` (or the attribute omitted, per the current Angular default where components are standalone by default). Align the template so a newly added component actually renders as a standalone component consistent with the first-party packages. (The first-party component `.ts` files were migrated to standalone in SVY-21388-era work; match that.) This is the one template detail to confirm by building a generated package (acceptance criterion in §5).

### 3.5 New-package wizard — `NewPackageProjectWizard.copyAndRenameFiles`

Remove the two dead `ng2package.module.ts` lines (the `replaceTagInFile(...ng2package.module.ts...)` and the `renameTo(... + ".module.ts")`). Keep everything else (the `public-api.ts` tag replacement, the `angular.json`/`package.json`/`build.js`/`ng-package.json`/`karma.conf.js`/`project/package.json` replacements, `ignoreNodeModules`, and the refresh). The modern `packagetemplate` already ships a stub `public-api.ts` and no module file, so after this removal a created package is internally consistent.

### 3.6 Read paths and sablo — explicitly unchanged

Do **not** modify `WebPackagesListener.generateAllComponentsModuleContent` (keep the `else if (spec.getNg2Module() != null)` branches), the `WebPackagesListener` collection-loop gates, `ComponentTemplateGenerator`'s `getNg2Module() != null` OR-condition, or sablo's `PackageSpecification.getNg2Module()`. These are the back-compat read paths for `NG2-Module`-declaring packages (`svykanban`, customer/marketplace). Optionally, log a one-time deprecation `ServoyLog` warning when a package that still declares `NG2-Module` is consumed — low value, leave out unless trivial; the Jira "Servoy-version-gated upgrade message" open question is deferred (see §7).

## 4. Implementation plan

All edits in `com.servoy.eclipse.ui` and `com.servoy.eclipse.ngclient.ui` go through the `eclipse-coder` tools; after each edit run `getCompilationErrors` and `organizeImports`. The `.ts` template files under `com.servoy.eclipse.ngclient.ui/component-templates/` are plugin resources (not Java) — edit them with `eclipse-coder` as well since they live inside an Eclipse project.

1. **`NewResourcesComponentsOrServicesPackageAction.createManifest`** (`com.servoy.eclipse.ui`): in the `WEB_COMPONENT` branch, replace the `NG2-Module` put with an empty `NG2-Components` put. Leave `NPM-PackageName` and `Entry-Point` as they are.
2. **Add the shared `NG2-Components` manifest helper** (§3.3) — a static method that reads/parses/updates/writes the `NG2-Components` attribute of a package's `MANIFEST.MF`, idempotently, reusing the `java.util.jar.Manifest` round-trip already used by `addToManifest`. Place it in `NewWebObjectAction` (or a shared util) so both actions can call it.
3. **`NewWebObjectAction.addNG2Code`** (`com.servoy.eclipse.ui`):
   - Component branch: delete the `moduleFile` read/`addToModuleFileContent`/`setContents` block; call the §3.3 helper to append the component `className` to `NG2-Components`. Keep `public-api.ts` export and `modifyBuildFile`.
   - Service branch: delete the `providers:` module-injection block; do not touch the manifest `NG2-Components`; keep `public-api.ts` export and `modifyBuildFile`.
   - Add the defensive legacy-package detection (§3.2): if the manifest has `NG2-Module` and a real `*.module.ts` exists, keep legacy module editing for that package; otherwise standalone path + warning.
   - Remove the now-unused `addToModuleFileContent` method.
4. **`component-templates/service.ts`** (`com.servoy.eclipse.ngclient.ui`): change `@Injectable()` to `@Injectable({ providedIn: 'root' })`.
5. **`component-templates/component.ts`** (`com.servoy.eclipse.ngclient.ui`): align `standalone` with first-party standalone components (confirm by building a generated package); adjust if needed.
6. **`WebPackageUpgradeAction.run`** (`com.servoy.eclipse.ui`): replace the `NG2-Module: <project>Module` manifest write with an empty-or-built `NG2-Components` write (the per-component `addNG2Code` calls will populate it via the §3.3 helper). Keep the rest (component stub creation, ng1 html copy, refresh). Confirm the loop still calls `addNG2Code` per component so the manifest ends up listing all upgraded components.
7. **`NewPackageProjectWizard.copyAndRenameFiles`** (`com.servoy.eclipse.ui`): remove the two dead `ng2package.module.ts` lines (replace + rename); keep the `public-api.ts` replacement and all other replacements.
8. **Verify no other references to the deleted template** remain: search the `com.servoy.eclipse.ui`/`com.servoy.eclipse.ngclient.ui` sources for `ng2package.module.ts` and `.module.ts` creation; there should be none left in the write paths.
9. **Tests** (`com.servoy.eclipse.ngclient.ui.tests`, plainjunit) — see §5. Keep the legacy-module assertions in `AllComponentsModuleGeneratorTest` as a back-compat guard; add a new plainjunit test that exercises the §3.3 manifest helper (empty → one component → idempotent re-add; comma-separated formatting; absent attribute handling).
10. **Compile + run** the plainjunit tests for `com.servoy.eclipse.ngclient.ui.tests` (package `com.servoy.eclipse.ngclient.ui.tests.plainjunit`) via `eclipse-ide.runJUnitTests` (JUnit 6). Fix any `getCompilationErrors` findings in both plugins.

## 5. Acceptance criteria

- [ ] A new web-component package created from the wizard has an `NG2-Components` manifest attribute (empty initially), no `NG2-Module` attribute, and **no `*.module.ts` file** anywhere in the package.
- [ ] Adding a component to a (standalone) package appends its class name to the package's `NG2-Components` manifest attribute, writes the component files, exports it from `public-api.ts`, edits `scripts/build.js`, and creates/edits **no** `*.module.ts`.
- [ ] Adding a service to a (standalone) package writes `<name>.service.ts` with `@Injectable({ providedIn: 'root' })`, exports it from `public-api.ts`, edits `scripts/build.js`, does **not** add it to `NG2-Components`, and creates/edits **no** `*.module.ts`.
- [ ] A package created, then given one added component and one added service, builds and is picked up by Servoy Developer with no `*.module.ts`.
- [ ] `WebPackageUpgradeAction` produces an `NG2-Components` manifest listing all the package's components (no `NG2-Module` written).
- [ ] No references to the deleted `ng2package.module.ts` template remain in `NewPackageProjectWizard` (or elsewhere in the write paths).
- [ ] Generator behavior for `NG2-Components` packages is unchanged: existing `AllComponentsModuleGeneratorTest` standalone assertions still pass.
- [ ] `NG2-Module` **read** paths are preserved: the `AllComponentsModuleGeneratorTest` legacy-module assertions (`generatesModuleImportsForLegacyPackage`, `handlesMixedPackages`) still pass, and a package declaring `NG2-Module` (e.g. `svykanban`) is still consumed at runtime and in Developer.
- [ ] New plainjunit test(s) cover the `NG2-Components` manifest helper (add, de-dupe/idempotent, formatting, empty/absent cases).
- [ ] Both `com.servoy.eclipse.ui` and `com.servoy.eclipse.ngclient.ui` compile with no new errors; the two highest-severity SpotBugs levels are clean for the changed code.

## 6. Out of scope

- Removing or deprecating the `NG2-Module` **read** paths in `WebPackagesListener`, `ComponentTemplateGenerator`, or sablo's `PackageSpecification.getNg2Module()` (Approach 2 in triage — an owner decision; see §7).
- Any change in the **sablo** repo.
- Migrating existing packages (e.g. converting `svykanban` from `NG2-Module` to `NG2-Components`).
- Deleting a component/service action — the ticket mentions updating `NG2-Components` on delete "if we have such an action". A delete-web-object action is not part of the current write paths investigated; if one exists it should symmetrically remove the class from `NG2-Components`, but confirming/finding it is out of scope for this change unless it surfaces during implementation.
- Changing `Entry-Point`/build pipeline behavior beyond the `NG2-Module`→`NG2-Components` switch.
- Backporting to maintenance branches — master (2026.12) only.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should the `NG2-Module` read paths (WebPackagesListener / ComponentTemplateGenerator / sablo `getNg2Module()`) ever be removed, and under which minimum-Servoy-version contract? (Triage Approach 2) | Product owner | Deferred — kept as back-compat per approved approach; surface in review only. |
| Should `WebPackageUpgradeAction` show a Servoy-version-gated message for packages that still declare `NG2-Module`? | Product owner | Deferred — not implemented; low value, revisit if requested. |
| `component.ts` template `standalone` flag: `standalone: true` vs omitted (Angular default) for a `NG2-Components` package — confirm by building a generated package. | Implementer | Resolve during implementation (acceptance-criteria build check). |
| Is there an existing "delete component/service" action that should also maintain `NG2-Components`? | Implementer | Confirm during implementation; out of scope if absent. |

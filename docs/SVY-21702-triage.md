# Triage Report — SVY-21702

**Verdict:** PROCEED

## Reported problem

Since SVY-21388, every first-party component package (aggrid, bootstrapcomponents,
bootstrapextracomponents, servoy-extra-components, custom-rendered-components,
servoydefault) declares only `NG2-Components` (standalone) in its MANIFEST.MF and no
longer ships a `*.module.ts` file. The Java tooling in Servoy Developer, however, still
writes `NG2-Module` entries and still reads/writes a package `*.module.ts` file. The
observable symptom: **creating a new component package from the wizard, or adding a
component/service to one, produces a broken or incomplete package** — the manifest gets a
`NG2-Module` attribute for a module that does not exist, the template rename of
`ng2package.module.ts` silently fails (the file was deleted), and the new component is
never registered anywhere (it is only logged as an exception).

The ticket also *proposes a solution*: switch the wizards to write `NG2-Components`, drop
the module-file editing, and either remove or deprecate the `NG2-Module` code paths in the
generators and in sablo. It explicitly leaves one decision open (remove vs. keep-for-
third-party).

## Root-cause assessment

The problem is real and lives in this project's Java tooling. Confirmed against source:

**Wizard / action write paths (com.servoy.eclipse.ui) — these are the actual bug:**

- `NewResourcesComponentsOrServicesPackageAction.createManifest`
  (`src/com/servoy/eclipse/ui/views/solutionexplorer/actions/NewResourcesComponentsOrServicesPackageAction.java:291`)
  writes `NG2-Module: <packageName>Module` for new `WEB_COMPONENT` packages. There is no
  module file and no such module class — this is wrong on a standalone package.
- `WebPackageUpgradeAction.run`
  (`.../actions/WebPackageUpgradeAction.java:85`) does the same `NG2-Module` write when
  upgrading, then (line 109) calls `NewWebObjectAction.addNG2Code(...)` per component.
- `NewPackageProjectWizard.copyAndRenameFiles`
  (`src/com/servoy/eclipse/ui/wizards/NewPackageProjectWizard.java:143-145`) still does
  `replaceTagInFile(project/src/ng2package.module.ts, ...)` and renames it to
  `<project>.module.ts`. Commit `15790ffd91` ("modernize packagetemplate: Angular 22,
  Vitest, standalone, no peerDeps") **deleted** `packagetemplate/project/src/ng2package.module.ts`
  (confirmed in that commit's stat), so the replace is a no-op that logs an exception and
  the rename targets a missing file. The new template's `public-api.ts` is now a commented
  stub (`//export * from './mycomponent/mycomponent';`).
- `NewWebObjectAction.addNG2Code`
  (`.../actions/NewWebObjectAction.java:270-368`) builds `project/src/<moduleName>.module.ts`,
  calls `moduleFile.getContents(true)`, and injects the new class into
  `declarations:`/`exports:`/`providers:`. Since the file no longer exists in a new
  package, `getContents` throws; the catch (line ~366) just `ServoyLog.logError`s it, so
  the component's `.ts`/`.html`/`.spec.ts` get written and `public-api.ts` gets the export,
  but the component is **never added to the `NG2-Components` manifest attribute** — which is
  now the only registration mechanism. Hence "created but not registered".

**Generator / read paths — these are dead for first-party packages but not individually broken:**

- `WebPackagesListener.generateAllComponentsModuleContent`
  (`com.servoy.eclipse.ngclient.ui/.../WebPackagesListener.java:213-271`) has three
  `else if (spec.getNg2Module() != null)` branches that run only when a package is *not*
  standalone.
- `WebPackagesListener` package-collection loops (lines 419-425 and 441-447) gate on
  `(!stringIsEmpty(module) || entry.hasStandaloneComponents())`.
- `ComponentTemplateGenerator`
  (`.../ComponentTemplateGenerator.java:128-130`) uses `getNg2Module() != null` as one of
  several OR-conditions to decide NG2 compatibility; `getNpmPackageName() != null` and
  `getEntryPoint() != null` already cover standalone packages, so this branch is redundant
  for them but harmless.

**sablo:** `org.sablo.specification.PackageSpecification`
(`sablo/src/main/java/org/sablo/specification/PackageSpecification.java:38,70,141`) reads
`NG2-Module` from the manifest and exposes `getNg2Module()`. Its only consumers are the
Servoy tooling above.

**Decision-relevant finding (third-party packages):** a workspace-wide MANIFEST.MF search
shows every first-party component package is `NG2-Components`-only, **except `svykanban`**
(`svykanban/svykanban/META-INF/MANIFEST.MF`), which still declares **both**
`NG2-Module: SvyKanbanModule` and `NG2-Components: SvyKanban`. This is a concrete example
of a package that still carries an `NG2-Module` entry. Because `hasStandaloneComponents()`
is true for it, the standalone branches already take precedence in the generator — but it
confirms that `NG2-Module` entries still exist in the wild. Other extra/marketplace
packages (svyGMaps, svyChartJS, svycanvas, svyPDFViewer, smartDocumentEditor, 12grid,
fullcalendarcomponent2) are all `NG2-Components`-only.

## Ticket premise check

The premise holds up. The write paths are genuinely broken (not just dead), and the fix
direction the ticket proposes — wizards must emit `NG2-Components` and stop touching a
module file — is correct and matches how the runtime now consumes packages
(`generateAllComponentsModuleContent` already prefers `hasStandaloneComponents()`).

The one place the ticket is deliberately *not* prescriptive is the read/generator paths:
it asks whether to delete the `NG2-Module` handling outright or keep it (deprecated) for
third-party packages. That is a real product decision, but it does **not** block the fix —
the divergence test does not trip, because the broken write paths have a single correct
fix regardless of the read-path decision, and the read-path decision is a product/API
compatibility choice the implementer can carry to the owner rather than an un-investigable
ambiguity. So this is PROCEED with a recommended default, not NEEDS_INPUT.

## Approaches considered

1. **Fix the write paths + keep `NG2-Module` read paths as deprecated/back-compat
   (recommended).** Change `createManifest` and `WebPackageUpgradeAction.run` to write
   `NG2-Components` (built from the components added to the package) instead of
   `NG2-Module`. Rework `NewWebObjectAction.addNG2Code` to append the new component's class
   name to the `NG2-Components` manifest attribute (keeping the `public-api.ts` export and
   the build-file edit) and, for services, decide on `providedIn: 'root'` vs. explicit
   registration — with **no** `*.module.ts` creation or editing. Remove the dead
   `ng2package.module.ts` replace/rename from `NewPackageProjectWizard.copyAndRenameFiles`.
   Leave the `getNg2Module()` read branches in `WebPackagesListener`/
   `ComponentTemplateGenerator` and `PackageSpecification.getNg2Module()` in place (they are
   already subordinate to `hasStandaloneComponents()`), optionally logging a one-time
   deprecation warning when a package still declares `NG2-Module`.
   - Pros: fixes the actual breakage; does not break third-party/marketplace packages that
     still ship `NG2-Module` (e.g. `svykanban`); low risk to the generator, whose standalone
     path is unchanged (satisfies the "existing generator tests still pass" criterion).
   - Cons: leaves some legacy branches in the codebase; the `AllComponentsModuleGeneratorTest`
     legacy-module test stays as a back-compat guard rather than being deleted.

2. **Fix the write paths + fully remove all `NG2-Module` handling** (generators, sablo
   `getNg2Module()`, and the legacy test cases).
   - Pros: cleanest end state; removes dead code the ticket points at.
   - Cons: any remaining `NG2-Module`-declaring package (confirmed to exist: `svykanban`,
     plus unknown customer/marketplace packages) would lose its read path; needs a stated
     minimum-Servoy-version contract; touches sablo (a separate repo/release) — larger blast
     radius for marginal gain. This is the ticket's "Open question" and is an owner decision.

3. **No code change.** Honestly evaluated and rejected: the wizard/upgrade/add-component
   flows are demonstrably broken for the current standalone-only packages (manifest written
   with a non-existent module, component never registered), and the deleted template file
   guarantees the failure. Doing nothing leaves package creation broken on master.

## Recommendation

**PROCEED with Approach 1.** Fix the three write paths (`createManifest`,
`WebPackageUpgradeAction.run`, `NewWebObjectAction.addNG2Code`) to produce/maintain
`NG2-Components` and stop creating or editing any `*.module.ts`, and strip the dead
`ng2package.module.ts` replace/rename from `NewPackageProjectWizard.copyAndRenameFiles`.
Keep the `NG2-Module` **read** paths in `WebPackagesListener`, `ComponentTemplateGenerator`
and sablo as deprecated back-compat (optionally with a deprecation log) rather than
removing them, because at least one real package (`svykanban`) and potentially customer/
marketplace packages still declare `NG2-Module`. Update `AllComponentsModuleGeneratorTest`
to keep the legacy-module assertions as a back-compat guard and add/keep standalone
assertions; add a test that a newly created/added component lands in the `NG2-Components`
manifest attribute.

Alternative if the product owner confirms no third-party `NG2-Module` support is required
(with a minimum-Servoy-version note): Approach 2, which additionally removes the generator
branches, `PackageSpecification.getNg2Module()`, and the legacy test cases, and would be
done partly in the sablo repo. Surface this as the one explicit decision in review; it does
not block the write-path fix.

## Git history findings

- `15790ffd91` ("modernize packagetemplate: Angular 22, Vitest, standalone, no peerDeps
  [ai]", Johan Compagner, 2026-08-27) deleted
  `com.servoy.eclipse.ngclient.ui/packagetemplate/project/src/ng2package.module.ts` and
  reduced `public-api.ts` to a commented stub — this is exactly what makes
  `NewPackageProjectWizard.copyAndRenameFiles` and `NewWebObjectAction.addNG2Code` fail
  today. The wizard Java code was not updated in the same commit.
- Related issue **SVY-21388** (Resolved) removed the last `NG2-Module` entries and
  `*.module.ts` files from the first-party component packages; its spec/triage exist under
  `docs/SVY-21388-*.md`. SVY-21702 is the follow-up tooling cleanup it left behind.
- No dependency/target-platform version bump is implicated; this is a tooling/behavior
  issue, not a library regression.

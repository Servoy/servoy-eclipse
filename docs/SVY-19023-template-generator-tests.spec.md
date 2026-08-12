# Spec: Unit Tests for ComponentTemplateGenerator and AllComponentsModule Generation

## Goal

Create JUnit tests that verify:
1. `ComponentTemplateGenerator` produces correct Angular template output for known component specs
2. The `allcomponents.module.ts` generation logic in `WebPackagesListener` correctly handles both `NG2-Module` (legacy) and `NG2-Components` (standalone) packages

## Background

### Current Architecture
- `ComponentTemplateGenerator.generateHTMLTemplate()` uses `WebComponentSpecProvider.getSpecProviderState()` (static singleton)
- `WebPackagesListener` generates `allcomponents.module.ts` inline in a large Job method
- Both are untestable in isolation without refactoring

### Test Infrastructure Available (sablo)
- `InMemPackageReader` — reads component specs from `Map<String, String>` (manifest + spec JSON)
- `WebComponentSpecProvider.init(IPackageReader[], IDefaultComponentPropertiesProvider)` — initializes the singleton from readers
- `WebComponentSpecProvider.disposeInstance()` — cleans up after test
- Existing test pattern: `CustomArrayAndCustomObjectTypeTest`, `WebComponentLibTest`, `WebComponentPropertiesTest`

### Test Infrastructure Available (servoy-client)
- `DefaultComponentPropertiesProvider` — adds internal properties (location, size, etc.)
- Spec files in `servoy_ngclient/war/servoydefault/` — real component definitions

## Design

### 1. Refactor: Extract AllComponents Generation Logic

Extract the `allcomponents.module.ts` generation from `WebPackagesListener` (lines 489-510) into a standalone static method:

```java
// In new file or in WebPackagesListener
public static String generateAllComponentsModuleContent(
    Collection<PackageSpecification<?>> packages)
```

This takes a collection of package specs and returns the generated TypeScript content. No file I/O, no Eclipse dependencies.

### 2. Refactor: Make ComponentTemplateGenerator Testable

Add an overload that accepts specs directly:

```java
public Pair<StringBuilder, StringBuilder> generateHTMLTemplate(
    WebObjectSpecification[] specs,
    Map<String, PackageSpecification<?>> packageSpecs,
    ITiNGExportModel model)
```

The existing method becomes a thin wrapper calling the new one with data from `WebComponentSpecProvider`.

### 3. Test: ComponentTemplateGeneratorTest

Location: `com.servoy.eclipse.ngclient.ui.tests/src/test/java/com/servoy/eclipse/ngclient/ui/ComponentTemplateGeneratorTest.java`

```java
@BeforeAll
static void setup() {
    // Load servoydefault MANIFEST + all .spec files via DirPackageReader or InMemPackageReader
    // Initialize WebComponentSpecProvider with DefaultComponentPropertiesProvider
}

@AfterAll
static void teardown() {
    WebComponentSpecProvider.disposeInstance();
}

@Test
void generatesCorrectTemplateForButton() {
    // Verify the generated ng-template for servoydefault-button contains:
    // - correct selector
    // - only non-internal property bindings
    // - correct handler bindings
    // - servoyApi and name bindings
}

@Test
void skipsInternalProperties() {
    // Verify location, size, cssPosition, anchors, formIndex are NOT in output
}

@Test
void skipsFormcomponentNavigatorPortal() {
    // Verify servoycore-formcomponent, servoycore-navigator, servoycore-portal are not generated
}

@Test
void generatesViewChildReferences() {
    // Verify readonly xxx = viewChild<TemplateRef<any>>('xxx') entries
}

@Test
void fullOutputMatchesExpectedFile() {
    // Compare full generated output against a committed expected file
    // Expected file: src/test/resources/expected_form_component_template.txt
}
```

### 4. Test: AllComponentsModuleGeneratorTest

```java
@Test
void generatesModuleImportsForLegacyPackage() {
    // Package with NG2-Module → generates NgModule import
}

@Test
void generatesDirectImportsForStandalonePackage() {
    // Package with NG2-Components → generates individual component imports
}

@Test
void handlesMixedPackages() {
    // Some legacy, some standalone → generates correct mixed output
}

@Test
void servoydefaultGeneratesAllComponents() {
    // Load real servoydefault MANIFEST with NG2-Components
    // Verify all 22 components appear in the imports
}
```

### 5. Loading Specs in Tests

**Option A: DirPackageReader** (simplest, uses real spec files)
```java
File servoydefaultDir = new File("../../servoy-client/servoy_ngclient/war/servoydefault");
IPackageReader reader = new Package.DirPackageReader(servoydefaultDir);
WebComponentSpecProvider.init(new IPackageReader[] { reader }, new DefaultComponentPropertiesProvider());
```

**Option B: InMemPackageReader** (self-contained, no external file dependency)
```java
// Read spec files from test resources
String manifest = readResource("servoydefault-manifest.mf");
Map<String, String> specs = Map.of("button/button.spec", readResource("button.spec"), ...);
IPackageReader reader = new InMemPackageReader(manifest, specs);
```

**Recommendation:** Option A for integration test (uses real specs, catches drift), Option B for focused unit tests (isolated, fast).

### 6. Expected Output File

Commit a golden file: `src/test/resources/expected_form_component_template.txt` containing the expected generated output for servoydefault components. When the generator changes, the test fails and the developer regenerates it.

## Implementation Plan

1. Extract `generateAllComponentsModuleContent()` from `WebPackagesListener` → static method
2. Add `generateHTMLTemplate(specs, packageSpecs, model)` overload to `ComponentTemplateGenerator`
3. Create `ComponentTemplateGeneratorTest.java` using `DirPackageReader` + `DefaultComponentPropertiesProvider`
4. Create `AllComponentsModuleGeneratorTest.java`  
5. Generate and commit expected output golden file
6. Verify tests pass in CI (may need `servoy-client` dependency in test project's classpath)

## Dependencies

The test project `com.servoy.eclipse.ngclient.ui.tests` is a fragment of `com.servoy.eclipse.ngclient.ui`, so it already has access to all its classes. Additional requirements:
- Access to `sablo` classes (WebComponentSpecProvider, InMemPackageReader, DirPackageReader)
- Access to `servoy_ngclient` classes (DefaultComponentPropertiesProvider)
- These should already be on the classpath via the fragment host's Require-Bundle

## Open Questions

| Question | Notes |
|----------|-------|
| Should the test use DirPackageReader pointing to servoy-client repo, or copy spec files into test resources? | DirPackageReader is simpler but creates a cross-repo dependency in tests. Copying avoids this but specs can drift. |
| Should the golden file comparison be exact or structural (ignoring whitespace/ordering)? | Exact match is stricter but fragile. Structural comparison is more robust. |
| Is `eclipse-test-plugin` packaging needed, or can this be a plain JUnit test? | The fragment host pattern requires PDE test infrastructure. However, if we extract the logic into static methods with no Eclipse deps, we could use a plain test. |

package com.servoy.eclipse.ngclient.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sablo.InMemPackageReader;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.Package.IPackageReader;

class AllComponentsModuleGeneratorTest
{
	private static final String STANDALONE_MANIFEST = "Manifest-Version: 1.0\n" +
		"Bundle-Name: Servoy Default\n" +
		"Bundle-SymbolicName: servoydefault\n" +
		"NPM-PackageName: @servoy/servoydefault\n" +
		"NG2-Components: ServoyDefaultTextField,ServoyDefaultButton,ServoyDefaultLabel\n" +
		"Entry-Point: projects/servoydefault\n" +
		"\n" +
		"Name: button/button.spec\n" +
		"Web-Component: True\n";

	private static final String LEGACY_MANIFEST = "Manifest-Version: 1.0\n" +
		"Bundle-Name: Servoy Extra\n" +
		"Bundle-SymbolicName: servoyextra\n" +
		"NPM-PackageName: @servoy/servoyextra\n" +
		"NG2-Module: ServoyExtraComponentsModule\n" +
		"\n" +
		"Name: slider/slider.spec\n" +
		"Web-Component: True\n";

	private static final String BUTTON_SPEC = "{\n" +
		"  \"name\": \"servoydefault-button\",\n" +
		"  \"displayName\": \"Button\",\n" +
		"  \"version\": 1,\n" +
		"  \"definition\": \"servoydefault/button/button.js\",\n" +
		"  \"libraries\": [],\n" +
		"  \"model\": { \"text\": \"string\" },\n" +
		"  \"handlers\": {},\n" +
		"  \"api\": {}\n" +
		"}";

	private static final String SLIDER_SPEC = "{\n" +
		"  \"name\": \"servoyextra-slider\",\n" +
		"  \"displayName\": \"Slider\",\n" +
		"  \"version\": 1,\n" +
		"  \"definition\": \"servoyextra/slider/slider.js\",\n" +
		"  \"libraries\": [],\n" +
		"  \"model\": { \"value\": \"int\" },\n" +
		"  \"handlers\": {},\n" +
		"  \"api\": {}\n" +
		"}";

	private PackageSpecification<WebObjectSpecification> createPackageSpec(String manifest, Map<String, String> specs) throws Exception
	{
		IPackageReader reader = new InMemPackageReader(manifest, specs);
		WebComponentSpecProvider.init(new IPackageReader[] { reader }, null);
		try
		{
			Map<String, PackageSpecification<WebObjectSpecification>> pkgSpecs = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecifications();
			return pkgSpecs.values().iterator().next();
		}
		finally
		{
			WebComponentSpecProvider.disposeInstance();
		}
	}

	@Test
	void generatesModuleImportsForLegacyPackage() throws Exception
	{
		HashMap<String, String> specs = new HashMap<>();
		specs.put("slider/slider.spec", SLIDER_SPEC);
		PackageSpecification<WebObjectSpecification> pkg = createPackageSpec(LEGACY_MANIFEST, specs);

		List<PackageSpecification<?>> packages = new ArrayList<>();
		packages.add(pkg);

		String result = WebPackagesListener.generateAllComponentsModuleContent(packages);

		assertTrue(result.contains("import { ServoyExtraComponentsModule } from '@servoy/servoyextra';"));
		assertTrue(result.contains("ServoyExtraComponentsModule,\n"));
		assertFalse(result.contains("ServoyDefaultTextField"));
	}

	@Test
	void generatesDirectImportsForStandalonePackage() throws Exception
	{
		HashMap<String, String> specs = new HashMap<>();
		specs.put("button/button.spec", BUTTON_SPEC);
		PackageSpecification<WebObjectSpecification> pkg = createPackageSpec(STANDALONE_MANIFEST, specs);

		List<PackageSpecification<?>> packages = new ArrayList<>();
		packages.add(pkg);

		String result = WebPackagesListener.generateAllComponentsModuleContent(packages);

		assertTrue(result.contains("import { ServoyDefaultTextField,ServoyDefaultButton,ServoyDefaultLabel } from '@servoy/servoydefault';"));
		assertTrue(result.contains("ServoyDefaultTextField,\n"));
		assertTrue(result.contains("ServoyDefaultButton,\n"));
		assertTrue(result.contains("ServoyDefaultLabel,\n"));
	}

	@Test
	void handlesMixedPackages() throws Exception
	{
		HashMap<String, String> standaloneSpecs = new HashMap<>();
		standaloneSpecs.put("button/button.spec", BUTTON_SPEC);
		PackageSpecification<WebObjectSpecification> standalonePkg = createPackageSpec(STANDALONE_MANIFEST, standaloneSpecs);

		HashMap<String, String> legacySpecs = new HashMap<>();
		legacySpecs.put("slider/slider.spec", SLIDER_SPEC);
		PackageSpecification<WebObjectSpecification> legacyPkg = createPackageSpec(LEGACY_MANIFEST, legacySpecs);

		List<PackageSpecification<?>> packages = new ArrayList<>();
		packages.add(standalonePkg);
		packages.add(legacyPkg);

		String result = WebPackagesListener.generateAllComponentsModuleContent(packages);

		assertTrue(result.contains("import { ServoyDefaultTextField,ServoyDefaultButton,ServoyDefaultLabel } from '@servoy/servoydefault';"));
		assertTrue(result.contains("import { ServoyExtraComponentsModule } from '@servoy/servoyextra';"));
		assertTrue(result.contains("ServoyDefaultTextField,\n"));
		assertTrue(result.contains("ServoyExtraComponentsModule,\n"));
		assertTrue(result.contains("export class AllComponentsModule { }"));
	}

	@Test
	void outputContainsNgModuleDecorator() throws Exception
	{
		HashMap<String, String> specs = new HashMap<>();
		specs.put("button/button.spec", BUTTON_SPEC);
		PackageSpecification<WebObjectSpecification> pkg = createPackageSpec(STANDALONE_MANIFEST, specs);

		List<PackageSpecification<?>> packages = new ArrayList<>();
		packages.add(pkg);

		String result = WebPackagesListener.generateAllComponentsModuleContent(packages);

		assertTrue(result.contains("import { NgModule } from '@angular/core';"));
		assertTrue(result.contains("@NgModule({"));
		assertTrue(result.contains("imports: ["));
		assertTrue(result.contains("exports: ["));
	}
}

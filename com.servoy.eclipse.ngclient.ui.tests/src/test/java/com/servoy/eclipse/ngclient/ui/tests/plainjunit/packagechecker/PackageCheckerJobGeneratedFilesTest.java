/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*/

package com.servoy.eclipse.ngclient.ui.tests.plainjunit.packagechecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.websocket.impl.ClientService;

import com.servoy.eclipse.model.war.exporter.ITiNGExportModel;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob.SpecInfo;
import com.servoy.eclipse.ngclient.ui.tests.utils.FakePackageCheckerEnvironment;
import com.servoy.eclipse.ngclient.ui.tests.utils.GeneratedProject;
import com.servoy.eclipse.ngclient.ui.tests.utils.ProjectFiles;
import com.servoy.eclipse.ngclient.ui.tests.utils.SpecMocks;

/**
 * SVY-21834 - PackageCheckerJob.writeAllServices / writeAllComponentsModule /
 * ensureZoneJsPolyfill / needsInstallOrBuild: the generated
 * allservices.service.ts, allcomponents.module.ts and polyfills.ts.
 */
class PackageCheckerJobGeneratedFilesTest {
	private static final String ZONE_PROPERTY = "ti.ng.includezonejs";

	@TempDir
	File projectFolder;

	private final FakePackageCheckerEnvironment env = new FakePackageCheckerEnvironment();
	private String zoneProperty;

	@BeforeEach
	void rememberZoneProperty() {
		zoneProperty = System.getProperty(ZONE_PROPERTY);
	}

	@AfterEach
	void restoreZoneProperty() {
		if (zoneProperty == null)
			System.clearProperty(ZONE_PROPERTY);
		else
			System.setProperty(ZONE_PROPERTY, zoneProperty);
	}

	private PackageCheckerJob job(ITiNGExportModel war) {
		return new PackageCheckerJob("test", projectFolder, war, null, env);
	}

	private static WebObjectSpecification service(String name, String packageName, String serviceName,
			String moduleName) {
		return SpecMocks.spec(name, packageName,
				"{\"packageName\":\"@test/svc\",\"entryPoint\":\"projects/svc\",\"serviceName\":\"" + serviceName + "\""
						+ (moduleName != null ? ",\"moduleName\":\"" + moduleName + "\"" : "") + "}");
	}

	private static SpecInfo infoWith(WebObjectSpecification... services) {
		SpecInfo info = new SpecInfo();
		for (WebObjectSpecification service : services)
			info.ng2Services.put(service, SpecMocks.reader(service.getPackageName(), "1"));
		return info;
	}

	@Test
	void servicesAreImportedInjectedAndProvidedInNameOrder() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		SpecInfo info = infoWith(service("svcpkg-b", "svcpkg", "SvcB", null),
				service("svcpkg-a", "svcpkg", "SvcA", null));

		assertTrue(job(null).writeAllServices(info, env.console()));

		String content = ProjectFiles.read(projectFolder, GeneratedProject.SERVICES);
		assertTrue(content.contains("import { SvcA } from '@test/svc';\nimport { SvcB } from '@test/svc';\n"), content);
		assertTrue(
				content.contains("private readonly " + ClientService.convertToJSName("svcpkg-a") + " = inject(SvcA);\n"
						+ "private readonly " + ClientService.convertToJSName("svcpkg-b") + " = inject(SvcB);\n"),
				content);
		assertTrue(content.contains("// generated providers start\nSvcA,\nSvcB,\n// generated providers end"), content);
		assertTrue(content.contains("// generated modules start\n// generated modules end"), content);
		assertTrue(env.consoleText().contains("- services ts file changed"));
	}

	@Test
	void aServiceModuleIsImportedAndRegisteredOnceAndIsNotAProvider() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		SpecInfo info = infoWith(service("svcpkg-a", "svcpkg", "SvcA", "SharedModule"),
				service("svcpkg-b", "svcpkg", "SvcB", "SharedModule"));

		assertTrue(job(null).writeAllServices(info, env.console()));

		String content = ProjectFiles.read(projectFolder, GeneratedProject.SERVICES);
		assertTrue(
				content.contains("import { SvcA,SharedModule } from '@test/svc';\nimport { SvcB } from '@test/svc';\n"),
				content);
		assertTrue(content.contains("// generated modules start\nSharedModule,\n// generated modules end"), content);
		assertTrue(content.contains("// generated providers start\n// generated providers end"), content);
	}

	@Test
	void theAggridServiceIsNeverAProvider() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");

		assertTrue(job(null).writeAllServices(
				infoWith(service("aggridservice-s", "aggridservice", "AgGridService", null)), env.console()));

		String content = ProjectFiles.read(projectFolder, GeneratedProject.SERVICES);
		assertTrue(content.contains("import { AgGridService } from '@test/svc';"), content);
		assertTrue(content.contains("// generated providers start\n// generated providers end"), content);
	}

	@Test
	void aServiceWithoutServiceNameIsSkipped() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		WebObjectSpecification noName = SpecMocks.spec("svcpkg-c", "svcpkg",
				"{\"packageName\":\"@test/svc\",\"entryPoint\":\"projects/svc\"}");

		assertFalse(job(null).writeAllServices(infoWith(noName), env.console()),
				"nothing generated, the file stays as it is");
	}

	@Test
	void aWarExportOnlyContainsTheExportedServices() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		SpecInfo info = infoWith(service("svcpkg-a", "svcpkg", "SvcA", null),
				service("svcpkg-b", "svcpkg", "SvcB", null));

		assertTrue(job(SpecMocks.war("true", Set.of(), Set.of(), Set.of("svcpkg-b"))).writeAllServices(info,
				env.console()));

		String content = ProjectFiles.read(projectFolder, GeneratedProject.SERVICES);
		assertTrue(content.contains("SvcB"), content);
		assertFalse(content.contains("SvcA"), content);
	}

	@Test
	void anUpToDateServicesFileIsNotRewrittenAndAMissingOneIsReported() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		SpecInfo info = infoWith(service("svcpkg-a", "svcpkg", "SvcA", null));
		assertTrue(job(null).writeAllServices(info, env.console()));
		FakePackageCheckerEnvironment second = new FakePackageCheckerEnvironment();
		assertFalse(new PackageCheckerJob("test", projectFolder, null, null, second).writeAllServices(info,
				second.console()));
		assertEquals("", second.consoleText());

		assertTrue(new File(projectFolder, GeneratedProject.SERVICES).delete());
		assertFalse(job(null).writeAllServices(info, env.console()));
		assertTrue(env.consoleText().contains("Exception while generating allservices.service.ts: "),
				env.consoleText());
	}

	@Test
	void theComponentsModuleListsTheComponentPackages() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		SpecInfo info = new SpecInfo();
		PackageSpecification<WebObjectSpecification> module = SpecMocks.pkg("zmodule", "@test/zmodule", "ZModule", null,
				"p", null, Map.of());
		PackageSpecification<WebObjectSpecification> standalone = SpecMocks.pkg("astandalone", "@test/a", null,
				"CompA, CompB", "p", null, Map.of());
		info.componentPackageSpecToReader.put(module, SpecMocks.reader("zmodule", "1"));
		info.componentPackageSpecToReader.put(standalone, SpecMocks.reader("astandalone", "1"));

		assertTrue(job(null).writeAllComponentsModule(info, env.console()));

		String content = ProjectFiles.read(projectFolder, GeneratedProject.MODULE);
		assertEquals(WebPackagesListener.generateAllComponentsModuleContent(info.componentPackageSpecToReader.keySet()),
				content);
		assertTrue(
				content.contains("import { CompA, CompB } from '@test/a';\nimport { ZModule } from '@test/zmodule';\n"),
				content);
		assertTrue(env.consoleText().contains("- component modules ts file changed"));
		FakePackageCheckerEnvironment second = new FakePackageCheckerEnvironment();
		assertFalse(new PackageCheckerJob("test", projectFolder, null, null, second).writeAllComponentsModule(info,
				second.console()));
	}

	@Test
	void aMissingComponentsModuleFileIsReported() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		assertTrue(new File(projectFolder, GeneratedProject.MODULE).delete());

		assertFalse(job(null).writeAllComponentsModule(new SpecInfo(), env.console()));

		assertTrue(env.consoleText().contains("Exception while generating allcomponents.module.ts: "),
				env.consoleText());
	}

	@Test
	void theDeveloperRunAddsTheZoneJsImportByDefault() throws IOException {
		System.clearProperty(ZONE_PROPERTY);
		GeneratedProject.create(projectFolder, false, "{}");
		ProjectFiles.write(projectFolder, GeneratedProject.POLYFILLS, "// polyfills\n");

		assertTrue(job(null).ensureZoneJsPolyfill(env.console()));

		assertEquals("import 'zone.js';\r\n// polyfills\n",
				ProjectFiles.read(projectFolder, GeneratedProject.POLYFILLS));
		assertTrue(env.consoleText().contains("- adding zone.js import to src/polyfills.ts"));
		assertFalse(job(null).ensureZoneJsPolyfill(env.console()), "the import is already there");
	}

	@Test
	void aMissingPolyfillsFileIsCreated() throws IOException {
		System.setProperty(ZONE_PROPERTY, "true");
		GeneratedProject.create(projectFolder, false, "{}");
		assertTrue(new File(projectFolder, GeneratedProject.POLYFILLS).delete());

		assertTrue(job(null).ensureZoneJsPolyfill(env.console()));

		assertEquals("import 'zone.js';\r\n", ProjectFiles.read(projectFolder, GeneratedProject.POLYFILLS));
	}

	@Test
	void zoneJsIsNotAddedWhenThePropertyIsFalseOrForAWarExport() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		ProjectFiles.write(projectFolder, GeneratedProject.POLYFILLS, "");

		System.setProperty(ZONE_PROPERTY, "false");
		assertFalse(job(null).ensureZoneJsPolyfill(env.console()));

		System.setProperty(ZONE_PROPERTY, "true");
		assertFalse(job(SpecMocks.war("true", Set.of(), Set.of(), Set.of())).ensureZoneJsPolyfill(env.console()),
				"a WAR export always stays zoneless");

		assertEquals("", ProjectFiles.read(projectFolder, GeneratedProject.POLYFILLS));
		assertEquals("", env.consoleText());
	}

	@Test
	void anUnwritablePolyfillsLocationIsReported() throws IOException {
		System.setProperty(ZONE_PROPERTY, "true");
		GeneratedProject.create(projectFolder, false, "{}");
		assertTrue(new File(projectFolder, GeneratedProject.POLYFILLS).delete());
		File polyfills = new File(projectFolder, GeneratedProject.POLYFILLS);
		assertTrue(polyfills.mkdir(), "a directory where the file should be: reading and writing fail");

		assertFalse(job(null).ensureZoneJsPolyfill(env.console()));

		assertTrue(env.consoleText().contains("Exception while adding zone.js to src/polyfills.ts: "),
				env.consoleText());
	}

	@Test
	void installAndBuildAreNeededForPackagesChangedSourcesOrAMissingDist() throws IOException {
		GeneratedProject.create(projectFolder, true, "{}");
		PackageCheckerJob job = job(null);

		assertFalse(job.needsInstallOrBuild(Set.of(), false));
		assertTrue(job.needsInstallOrBuild(Set.of("pkg@1"), false));
		assertTrue(job.needsInstallOrBuild(Set.of(), true));

		File dist = new File(projectFolder, "dist");
		java.nio.file.Files.walkFileTree(dist.toPath(), com.servoy.j2db.util.DeletePathVisitor.INSTANCE);
		assertTrue(job.needsInstallOrBuild(Set.of(), false), "no dist folder yet");
	}
}

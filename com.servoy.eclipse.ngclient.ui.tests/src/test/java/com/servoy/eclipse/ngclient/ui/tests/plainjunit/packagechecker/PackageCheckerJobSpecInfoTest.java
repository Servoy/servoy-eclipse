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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sablo.specification.CssLib;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.model.war.exporter.ITiNGExportModel;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob.SpecInfo;
import com.servoy.eclipse.ngclient.ui.tests.utils.FakePackageCheckerEnvironment;
import com.servoy.eclipse.ngclient.ui.tests.utils.SpecMocks;
import com.servoy.j2db.util.Pair;

/**
 * SVY-21834 - PackageCheckerJob.collectSpecInfo / collectCssAndAssets: what is
 * collected from the sablo spec providers and how a WAR export model filters
 * it.
 */
class PackageCheckerJobSpecInfoTest {
	private static final String SERVICE_JSON = "{\"packageName\":\"@test/svc\",\"serviceName\":\"SvcA\",\"entryPoint\":\"projects/svc\","
			+ "\"dependencies\":{\"csslibrary\":[\"~@test/svc/a.css;priority=3\"]},\"assets\":[\"a.png\"]}";

	@TempDir
	File projectFolder;

	private final FakePackageCheckerEnvironment env = new FakePackageCheckerEnvironment();
	private final IProgressMonitor monitor = new NullProgressMonitor();

	private PackageCheckerJob job(ITiNGExportModel war) {
		return new PackageCheckerJob("test", projectFolder, war, null, env);
	}

	private static Set<String> urls(Iterable<CssLib> libs) {
		java.util.Set<String> result = new java.util.HashSet<>();
		libs.forEach(lib -> result.add(lib.getUrl()));
		return result;
	}

	private static ITiNGExportModel warExporting(Set<String> packages, Set<String> components, Set<String> services) {
		ITiNGExportModel war = mock(ITiNGExportModel.class);
		doReturn(packages).when(war).getExportedPackagesExceptSablo();
		doReturn(components).when(war).getAllExportedComponents();
		doReturn(services).when(war).getAllExportedServicesWithoutSabloServices();
		return war;
	}

	private static IProgressMonitor cancelled() {
		NullProgressMonitor cancelled = new NullProgressMonitor();
		cancelled.setCanceled(true);
		return cancelled;
	}

	@Test
	void serviceCssAssetsAndReaderAreCollected() {
		WebObjectSpecification service = SpecMocks.spec("svcpkg-a", "svcpkg", SERVICE_JSON);
		IPackageReader reader = SpecMocks.reader("svcpkg", "1.0");
		env.serviceState = SpecMocks.state(Map.of(), Map.of(), List.of(service), List.of(reader));

		SpecInfo info = job(null).collectSpecInfo(monitor);

		assertNotNull(info);
		assertSame(reader, info.ng2Services.get(service));
		assertEquals(1, info.ng2Services.size());
		assertEquals(Set.of("~@test/svc/a.css"), urls(info.cssLibs));
		assertEquals(Set.of("a.png"), info.assetsToAdd);
		assertFalse(info.hasFontAwesomePro);
	}

	@Test
	void fontAwesomeProServiceSetsTheFlagOnlyWhenItHasCssLibraries() {
		WebObjectSpecification withCss = SpecMocks.spec("fa-a", "fapkg",
				"{\"packageName\":\"@servoy/fontawesomepro\",\"entryPoint\":\"p\",\"dependencies\":{\"csslibrary\":[\"~@fa/all.css\"]}}");
		env.serviceState = SpecMocks.state(Map.of(), Map.of(), List.of(withCss),
				List.of(SpecMocks.reader("fapkg", "1")));
		assertTrue(job(null).collectSpecInfo(monitor).hasFontAwesomePro);

		WebObjectSpecification withoutCss = SpecMocks.spec("fa-b", "fapkg",
				"{\"packageName\":\"@servoy/fontawesomepro\",\"entryPoint\":\"p\"}");
		env.serviceState = SpecMocks.state(Map.of(), Map.of(), List.of(withoutCss),
				List.of(SpecMocks.reader("fapkg", "1")));
		assertFalse(job(null).collectSpecInfo(monitor).hasFontAwesomePro);
	}

	@Test
	void serviceWithoutNg2PackageNameContributesOnlyCssAndAssets() {
		WebObjectSpecification service = SpecMocks.spec("svcpkg-b", "svcpkg",
				"{\"dependencies\":{\"csslibrary\":[\"b.css\"]},\"assets\":[\"b.png\"]}");
		env.serviceState = SpecMocks.state(Map.of(), Map.of(), List.of(service),
				List.of(SpecMocks.reader("svcpkg", "1")));

		SpecInfo info = job(null).collectSpecInfo(monitor);

		assertTrue(info.ng2Services.isEmpty());
		assertEquals(Set.of("b.css"), urls(info.cssLibs));
		assertEquals(Set.of("b.png"), info.assetsToAdd);
	}

	@Test
	void warExportSkipsServicesOfPackagesThatAreNotExported() {
		WebObjectSpecification service = SpecMocks.spec("svcpkg-a", "svcpkg", SERVICE_JSON);
		env.serviceState = SpecMocks.state(Map.of(), Map.of(), List.of(service),
				List.of(SpecMocks.reader("svcpkg", "1.0")));

		SpecInfo notExported = job(warExporting(Set.of("other"), Set.of(), Set.of())).collectSpecInfo(monitor);
		assertTrue(notExported.ng2Services.isEmpty());
		assertTrue(notExported.cssLibs.isEmpty());
		assertTrue(notExported.assetsToAdd.isEmpty());

		SpecInfo exported = job(warExporting(Set.of("svcpkg"), Set.of(), Set.of())).collectSpecInfo(monitor);
		assertEquals(1, exported.ng2Services.size());
		assertEquals(Set.of("a.png"), exported.assetsToAdd);
	}

	@Test
	void componentPackagesNeedAModuleOrStandaloneComponentsAndAnNpmName() {
		PackageSpecification<WebObjectSpecification> module = SpecMocks.pkg("zmodule", "@test/zmodule", "ZModule", null,
				"projects/zmodule", Set.of(new CssLib("~@test/zmodule/z.css")), Map.of());
		PackageSpecification<WebObjectSpecification> standalone = SpecMocks.pkg("astandalone", "@test/astandalone",
				null, "A,B", "projects/a", null, Map.of());
		PackageSpecification<WebObjectSpecification> noNpmName = SpecMocks.pkg("nonpm", null, "NoNpm", null, null, null,
				Map.of());
		PackageSpecification<WebObjectSpecification> nothing = SpecMocks.pkg("nothing", "@test/nothing", null, null,
				null, Set.of(new CssLib("~@test/nothing/n.css")), Map.of());
		IPackageReader moduleReader = SpecMocks.reader("zmodule", "1");
		IPackageReader standaloneReader = SpecMocks.reader("astandalone", "1");
		env.componentState = SpecMocks.state(
				Map.of("zmodule", module, "astandalone", standalone, "nonpm", noNpmName, "nothing", nothing), Map.of(),
				List.of(), List.of(moduleReader, standaloneReader, SpecMocks.reader("nonpm", "1"),
						SpecMocks.reader("nothing", "1")));

		SpecInfo info = job(null).collectSpecInfo(monitor);

		List<String> collected = info.componentPackageSpecToReader.keySet().stream()
				.map(PackageSpecification::getPackageName).collect(Collectors.toList());
		assertEquals(List.of("astandalone", "zmodule"), collected,
				"sorted by package name; only packages with module/standalone AND npm name");
		assertSame(moduleReader, info.componentPackageSpecToReader.get(module));
		assertSame(standaloneReader, info.componentPackageSpecToReader.get(standalone));
		assertEquals(Set.of("~@test/zmodule/z.css", "~@test/nothing/n.css"), urls(info.cssLibs),
				"css libraries are collected for every (exported) package, also those without a module");
	}

	@Test
	void warExportSkipsComponentAndLayoutPackagesThatAreNotExported() {
		PackageSpecification<WebObjectSpecification> component = SpecMocks.pkg("comp", "@test/comp", "CompModule", null,
				"projects/comp", Set.of(new CssLib("c.css")), Map.of());
		PackageSpecification<WebLayoutSpecification> layout = SpecMocks.pkg("lay", "@test/lay", "LayModule", null,
				"projects/lay", Set.of(new CssLib("l.css")),
				Map.of("section", SpecMocks.layout("lay-section", "section")));
		env.componentState = SpecMocks.state(Map.of("comp", component), Map.of("lay", layout), List.of(),
				List.of(SpecMocks.reader("comp", "1"), SpecMocks.reader("lay", "1")));

		SpecInfo none = job(warExporting(Set.of("other"), Set.of(), Set.of())).collectSpecInfo(monitor);
		assertTrue(none.componentPackageSpecToReader.isEmpty());
		assertTrue(none.cssLibs.isEmpty());
		assertTrue(none.structureTagNames.isEmpty());

		SpecInfo layoutOnly = job(warExporting(Set.of("lay"), Set.of(), Set.of())).collectSpecInfo(monitor);
		assertEquals(1, layoutOnly.componentPackageSpecToReader.size());
		assertEquals(Set.of("l.css"), urls(layoutOnly.cssLibs));
		assertEquals(Set.of("section"), layoutOnly.structureTagNames.keySet());
	}

	@Test
	void layoutTagNamesAndDirectivesDecideTheStructureTemplates() {
		WebLayoutSpecification plainDiv = SpecMocks.layout("lay-plain", "div");
		WebLayoutSpecification noTagType = SpecMocks.layout("lay-nodiv", null);
		WebLayoutSpecification section = SpecMocks.layout("lay-section", "section");
		WebLayoutSpecification divWithDirective = SpecMocks.layout("lay-dirdiv", "div", "dir1");
		WebLayoutSpecification noTagWithDirective = SpecMocks.layout("lay-dirnone", null, "dir2");
		WebLayoutSpecification spanWithDirective = SpecMocks.layout("lay-dirspan", "span", "dir3");
		PackageSpecification<WebLayoutSpecification> layout = SpecMocks.pkg("lay", null, null, null, null, null,
				Map.of("plain", plainDiv, "nodiv", noTagType, "section", section, "dirdiv", divWithDirective, "dirnone",
						noTagWithDirective, "dirspan", spanWithDirective));
		env.componentState = SpecMocks.state(Map.of(), Map.of("lay", layout), List.of(),
				List.of(SpecMocks.reader("lay", "1")));

		Map<String, Pair<WebLayoutSpecification, String>> tags = job(null).collectSpecInfo(monitor).structureTagNames;

		assertEquals(Set.of("section", "lay-dirdiv", "lay-dirnone", "lay-dirspan"), tags.keySet(),
				"a div / missing tag without directives needs no template; a non-div tag keeps its tag name as key unless a directive replaces it");
		assertSame(section, tags.get("section").getLeft());
		assertEquals("section", tags.get("section").getRight());
		assertEquals("div", tags.get("lay-dirdiv").getRight());
		assertEquals("div", tags.get("lay-dirnone").getRight());
		assertEquals("span", tags.get("lay-dirspan").getRight());
		assertSame(spanWithDirective, tags.get("lay-dirspan").getLeft());
		assertFalse(tags.containsKey("span"), "the plain span entry is replaced by the directive entry");
	}

	@Test
	void cancellingStopsEveryCollectionLoop() {
		WebObjectSpecification service = SpecMocks.spec("svcpkg-a", "svcpkg", SERVICE_JSON);
		env.serviceState = SpecMocks.state(Map.of(), Map.of(), List.of(service),
				List.of(SpecMocks.reader("svcpkg", "1")));
		assertNull(job(null).collectSpecInfo(cancelled()), "service loop");

		env.serviceState = SpecMocks.emptyState();
		PackageSpecification<WebObjectSpecification> component = SpecMocks.pkg("comp", "@test/comp", "M", null, null,
				null, Map.of());
		env.componentState = SpecMocks.state(Map.of("comp", component), Map.of(), List.of(),
				List.of(SpecMocks.reader("comp", "1")));
		assertNull(job(null).collectSpecInfo(cancelled()), "component loop");

		PackageSpecification<WebLayoutSpecification> layout = SpecMocks.pkg("lay", null, null, null, null, null,
				Map.of());
		env.componentState = SpecMocks.state(Map.of(), Map.of("lay", layout), List.of(),
				List.of(SpecMocks.reader("lay", "1")));
		assertNull(job(null).collectSpecInfo(cancelled()), "layout loop");
	}

	@Test
	void collectCssAndAssetsAddsTheCssAndAssetsOfAllComponents() {
		WebObjectSpecification a = SpecMocks.spec("pkg-a", "pkg",
				"{\"dependencies\":{\"csslibrary\":[\"a.css\"]},\"assets\":[\"a.png\"]}");
		WebObjectSpecification b = SpecMocks.spec("pkg-b", "pkg", "{\"assets\":[\"b.png\"]}");
		WebObjectSpecification c = SpecMocks.spec("pkg-c", "pkg", "{}");
		env.componentState = SpecMocks.state(Map.of(), Map.of(), List.of(a, b, c), List.of());
		SpecInfo info = new SpecInfo();

		assertTrue(job(null).collectCssAndAssets(info, monitor));

		assertEquals(Set.of("a.css"), urls(info.cssLibs));
		assertEquals(Set.of("a.png", "b.png"), info.assetsToAdd);
	}

	@Test
	void collectCssAndAssetsOnlyTakesTheExportedComponentsOfAWar() {
		WebObjectSpecification a = SpecMocks.spec("pkg-a", "pkg",
				"{\"dependencies\":{\"csslibrary\":[\"a.css\"]},\"assets\":[\"a.png\"]}");
		WebObjectSpecification b = SpecMocks.spec("pkg-b", "pkg",
				"{\"dependencies\":{\"csslibrary\":[\"b.css\"]},\"assets\":[\"b.png\"]}");
		env.componentState = SpecMocks.state(Map.of(), Map.of(), List.of(a, b), List.of());
		SpecInfo info = new SpecInfo();

		assertTrue(job(warExporting(Set.of(), Set.of("pkg-b"), Set.of())).collectCssAndAssets(info, monitor));

		assertEquals(Set.of("b.css"), urls(info.cssLibs));
		assertEquals(Set.of("b.png"), info.assetsToAdd);
	}

	@Test
	void collectCssAndAssetsReportsCancellation() {
		env.componentState = SpecMocks.state(Map.of(), Map.of(), List.of(SpecMocks.spec("pkg-a", "pkg", "{}")),
				List.of());

		assertFalse(job(null).collectCssAndAssets(new SpecInfo(), cancelled()));
	}
}

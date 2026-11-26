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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sablo.InMemPackageReader;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;

import com.servoy.eclipse.model.war.exporter.ITiNGExportModel;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener.PackageCheckerJob.SpecInfo;
import com.servoy.eclipse.ngclient.ui.tests.utils.FakePackageCheckerEnvironment;
import com.servoy.eclipse.ngclient.ui.tests.utils.GeneratedProject;
import com.servoy.eclipse.ngclient.ui.tests.utils.ProjectFiles;
import com.servoy.eclipse.ngclient.ui.tests.utils.SpecMocks;
import com.servoy.j2db.server.ngclient.DefaultComponentPropertiesProvider;
import com.servoy.j2db.server.ngclient.property.types.Types;
import com.servoy.j2db.util.Pair;

/**
 * SVY-21834 - PackageCheckerJob.writeComponentTemplates: the generated
 * component / structure / viewchild regions of the form, designer, list form
 * component and (optional) form template files. Uses the real component
 * template generator on a real (in memory) sablo spec provider.
 */
class PackageCheckerJobTemplatesTest {
	private static final String MANIFEST = "Manifest-Version: 1.0\nBundle-Name: Test Package\nBundle-SymbolicName: testpkg\n"
			+ "NPM-PackageName: @test/testpkg\nNG2-Components: TestButton\nEntry-Point: projects/testpkg\n\nName: button/button.spec\nWeb-Component: True\n";

	private static final String BUTTON_SPEC = "{\"name\":\"testpkg-button\",\"displayName\":\"Button\",\"version\":1,"
			+ "\"definition\":\"testpkg/button/button.js\",\"libraries\":[],\"model\":{\"text\":{\"type\":\"tagstring\",\"initialValue\":\"button\"}},"
			+ "\"handlers\":{},\"api\":{}}";

	private static final String BUTTON_TEMPLATE = "<ng-template #testpkgButton";
	private static final String BUTTON_VIEWCHILD = "readonly testpkgButton = viewChild<TemplateRef<any>>('testpkgButton');";

	@TempDir
	File projectFolder;

	private final FakePackageCheckerEnvironment env = new FakePackageCheckerEnvironment();

	@BeforeAll
	static void initSpecProvider() {
		Types.getTypesInstance().registerTypes();
		HashMap<String, String> components = new HashMap<>();
		components.put("button/button.spec", BUTTON_SPEC);
		IPackageReader reader = new InMemPackageReader(MANIFEST, components);
		WebComponentSpecProvider.init(new IPackageReader[] { reader }, DefaultComponentPropertiesProvider.instance);
	}

	@AfterAll
	static void disposeSpecProvider() {
		WebComponentSpecProvider.disposeInstance();
	}

	private PackageCheckerJob job(ITiNGExportModel war) {
		return new PackageCheckerJob("test", projectFolder, war, null, env);
	}

	private static SpecInfo infoWithSectionLayout() {
		SpecInfo info = new SpecInfo();
		WebLayoutSpecification section = SpecMocks.layout("lay-section", "section", "dirA");
		info.structureTagNames.put("section", new Pair<>(section, "section"));
		return info;
	}

	@Test
	void componentTemplatesAreWrittenIntoTheFormAndDesignerComponents() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");

		assertTrue(job(null).writeComponentTemplates(new SpecInfo(), env.console()));

		String form = ProjectFiles.read(projectFolder, GeneratedProject.FORM);
		String designer = ProjectFiles.read(projectFolder, GeneratedProject.DESIGNER);
		assertAll(() -> assertTrue(form.contains(BUTTON_TEMPLATE), form),
				() -> assertTrue(form.contains(BUTTON_VIEWCHILD), form),
				() -> assertTrue(designer.contains(BUTTON_TEMPLATE), designer),
				() -> assertTrue(designer.contains(BUTTON_VIEWCHILD), designer),
				() -> assertFalse(form.contains("[attr.data-svy-name]"),
						"only the form template copy stamps the data-svy attributes"),
				() -> assertTrue(env.consoleText().contains("- components ts file changed")),
				() -> assertTrue(env.consoleText().contains("- editor ts file changed")),
				() -> assertFalse(new File(projectFolder, GeneratedProject.FORM_TEMPLATE).exists(),
						"the optional form template copy is not created"));
	}

	@Test
	void anUpToDateProjectIsNotRewritten() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		assertTrue(job(null).writeComponentTemplates(new SpecInfo(), env.console()));
		String form = ProjectFiles.read(projectFolder, GeneratedProject.FORM);
		FakePackageCheckerEnvironment second = new FakePackageCheckerEnvironment();

		assertFalse(new PackageCheckerJob("test", projectFolder, null, null, second)
				.writeComponentTemplates(new SpecInfo(), second.console()));

		assertEquals(form, ProjectFiles.read(projectFolder, GeneratedProject.FORM));
		assertEquals("", second.consoleText());
	}

	@Test
	void aWarExportOnlyGeneratesTheExportedComponents() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		assertTrue(job(null).writeComponentTemplates(new SpecInfo(), env.console()));
		ITiNGExportModel nothingExported = SpecMocks.war("true", Set.of(), Set.of(), Set.of());

		assertTrue(job(nothingExported).writeComponentTemplates(new SpecInfo(), env.console()),
				"the generated button template must be removed again");

		assertFalse(ProjectFiles.read(projectFolder, GeneratedProject.FORM).contains(BUTTON_TEMPLATE));
		assertFalse(ProjectFiles.read(projectFolder, GeneratedProject.DESIGNER).contains(BUTTON_VIEWCHILD));
	}

	@Test
	void structureTemplatesGoIntoTheFormDesignerAndListFormComponent() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");

		assertTrue(job(null).writeComponentTemplates(infoWithSectionLayout(), env.console()));

		String form = ProjectFiles.read(projectFolder, GeneratedProject.FORM);
		String designer = ProjectFiles.read(projectFolder, GeneratedProject.DESIGNER);
		String lfc = ProjectFiles.read(projectFolder, GeneratedProject.LFC);
		String viewChild = "readonly svyResponsivesection = viewChild<TemplateRef<any>>('svyResponsivesection');";
		assertAll(() -> assertTrue(form.contains("<ng-template  #svyResponsivesection"), form),
				() -> assertTrue(form.contains("<section dirA [svyContainerStyle]=\"state\""), form),
				() -> assertTrue(form.contains("getTemplate(item)"), form),
				() -> assertFalse(form.contains("getRowItemTemplate"), "the form uses the plain item template"),
				() -> assertTrue(form.contains(viewChild), form),
				() -> assertTrue(designer.contains("<ng-template  #svyResponsivesection"), designer),
				() -> assertTrue(designer.contains(viewChild), designer),
				() -> assertTrue(lfc.contains("let-row=\"row\" let-i=\"i\""), lfc),
				() -> assertTrue(lfc.contains("getRowItemTemplate(item)"), lfc),
				() -> assertTrue(lfc.contains(viewChild), lfc),
				() -> assertTrue(env.consoleText().contains("- LFC components ts file changed")));
	}

	@Test
	void withoutStructureTagsTheListFormComponentIsLeftAlone() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		File lfc = new File(projectFolder, GeneratedProject.LFC);
		assertTrue(lfc.delete());

		assertTrue(job(null).writeComponentTemplates(new SpecInfo(), env.console()));

		assertFalse(lfc.exists(), "it is not even read when there are no structure tags");
		assertFalse(env.consoleText().contains("LFC"));
	}

	@Test
	void theFormTemplateCopyIsFilledWithTheDataSvyAttributes() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		ProjectFiles.write(projectFolder, GeneratedProject.FORM_TEMPLATE,
				GeneratedProject.COMPONENT_MARKERS + GeneratedProject.STRUCTURE_MARKERS);

		assertTrue(job(null).writeComponentTemplates(infoWithSectionLayout(), env.console()));

		String formTemplate = ProjectFiles.read(projectFolder, GeneratedProject.FORM_TEMPLATE);
		assertAll(() -> assertTrue(formTemplate.contains(BUTTON_TEMPLATE), formTemplate),
				() -> assertTrue(formTemplate.contains("[attr.data-svy-name]=\"state.name\""), formTemplate),
				() -> assertTrue(formTemplate.contains("[attr.data-svy-id]=\"state.model.svyMarkupId\""), formTemplate),
				() -> assertTrue(formTemplate.contains("<ng-template  #svyResponsivesection"), formTemplate),
				() -> assertTrue(env.consoleText().contains("- form template ts file changed")));
	}

	@Test
	void aMissingFileIsReportedAndLoggedButDoesNotThrow() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		assertTrue(new File(projectFolder, GeneratedProject.DESIGNER).delete());

		assertFalse(job(null).writeComponentTemplates(new SpecInfo(), env.console()));

		assertTrue(env.consoleText().contains("Exception while adjusting component templates: "), env.consoleText());
		verify(env.log).error(anyString(), any(Throwable.class));
		assertFalse(ProjectFiles.read(projectFolder, GeneratedProject.FORM).contains(BUTTON_TEMPLATE),
				"nothing is written when a source file cannot be read");
	}

	@Test
	void aMissingListFormComponentWithStructureTagsAbortsBeforeAnythingIsWritten() throws IOException {
		GeneratedProject.create(projectFolder, false, "{}");
		assertTrue(new File(projectFolder, GeneratedProject.LFC).delete());

		assertFalse(job(null).writeComponentTemplates(infoWithSectionLayout(), env.console()));

		assertTrue(env.consoleText().contains("Exception while adjusting component templates: "), env.consoleText());
		assertEquals(GeneratedProject.COMPONENT_MARKERS + GeneratedProject.STRUCTURE_MARKERS,
				ProjectFiles.read(projectFolder, GeneratedProject.FORM));
	}
}

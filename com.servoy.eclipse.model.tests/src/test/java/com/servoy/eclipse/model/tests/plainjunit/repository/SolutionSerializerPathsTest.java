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

package com.servoy.eclipse.model.tests.plainjunit.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.tests.plainjunit.utils.MemoryFileAccess;
import com.servoy.eclipse.model.tests.plainjunit.utils.TestPersists;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.UUID;

/**
 * The file layout helpers of {@link SolutionSerializer}: which file and folder each persist is stored in, and the working sets file.
 */
class SolutionSerializerPathsTest
{
	private Solution solution;

	@BeforeEach
	void setUp()
	{
		// a mocked root: getAncestor(SOLUTIONS) on its children answers null, so the current (>= 38) file layout is used
		solution = mock(Solution.class);
		doReturn("sol").when(solution).getName();
	}

	private Form form(String name)
	{
		Form form = TestPersists.create(solution, IRepository.FORMS, UUID.randomUUID());
		form.setName(name);
		return form;
	}

	@Nested
	@DisplayName("file paths of persists")
	class FilePaths
	{
		@Test
		@DisplayName("forms, relations, valuelists and media go into their folder with their extension")
		void topLevelPersists()
		{
			Relation relation = TestPersists.create(solution, IRepository.RELATIONS, UUID.randomUUID());
			relation.setName("orders_to_lines");
			ValueList valuelist = TestPersists.create(solution, IRepository.VALUELISTS, UUID.randomUUID());
			valuelist.setName("countries");
			Media media = TestPersists.create(solution, IRepository.MEDIA, UUID.randomUUID());
			media.setName("logo.png");

			assertAll(() -> assertEquals("sol/forms/orders.frm", SolutionSerializer.getRelativeFilePath(form("orders"), false)),
				() -> assertEquals("sol/relations/orders_to_lines.rel", SolutionSerializer.getRelativeFilePath(relation, false)),
				() -> assertEquals("sol/valuelists/countries.val", SolutionSerializer.getRelativeFilePath(valuelist, false)),
				() -> assertEquals("sol/medias/logo.png", SolutionSerializer.getRelativeFilePath(media, false)),
				() -> assertEquals("sol/", SolutionSerializer.getRelativePath(solution, false)),
				() -> assertEquals("solution_settings.obj", SolutionSerializer.getFileName(solution, false)));
		}

		@Test
		@DisplayName("form elements are stored in the form file, form methods in the form's js file")
		void formChildren()
		{
			Form orders = form("orders");
			GraphicalComponent button = TestPersists.create(orders, IRepository.GRAPHICALCOMPONENTS, UUID.randomUUID());
			ScriptMethod method = TestPersists.create(orders, IRepository.METHODS, UUID.randomUUID());
			method.setName("onShow");

			assertAll(() -> assertEquals("sol/forms/orders.frm", SolutionSerializer.getRelativeFilePath(button, false)),
				() -> assertEquals("sol/forms/orders.js", SolutionSerializer.getRelativeFilePath(method, false)),
				() -> assertEquals("sol/forms/orders.js", SolutionSerializer.getScriptPath(method, false)),
				() -> assertEquals("sol/forms/orders.js", SolutionSerializer.getScriptPath(orders, false)),
				() -> assertArrayEquals(new String[] { "sol/forms/orders.js" }, SolutionSerializer.getScriptPaths(orders, false)));
		}

		@Test
		@DisplayName("scope methods and variables go into <scope>.js, globals by default")
		void scopes()
		{
			ScriptMethod global = TestPersists.create(solution, IRepository.METHODS, UUID.randomUUID());
			global.setName("init");
			ScriptVariable variable = TestPersists.create(solution, IRepository.SCRIPTVARIABLES, UUID.randomUUID());
			variable.setName("counter");
			variable.setScopeName("utils");

			assertAll(() -> assertEquals("sol/globals.js", SolutionSerializer.getRelativeFilePath(global, false)),
				() -> assertEquals("sol/utils.js", SolutionSerializer.getScriptPath(variable, false)),
				() -> assertEquals("globals.js", SolutionSerializer.getScriptName(solution, false)),
				() -> assertNull(SolutionSerializer.getScriptPath(TestPersists.create(null, IRepository.METHODS, UUID.randomUUID()), false)));
		}

		@Test
		@DisplayName("a nameless persist is named sv_<uuid with underscores>")
		void namelessPersist()
		{
			UUID uuid = UUID.fromString("7C783D6E-8E26-40B9-8BDA-E2DC4F2ECDF8");
			Form nameless = TestPersists.create(solution, IRepository.FORMS, uuid);

			assertEquals("sv_" + uuid.toString().replace('-', '_'), SolutionSerializer.getPersistBaseFilename(nameless, false));
		}

		@Test
		@DisplayName("with useOldName the name before a rename is used; an empty old name means the persist had no name")
		void oldName()
		{
			Form orders = form("orders_renamed");
			orders.setRuntimeProperty(AbstractBase.NameChangeProperty, "orders");
			Form wasNameless = form("now_named");
			wasNameless.setRuntimeProperty(AbstractBase.NameChangeProperty, "");

			assertAll(() -> assertEquals("sol/forms/orders.frm", SolutionSerializer.getRelativeFilePath(orders, true)),
				() -> assertEquals("sol/forms/orders_renamed.frm", SolutionSerializer.getRelativeFilePath(orders, false)),
				() -> assertTrue(SolutionSerializer.getPersistBaseFilename(wasNameless, true).startsWith("sv_")));
		}

		@Test
		@DisplayName("appendExtensionToFileName per persist type")
		void extensions()
		{
			assertAll(() -> assertEquals("a.frm", SolutionSerializer.appendExtensionToFileName(IRepository.FORMS, "a")),
				() -> assertEquals("a.rel", SolutionSerializer.appendExtensionToFileName(IRepository.RELATIONS, "a")),
				() -> assertEquals("a.val", SolutionSerializer.appendExtensionToFileName(IRepository.VALUELISTS, "a")),
				() -> assertEquals("a.mnu", SolutionSerializer.appendExtensionToFileName(IRepository.MENUS, "a")),
				() -> assertEquals("a.tbl", SolutionSerializer.appendExtensionToFileName(IRepository.TABLENODES, "a")),
				() -> assertEquals("a.obj", SolutionSerializer.appendExtensionToFileName(IRepository.GRAPHICALCOMPONENTS, "a")));
		}

		@Test
		@DisplayName("isJSONFile recognizes the persist file extensions")
		void jsonFiles()
		{
			assertAll(() -> assertTrue(SolutionSerializer.isJSONFile("a.obj")), () -> assertTrue(SolutionSerializer.isJSONFile("a.frm")),
				() -> assertTrue(SolutionSerializer.isJSONFile("a.rel")), () -> assertTrue(SolutionSerializer.isJSONFile("a.val")),
				() -> assertTrue(SolutionSerializer.isJSONFile("a.tbl")), () -> assertTrue(SolutionSerializer.isJSONFile("a.mnu")),
				() -> assertFalse(SolutionSerializer.isJSONFile("a.js")), () -> assertFalse(SolutionSerializer.isJSONFile("a.png")));
		}

		@Test
		@DisplayName("composite classification of persists")
		void composites()
		{
			Form orders = form("orders");
			GraphicalComponent button = TestPersists.create(orders, IRepository.GRAPHICALCOMPONENTS, UUID.randomUUID());
			ValueList valuelist = TestPersists.create(solution, IRepository.VALUELISTS, UUID.randomUUID());

			assertAll(() -> assertTrue(SolutionSerializer.isCompositeWithItems(orders)),
				() -> assertFalse(SolutionSerializer.isCompositeWithItems(valuelist)), () -> assertFalse(SolutionSerializer.isCompositeWithItems(null)),
				() -> assertTrue(SolutionSerializer.isCompositeWithIndependentSerializationOfSubItems(solution)),
				() -> assertFalse(SolutionSerializer.isCompositeWithIndependentSerializationOfSubItems(button)),
				() -> assertTrue(SolutionSerializer.isCompositeItem(button)), () -> assertFalse(SolutionSerializer.isCompositeItem(orders)),
				() -> assertFalse(SolutionSerializer.isCompositeItem(valuelist)));
		}
	}

	@Nested
	@DisplayName("names from workspace files")
	class FromFiles
	{
		private IResource resource(String projectRelativePath)
		{
			IResource resource = mock(IResource.class);
			when(resource.getProjectRelativePath()).thenReturn(new Path(projectRelativePath));
			return resource;
		}

		@Test
		@DisplayName("getFormNameFromFile reads forms/<name>.js and .frm, nothing else")
		void formName()
		{
			assertAll(() -> assertEquals("orders", SolutionSerializer.getFormNameFromFile(resource("forms/orders.js"))),
				() -> assertEquals("orders", SolutionSerializer.getFormNameFromFile(resource("forms/orders.frm"))),
				() -> assertNull(SolutionSerializer.getFormNameFromFile(resource("forms/orders.less"))),
				() -> assertNull(SolutionSerializer.getFormNameFromFile(resource("forms/sub/orders.js"))),
				() -> assertNull(SolutionSerializer.getFormNameFromFile(resource("relations/orders.js"))),
				() -> assertNull(SolutionSerializer.getFormNameFromFile(null)));
		}

		@Test
		@DisplayName("calculation and entity js files give server and table name")
		void tableNodeFiles()
		{
			assertAll(
				() -> assertArrayEquals(new String[] { "example_data", "orders" },
					SolutionSerializer.getDataSourceForCalculationJSFile(resource("datasources/example_data/orders_calculations.js"))),
				() -> assertArrayEquals(new String[] { "example_data", "orders" },
					SolutionSerializer.getDataSourceForFoundsetJSFile(resource("datasources/example_data/orders_entity.js"))),
				() -> assertNull(SolutionSerializer.getDataSourceForCalculationJSFile(resource("datasources/example_data/orders_entity.js"))),
				() -> assertNull(SolutionSerializer.getDataSourceForFoundsetJSFile(resource("forms/example_data/orders_entity.js"))),
				() -> assertNull(SolutionSerializer.getDataSourceForFoundsetJSFile(null)));
		}

		@Test
		@DisplayName("getParentFile finds the form file of a form js, the table node of calculations, and the solution settings at the top")
		void parentFile(@TempDir File workspace) throws Exception
		{
			File project = new File(workspace, "sol");
			File forms = new File(project, "forms");
			File medias = new File(project, "medias");
			File tables = new File(project, "datasources/example_data");
			forms.mkdirs();
			medias.mkdirs();
			tables.mkdirs();
			File rootMetadata = new File(project, "rootmetadata.obj");
			Files.writeString(rootMetadata.toPath(), "{}");
			File formFile = new File(forms, "orders.frm");
			Files.writeString(formFile.toPath(), "{}");
			File tableFile = new File(tables, "orders.tbl");
			Files.writeString(tableFile.toPath(), "{}");

			assertAll(() -> assertEquals(formFile, SolutionSerializer.getParentFile(project, new File(forms, "orders.js"))),
				() -> assertEquals(tableFile, SolutionSerializer.getParentFile(project, new File(tables, "orders_calculations.js"))),
				() -> assertEquals(tableFile, SolutionSerializer.getParentFile(project, new File(tables, "orders_entity.js"))),
				() -> assertEquals(rootMetadata, SolutionSerializer.getParentFile(project, new File(forms, "other.js"))),
				() -> assertEquals(rootMetadata, SolutionSerializer.getParentFile(project, new File(project, "globals.js"))),
				() -> assertNull(SolutionSerializer.getParentFile(project, rootMetadata)),
				() -> assertNull(SolutionSerializer.getParentFile(project, null)), () -> assertNull(SolutionSerializer.getParentFile(project, workspace)));

			File settings = new File(project, "solution_settings.obj");
			Files.writeString(settings.toPath(), "{}");
			assertAll(() -> assertEquals(settings, SolutionSerializer.getParentFile(project, new File(project, "globals.js"))),
				() -> assertNull(SolutionSerializer.getParentFile(project, settings)));
		}
	}

	@Nested
	@DisplayName("working sets")
	class WorkingSets
	{
		private static final String PATH = "resources/workingsets/workingsets.json";

		@Test
		@DisplayName("working sets are written sorted by name with sorted paths, and read back")
		void roundTrip() throws Exception
		{
			MemoryFileAccess files = new MemoryFileAccess();
			Map<String, List<String>> sets = new TreeMap<>();
			sets.put("zeta", new ArrayList<>(List.of("sol/forms/b.frm", "sol/forms/a.frm")));
			sets.put("alpha", null);

			SolutionSerializer.serializeWorkingSetInfo(files, "resources", sets);
			Map<String, List<String>> back = SolutionDeserializer.deserializeWorkingSets(files, "resources");

			assertAll(() -> assertEquals(PATH, SolutionSerializer.getWorkingSetPath("resources")),
				() -> assertTrue(files.getUTF8Contents(PATH).indexOf("alpha") < files.getUTF8Contents(PATH).indexOf("zeta")),
				() -> assertEquals(Map.of("alpha", List.of(), "zeta", List.of("sol/forms/a.frm", "sol/forms/b.frm")), back));
		}

		@Test
		@DisplayName("null working sets or file access write nothing; a missing file or file access reads as no working sets")
		void nothing()
		{
			MemoryFileAccess files = new MemoryFileAccess();

			SolutionSerializer.serializeWorkingSetInfo(files, "resources", null);
			SolutionSerializer.serializeWorkingSetInfo(null, "resources", Map.of());

			assertAll(() -> assertEquals(Map.of(), files.contents()),
				() -> assertEquals(Map.of(), SolutionDeserializer.deserializeWorkingSets(files, "resources")),
				() -> assertEquals(Map.of(), SolutionDeserializer.deserializeWorkingSets(null, "resources")));
		}

		@Test
		@DisplayName("entries without name are skipped; a broken file reads as no working sets")
		void badContent() throws Exception
		{
			MemoryFileAccess files = new MemoryFileAccess();
			files.setUTF8Contents(PATH, "[{\"paths\":[\"x\"]}, null, {\"name\":\"ok\"}]");

			assertEquals(Map.of("ok", List.of()), SolutionDeserializer.deserializeWorkingSets(files, "resources"));

			files.setUTF8Contents(PATH, "not json");
			assertEquals(Map.of(), SolutionDeserializer.deserializeWorkingSets(files, "resources"));
		}
	}
}

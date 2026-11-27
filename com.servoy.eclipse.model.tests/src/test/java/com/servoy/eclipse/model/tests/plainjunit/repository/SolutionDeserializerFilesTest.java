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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionDeserializer.ObjBeforeJSExtensionComparator;
import com.servoy.eclipse.model.repository.StringResourceDeserializer;
import com.servoy.eclipse.model.tests.plainjunit.utils.MemoryFileAccess;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

/**
 * The file-level parts of {@link SolutionDeserializer} (root metadata, uuid lookup, json helpers, file ordering) and
 * {@link StringResourceDeserializer} (styles and templates in the resources project), on real files in a temp folder.
 */
class SolutionDeserializerFilesTest
{
	private static final UUID SOLUTION_UUID = UUID.fromString("7C783D6E-8E26-40B9-8BDA-E2DC4F2ECDF8");
	private static final UUID STYLE_UUID = UUID.fromString("1B2C3D4E-0000-40B9-8BDA-E2DC4F2ECDF8");

	private IDeveloperRepository repository;

	@BeforeEach
	void setUp() throws Exception
	{
		repository = mock(IDeveloperRepository.class);
		when(repository.createRootObjectMetaData(any(UUID.class), anyString(), anyInt(), anyInt(), anyInt()))
			.thenAnswer(invocation -> invocation.getArgument(2, Integer.class).intValue() == IRepository.SOLUTIONS
				? new SolutionMetaData(invocation.getArgument(0), invocation.getArgument(1), IRepository.SOLUTIONS, 1, 1)
				: new RootObjectMetaData(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2), 1, 1));
	}

	private static File write(File dir, String name, String content) throws IOException
	{
		dir.mkdirs();
		File file = new File(dir, name);
		Files.writeString(file.toPath(), content);
		return file;
	}

	@Nested
	@DisplayName("deserializeRootMetaData")
	class RootMetaData
	{
		private String rootMetadata(int fileVersion)
		{
			return "{uuid:\"" + SOLUTION_UUID + "\",typeid:" + IRepository.SOLUTIONS + ",name:\"x\",fileVersion:" + fileVersion +
				",solutionType:" + SolutionMetaData.MODULE + ",mustAuthenticate:true}";
		}

		@Test
		@DisplayName("reads uuid, type, solution type, mustAuthenticate and file version; the current version is not flagged changed")
		void currentVersion(@TempDir File project) throws Exception
		{
			write(project, "rootmetadata.obj", rootMetadata(AbstractRepository.repository_version));

			SolutionMetaData metadata = SolutionDeserializer.deserializeRootMetaData(repository, project, "sol");

			assertAll(() -> assertEquals(SOLUTION_UUID, metadata.getRootObjectUuid()), () -> assertEquals("sol", metadata.getName()),
				() -> assertEquals(SolutionMetaData.MODULE, metadata.getSolutionType()), () -> assertTrue(metadata.getMustAuthenticate()),
				() -> assertEquals(AbstractRepository.repository_version, metadata.getFileVersion()), () -> assertFalse(metadata.isChanged()));
		}

		@Test
		@DisplayName("an older file version is flagged changed so the solution gets written in the new format")
		void olderVersion(@TempDir File project) throws Exception
		{
			write(project, "rootmetadata.obj", rootMetadata(38));

			assertTrue(SolutionDeserializer.deserializeRootMetaData(repository, project, "sol").isChanged());
		}

		@Test
		@DisplayName("a missing file gives null; a missing or invalid version and broken json are repository exceptions")
		void errors(@TempDir File project) throws Exception
		{
			assertNull(SolutionDeserializer.deserializeRootMetaData(repository, project, "sol"));

			write(project, "rootmetadata.obj", "{uuid:\"" + SOLUTION_UUID + "\"}");
			assertEquals("Cannot handle files with unknown version",
				assertThrows(RepositoryException.class, () -> SolutionDeserializer.deserializeRootMetaData(repository, project, "sol")).getMessage());

			write(project, "rootmetadata.obj", rootMetadata(0));
			assertEquals("Cannot handle files with invalid version (<= 0)",
				assertThrows(RepositoryException.class, () -> SolutionDeserializer.deserializeRootMetaData(repository, project, "sol")).getMessage());

			write(project, "rootmetadata.obj", "{fileVersion:5");
			assertThrows(RepositoryException.class, () -> SolutionDeserializer.deserializeRootMetaData(repository, project, "sol"));
		}
	}

	@Nested
	@DisplayName("uuids from files")
	class Uuids
	{
		@Test
		@DisplayName("getUUID(File) reads the uuid of a json file; arrays and non-json files give null; a json without uuid gets a random one")
		void uuidOfFile(@TempDir File dir) throws Exception
		{
			File form = write(dir, "orders.frm", "{uuid:\"" + SOLUTION_UUID + "\",name:\"orders\"}");
			File medias = write(dir, "medias.obj", "[{uuid:\"" + SOLUTION_UUID + "\"}]");
			File js = write(dir, "orders.js", "var a;");
			File noUuid = write(dir, "x.obj", "{name:\"x\"}");
			File broken = write(dir, "broken.obj", "{name:");

			assertAll(() -> assertEquals(SOLUTION_UUID, SolutionDeserializer.getUUID(form)), () -> assertNull(SolutionDeserializer.getUUID(medias)),
				() -> assertNull(SolutionDeserializer.getUUID(js)), () -> assertNotNull(SolutionDeserializer.getUUID(noUuid)),
				() -> assertNull(SolutionDeserializer.getUUID(broken)));
		}

		@Test
		@DisplayName("getUUID(File, position) finds the uuid of the json object around a position")
		void uuidAtPosition(@TempDir File dir) throws Exception
		{
			String content = "{\nname:\"orders\",\nuuid:\"" + SOLUTION_UUID + "\",\nitems:[\n{\nname:\"btn\",\ntext:\"x\",\nuuid:\"" + STYLE_UUID +
				"\"\n}\n]\n}";
			File form = write(dir, "orders.frm", content);

			assertAll(() -> assertEquals(STYLE_UUID.toString(), SolutionDeserializer.getUUID(form, content.indexOf("text"))),
				() -> assertEquals(SOLUTION_UUID.toString(), SolutionDeserializer.getUUID(form, content.indexOf("name:\"orders\""))),
				() -> assertNull(SolutionDeserializer.getUUID(form, -1)), () -> assertNull(SolutionDeserializer.getUUID(form, content.length() + 5)),
				() -> assertNull(SolutionDeserializer.getUUID(write(dir, "a.js", "var a;"), 1)));
		}

		@Test
		@DisplayName("getJSONObject / getJSONArray parse, or give null for broken content")
		void jsonHelpers()
		{
			assertAll(() -> assertEquals("x", SolutionDeserializer.getJSONObject("{name:\"x\"}").getString("name")),
				() -> assertNull(SolutionDeserializer.getJSONObject("{name:")), () -> assertEquals(2, SolutionDeserializer.getJSONArray("[1,2]").length()),
				() -> assertNull(SolutionDeserializer.getJSONArray("[1,")));
		}

		@Test
		@DisplayName("json files sort before other files, each group by name")
		void objBeforeJs()
		{
			String[] names = { "b.js", "b.frm", "a.js", "a.obj", "c.rel" };

			Arrays.sort(names, new ObjBeforeJSExtensionComparator());

			assertAll(() -> assertArrayEquals(new String[] { "a.obj", "b.frm", "c.rel", "a.js", "b.js" }, names),
				() -> assertEquals(0, new ObjBeforeJSExtensionComparator().compare(null, "a.js")));
		}
	}

	@Nested
	@DisplayName("StringResourceDeserializer")
	class StringResources
	{
		@Test
		@DisplayName("style and template paths are in the styles and templates folders of the resources project")
		void paths()
		{
			assertAll(() -> assertEquals("res/styles/main.css", StringResourceDeserializer.getStringResourceContentFilePath("res", "main", IRepository.STYLES)),
				() -> assertEquals("res/styles/main.obj", StringResourceDeserializer.getStringResourceObjFilePath("res", "main", IRepository.STYLES)),
				() -> assertEquals("res/templates/t.template",
					StringResourceDeserializer.getStringResourceContentFilePath("res", "t", IRepository.TEMPLATES)),
				() -> assertEquals("res/templates/t.obj", StringResourceDeserializer.getStringResourceObjFilePath("res", "t", IRepository.TEMPLATES)),
				() -> assertThrows(IllegalArgumentException.class,
					() -> StringResourceDeserializer.getStringResourceContentFilePath("res", "x", IRepository.FORMS)),
				() -> assertThrows(IllegalArgumentException.class,
					() -> StringResourceDeserializer.getStringResourceObjFilePath("res", "x", IRepository.FORMS)));
		}

		private Style style(String content)
		{
			Style style = new Style(repository, new RootObjectMetaData(STYLE_UUID, "main", IRepository.STYLES, 1, 1));
			style.setContent(content);
			return style;
		}

		@Test
		@DisplayName("writeStringResource writes the .obj metadata and the content (empty when null)")
		void writeResource() throws Exception
		{
			MemoryFileAccess files = new MemoryFileAccess();

			StringResourceDeserializer.writeStringResource(style("body {}"), files, "res");
			// written without the outer braces, as deserializeMetadata reads it
			JSONObject metadata = new ServoyJSONObject(files.getUTF8Contents("res/styles/main.obj"), true);
			StringResourceDeserializer.writeStringResource(style(null), files, "res2");

			assertAll(() -> assertEquals("body {}", files.getUTF8Contents("res/styles/main.css")),
				() -> assertEquals(STYLE_UUID.toString(), metadata.getString("uuid")), () -> assertEquals(IRepository.STYLES, metadata.getInt("typeid")),
				() -> assertEquals("main", metadata.getString("name")), () -> assertEquals("", files.getUTF8Contents("res2/styles/main.css")));
		}

		@Test
		@DisplayName("deleteStringResource deletes both files and rethrows a failed delete after trying both")
		void delete() throws Exception
		{
			MemoryFileAccess files = new MemoryFileAccess();
			StringResourceDeserializer.writeStringResource(style("x"), files, "res");

			StringResourceDeserializer.deleteStringResource(style("x"), files, "res");
			assertEquals(List.of(), List.copyOf(files.contents().keySet()));

			StringResourceDeserializer.writeStringResource(style("x"), files, "res");
			files.failDeleteOf("res/styles/main.css");
			assertThrows(IOException.class, () -> StringResourceDeserializer.deleteStringResource(style("x"), files, "res"));
			assertEquals(List.of("res/styles/main.css"), List.copyOf(files.contents().keySet()));
		}

		@Test
		@DisplayName("deserializeMetadatas reads the .obj files of the resource type, skipping other files and other types")
		void metadatas(@TempDir File project) throws Exception
		{
			File styles = new File(project, "styles");
			// string resource .obj files are stored without the outer braces
			write(styles, "main.obj", "uuid:\"" + STYLE_UUID + "\",typeid:" + IRepository.STYLES + ",name:\"main\"");
			write(styles, "main.css", "body {}");
			write(styles, "wrong.obj", "uuid:\"" + SOLUTION_UUID + "\",typeid:" + IRepository.TEMPLATES);

			RootObjectMetaData[] result = StringResourceDeserializer.deserializeMetadatas(repository, project, "res", IRepository.STYLES);

			assertAll(() -> assertEquals(1, result.length), () -> assertEquals("main", result[0].getName()),
				() -> assertEquals(STYLE_UUID, result[0].getRootObjectUuid()),
				() -> assertEquals(0, StringResourceDeserializer.deserializeMetadatas(repository, project, "res", IRepository.TEMPLATES).length),
				() -> assertEquals(0, StringResourceDeserializer.deserializeMetadatas(repository, null, "res", IRepository.STYLES).length),
				() -> assertThrows(IllegalArgumentException.class,
					() -> StringResourceDeserializer.deserializeMetadatas(repository, project, "res", IRepository.FORMS)));
		}

		@Test
		@DisplayName("deserializeMetadata ignores non-.obj and missing files, and wraps broken json")
		void metadata(@TempDir File dir) throws Exception
		{
			assertAll(() -> assertNull(StringResourceDeserializer.deserializeMetadata(repository, write(dir, "a.css", "x"), IRepository.STYLES)),
				() -> assertNull(StringResourceDeserializer.deserializeMetadata(repository, new File(dir, "missing.obj"), IRepository.STYLES)),
				() -> assertNull(StringResourceDeserializer.deserializeMetadata(repository, null, IRepository.STYLES)),
				() -> assertNull(StringResourceDeserializer.deserializeMetadata(repository, write(dir, ".obj", "{}"), IRepository.STYLES)),
				() -> assertThrows(RepositoryException.class,
					() -> StringResourceDeserializer.deserializeMetadata(repository, write(dir, "b.obj", "{uuid:"), IRepository.STYLES)));
		}

		@Test
		@DisplayName("readStringResource creates the resource and loads its content file next to the project")
		void read(@TempDir File workspace) throws Exception
		{
			File project = new File(workspace, "res");
			write(new File(project, "styles"), "main.css", "body { color: red; }");
			RootObjectMetaData metadata = new RootObjectMetaData(STYLE_UUID, "main", IRepository.STYLES, 1, 1);
			Style created = new Style(repository, metadata);
			when(repository.createRootObject(metadata)).thenReturn(created);

			Style read = (Style)StringResourceDeserializer.readStringResource(repository, project, "res", metadata);

			assertAll(() -> assertSame(created, read), () -> assertEquals("body { color: red; }", read.getContent()),
				() -> assertNotNull(read.getChangeHandler()));
		}
	}
}

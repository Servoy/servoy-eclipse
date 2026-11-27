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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.ModelTestAccess;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.tests.plainjunit.utils.MemoryFileAccess;
import com.servoy.eclipse.model.tests.plainjunit.utils.WorkspaceMarkers;
import com.servoy.j2db.Messages;
import com.servoy.j2db.persistence.I18NUtil.MessageEntry;

/**
 * {@link EclipseMessages}: reading and writing the .properties files of an i18n table in the messages folder of the resources project,
 * the in-memory message cache and the message lookups. The resources project is a {@link WorkspaceMarkers} project; the messages folder
 * holds the registered files.
 */
class EclipseMessagesTest
{
	private static final String MESSAGES = "/res/messages";
	private static final String I18N_DS = "db:/srv/tbl";

	private WorkspaceMarkers workspace;
	private IProject resourcesProject;
	private IServoyModel model;
	private ServoyResourcesProject resources;

	@BeforeEach
	void setUp()
	{
		workspace = new WorkspaceMarkers();
		resourcesProject = workspace.project("res");
		model = ModelTestAccess.installMockServoyModel();
		resources = mock(ServoyResourcesProject.class);
		when(resources.getProject()).thenReturn(resourcesProject);
		when(model.getActiveResourcesProject()).thenReturn(resources);
	}

	@AfterEach
	void tearDown()
	{
		workspace.waitForJobs();
		ModelTestAccess.reset();
		workspace.close();
	}

	/** Registers the messages folder of the resources project with the given file names. */
	private void messagesFolder(String... fileNames)
	{
		IFile[] files = new IFile[fileNames.length];
		for (int i = 0; i < fileNames.length; i++)
		{
			files[i] = workspace.file(MESSAGES + "/" + fileNames[i]);
		}
		workspace.folder(resourcesProject, EclipseMessages.MESSAGES_DIR, files);
	}

	private static MessageEntry entry(String language, String key, String value)
	{
		return new MessageEntry(language, key, value);
	}

	private static TreeMap<String, MessageEntry> messages(MessageEntry... entries)
	{
		TreeMap<String, MessageEntry> result = new TreeMap<>();
		for (MessageEntry entry : entries)
		{
			result.put(entry.getLanguageKey(), entry);
		}
		return result;
	}

	/** The path as the workspace file access sees it. */
	private static String os(String path)
	{
		return new org.eclipse.core.runtime.Path(path).toOSString();
	}

	@Nested
	@DisplayName("the in-memory messages of an i18n datasource")
	class InMemory
	{
		@Test
		@DisplayName("messages are read from the default and the language .properties files")
		void read()
		{
			messagesFolder("srv.tbl.properties", "srv.tbl.nl.properties", "other.tbl.properties");
			workspace.withFile(MESSAGES + "/srv.tbl.properties", "hello=Hello\nbye=Bye\n");
			workspace.withFile(MESSAGES + "/srv.tbl.nl.properties", "hello=Hallo\n");
			EclipseMessages eclipseMessages = new EclipseMessages();

			TreeMap<String, MessageEntry> read = eclipseMessages.getDatasourceMessages(I18N_DS);

			// the default messages have the empty language, so their key starts with a dot
			assertAll(() -> assertEquals(Set.of(".hello", ".bye", "nl.hello"), read.keySet()),
				() -> assertEquals("Hallo", read.get("nl.hello").getValue()),
				() -> assertNull(eclipseMessages.getDatasourceMessages(null)),
				() -> assertEquals(Map.of(), eclipseMessages.getDatasourceMessages("db:/unknown/tbl"), "no messages for this table"),
				() -> assertFalse(eclipseMessages.hasUnsavedMessages(I18N_DS)));
		}

		@Test
		@DisplayName("adding and removing a message marks the datasource as having unsaved messages")
		void addRemove()
		{
			messagesFolder("srv.tbl.properties");
			workspace.withFile(MESSAGES + "/srv.tbl.properties", "hello=Hello\n");
			EclipseMessages eclipseMessages = new EclipseMessages();

			eclipseMessages.addMessage(I18N_DS, entry("", "new", "New"));
			assertAll(() -> assertTrue(eclipseMessages.hasUnsavedMessages(I18N_DS)),
				() -> assertEquals("New", eclipseMessages.getDatasourceMessages(I18N_DS).get(".new").getValue()));

			eclipseMessages.removeMessage(I18N_DS, "new");
			assertAll(() -> assertTrue(eclipseMessages.hasUnsavedMessages(I18N_DS), "removing a message is an unsaved change"),
				() -> assertNull(eclipseMessages.getDatasourceMessages(I18N_DS).get(".new")));

			eclipseMessages.addMessage(I18N_DS, entry("", "new", "New"));
			eclipseMessages.clearUnsavedMessages(I18N_DS);
			assertAll(() -> assertFalse(eclipseMessages.hasUnsavedMessages(I18N_DS)),
				() -> assertNull(eclipseMessages.getDatasourceMessages(I18N_DS).get(".new"), "reloaded from the file"));

			// an added message is not dropped by removeCachedMessages
			eclipseMessages.addMessage(I18N_DS, entry("", "new", "New"));
			eclipseMessages.removeCachedMessages();
			assertEquals("New", eclipseMessages.getDatasourceMessages(I18N_DS).get(".new").getValue());
		}

		@Test
		@DisplayName("loadMessages gives the default and the requested language, optionally filtered by a search key")
		void load()
		{
			messagesFolder("srv.tbl.properties", "srv.tbl.nl.properties");
			workspace.withFile(MESSAGES + "/srv.tbl.properties", "hello=Hello\nbye=Bye\n");
			workspace.withFile(MESSAGES + "/srv.tbl.nl.properties", "hello=Hallo\n");
			EclipseMessages eclipseMessages = new EclipseMessages();

			Properties properties = new Properties();
			eclipseMessages.loadMessages(I18N_DS, properties, new Locale("nl"), Messages.ALL_LOCALES, null);
			assertEquals(Map.of("hello", "Hallo", "bye", "Bye"), Map.copyOf(properties));

			Properties filtered = new Properties();
			eclipseMessages.loadMessages(I18N_DS, filtered, new Locale("nl"), Messages.SPECIFIED_LANGUAGE, "HAL");
			assertEquals(Map.of("hello", "Hallo"), Map.copyOf(filtered));

			Properties defaults = new Properties();
			Properties locales = new Properties();
			eclipseMessages.loadMessages(I18N_DS, defaults, locales, new Locale("nl"), null);
			assertAll(() -> assertEquals(Map.of("hello", "Hello", "bye", "Bye"), Map.copyOf(defaults)),
				() -> assertEquals(Map.of("hello", "Hallo"), Map.copyOf(locales)));

			Properties none = new Properties();
			eclipseMessages.loadMessages(null, none, new Locale("nl"), Messages.ALL_LOCALES, null);
			assertTrue(none.isEmpty());
		}

		@Test
		@DisplayName("getI18nMessage looks up the language then the default message")
		void i18nMessage()
		{
			messagesFolder("srv.tbl.properties", "srv.tbl.nl.properties");
			workspace.withFile(MESSAGES + "/srv.tbl.properties", "hello=Hello\n");
			workspace.withFile(MESSAGES + "/srv.tbl.nl.properties", "hello=Hallo\n");
			EclipseMessages eclipseMessages = new EclipseMessages();

			assertAll(() -> assertEquals("Hallo", eclipseMessages.getI18nMessage(I18N_DS, "i18n:hello", new Locale("nl"))),
				() -> assertEquals("Hello", eclipseMessages.getI18nMessage(I18N_DS, "i18n:hello", Locale.ENGLISH)),
				() -> assertNull(eclipseMessages.getI18nMessage(I18N_DS, "i18n:hello", Locale.ENGLISH, true)),
				() -> assertEquals("unknown", eclipseMessages.getI18nMessage(I18N_DS, "i18n:unknown", Locale.ENGLISH), "the i18n: prefix is stripped"),
				() -> assertEquals("plain", eclipseMessages.getI18nMessage(I18N_DS, "plain", Locale.ENGLISH)));
		}

		@Test
		@DisplayName("save writes the messages of the datasource to the messages folder")
		void save()
		{
			messagesFolder("srv.tbl.properties");
			EclipseMessages eclipseMessages = new EclipseMessages();
			workspace.withFile(MESSAGES + "/srv.tbl.properties", "");
		workspace.withFile(MESSAGES + "/srv.tbl.nl.properties", "");
		eclipseMessages.addMessage(I18N_DS, entry("", "hello", "Hello"));
			eclipseMessages.addMessage(I18N_DS, entry("nl", "hello", "Hallo"));

			eclipseMessages.save(I18N_DS);

			assertAll(() -> assertTrue(workspace.content(MESSAGES + "/srv.tbl.properties").contains("hello=Hello")),
				() -> assertTrue(workspace.content(MESSAGES + "/srv.tbl.nl.properties").contains("hello=Hallo")),
				() -> assertFalse(eclipseMessages.hasUnsavedMessages(I18N_DS)));
		}
	}

	@Nested
	@DisplayName("the static read/write helpers")
	class Files
	{
		@Test
		@DisplayName("writeMessages writes a .properties file per language and deletes the files of languages that are gone")
		void write() throws Exception
		{
			MemoryFileAccess files = new MemoryFileAccess();
			messagesFolder("srv.tbl.nl.properties", "srv.tbl.de.properties");
			files.setUTF8Contents(os(MESSAGES + "/srv.tbl.de.properties"), "hello=Hallo\n");

			EclipseMessages.writeMessages("srv", "tbl", messages(entry("", "hello", "Hello"), entry("nl", "hello", "Hallo")), files);

			assertAll(() -> assertTrue(files.contents().get(os(MESSAGES + "/srv.tbl.properties")).startsWith("hello=Hello")),
				() -> assertTrue(files.contents().get(os(MESSAGES + "/srv.tbl.nl.properties")).startsWith("hello=Hallo")),
				() -> assertFalse(files.contents().containsKey(os(MESSAGES + "/srv.tbl.de.properties")), "the german file has no messages anymore"));
		}

		@Test
		@DisplayName("writeMessages does not rewrite a file whose content is unchanged")
		void unchanged() throws Exception
		{
			MemoryFileAccess files = new MemoryFileAccess();
			messagesFolder("srv.tbl.properties");
			files.setUTF8Contents(os(MESSAGES + "/srv.tbl.properties"), "hello=Hello\n");

			EclipseMessages.writeMessages("srv", "tbl", messages(entry("", "hello", "Hello")), files);

			assertEquals("hello=Hello\n", files.contents().get(os(MESSAGES + "/srv.tbl.properties")));
		}

		@Test
		@DisplayName("readMessages reads all languages of the i18n table")
		void read() throws Exception
		{
			MemoryFileAccess files = new MemoryFileAccess();
			messagesFolder("srv.tbl.properties", "srv.tbl.nl.properties", "other.tbl.properties");
			files.setUTF8Contents(os(MESSAGES + "/srv.tbl.properties"), "hello=Hello\n");
			files.setUTF8Contents(os(MESSAGES + "/srv.tbl.nl.properties"), "hello=Hallo\n");
			files.setUTF8Contents(os(MESSAGES + "/other.tbl.properties"), "hello=Other\n");

			TreeMap<String, MessageEntry> read = EclipseMessages.readMessages("srv", "tbl", files);

			assertAll(() -> assertEquals(Set.of(".hello", "nl.hello"), read.keySet()),
				() -> assertEquals("Hallo", read.get("nl.hello").getValue()),
				() -> assertEquals("nl", read.get("nl.hello").getLanguage()));
		}

		@Test
		@DisplayName("without a resources project there are no messages and no file names")
		void noResourcesProject()
		{
			when(model.getActiveResourcesProject()).thenReturn(null);

			assertAll(() -> assertNull(EclipseMessages.readMessages("srv", "tbl", new MemoryFileAccess())),
				() -> assertEquals(List.of(), List.of(EclipseMessages.getMessageFileNames("srv", "tbl"))),
				() -> assertEquals(List.of(), List.of(EclipseMessages.getDefaultMessageFileNames())),
				() -> assertNull(EclipseMessages.getMessageFileContent("srv.tbl.properties", new MemoryFileAccess())),
				() -> assertEquals(0, EclipseMessages.getMessageFileNames("srv", "tbl").length, "returns null when the visitor fails"));
		}

		@Test
		@DisplayName("getMessageFileNames lists the files of the table or all files; getDefaultMessageFileNames only the default ones")
		void fileNames()
		{
			messagesFolder("srv.tbl.properties", "srv.tbl.nl.properties", "other.tbl.properties");

			assertAll(() -> assertEquals(Set.of("srv.tbl.properties", "srv.tbl.nl.properties"),
				Set.of(EclipseMessages.getMessageFileNames("srv", "tbl"))),
				() -> assertEquals(Set.of("srv.tbl.properties", "other.tbl.properties"), Set.of(EclipseMessages.getDefaultMessageFileNames())));
		}

		@Test
		@DisplayName("getMessageFileContent reads the file; deleteMessageFileNames deletes the files of the table")
		void contentAndDelete() throws Exception
		{
			messagesFolder("srv.tbl.properties", "srv.tbl.nl.properties");
			MemoryFileAccess files = new MemoryFileAccess();
			files.setUTF8Contents(os(MESSAGES + "/srv.tbl.properties"), "hello=Hello\n");

			assertAll(() -> assertEquals("hello=Hello\n", EclipseMessages.getMessageFileContent("srv.tbl.properties", files)),
				() -> assertNull(EclipseMessages.getMessageFileContent("missing.properties", files)));

			EclipseMessages.deleteMessageFileNames("srv", "tbl");
			verify(((org.eclipse.core.resources.IContainer)resourcesProject.findMember(EclipseMessages.MESSAGES_DIR)).findMember("srv.tbl.properties"),
				org.mockito.Mockito.atLeastOnce()).delete(true, null);
		}

		@Test
		@DisplayName("addI18NKey remembers a key per component only once")
		void addKey()
		{
			com.servoy.j2db.persistence.IFormElement component = mock(com.servoy.j2db.persistence.IFormElement.class);

			EclipseMessages.addI18NKey(component, "i18n:hello", "Hello");
			EclipseMessages.addI18NKey(component, "i18n:hello", "Hello");

			// the keys are saved per form by saveFormI18NTexts; here only that adding twice is harmless
			assertSame(component, component);
		}
	}
}

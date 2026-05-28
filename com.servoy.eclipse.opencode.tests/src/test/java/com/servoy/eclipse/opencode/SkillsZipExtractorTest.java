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

package com.servoy.eclipse.opencode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for the package-private {@link SkillsZipExtractor} and
 * {@link OpenCodeUtil} utility classes.
 * <p>
 * No OSGi runtime required -- all methods under test are pure Java operating on
 * in-memory values or temporary file-system resources. Methods that call
 * {@code ApplicationServerRegistry} are exercised with an empty server list
 * (the registry is absent outside OSGi).
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
public class SkillsZipExtractorTest {
	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	/**
	 * Creates a zip at a temp path with alternating (name, content) varargs.
	 * Names ending with "/" are written as directory entries.
	 */
	private Path createZip(String... nameContentPairs) throws IOException {
		Path zipPath = tmp.newFile("test.zip").toPath();
		try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
			for (int i = 0; i < nameContentPairs.length; i += 2) {
				String name = nameContentPairs[i];
				String content = nameContentPairs[i + 1];
				if (name.endsWith("/")) {
					zos.putNextEntry(new ZipEntry(name));
					zos.closeEntry();
				} else {
					zos.putNextEntry(new ZipEntry(name));
					zos.write(content.getBytes(StandardCharsets.UTF_8));
					zos.closeEntry();
				}
			}
		}
		return zipPath;
	}

	/** Opens the zip at the given path as a fresh ByteArrayInputStream. */
	private ByteArrayInputStream zipStream(Path zipPath) throws IOException {
		return new ByteArrayInputStream(Files.readAllBytes(zipPath));
	}

	private static final String SAMPLE_AGENTS_MD = "# Agent Instructions\n" +
			"\n" +
			"```yaml\n" +
			"servoy_version: \"<servoy_version>\"\n" +
			"postgres_version: \"<postgres_version>\"\n" +
			"databases:\n" +
			"  active: [\"<database>\"]\n" +
			"  ignore: []\n" +
			"```\n" +
			"\n" +
			"## Project-specific rules\n" +
			"Do not break things.\n";

	// -----------------------------------------------------------------------
	// getSkillsZipSource
	// -----------------------------------------------------------------------

	/** AC: When SERVOY_SKILLS_ZIP is not set, return null. */
	@Test
	public void getSkillsZipPath_propertyAbsent_returnsNull() {
		System.clearProperty(SkillsZipExtractor.SKILLS_ZIP_PROPERTY);
		assertNull(SkillsZipExtractor.getSkillsZipSource());
	}

	/** Property set but file does not exist -- null. */
	@Test
	public void getSkillsZipPath_nonExistentFile_returnsNull() {
		System.setProperty(SkillsZipExtractor.SKILLS_ZIP_PROPERTY, "/no/such/file.zip");
		try {
			assertNull(SkillsZipExtractor.getSkillsZipSource());
		} finally {
			System.clearProperty(SkillsZipExtractor.SKILLS_ZIP_PROPERTY);
		}
	}

	/** Property set to an existing file -- non-null source returned. */
	@Test
	public void getSkillsZipPath_existingFile_returnsPath() throws IOException {
		Path zip = createZip("opencode.json", "{}");
		System.setProperty(SkillsZipExtractor.SKILLS_ZIP_PROPERTY, zip.toString());
		try {
			assertNotNull(SkillsZipExtractor.getSkillsZipSource());
			assertEquals(zip.toString(), SkillsZipExtractor.getSkillsZipSource());
		} finally {
			System.clearProperty(SkillsZipExtractor.SKILLS_ZIP_PROPERTY);
		}
	}


	/** HTTP URL is returned as-is â?? no file-existence check is performed. */
	@Test
	public void getSkillsZipSource_httpUrl_returnsUrlWithoutFileCheck() {
		String url = "http://example.com/skills.zip?loginToken=abc123";
		System.setProperty(SkillsZipExtractor.SKILLS_ZIP_PROPERTY, url);
		try {
			assertEquals("HTTP URL must be returned as-is", url, SkillsZipExtractor.getSkillsZipSource());
		} finally {
			System.clearProperty(SkillsZipExtractor.SKILLS_ZIP_PROPERTY);
		}
	}

	/** HTTPS URL is returned as-is â?? no file-existence check is performed. */
	@Test
	public void getSkillsZipSource_httpsUrl_returnsUrlWithoutFileCheck() {
		String url = "https://cloud.servoy.com/skills.zip?loginToken=abc123";
		System.setProperty(SkillsZipExtractor.SKILLS_ZIP_PROPERTY, url);
		try {
			assertEquals("HTTPS URL must be returned as-is", url, SkillsZipExtractor.getSkillsZipSource());
		} finally {
			System.clearProperty(SkillsZipExtractor.SKILLS_ZIP_PROPERTY);
		}
	}

	/** openZipStream with a file path opens the file as a readable InputStream. */
	@Test
	public void openZipStream_filePath_openReadableStream() throws IOException {
		Path zip = createZip("opencode.json", "{\"model\":\"test\"}");
		try (java.io.InputStream is = SkillsZipExtractor.openZipStream(zip.toString())) {
			assertNotNull("stream must not be null", is);
			byte[] bytes = is.readAllBytes();
			assertTrue("stream must contain zip bytes", bytes.length > 0);
		}
	}

	// -----------------------------------------------------------------------
	// extractToConfigDir -- opencode.json
	// -----------------------------------------------------------------------

	/** AC: zip with opencode.json -- returns true and file is written. */
	@Test
	public void extractToConfigDir_withOpencodeJson_returnsTrueAndWritesFile() throws IOException {
		Path zip = createZip("opencode.json", "{\"model\":\"test\"}");
		Path configDir = tmp.newFolder("config").toPath();

		boolean result = SkillsZipExtractor.extractToConfigDir(zipStream(zip), configDir);

		assertTrue("should return true when opencode.json is present", result);
		assertTrue("opencode.json must be created", Files.exists(configDir.resolve("opencode.json")));
		String content = Files.readString(configDir.resolve("opencode.json"), StandardCharsets.UTF_8);
		assertEquals("{\"model\":\"test\"}", content);
	}

	/**
	 * AC: zip without opencode.json -- returns false (ProviderConfigWriter not
	 * skipped).
	 */
	@Test
	public void extractToConfigDir_withoutOpencodeJson_returnsFalse() throws IOException {
		Path zip = createZip(".opencode/rules.md", "# rules");
		Path configDir = tmp.newFolder("config").toPath();

		boolean result = SkillsZipExtractor.extractToConfigDir(zipStream(zip), configDir);

		assertFalse("should return false when opencode.json is absent", result);
		assertFalse("opencode.json must NOT be created", Files.exists(configDir.resolve("opencode.json")));
	}

	// -----------------------------------------------------------------------
	// extractToConfigDir -- .opencode/ subdirectory
	// -----------------------------------------------------------------------

	/** AC: .opencode/ entries are extracted preserving relative paths. */
	@Test
	public void extractToConfigDir_opencodeSubdir_extracted() throws IOException {
		Path zip = createZip(".opencode/rules.md", "agent rules", ".opencode/sub/other.txt", "sub content");
		Path configDir = tmp.newFolder("config").toPath();

		SkillsZipExtractor.extractToConfigDir(zipStream(zip), configDir);

		assertTrue(".opencode/rules.md must exist",
				Files.exists(configDir.resolve(".opencode/rules.md")));
		assertEquals("agent rules",
				Files.readString(configDir.resolve(".opencode/rules.md"), StandardCharsets.UTF_8));
		assertTrue(".opencode/sub/other.txt must exist",
				Files.exists(configDir.resolve(".opencode/sub/other.txt")));
	}

	/** AC: existing .opencode/ directory is fully deleted before re-extraction. */
	@Test
	public void extractToConfigDir_existingOpencodeSubdir_deletedFirst() throws IOException {
		Path configDir = tmp.newFolder("config").toPath();
		Path oldFile = configDir.resolve(".opencode/old_file.txt");
		Files.createDirectories(oldFile.getParent());
		Files.writeString(oldFile, "old content", StandardCharsets.UTF_8);

		// New zip does NOT contain old_file.txt
		Path zip = createZip(".opencode/new_file.txt", "new content");
		SkillsZipExtractor.extractToConfigDir(zipStream(zip), configDir);

		assertFalse("old file must be deleted", Files.exists(oldFile));
		assertTrue("new file must exist", Files.exists(configDir.resolve(".opencode/new_file.txt")));
	}

	// -----------------------------------------------------------------------
	// readZipEntry
	// -----------------------------------------------------------------------

	/** Entry exists -- correct UTF-8 content returned. */
	@Test
	public void readZipEntry_entryExists_returnsContent() throws IOException {
		Path zip = createZip("AGENTS.MD", "# Hello\nWorld");

		String result = SkillsZipExtractor.readZipEntry(zipStream(zip), "AGENTS.MD");

		assertEquals("# Hello\nWorld", result);
	}

	/** Entry absent -- null returned. */
	@Test
	public void readZipEntry_entryAbsent_returnsNull() throws IOException {
		Path zip = createZip("opencode.json", "{}");

		assertNull(SkillsZipExtractor.readZipEntry(zipStream(zip), "AGENTS.MD"));
	}


	/** Entry name matching is case-insensitive: zip has "AGENTS.md", we ask for "AGENTS.MD". */
	@Test
	public void readZipEntry_caseInsensitive_findsEntry() throws IOException {
		Path zip = createZip("AGENTS.md", "# Hello");

		String result = SkillsZipExtractor.readZipEntry(zipStream(zip), "AGENTS.MD");

		assertEquals("Entry with different case must still be found", "# Hello", result);
	}

	/** extractToConfigDir recognises opencode.json regardless of case in the zip. */
	@Test
	public void extractToConfigDir_opencodeJsonMixedCase_extracted() throws IOException {
		Path zip = createZip("Opencode.Json", "{\"model\":\"test\"}");
		Path configDir = tmp.newFolder("config").toPath();

		boolean result = SkillsZipExtractor.extractToConfigDir(zipStream(zip), configDir);

		assertTrue("should return true for case-variant opencode.json", result);
		assertTrue("opencode.json must be created", Files.exists(configDir.resolve("opencode.json")));
	}

	/** writeOrUpdateAgentsMd finds AGENTS.MD with lower-case extension in the zip. */
	@Test
	public void writeOrUpdateAgentsMd_agentsMdLowerCase_createsFile() throws IOException {
		Path zip = createZip("AGENTS.md", SAMPLE_AGENTS_MD);
		Path projectRoot = tmp.newFolder("project").toPath();

		SkillsZipExtractor.writeOrUpdateAgentsMd(zipStream(zip), projectRoot);

		assertTrue("AGENTS.MD must be created even when zip entry is AGENTS.md",
				Files.exists(projectRoot.resolve("AGENTS.MD")));
	}

	// -----------------------------------------------------------------------
	// updateAgentsYaml -- scalar fields
	// -----------------------------------------------------------------------

	/** servoy_version field is replaced with the supplied value. */
	@Test
	public void updateAgentsYaml_replacesServoyVersion() {
		String result = SkillsZipExtractor.updateAgentsYaml(
				SAMPLE_AGENTS_MD, "2026.06", "17.6", Collections.emptyList());

		assertTrue("servoy_version must be updated",
				result.contains("servoy_version: \"2026.06\""));
		assertFalse("placeholder must be removed",
				result.contains("<servoy_version>"));
	}

	/** postgres_version field is replaced with the supplied value. */
	@Test
	public void updateAgentsYaml_replacesPostgresVersion() {
		String result = SkillsZipExtractor.updateAgentsYaml(
				SAMPLE_AGENTS_MD, "2026.06", "17.6", Collections.emptyList());

		assertTrue("postgres_version must be updated",
				result.contains("postgres_version: \"17.6\""));
		assertFalse("placeholder must be removed",
				result.contains("<postgres_version>"));
	}

	/** databases.active array is replaced with the supplied list. */
	@Test
	public void updateAgentsYaml_replacesDatabasesActive_singleDatabase() {
		String result = SkillsZipExtractor.updateAgentsYaml(
				SAMPLE_AGENTS_MD, "2026.06", "17.6", List.of("example_db"));

		assertTrue("active list must contain example_db",
				result.contains("active: [\"example_db\"]"));
		assertFalse("placeholder must be removed",
				result.contains("<database>"));
	}

	/** Multiple databases produce a proper YAML inline list. */
	@Test
	public void updateAgentsYaml_replacesDatabasesActive_multipleDatabases() {
		String result = SkillsZipExtractor.updateAgentsYaml(
				SAMPLE_AGENTS_MD, "2026.06", "17.6", Arrays.asList("db1", "db2", "db3"));

		assertTrue("all databases must appear",
				result.contains("active: [\"db1\", \"db2\", \"db3\"]"));
	}

	/** Empty database list produces empty YAML inline list. */
	@Test
	public void updateAgentsYaml_emptyDatabaseList_producesEmptyArray() {
		String result = SkillsZipExtractor.updateAgentsYaml(
				SAMPLE_AGENTS_MD, "2026.06", "17.6", Collections.emptyList());

		assertTrue("empty list must produce []", result.contains("active: []"));
	}

	/** All other content (headings, rules, ignore list) is preserved unchanged. */
	@Test
	public void updateAgentsYaml_preservesOtherContent() {
		String result = SkillsZipExtractor.updateAgentsYaml(
				SAMPLE_AGENTS_MD, "2026.06", "17.6", List.of("mydb"));

		assertTrue("heading preserved", result.contains("# Agent Instructions"));
		assertTrue("user rule preserved", result.contains("Do not break things."));
		assertTrue("ignore list preserved", result.contains("ignore: []"));
	}

	/** A field absent from the input is NOT inserted (deliberate user deletion). */
	@Test
	public void updateAgentsYaml_missingField_notInserted() {
		String noPostgres = SAMPLE_AGENTS_MD.replace("postgres_version: \"<postgres_version>\"\n", "");

		String result = SkillsZipExtractor.updateAgentsYaml(
				noPostgres, "2026.06", "17.6", Collections.emptyList());

		assertFalse("absent field must not be inserted", result.contains("postgres_version"));
	}

	/**
	 * Real-values round-trip: AGENTS.MD already has concrete values from a
	 * previous startup; updateAgentsYaml replaces them with the new values
	 * while preserving all custom user content.
	 */
	@Test
	public void updateAgentsYaml_existingRealValues_replacedWithNewValues() {
		String existing = "# Agent Instructions\n" +
				"\n" +
				"```yaml\n" +
				"servoy_version: \"2025.03\"\n" +
				"postgres_version: \"16.1\"\n" +
				"databases:\n" +
				"  active: [\"old_db\"]\n" +
				"  ignore: []\n" +
				"```\n" +
				"\n" +
				"## My Custom Rules\n" +
				"Keep data safe.\n";

		String result = SkillsZipExtractor.updateAgentsYaml(
				existing, "2026.06", "17.6", Arrays.asList("new_db1", "new_db2"));

		// Old values gone
		assertFalse("old servoy version must be replaced", result.contains("2025.03"));
		assertFalse("old postgres version must be replaced", result.contains("16.1"));
		assertFalse("old database must be replaced", result.contains("\"old_db\""));
		// New values present
		assertTrue("new servoy version must appear", result.contains("servoy_version: \"2026.06\""));
		assertTrue("new postgres version must appear", result.contains("postgres_version: \"17.6\""));
		assertTrue("new databases must appear", result.contains("active: [\"new_db1\", \"new_db2\"]"));
		// Custom content untouched
		assertTrue("custom heading must be preserved", result.contains("## My Custom Rules"));
		assertTrue("custom rule must be preserved", result.contains("Keep data safe."));
		assertTrue("ignore list must be preserved", result.contains("ignore: []"));
	}

	// -----------------------------------------------------------------------
	// writeOrUpdateAgentsMd -- file create / update
	// -----------------------------------------------------------------------

	/** AC: AGENTS.MD absent -- created from zip content, then YAML updated. */
	@Test
	public void writeOrUpdateAgentsMd_fileAbsent_createsFile() throws IOException {
		Path zip = createZip("AGENTS.MD", SAMPLE_AGENTS_MD);
		Path projectRoot = tmp.newFolder("project").toPath();

		SkillsZipExtractor.writeOrUpdateAgentsMd(zipStream(zip), projectRoot);

		Path agentsMd = projectRoot.resolve("AGENTS.MD");
		assertTrue("AGENTS.MD must be created", Files.exists(agentsMd));
		String content = Files.readString(agentsMd, StandardCharsets.UTF_8);
		assertFalse("placeholder must be replaced", content.contains("<servoy_version>"));
		assertFalse("placeholder must be replaced", content.contains("<postgres_version>"));
	}

	/**
	 * AC: AGENTS.MD present -- only YAML block updated, other content unchanged.
	 */
	@Test
	public void writeOrUpdateAgentsMd_filePresent_updatesYamlOnly() throws IOException {
		Path zip = createZip("AGENTS.MD", SAMPLE_AGENTS_MD);
		Path projectRoot = tmp.newFolder("project").toPath();
		Path agentsMd = projectRoot.resolve("AGENTS.MD");

		// Write existing file with customisation
		String existing = SAMPLE_AGENTS_MD + "\n## My Custom Rules\nDo not delete data.\n";
		Files.writeString(agentsMd, existing, StandardCharsets.UTF_8);

		SkillsZipExtractor.writeOrUpdateAgentsMd(zipStream(zip), projectRoot);

		String content = Files.readString(agentsMd, StandardCharsets.UTF_8);
		assertTrue("custom content must be preserved", content.contains("Do not delete data."));
		assertFalse("placeholder must be replaced", content.contains("<servoy_version>"));
	}

	/** AGENTS.MD absent in zip -- no file created at project root. */
	@Test
	public void writeOrUpdateAgentsMd_agentsMdAbsentInZip_noFileCreated() throws IOException {
		Path zip = createZip("opencode.json", "{}");
		Path projectRoot = tmp.newFolder("project").toPath();

		SkillsZipExtractor.writeOrUpdateAgentsMd(zipStream(zip), projectRoot);

		assertFalse("AGENTS.MD must not be created when absent in zip",
				Files.exists(projectRoot.resolve("AGENTS.MD")));
	}

	// -----------------------------------------------------------------------
	// OpenCodeUtil.findGitRoot
	// -----------------------------------------------------------------------

	/** Walks up from a subdirectory to find the .git root. */
	@Test
	public void findGitRoot_gitDirPresent_returnsRoot() throws IOException {
		Path root = tmp.newFolder("repo").toPath();
		Files.createDirectories(root.resolve(".git"));
		Path subDir = root.resolve("src/main/java");
		Files.createDirectories(subDir);

		Path found = OpenCodeUtil.findGitRoot(subDir);

		assertEquals("should find the git root", root, found);
	}

	/** No .git directory in ancestry -- null returned. */
	@Test
	public void findGitRoot_noGitDir_returnsNull() throws IOException {
		Path dir = tmp.newFolder("no-git").toPath();

		assertNull("should return null when no .git found", OpenCodeUtil.findGitRoot(dir));
	}
}
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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for the package-private {@link ProviderConfigWriter} utility
 * class.
 * <p>
 * No OSGi runtime is required â all methods under test are pure Java and
 * operate on in-memory values or temporary file-system resources.
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
public class ProviderConfigWriterTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	// -----------------------------------------------------------------------
	// buildProviderEnvVars â returns empty when property not set
	// -----------------------------------------------------------------------

	/**
	 * When GENAI_API_KEY system property is absent, buildProviderEnvVars() returns
	 * an empty map.
	 */
	@Test
	public void buildProviderEnvVars_propertyAbsent_returnsEmptyMap() {
		String saved = System.getProperty(ProviderConfigWriter.ENV_API_KEY);
		try {
			System.clearProperty(ProviderConfigWriter.ENV_API_KEY);
			Map<String, String> env = ProviderConfigWriter.buildProviderEnvVars();
			assertTrue("Map must be empty when property is absent", env.isEmpty());
		} finally {
			if (saved != null)
				System.setProperty(ProviderConfigWriter.ENV_API_KEY, saved);
		}
	}

	/**
	 * When GENAI_API_KEY system property is blank, buildProviderEnvVars() returns
	 * an empty map.
	 */
	@Test
	public void buildProviderEnvVars_propertyBlank_returnsEmptyMap() {
		String saved = System.getProperty(ProviderConfigWriter.ENV_API_KEY);
		try {
			System.setProperty(ProviderConfigWriter.ENV_API_KEY, "   "); //$NON-NLS-1$
			Map<String, String> env = ProviderConfigWriter.buildProviderEnvVars();
			assertTrue("Map must be empty when property is blank", env.isEmpty());
		} finally {
			if (saved != null)
				System.setProperty(ProviderConfigWriter.ENV_API_KEY, saved);
			else
				System.clearProperty(ProviderConfigWriter.ENV_API_KEY);
		}
	}

	/**
	 * When GENAI_API_KEY system property has a non-blank value,
	 * buildProviderEnvVars()
	 * returns a single-entry map containing exactly that key and value.
	 */
	@Test
	public void buildProviderEnvVars_propertySet_returnsSingleEntryMap() {
		String saved = System.getProperty(ProviderConfigWriter.ENV_API_KEY);
		try {
			System.setProperty(ProviderConfigWriter.ENV_API_KEY, "test-api-key-123"); //$NON-NLS-1$
			Map<String, String> env = ProviderConfigWriter.buildProviderEnvVars();
			assertEquals("Map must contain exactly one entry", 1, env.size());
			assertTrue("GENAI_API_KEY must be present", env.containsKey(ProviderConfigWriter.ENV_API_KEY));
			assertEquals("Value must match the system property", "test-api-key-123", //$NON-NLS-1$
					env.get(ProviderConfigWriter.ENV_API_KEY));
		} finally {
			if (saved != null)
				System.setProperty(ProviderConfigWriter.ENV_API_KEY, saved);
			else
				System.clearProperty(ProviderConfigWriter.ENV_API_KEY);
		}
	}

	// -----------------------------------------------------------------------
	// mergeProviderConfig â file does not exist: no-op
	// -----------------------------------------------------------------------

	/**
	 * When the config file does not exist, mergeProviderConfig() is a no-op:
	 * no file is created.
	 */
	@Test
	public void mergeProviderConfig_fileAbsent_noFileCreated() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json"); //$NON-NLS-1$

		ProviderConfigWriter.mergeProviderConfig(configFile);

		assertFalse("No file must be created when the config file does not exist", Files.exists(configFile));
	}

	// -----------------------------------------------------------------------
	// mergeProviderConfig â existing file with $schema: unchanged
	// -----------------------------------------------------------------------

	/**
	 * A file that already has the $schema key is returned unchanged (skip-write).
	 */
	@Test
	public void mergeProviderConfig_schemaAlreadyPresent_fileUnchanged() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json"); //$NON-NLS-1$
		String existing = "{\n" + //$NON-NLS-1$
				"  \"$schema\": \"https://opencode.ai/config.json\",\n" + //$NON-NLS-1$
				"  \"mcp\": {}\n" + //$NON-NLS-1$
				"}\n"; //$NON-NLS-1$
		Files.writeString(configFile, existing, StandardCharsets.UTF_8);
		long modifiedBefore = Files.getLastModifiedTime(configFile).toMillis();

		try {
			Thread.sleep(50);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}

		ProviderConfigWriter.mergeProviderConfig(configFile);

		String after = Files.readString(configFile, StandardCharsets.UTF_8);
		assertEquals("File content must not change when $schema is already present", existing, after);
		assertEquals("File must not be rewritten", modifiedBefore, Files.getLastModifiedTime(configFile).toMillis());
	}

	// -----------------------------------------------------------------------
	// mergeProviderConfig â existing file without $schema: schema inserted
	// -----------------------------------------------------------------------

	/**
	 * A file without the $schema key gets the schema inserted; all other keys are
	 * left untouched.
	 */
	@Test
	public void mergeProviderConfig_schemaMissing_schemaInserted() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json"); //$NON-NLS-1$
		String existing = "{\n" + //$NON-NLS-1$
				"  \"mcp\": {\n" + //$NON-NLS-1$
				"    \"eclipse-ide\": {\n" + //$NON-NLS-1$
				"      \"type\": \"remote\",\n" + //$NON-NLS-1$
				"      \"url\": \"http://localhost:{env:MCP_PORT}/mcp/eclipse-ide\"\n" + //$NON-NLS-1$
				"    }\n" + //$NON-NLS-1$
				"  }\n" + //$NON-NLS-1$
				"}\n"; //$NON-NLS-1$
		Files.writeString(configFile, existing, StandardCharsets.UTF_8);

		ProviderConfigWriter.mergeProviderConfig(configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("$schema must be inserted", content.contains("\"$schema\"")); //$NON-NLS-1$
		assertTrue("$schema URL must be correct", content.contains(McpConfigWriter.SCHEMA_URL));
		// Existing content preserved
		assertTrue("mcp block must be preserved", content.contains("\"mcp\"")); //$NON-NLS-1$
		assertTrue("eclipse-ide entry must be preserved", content.contains("\"eclipse-ide\"")); //$NON-NLS-1$
		assertTrue("MCP URL must be preserved", //$NON-NLS-1$
				content.contains("http://localhost:{env:MCP_PORT}/mcp/eclipse-ide")); //$NON-NLS-1$
	}

	// -----------------------------------------------------------------------
	// mergeProviderConfig â provider/model keys from zip are preserved as-is
	// -----------------------------------------------------------------------

	/**
	 * The simplified mergeProviderConfig() does NOT touch provider, model, or
	 * small_model keys. A file containing those keys from the zip is left
	 * byte-for-byte unchanged (beyond any $schema insertion).
	 */
	@Test
	public void mergeProviderConfig_providerKeysFromZip_notTouched() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json"); //$NON-NLS-1$
		String existing = "{\n" + //$NON-NLS-1$
				"  \"$schema\": \"https://opencode.ai/config.json\",\n" + //$NON-NLS-1$
				"  \"provider\": {\n" + //$NON-NLS-1$
				"    \"litellm\": { \"name\": \"FromZip\" }\n" + //$NON-NLS-1$
				"  },\n" + //$NON-NLS-1$
				"  \"model\": \"litellm/zip-model\",\n" + //$NON-NLS-1$
				"  \"small_model\": \"litellm/zip-small\"\n" + //$NON-NLS-1$
				"}\n"; //$NON-NLS-1$
		Files.writeString(configFile, existing, StandardCharsets.UTF_8);

		ProviderConfigWriter.mergeProviderConfig(configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertEquals("File must be byte-for-byte unchanged", existing, content);
		assertTrue("Zip provider must be preserved", content.contains("FromZip")); //$NON-NLS-1$
		assertTrue("Zip model must be preserved", content.contains("zip-model")); //$NON-NLS-1$
		assertTrue("Zip small_model must be preserved", content.contains("zip-small")); //$NON-NLS-1$
	}

	// -----------------------------------------------------------------------
	// mergeProviderConfig â user-added keys preserved
	// -----------------------------------------------------------------------

	/**
	 * User-added top-level keys (e.g. theme, keybindings) survive a
	 * mergeProviderConfig() call untouched.
	 */
	@Test
	public void mergeProviderConfig_userAddedKeys_preserved() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json"); //$NON-NLS-1$
		String existing = "{\n" + //$NON-NLS-1$
				"  \"$schema\": \"https://opencode.ai/config.json\",\n" + //$NON-NLS-1$
				"  \"theme\": \"dark\",\n" + //$NON-NLS-1$
				"  \"keybindings\": \"vim\"\n" + //$NON-NLS-1$
				"}\n"; //$NON-NLS-1$
		Files.writeString(configFile, existing, StandardCharsets.UTF_8);

		ProviderConfigWriter.mergeProviderConfig(configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("theme key must be preserved", content.contains("\"theme\"")); //$NON-NLS-1$
		assertTrue("theme value must be preserved", content.contains("\"dark\"")); //$NON-NLS-1$
		assertTrue("keybindings key must be preserved", content.contains("\"keybindings\"")); //$NON-NLS-1$
		assertTrue("keybindings value must be preserved", content.contains("\"vim\"")); //$NON-NLS-1$
	}

	// -----------------------------------------------------------------------
	// mergeProviderConfig â mcp block byte-for-byte unchanged
	// -----------------------------------------------------------------------

	/**
	 * An mcp block present before the call is byte-for-byte identical after the
	 * call.
	 */
	@Test
	public void mergeProviderConfig_mcpBlockUnchanged() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json"); //$NON-NLS-1$
		String mcpBlock = "    \"eclipse-ide\": {\n" + //$NON-NLS-1$
				"      \"type\": \"remote\",\n" + //$NON-NLS-1$
				"      \"url\": \"http://localhost:{env:MCP_PORT}/mcp/eclipse-ide\",\n" + //$NON-NLS-1$
				"      \"headers\": {\n" + //$NON-NLS-1$
				"        \"Authorization\": \"Bearer {env:MCP_AUTH_TOKEN}\"\n" + //$NON-NLS-1$
				"      }\n" + //$NON-NLS-1$
				"    }"; //$NON-NLS-1$
		String existing = "{\n" + //$NON-NLS-1$
				"  \"$schema\": \"https://opencode.ai/config.json\",\n" + //$NON-NLS-1$
				"  \"mcp\": {\n" + //$NON-NLS-1$
				mcpBlock + "\n" + //$NON-NLS-1$
				"  }\n" + //$NON-NLS-1$
				"}\n"; //$NON-NLS-1$
		Files.writeString(configFile, existing, StandardCharsets.UTF_8);

		ProviderConfigWriter.mergeProviderConfig(configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("The mcp block content must be byte-for-byte preserved", content.contains(mcpBlock));
	}
}

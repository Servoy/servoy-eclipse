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
 * No OSGi runtime is required â?? all methods under test are pure Java and
 * operate
 * on in-memory values or temporary file-system resources.
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
public class ProviderConfigWriterTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	// -----------------------------------------------------------------------
	// Test 1: buildProviderEnvVars
	// -----------------------------------------------------------------------

	/** Map contains GENAI_API_KEY â?? DEFAULT_API_KEY and no other keys. */
	@Test
	public void buildProviderEnvVars_returnsApiKey() {
		Map<String, String> env = ProviderConfigWriter.buildProviderEnvVars();
		assertTrue("GENAI_API_KEY must be present", env.containsKey(ProviderConfigWriter.ENV_API_KEY));
		assertEquals("Value must equal DEFAULT_API_KEY",
				ProviderConfigWriter.DEFAULT_API_KEY,
				env.get(ProviderConfigWriter.ENV_API_KEY));
		assertEquals("Map must contain exactly one entry", 1, env.size());
	}

	// -----------------------------------------------------------------------
	// Test 2: mergeProviderConfig - fresh file creation
	// -----------------------------------------------------------------------

	/**
	 * Non-existent file is created; result contains $schema, provider, model,
	 * small_model with the correct values from PROVIDER_CONFIG_JSON.
	 */
	@Test
	public void mergeProviderConfig_freshFile_createsAllFields() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");

		ProviderConfigWriter.mergeProviderConfig(configFile);

		assertTrue("opencode.json must be created", Files.exists(configFile));
		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("$schema must be present", content.contains("$schema"));
		assertTrue("$schema URL must be correct", content.contains(McpConfigWriter.SCHEMA_URL));
		assertTrue("provider block must be present", content.contains("\"provider\""));
		assertTrue("litellm provider must be present", content.contains("\"litellm\""));
		assertTrue("apiKey env-var reference must be present",
				content.contains("{env:" + ProviderConfigWriter.ENV_API_KEY + "}"));
		assertTrue("baseURL must be present", content.contains("genai.servoy-cloud.eu"));
		assertTrue("model must be present", content.contains("\"model\""));
		assertTrue("model value must be correct",
				content.contains("litellm/eu.anthropic.claude-sonnet-4-6"));
		assertTrue("small_model must be present", content.contains("\"small_model\""));
		assertTrue("small_model value must be correct",
				content.contains("litellm/eu.anthropic.claude-haiku-4-5-20251001-v1:0"));
	}

	// -----------------------------------------------------------------------
	// Test 3: mergeProviderConfig - existing MCP file gets provider fields added
	// -----------------------------------------------------------------------

	/**
	 * File that already has an mcp block gets provider, model, small_model added
	 * without disturbing the mcp content.
	 */
	@Test
	public void mergeProviderConfig_existingMcpFile_addsProviderFields() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		String existing = "{\n" +
				"  \"$schema\": \"https://opencode.ai/config.json\",\n" +
				"  \"mcp\": {\n" +
				"    \"eclipse-ide\": {\n" +
				"      \"type\": \"remote\",\n" +
				"      \"url\": \"http://localhost:{env:MCP_PORT}/mcp/eclipse-ide\"\n" +
				"    }\n" +
				"  }\n" +
				"}\n";
		Files.writeString(configFile, existing, StandardCharsets.UTF_8);

		ProviderConfigWriter.mergeProviderConfig(configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		// Provider fields added
		assertTrue("provider block must be added", content.contains("\"provider\""));
		assertTrue("model must be added", content.contains("\"model\""));
		assertTrue("small_model must be added", content.contains("\"small_model\""));
		// MCP block untouched
		assertTrue("mcp block must still be present", content.contains("\"mcp\""));
		assertTrue("eclipse-ide entry must be preserved", content.contains("\"eclipse-ide\""));
		assertTrue("MCP URL must be preserved",
				content.contains("http://localhost:{env:MCP_PORT}/mcp/eclipse-ide"));
	}

	// -----------------------------------------------------------------------
	// Test 4: mergeProviderConfig - already correct, file unchanged
	// -----------------------------------------------------------------------

	/**
	 * Calling mergeProviderConfig() twice does not change the file content on the
	 * second call (skip-if-unchanged).
	 */
	@Test
	public void mergeProviderConfig_alreadyCorrect_fileUnchanged() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");

		// First call â?? creates the file
		ProviderConfigWriter.mergeProviderConfig(configFile);
		String afterFirst = Files.readString(configFile, StandardCharsets.UTF_8);
		long lastModifiedAfterFirst = Files.getLastModifiedTime(configFile).toMillis();

		// Brief pause to ensure any write would change the timestamp
		try {
			Thread.sleep(50);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}

		// Second call â?? must be a no-op
		ProviderConfigWriter.mergeProviderConfig(configFile);
		String afterSecond = Files.readString(configFile, StandardCharsets.UTF_8);
		long lastModifiedAfterSecond = Files.getLastModifiedTime(configFile).toMillis();

		assertEquals("File content must not change on second call", afterFirst, afterSecond);
		assertEquals("File must not be rewritten when content is unchanged",
				lastModifiedAfterFirst, lastModifiedAfterSecond);
	}

	// -----------------------------------------------------------------------
	// Test 5: mergeProviderConfig - stale provider block replaced
	// -----------------------------------------------------------------------

	/**
	 * File with an outdated provider block (different baseURL or models) gets the
	 * provider block replaced with the current PROVIDER_CONFIG_JSON values.
	 */
	@Test
	public void mergeProviderConfig_staleProviderBlock_replaced() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		String stale = "{\n" +
				"  \"$schema\": \"https://opencode.ai/config.json\",\n" +
				"  \"provider\": {\n" +
				"    \"litellm\": {\n" +
				"      \"npm\": \"@ai-sdk/openai-compatible\",\n" +
				"      \"name\": \"OldLiteLLM\",\n" +
				"      \"options\": {\n" +
				"        \"apiKey\": \"{env:GENAI_API_KEY}\",\n" +
				"        \"baseURL\": \"https://old-genai.example.com/v1\"\n" +
				"      },\n" +
				"      \"models\": {}\n" +
				"    }\n" +
				"  },\n" +
				"  \"model\": \"litellm/old-model\",\n" +
				"  \"small_model\": \"litellm/old-small-model\"\n" +
				"}\n";
		Files.writeString(configFile, stale, StandardCharsets.UTF_8);

		ProviderConfigWriter.mergeProviderConfig(configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		// Old values gone
		assertFalse("Old baseURL must be replaced", content.contains("old-genai.example.com"));
		assertFalse("Old model must be replaced", content.contains("litellm/old-model\""));
		assertFalse("Old small_model must be replaced", content.contains("litellm/old-small-model\""));
		// New values present
		assertTrue("New baseURL must be present", content.contains("genai.servoy-cloud.eu"));
		assertTrue("New model must be present",
				content.contains("litellm/eu.anthropic.claude-sonnet-4-6"));
		assertTrue("New small_model must be present",
				content.contains("litellm/eu.anthropic.claude-haiku-4-5-20251001-v1:0"));
	}

	// -----------------------------------------------------------------------
	// Test 6: mergeProviderConfig - user-added keys preserved
	// -----------------------------------------------------------------------

	/**
	 * User-added top-level keys (e.g. a custom theme key) survive a
	 * mergeProviderConfig() call untouched.
	 */
	@Test
	public void mergeProviderConfig_userAddedKeys_preserved() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		String existing = "{\n" +
				"  \"$schema\": \"https://opencode.ai/config.json\",\n" +
				"  \"theme\": \"dark\",\n" +
				"  \"keybindings\": \"vim\"\n" +
				"}\n";
		Files.writeString(configFile, existing, StandardCharsets.UTF_8);

		ProviderConfigWriter.mergeProviderConfig(configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("theme key must be preserved", content.contains("\"theme\""));
		assertTrue("theme value must be preserved", content.contains("\"dark\""));
		assertTrue("keybindings key must be preserved", content.contains("\"keybindings\""));
		assertTrue("keybindings value must be preserved", content.contains("\"vim\""));
		// Provider fields also added
		assertTrue("provider block must be added", content.contains("\"provider\""));
		assertTrue("model must be added", content.contains("\"model\""));
	}

	// -----------------------------------------------------------------------
	// Test 7: mergeProviderConfig - mcp block byte-for-byte unchanged
	// -----------------------------------------------------------------------

	/**
	 * An mcp block present before the call is byte-for-byte identical after the
	 * call.
	 */
	@Test
	public void mergeProviderConfig_mcpBlockUnchanged() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		// The mcp block has a very specific format that must be preserved exactly
		String mcpBlock = "    \"eclipse-ide\": {\n" +
				"      \"type\": \"remote\",\n" +
				"      \"url\": \"http://localhost:{env:MCP_PORT}/mcp/eclipse-ide\",\n" +
				"      \"headers\": {\n" +
				"        \"Authorization\": \"Bearer {env:MCP_AUTH_TOKEN}\"\n" +
				"      }\n" +
				"    }";
		String existing = "{\n" +
				"  \"$schema\": \"https://opencode.ai/config.json\",\n" +
				"  \"mcp\": {\n" +
				mcpBlock + "\n" +
				"  }\n" +
				"}\n";
		Files.writeString(configFile, existing, StandardCharsets.UTF_8);

		ProviderConfigWriter.mergeProviderConfig(configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("The mcp block content must be byte-for-byte preserved", content.contains(mcpBlock));
	}
}

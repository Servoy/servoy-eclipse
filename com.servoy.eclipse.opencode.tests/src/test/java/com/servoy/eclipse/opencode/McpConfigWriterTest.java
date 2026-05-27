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

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for the package-private {@link McpConfigWriter} utility class.
 * <p>
 * No OSGi runtime is required ??? all methods under test are pure Java and
 * operate on in-memory values or temporary file-system resources. The
 * {@link McpConfigWriter#collectProviders()} method is excluded because it
 * calls {@code Platform.getExtensionRegistry()} which requires an OSGi
 * container.
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
public class McpConfigWriterTest {
	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	/** Creates a simple {@link IMcpEndpointProvider} from literals. */
	private static IMcpEndpointProvider provider(List<String> urls, String token) {
		return new IMcpEndpointProvider() {
			@Override
			public List<String> getUrls() {
				return urls;
			}

			@Override
			public String getAuthToken() {
				return token;
			}
		};
	}

	// -----------------------------------------------------------------------
	// extractPort
	// -----------------------------------------------------------------------

	@Test
	public void extractPort_standardUrl_returnsCorrectPort() {
		assertEquals(8085, McpConfigWriter.extractPort("http://localhost:8085/mcp/eclipse-ide"));
	}

	@Test
	public void extractPort_alternativePort_returnsCorrectPort() {
		assertEquals(9090, McpConfigWriter.extractPort("http://localhost:9090/mcp/tool"));
	}

	@Test
	public void extractPort_highPortNumber_returnsCorrectPort() {
		assertEquals(65535, McpConfigWriter.extractPort("http://localhost:65535/mcp/tool"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void extractPort_noExplicitPort_throwsIllegalArgumentException() {
		McpConfigWriter.extractPort("http://localhost/mcp/eclipse-ide");
	}

	@Test(expected = IllegalArgumentException.class)
	public void extractPort_invalidUrl_throwsIllegalArgumentException() {
		McpConfigWriter.extractPort("not a url at all");
	}

	// -----------------------------------------------------------------------
	// serverNameFromUrl
	// -----------------------------------------------------------------------

	@Test
	public void serverNameFromUrl_standardPath_returnsLastSegment() {
		assertEquals("eclipse-ide", McpConfigWriter.serverNameFromUrl("http://localhost:8085/mcp/eclipse-ide"));
	}

	@Test
	public void serverNameFromUrl_differentTool_returnsCorrectName() {
		assertEquals("eclipse-coder", McpConfigWriter.serverNameFromUrl("http://localhost:8085/mcp/eclipse-coder"));
	}

	@Test
	public void serverNameFromUrl_trailingSlash_returnsLastNonEmptySegment() {
		assertEquals("eclipse-ide", McpConfigWriter.serverNameFromUrl("http://localhost:8085/mcp/eclipse-ide/"));
	}

	@Test
	public void serverNameFromUrl_singlePathSegment_returnsThatSegment() {
		assertEquals("tool", McpConfigWriter.serverNameFromUrl("http://localhost:8085/tool"));
	}

	// -----------------------------------------------------------------------
	// templateUrl
	// -----------------------------------------------------------------------

	@Test
	public void templateUrl_replacesPortWithEnvRef() {
		assertEquals(
				"http://localhost:{env:MCP_PORT}/mcp/eclipse-ide",
				McpConfigWriter.templateUrl("http://localhost:8085/mcp/eclipse-ide"));
	}

	@Test
	public void templateUrl_differentPort_replacesCorrectly() {
		assertEquals(
				"http://localhost:{env:MCP_PORT}/mcp/eclipse-coder",
				McpConfigWriter.templateUrl("http://localhost:9090/mcp/eclipse-coder"));
	}

	@Test
	public void templateUrl_resultContainsEnvMcpPortPlaceholder() {
		String result = McpConfigWriter.templateUrl("http://localhost:8085/mcp/eclipse-ide");
		assertTrue("Template URL must contain {env:MCP_PORT}", result.contains("{env:MCP_PORT}"));
	}

	@Test
	public void templateUrl_resultDoesNotContainOriginalPort() {
		String result = McpConfigWriter.templateUrl("http://localhost:8085/mcp/eclipse-ide");
		assertFalse("Original port :8085 must not remain in template URL", result.contains(":8085/"));
	}

	@Test
	public void templateUrl_pathIsPreservedExactly() {
		String result = McpConfigWriter.templateUrl("http://localhost:8085/mcp/eclipse-ide");
		assertTrue("Path /mcp/eclipse-ide must be preserved", result.endsWith("/mcp/eclipse-ide"));
	}

	// -----------------------------------------------------------------------
	// buildEnvVars
	// -----------------------------------------------------------------------

	@Test
	public void buildEnvVars_emptyProviderList_returnsEmptyMap() {
		assertTrue(McpConfigWriter.buildEnvVars(Collections.emptyList()).isEmpty());
	}

	@Test
	public void buildEnvVars_providerWithUrls_mapContainsMcpPort() {
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		Map<String, String> env = McpConfigWriter.buildEnvVars(List.of(p));
		assertTrue("MCP_PORT must be present", env.containsKey(McpConfigWriter.ENV_PORT));
	}

	@Test
	public void buildEnvVars_providerWithUrls_mcpPortValueIsExtractedPort() {
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		Map<String, String> env = McpConfigWriter.buildEnvVars(List.of(p));
		assertEquals("8085", env.get(McpConfigWriter.ENV_PORT));
	}

	@Test
	public void buildEnvVars_providerWithAuthToken_mapContainsMcpAuthToken() {
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), "secret");
		Map<String, String> env = McpConfigWriter.buildEnvVars(List.of(p));
		assertTrue("MCP_AUTH_TOKEN must be present when token is provided", env.containsKey(McpConfigWriter.ENV_TOKEN));
	}

	@Test
	public void buildEnvVars_providerWithAuthToken_tokenValueStoredCorrectly() {
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), "secret");
		Map<String, String> env = McpConfigWriter.buildEnvVars(List.of(p));
		assertEquals("secret", env.get(McpConfigWriter.ENV_TOKEN));
	}

	@Test
	public void buildEnvVars_providerWithNullToken_mapDoesNotContainMcpAuthToken() {
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		Map<String, String> env = McpConfigWriter.buildEnvVars(List.of(p));
		assertFalse("MCP_AUTH_TOKEN must be absent when token is null", env.containsKey(McpConfigWriter.ENV_TOKEN));
	}

	@Test
	public void buildEnvVars_providerWithEmptyUrlList_returnsEmptyMap() {
		IMcpEndpointProvider p = provider(Collections.emptyList(), "token");
		assertTrue(McpConfigWriter.buildEnvVars(List.of(p)).isEmpty());
	}

	@Test
	public void buildEnvVars_multipleProviders_mcpPortFromFirstUrl() {
		IMcpEndpointProvider p1 = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		IMcpEndpointProvider p2 = provider(List.of("http://localhost:8085/mcp/eclipse-coder"), null);
		Map<String, String> env = McpConfigWriter.buildEnvVars(List.of(p1, p2));
		assertEquals("8085", env.get(McpConfigWriter.ENV_PORT));
	}

	@Test
	public void buildEnvVars_tokenInSecondProvider_tokenPresent() {
		IMcpEndpointProvider p1 = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		IMcpEndpointProvider p2 = provider(List.of("http://localhost:8085/mcp/eclipse-coder"), "mytoken");
		Map<String, String> env = McpConfigWriter.buildEnvVars(List.of(p1, p2));
		assertEquals("mytoken", env.get(McpConfigWriter.ENV_TOKEN));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void buildEnvVars_returnedMap_isUnmodifiable() {
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), "token");
		Map<String, String> env = McpConfigWriter.buildEnvVars(List.of(p));
		env.put("EXTRA", "value"); // must throw
	}

	// -----------------------------------------------------------------------
	// mergeConfig ??? fresh file creation
	// -----------------------------------------------------------------------

	@Test
	public void mergeConfig_emptyProviders_doesNotCreateFile() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		McpConfigWriter.mergeConfig(Collections.emptyList(), configFile);
		assertFalse("No file should be created when providers list is empty", Files.exists(configFile));
	}

	@Test
	public void mergeConfig_fileAbsent_createsFile() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		McpConfigWriter.mergeConfig(List.of(p), configFile);
		assertTrue("opencode.json should be created", Files.exists(configFile));
	}

	@Test
	public void mergeConfig_freshFile_containsSchemaUrl() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		McpConfigWriter.mergeConfig(List.of(p), configFile);
		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("$schema field must be present", content.contains(McpConfigWriter.SCHEMA_URL));
	}

	@Test
	public void mergeConfig_freshFile_urlUsesEnvPortTemplate() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		McpConfigWriter.mergeConfig(List.of(p), configFile);
		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("URL must contain {env:MCP_PORT}", content.contains("{env:" + McpConfigWriter.ENV_PORT + "}"));
	}

	@Test
	public void mergeConfig_freshFile_hardcodedPortAbsent() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		McpConfigWriter.mergeConfig(List.of(p), configFile);
		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertFalse("Hardcoded port must not appear in the generated URL", content.contains(":8085/"));
	}

	@Test
	public void mergeConfig_freshFile_serverNameDerivedFromUrlPath() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		McpConfigWriter.mergeConfig(List.of(p), configFile);
		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("Server name 'eclipse-ide' must appear as JSON key", content.contains("\"eclipse-ide\""));
	}

	@Test
	public void mergeConfig_freshFile_withToken_authHeaderPresent() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), "tok123");
		McpConfigWriter.mergeConfig(List.of(p), configFile);
		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("Authorization header must be present", content.contains("Authorization"));
		assertTrue("MCP_AUTH_TOKEN env ref must appear", content.contains("{env:" + McpConfigWriter.ENV_TOKEN + "}"));
	}

	@Test
	public void mergeConfig_freshFile_withoutToken_noAuthHeader() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		McpConfigWriter.mergeConfig(List.of(p), configFile);
		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertFalse("Authorization header must NOT appear when no token", content.contains("Authorization"));
	}

	@Test
	public void mergeConfig_freshFile_multipleUrls_allServerNamesPresent() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		IMcpEndpointProvider p = provider(
				List.of("http://localhost:8085/mcp/eclipse-ide", "http://localhost:8085/mcp/eclipse-coder"), "tok");
		McpConfigWriter.mergeConfig(List.of(p), configFile);
		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue(content.contains("\"eclipse-ide\""));
		assertTrue(content.contains("\"eclipse-coder\""));
	}

	@Test
	public void mergeConfig_createsParentDirectoriesIfAbsent() throws IOException {
		Path deep = tmp.getRoot().toPath().resolve("a/b/c/opencode.json");
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		McpConfigWriter.mergeConfig(List.of(p), deep);
		assertTrue("File must be created even when parent dirs are missing", Files.exists(deep));
	}

	// -----------------------------------------------------------------------
	// mergeConfig ??? merge into existing file
	// -----------------------------------------------------------------------

	@Test
	public void mergeConfig_existingFileUpToDate_correctEntriesStillPresent() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), "tok");

		// First write
		McpConfigWriter.mergeConfig(List.of(p), configFile);
		// Second write ??? should be idempotent
		McpConfigWriter.mergeConfig(List.of(p), configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue(content.contains("\"eclipse-ide\""));
		assertTrue(content.contains("{env:" + McpConfigWriter.ENV_PORT + "}"));
		assertTrue(content.contains("{env:" + McpConfigWriter.ENV_TOKEN + "}"));
	}

	@Test
	public void mergeConfig_existingFileWithUserEntry_userEntryPreserved() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		String existing = "{\n" +
				"  \"$schema\": \"https://opencode.ai/config.json\",\n" +
				"  \"mcp\": {\n" +
				"    \"atlassian\": {\n" +
				"      \"type\": \"remote\",\n" +
				"      \"url\": \"https://mcp.atlassian.com/v1/mcp\"\n" +
				"    }\n" +
				"  }\n" +
				"}\n";
		Files.writeString(configFile, existing, StandardCharsets.UTF_8);

		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), "tok");
		McpConfigWriter.mergeConfig(List.of(p), configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("User-added 'atlassian' entry must be preserved", content.contains("\"atlassian\""));
		assertTrue("Contributed 'eclipse-ide' entry must be added", content.contains("\"eclipse-ide\""));
	}

	@Test
	public void mergeConfig_existingFileWithEmptyMcpBlock_entryAdded() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		Files.writeString(configFile, "{\n  \"mcp\": {}\n}\n", StandardCharsets.UTF_8);

		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		McpConfigWriter.mergeConfig(List.of(p), configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("Missing 'eclipse-ide' must be added to empty mcp block", content.contains("\"eclipse-ide\""));
		assertTrue("URL must use env template", content.contains("{env:" + McpConfigWriter.ENV_PORT + "}"));
	}

	@Test
	public void mergeConfig_existingFileWithHardcodedPortUrl_urlUpdatedToTemplate() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		String stale = "{\n  \"mcp\": {\n    \"eclipse-ide\": {\n" +
				"      \"type\": \"remote\",\n" +
				"      \"url\": \"http://localhost:8085/mcp/eclipse-ide\"\n" +
				"    }\n  }\n}\n";
		Files.writeString(configFile, stale, StandardCharsets.UTF_8);

		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		McpConfigWriter.mergeConfig(List.of(p), configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("URL must be updated to template form", content.contains("{env:" + McpConfigWriter.ENV_PORT + "}"));
		assertFalse("Hardcoded port must be gone after update",
				content.contains("\"http://localhost:8085/mcp/eclipse-ide\""));
	}

	@Test
	public void mergeConfig_existingFileWithNoMcpBlock_mcpBlockAddedWithEntry() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		String noMcp = "{\n  \"$schema\": \"" + McpConfigWriter.SCHEMA_URL + "\"\n}\n";
		Files.writeString(configFile, noMcp, StandardCharsets.UTF_8);

		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		McpConfigWriter.mergeConfig(List.of(p), configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("eclipse-ide must be added even if mcp block was absent",
				content.contains("\"eclipse-ide\""));
	}

	@Test
	public void mergeConfig_existingFileWithoutSchema_schemaFieldAdded() throws IOException {
		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		Files.writeString(configFile, "{\n  \"mcp\": {}\n}\n", StandardCharsets.UTF_8);

		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		McpConfigWriter.mergeConfig(List.of(p), configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("$schema must be added to file that lacked it", content.contains(McpConfigWriter.SCHEMA_URL));
	}
	// -----------------------------------------------------------------------
	// collectProviders ??? test-seam (OSGi extension point bypass)
	// -----------------------------------------------------------------------

	/**
	 * Clears the test-seam override after each test so that no state leaks
	 * into subsequent tests. Uses a separate @After to keep it alongside the
	 * tests that use it rather than mixed into the TemporaryFolder teardown.
	 */
	@After
	public void clearTestProvidersOverride() {
		McpConfigWriter.testProvidersOverride = null;
	}

	@Test
	public void collectProviders_withOverride_returnsInjectedList() {
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		McpConfigWriter.testProvidersOverride = List.of(p);

		List<IMcpEndpointProvider> result = McpConfigWriter.collectProviders();

		assertEquals("collectProviders must return the injected provider list", 1, result.size());
		assertSame("Returned provider must be the same instance", p, result.get(0));
	}

	@Test
	public void collectProviders_withOverride_returnsDefensiveCopy() {
		IMcpEndpointProvider p = provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null);
		McpConfigWriter.testProvidersOverride = List.of(p);

		List<IMcpEndpointProvider> result = McpConfigWriter.collectProviders();
		result.clear(); // mutate the returned list

		// Original override must be unchanged
		assertEquals("testProvidersOverride must not be mutated by callers", 1,
				McpConfigWriter.testProvidersOverride.size());
	}

	@Test
	public void collectProviders_withoutOverride_returnsEmptyListWhenNoOsgi() {
		// No OSGi runtime in this test environment: Platform.getExtensionRegistry()
		// returns null, so collectProviders() must return an empty list gracefully.
		McpConfigWriter.testProvidersOverride = null;
		List<IMcpEndpointProvider> result = McpConfigWriter.collectProviders();
		assertNotNull("collectProviders must never return null", result);
		assertTrue("Without OSGi, collectProviders must return an empty list", result.isEmpty());
	}

	// -----------------------------------------------------------------------
	// Full pipeline: collectProviders (mocked) + mergeConfig + buildEnvVars
	// -----------------------------------------------------------------------

	@Test
	public void fullPipeline_twoUrlsWithToken_fileContainsBothServerNames() throws IOException {
		McpConfigWriter.testProvidersOverride = List.of(
				provider(
						List.of("http://localhost:8085/mcp/eclipse-ide",
								"http://localhost:8085/mcp/eclipse-coder"),
						"authToken123"));

		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		List<IMcpEndpointProvider> providers = McpConfigWriter.collectProviders();
		McpConfigWriter.mergeConfig(providers, configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("eclipse-ide server name must be present", content.contains("\"eclipse-ide\""));
		assertTrue("eclipse-coder server name must be present", content.contains("\"eclipse-coder\""));
	}

	@Test
	public void fullPipeline_twoUrlsWithToken_urlsUseEnvPortTemplate() throws IOException {
		McpConfigWriter.testProvidersOverride = List.of(
				provider(
						List.of("http://localhost:8085/mcp/eclipse-ide",
								"http://localhost:8085/mcp/eclipse-coder"),
						"authToken123"));

		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		List<IMcpEndpointProvider> providers = McpConfigWriter.collectProviders();
		McpConfigWriter.mergeConfig(providers, configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("URL must use {env:MCP_PORT} template",
				content.contains("{env:" + McpConfigWriter.ENV_PORT + "}"));
		assertFalse("Hardcoded port :8085 must not appear in the file", content.contains(":8085/"));
	}

	@Test
	public void fullPipeline_twoUrlsWithToken_authHeadersPresentForBothEntries() throws IOException {
		McpConfigWriter.testProvidersOverride = List.of(
				provider(
						List.of("http://localhost:8085/mcp/eclipse-ide",
								"http://localhost:8085/mcp/eclipse-coder"),
						"authToken123"));

		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		List<IMcpEndpointProvider> providers = McpConfigWriter.collectProviders();
		McpConfigWriter.mergeConfig(providers, configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("Authorization header must be present", content.contains("Authorization"));
		assertTrue("MCP_AUTH_TOKEN env ref must be present",
				content.contains("{env:" + McpConfigWriter.ENV_TOKEN + "}"));
		// Both entries share the same auth header ??? verify it appears at least twice
		int first = content.indexOf("Authorization");
		int second = content.indexOf("Authorization", first + 1);
		assertTrue("Authorization header must appear for each of the two entries", second > first);
	}

	@Test
	public void fullPipeline_twoUrlsWithToken_schemaFieldPresent() throws IOException {
		McpConfigWriter.testProvidersOverride = List.of(
				provider(
						List.of("http://localhost:8085/mcp/eclipse-ide",
								"http://localhost:8085/mcp/eclipse-coder"),
						"authToken123"));

		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		List<IMcpEndpointProvider> providers = McpConfigWriter.collectProviders();
		McpConfigWriter.mergeConfig(providers, configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue("$schema must be present in the generated file",
				content.contains(McpConfigWriter.SCHEMA_URL));
	}

	@Test
	public void fullPipeline_twoUrlsWithToken_envVarsContainPortAndToken() {
		McpConfigWriter.testProvidersOverride = List.of(
				provider(
						List.of("http://localhost:8085/mcp/eclipse-ide",
								"http://localhost:8085/mcp/eclipse-coder"),
						"authToken123"));

		List<IMcpEndpointProvider> providers = McpConfigWriter.collectProviders();
		Map<String, String> env = McpConfigWriter.buildEnvVars(providers);

		assertEquals("MCP_PORT must be 8085", "8085", env.get(McpConfigWriter.ENV_PORT));
		assertEquals("MCP_AUTH_TOKEN must match the injected token", "authToken123",
				env.get(McpConfigWriter.ENV_TOKEN));
	}

	@Test
	public void fullPipeline_twoProvidersWithSamePort_envVarsCorrect() {
		// Two separate providers contributing different endpoints on the same port
		McpConfigWriter.testProvidersOverride = List.of(
				provider(List.of("http://localhost:8085/mcp/eclipse-ide"), "sharedToken"),
				provider(List.of("http://localhost:8085/mcp/eclipse-coder"), null));

		List<IMcpEndpointProvider> providers = McpConfigWriter.collectProviders();
		Map<String, String> env = McpConfigWriter.buildEnvVars(providers);

		assertEquals("8085", env.get(McpConfigWriter.ENV_PORT));
		assertEquals("sharedToken", env.get(McpConfigWriter.ENV_TOKEN));
	}

	@Test
	public void fullPipeline_twoProvidersWithSamePort_bothServerNamesInFile() throws IOException {
		McpConfigWriter.testProvidersOverride = List.of(
				provider(List.of("http://localhost:8085/mcp/eclipse-ide"), "sharedToken"),
				provider(List.of("http://localhost:8085/mcp/eclipse-coder"), null));

		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		List<IMcpEndpointProvider> providers = McpConfigWriter.collectProviders();
		McpConfigWriter.mergeConfig(providers, configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertTrue(content.contains("\"eclipse-ide\""));
		assertTrue(content.contains("\"eclipse-coder\""));
	}

	@Test
	public void fullPipeline_noToken_noAuthHeaderInFile() throws IOException {
		McpConfigWriter.testProvidersOverride = List.of(
				provider(
						List.of("http://localhost:8085/mcp/eclipse-ide",
								"http://localhost:8085/mcp/eclipse-coder"),
						null));

		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		List<IMcpEndpointProvider> providers = McpConfigWriter.collectProviders();
		McpConfigWriter.mergeConfig(providers, configFile);

		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertFalse("No Authorization header must appear when no auth token provided",
				content.contains("Authorization"));
	}

	@Test
	public void fullPipeline_noToken_envVarsHasNoAuthToken() {
		McpConfigWriter.testProvidersOverride = List.of(
				provider(List.of("http://localhost:8085/mcp/eclipse-ide"), null));

		List<IMcpEndpointProvider> providers = McpConfigWriter.collectProviders();
		Map<String, String> env = McpConfigWriter.buildEnvVars(providers);

		assertFalse("MCP_AUTH_TOKEN must be absent when no token provided",
				env.containsKey(McpConfigWriter.ENV_TOKEN));
		assertEquals("MCP_PORT must still be populated", "8085", env.get(McpConfigWriter.ENV_PORT));
	}

	@Test
	public void fullPipeline_emptyProviders_doesNotCreateFile() throws IOException {
		McpConfigWriter.testProvidersOverride = Collections.emptyList();

		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		List<IMcpEndpointProvider> providers = McpConfigWriter.collectProviders();
		McpConfigWriter.mergeConfig(providers, configFile);

		assertFalse("No opencode.json must be created when provider list is empty",
				Files.exists(configFile));
	}

	@Test
	public void fullPipeline_envVarsAndFileAreConsistent() throws IOException {
		// Verify that the port written into the file matches the port in the env map
		McpConfigWriter.testProvidersOverride = List.of(
				provider(
						List.of("http://localhost:9999/mcp/eclipse-ide",
								"http://localhost:9999/mcp/eclipse-coder"),
						"myToken"));

		Path configFile = tmp.getRoot().toPath().resolve("opencode.json");
		List<IMcpEndpointProvider> providers = McpConfigWriter.collectProviders();
		McpConfigWriter.mergeConfig(providers, configFile);
		Map<String, String> env = McpConfigWriter.buildEnvVars(providers);

		assertEquals("MCP_PORT must reflect the actual URL port", "9999",
				env.get(McpConfigWriter.ENV_PORT));
		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		assertFalse("The actual port 9999 must not appear hard-coded in the file",
				content.contains(":9999/"));
		assertTrue("File must contain the env-var placeholder, not the literal port",
				content.contains("{env:" + McpConfigWriter.ENV_PORT + "}"));
	}
}

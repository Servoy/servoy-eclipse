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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Builds, merges, and writes {@code opencode.json} from MCP endpoint
 * contributions.
 * <p>
 * All methods are package-private static so they can be exercised from
 * {@code com.servoy.eclipse.opencode.tests} without an OSGi runtime.
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
class McpConfigWriter {

	static final String ENV_PORT = "MCP_PORT"; //$NON-NLS-1$
	static final String ENV_TOKEN = "MCP_AUTH_TOKEN"; //$NON-NLS-1$
	static final String SCHEMA_URL = "https://opencode.ai/config.json"; //$NON-NLS-1$

	private static final String EXTENSION_POINT_ID = "com.servoy.eclipse.opencode.mcpEndpoint"; //$NON-NLS-1$
	private static final String ELEMENT_ENDPOINT = "endpoint"; //$NON-NLS-1$
	private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

	/**
	 * Test-only injection point. When non-{@code null}, {@link #collectProviders()}
	 * returns a copy of this list instead of querying the OSGi extension registry.
	 * Must be reset to {@code null} after each test to restore normal behaviour.
	 */
	// package-private for testing
	static volatile List<IMcpEndpointProvider> testProvidersOverride = null;

	/** Collects all registered providers via the extension point registry. */
	static List<IMcpEndpointProvider> collectProviders() {
		List<IMcpEndpointProvider> override = testProvidersOverride;
		if (override != null) {
			return new ArrayList<>(override);
		}
		List<IMcpEndpointProvider> providers = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		if (registry == null) {
			return providers;
		}
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_POINT_ID);
		for (IConfigurationElement element : elements) {
			if (ELEMENT_ENDPOINT.equals(element.getName())) {
				try {
					Object obj = element.createExecutableExtension(ATTR_CLASS);
					if (obj instanceof IMcpEndpointProvider provider) {
						providers.add(provider);
					} else {
						ServoyLog.logError(
								"McpConfigWriter: contributed class does not implement IMcpEndpointProvider: " + //$NON-NLS-1$
										element.getAttribute(ATTR_CLASS),
								null);
					}
				} catch (Exception e) {
					ServoyLog.logError("McpConfigWriter: failed to instantiate endpoint provider: " + //$NON-NLS-1$
							element.getAttribute(ATTR_CLASS), e);
				}
			}
		}
		return providers;
	}

	/**
	 * Extracts the port number from a URL such as
	 * {@code http://localhost:8085/mcp/eclipse-ide}.
	 *
	 * @throws IllegalArgumentException if the URL has no explicit port
	 */
	static int extractPort(String url) {
		try {
			URI uri = new URI(url);
			int port = uri.getPort();
			if (port < 0) {
				throw new IllegalArgumentException("URL has no explicit port: " + url); //$NON-NLS-1$
			}
			return port;
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URL: " + url, e); //$NON-NLS-1$
		}
	}

	/**
	 * Derives the MCP server name from a URL by taking the last non-empty
	 * path segment. {@code ".../mcp/eclipse-ide"} â?? {@code "eclipse-ide"}.
	 */
	static String serverNameFromUrl(String url) {
		try {
			URI uri = new URI(url);
			String path = uri.getPath();
			if (path == null || path.isEmpty()) {
				throw new IllegalArgumentException("URL has no path: " + url); //$NON-NLS-1$
			}
			// Remove trailing slash if present
			if (path.endsWith("/")) //$NON-NLS-1$
			{
				path = path.substring(0, path.length() - 1);
			}
			int lastSlash = path.lastIndexOf('/');
			if (lastSlash < 0 || lastSlash == path.length() - 1) {
				throw new IllegalArgumentException("Cannot derive server name from URL path: " + url); //$NON-NLS-1$
			}
			return path.substring(lastSlash + 1);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URL: " + url, e); //$NON-NLS-1$
		}
	}

	/**
	 * Builds the template URL by replacing the port with {@code {env:MCP_PORT}}.
	 * {@code "http://localhost:8085/mcp/eclipse-ide"}
	 * â?? {@code "http://localhost:{env:MCP_PORT}/mcp/eclipse-ide"}
	 */
	static String templateUrl(String url) {
		int port = extractPort(url);
		return url.replace(":" + port + "/", ":{env:" + ENV_PORT + "}/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Builds the environment variable map for the given providers.
	 * Returns a map containing {@code MCP_PORT} and, if any provider returns
	 * a non-null auth token, {@code MCP_AUTH_TOKEN}.
	 *
	 * @return unmodifiable map; empty if {@code providers} is empty or all
	 *         return empty URL lists
	 */
	static Map<String, String> buildEnvVars(List<IMcpEndpointProvider> providers) {
		if (providers.isEmpty()) {
			return Collections.emptyMap();
		}

		String port = null;
		String token = null;

		for (IMcpEndpointProvider provider : providers) {
			List<String> urls = provider.getUrls();
			if (urls != null) {
				for (String url : urls) {
					if (url != null && !url.isEmpty() && port == null) {
						try {
							port = String.valueOf(extractPort(url));
						} catch (IllegalArgumentException e) {
							ServoyLog.logError("McpConfigWriter: cannot extract port from URL: " + url, e); //$NON-NLS-1$
						}
					}
				}
			}
			if (token == null && provider.getAuthToken() != null) {
				token = provider.getAuthToken();
			}
		}

		if (port == null) {
			return Collections.emptyMap();
		}

		Map<String, String> envVars = new HashMap<>();
		envVars.put(ENV_PORT, port);
		if (token != null) {
			envVars.put(ENV_TOKEN, token);
		}
		return Collections.unmodifiableMap(envVars);
	}

	/**
	 * Merges contributed endpoints into the opencode.json at {@code configFile}.
	 * Creates the file (and parent directories) if absent; merges into the
	 * existing content if present.
	 *
	 * @param providers  collected extension point contributions
	 * @param configFile target path, e.g.
	 *                   {@code ~/.servoy/opencode/opencode.json}
	 */
	static void mergeConfig(List<IMcpEndpointProvider> providers, Path configFile) throws IOException {
		if (providers.isEmpty()) {
			return;
		}

		// Collect all contributed entries: serverName -> {templateUrl, authToken}
		Map<String, String[]> contributed = new LinkedHashMap<>();
		for (IMcpEndpointProvider provider : providers) {
			List<String> urls = provider.getUrls();
			if (urls == null)
				continue;
			String authToken = provider.getAuthToken();
			for (String url : urls) {
				if (url == null || url.isEmpty())
					continue;
				try {
					String serverName = serverNameFromUrl(url);
					String tmplUrl = templateUrl(url);
					contributed.put(serverName, new String[] { tmplUrl, authToken });
				} catch (IllegalArgumentException e) {
					ServoyLog.logError("McpConfigWriter: skipping invalid URL: " + url, e); //$NON-NLS-1$
				}
			}
		}

		if (contributed.isEmpty()) {
			return;
		}

		// Determine desired auth header value (same for all providers per spec)
		String desiredAuthHeader = null;
		for (String[] entry : contributed.values()) {
			if (entry[1] != null) {
				desiredAuthHeader = "Basic {env:" + ENV_TOKEN + "}"; //$NON-NLS-1$ //$NON-NLS-2$
				break;
			}
		}

		String existingJson = null;
		if (Files.exists(configFile)) {
			try {
				existingJson = Files.readString(configFile, StandardCharsets.UTF_8);
			} catch (IOException e) {
				ServoyLog.logError("McpConfigWriter: cannot read existing opencode.json, will regenerate", e); //$NON-NLS-1$
				existingJson = null;
			}
		}

		String newJson;
		if (existingJson == null) {
			// Generate fresh
			newJson = buildFreshJson(contributed, desiredAuthHeader);
		} else {
			// Merge into existing
			newJson = mergeIntoExisting(existingJson, contributed, desiredAuthHeader);
		}

		// Write out
		Path parent = configFile.getParent();
		if (parent != null && !Files.exists(parent)) {
			Files.createDirectories(parent);
		}
		Files.writeString(configFile, newJson, StandardCharsets.UTF_8);
	}

	/**
	 * Generates a fresh opencode.json from the contributed entries.
	 *
	 * @param contributed       map of serverName â?? [templateUrl, authToken]
	 * @param desiredAuthHeader the full auth header value or null
	 * @return JSON string
	 */
	private static String buildFreshJson(Map<String, String[]> contributed, String desiredAuthHeader) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n"); //$NON-NLS-1$
		sb.append("  \"$schema\": \"").append(SCHEMA_URL).append("\",\n"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("  \"mcp\": {\n"); //$NON-NLS-1$

		int i = 0;
		for (Map.Entry<String, String[]> entry : contributed.entrySet()) {
			String serverName = entry.getKey();
			String tmplUrl = entry.getValue()[0];
			String authToken = entry.getValue()[1];
			sb.append("    \"").append(jsonEscape(serverName)).append("\": {\n"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("      \"type\": \"remote\",\n"); //$NON-NLS-1$
			sb.append("      \"url\": \"").append(jsonEscape(tmplUrl)).append("\""); //$NON-NLS-1$ //$NON-NLS-2$
			if (authToken != null && desiredAuthHeader != null) {
				sb.append(",\n"); //$NON-NLS-1$
				sb.append("      \"headers\": {\n"); //$NON-NLS-1$
				sb.append("        \"Authorization\": \"").append(jsonEscape(desiredAuthHeader)).append("\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("      }\n"); //$NON-NLS-1$
			} else {
				sb.append("\n"); //$NON-NLS-1$
			}
			sb.append("    }"); //$NON-NLS-1$
			if (i < contributed.size() - 1) {
				sb.append(","); //$NON-NLS-1$
			}
			sb.append("\n"); //$NON-NLS-1$
			i++;
		}

		sb.append("  }\n"); //$NON-NLS-1$
		sb.append("}\n"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * Merges contributed entries into the existing JSON string.
	 * Uses string-based parsing to find and update/insert mcp entries.
	 *
	 * @param existingJson      the current file content
	 * @param contributed       map of serverName â?? [templateUrl, authToken]
	 * @param desiredAuthHeader the full auth header value or null
	 * @return updated JSON string
	 */
	private static String mergeIntoExisting(String existingJson, Map<String, String[]> contributed,
			String desiredAuthHeader) {
		// For each contributed entry, check if it's already present and correct.
		// We use simple string searching: look for the server name key in the mcp
		// block.

		// First, ensure $schema is present
		String result = existingJson;
		if (!result.contains("\"$schema\"")) //$NON-NLS-1$
		{
			// Insert $schema after opening brace
			int firstBrace = result.indexOf('{');
			if (firstBrace >= 0) {
				result = result.substring(0, firstBrace + 1) +
						"\n  \"$schema\": \"" + SCHEMA_URL + "\"," + //$NON-NLS-1$ //$NON-NLS-2$
						result.substring(firstBrace + 1);
			}
		}

		// Ensure mcp block exists
		if (!result.contains("\"mcp\"")) //$NON-NLS-1$
		{
			// Add mcp block before closing brace of root object
			int lastBrace = result.lastIndexOf('}');
			if (lastBrace >= 0) {
				// Find if we need a comma (i.e., there's content before us)
				String before = result.substring(0, lastBrace).trim();
				String comma = before.endsWith(",") || before.endsWith("{") ? "" : ","; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				result = result.substring(0, lastBrace) +
						comma + "\n  \"mcp\": {}\n" + //$NON-NLS-1$
						result.substring(lastBrace);
			}
		}

		// For each contributed entry, check if it's up-to-date in the mcp block
		for (Map.Entry<String, String[]> entry : contributed.entrySet()) {
			String serverName = entry.getKey();
			String tmplUrl = entry.getValue()[0];
			String authToken = entry.getValue()[1];

			result = upsertMcpEntry(result, serverName, tmplUrl, authToken, desiredAuthHeader);
		}

		return result;
	}

	/**
	 * Inserts or updates a single MCP server entry in the JSON string.
	 * If the entry already has the correct URL template and auth header, it is left
	 * unchanged.
	 */
	private static String upsertMcpEntry(String json, String serverName, String tmplUrl,
			String authToken, String desiredAuthHeader) {
		String serverKey = "\"" + serverName + "\""; //$NON-NLS-1$ //$NON-NLS-2$

		// Check if the server name exists in the mcp block (simple search)
		int mcpStart = findMcpBlockStart(json);
		if (mcpStart < 0) {
			return json; // can't find mcp block - shouldn't happen
		}

		// Look for the key after the mcp block start
		int keyPos = json.indexOf(serverKey, mcpStart);
		if (keyPos < 0) {
			// Entry not present - insert it
			return insertMcpEntry(json, serverName, tmplUrl, authToken, desiredAuthHeader);
		}

		// Entry is present - check if URL is correct
		// Find the url value after keyPos
		String urlMarker = "\"url\""; //$NON-NLS-1$
		int urlPos = json.indexOf(urlMarker, keyPos);
		if (urlPos < 0) {
			// Malformed entry - replace the whole thing
			return replaceMcpEntry(json, serverName, tmplUrl, authToken, desiredAuthHeader);
		}

		// Extract the url value
		int urlValStart = json.indexOf('"', urlPos + urlMarker.length());
		if (urlValStart < 0) {
			return replaceMcpEntry(json, serverName, tmplUrl, authToken, desiredAuthHeader);
		}
		// Skip the opening quote to find the value
		urlValStart++; // now points at first char of url value
		int urlValEnd = json.indexOf('"', urlValStart);
		if (urlValEnd < 0) {
			return replaceMcpEntry(json, serverName, tmplUrl, authToken, desiredAuthHeader);
		}
		String existingUrl = json.substring(urlValStart, urlValEnd);

		// Check the auth header if token is provided
		boolean urlOk = tmplUrl.equals(existingUrl);
		boolean authOk;
		if (desiredAuthHeader != null && authToken != null) {
			String authMarker = "\"Authorization\""; //$NON-NLS-1$
			int authPos = json.indexOf(authMarker, keyPos);
			if (authPos < 0) {
				authOk = false;
			} else {
				int authValStart = json.indexOf('"', authPos + authMarker.length());
				if (authValStart < 0) {
					authOk = false;
				} else {
					authValStart++;
					int authValEnd = json.indexOf('"', authValStart);
					if (authValEnd < 0) {
						authOk = false;
					} else {
						String existingAuth = json.substring(authValStart, authValEnd);
						authOk = desiredAuthHeader.equals(existingAuth);
					}
				}
			}
		} else {
			// No auth token desired
			authOk = true;
		}

		if (urlOk && authOk) {
			// Already correct - leave unchanged
			return json;
		}

		// Entry exists but is outdated - replace it
		return replaceMcpEntry(json, serverName, tmplUrl, authToken, desiredAuthHeader);
	}

	/**
	 * Finds the position right after the opening brace of the "mcp" object in the
	 * JSON string.
	 */
	private static int findMcpBlockStart(String json) {
		int mcpKeyPos = json.indexOf("\"mcp\""); //$NON-NLS-1$
		if (mcpKeyPos < 0)
			return -1;
		int colonPos = json.indexOf(':', mcpKeyPos);
		if (colonPos < 0)
			return -1;
		int bracePos = json.indexOf('{', colonPos);
		if (bracePos < 0)
			return -1;
		return bracePos + 1;
	}

	/**
	 * Inserts a new MCP entry into the mcp object in the JSON string.
	 */
	private static String insertMcpEntry(String json, String serverName, String tmplUrl,
			String authToken, String desiredAuthHeader) {
		// Find the closing brace of the mcp block
		int mcpStart = findMcpBlockStart(json);
		if (mcpStart < 0)
			return json;

		// Find the matching closing brace of the mcp object
		int braceDepth = 1;
		int pos = mcpStart;
		while (pos < json.length() && braceDepth > 0) {
			char c = json.charAt(pos);
			if (c == '{')
				braceDepth++;
			else if (c == '}')
				braceDepth--;
			if (braceDepth > 0)
				pos++;
		}
		// pos now points at the closing brace of mcp block
		if (braceDepth != 0)
			return json;

		String entryJson = buildEntryJson(serverName, tmplUrl, authToken, desiredAuthHeader, "    "); //$NON-NLS-1$

		// Check if mcp block is empty (no existing entries)
		String mcpContent = json.substring(mcpStart, pos).trim();
		String comma = mcpContent.isEmpty() ? "" : ",\n"; //$NON-NLS-1$ //$NON-NLS-2$

		return json.substring(0, pos) + comma + entryJson + "\n  " + json.substring(pos); //$NON-NLS-1$
	}

	/**
	 * Replaces an existing MCP entry (identified by serverName) with updated
	 * content.
	 */
	private static String replaceMcpEntry(String json, String serverName, String tmplUrl,
			String authToken, String desiredAuthHeader) {
		// Find the server name key and its enclosing object in the mcp block
		int mcpStart = findMcpBlockStart(json);
		if (mcpStart < 0)
			return json;

		String serverKey = "\"" + serverName + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		int keyPos = json.indexOf(serverKey, mcpStart);
		if (keyPos < 0)
			return json;

		// Find the colon after the key
		int colonPos = json.indexOf(':', keyPos + serverKey.length());
		if (colonPos < 0)
			return json;

		// Find the opening brace of the entry object
		int entryBraceStart = json.indexOf('{', colonPos);
		if (entryBraceStart < 0)
			return json;

		// Find the matching closing brace
		int braceDepth = 1;
		int pos = entryBraceStart + 1;
		while (pos < json.length() && braceDepth > 0) {
			char c = json.charAt(pos);
			if (c == '{')
				braceDepth++;
			else if (c == '}')
				braceDepth--;
			if (braceDepth > 0)
				pos++;
		}
		int entryBraceEnd = pos; // points at closing brace

		// Build the replacement entry (without the key, just the value object)
		String entryValue = buildEntryValueJson(tmplUrl, authToken, desiredAuthHeader, "    "); //$NON-NLS-1$

		return json.substring(0, entryBraceStart) + entryValue + json.substring(entryBraceEnd + 1);
	}

	/**
	 * Builds the full entry JSON (key + value) for insertion.
	 */
	private static String buildEntryJson(String serverName, String tmplUrl, String authToken,
			String desiredAuthHeader, String indent) {
		return indent + "\"" + jsonEscape(serverName) + "\": " + //$NON-NLS-1$ //$NON-NLS-2$
				buildEntryValueJson(tmplUrl, authToken, desiredAuthHeader, indent);
	}

	/**
	 * Builds just the value object for an MCP entry.
	 */
	private static String buildEntryValueJson(String tmplUrl, String authToken, String desiredAuthHeader,
			String indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n"); //$NON-NLS-1$
		sb.append(indent).append("  \"type\": \"remote\",\n"); //$NON-NLS-1$
		sb.append(indent).append("  \"url\": \"").append(jsonEscape(tmplUrl)).append("\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (authToken != null && desiredAuthHeader != null) {
			sb.append(",\n"); //$NON-NLS-1$
			sb.append(indent).append("  \"headers\": {\n"); //$NON-NLS-1$
			sb.append(indent).append("    \"Authorization\": \"").append(jsonEscape(desiredAuthHeader)).append("\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(indent).append("  }\n"); //$NON-NLS-1$
		} else {
			sb.append("\n"); //$NON-NLS-1$
		}
		sb.append(indent).append("}"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * Escapes special characters in a JSON string value.
	 */
	private static String jsonEscape(String value) {
		if (value == null)
			return ""; //$NON-NLS-1$
		return value
				.replace("\\", "\\\\") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\"", "\\\"") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\n", "\\n") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\r", "\\r") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\t", "\\t"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/** Private constructor â?? static utility class. */
	private McpConfigWriter() {
	}
}

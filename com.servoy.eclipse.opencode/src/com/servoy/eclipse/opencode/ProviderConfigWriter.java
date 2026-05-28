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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Writes the Servoy GenAI gateway provider configuration into
 * {@code opencode.json} ({@code provider}, {@code model},
 * {@code small_model} top-level keys).
 * <p>
 * All methods are package-private static so they can be exercised from
 * {@code com.servoy.eclipse.opencode.tests} without an OSGi runtime.
 * </p>
 * <p>
 * {@link McpConfigWriter} owns the {@code mcp} block; this class owns
 * {@code provider}, {@code model}, and {@code small_model}. Neither disturbs
 * the other's fields or any user-added entries.
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
class ProviderConfigWriter {

	/** Name of the environment variable that carries the GenAI API key. */
	static final String ENV_API_KEY = "GENAI_API_KEY"; //$NON-NLS-1$

	/**
	 * Hardcoded API key value â?? will be replaced by a cloud-supplied key in a
	 * future ticket.
	 */
	static final String DEFAULT_API_KEY = System.getProperty(ENV_API_KEY); //$NON-NLS-1$

	/**
	 * Full provider/model config fragment. The {@code apiKey} value is the
	 * env-var reference {@code {env:GENAI_API_KEY}} so the secret stays off disk.
	 * In a future ticket this constant is replaced by whatever Servoy Cloud
	 * returns.
	 */
	static final String PROVIDER_CONFIG_JSON = """
			{
			  "provider": {
			    "litellm": {
			      "npm": "@ai-sdk/openai-compatible",
			      "name": "LiteLLM",
			      "options": {
			        "apiKey": "{env:GENAI_API_KEY}",
			        "baseURL": "https://genai.servoy-cloud.eu/v1"
			      },
			      "models": {
			        "eu.anthropic.claude-sonnet-4-6": { "name": "Claude Sonnet 4.6" },
			        "eu.anthropic.claude-haiku-4-5-20251001-v1:0": { "name": "Claude Haiku 4.5" },
			        "eu.anthropic.claude-opus-4-7": { "name": "Claude Opus 4.7" }
			      }
			    }
			  },
			  "model": "litellm/eu.anthropic.claude-sonnet-4-6",
			  "small_model": "litellm/eu.anthropic.claude-haiku-4-5-20251001-v1:0"
			}
			""";

	/**
	 * Returns the environment variables that must be passed to the opencode
	 * child process so that the {@code {env:GENAI_API_KEY}} reference in
	 * {@code opencode.json} resolves correctly.
	 *
	 * @return unmodifiable map with a single entry {@code GENAI_API_KEY} â??
	 *         {@link #DEFAULT_API_KEY}
	 */
	static Map<String, String> buildProviderEnvVars() {
		Map<String, String> envVars = new HashMap<>();
		envVars.put(ENV_API_KEY, DEFAULT_API_KEY);
		return Collections.unmodifiableMap(envVars);
	}

	/**
	 * Merges the Servoy GenAI provider configuration into the opencode.json at
	 * {@code configFile}. Creates the file (and parent directories) if absent;
	 * merges into the existing content if present.
	 * <p>
	 * Keys managed by this method: {@code $schema}, {@code provider},
	 * {@code model}, {@code small_model}. All other keys (e.g. {@code mcp} written
	 * by {@link McpConfigWriter}) are left untouched.
	 * </p>
	 *
	 * @param configFile target path, e.g. {@code ~/.servoy/opencode/opencode.json}
	 * @throws IOException if the file cannot be read or written
	 */
	static void mergeProviderConfig(Path configFile) throws IOException {
		String existingJson = null;
		if (Files.exists(configFile)) {
			try {
				existingJson = Files.readString(configFile, StandardCharsets.UTF_8);
			} catch (IOException e) {
				// Fall through â?? regenerate from scratch
				existingJson = null;
			}
		}

		String newJson;
		if (existingJson == null) {
			newJson = buildFreshJson();
		} else {
			newJson = mergeIntoExisting(existingJson);
		}

		// Skip write if nothing changed
		if (newJson.equals(existingJson)) {
			return;
		}

		Path parent = configFile.getParent();
		if (parent != null && !Files.exists(parent)) {
			Files.createDirectories(parent);
		}
		Files.writeString(configFile, newJson, StandardCharsets.UTF_8);
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Generates a fresh opencode.json containing only {@code $schema},
	 * {@code provider}, {@code model}, and {@code small_model} parsed from
	 * {@link #PROVIDER_CONFIG_JSON}.
	 */
	private static String buildFreshJson() {
		// Parse the three top-level values we care about from PROVIDER_CONFIG_JSON
		String providerJson = extractTopLevelObject(PROVIDER_CONFIG_JSON, "provider"); //$NON-NLS-1$
		String model = extractTopLevelString(PROVIDER_CONFIG_JSON, "model"); //$NON-NLS-1$
		String smallModel = extractTopLevelString(PROVIDER_CONFIG_JSON, "small_model"); //$NON-NLS-1$

		StringBuilder sb = new StringBuilder();
		sb.append("{\n"); //$NON-NLS-1$
		sb.append("  \"$schema\": \"").append(McpConfigWriter.SCHEMA_URL).append("\",\n"); //$NON-NLS-1$ //$NON-NLS-2$
		if (providerJson != null) {
			sb.append("  \"provider\": ").append(providerJson).append(",\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (model != null) {
			sb.append("  \"model\": \"").append(model).append("\",\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (smallModel != null) {
			sb.append("  \"small_model\": \"").append(smallModel).append("\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sb.append("}\n"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * Merges provider, model, and small_model from {@link #PROVIDER_CONFIG_JSON}
	 * into the existing JSON string, preserving all other keys.
	 */
	private static String mergeIntoExisting(String existingJson) {
		String result = existingJson;

		// Ensure $schema is present
		if (!result.contains("\"$schema\"")) { //$NON-NLS-1$
			int firstBrace = result.indexOf('{');
			if (firstBrace >= 0) {
				result = result.substring(0, firstBrace + 1) +
						"\n  \"$schema\": \"" + McpConfigWriter.SCHEMA_URL + "\"," + //$NON-NLS-1$ //$NON-NLS-2$
						result.substring(firstBrace + 1);
			}
		}

		// Upsert provider (object value)
		String providerJson = extractTopLevelObject(PROVIDER_CONFIG_JSON, "provider"); //$NON-NLS-1$
		if (providerJson != null) {
			result = upsertTopLevelObject(result, "provider", providerJson); //$NON-NLS-1$
		}

		// Upsert model (string value)
		String model = extractTopLevelString(PROVIDER_CONFIG_JSON, "model"); //$NON-NLS-1$
		if (model != null) {
			result = upsertTopLevelString(result, "model", model); //$NON-NLS-1$
		}

		// Upsert small_model (string value)
		String smallModel = extractTopLevelString(PROVIDER_CONFIG_JSON, "small_model"); //$NON-NLS-1$
		if (smallModel != null) {
			result = upsertTopLevelString(result, "small_model", smallModel); //$NON-NLS-1$
		}

		return result;
	}

	/**
	 * Extracts the string value of a top-level JSON key from the given JSON text.
	 * Returns {@code null} if the key is not found or the value is not a string.
	 */
	static String extractTopLevelString(String json, String key) {
		String searchKey = "\"" + key + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		int keyPos = json.indexOf(searchKey);
		if (keyPos < 0)
			return null;
		int colonPos = json.indexOf(':', keyPos + searchKey.length());
		if (colonPos < 0)
			return null;
		// Find the opening quote of the string value
		int quoteStart = json.indexOf('"', colonPos + 1);
		if (quoteStart < 0)
			return null;
		quoteStart++; // move past the opening quote
		int quoteEnd = json.indexOf('"', quoteStart);
		if (quoteEnd < 0)
			return null;
		return json.substring(quoteStart, quoteEnd);
	}

	/**
	 * Extracts the object value (as a JSON string including braces) of a top-level
	 * key from the given JSON text. Returns {@code null} if the key is not found or
	 * the value is not an object.
	 */
	static String extractTopLevelObject(String json, String key) {
		String searchKey = "\"" + key + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		int keyPos = json.indexOf(searchKey);
		if (keyPos < 0)
			return null;
		int colonPos = json.indexOf(':', keyPos + searchKey.length());
		if (colonPos < 0)
			return null;
		int braceStart = json.indexOf('{', colonPos + 1);
		if (braceStart < 0)
			return null;
		// Brace-count to find matching close
		int depth = 1;
		int pos = braceStart + 1;
		while (pos < json.length() && depth > 0) {
			char c = json.charAt(pos);
			if (c == '{')
				depth++;
			else if (c == '}')
				depth--;
			if (depth > 0)
				pos++;
		}
		if (depth != 0)
			return null;
		return json.substring(braceStart, pos + 1);
	}

	/**
	 * Inserts or replaces a top-level string key in the JSON string.
	 * Preserves all other keys.
	 *
	 * @param json  the current JSON text
	 * @param key   the top-level key name (without quotes)
	 * @param value the new string value (without quotes)
	 * @return updated JSON text
	 */
	static String upsertTopLevelString(String json, String key, String value) {
		String searchKey = "\"" + key + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		int keyPos = json.indexOf(searchKey);
		if (keyPos >= 0) {
			// Key exists â?? replace its value
			int colonPos = json.indexOf(':', keyPos + searchKey.length());
			if (colonPos < 0)
				return json;
			int quoteStart = json.indexOf('"', colonPos + 1);
			if (quoteStart < 0)
				return json;
			int quoteEnd = json.indexOf('"', quoteStart + 1);
			if (quoteEnd < 0)
				return json;
			// Replace from opening quote to closing quote (inclusive)
			return json.substring(0, quoteStart) + "\"" + value + "\"" + json.substring(quoteEnd + 1); //$NON-NLS-1$ //$NON-NLS-2$
		}
		// Key absent â?? insert before the closing brace of the root object
		return insertTopLevelEntry(json, "\"" + key + "\": \"" + value + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Inserts or replaces a top-level object key in the JSON string.
	 * The entire object value is replaced (it is fully Servoy-managed).
	 * Preserves all other keys.
	 *
	 * @param json       the current JSON text
	 * @param key        the top-level key name (without quotes)
	 * @param objectJson the new object value as a JSON string (including braces)
	 * @return updated JSON text
	 */
	static String upsertTopLevelObject(String json, String key, String objectJson) {
		String searchKey = "\"" + key + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		int keyPos = json.indexOf(searchKey);
		if (keyPos >= 0) {
			// Key exists â?? replace the entire object value
			int colonPos = json.indexOf(':', keyPos + searchKey.length());
			if (colonPos < 0)
				return json;
			int braceStart = json.indexOf('{', colonPos + 1);
			if (braceStart < 0)
				return json;
			// Brace-count to find matching close
			int depth = 1;
			int pos = braceStart + 1;
			while (pos < json.length() && depth > 0) {
				char c = json.charAt(pos);
				if (c == '{')
					depth++;
				else if (c == '}')
					depth--;
				if (depth > 0)
					pos++;
			}
			if (depth != 0)
				return json;
			// Replace from opening brace to closing brace (inclusive)
			return json.substring(0, braceStart) + objectJson + json.substring(pos + 1);
		}
		// Key absent â?? insert before the closing brace of the root object
		return insertTopLevelEntry(json, "\"" + key + "\": " + objectJson); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Inserts a new key-value fragment before the last closing brace of the root
	 * JSON object, adding a comma if necessary.
	 */
	private static String insertTopLevelEntry(String json, String fragment) {
		int lastBrace = json.lastIndexOf('}');
		if (lastBrace < 0)
			return json;
		String before = json.substring(0, lastBrace).stripTrailing();
		String comma = (before.endsWith(",") || before.endsWith("{")) ? "" : ","; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		return before + comma + "\n  " + fragment + "\n" + json.substring(lastBrace); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/** Private constructor â?? static utility class. */
	private ProviderConfigWriter() {
	}
}

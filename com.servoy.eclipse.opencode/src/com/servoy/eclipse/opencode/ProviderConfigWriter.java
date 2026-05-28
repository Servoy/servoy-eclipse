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
import java.util.Map;

/**
 * Handles the GenAI API key environment variable and the {@code $schema} field
 * inside {@code opencode.json}.
 * <p>
 * Provider configuration (models, LiteLLM block, etc.) now comes entirely from
 * the skills zip ({@code SERVOY_SKILLS_ZIP}). This class no longer owns or
 * hard-codes any provider, model, or small_model values.
 * </p>
 * <p>
 * All methods are package-private static so they can be exercised from
 * {@code com.servoy.eclipse.opencode.tests} without an OSGi runtime.
 * </p>
 * <p>
 * {@link McpConfigWriter} owns the {@code mcp} block. Neither class disturbs
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
	 * Returns the environment variables that must be passed to the opencode child
	 * process so that the {@code {env:GENAI_API_KEY}} reference in
	 * {@code opencode.json} resolves correctly.
	 * <p>
	 * Reads {@code System.getProperty(ENV_API_KEY)} on every call (no caching).
	 * Returns an empty map when the property is absent or blank.
	 * </p>
	 *
	 * @return unmodifiable map â either empty or a single-entry map with
	 *         {@code GENAI_API_KEY}
	 */
	static Map<String, String> buildProviderEnvVars() {
		String apiKey = System.getProperty(ENV_API_KEY);
		if (apiKey == null || apiKey.isBlank()) {
			return Collections.emptyMap();
		}
		return Collections.singletonMap(ENV_API_KEY, apiKey);
	}

	/**
	 * Ensures the {@code $schema} field is present in the opencode.json at
	 * {@code configFile}. If the file does not exist, returns immediately (nothing
	 * to do). If the file exists, reads it, calls {@link #mergeIntoExisting}, and
	 * writes back only if the content changed.
	 *
	 * @param configFile target path, e.g. {@code ~/.servoy/opencode/opencode.json}
	 * @throws IOException if the file cannot be read or written
	 */
	static void mergeProviderConfig(Path configFile) throws IOException {
		if (!Files.exists(configFile)) {
			return;
		}

		String existingJson;
		try {
			existingJson = Files.readString(configFile, StandardCharsets.UTF_8);
		} catch (IOException e) {
			// Cannot read â leave the file as-is
			return;
		}

		String newJson = mergeIntoExisting(existingJson);

		// Skip write if nothing changed
		if (newJson.equals(existingJson)) {
			return;
		}

		Files.writeString(configFile, newJson, StandardCharsets.UTF_8);
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Ensures {@code $schema} is present in the existing JSON string.
	 * All other keys (provider, model, mcp, user-added keys) are left untouched.
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
			// Key exists â replace its value
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
		// Key absent â insert before the closing brace of the root object
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
			// Key exists â replace the entire object value
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
		// Key absent â insert before the closing brace of the root object
		return insertTopLevelEntry(json, "\"" + key + "\": " + objectJson); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Inserts a new key-value fragment before the last closing brace of the root
	 * JSON object, adding a comma if necessary.
	 */
	static String insertTopLevelEntry(String json, String fragment) {
		int lastBrace = json.lastIndexOf('}');
		if (lastBrace < 0)
			return json;
		String before = json.substring(0, lastBrace).stripTrailing();
		String comma = (before.endsWith(",") || before.endsWith("{")) ? "" : ","; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		return before + comma + "\n  " + fragment + "\n" + json.substring(lastBrace); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/** Private constructor â static utility class. */
	private ProviderConfigWriter() {
	}
}

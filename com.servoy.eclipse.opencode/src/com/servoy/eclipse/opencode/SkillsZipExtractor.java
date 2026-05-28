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
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * Extracts a skills zip into the user's opencode config directory and updates
 * the {@code AGENTS.MD} file in the active project root.
 * <p>
 * The zip source is read from the system property
 * {@value #SKILLS_ZIP_PROPERTY}.
 * The value may be an HTTP/HTTPS URL or a local file path.
 * When the property is absent or the file does not exist the feature is
 * silently skipped.
 * </p>
 * <p>
 * All methods are package-private static so they can be exercised from
 * {@code com.servoy.eclipse.opencode.tests} without an OSGi runtime.
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
class SkillsZipExtractor {

	/**
	 * System property name that carries the URL or absolute path to the skills zip.
	 */
	static final String SKILLS_ZIP_PROPERTY = "SERVOY_SKILLS_ZIP"; //$NON-NLS-1$

	/** Hardcoded PostgreSQL version embedded in the AGENTS.MD YAML block. */
	static final String POSTGRES_VERSION = "17.6"; //$NON-NLS-1$

	private static final String AGENTS_MD = "AGENTS.MD"; //$NON-NLS-1$
	private static final String OPENCODE_JSON = "opencode.json"; //$NON-NLS-1$
	private static final String OPENCODE_DIR_PREFIX = ".opencode/"; //$NON-NLS-1$

	/**
	 * Returns the skills zip source (HTTP URL or file path) from the system
	 * property, or {@code null} if the property is absent or the file does not
	 * exist.
	 * <p>
	 * For HTTP/HTTPS URLs the value is returned as-is. For file paths, existence
	 * is verified before returning.
	 * </p>
	 */
	static String getSkillsZipSource() {
		String prop = System.getProperty(SKILLS_ZIP_PROPERTY);
		if (prop == null || prop.isBlank())
			return null;
		if (prop.startsWith("http://") || prop.startsWith("https://")) //$NON-NLS-1$ //$NON-NLS-2$
			return prop;
		return Files.exists(Paths.get(prop)) ? prop : null;
	}

	/**
	 * Opens an {@link InputStream} from the given source, which may be an
	 * HTTP/HTTPS URL or a local file path.
	 *
	 * @param source HTTP/HTTPS URL or absolute file path
	 * @return an open {@link InputStream} -- caller must close it
	 * @throws IOException on network or file-system failure
	 */
	static InputStream openZipStream(String source) throws IOException {
		if (source.startsWith("http://") || source.startsWith("https://")) { //$NON-NLS-1$ //$NON-NLS-2$
			return new URL(source).openStream();
		}
		return Files.newInputStream(Paths.get(source));
	}

	/**
	 * Extracts the zip into the opencode config directory:
	 * <ul>
	 * <li>{@code .opencode/} subdirectory -- fully deleted then re-extracted into
	 * {@code configDir}</li>
	 * <li>{@code opencode.json} -- written into {@code configDir} as-is</li>
	 * </ul>
	 * <p>
	 * Takes ownership of {@code zipStream} and closes it.
	 * </p>
	 *
	 * @param zipStream input stream over the zip bytes
	 * @param configDir target directory (e.g. {@code ~/.servoy/opencode/})
	 * @return {@code true} if {@code opencode.json} was written from the zip
	 *         (caller should skip {@link ProviderConfigWriter})
	 * @throws IOException on read/write failure
	 */
	static boolean extractToConfigDir(InputStream zipStream, Path configDir) throws IOException {
		Files.createDirectories(configDir);

		// Delete existing .opencode/ directory completely
		Path opencodeSubDir = configDir.resolve(".opencode"); //$NON-NLS-1$
		if (Files.exists(opencodeSubDir)) {
			try (var stream = Files.walk(opencodeSubDir)) {
				stream.sorted(Comparator.reverseOrder()).forEach(p -> {
					try {
						Files.delete(p);
					} catch (IOException e) {
						// log inline -- non-fatal
					}
				});
			}
		}

		boolean wroteOpencodeJson = false;

		try (ZipInputStream zis = new ZipInputStream(zipStream)) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				String name = entry.getName();

				if (entry.isDirectory())
					continue;

				if (OPENCODE_JSON.equalsIgnoreCase(name)) {
					// Write opencode.json into configDir
					Files.write(configDir.resolve(OPENCODE_JSON), zis.readAllBytes());
					wroteOpencodeJson = true;

				} else if (name.toLowerCase().startsWith(OPENCODE_DIR_PREFIX)) {
					// Extract .opencode/ entries preserving relative structure
					Path relative = Paths.get(name); // e.g. .opencode/foo/bar.txt
					Path target = configDir.resolve(relative);
					Files.createDirectories(target.getParent());
					Files.write(target, zis.readAllBytes());
				}
			}
		}

		return wroteOpencodeJson;
	}

	/**
	 * Writes {@code AGENTS.MD} to {@code projectRoot}:
	 * <ul>
	 * <li>If absent: create from zip content as-is.</li>
	 * <li>If present: update only the YAML block fields
	 * ({@code servoy_version}, {@code postgres_version},
	 * {@code databases.active}).</li>
	 * </ul>
	 * <p>
	 * Takes ownership of {@code zipStream} and closes it.
	 * </p>
	 *
	 * @param zipStream   input stream over the zip bytes
	 * @param projectRoot project / git-root directory
	 * @throws IOException on read/write failure
	 */
	static void writeOrUpdateAgentsMd(InputStream zipStream, Path projectRoot) throws IOException {
		String servoyVersion = ClientVersion.getMajorVersion() + "." + //$NON-NLS-1$
				String.format("%02d", ClientVersion.getMiddleVersion()); //$NON-NLS-1$
		String postgresVersion = POSTGRES_VERSION;
		List<String> databases = getDatabaseNames();

		Path agentsMd = projectRoot.resolve(AGENTS_MD);

		if (!Files.exists(agentsMd)) {
			// Create from zip content as-is
			String content = readZipEntry(zipStream, AGENTS_MD);
			if (content != null) {
				Files.writeString(agentsMd, content, StandardCharsets.UTF_8);
			}
			// Whether we wrote it or not, now update the YAML block
			if (Files.exists(agentsMd)) {
				String existing = Files.readString(agentsMd, StandardCharsets.UTF_8);
				String updated = updateAgentsYaml(existing, servoyVersion, postgresVersion, databases);
				if (!updated.equals(existing)) {
					Files.writeString(agentsMd, updated, StandardCharsets.UTF_8);
				}
			}
		} else {
			// Update only the YAML block fields (zip stream not needed)
			try {
				zipStream.close();
			} catch (IOException e) {
				// ignore close failure
			}
			String existing = Files.readString(agentsMd, StandardCharsets.UTF_8);
			String updated = updateAgentsYaml(existing, servoyVersion, postgresVersion, databases);
			if (!updated.equals(existing)) {
				Files.writeString(agentsMd, updated, StandardCharsets.UTF_8);
			}
		}
	}

	/**
	 * Updates the YAML block inside an existing AGENTS.MD string. Fields updated:
	 * {@code servoy_version}, {@code postgres_version}, {@code databases.active}.
	 * Everything else is left unchanged. If a field is missing it is not inserted.
	 * <p>
	 * Package-private for testing.
	 * </p>
	 *
	 * @param content         the full file content
	 * @param servoyVersion   e.g. {@code "2026.06"}
	 * @param postgresVersion e.g. {@code "17.6"}
	 * @param databases       list of enabled server names
	 * @return updated content
	 */
	static String updateAgentsYaml(String content, String servoyVersion, String postgresVersion,
			List<String> databases) {
		String result = content;

		result = replaceYamlScalar(result, "servoy_version", "\"" + servoyVersion + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		result = replaceYamlScalar(result, "postgres_version", "\"" + postgresVersion + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		result = replaceActiveDatabases(result, databases);

		return result;
	}

	/**
	 * Reads a named entry from the zip stream, or returns {@code null} if the
	 * entry is absent.
	 * <p>
	 * Takes ownership of {@code zipStream} and closes it.
	 * </p>
	 *
	 * @param zipStream input stream over the zip bytes
	 * @param entryName entry name exactly as it appears in the zip
	 * @return UTF-8 decoded content, or {@code null} if not found
	 * @throws IOException on read failure
	 */
	static String readZipEntry(InputStream zipStream, String entryName) throws IOException {
		try (ZipInputStream zis = new ZipInputStream(zipStream)) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entryName.equalsIgnoreCase(entry.getName())) {
					return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
				}
			}
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Returns the list of enabled non-system database server names from
	 * {@link ApplicationServerRegistry}.
	 */
	private static List<String> getDatabaseNames() {
		try {
			if (!ApplicationServerRegistry.exists())
				return Collections.emptyList();
			String[] names = ApplicationServerRegistry.get().getServerManager()
					.getServerNames(true, false, false, false);
			if (names == null)
				return Collections.emptyList();
			return Arrays.asList(names);
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	/**
	 * Replaces the value of a simple YAML scalar field (single line) in
	 * {@code content}. Finds the first line that starts (after optional leading
	 * whitespace) with {@code key:} and replaces everything after the colon with
	 * {@code " " + newValue}.
	 */
	private static String replaceYamlScalar(String content, String key, String newValue) {
		String[] lines = content.split("\n", -1); //$NON-NLS-1$
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			String trimmed = line.stripLeading();
			if (trimmed.startsWith(key + ":")) { //$NON-NLS-1$
				// Preserve leading whitespace, replace value
				int indent = line.length() - trimmed.length();
				sb.append(line, 0, indent).append(key).append(": ").append(newValue); //$NON-NLS-1$
			} else {
				sb.append(line);
			}
			if (i < lines.length - 1) {
				sb.append('\n');
			}
		}
		return sb.toString();
	}

	/**
	 * Replaces the {@code active:} array value inside the {@code databases:} block.
	 * Finds the {@code databases:} key first, then the first {@code active:} line
	 * after it, and replaces the array value.
	 */
	private static String replaceActiveDatabases(String content, List<String> databases) {
		String[] lines = content.split("\n", -1); //$NON-NLS-1$
		StringBuilder sb = new StringBuilder();

		boolean inDatabasesBlock = false;
		boolean replacedActive = false;

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			String trimmed = line.stripLeading();

			if (!inDatabasesBlock) {
				if (trimmed.startsWith("databases:")) { //$NON-NLS-1$
					inDatabasesBlock = true;
				}
				sb.append(line);
			} else {
				// Inside databases block -- look for the active: line
				if (!replacedActive && trimmed.startsWith("active:")) { //$NON-NLS-1$
					int indent = line.length() - trimmed.length();
					sb.append(line, 0, indent).append("active: ").append(buildYamlList(databases)); //$NON-NLS-1$
					replacedActive = true;
				} else {
					// Exit the block when we hit a non-indented key (i.e. a sibling of databases)
					if (!trimmed.isEmpty() && !Character.isWhitespace(line.charAt(0)) && !trimmed.startsWith("#")) { //$NON-NLS-1$
						inDatabasesBlock = false;
					}
					sb.append(line);
				}
			}

			if (i < lines.length - 1) {
				sb.append('\n');
			}
		}
		return sb.toString();
	}

	/**
	 * Builds a YAML inline list string, e.g. {@code ["db1", "db2"]}.
	 */
	private static String buildYamlList(List<String> items) {
		if (items == null || items.isEmpty())
			return "[]"; //$NON-NLS-1$
		StringBuilder sb = new StringBuilder("["); //$NON-NLS-1$
		for (int i = 0; i < items.size(); i++) {
			if (i > 0)
				sb.append(", "); //$NON-NLS-1$
			sb.append('"').append(items.get(i)).append('"');
		}
		sb.append(']');
		return sb.toString();
	}

	/** Private constructor -- static utility class. */
	private SkillsZipExtractor() {
	}
}
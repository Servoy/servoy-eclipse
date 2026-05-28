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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ngclient.ui.RunNPMCommand;

/**
 * One-shot Eclipse Job that ensures the opencode npm package is installed in
 * the plugin's state directory, then schedules {@link RunOpencodeCommand} to
 * start the server.
 * <p>
 * Follows the same version-sentinel pattern as {@code NodeFolderCreatorJob} in
 * {@code com.servoy.eclipse.ngclient.ui}: a {@code package_copy.json} file
 * records the last installed version; if it differs from the bundle's
 * {@code /opencode/package.json}, a clean re-install is triggered.
 * </p>
 * <p>
 * The helper methods {@link #needsInstall(File, File, String)},
 * {@link #readUrlContent(URL)}, and {@link #deleteDirectory(Path)} are
 * package-visible so they can be exercised by
 * {@code com.servoy.eclipse.opencode.tests} without an OSGi runtime.
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
public class OpencodeFolderCreatorJob extends Job {
	public OpencodeFolderCreatorJob() {
		super("Setting up Servoy AI");
		setUser(false);
		setSystem(true);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		// Servoy AI requires both GENAI_API_KEY and SERVOY_SKILLS_ZIP to be configured.
		// If either is absent the view will show a "not enabled" page; skip setup
		// entirely.
		if (System.getProperty(ProviderConfigWriter.ENV_API_KEY) == null ||
				System.getProperty(ProviderConfigWriter.ENV_API_KEY).isBlank() ||
				SkillsZipExtractor.getSkillsZipSource() == null) {
			ServoyLog
					.logInfo("Servoy AI: not configured (GENAI_API_KEY or SERVOY_SKILLS_ZIP missing), skipping setup."); //$NON-NLS-1$
			return Status.OK_STATUS;
		}

		com.servoy.eclipse.ngclient.ui.Activator ngActivator = com.servoy.eclipse.ngclient.ui.Activator.getInstance();
		if (ngActivator == null) {
			ServoyLog.logError(
					"OpencodeFolderCreatorJob: ngclient.ui Activator not available - Node.js may not be installed.",
					null);
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Node.js Activator not available");
		}

		Activator activator = Activator.getInstance();
		File stateLocation = activator.getStateLocation().toFile();
		File opencodeDir = new File(stateLocation, "opencode");
		File markerFile = new File(stateLocation, ".fullygenerated");
		File sentinelFile = new File(opencodeDir, "package_copy.json");

		URL bundlePkgUrl = activator.getBundle().getEntry("/opencode/package.json");
		if (bundlePkgUrl == null) {
			ServoyLog.logError("OpencodeFolderCreatorJob: /opencode/package.json not found in bundle.", null);
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "/opencode/package.json missing from bundle");
		}

		String bundleContent;
		try {
			bundleContent = readUrlContent(bundlePkgUrl);
		} catch (IOException e) {
			ServoyLog.logError("OpencodeFolderCreatorJob: cannot read bundle package.json", e);
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Cannot read bundle package.json: " + e.getMessage());
		}

		boolean needsInstall = needsInstall(markerFile, sentinelFile, bundleContent);

		ServoyLog.logInfo(
				"Servoy AI setup: " + (needsInstall ? "installing opencode..." : "opencode already up to date."));

		if (needsInstall) {
			// Clean slate
			if (opencodeDir.exists()) {
				try {
					deleteDirectory(opencodeDir.toPath());
				} catch (IOException e) {
					ServoyLog.logError("OpencodeFolderCreatorJob: cannot delete opencode dir", e);
				}
			}
			opencodeDir.mkdirs();
			markerFile.delete();

			// Write package.json + sentinel
			try {
				Files.writeString(new File(opencodeDir, "package.json").toPath(), bundleContent,
						StandardCharsets.UTF_8);
				Files.writeString(sentinelFile.toPath(), bundleContent, StandardCharsets.UTF_8);
			} catch (IOException e) {
				ServoyLog.logError("OpencodeFolderCreatorJob: cannot write package.json", e);
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Cannot write package.json: " + e.getMessage());
			}
		}

		if (needsInstall || !markerFile.exists()) {
			// createNPMCommand internally waits for Node.js extraction (via
			// waitForNodeExtraction)
			RunNPMCommand install = ngActivator.createNPMCommand(opencodeDir, List.of("install"));
			try {
				install.runCommand(monitor);
			} catch (IOException | InterruptedException e) {
				ServoyLog.logError("OpencodeFolderCreatorJob: npm install failed", e);
				return new Status(IStatus.WARNING, Activator.PLUGIN_ID, "npm install failed: " + e.getMessage());
			}

			if (install.getExitCode() == 0) {
				try {
					markerFile.createNewFile();
					ServoyLog.logInfo("Servoy AI: opencode installed successfully.");
				} catch (IOException e) {
					ServoyLog.logError("OpencodeFolderCreatorJob: cannot create marker file", e);
				}
			} else {
				String msg = "npm install for opencode exited with code " + install.getExitCode();
				ServoyLog.logError(msg, null);
				return new Status(IStatus.WARNING, Activator.PLUGIN_ID, msg);
			}
		}

		// Write / merge the opencode.json MCP config
		Path servoyOpencodeCfgDir = Path.of(System.getProperty("user.home"), ".servoy", "opencode"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// 1. Extract skills zip (if available)
		boolean providerFromZip = false;
		String skillsZipSource = SkillsZipExtractor.getSkillsZipSource();
		if (skillsZipSource != null) {
			byte[] zipBytes;
			try (InputStream is = SkillsZipExtractor.openZipStream(skillsZipSource)) {
				zipBytes = is.readAllBytes();
			} catch (IOException e) {
				ServoyLog.logError("OpencodeFolderCreatorJob: failed to open skills zip", e); //$NON-NLS-1$
				zipBytes = null;
			}
			if (zipBytes != null) {
				try {
					providerFromZip = SkillsZipExtractor.extractToConfigDir(
							new ByteArrayInputStream(zipBytes), servoyOpencodeCfgDir);
				} catch (IOException e) {
					ServoyLog.logError("OpencodeFolderCreatorJob: failed to extract skills zip", e); //$NON-NLS-1$
				}

				String projectRoot = OpenCodeUtil.getActiveProjectPath();
				if (projectRoot != null) {
					try {
						SkillsZipExtractor.writeOrUpdateAgentsMd(
								new ByteArrayInputStream(zipBytes), Path.of(projectRoot));
					} catch (IOException e) {
						ServoyLog.logError("OpencodeFolderCreatorJob: failed to write AGENTS.MD", e); //$NON-NLS-1$
					}
				} else {
					ServoyLog.logInfo("OpencodeFolderCreatorJob: no active project, AGENTS.MD skipped."); //$NON-NLS-1$
				}
			}
		}

		// 2. Merge MCP endpoints (always)
		List<IMcpEndpointProvider> providers = McpConfigWriter.collectProviders();
		try {
			McpConfigWriter.mergeConfig(providers, servoyOpencodeCfgDir.resolve("opencode.json")); //$NON-NLS-1$
		} catch (IOException e) {
			ServoyLog.logError("OpencodeFolderCreatorJob: failed to write opencode.json", e); //$NON-NLS-1$
			// non-fatal: opencode starts without the Servoy MCP endpoints configured
		}
		Map<String, String> mcpEnvVars = McpConfigWriter.buildEnvVars(providers);

		// 3. Provider config fallback (only if zip did not supply opencode.json)
		if (!providerFromZip) {
			try {
				ProviderConfigWriter.mergeProviderConfig(servoyOpencodeCfgDir.resolve("opencode.json")); //$NON-NLS-1$
			} catch (IOException e) {
				ServoyLog.logError("OpencodeFolderCreatorJob: failed to write provider config", e); //$NON-NLS-1$
				// non-fatal: opencode starts without Servoy GenAI provider configured
			}
		}

		Map<String, String> allEnvVars = new HashMap<>(mcpEnvVars);
		allEnvVars.putAll(ProviderConfigWriter.buildProviderEnvVars());

		// Start the opencode server
		new RunOpencodeCommand(opencodeDir, Collections.unmodifiableMap(allEnvVars)).schedule();
		return Status.OK_STATUS;
	}

	// --- package-visible helpers (accessible from test fragment) ---

	/**
	 * Determines whether {@code npm install} needs to run.
	 * <p>
	 * Returns {@code true} when any of:
	 * <ul>
	 * <li>the {@code .fullygenerated} marker is absent,</li>
	 * <li>the {@code package_copy.json} sentinel is absent, or</li>
	 * <li>the sentinel content differs from {@code bundleContent}
	 * (meaning the shipped version changed).</li>
	 * </ul>
	 * </p>
	 *
	 * @param markerFile    the {@code .fullygenerated} marker
	 * @param sentinelFile  the {@code package_copy.json} sentinel
	 * @param bundleContent the content of {@code /opencode/package.json} from the
	 *                      bundle
	 * @return {@code true} if a (re-)install is required
	 */
	static boolean needsInstall(File markerFile, File sentinelFile, String bundleContent) {
		if (!markerFile.exists() || !sentinelFile.exists())
			return true;
		try {
			String installedContent = Files.readString(sentinelFile.toPath(), StandardCharsets.UTF_8);
			return !installedContent.equals(bundleContent);
		} catch (IOException e) {
			ServoyLog.logError("OpencodeFolderCreatorJob: cannot read sentinel file", e);
			return true; // defensive: re-install on read error
		}
	}

	/**
	 * Reads the full content of the given URL as a UTF-8 string.
	 */
	static String readUrlContent(URL url) throws IOException {
		try (InputStream is = url.openStream()) {
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Recursively deletes {@code dir} and all its contents.
	 */
	static void deleteDirectory(Path dir) throws IOException {
		try (var stream = Files.walk(dir)) {
			stream.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.delete(path);
				} catch (IOException e) {
					ServoyLog.logError("OpencodeFolderCreatorJob: cannot delete " + path, e);
				}
			});
		}
	}
}

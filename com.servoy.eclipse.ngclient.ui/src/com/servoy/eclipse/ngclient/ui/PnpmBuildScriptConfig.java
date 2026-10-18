/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.ngclient.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.servoy.eclipse.model.repository.SolutionSerializer;

/**
 * SVY-21502: generates the solution-local <code>pnpm-workspace.yaml</code> from the committed allow/deny lists and
 * runs the bounded discovery query that enumerates packages pnpm wants to run build scripts for.
 * <p>
 * This replaces the SVY-21456 stopgap (an unconditional <code>--config.strictDepBuilds=false</code> plus a static
 * <code>strictDepBuilds: false</code> in the shipped yaml). The committed
 * <code>ng_web_packages/pnpm-build-approvals.json</code> ({@link PnpmBuildApprovals}) is the single source of truth;
 * on every pnpm-mode build this class rewrites <code>target/&lt;solution&gt;/pnpm-workspace.yaml</code> to carry the
 * decisions as pnpm's explicit <b>version-scoped</b> <code>allowBuilds</code> map (each allowed
 * <code>name@version</code> maps to <code>true</code>, each denied <code>name@version</code> to <code>false</code> -
 * pnpm 12 removed the pnpm-10 <code>onlyBuiltDependencies</code>/<code>ignoredBuiltDependencies</code> keys) plus
 * <code>strictDepBuilds: true</code>, so a newly-appearing pending script (including a bumped version of an
 * already-decided package) is surfaced rather than silently ignored. It also seeds/commits-back the committed
 * <code>pnpm-lock.yaml</code> so CI installs the exact same versions frozen.
 * <p>
 * SWT-free so it is reachable from both the Developer build and the headless WAR export path.
 *
 * @author jcompagner
 * @since 2026.12
 */
public class PnpmBuildScriptConfig
{
	/** The generated solution-local pnpm workspace config file name. */
	public static final String PNPM_WORKSPACE_YAML = "pnpm-workspace.yaml";

	/** The pnpm throwaway node_modules metadata file that carries the versioned <code>ignoredBuilds</code> array. */
	public static final String MODULES_YAML_NAME = ".modules.yaml";

	// a real name@version token: an optional '@scope/' prefixed package name, then '@', then a version (anything up
	// to the next delimiter). Scoped names have two '@' (e.g. @parcel/watcher@2.5.1); the version split is on the
	// LAST '@'. This matcher just validates the name portion; parsing does the split. Anything else (prose, progress
	// lines, summaries) is rejected.
	private static final Pattern NAME_AT_VERSION = Pattern.compile("^(?:@[a-z0-9][a-z0-9._-]*/)?[a-z0-9][a-z0-9._-]*@[A-Za-z0-9][A-Za-z0-9.+_~^-]*$");

	private PnpmBuildScriptConfig()
	{
	}

	/**
	 * Rewrites <code>&lt;projectFolder&gt;/pnpm-workspace.yaml</code> from the given committed approvals:
	 * <ul>
	 * <li>a version-scoped <code>allowBuilds</code> map: each allowed <code>name@version</code> maps to
	 * <code>true</code>, each denied <code>name@version</code> to <code>false</code> (pnpm 12 syntax - pnpm removed
	 * <code>onlyBuiltDependencies</code>/<code>ignoredBuiltDependencies</code> in v11),</li>
	 * <li><code>strictDepBuilds: true</code>,</li>
	 * <li>keeps <code>nodeLinker: hoisted</code> and <code>shamefullyHoist: true</code>.</li>
	 * </ul>
	 * When both lists are empty the <code>allowBuilds</code> block is omitted entirely. The generated file is a
	 * <code>target/</code> artifact so regenerating it every build is correct and cheap.
	 *
	 * @param projectFolder the solution's standalone pnpm project folder (<code>target/&lt;solution&gt;/</code>)
	 * @param approvals the committed allow/deny lists (never <code>null</code>)
	 * @param console optional console for progress messages (may be <code>null</code>)
	 */
	public static void writeWorkspaceYaml(File projectFolder, PnpmBuildApprovals approvals, StringOutputStream console)
	{
		if (projectFolder == null || approvals == null) return;
		StringBuilder yaml = new StringBuilder(256);
		yaml.append("nodeLinker: hoisted\n");
		yaml.append("shamefullyHoist: true\n");
		// SVY-21502: strict is now ON; the allowBuilds map below drives what is actually built, and any newly
		// appearing pending build script is surfaced by the discovery query instead of being silently ignored.
		yaml.append("strictDepBuilds: true\n");
		appendAllowBuilds(yaml, approvals.getAllowed(), approvals.getDenied());
		try
		{
			FileUtils.writeStringToFile(new File(projectFolder, PNPM_WORKSPACE_YAML), yaml.toString(), StandardCharsets.UTF_8);
			writeConsole(console, "- pnpm mode: generated pnpm-workspace.yaml (allowBuilds allowed=" + approvals.getAllowed() +
				", denied=" + approvals.getDenied() + ")");
		}
		catch (IOException e)
		{
			Activator.getInstance().getLog().error("Error writing generated pnpm-workspace.yaml in " + projectFolder, e);
		}
	}

	/**
	 * Appends the pnpm 12 version-scoped <code>allowBuilds</code> map: each allowed <code>name@version</code> as
	 * <code>'&lt;name@version&gt;': true</code> and each denied <code>name@version</code> as
	 * <code>'&lt;name@version&gt;': false</code>. If both lists are empty nothing is appended (no empty
	 * <code>allowBuilds:</code> key). Keys that need quoting (every <code>name@version</code> contains <code>@</code>,
	 * plus scoped names, spaces or <code>|</code>) are single-quoted with the YAML single-quote escaping; the values
	 * are the bare literals <code>true</code>/<code>false</code>.
	 */
	private static void appendAllowBuilds(StringBuilder yaml, Set<String> allowed, Set<String> denied)
	{
		boolean hasAllowed = allowed != null && !allowed.isEmpty();
		boolean hasDenied = denied != null && !denied.isEmpty();
		if (!hasAllowed && !hasDenied) return;
		yaml.append("allowBuilds:\n");
		if (hasAllowed)
		{
			for (String value : allowed)
			{
				yaml.append("  ").append(yamlKey(value)).append(": true\n");
			}
		}
		if (hasDenied)
		{
			for (String value : denied)
			{
				yaml.append("  ").append(yamlKey(value)).append(": false\n");
			}
		}
	}

	/**
	 * Renders a <code>name@version</code> entry as a YAML mapping key, single-quoting (with single-quote escaping)
	 * when the value is not a plain unquoted scalar - which every versioned key is, since it contains <code>@</code>
	 * (and possibly <code>/</code> for a scoped name, a space, or <code>|</code> for a disjunction).
	 */
	private static String yamlKey(String name)
	{
		if (name != null && name.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) return name;
		return "'" + (name == null ? "" : name.replace("'", "''")) + "'";
	}

	/**
	 * Runs the bounded discovery against the <em>already-installed</em> dependency tree and returns the exact
	 * <code>name@version</code> entries pnpm reports as having (ignored) build scripts.
	 * <p>
	 * The authoritative, <b>versioned</b> source is <code>&lt;projectFolder&gt;/node_modules/.modules.yaml</code>'s
	 * <code>ignoredBuilds</code> array (verified to list <code>name@version</code> entries, e.g.
	 * <code>esbuild@0.28.1</code>, <code>esbuild@0.28.2</code>, <code>lmdb@3.5.6</code>). <code>pnpm
	 * ignored-builds</code> is deliberately NOT used because it prints bare names only and so cannot distinguish two
	 * installed versions of the same package. The caller MUST have run the package install(s) first (inside a
	 * {@link RunPNPMCommand#beginBuildScriptDiscoveryDemotion()} window so those installs did not hard-fail on a
	 * pending script) so that <code>node_modules/.modules.yaml</code> exists; this method does NOT run an install
	 * itself. On a missing/unparsable file the caller gets an empty set (a build is never failed by discovery).
	 * <p>
	 * This does NOT subtract the committed lists; use {@link PnpmBuildApprovals#pending(Set)} for that.
	 *
	 * @param projectFolder the solution's standalone pnpm project folder (with node_modules already materialized)
	 * @param monitor progress monitor (may be <code>null</code>; unused - kept for signature stability)
	 * @param console optional console for progress messages (may be <code>null</code>)
	 * @return the discovered <code>name@version</code> entries (possibly empty, never <code>null</code>)
	 */
	public static Set<String> discoverPendingBuildScripts(File projectFolder, IProgressMonitor monitor, StringOutputStream console)
	{
		Set<String> discovered = new LinkedHashSet<>();
		if (projectFolder == null || !Activator.isPnpmMode()) return discovered;
		try
		{
			// node_modules/.modules.yaml carries a versioned "ignoredBuilds" array for the installed tree - the
			// name@version entries pnpm blocked during the last install. Any name@version already allowed in the
			// generated allowBuilds map had its script run during the install, so it is NOT listed here; only the
			// genuinely-pending versions remain, which is exactly the set we want to surface.
			File modulesYaml = new File(new File(projectFolder, "node_modules"), MODULES_YAML_NAME);
			if (modulesYaml.exists())
			{
				String content = FileUtils.readFileToString(modulesYaml, StandardCharsets.UTF_8);
				discovered.addAll(parseModulesYamlIgnoredBuilds(content));
			}
			else if (new File(projectFolder, "node_modules").isDirectory())
			{
				// SVY-21502 N1: node_modules was materialized but .modules.yaml is missing - pnpm always writes it
				// after an install, so this is an anomaly. Do NOT silently treat it as "nothing pending" (that would
				// let a headless WAR build ship unreviewed build scripts). Record it; isModulesStateReadable(...)
				// lets the WAR caller fail loud instead of trusting an empty set.
				writeConsole(console, "- pnpm mode: WARNING - node_modules present but " + MODULES_YAML_NAME +
					" is missing; cannot determine pending build scripts reliably.");
			}
			writeConsole(console, "- pnpm mode: discovery query found packages with pending build scripts: " + discovered);
		}
		catch (IOException | RuntimeException e)
		{
			// discovery must never break a build; fall back to an empty pending set.
			Activator.getInstance().getLog().error("Error running pnpm build-script discovery query in " + projectFolder, e);
		}
		return discovered;
	}

	/**
	 * SVY-21502 N1: reports whether the installed build-script state could actually be read - i.e. whether a
	 * headless WAR/CI build may trust an empty {@link #discoverPendingBuildScripts} result. Returns <code>false</code>
	 * (indeterminate) when <code>node_modules</code> exists but <code>node_modules/.modules.yaml</code> is missing:
	 * pnpm always writes that file after an install, so its absence means discovery could not see what was ignored,
	 * and the headless path must fail loud rather than silently ship unreviewed build scripts. Returns
	 * <code>true</code> when the file is present (readable state) or when <code>node_modules</code> does not exist yet
	 * (nothing was installed, so there is genuinely nothing to decide).
	 *
	 * @param projectFolder the solution's standalone pnpm project folder
	 * @return <code>true</code> if the pending set is determinate, <code>false</code> if it could not be determined
	 */
	public static boolean isModulesStateReadable(File projectFolder)
	{
		if (projectFolder == null) return true;
		File nodeModules = new File(projectFolder, "node_modules");
		if (!nodeModules.isDirectory()) return true; // nothing installed -> nothing to decide
		return new File(nodeModules, MODULES_YAML_NAME).exists();
	}

	/**
	 * Runs <code>pnpm rebuild</code> so that the build scripts of the packages now allowed in the generated
	 * <code>pnpm-workspace.yaml</code> actually run (pnpm's install does not retroactively run a script that was
	 * ignored on a previous install; <code>rebuild</code> is how the hint tells you to apply a freshly-added
	 * <code>allowBuilds: &lt;pkg&gt;: true</code>). A non-zero exit is logged but never thrown - a failing optional
	 * native build must not break the whole TiNG build here; the subsequent build step reports real failures.
	 *
	 * @param projectFolder the solution's standalone pnpm project folder
	 * @param monitor progress monitor (may be <code>null</code>)
	 * @param console optional console for progress messages (may be <code>null</code>)
	 */
	public static void rebuildApprovedBuildScripts(File projectFolder, IProgressMonitor monitor, StringOutputStream console)
	{
		if (projectFolder == null || !Activator.isPnpmMode()) return;
		IProgressMonitor mon = monitor != null ? monitor : new NullProgressMonitor();
		try
		{
			writeConsole(console, "- pnpm mode: running 'pnpm rebuild' so approved build scripts run");
			IRunNPMCommand rebuild = Activator.getInstance().createNPMCommand(projectFolder, List.of("rebuild"));
			rebuild.runCommand(mon);
			if (rebuild.getExitCode() != 0)
			{
				writeConsole(console, "- pnpm mode: 'pnpm rebuild' exit code was " + rebuild.getExitCode() + " (continuing)");
			}
		}
		catch (IOException | InterruptedException e)
		{
			Activator.getInstance().getLog().error("Error running 'pnpm rebuild' in " + projectFolder, e);
			if (e instanceof InterruptedException) Thread.currentThread().interrupt();
		}
	}

	/**
	 * Parses the versioned <code>ignoredBuilds</code> entries out of a <code>node_modules/.modules.yaml</code> file.
	 * <p>
	 * pnpm writes this metadata file after an install; its <code>ignoredBuilds</code> key lists the exact
	 * <code>name@version</code> entries whose build scripts were blocked. Depending on the pnpm version the value is
	 * written either as a JSON/flow array:
	 *
	 * <pre>
	 * ignoredBuilds: ["esbuild@0.28.1", "esbuild@0.28.2", "lmdb@3.5.6"]
	 * </pre>
	 *
	 * or as a block-style YAML list:
	 *
	 * <pre>
	 * ignoredBuilds:
	 *   - esbuild@0.28.1
	 *   - esbuild@0.28.2
	 * </pre>
	 *
	 * Both forms are handled: collection starts at the <code>ignoredBuilds:</code> key and stops at the next
	 * top-level key (a non-indented <code>key:</code> line) or the closing bracket of a flow array. Entries are
	 * unquoted, trimmed and validated as <code>name@version</code>; anything else (<code>pendingBuilds: []</code>,
	 * other keys, prose) is ignored. This is an isolated, side-effect-free static method so it can be unit-tested
	 * directly. On a null/empty/malformed input an empty set is returned (discovery never fails a build).
	 *
	 * @param content the full text of <code>node_modules/.modules.yaml</code> (may be <code>null</code>)
	 * @return the discovered <code>name@version</code> entries, in file order (possibly empty, never <code>null</code>)
	 */
	static Set<String> parseModulesYamlIgnoredBuilds(String content)
	{
		Set<String> result = new LinkedHashSet<>();
		if (content == null || content.isBlank()) return result;
		String[] lines = content.split("\\r?\\n", -1);
		boolean inSection = false;
		for (String rawLine : lines)
		{
			String line = rawLine;
			if (!inSection)
			{
				int idx = indexOfIgnoredBuildsKey(line);
				if (idx < 0) continue;
				// everything after the key's colon on the same line (handles the inline flow-array form, quoted or
				// unquoted key). Slice from the first ':' at/after the key start rather than a fixed key length.
				int colon = line.indexOf(':', idx);
				String after = colon >= 0 ? line.substring(colon + 1) : "";
				if (after.contains("]"))
				{
					// a self-contained single-line flow array: ignoredBuilds: [ ... ]
					collectTokens(after, result);
					continue; // stays out of section; a one-line array is complete
				}
				collectTokens(after, result);
				inSection = true;
				continue;
			}
			// inside a multi-line section: a flow array spilled over lines, or a block list of "- entry" items.
			if (line.contains("]"))
			{
				collectTokens(line, result);
				inSection = false;
				continue;
			}
			// a new top-level key (no leading whitespace and a ':') ends the block list.
			if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0)) && line.contains(":"))
			{
				inSection = false;
				continue;
			}
			collectTokens(line, result);
		}
		return result;
	}

	/**
	 * @return the column at which the <code>ignoredBuilds:</code> key starts on the given line (ignoring leading
	 *         whitespace), or <code>-1</code> if this line is not that key.
	 */
	private static int indexOfIgnoredBuildsKey(String line)
	{
		String trimmed = line.stripLeading();
		// pnpm 12 writes .modules.yaml as JSON, so the key is quoted ("ignoredBuilds":); older/YAML forms use a bare
		// ignoredBuilds:. Accept both. Must NOT match "pendingBuilds"/other keys.
		if (trimmed.startsWith("ignoredBuilds:") || trimmed.startsWith("\"ignoredBuilds\":"))
		{
			return line.length() - trimmed.length();
		}
		return -1;
	}

	/**
	 * Extracts every <code>name@version</code> token from a fragment of a <code>.modules.yaml</code> value (a flow
	 * array piece like <code>["esbuild@0.28.1", "lmdb@3.5.6"]</code> or a block-list item like
	 * <code>- esbuild@0.28.1</code>), adding the valid ones to the sink. Bracket, comma, quote, dash and bullet
	 * decoration is stripped; only tokens matching {@link #NAME_AT_VERSION} are kept.
	 */
	private static void collectTokens(String fragment, Set<String> sink)
	{
		if (fragment == null) return;
		// split on the YAML/JSON structural characters that separate entries; quotes are also treated as delimiters
		// so a quoted "esbuild@0.28.1" yields the bare token.
		for (String piece : fragment.split("[\\[\\],\"']+"))
		{
			String token = piece.replaceFirst("^[\\s\\u2022\\-*>·]+", "").trim();
			if (!token.isEmpty() && NAME_AT_VERSION.matcher(token).matches())
			{
				sink.add(token);
			}
		}
	}

	/**
	 * Fallback parser for the versioned <code>ERR_PNPM_IGNORED_BUILDS</code> error line, used only when
	 * <code>node_modules/.modules.yaml</code> is unavailable. pnpm 12 prints the blocked builds as a comma-separated,
	 * <b>versioned</b> list, e.g.:
	 *
	 * <pre>
	 * Ignored build scripts: esbuild@0.28.1, esbuild@0.28.2, lmdb@3.5.6.
	 * </pre>
	 *
	 * The list portion after the colon is split on commas and each entry validated as <code>name@version</code>; a
	 * trailing period and surrounding whitespace are tolerated. Lines without that marker contribute nothing. On
	 * null/empty input an empty set is returned.
	 *
	 * @param outputLines captured pnpm console output lines (may be <code>null</code>)
	 * @return the discovered <code>name@version</code> entries (possibly empty, never <code>null</code>)
	 */
	static Set<String> parseIgnoredBuildsErrorLine(List<String> outputLines)
	{
		Set<String> result = new LinkedHashSet<>();
		if (outputLines == null) return result;
		for (String raw : outputLines)
		{
			if (raw == null) continue;
			for (String line : raw.split("\\r?\\n", -1))
			{
				String lower = line.toLowerCase();
				int colon = -1;
				if (lower.contains("ignored build scripts")) colon = line.indexOf(':', lower.indexOf("ignored build scripts"));
				else if (lower.contains("err_pnpm_ignored_builds")) colon = line.indexOf(':', lower.indexOf("err_pnpm_ignored_builds"));
				if (colon < 0) continue;
				collectTokens(line.substring(colon + 1), result);
			}
		}
		return result;
	}

	/** The committed/generated pnpm lockfile name (seeded from and committed back to <code>ng_web_packages</code>). */
	public static final String PNPM_LOCK_YAML = "pnpm-lock.yaml";

	/**
	 * The committed/generated solution <code>package.json</code> name (SVY-21502 §3.11). The committed
	 * <code>ng_web_packages/package.json</code> is the FULL-set counterpart of the committed lockfile: the two are a
	 * matched pair (the lockfile is exactly the resolution of this package.json) covering every component tarball the
	 * developer's full build packed. They are seeded together and committed back together so a frozen WAR/CI install
	 * has a package.json that agrees with the lockfile.
	 */
	public static final String PACKAGE_JSON = "package.json";

	/**
	 * SVY-21502 §3.10: seeds the generated <code>&lt;projectFolder&gt;/pnpm-lock.yaml</code> from the committed
	 * <code>&lt;solution-project&gt;/ng_web_packages/pnpm-lock.yaml</code> if it exists, so both the Developer build
	 * and the WAR/CI build install the exact same, committed dependency versions. This is an EXPLICIT copy, not part
	 * of {@link NodeFolderCreatorJob}'s template copy (which skips lockfiles - SVY-21502 §2.3). A missing committed
	 * lockfile is a no-op (a first build with no committed lockfile proceeds unseeded).
	 *
	 * @param solutionProject the solution's Eclipse project (may be <code>null</code> -> no-op)
	 * @param projectFolder the solution's standalone pnpm project folder (<code>target/&lt;solution&gt;/</code>)
	 * @param console optional console for progress messages (may be <code>null</code>)
	 */
	public static void seedLockfileFromCommitted(IProject solutionProject, File projectFolder, StringOutputStream console)
	{
		if (solutionProject == null || projectFolder == null || !Activator.isPnpmMode()) return;
		try
		{
			File webPackages = webPackagesFolder(solutionProject);
			File committedLock = new File(webPackages, PNPM_LOCK_YAML);
			if (!committedLock.exists()) return;
			// SVY-21502 §3.11: seed the committed package.json + pnpm-lock.yaml as a MATCHED PAIR. pnpm's
			// --frozen-lockfile compares the package.json specifier set against the lockfile (content, not mtime), so
			// a seeded lockfile that lists the component tarballs is only usable if the seeded package.json declares
			// those same tarballs. The generated core package.json does NOT (NodeFolderCreatorJob copies the shipped
			// core package.json and strips @servoy/public), so without this the frozen install fails
			// ERR_PNPM_OUTDATED_LOCKFILE. Both files are the FULL component set; the WAR path then trims the
			// not-exported components with 'pnpm remove' (see removeNonExportedComponents) before installing frozen.
			File committedPkg = new File(webPackages, PACKAGE_JSON);
			if (committedPkg.exists())
			{
				FileUtils.copyFile(committedPkg, new File(projectFolder, PACKAGE_JSON));
			}
			FileUtils.copyFile(committedLock, new File(projectFolder, PNPM_LOCK_YAML));
			writeConsole(console, "- pnpm mode: seeded package.json + pnpm-lock.yaml from committed ng_web_packages");
		}
		catch (IOException | RuntimeException e)
		{
			// seeding is best-effort; a build can still proceed (just not frozen to the committed versions).
			Activator.getInstance().getLog().error("Error seeding package.json/pnpm-lock.yaml from committed ng_web_packages in " + projectFolder, e);
		}
	}

	/**
	 * SVY-21502 §3.10 / §3.5d: after a successful Developer install/rebuild, copies the resulting
	 * <code>&lt;projectFolder&gt;/pnpm-lock.yaml</code> back to the committed
	 * <code>&lt;solution-project&gt;/ng_web_packages/pnpm-lock.yaml</code> (creating the folder if needed) and
	 * best-effort refreshes it in the workspace, so the developer commits the exact versions they installed. This is
	 * called only on the Developer path - CI/WAR must NOT mutate committed sources.
	 *
	 * @param solutionProject the solution's Eclipse project (may be <code>null</code> -> no-op)
	 * @param projectFolder the solution's standalone pnpm project folder (<code>target/&lt;solution&gt;/</code>)
	 * @param console optional console for progress messages (may be <code>null</code>)
	 */
	public static void commitLockfileBack(IProject solutionProject, File projectFolder, StringOutputStream console)
	{
		if (solutionProject == null || projectFolder == null || !Activator.isPnpmMode()) return;
		try
		{
			File generatedLock = new File(projectFolder, PNPM_LOCK_YAML);
			if (!generatedLock.exists()) return;
			File webPackages = webPackagesFolder(solutionProject);
			if (!webPackages.exists()) webPackages.mkdirs();
			// SVY-21502 §3.11: commit the generated package.json + pnpm-lock.yaml back as a MATCHED PAIR (the FULL
			// component set the developer's full build produced). The WAR/CI path restores both and trims the
			// not-exported components; committing only the lockfile would leave package.json/lockfile out of sync.
			File generatedPkg = new File(projectFolder, PACKAGE_JSON);
			if (generatedPkg.exists())
			{
				FileUtils.copyFile(generatedPkg, new File(webPackages, PACKAGE_JSON));
			}
			FileUtils.copyFile(generatedLock, new File(webPackages, PNPM_LOCK_YAML));
			writeConsole(console, "- pnpm mode: committed package.json + pnpm-lock.yaml back to ng_web_packages");
			try
			{
				solutionProject.getFolder(SolutionSerializer.NG_PACKAGES_DIR_NAME).refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
			}
			catch (CoreException ce)
			{
				// refresh is best-effort; the files are already written to disk
				Activator.getInstance().getLog().error("Error refreshing ng_web_packages after committing back package.json/pnpm-lock.yaml", ce);
			}
		}
		catch (IOException | RuntimeException e)
		{
			Activator.getInstance().getLog().error("Error committing package.json/pnpm-lock.yaml back to ng_web_packages in " + projectFolder, e);
		}
	}

	/** @return the solution's <code>ng_web_packages</code> directory as a {@link File}. */
	private static File webPackagesFolder(IProject solutionProject)
	{
		return solutionProject.getFolder(SolutionSerializer.NG_PACKAGES_DIR_NAME).getLocation().toFile();
	}

	private static void writeConsole(StringOutputStream console, String message)
	{
		if (console == null) return;
		try
		{
			console.write(message + "\n");
		}
		catch (IOException e)
		{
			// ignore console write failures
		}
	}
}

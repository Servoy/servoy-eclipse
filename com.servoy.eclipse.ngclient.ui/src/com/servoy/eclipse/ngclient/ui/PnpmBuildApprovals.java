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
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.model.repository.SolutionSerializer;

/**
 * SVY-21502: per-solution allow/deny list of pnpm dependency build scripts.
 * <p>
 * This is the single reader/writer of the git-committed
 * <code>&lt;solution-project&gt;/ng_web_packages/pnpm-build-approvals.json</code> file that holds both an
 * <code>allowed</code> and a <code>denied</code> list of <b>exact <code>name@version</code></b> entries. In the
 * generated solution-local <code>pnpm-workspace.yaml</code> each allowed <code>name@version</code> becomes an
 * <code>allowBuilds: '&lt;name@version&gt;': true</code> entry and each denied <code>name@version</code> an
 * <code>allowBuilds: '&lt;name@version&gt;': false</code> entry (pnpm 12 removed the pnpm-10
 * <code>onlyBuiltDependencies</code>/<code>ignoredBuiltDependencies</code> keys); a <code>name@version</code> in
 * neither list is <em>pending</em> and must be decided before a Developer build proceeds. Approval is version-scoped
 * on purpose: a transitive bump to a version the developer never reviewed is automatically pending, so a newly
 * published (possibly compromised) version cannot inherit a prior version's trust.
 * <p>
 * The helper is deliberately SWT-free so it can be used both by the Developer build and by the headless WAR export
 * CLI (which has no UI). The file lives next to the component/service web-package zips in the solution's
 * <code>ng_web_packages</code> directory ({@link SolutionSerializer#NG_PACKAGES_DIR_NAME}), so it is git-committed
 * and travels with the solution.
 *
 * @author jcompagner
 * @since 2026.12
 */
public class PnpmBuildApprovals
{
	/** The committed file name inside the solution's <code>ng_web_packages</code> directory. */
	public static final String APPROVALS_FILE_NAME = "pnpm-build-approvals.json";

	/** Current on-disk format version. */
	public static final int CURRENT_VERSION = 1;

	private static final String KEY_VERSION = "version";
	private static final String KEY_ALLOWED = "allowed";
	private static final String KEY_DENIED = "denied";

	private int version = CURRENT_VERSION;
	// LinkedHashSet keeps a stable, hand-reviewable order in the committed file.
	private final Set<String> allowed = new LinkedHashSet<>();
	private final Set<String> denied = new LinkedHashSet<>();

	/** Creates an empty set of approvals (no allowed, no denied). */
	public PnpmBuildApprovals()
	{
	}

	/**
	 * Loads the approvals from <code>&lt;project&gt;/ng_web_packages/pnpm-build-approvals.json</code>. A missing
	 * folder or missing file yields an empty (but valid) {@link PnpmBuildApprovals}.
	 *
	 * @param project the solution's Eclipse project (may be <code>null</code> -> empty approvals)
	 */
	public static PnpmBuildApprovals fromWebPackagesFolder(IProject project)
	{
		if (project == null) return new PnpmBuildApprovals();
		File folder = project.getFolder(SolutionSerializer.NG_PACKAGES_DIR_NAME).getLocation().toFile();
		return fromFile(new File(folder, APPROVALS_FILE_NAME));
	}

	/**
	 * Loads the approvals from the given file. A missing or empty/unreadable file yields an empty (but valid)
	 * {@link PnpmBuildApprovals} so that a build with no committed decisions is still well-defined.
	 *
	 * @param file the <code>pnpm-build-approvals.json</code> file (may be <code>null</code> or non-existent)
	 */
	public static PnpmBuildApprovals fromFile(File file)
	{
		PnpmBuildApprovals approvals = new PnpmBuildApprovals();
		if (file == null || !file.exists()) return approvals;
		try
		{
			String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
			if (content == null || content.isBlank()) return approvals;
			JSONObject json = new JSONObject(content);
			approvals.version = json.optInt(KEY_VERSION, CURRENT_VERSION);
			addAll(approvals.allowed, json.optJSONArray(KEY_ALLOWED));
			addAll(approvals.denied, json.optJSONArray(KEY_DENIED));
		}
		catch (IOException | RuntimeException e)
		{
			Activator.getInstance().getLog().error("Error reading pnpm build approvals from " + file.getAbsolutePath(), e);
		}
		return approvals;
	}

	private static void addAll(Set<String> target, JSONArray array)
	{
		if (array == null) return;
		for (int i = 0; i < array.length(); i++)
		{
			String name = array.optString(i, null);
			if (name != null && !name.isBlank()) target.add(name.trim());
		}
	}

	/**
	 * Persists these approvals into <code>&lt;project&gt;/ng_web_packages/pnpm-build-approvals.json</code>, creating
	 * the <code>ng_web_packages</code> folder if it does not exist yet.
	 *
	 * @param project the solution's Eclipse project
	 * @throws IOException if the file cannot be written
	 */
	public void saveToWebPackagesFolder(IProject project) throws IOException
	{
		if (project == null) return;
		File folder = project.getFolder(SolutionSerializer.NG_PACKAGES_DIR_NAME).getLocation().toFile();
		if (!folder.exists()) folder.mkdirs();
		save(new File(folder, APPROVALS_FILE_NAME));
	}

	/**
	 * Persists these approvals into the given file as a small, hand-reviewable JSON document.
	 *
	 * @param file the <code>pnpm-build-approvals.json</code> file to (over)write
	 * @throws IOException if the file cannot be written
	 */
	public void save(File file) throws IOException
	{
		JSONObject json = new JSONObject();
		json.put(KEY_VERSION, version);
		json.put(KEY_ALLOWED, new JSONArray(allowed));
		json.put(KEY_DENIED, new JSONArray(denied));
		File parent = file.getParentFile();
		if (parent != null && !parent.exists()) parent.mkdirs();
		FileUtils.writeStringToFile(file, json.toString(2), StandardCharsets.UTF_8);
	}

	public int getVersion()
	{
		return version;
	}

	/**
	 * @return the allowed <code>name@version</code> entries (pnpm <code>allowBuilds: '&lt;name@version&gt;': true</code>),
	 *         in a stable order.
	 */
	public Set<String> getAllowed()
	{
		return new LinkedHashSet<>(allowed);
	}

	/**
	 * @return the denied <code>name@version</code> entries (pnpm <code>allowBuilds: '&lt;name@version&gt;': false</code>),
	 *         in a stable order.
	 */
	public Set<String> getDenied()
	{
		return new LinkedHashSet<>(denied);
	}

	/**
	 * Computes the <em>pending</em> set: <code>name@version</code> entries that pnpm discovered as having (ignored)
	 * build scripts but that are in neither the allowed nor the denied list. These are the exact versions the
	 * developer still needs to decide on. The subtraction is by exact <code>name@version</code> string, so a new
	 * version of an already-decided package (e.g. <code>esbuild@0.28.3</code> when only <code>0.28.1</code>/
	 * <code>0.28.2</code> are decided) is correctly pending and re-asked.
	 *
	 * @param discovered the <code>name@version</code> entries pnpm reported as having pending build scripts (may be
	 *            <code>null</code>)
	 * @return the discovered entries minus the already-decided ones, in the discovered order
	 */
	public Set<String> pending(Set<String> discovered)
	{
		Set<String> result = new LinkedHashSet<>();
		if (discovered == null) return result;
		for (String name : discovered)
		{
			if (name == null || name.isBlank()) continue;
			String trimmed = name.trim();
			if (!allowed.contains(trimmed) && !denied.contains(trimmed)) result.add(trimmed);
		}
		return result;
	}

	/**
	 * Adds a <code>name@version</code> entry to the allowed list (and removes it from the denied list if present).
	 */
	public void allow(String packageName)
	{
		if (packageName == null || packageName.isBlank()) return;
		String trimmed = packageName.trim();
		denied.remove(trimmed);
		allowed.add(trimmed);
	}

	/**
	 * Adds a <code>name@version</code> entry to the denied list (and removes it from the allowed list if present).
	 */
	public void deny(String packageName)
	{
		if (packageName == null || packageName.isBlank()) return;
		String trimmed = packageName.trim();
		allowed.remove(trimmed);
		denied.add(trimmed);
	}

	/**
	 * Removes a <code>name@version</code> entry from both lists (making it pending again).
	 */
	public void remove(String packageName)
	{
		if (packageName == null) return;
		String trimmed = packageName.trim();
		allowed.remove(trimmed);
		denied.remove(trimmed);
	}
}

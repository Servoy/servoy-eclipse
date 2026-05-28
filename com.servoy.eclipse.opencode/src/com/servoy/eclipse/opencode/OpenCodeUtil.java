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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IProject;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Static utility methods shared by {@link OpenCodeView} and
 * {@link OpencodeFolderCreatorJob}.
 *
 * @author jcompagner
 * @since 2026.06
 */
class OpenCodeUtil {

	/**
	 * Returns the path to open in opencode for the currently active Servoy
	 * solution project, walking up to the git root if found.
	 *
	 * @return the path string, or {@code null} if no solution is active
	 */
	static String getActiveProjectPath() {
		IServoyModel model = ServoyModelFinder.getServoyModel();
		if (model == null)
			return null;
		ServoyProject activeProject = model.getActiveProject();
		if (activeProject == null)
			return null;
		IProject eclipseProject = activeProject.getProject();
		if (eclipseProject == null)
			return null;
		java.net.URI uri = eclipseProject.getLocationURI();
		if (uri == null)
			return null;
		try {
			Path projectDir = Paths.get(uri);
			Path gitRoot = findGitRoot(projectDir);
			return gitRoot != null ? gitRoot.toString() : projectDir.toString();
		} catch (Exception e) {
			ServoyLog.logError("OpenCodeUtil: cannot resolve active project path", e); //$NON-NLS-1$
			return null;
		}
	}

	/**
	 * Walks up from {@code dir} looking for a {@code .git} directory.
	 *
	 * @return the git root, or {@code null} if none found
	 */
	static Path findGitRoot(Path dir) {
		Path current = dir;
		while (current != null) {
			if (Files.isDirectory(current.resolve(".git"))) { //$NON-NLS-1$
				return current;
			}
			current = current.getParent();
		}
		return null;
	}

	/** Private constructor â static utility class. */
	private OpenCodeUtil() {
	}
}

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

package com.servoy.eclipse.ngclient.ui.tests.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Small file helpers for the generated-project folders of the PackageCheckerJob
 * tests.
 */
public final class ProjectFiles {
	private ProjectFiles() {
	}

	public static File write(File root, String relativePath, String content) throws IOException {
		File file = new File(root, relativePath);
		Files.createDirectories(file.getParentFile().toPath());
		Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
		return file;
	}

	public static String read(File root, String relativePath) throws IOException {
		return Files.readString(new File(root, relativePath).toPath(), StandardCharsets.UTF_8);
	}
}

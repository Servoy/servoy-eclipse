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

package com.servoy.eclipse.ngclient.ui.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.servoy.eclipse.ngclient.ui.WebPackagesListener;

/**
 * SVY-21642 - exact argument lists handed to the bundled precompress-war.mjs
 * script.
 */
class WebPackagesListenerPrecompressArgsTest {
	@TempDir
	Path tmp;

	@SuppressWarnings("unchecked")
	private static List<String> build(File dir, boolean brotli, boolean gzip) throws Exception {
		Method m = WebPackagesListener.class.getDeclaredMethod("buildPrecompressArgs", File.class, boolean.class,
				boolean.class);
		m.setAccessible(true);
		return (List<String>) m.invoke(null, dir, brotli, gzip);
	}

	@Test
	@DisplayName("brotli only gives [dir, --brotli]")
	void brotliOnly() throws Exception {
		File dir = tmp.toFile();
		assertEquals(List.of(dir.getCanonicalPath(), "--brotli"), build(dir, true, false));
	}

	@Test
	@DisplayName("gzip only gives [dir, --gzip]")
	void gzipOnly() throws Exception {
		File dir = tmp.toFile();
		assertEquals(List.of(dir.getCanonicalPath(), "--gzip"), build(dir, false, true));
	}

	@Test
	@DisplayName("both gives [dir, --brotli, --gzip] in that order")
	void both() throws Exception {
		File dir = tmp.toFile();
		assertEquals(List.of(dir.getCanonicalPath(), "--brotli", "--gzip"), build(dir, true, true));
	}

	@Test
	@DisplayName("neither format gives only the directory")
	void neither() throws Exception {
		File dir = tmp.toFile();
		assertEquals(List.of(dir.getCanonicalPath()), build(dir, false, false));
	}

	@Test
	@DisplayName("a non-canonical directory path is canonicalized")
	void canonicalizes() throws Exception {
		File dir = new File(tmp.toFile(), "sub" + File.separator + ".." + File.separator);
		assertEquals(tmp.toFile().getCanonicalPath(), build(dir, true, false).get(0));
	}
}

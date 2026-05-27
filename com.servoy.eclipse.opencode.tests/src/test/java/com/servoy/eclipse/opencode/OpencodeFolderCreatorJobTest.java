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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for the package-visible helper methods of
 * {@link OpencodeFolderCreatorJob}.
 * <p>
 * No OSGi runtime is required — all tests operate on temporary file-system
 * resources created by {@link TemporaryFolder}.
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
public class OpencodeFolderCreatorJobTest
{
	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	// --- needsInstall ---

	@Test
	public void needsInstall_markerAbsent_returnsTrue() throws Exception
	{
		File marker = new File(tmp.getRoot(), ".fullygenerated"); // not created
		File sentinel = tmp.newFile("package_copy.json");
		Files.writeString(sentinel.toPath(), "{}", StandardCharsets.UTF_8);

		assertTrue("Missing marker should trigger install",
			OpencodeFolderCreatorJob.needsInstall(marker, sentinel, "{}"));
	}

	@Test
	public void needsInstall_sentinelAbsent_returnsTrue() throws Exception
	{
		File marker = tmp.newFile(".fullygenerated");
		File sentinel = new File(tmp.getRoot(), "package_copy.json"); // not created

		assertTrue("Missing sentinel should trigger install",
			OpencodeFolderCreatorJob.needsInstall(marker, sentinel, "{}"));
	}

	@Test
	public void needsInstall_contentMatches_returnsFalse() throws Exception
	{
		String content = "{\"name\":\"servoy-opencode-host\",\"version\":\"1.0.0\"}";
		File marker = tmp.newFile(".fullygenerated");
		File sentinel = tmp.newFile("package_copy.json");
		Files.writeString(sentinel.toPath(), content, StandardCharsets.UTF_8);

		assertFalse("Matching sentinel should NOT trigger install",
			OpencodeFolderCreatorJob.needsInstall(marker, sentinel, content));
	}

	@Test
	public void needsInstall_contentDiffers_returnsTrue() throws Exception
	{
		File marker = tmp.newFile(".fullygenerated");
		File sentinel = tmp.newFile("package_copy.json");
		Files.writeString(sentinel.toPath(), "{\"version\":\"^1.0.0\"}", StandardCharsets.UTF_8);

		assertTrue("Changed sentinel content should trigger re-install",
			OpencodeFolderCreatorJob.needsInstall(marker, sentinel, "{\"version\":\"^2.0.0\"}"));
	}

	@Test
	public void needsInstall_bothPresent_emptyBundle_returnsFalse() throws Exception
	{
		// Edge case: empty string matches empty string — no re-install
		File marker = tmp.newFile(".fullygenerated");
		File sentinel = tmp.newFile("package_copy.json");
		Files.writeString(sentinel.toPath(), "", StandardCharsets.UTF_8);

		assertFalse(OpencodeFolderCreatorJob.needsInstall(marker, sentinel, ""));
	}

	// --- readUrlContent ---

	@Test
	public void readUrlContent_readsFileUrl() throws Exception
	{
		String expected = "{\"name\":\"test\",\"version\":\"1.0\"}";
		File f = tmp.newFile("pkg.json");
		Files.writeString(f.toPath(), expected, StandardCharsets.UTF_8);

		String actual = OpencodeFolderCreatorJob.readUrlContent(f.toURI().toURL());
		assertEquals("readUrlContent should return exact file contents", expected, actual);
	}

	@Test
	public void readUrlContent_handlesUnicode() throws Exception
	{
		String expected = "Servoy é opencode …";
		File f = tmp.newFile("unicode.txt");
		Files.writeString(f.toPath(), expected, StandardCharsets.UTF_8);

		assertEquals(expected, OpencodeFolderCreatorJob.readUrlContent(f.toURI().toURL()));
	}

	// --- deleteDirectory ---

	@Test
	public void deleteDirectory_removesNestedStructure() throws Exception
	{
		File root = tmp.newFolder("toDelete");
		File sub = new File(root, "sub/deep");
		sub.mkdirs();
		new File(sub, "file.txt").createNewFile();
		new File(root, "toplevel.json").createNewFile();

		assertTrue("Directory should exist before deletion", root.exists());
		OpencodeFolderCreatorJob.deleteDirectory(root.toPath());
		assertFalse("Directory should be gone after deleteDirectory()", root.exists());
	}

	@Test
	public void deleteDirectory_emptyDir() throws Exception
	{
		File dir = tmp.newFolder("empty");
		OpencodeFolderCreatorJob.deleteDirectory(dir.toPath());
		assertFalse("Empty directory should be removed", dir.exists());
	}
}

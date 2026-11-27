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

package com.servoy.eclipse.model.tests.plainjunit.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import com.servoy.eclipse.model.util.IFileAccess;

/**
 * An in-memory {@link IFileAccess} for plain unit tests of code that reads and writes workspace files through IFileAccess. Paths are kept
 * as given; {@link #failDeleteOf(String)} makes delete of a path throw.
 */
public final class MemoryFileAccess implements IFileAccess
{
	private final TreeMap<String, byte[]> files = new TreeMap<>();
	private final Set<String> failingDeletes = new HashSet<>();

	/** The files written so far, path to UTF-8 content. */
	public TreeMap<String, String> contents()
	{
		TreeMap<String, String> result = new TreeMap<>();
		files.forEach((path, bytes) -> result.put(path, new String(bytes, StandardCharsets.UTF_8)));
		return result;
	}

	public MemoryFileAccess failDeleteOf(String path)
	{
		failingDeletes.add(path);
		return this;
	}

	@Override
	public void setUTF8Contents(String relativeFilePath, String contents) throws IOException
	{
		files.put(relativeFilePath, contents.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public String getUTF8Contents(String relativeFilePath) throws IOException
	{
		return new String(getContents(relativeFilePath), StandardCharsets.UTF_8);
	}

	@Override
	public void setContents(String relativeFilePath, byte[] contents) throws IOException
	{
		files.put(relativeFilePath, contents.clone());
	}

	@Override
	public byte[] getContents(String relativeFilePath) throws IOException
	{
		byte[] bytes = files.get(relativeFilePath);
		if (bytes == null) throw new IOException("no such file: " + relativeFilePath);
		return bytes.clone();
	}

	@Override
	public OutputStream getOutputStream(String relativeFilePath) throws IOException
	{
		return new ByteArrayOutputStream()
		{
			@Override
			public void close() throws IOException
			{
				super.close();
				files.put(relativeFilePath, toByteArray());
			}
		};
	}

	@Override
	public boolean exists(String relativeFilePath)
	{
		return files.containsKey(relativeFilePath);
	}

	@Override
	public void delete(String relativeFilePath) throws IOException
	{
		if (failingDeletes.contains(relativeFilePath)) throw new IOException("cannot delete " + relativeFilePath);
		files.remove(relativeFilePath);
	}

	@Override
	public void deleteAll(String relativeFilePath) throws IOException
	{
		files.keySet().removeIf(path -> path.equals(relativeFilePath) || path.startsWith(relativeFilePath + "/"));
	}

	@Override
	public boolean move(String relativeFilePathFrom, String relativeFilePathTo) throws IOException
	{
		byte[] bytes = files.remove(relativeFilePathFrom);
		if (bytes == null) return false;
		files.put(relativeFilePathTo, bytes);
		return true;
	}

	@Override
	public long getFileLength(String relativeFilePath) throws IOException
	{
		return getContents(relativeFilePath).length;
	}

	@Override
	public String getWorkspaceOSPath(String projectName)
	{
		return null;
	}

	@Override
	public String[] list()
	{
		return files.keySet().toArray(new String[0]);
	}

	@Override
	public File getProjectFile(String projectName)
	{
		return null;
	}

	@Override
	public void createFolder(String relativeFilePath) throws IOException
	{
	}

	@Override
	public void closeOutputStream(OutputStream os) throws IOException
	{
		os.close();
	}
}

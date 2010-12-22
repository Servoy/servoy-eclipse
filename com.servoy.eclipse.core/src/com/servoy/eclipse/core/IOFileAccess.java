/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.core;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;

import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.j2db.util.Utils;

/**
 * IFileAccess implementation for accessing files using the java.io package
 * 
 * @author rgansevles
 * 
 */
public class IOFileAccess implements IFileAccess
{
	protected final File baseDir;

	public IOFileAccess(File baseDir)
	{
		this.baseDir = baseDir;
	}

	protected File getFile(String relativeFilePath)
	{
		return new File(baseDir, relativeFilePath);
	}

	public void setUTF8Contents(String relativeFilePath, String contents) throws IOException
	{
		BufferedWriter bw = null;
		try
		{
			File file = getFile(relativeFilePath);
			if (!file.exists())
			{
				file.getParentFile().mkdirs();
			}
			OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(file), "UTF8");
			bw = new BufferedWriter(ow);
			bw.write(contents);
		}
		finally
		{
			Utils.closeWriter(bw);
		}
	}

	public void setContents(String relativeFilePath, byte[] contents) throws IOException
	{
		BufferedOutputStream bos = null;
		try
		{
			File file = getFile(relativeFilePath);
			if (!file.exists())
			{
				file.getParentFile().mkdirs();
			}
			bos = new BufferedOutputStream(new FileOutputStream(file));
			bos.write(contents, 0, contents.length);
		}
		finally
		{
			Utils.closeOutputStream(bos);
		}
	}

	public String getUTF8Contents(String relativeFilePath) throws IOException
	{
		InputStreamReader contents = null;
		try
		{
			contents = new InputStreamReader(new FileInputStream(getFile(relativeFilePath)), "UTF8");
			StringWriter stringWriter = new StringWriter();
			Utils.readerWriterCopy(contents, stringWriter);
			return stringWriter.toString();
		}
		finally
		{
			Utils.closeReader(contents);
		}
	}

	public byte[] getContents(String relativeFilePath) throws IOException
	{
		return Utils.getFileContent(getFile(relativeFilePath));
	}

	public void createFolder(String relativeFilePath) throws IOException
	{
		File dir = getFile(relativeFilePath);
		if (!dir.exists())
		{
			dir.mkdirs();
		}
	}

	public boolean exists(String relativeFilePath)
	{
		return getFile(relativeFilePath).exists();
	}

	public OutputStream getOutputStream(String relativeFilePath) throws IOException
	{
		File file = getFile(relativeFilePath);
		if (!file.exists())
		{
			file.getParentFile().mkdirs();
		}
		return new FileOutputStream(file);
	}

	public void closeOutputStream(OutputStream os) throws IOException
	{
		if (os != null) os.close();
	}

	public void delete(String relativeFilePath)
	{
		getFile(relativeFilePath).delete();
	}

	public void deleteAll(String relativeFilePath)
	{
		deleteAll(getFile(relativeFilePath));
	}

	private void deleteAll(File f)
	{
		if (!f.exists()) return;
		if (f.isDirectory())
		{
			for (File fl : f.listFiles())
				deleteAll(fl);
		}
		f.delete();
	}

	public boolean move(String relativeFilePathFrom, String relativeFilePathTo)
	{
		return getFile(relativeFilePathFrom).renameTo(getFile(relativeFilePathTo));
	}

	public long getFileLength(String relativeFilePath) throws IOException
	{
		return getFile(relativeFilePath).length();
	}

	public String toOSPath()
	{
		return baseDir.getPath();
	}

	public String[] list()
	{
		return baseDir.list();
	}

	public File toFile()
	{
		return baseDir;
	}

	@Override
	public String toString()
	{
		return "IOFileAccess(" + baseDir + ')';
	}
}

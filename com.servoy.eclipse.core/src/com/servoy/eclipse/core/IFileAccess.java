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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for accessing files.
 * 
 * @author rob
 * 
 */
public interface IFileAccess
{
	/**
	 * Write the contents to the file in UTF8 encoding. When the file or its parent folder do not exist, create it first.
	 * 
	 * @param relativeFilePath
	 * @param contents
	 * @throws IOException
	 */
	void setUTF8Contents(String relativeFilePath, String contents) throws IOException;

	String getUTF8Contents(String relativeFilePath) throws IOException;

	void setContents(String relativeFilePath, byte[] contents) throws IOException;

	byte[] getContents(String relativeFilePath) throws IOException;

	OutputStream getOutputStream(final String relativeFilePath) throws IOException;

	boolean exists(String relativeFilePath);

	void delete(String relativeFilePath) throws IOException;

	void deleteAll(String relativeFilePath) throws IOException;

	boolean move(String relativeFilePathFrom, String relativeFilePathTo) throws IOException;

	long getFileLength(String relativeFilePath) throws IOException;

	String toOSPath();

	String[] list();

	java.io.File toFile();

	/**
	 * Create if not existing yet. Also create parents when not existing yet.
	 * 
	 * @throws IOException
	 */
	void createFolder(String relativeFilePath) throws IOException;

	public void closeOutputStream(OutputStream os) throws IOException;
}

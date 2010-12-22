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

package com.servoy.eclipse.ui.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.core.search.SearchMatch;
import org.eclipse.search.core.text.TextSearchMatchAccess;

import com.servoy.eclipse.model.util.ServoyLog;

class SearchMatchAccess extends TextSearchMatchAccess
{

	private final SearchMatch match;

	private String contents = null;

	private boolean possibleMatch = false;

	/**
	 * @param match
	 */
	public SearchMatchAccess(SearchMatch match)
	{
		this.match = match;
	}

	/**
	 * @return the possibleMatch
	 */
	public boolean isPossibleMatch()
	{
		return possibleMatch;
	}

	/**
	 * @param possibleMatch the possibleMatch to set
	 */
	public void setPossibleMatch(boolean possibleMatch)
	{
		this.possibleMatch = possibleMatch;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.core.text.TextSearchMatchAccess#getFile()
	 */
	@Override
	public IFile getFile()
	{
		return (IFile)match.getResource();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.core.text.TextSearchMatchAccess#getMatchOffset()
	 */
	@Override
	public int getMatchOffset()
	{
		return match.getOffset();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.core.text.TextSearchMatchAccess#getMatchLength()
	 */
	@Override
	public int getMatchLength()
	{
		return match.getLength();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.core.text.TextSearchMatchAccess#getFileContentLength()
	 */
	@Override
	public int getFileContentLength()
	{
		readContent();
		return contents.length();
	}

	/**
	 * 
	 */
	private void readContent()
	{
		if (contents == null)
		{
			try
			{
				contents = getSource(getFile());
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
				contents = ""; //$NON-NLS-1$
			}
		}
	}

	private String getSource(IFile file) throws IOException, CoreException
	{
		String encoding = null;
		try
		{
			encoding = file.getCharset();
		}
		catch (CoreException ex)
		{
			// fall through. Take default encoding.
		}

		StringBuffer sb = new StringBuffer();
		BufferedReader br = null;
		InputStream in = null;
		try
		{
			in = file.getContents();
			if (encoding != null) br = new BufferedReader(new InputStreamReader(in, encoding));
			else br = new BufferedReader(new InputStreamReader(in));
			int read = 0;
			while ((read = br.read()) != -1)
				sb.append((char)read);
		}
		finally
		{
			if (br != null)
			{
				br.close();
			}
			if (in != null)
			{
				in.close();
			}
		}
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.core.text.TextSearchMatchAccess#getFileContentChar(int)
	 */
	@Override
	public char getFileContentChar(int offset)
	{
		readContent();
		return contents.charAt(offset);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.core.text.TextSearchMatchAccess#getFileContent(int, int)
	 */
	@Override
	public String getFileContent(int offset, int length)
	{
		readContent();
		return contents.substring(offset, offset + length);
	}

}
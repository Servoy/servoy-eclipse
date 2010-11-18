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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.Region;
import org.eclipse.search.ui.text.Match;

/**
 * A {@link Match} implementation for {@link IFile} matches.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class FileMatch extends Match
{
	private final LineElement fLineElement;
	private Region fOriginalLocation;
	private boolean possibleMatch;

	public FileMatch(IFile element, int offset, int length, LineElement lineEntry)
	{
		super(element, offset, length);
		Assert.isLegal(lineEntry != null);
		fLineElement = lineEntry;
	}

	@Override
	public void setOffset(int offset)
	{
		if (fOriginalLocation == null)
		{
			// remember the original location before changing it
			fOriginalLocation = new Region(getOffset(), getLength());
		}
		super.setOffset(offset);
	}

	@Override
	public void setLength(int length)
	{
		if (fOriginalLocation == null)
		{
			// remember the original location before changing it
			fOriginalLocation = new Region(getOffset(), getLength());
		}
		super.setLength(length);
	}

	public int getOriginalOffset()
	{
		if (fOriginalLocation != null)
		{
			return fOriginalLocation.getOffset();
		}
		return getOffset();
	}

	public int getOriginalLength()
	{
		if (fOriginalLocation != null)
		{
			return fOriginalLocation.getLength();
		}
		return getLength();
	}


	public LineElement getLineElement()
	{
		return fLineElement;
	}

	public IFile getFile()
	{
		return (IFile)getElement();
	}

	public boolean isFileSearch()
	{
		return fLineElement == null;
	}

	/**
	 * @param b
	 */
	public void setPossibleMatch(boolean possibleMatch)
	{
		this.possibleMatch = possibleMatch;
	}

	/**
	 * @return the possibleMatch
	 */
	public boolean isPossibleMatch()
	{
		return possibleMatch;
	}
}
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

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

/**
 * Class that holds the line number information of the search results.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class LineElement
{

	private final IResource fParent;

	private final int fLineNumber;
	private final int fLineStartOffset;
	private final String fLineContents;

	public LineElement(IResource parent, int lineNumber, int lineStartOffset, String lineContents)
	{
		fParent = parent;
		fLineNumber = lineNumber;
		fLineStartOffset = lineStartOffset;
		fLineContents = lineContents;
	}

	public IResource getParent()
	{
		return fParent;
	}

	public int getLine()
	{
		return fLineNumber;
	}

	public String getContents()
	{
		return fLineContents;
	}

	public int getOffset()
	{
		return fLineStartOffset;
	}

	public boolean contains(int offset)
	{
		return fLineStartOffset <= offset && offset < fLineStartOffset + fLineContents.length();
	}

	public int getLength()
	{
		return fLineContents.length();
	}

	public FileMatch[] getMatches(AbstractTextSearchResult result)
	{
		ArrayList<FileMatch> res = new ArrayList<FileMatch>();
		Match[] matches = result.getMatches(fParent);
		for (Match matche : matches)
		{
			FileMatch curr = (FileMatch)matche;
			if (curr.getLineElement() == this)
			{
				res.add(curr);
			}
		}
		return res.toArray(new FileMatch[res.size()]);
	}

	public int getNumberOfMatches(AbstractTextSearchResult result)
	{
		int count = 0;
		Match[] matches = result.getMatches(fParent);
		for (Match matche : matches)
		{
			FileMatch curr = (FileMatch)matche;
			if (curr.getLineElement() == this)
			{
				count++;
			}
		}
		return count;
	}


}
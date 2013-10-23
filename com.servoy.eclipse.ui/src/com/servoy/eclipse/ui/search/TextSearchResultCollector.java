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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

/**
 * A {@link TextSearchRequestor} that caches {@link FileMatch} objects and flushes them to the {@link AbstractTextSearchResult}
 * 
 * @author jcompagner
 * @since 6.0
 */
class TextSearchResultCollector extends TextSearchRequestor
{

	private final AbstractTextSearchResult fResult;
	private final String fileToSkip;
	private ArrayList<FileMatch> fCachedMatches;

	TextSearchResultCollector(AbstractTextSearchResult result)
	{
		fResult = result;
		this.fileToSkip = "";
	}

	TextSearchResultCollector(AbstractTextSearchResult result, String fileToSkip)
	{
		fResult = result;
		this.fileToSkip = fileToSkip;
	}

	@Override
	public boolean acceptFile(IFile file) throws CoreException
	{
		flushMatches();
		if (file.getFullPath().segment(file.getFullPath().segmentCount() - 1).equals(fileToSkip)) return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.core.text.TextSearchRequestor#reportBinaryFile(org.eclipse.core.resources.IFile)
	 */
	@Override
	public boolean reportBinaryFile(IFile file)
	{
		return false;
	}

	@Override
	public boolean acceptPatternMatch(TextSearchMatchAccess matchRequestor) throws CoreException
	{
		int matchOffset = matchRequestor.getMatchOffset();

		LineElement lineElement = getLineElement(matchOffset, matchRequestor);
		if (lineElement != null)
		{
			FileMatch fileMatch = createFileMatch(matchRequestor, matchOffset, lineElement);
			if (fileMatch != null) fCachedMatches.add(fileMatch);
		}
		return true;
	}

	/**
	 * @param matchRequestor
	 * @param matchOffset
	 * @param lineElement
	 * @return
	 */
	protected FileMatch createFileMatch(TextSearchMatchAccess matchRequestor, int matchOffset, LineElement lineElement)
	{
		return new FileMatch(matchRequestor.getFile(), matchOffset, matchRequestor.getMatchLength(), lineElement);
	}

	private LineElement getLineElement(int offset, TextSearchMatchAccess matchRequestor)
	{
		int lineNumber = 1;
		int lineStart = 0;
		if (!fCachedMatches.isEmpty())
		{
			// match on same line as last?
			FileMatch last = fCachedMatches.get(fCachedMatches.size() - 1);
			LineElement lineElement = last.getLineElement();
			if (lineElement.contains(offset))
			{
				return lineElement;
			}
			// start with the offset and line information from the last match
			lineStart = lineElement.getOffset() + lineElement.getLength();
			lineNumber = lineElement.getLine() + 1;
		}
		if (offset < lineStart)
		{
			return null; // offset before the last line
		}

		int i = lineStart;
		int contentLength = matchRequestor.getFileContentLength();
		while (i < contentLength)
		{
			char ch = matchRequestor.getFileContentChar(i++);
			if (ch == '\n' || ch == '\r')
			{
				if (ch == '\r' && i < contentLength && matchRequestor.getFileContentChar(i) == '\n')
				{
					i++;
				}
				if (offset < i)
				{
					String lineContent = getContents(matchRequestor, lineStart, i); // include line delimiter
					return new LineElement(matchRequestor.getFile(), lineNumber, lineStart, lineContent);
				}
				lineNumber++;
				lineStart = i;
			}
		}
		if (offset < i)
		{
			String lineContent = getContents(matchRequestor, lineStart, i); // until end of file
			return new LineElement(matchRequestor.getFile(), lineNumber, lineStart, lineContent);
		}
		return null; // offset outside of range
	}

	private static String getContents(TextSearchMatchAccess matchRequestor, int start, int end)
	{
		StringBuffer buf = new StringBuffer();
		for (int i = start; i < end; i++)
		{
			char ch = matchRequestor.getFileContentChar(i);
			if (Character.isWhitespace(ch) || Character.isISOControl(ch))
			{
				buf.append(' ');
			}
			else
			{
				buf.append(ch);
			}
		}
		return buf.toString();
	}

	@Override
	public void beginReporting()
	{
		fCachedMatches = new ArrayList<FileMatch>();
	}

	@Override
	public void endReporting()
	{
		flushMatches();
		fCachedMatches = null;
	}

	private void flushMatches()
	{
		if (!fCachedMatches.isEmpty())
		{
			fResult.addMatches(fCachedMatches.toArray(new Match[fCachedMatches.size()]));
			fCachedMatches.clear();
		}
	}
}
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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

/**
 * An {@link AbstractTextSearchResult} implementation for holding {@link FileMatch} results.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class FileSearchResult extends AbstractTextSearchResult implements IEditorMatchAdapter, IFileMatchAdapter
{
	private final Match[] EMPTY_ARR = new Match[0];

	private final ISearchQuery fQuery;

	private int possibleMatch;

	public FileSearchResult(ISearchQuery job)
	{
		fQuery = job;
	}

	public ImageDescriptor getImageDescriptor()
	{
		return null;
	}

	@SuppressWarnings("nls")
	public String getLabel()
	{
		String noneExactMatchLabel = "";
		if (possibleMatch > 0)
		{
			noneExactMatchLabel = " (" + possibleMatch + " none exact matches)";
		}
		return fQuery.getLabel() + " found " + getMatchCount() + " references" + noneExactMatchLabel;
	}

	public String getTooltip()
	{
		return getLabel();
	}

	public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file)
	{
		return getMatches(file);
	}

	public IFile getFile(Object element)
	{
		if (element instanceof IFile) return (IFile)element;
		return null;
	}

	public boolean isShownInEditor(Match match, IEditorPart editor)
	{
		IEditorInput ei = editor.getEditorInput();
		if (ei instanceof IFileEditorInput)
		{
			IFileEditorInput fi = (IFileEditorInput)ei;
			return match.getElement().equals(fi.getFile());
		}
		return false;
	}

	public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor)
	{
		IEditorInput ei = editor.getEditorInput();
		if (ei instanceof IFileEditorInput)
		{
			IFileEditorInput fi = (IFileEditorInput)ei;
			return getMatches(fi.getFile());
		}
		return EMPTY_ARR;
	}

	public ISearchQuery getQuery()
	{
		return fQuery;
	}

	@Override
	public IFileMatchAdapter getFileMatchAdapter()
	{
		return this;
	}

	@Override
	public IEditorMatchAdapter getEditorMatchAdapter()
	{
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#addMatch(org.eclipse.search.ui.text.Match)
	 */
	@Override
	public void addMatch(Match match)
	{
		if (match instanceof FileMatch && ((FileMatch)match).isPossibleMatch())
		{
			possibleMatch++;
		}

		super.addMatch(match);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#addMatches(org.eclipse.search.ui.text.Match[])
	 */
	@Override
	public void addMatches(Match[] matches)
	{
		for (Match matche : matches)
		{
			if (matche instanceof FileMatch && ((FileMatch)matche).isPossibleMatch())
			{
				possibleMatch++;
			}
		}
		super.addMatches(matches);
	}
}
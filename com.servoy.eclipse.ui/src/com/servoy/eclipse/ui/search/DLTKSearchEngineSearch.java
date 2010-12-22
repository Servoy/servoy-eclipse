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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.core.search.SearchEngine;
import org.eclipse.dltk.core.search.SearchMatch;
import org.eclipse.dltk.core.search.SearchParticipant;
import org.eclipse.dltk.core.search.SearchPattern;
import org.eclipse.dltk.core.search.SearchRequestor;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.text.AbstractTextSearchResult;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Solution;


/**
 * @author jcompagner
 *
 */
public abstract class DLTKSearchEngineSearch extends AbstractPersistSearch
{

	/**
	 * @author jcompagner
	 *
	 */
	private final class DLTKSearchRequestor extends SearchRequestor
	{
		private final TextSearchRequestor collector;
		private IFile currentFile;

		/**
		 * @param collector
		 * @param persist 
		 */
		private DLTKSearchRequestor(TextSearchRequestor collector)
		{
			this.collector = collector;
		}

		@Override
		public void acceptSearchMatch(SearchMatch match) throws CoreException
		{
			if (currentFile != match.getResource())
			{
				currentFile = (IFile)match.getResource();
				collector.acceptFile(currentFile);
			}
			collector.acceptPatternMatch(new SearchMatchAccess(match));
		}
	}

	/**
	 * 
	 */
	public DLTKSearchEngineSearch()
	{
		super();
	}

	@Override
	protected TextSearchResultCollector createTextSearchCollector(AbstractTextSearchResult searchResult)
	{
		return new TextSearchResultCollector(searchResult)
		{
			/*
			 * (non-Javadoc)
			 * 
			 * @see com.servoy.eclipse.ui.search.TextSearchResultCollector#createFileMatch(org.eclipse.search.core.text.TextSearchMatchAccess, int,
			 * com.servoy.eclipse.ui.search.LineElement)
			 */
			@Override
			protected FileMatch createFileMatch(TextSearchMatchAccess searchMatch, int matchOffset, LineElement lineElement)
			{
				FileMatch fileMatch = super.createFileMatch(searchMatch, matchOffset, lineElement);
				if (searchMatch instanceof SearchMatchAccess)
				{
					fileMatch.setPossibleMatch(((SearchMatchAccess)searchMatch).isPossibleMatch());
				}
				return fileMatch;
			}
		};
	}

	/**
	 * @param monitor
	 * @param collector
	 */
	protected void callDLTKSearchEngine(IProgressMonitor monitor, final TextSearchRequestor collector, IModelElement element, int limitTo, Solution sol)
	{
		IDLTKLanguageToolkit toolkit = DLTKLanguageManager.getLanguageToolkit(element);
		IDLTKSearchScope dltkScope = SearchEngine.createWorkspaceScope(toolkit);
		SearchPattern pattern = SearchPattern.createPattern(element, limitTo, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, toolkit);
		SearchParticipant[] participants = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };

		SearchRequestor requestor = new DLTKSearchRequestor(collector);
		collector.beginReporting();
		try
		{
			new SearchEngine().search(pattern, participants, dltkScope, requestor, monitor);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
		finally
		{
			collector.endReporting();
		}
	}

}
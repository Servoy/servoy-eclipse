/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import org.eclipse.core.resources.IResource;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.TextSearchQueryProvider;
import org.eclipse.ui.IWorkingSet;

/**
 * This class consists of code from org.eclipse.search2.internal.ui.text2.DefaultTextSearchQueryProvider
 * but is used to return the SerovyFileSearchQuery for each created query, in order to use 
 * Servoy's SearchResultUpdater class.
 * 
 * @author acostache
 *
 */
public class ServoyTextSearchQueryProvider extends TextSearchQueryProvider
{
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.text.TextSearchQueryProvider#createQuery(TextSearchInput)
	 */
	@Override
	public ISearchQuery createQuery(TextSearchInput input)
	{
		FileTextSearchScope scope = input.getScope();
		String text = input.getSearchText();
		boolean regEx = input.isRegExSearch();
		boolean caseSensitive = input.isCaseSensitiveSearch();
		return new ServoyFileSearchQuery(text, regEx, caseSensitive, scope);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.text.TextSearchQueryProvider#createQuery(java.lang.String)
	 */
	@Override
	public ISearchQuery createQuery(String searchForString)
	{
		FileTextSearchScope scope = FileTextSearchScope.newWorkspaceScope(getPreviousFileNamePatterns(), false);
		return new ServoyFileSearchQuery(searchForString, false, true, scope);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.text.TextSearchQueryProvider#createQuery(java.lang.String, org.eclipse.core.resources.IResource[])
	 */
	@Override
	public ISearchQuery createQuery(String selectedText, IResource[] resources)
	{
		FileTextSearchScope scope = FileTextSearchScope.newSearchScope(resources, getPreviousFileNamePatterns(), false);
		return new ServoyFileSearchQuery(selectedText, false, true, scope);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.text.TextSearchQueryProvider#createQuery(java.lang.String, org.eclipse.ui.IWorkingSet[])
	 */
	@Override
	public ISearchQuery createQuery(String selectedText, IWorkingSet[] ws)
	{
		FileTextSearchScope scope = FileTextSearchScope.newSearchScope(ws, getPreviousFileNamePatterns(), false);
		return new ServoyFileSearchQuery(selectedText, false, true, scope);
	}

	private String[] getPreviousFileNamePatterns()
	{
		return new String[] { "*" }; //$NON-NLS-1$
	}
}

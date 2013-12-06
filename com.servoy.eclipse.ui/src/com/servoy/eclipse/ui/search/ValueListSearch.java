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

import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.FileTextSearchScope;

import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.Pair;

/**
 * An {@link ISearchQuery} implementation for finding valuelists in frm and js files.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class ValueListSearch extends AbstractPersistSearch
{
	private final ValueList valueList;

	public ValueListSearch(ValueList valueList)
	{
		this.valueList = valueList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException
	{
		IResource[] scopes = getScopes((Solution)valueList.getRootObject());
		TextSearchResultCollector collector = getResultCollector();

		FileTextSearchScope scope = FileTextSearchScope.newSearchScope(scopes, new String[] { "*.frm", "*.val" }, true);
		TextSearchEngine.create().search(scope, collector, Pattern.compile(valueList.getUUID().toString()), monitor);

		scope = FileTextSearchScope.newSearchScope(scopes, new String[] { "*.js" }, true);
		TextSearchEngine.create().search(scope, collector, Pattern.compile("etValueList.*\"" + valueList.getName() + "\""), monitor);
		TextSearchEngine.create().search(scope, collector, Pattern.compile("etValueList.*'" + valueList.getName() + "'"), monitor);


		return Status.OK_STATUS;
	}

	@Override
	protected TextSearchResultCollector createTextSearchCollector(AbstractTextSearchResult searchResult)
	{
		Pair<String, String> filePathPair = SolutionSerializer.getFilePath(valueList, false);
		final String fileName = "/" + filePathPair.getLeft() + filePathPair.getRight(); //$NON-NLS-1$

		return new TextSearchResultCollector(searchResult)
		{
			@Override
			public boolean acceptFile(IFile file) throws CoreException
			{
				// ignore the declaration.
				if (file.getFullPath().toPortableString().equals(fileName))
				{
					super.acceptFile(file);
					return false;
				}
				return super.acceptFile(file);
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel()
	{
		return "Searching references to valuelist '" + valueList.getName() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
	}


}

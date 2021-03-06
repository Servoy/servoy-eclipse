/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.InMemServerWrapper;

/**
 * @author Diana
 *
 * An {@link ISearchQuery} implementation for finding in memory datasources in frm and js files.
 */
public class InMemDatasourceSearch extends DLTKSearchEngineSearch
{

	private final InMemServerWrapper inMemServerWrapper;

	public InMemDatasourceSearch(InMemServerWrapper inMemServerWrapper)
	{
		this.inMemServerWrapper = inMemServerWrapper;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException
	{
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (activeProject == null) return Status.OK_STATUS;
		IResource[] scopes = getAllScopes();
		final TextSearchRequestor collector = getResultCollector();

		FileTextSearchScope scope = FileTextSearchScope.newSearchScope(scopes, new String[] { "*.frm", "*.rel", "*.val", "*.js" }, true);
		TextSearchEngine.create().search(scope, collector,
			Pattern.compile(inMemServerWrapper.getServerName() + ":" + inMemServerWrapper.getTableName()),
			monitor);

		return Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	@Override
	public String getLabel()
	{
		return "Searching references to in memory datasource '" + inMemServerWrapper.getTableName() + "'";
	}

}

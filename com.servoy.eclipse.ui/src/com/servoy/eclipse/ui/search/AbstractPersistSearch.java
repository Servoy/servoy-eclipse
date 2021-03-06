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
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.Solution;

/**
 * Base persist search implementation of {@link ISearchQuery}
 *
 * @author jcompagner
 * @since 6.0
 */
public abstract class AbstractPersistSearch implements ISearchQuery
{

	private FileSearchResult fResult;
	private final boolean updateSearchResults;

	/**
	 *
	 */
	public AbstractPersistSearch()
	{
		super();
		updateSearchResults = true;
	}

	public AbstractPersistSearch(boolean updateSearchResults)
	{
		super();
		this.updateSearchResults = updateSearchResults;
	}

	/**
	 * @return
	 */
	protected TextSearchResultCollector getResultCollector()
	{
		AbstractTextSearchResult searchResult = (AbstractTextSearchResult)getSearchResult();
		searchResult.removeAll();
		return createTextSearchCollector(searchResult);
	}

	/**
	 * @param searchResult
	 * @return
	 */
	protected TextSearchResultCollector createTextSearchCollector(AbstractTextSearchResult searchResult)
	{
		return new TextSearchResultCollector(searchResult);
	}

	/**
	 * Returns a list of the Projects of a solution that has the given solution as a module.
	 * Only the Projects that have the given solution as a module will be included.
	 *
	 * @param sol
	 */
	protected IResource[] getScopes(Solution sol)
	{
		List<IResource> scopes = new ArrayList<IResource>();
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

		// get the active solution and then all its modules (this is already flattened)
		// this should include the start search project/solution itself.
		ServoyProject currentProject = servoyModel.getActiveProject();
		Solution[] allSolutions = currentProject.getModules();

		for (Solution solution : allSolutions)
		{
			ServoyProject servoyProject = servoyModel.getServoyProject(solution.getName());
			Solution[] modules = servoyProject.getModules();
			for (Solution module : modules)
			{
				if (module.equals(sol))
				{
					// one module of this solution is the start solution, add this solution to the list
					scopes.add(servoyProject.getProject());
					break;
				}
			}
		}
		return scopes.toArray(new IResource[scopes.size()]);
	}


	/**
	 * Returns a list of all the Projects of the active solution, including all its modules.
	 *
	 */
	protected IResource[] getAllScopes()
	{
		List<IResource> scopes = new ArrayList<IResource>();
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

		ServoyProject currentProject = servoyModel.getActiveProject();
		Solution[] allSolutions = currentProject.getModules();

		for (Solution solution : allSolutions)
		{
			ServoyProject servoyProject = servoyModel.getServoyProject(solution.getName());
			scopes.add(servoyProject.getProject());
		}
		return scopes.toArray(new IResource[scopes.size()]);
	}

	protected IResource[] getAllScopesAndActiveResourceProject()
	{
		List<IResource> scopes = new ArrayList<IResource>(Arrays.asList(getAllScopes()));

		ServoyProject currentProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (currentProject != null && currentProject.getResourcesProject() != null)
		{
			scopes.add(currentProject.getResourcesProject().getProject());
		}

		return scopes.toArray(new IResource[scopes.size()]);
	}

	public boolean canRerun()
	{
		return true;
	}

	public boolean canRunInBackground()
	{
		return true;
	}

	public ISearchResult getSearchResult()
	{
		if (fResult == null)
		{
			fResult = new FileSearchResult(this);
			if (updateSearchResults)
			{
				new SearchResultUpdater(fResult);
			}

		}
		return fResult;
	}

}
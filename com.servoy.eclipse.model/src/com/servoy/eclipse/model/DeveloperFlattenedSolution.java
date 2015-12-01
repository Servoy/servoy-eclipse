/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.model;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.servoy.eclipse.model.builder.ScriptingUtils;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.EnumDataProvider;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;


/**
 * This class filters out, in Developer only, things which should not be allowed, as (currently) improper usage
 * of import hooks (for instance, pre-import hooks need special attention as their objects cannot be used
 * inside the main solution)
 *
 * @author acostache
 *
 */
public class DeveloperFlattenedSolution extends FlattenedSolution
{
	public DeveloperFlattenedSolution(boolean cacheFlattenedForms)
	{
		super(cacheFlattenedForms);
	}

	/*
	 * When the main solution is not an in import hook, filter out all import hooks. Modules of the import hook that are not directly modules of the solution
	 * are also excluded.
	 */
	private Solution[] filterModules(String mainSolutionName, Solution[] modules)
	{
		// put modules in a map
		Map<String, Solution> map = new HashMap<String, Solution>();
		for (Solution m : modules)
		{
			map.put(m.getName(), m);
		}

		// find the main solution
		Solution mainSolution = map.get(mainSolutionName);
		if (mainSolution == null)
		{
			// should not happen, throw?
			return modules;
		}

		if (SolutionMetaData.isImportHook(mainSolution.getSolutionMetaData()))
		{
			// import hook has all
			return modules;
		}

		// main solution is not an import hook, filter out import hooks and their modules
		List<Solution> filteredModules = new ArrayList<Solution>();
		List<Solution> toProcess = new ArrayList<Solution>();
		toProcess.add(mainSolution);
		while (toProcess.size() > 0)
		{
			Solution solution = toProcess.remove(0);
			if (solution != null && !SolutionMetaData.isImportHook(solution.getSolutionMetaData()) && !filteredModules.contains(solution))
			{
				filteredModules.add(solution);
				for (String modName : Utils.getTokenElements(solution.getModulesNames(), ",", true))
				{
					toProcess.add(map.get(modName));
				}
			}
		}

		return filteredModules.toArray(new Solution[filteredModules.size()]);
	}

	@Override
	protected void setSolutionAndModules(String mainSolutionName, Solution[] mods) throws RemoteException
	{
		super.setSolutionAndModules(mainSolutionName, filterModules(mainSolutionName, mods));
	}

	@Override
	protected void addGlobalsScope(Map<String, Pair<String, IRootObject>> scopes)
	{
		//NOP
	}

	@Override
	protected IDataProvider getEnumDataProvider(String id) throws RepositoryException
	{
		String[] enumParts = id.split("\\.");
		if (enumParts.length > 3)
		{
			IDataProvider globalDataProvider = getGlobalDataProvider(enumParts[0] + '.' + enumParts[1] + '.' + enumParts[2]);
			if (globalDataProvider instanceof ScriptVariable && ((ScriptVariable)globalDataProvider).isEnum())
			{
				List<EnumDataProvider> enumDataProviders = ScriptingUtils.getEnumDataProviders((ScriptVariable)globalDataProvider);
				for (EnumDataProvider enumProvider : enumDataProviders)
				{
					if (enumProvider.getDataProviderID().equals(id))
					{
						return enumProvider;
					}
				}
			}
		}

		return null;
	}

	@Override
	public ITable getTable(String dataSource)
	{
		return ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(dataSource);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.FlattenedSolution#getGlobalDataProvider(java.lang.String, boolean)
	 */
	@Override
	public IDataProvider getGlobalDataProvider(String id, boolean quiet) throws RepositoryException
	{
		int lastIndex = id.lastIndexOf('.');
		if (lastIndex != -1)
		{
			Relation[] relationSequence = getRelationSequence(id.substring(0, lastIndex));
			if (relationSequence != null && relationSequence.length > 0)
			{
				ITable foreignTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(
					relationSequence[relationSequence.length - 1].getForeignDataSource());
				if (foreignTable != null)
				{
					return getDataProviderForTable(foreignTable, id.substring(lastIndex + 1));
				}

			}
		}
		return super.getGlobalDataProvider(id, quiet);
	}
}

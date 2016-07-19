/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.designer.webpackage.endpoint;

import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;

/**
 * @author jcompagner
 *
 */
public class GetSolutionList implements IDeveloperService, IActiveProjectListener
{
	public static final String GET_SOLUTION_LIST_METHOD = "getSolutionList";

	private final WebPackageManagerEndpoint endpoint;

	public GetSolutionList(WebPackageManagerEndpoint endpoint)
	{
		this.endpoint = endpoint;
		ServoyModelManager.getServoyModelManager().getServoyModel().addActiveProjectListener(this);
	}

	@Override
	public JSONArray executeMethod(JSONObject msg)
	{
		ServoyProject[] modulesOfActiveProject = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
		JSONArray array = new JSONArray();
		for (int i = 0; i < modulesOfActiveProject.length; i++)
		{
			array.put(i, modulesOfActiveProject[i].getSolutionMetaData().getName());
		}
		return array;
	}

	@Override
	public void dispose()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel().removeActiveProjectListener(this);
	}

	@Override
	public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
	{
		return true;
	}

	@Override
	public void activeProjectChanged(ServoyProject activeProject)
	{
	}

	@Override
	public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
	{
		if (updateInfo == IActiveProjectListener.MODULES_UPDATED)
		{
			JSONArray packages = executeMethod(null);
			JSONObject jsonResult = new JSONObject();
			jsonResult.put("method", GET_SOLUTION_LIST_METHOD);
			jsonResult.put("result", packages);
			endpoint.send(jsonResult.toString());
		}
	}


}

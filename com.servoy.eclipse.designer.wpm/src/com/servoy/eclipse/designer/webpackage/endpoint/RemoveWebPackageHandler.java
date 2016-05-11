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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 *
 */
public class RemoveWebPackageHandler implements IDeveloperService
{

	@Override
	public JSONObject executeMethod(JSONObject msg)
	{
		JSONObject pck = msg.getJSONObject("package");

		String packageName = pck.getString("name");
		String componentsOrServices = "components";
		if (pck.getString("packageType").endsWith("Web-Service")) componentsOrServices = "services";
		else if (pck.getString("packageType").endsWith("Web-Layout")) componentsOrServices = "layout"; // TODO
		IFolder componentsFolder = checkComponentsFolderCreated(componentsOrServices);
		IFile file = componentsFolder.getFile(packageName + ".zip");
		try
		{
			file.delete(true, new NullProgressMonitor());
		}
		catch (CoreException e)
		{
			Debug.error(e);
		}
		return null;
	}

	static IFolder checkComponentsFolderCreated(String componentsOrServices)
	{
		IProject project = getResourcesProject();

		try
		{
			project.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
		}
		catch (CoreException e1)
		{
			e1.printStackTrace();
		}
		IFolder folder = project.getFolder(componentsOrServices);
		if (!folder.exists())
		{
			try
			{
				folder.create(true, true, new NullProgressMonitor());
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
		return folder;
	}

	static IProject getResourcesProject()
	{
		ServoyProject initialActiveProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		ServoyResourcesProject resourcesProject = initialActiveProject.getResourcesProject();
		IProject project = resourcesProject.getProject();
		return project;
	}

	@Override
	public void dispose()
	{
	}

}

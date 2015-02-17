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
package com.servoy.eclipse.team;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;

import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.GroupSecurityInfo;
import com.servoy.j2db.server.shared.SecurityInfo;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;

public class TeamEclipseUserManager extends WorkspaceUserManager
{
	private final IFileAccess fileAccess;
	private final Solution solution;
	private final String resourcesProjectName;

	public TeamEclipseUserManager(IFileAccess fileAccess, String resourcesProjectname)
	{
		this.fileAccess = fileAccess;
		this.solution = null;
		this.resourcesProjectName = resourcesProjectname;
	}

	public TeamEclipseUserManager(IFileAccess fileAccess, Solution solution)
	{
		this.fileAccess = fileAccess;
		this.solution = solution;
		this.resourcesProjectName = null;
	}

	public void writeRepositorySecurityInformationToDir() throws RepositoryException
	{
		try
		{
			writeUserAndGroupInfo();
		}
		catch (JSONException e)
		{
			throw new RepositoryException(e);
		}
		catch (IOException e)
		{
			throw new RepositoryException(e);
		}
		writeAllTableInfo();
	}

	@Override
	public void writeSecurityInfo(String serverName, String tableName, boolean later) throws RepositoryException
	{
		try
		{
			String out = serializeSecurityInfo(serverName, tableName);
			if (out.length() > 0)
			{
				fileAccess.setUTF8Contents(resourcesProjectName + '/' + DataModelManager.getRelativeServerPath(serverName) + getFileName(tableName), out);
			}
		}
		catch (IOException e)
		{
			throw new RepositoryException(e);
		}
		catch (JSONException e)
		{
			throw new RepositoryException(e);
		}
	}

	public void writeSolutionSecurityInformationToDir(Solution sol) throws RepositoryException
	{
		Iterator<Form> allFormsInActiveModules = sol.getForms(null, false);
		while (allFormsInActiveModules.hasNext())
		{
			writeSecurityInfo(allFormsInActiveModules.next());
		}
	}

	private void writeUserAndGroupInfo() throws JSONException, IOException
	{
		String out = serializeUserAndGroupInfo();
		if (out.length() > 0)
		{
			fileAccess.setUTF8Contents(resourcesProjectName + '/' + SECURITY_DIR + '/' + SECURITY_FILENAME, out);
		}
	}

	private void writeSecurityInfo(Form f) throws RepositoryException
	{
		try
		{
			String out = serializeSecurityInfo(f);
			if (out.length() > 0)
			{
				fileAccess.setUTF8Contents(SolutionSerializer.getRelativePath(f, false) + getFileName(f), out);
			}
		}
		catch (JSONException e)
		{
			throw new RepositoryException(e);
		}
		catch (IOException e)
		{
			throw new RepositoryException(e);
		}
	}

	@Override
	public void setFormSecurityAccess(String clientId, String groupName, Integer accessMask, UUID elementUUID, String solutionName) throws ServoyException
	{
		if (groupName == null || groupName.length() == 0 || elementUUID == null || (!isOperational()))
		{
			ServoyLog.logError("Invalid parameters received, or manager is not operational - setFormSecurityAccess(...)", null);
			return;
		}
		GroupSecurityInfo gsi = getGroupSecurityInfo(groupName);

		if (gsi != null)
		{
			// now we must find the Form's UUID from the elementUUID
			Form form = null;

			Iterator<Form> it = solution.getForms(null, false);
			while (it.hasNext())
			{
				Form f = it.next();
				if (isElementChildOfForm(elementUUID.toString(), f))
				{
					form = f;
				}
			}

			if (form != null)
			{
				List<SecurityInfo> securityInfo = gsi.formSecurity.get(form.getUUID());
				if (securityInfo == null)
				{
					securityInfo = new ArrayList<SecurityInfo>();
					gsi.formSecurity.put(form.getUUID(), securityInfo);
				}
				setSecurityInfo(securityInfo, elementUUID.toString(), accessMask.intValue(), true);
			}
			else
			{
				ServoyLog.logWarning("setFormSecurityAccess(...) cannot find element with given UUID or not form element!", null);
			}
		}
		else
		{
			ServoyLog.logWarning("setFormSecurityAccess(...) cannot find the group with the given name!", null);
		}
	}

	@Override
	public boolean isOperational()
	{
		// Team provider works with java.io in this class - and it doesn't need a resource project & stuff.<BR>
		// So it will not rely on an existing resources project, but needs to operate with create/set/get methods...
		return true;
	}
}
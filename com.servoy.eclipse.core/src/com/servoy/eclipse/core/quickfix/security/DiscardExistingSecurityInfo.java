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
package com.servoy.eclipse.core.quickfix.security;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONException;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * Quick fix for security read errors - discards existing security information.
 *
 * @author acostescu
 */
public class DiscardExistingSecurityInfo extends SecurityQuickFix
{

	private static DiscardExistingSecurityInfo instance;

	public static DiscardExistingSecurityInfo getInstance()
	{
		if (instance == null)
		{
			instance = new DiscardExistingSecurityInfo();
		}
		return instance;
	}

	public String getLabel()
	{
		return "Discard all existing User/Security information. WARNING: previous user/security info will be lost!";
	}

	@Override
	public void run(IMarker marker)
	{
		IProject project = marker.getResource().getProject();
		ServoyProject[] activeModules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
		ServoyResourcesProject activeResourceProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
		boolean ok = (activeResourceProject.getProject() == project);
		if (!ok) for (ServoyProject p : activeModules)
		{
			if (p.getProject() == project)
			{
				ok = true;
				break;
			}
		}
		ok = ok && (activeResourceProject != null);
		if (ok)
		{
			Shell shell = UIUtils.getActiveShell();
			if (shell != null)
			{
				if (!MessageDialog.openQuestion(shell, "Confirm discard",
					"Choosing yes will overwrite (most likely delete) existing security files in active solutions\nand resources project. Do you wish to continue?"))
					return;
			} // else maybe we should not bother the user with one more dialog
			try
			{
				ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().writeAllSecurityInformation(true);
			}
			catch (RepositoryException e)
			{
				MessageDialog.openError(UIUtils.getActiveShell(), "Error writing security info", e.getMessage());
				ServoyLog.logError("Cannot write empty security data (invalid security content quick fix)", e);
			}
		}
		else
		{
			ServoyLog.logError("Quick fix for security read error failed: the resource that reports the error is not part of any active module...", null);
		}
	}

	@Override
	public boolean canHandleMarker(IMarker marker)
	{
		return true;
	}

	// methods below are not used
	@Override
	protected boolean canHandleType(int type)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected String parseAndAlterSecurityFile(String fileContent) throws JSONException
	{
		// TODO Auto-generated method stub
		return null;
	}

}
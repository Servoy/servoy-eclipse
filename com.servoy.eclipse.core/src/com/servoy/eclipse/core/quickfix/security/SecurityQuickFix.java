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

import java.nio.charset.Charset;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IMarkerResolution;
import org.json.JSONException;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.ReturnValueRunnable;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.repository.WorkspaceUserManager.User;
import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.Utils;

public abstract class SecurityQuickFix implements IMarkerResolution
{

	public static class GetNewUserNameRunnable extends ReturnValueRunnable
	{

		private final List<User> existingUsers;
		private final String initialValue;
		private final String title;

		public GetNewUserNameRunnable(List<User> existingUsers, String title, String initialValue)
		{
			this.title = title;
			this.initialValue = initialValue;
			this.existingUsers = existingUsers;
		}

		public void run()
		{
			returnValue = null;
			InputDialog dialog;

			// request user name
			dialog = new InputDialog(UIUtils.getActiveShell(), title, "Please specify new user name", initialValue, new IInputValidator()
			{
				public String isValid(String newUserName)
				{
					if (newUserName == null || newUserName.trim().length() == 0) return "Please specify user name";
					return userNameExists(newUserName) ? "Another user with this name exists" : null;
				}
			});
			int result = dialog.open();
			if (result == Window.OK)
			{
				returnValue = dialog.getValue();
			}
		}

		protected boolean userNameExists(String name)
		{
			for (User u : existingUsers)
			{
				if (name.equals(u.name)) return true;
			}
			return false;
		}

	}

	public static class GetNewGroupNameRunnable extends ReturnValueRunnable
	{

		private final List<String> existingGroups;
		private final String initialValue;
		private final String title;

		public GetNewGroupNameRunnable(List<String> existingGroups, String title, String initialValue)
		{
			this.title = title;
			this.initialValue = initialValue;
			this.existingGroups = existingGroups;
		}

		public void run()
		{
			returnValue = null;
			InputDialog dialog;

			// request user name
			dialog = new InputDialog(UIUtils.getActiveShell(), title, "Please specify new permission name", initialValue, new IInputValidator()
			{
				public String isValid(String newUserName)
				{
					if (newUserName == null || newUserName.trim().length() == 0) return "Please specify permission name";
					return groupNameExists(newUserName) ? "Another permission with this name exists" : null;
				}
			});
			int result = dialog.open();
			if (result == Window.OK)
			{
				returnValue = dialog.getValue();
			}
		}

		protected boolean groupNameExists(String name)
		{
			for (String groupName : existingGroups)
			{
				if (name.equals(groupName)) return true;
			}
			return false;
		}

	}

	public static class GetNewPasswordRunnable extends ReturnValueRunnable
	{
		private final String title;

		public GetNewPasswordRunnable(String title)
		{
			this.title = title;
		}

		public void run()
		{
			returnValue = null;
			InputDialog dialog;
			// request password
			dialog = new InputDialog(UIUtils.getActiveShell(), title, "Please specify new password", null, new IInputValidator()
			{
				public String isValid(String newPassword)
				{
					if (newPassword == null || newPassword.trim().length() == 0) return "Please specify password";
					return null;
				}
			});
			int result = dialog.open();
			if (result == Window.OK)
			{
				returnValue = Utils.calculateAndPrefixPBKDF2PasswordHash(dialog.getValue());
			}
		}

	}

	protected Object wrongValue;
	private boolean silent;

	public boolean canHandleMarker(IMarker marker)
	{
		return canHandleType(getType(marker));
	}

	protected abstract boolean canHandleType(int type);

	public void run(IMarker marker)
	{
		if (!marker.exists()) return;
		// get the file
		IFile file;
		if (marker.getResource() instanceof IFile)
		{
			file = (IFile)marker.getResource();
		}
		else
		{
			reportProblem("Security marker on a non-file resource.");
			return;
		}

		run(getType(marker), getWrongValue(marker), file);
	}

	public void run(int type, Object wrongValue, IFile file)
	{
		this.wrongValue = wrongValue;
		// safety check
		ServoyResourcesProject resourcesProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
		if (resourcesProject == null && ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() == null)
		{
			reportProblem("No active solution or resources project found.");
			return;
		}

		// see that it is part of an active IProject
		boolean ok = false;
		if (resourcesProject != null && resourcesProject.getProject() == file.getProject()) ok = true;
		else
		{
			ServoyProject[] modulesOfActiveProject = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
			for (ServoyProject sp : modulesOfActiveProject)
			{
				if (sp.getProject() == file.getProject())
				{
					ok = true;
					break;
				}
			}
		}
		if (!ok)
		{
			reportProblem("Security marker on a file that is not part of any active solution/resources project.");
			return;
		}

		// parse and alter content in the file to fix the problem
		String correctedResult = null;
		try
		{
			String fileContent;
			if (file.exists())
			{
				fileContent = Utils.getTXTFileContent(file.getContents(true), Charset.forName("UTF8"));
			}
			else
			{
				fileContent = ""; // if file doesn't exist it's ok, it may be created
			}
			if (fileContent != null)
			{
				correctedResult = parseAndAlterSecurityFile(fileContent);
			}
			else
			{
				reportProblem("Cannot read security file contents.");
			}
		}
		catch (JSONException e)
		{
			ServoyLog.logError("JSON exception while trying to quick fix logic in file contents", e);
			reportProblem("File contents are not JSON compliant: " + e.getMessage());
		}
		catch (CoreException e)
		{
			ServoyLog.logError("Exception while trying to quick fix logic in file contents", e);
			reportProblem("Cannot read file contents:" + e.getMessage());
		}

		// write back results if changes were made
		if (correctedResult != null)
		{
			// if we are here the file already exists - no need to check
			try
			{
				if (correctedResult.length() == 0)
				{
					file.delete(true, null);
				}
				else
				{
					ResourcesUtils.createOrWriteFileUTF8(file, correctedResult, true);
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError("Cannot write sec file after quick fix", e);
				reportProblem("Cannot write sec file after quick fix: " + e.getMessage());
			}
		}
	}

	protected abstract String parseAndAlterSecurityFile(String fileContent) throws JSONException;

	private int getType(IMarker marker)
	{
		return marker.getAttribute(WorkspaceUserManager.MARKER_ATTRIBUTE_TYPE, WorkspaceUserManager.SecurityReadException.UNKNOWN);
	}

	private Object getWrongValue(IMarker marker)
	{
		int l = marker.getAttribute(WorkspaceUserManager.MARKER_ATTRIBUTE_WRONG_VALUE_ARRAY_LENGTH, -1);
		if (l != -1)
		{
			String[] wv = new String[l];
			for (int i = 0; i < l; i++)
			{
				wv[i] = marker.getAttribute(WorkspaceUserManager.MARKER_ATTRIBUTE_WRONG_VALUE_ARRAY + i, null);
			}
			return wv;
		}
		else
		{
			return marker.getAttribute(WorkspaceUserManager.MARKER_ATTRIBUTE_WRONG_VALUE, null);
		}
	}

	public void setSilent(boolean silent)
	{
		this.silent = silent;
	}

	public void reportProblem(final String message)
	{
		if (silent)
		{
			ServoyLog.logWarning("Quick fix encountered a problem: " + message, null);
		}
		else
		{
			UIUtils.reportWarning("Quick fix encountered a problem", message);
		}
	}

}
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.servoy.eclipse.core.util.ReturnValueRunnable;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.repository.WorkspaceUserManager.User;

/**
 * Quick fix for removing one of the users with duplicate UIDs.
 * 
 * @author acostescu
 */
public class RemoveSomeUsersWithDuplicateUUID extends AlterUserGroupSecFileQuickFix
{

	private static RemoveSomeUsersWithDuplicateUUID instance;

	public static RemoveSomeUsersWithDuplicateUUID getInstance()
	{
		if (instance == null)
		{
			instance = new RemoveSomeUsersWithDuplicateUUID();
		}
		return instance;
	}

	@Override
	protected boolean canHandleType(int type)
	{
		return type == WorkspaceUserManager.SecurityReadException.DUPLICATE_USER_UID;
	}

	public String getLabel()
	{
		return "Remove some of the users that have same UID.";
	}

	@Override
	protected boolean alterUserAndGroupInfo(List<String> groups, final List<User> users, final Map<String, List<String>> usersForGroups)
	{
		boolean altered = false;
		String duplicateUID = (String)wrongValue;
		final ArrayList<User> duplicates = new ArrayList<User>();
		for (User user : users)
		{
			if (duplicateUID.equals(user.userUid))
			{
				duplicates.add(user);
			}
		}

		if (duplicates.size() > 1)
		{
			ReturnValueRunnable runnable = new ReturnValueRunnable()
			{
				public void run()
				{
					returnValue = Boolean.FALSE;
					ElementListSelectionDialog dialog = new ElementListSelectionDialog(UIUtils.getActiveShell(), new LabelProvider());
					dialog.setMultipleSelection(true);
					dialog.setEmptySelectionMessage("no user selected");
					dialog.setTitle("Choose users");
					dialog.setMessage("Please select which user(s) should be removed.");
					Object[] dup = duplicates.toArray();
					dialog.setElements(dup);
					dialog.setInitialSelections(dup);

					int choice = dialog.open();
					if (choice == Window.OK)
					{
						Object[] result = dialog.getResult();
						for (Object user : result)
						{
							users.remove(user);
						}
						if (result.length == duplicates.size())
						{
							// all users with duplicate UID will be deleted - so the old UID would be
							// obsolete in user to group mappings
							for (List<String> uidsForAGroup : usersForGroups.values())
							{
								uidsForAGroup.remove(wrongValue);
							}
						}
						returnValue = (result.length > 0) ? Boolean.TRUE : Boolean.FALSE;
					}
				}
			};
			UIUtils.runInUI(runnable, true);
			altered = ((Boolean)runnable.getReturnValue()).booleanValue();
		}
		else
		{
			reportProblem("Cannot find more than 1 user with this UID...");
		}

		return altered;
	}
}

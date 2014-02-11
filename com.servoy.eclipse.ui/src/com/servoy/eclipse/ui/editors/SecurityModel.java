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
package com.servoy.eclipse.ui.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.EclipseUserManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

public class SecurityModel
{
	private final ArrayList<String> newGroups;
	private final ArrayList<String> deletedGroups;
	private final ArrayList<String> newUsers;
	private final ArrayList<String> deletedUsers;
	private final Map<String, String> editedUserNames;
	private final Map<String, String> editedPasswords;
	private final Map<String, String> editedUids;
	private final Map<String, ArrayList<String>> addedGroupsToUser;
	private final Map<String, ArrayList<String>> deletedGroupsFromUser;

	public SecurityModel()
	{
		newGroups = new ArrayList<String>();
		deletedGroups = new ArrayList<String>();
		newUsers = new ArrayList<String>();
		deletedUsers = new ArrayList<String>();
		editedUserNames = new HashMap<String, String>();
		editedPasswords = new HashMap<String, String>();
		editedUids = new HashMap<String, String>();
		addedGroupsToUser = new HashMap<String, ArrayList<String>>();
		deletedGroupsFromUser = new HashMap<String, ArrayList<String>>();
	}

	public void addGroup(String name)
	{
		newGroups.add(name);
	}

	public void removeGroups(List<String> names)
	{
		if (names != null)
		{
			for (String name : names)
			{
				if (newGroups.contains(name)) newGroups.remove(name);
				else deletedGroups.add(name);
			}
		}
	}

	public void addUser(String name)
	{
		newUsers.add(name);
	}

	public void removeUser(String name)
	{
		if (newUsers.contains(name)) newUsers.remove(name);
		else deletedUsers.add(name);
	}

	private String getOriginalUserName(String userName)
	{
		if (editedUserNames.containsValue(userName))
		{
			Iterator<String> iterator = editedUserNames.keySet().iterator();
			while (iterator.hasNext())
			{
				String key = iterator.next();
				if (editedUserNames.get(key).equals(userName)) return key;
			}
		}
		return userName;
	}

	public void editElement(String userName, String newElement, int column)
	{
		String originalName = getOriginalUserName(userName);
		if (column == SecurityEditor.CI_NAME) editedUserNames.put(originalName, newElement);
		else if (column == SecurityEditor.CI_PASSWORD) editedPasswords.put(originalName, newElement);
		else editedUids.put(originalName, newElement);
	}

	public void modifyUserGroup(String user, String group, boolean add)
	{
		String originalName = getOriginalUserName(user);
		if (add)
		{
			ArrayList<String> groups = addedGroupsToUser.get(originalName);
			if (groups == null) groups = new ArrayList<String>();
			if (!groups.contains(group)) groups.add(group);
			addedGroupsToUser.put(originalName, groups);
			groups = deletedGroupsFromUser.get(originalName);
			if (groups != null) groups.remove(group);
			deletedGroupsFromUser.put(originalName, groups);
		}
		else
		{
			ArrayList<String> groups = deletedGroupsFromUser.get(originalName);
			if (groups == null) groups = new ArrayList<String>();
			if (!groups.contains(group)) groups.add(group);
			deletedGroupsFromUser.put(originalName, groups);
			groups = addedGroupsToUser.get(originalName);
			if (groups != null) groups.remove(group);
			addedGroupsToUser.put(originalName, groups);
		}
	}

	public void createTreeData(Tree tree, String group)
	{
		TreeItem[] selection = tree.getSelection();
		String selectedUser = null;
		if (selection != null && selection.length == 1)
		{
			selectedUser = selection[0].getText(0);
		}
		TreeItem keptSelection = null;
		tree.removeAll();
		IDataSet users = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getUsers(ApplicationServerRegistry.get().getClientId());
		int usersNr = users.getRowCount();
		for (int i = 0; i < usersNr; i++)
		{
			String userName = users.getRow(i)[1].toString();
			String uid = users.getRow(i)[0].toString();
			if (!deletedUsers.contains(userName))
			{
				TreeItem item = new TreeItem(tree, SWT.NONE);
				String modifiedUserName = userName;
				String modifiedUid = uid;
				if (editedUserNames.containsKey(userName)) modifiedUserName = editedUserNames.get(userName);
				if (editedUids.containsKey(userName)) modifiedUid = editedUids.get(userName);
				if (selectedUser != null && selectedUser.equals(modifiedUserName)) keptSelection = item;
				item.setText(new String[] { modifiedUserName, "password", modifiedUid });
				boolean found = false;
				if (group != null)
				{
					ArrayList<String> groups = addedGroupsToUser.get(userName);
					if (groups != null && groups.contains(group))
					{
						item.setChecked(true);
						found = true;
					}
					else
					{
						groups = deletedGroupsFromUser.get(userName);
						if (groups != null && groups.contains(group))
						{
							item.setChecked(false);
							found = true;
						}
					}
				}
				if (!found)
				{
					if (group != null)
					{
						String[] groups = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getUserGroups(
							ApplicationServerRegistry.get().getClientId(),
							ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getUserUID(
								ApplicationServerRegistry.get().getClientId(), userName));
						if (groups != null)
						{
							for (String currentGroup : groups)
							{
								if (currentGroup.equals(group))
								{
									item.setChecked(true);
									found = true;
									break;
								}
							}
						}
					}
				}
				if (!found)
				{
					item.setChecked(false);
				}
			}
		}
		for (String user : newUsers)
		{
			TreeItem item = new TreeItem(tree, SWT.NONE);
			String modifiedUserName = user;
			if (editedUserNames.containsKey(user)) modifiedUserName = editedUserNames.get(user);
			if (selectedUser != null && selectedUser.equals(modifiedUserName)) keptSelection = item;
			item.setText(new String[] { modifiedUserName, "password", "" });
			boolean found = false;
			if (group != null)
			{
				ArrayList<String> groups = addedGroupsToUser.get(user);
				if (groups != null && groups.contains(group))
				{
					item.setChecked(true);
					found = true;
				}
				else
				{
					groups = deletedGroupsFromUser.get(user);
					if (groups != null && groups.contains(group))
					{
						item.setChecked(false);
						found = true;
					}
				}
			}
			if (!found) item.setChecked(false);
		}
		if (keptSelection != null) tree.setSelection(keptSelection);
	}

	public boolean isColumnValid(String text, int column)
	{
		if (column == SecurityEditor.CI_PASSWORD && text != null && text.length() > 0) return true;
		if (column == SecurityEditor.CI_UID && text != null && text.length() > 0)
		{
			if (ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getUserIdByUID(ApplicationServerRegistry.get().getClientId(),
				text) == -1)
			{
				// uid is unique
				return true;
			}
		}
		if (column == SecurityEditor.CI_NAME && text != null && text.length() > 0)
		{
			int id = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getUserIdByUserName(
				ApplicationServerRegistry.get().getClientId(), text);
			if (id == -1 && !newUsers.contains(text))
			{
				// username is unique
				return true;
			}
		}
		if (column == SecurityEditor.CI_GROUP && text != null && text.length() > 0)
		{
			int id = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getGroupId(ApplicationServerRegistry.get().getClientId(),
				text);
			if (id == -1 && !newGroups.contains(text))
			{
				// group is unique
				return true;
			}
		}
		return false;
	}

	public void saveSecurity()
	{
		try
		{
			EclipseUserManager manager = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager();
			String clientId = ApplicationServerRegistry.get().getClientId();
			for (String group : newGroups)
			{
				manager.createGroup(clientId, group);
			}
			for (String user : newUsers)
			{
				manager.createUser(clientId, user, "password", null, false);
			}
			Iterator<String> iterator = editedPasswords.keySet().iterator();
			while (iterator.hasNext())
			{
				String userName = iterator.next();
				manager.setPassword(clientId, manager.getUserUID(clientId, userName), editedPasswords.get(userName));
			}
			iterator = editedUids.keySet().iterator();
			while (iterator.hasNext())
			{
				String userName = iterator.next();
				String[] groups = manager.getUserGroups(clientId, manager.getUserUID(clientId, userName));
				if (groups != null && groups.length > 0)
				{
					for (String group : groups)
					{
						manager.removeUserFromGroup(clientId, manager.getUserIdByUserName(clientId, userName), manager.getGroupId(clientId, group));
					}
				}
				manager.setUserUID(clientId, manager.getUserUID(clientId, userName), editedUids.get(userName));
				if (groups != null && groups.length > 0)
				{
					for (String group : groups)
					{
						manager.addUserToGroup(clientId, manager.getUserIdByUserName(clientId, userName), manager.getGroupId(clientId, group));
					}
				}
			}
			iterator = addedGroupsToUser.keySet().iterator();
			while (iterator.hasNext())
			{
				String userName = iterator.next();
				ArrayList<String> groups = addedGroupsToUser.get(userName);
				if (groups != null)
				{
					for (String group : groups)
					{
						manager.addUserToGroup(clientId, manager.getUserIdByUserName(clientId, userName), manager.getGroupId(clientId, group));
					}
				}
			}
			iterator = deletedGroupsFromUser.keySet().iterator();
			while (iterator.hasNext())
			{
				String userName = iterator.next();
				ArrayList<String> groups = deletedGroupsFromUser.get(userName);
				if (groups != null)
				{
					for (String group : groups)
					{
						manager.removeUserFromGroup(clientId, manager.getUserIdByUserName(clientId, userName), manager.getGroupId(clientId, group));
					}
				}
			}
			iterator = editedUserNames.keySet().iterator();
			while (iterator.hasNext())
			{
				String userName = iterator.next();
				manager.changeUserName(clientId, manager.getUserUID(clientId, userName), editedUserNames.get(userName));
			}
			for (String user : deletedUsers)
			{
				manager.deleteUser(clientId, manager.getUserIdByUserName(clientId, user));
			}

			manager.deleteGroups(clientId, deletedGroups);

			newGroups.clear();
			newUsers.clear();
			deletedGroups.clear();
			deletedUsers.clear();
			editedUserNames.clear();
			editedPasswords.clear();
			addedGroupsToUser.clear();
			deletedGroupsFromUser.clear();

		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}
}

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

package com.servoy.eclipse.core.repository;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.server.shared.IUserManagerInternal;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;

/**
 * Because unit tests isolate the workspace users from the tests, but depend on the app. server
 * user manager for many operations (which would normally reflect workspace users and not the possibly
 * altered test run users), we need to be able to temporarily switch the app. server user manager behavior to that of the test client user manager.
 *
 * This class can be used as a user manager that is able to switch between a "main" EclipseUserManager and some other (IUserManagerInternal && IUserManager).
 *
 * @author acostescu
 */
public class SwitchableEclipseUserManager implements IUserManagerInternal, IUserManager
{

	IUserManager switchedTo = null;
	private final EclipseUserManager eclipseUserManger;

	public SwitchableEclipseUserManager()
	{
		eclipseUserManger = new EclipseUserManager();
	}

	/**
	 * Returns the default internal EclipseUserManager to which this user manager normally delegates.
	 */
	public EclipseUserManager getEclipseUserManager()
	{
		return eclipseUserManger;
	}

	/**
	 * Makes this user manager delegate to the given internalUserManager if not null, or back to it's eclipseUserManager if null.
	 * @param internalUserManager it must implement IUserManagerInternal as well.
	 */
	public void switchTo(IUserManager internalUserManager)
	{
		if (internalUserManager != null && !(internalUserManager instanceof IUserManagerInternal))
		{
			throw new RuntimeException("this user manager needs to implement IUserManagerInternal as well: " + internalUserManager.getClass());
		}
		switchedTo = internalUserManager;
	}

	public boolean checkIfUserIsAdministrator(String clientId, String userUid)
	{
		return switchedTo == null ? eclipseUserManger.checkIfUserIsAdministrator(clientId, userUid)
			: ((IUserManagerInternal)switchedTo).checkIfUserIsAdministrator(clientId, userUid);
	}

	public boolean checkIfAdministratorsAreAvailable(String clientId)
	{
		return switchedTo == null ? eclipseUserManger.checkIfAdministratorsAreAvailable(clientId)
			: ((IUserManagerInternal)switchedTo).checkIfAdministratorsAreAvailable(clientId);
	}

	public String checkPasswordForUserName(String clientId, String username, String password) throws RemoteException, ServoyException
	{
		return switchedTo == null ? eclipseUserManger.checkPasswordForUserName(clientId, username, password)
			: switchedTo.checkPasswordForUserName(clientId, username, password);
	}

	public boolean checkPasswordForUserUID(String clientId, String userUID, String password) throws RemoteException, ServoyException
	{
		return switchedTo == null ? eclipseUserManger.checkPasswordForUserUID(clientId, userUID, password)
			: switchedTo.checkPasswordForUserUID(clientId, userUID, password);
	}

	public String[] getUserGroups(String clientId, String userUID) throws RemoteException, ServoyException
	{
		return switchedTo == null ? eclipseUserManger.getUserGroups(clientId, userUID) : switchedTo.getUserGroups(clientId, userUID);
	}

	@Deprecated
	public Pair<Map<Object, Integer>, Set<Object>> getSecurityAccess(String clientId, UUID[] solution_uuids, int[] releaseNumber, String[] groups)
		throws RemoteException, ServoyException
	{
		return switchedTo == null ? eclipseUserManger.getSecurityAccess(clientId, solution_uuids, releaseNumber, groups)
			: switchedTo.getSecurityAccess(clientId, solution_uuids, releaseNumber, groups);
	}

	@Override
	public TableAndFormSecurityAccessInfo getSecurityAccessForTablesAndForms(String clientId, UUID[] solution_uuids, int[] releaseNumbers, String[] groups)
		throws ServoyException
	{
		return switchedTo == null ? eclipseUserManger.getSecurityAccessForTablesAndForms(clientId, solution_uuids, releaseNumbers, groups)
			: switchedTo.getSecurityAccessForTablesAndForms(clientId, solution_uuids, releaseNumbers, groups);
	}

	public String getUserUID(String clientId, String username) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.getUserUID(clientId, username) : switchedTo.getUserUID(clientId, username);
	}

	public int getGroupId(String clientId, String adminGroup) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.getGroupId(clientId, adminGroup) : switchedTo.getGroupId(clientId, adminGroup);
	}

	public String getGroupNameById(String clientId, int groupId) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.getGroupNameById(clientId, groupId) : switchedTo.getGroupNameById(clientId, groupId);
	}

	public IDataSet getUserGroups(String clientId, int userId) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.getUserGroups(clientId, userId) : switchedTo.getUserGroups(clientId, userId);
	}

	public IDataSet getGroups(String clientId) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.getGroups(clientId) : switchedTo.getGroups(clientId);
	}

	public String getUserUID(String clientId, int userId) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.getUserUID(clientId, userId) : switchedTo.getUserUID(clientId, userId);
	}

	public String getUserName(String clientId, int userId) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.getUserName(clientId, userId) : switchedTo.getUserName(clientId, userId);
	}

	public String getUserName(String clientId, String userUID) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.getUserName(clientId, userUID) : switchedTo.getUserName(clientId, userUID);
	}

	public String getUserPasswordHash(String clientId, String userUID) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.getUserPasswordHash(clientId, userUID) : switchedTo.getUserPasswordHash(clientId, userUID);
	}

	public int getUserIdByUserName(String clientId, String userName) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.getUserIdByUserName(clientId, userName) : switchedTo.getUserIdByUserName(clientId, userName);
	}

	public int getUserIdByUID(String clientId, String userUID) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.getUserIdByUID(clientId, userUID) : switchedTo.getUserIdByUID(clientId, userUID);
	}

	public IDataSet getUsers(String clientId) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.getUsers(clientId) : switchedTo.getUsers(clientId);
	}

	public IDataSet getUsersByGroup(String clientId, String group_name) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.getUsersByGroup(clientId, group_name) : switchedTo.getUsersByGroup(clientId, group_name);
	}

	public boolean addUserToGroup(String clientId, int userId, int groupId) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.addUserToGroup(clientId, userId, groupId) : switchedTo.addUserToGroup(clientId, userId, groupId);
	}

	public int createGroup(String clientId, String groupName) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.createGroup(clientId, groupName) : switchedTo.createGroup(clientId, groupName);
	}

	public int createUser(String clientId, String userName, String password, String userUID, boolean alreadyHashed) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.createUser(clientId, userName, password, userUID, alreadyHashed)
			: switchedTo.createUser(clientId, userName, password, userUID, alreadyHashed);
	}

	public boolean setPassword(String clientId, String userUID, String password, boolean hashPassword) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.setPassword(clientId, userUID, password, hashPassword)
			: switchedTo.setPassword(clientId, userUID, password, hashPassword);
	}

	public boolean setUserUID(String clientId, String oldUserUID, String newUserUID) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.setUserUID(clientId, oldUserUID, newUserUID) : switchedTo.setUserUID(clientId, oldUserUID, newUserUID);
	}

	public boolean deleteUser(String clientId, String userUID) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.deleteUser(clientId, userUID) : switchedTo.deleteUser(clientId, userUID);
	}

	public boolean deleteUser(String clientId, int userId) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.deleteUser(clientId, userId) : switchedTo.deleteUser(clientId, userId);
	}

	public boolean deleteGroup(String clientId, int groupId) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.deleteGroup(clientId, groupId) : switchedTo.deleteGroup(clientId, groupId);
	}

	public boolean removeUserFromGroup(String clientId, int userId, int groupId) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.removeUserFromGroup(clientId, userId, groupId)
			: switchedTo.removeUserFromGroup(clientId, userId, groupId);
	}

	public boolean changeGroupName(String clientId, int groupId, String groupName) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.changeGroupName(clientId, groupId, groupName) : switchedTo.changeGroupName(clientId, groupId, groupName);
	}

	public boolean changeUserName(String clientId, String userUID, String newUserName) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.changeUserName(clientId, userUID, newUserName)
			: switchedTo.changeUserName(clientId, userUID, newUserName);
	}

	@Override
	public void setFormSecurityAccess(String clientId, String groupName, Integer accessMask, String elementUID, String solutionName)
		throws ServoyException, RemoteException
	{
		if (switchedTo == null) eclipseUserManger.setFormSecurityAccess(clientId, groupName, accessMask, elementUID, solutionName);
		else switchedTo.setFormSecurityAccess(clientId, groupName, accessMask, elementUID, solutionName);
	}

	@Override
	public void setFormSecurityAccess(String clientId, String groupName, Integer accessMask, UUID formUUID, String elementUID, String solutionName)
		throws ServoyException, RemoteException
	{
		if (switchedTo == null) eclipseUserManger.setFormSecurityAccess(clientId, groupName, accessMask, formUUID, elementUID, solutionName);
		else switchedTo.setFormSecurityAccess(clientId, groupName, accessMask, formUUID, elementUID, solutionName);
	}

	@Deprecated
	public void setFormSecurityAccess(String clientId, String groupName, Integer accessMask, UUID elementUUID, String solutionName)
		throws ServoyException, RemoteException
	{
		if (switchedTo == null) eclipseUserManger.setFormSecurityAccess(clientId, groupName, accessMask, elementUUID, solutionName);
		else switchedTo.setFormSecurityAccess(clientId, groupName, accessMask, elementUUID, solutionName);
	}

	@Deprecated
	public void setFormSecurityAccess(String clientId, String groupName, Integer accessMask, UUID formUUID, UUID elementUUID, String solutionName)
		throws ServoyException, RemoteException
	{
		if (switchedTo == null) eclipseUserManger.setFormSecurityAccess(clientId, groupName, accessMask, formUUID, elementUUID, solutionName);
		else switchedTo.setFormSecurityAccess(clientId, groupName, accessMask, formUUID, elementUUID, solutionName);
	}

	public void setTableSecurityAccess(String clientId, String groupName, Integer accessMask, String connectionName, String tableName, String columnName)
		throws ServoyException, RemoteException
	{
		if (switchedTo == null) eclipseUserManger.setTableSecurityAccess(clientId, groupName, accessMask, connectionName, tableName, columnName);
		else switchedTo.setTableSecurityAccess(clientId, groupName, accessMask, connectionName, tableName, columnName);
	}

	public void dispose() throws RemoteException
	{
		if (switchedTo != null) switchedTo.dispose();
		eclipseUserManger.dispose();
	}

	@Override
	public long getPasswordLastSet(String clientId, String userUID) throws ServoyException, RemoteException
	{
		return switchedTo == null ? eclipseUserManger.getPasswordLastSet(clientId, userUID) : switchedTo.getPasswordLastSet(clientId, userUID);
	}

}
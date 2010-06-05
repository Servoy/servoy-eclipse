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
package com.servoy.eclipse.jsunit.runner;

import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.xmlxport.IXMLImportUserChannel;

/**
 * The <code>XMLDefaultImportUserChannel</code> interface is used to send info messages and to delegate any decisions needed during the import to the user.
 * This implementation gives default answers to all questions, and sends messages to trace output irrespective of their priority. This is only useful for
 * debugging, and should not be used in the final application.
 * 
 * $Id$
 */
public class TestXMLImportUserChannel implements IXMLImportUserChannel
{

	private static TestXMLImportUserChannel instance;

	public static TestXMLImportUserChannel instance()
	{
		if (instance == null)
		{
			instance = new TestXMLImportUserChannel();
		}
		return instance;
	}

	public String askServerForRepositoryUserData()
	{
		return IServer.REPOSITORY_SERVER;
	}

	public int askUnknownServerAction(String name)
	{
		return CANCEL_ACTION;
	}

	public int askStyleAlreadyExistsAction(String name)
	{
		return CANCEL_ACTION;
	}

	public int askMediaNameCollisionAction(String name)
	{
		return CANCEL_ACTION;
	}

	public int askMediaChangedAction(String name)
	{
		return CANCEL_ACTION;
	}

	public int askGroupAlreadyExistsAction(String name)
	{
		return CANCEL_ACTION;
	}

	public int askRenameRootObjectAction(String name, int objectTypeId)
	{
		return CANCEL_ACTION;
	}

	public int askImportRootObjectAsLocalName(String name, String localName, int objectTypeId)
	{
		return OK_ACTION;
	}

	public int askForImportPolicy(String name, int typeId, boolean hasRevisions)
	{
		return OK_ACTION;
	}

	public boolean getMergeEnabled()
	{
		return false;
	}

	public boolean getUseLocalOnMergeConflict()
	{
		return false;
	}

	public boolean getOverwriteEnabled()
	{
		return true;
	}

	public boolean getDeleteEnabled()
	{
		return false;
	}

	public String getNewName()
	{
		return null;
	}

	public boolean getMustAuthenticate(boolean mustAuthenticate)
	{
		return mustAuthenticate;
	}

	public void notifyWorkDone(float amount)
	{
		// Do nothing.
	}

	public void info(String message, int priority)
	{
		if (priority < ERROR)
		{
			Debug.trace("[" + priority + "] " + message);
		}
		else
		{
			Debug.error("[" + priority + "] " + message);
		}
	}

	public int askAllowSQLKeywords()
	{
		return CANCEL_ACTION;
	}

	public int askImportSampleData()
	{
		return CANCEL_ACTION;
	}

	public int askImportI18NPolicy()
	{
		return CANCEL_ACTION;
	}

	public boolean getInsertNewI18NKeysOnly()
	{
		return false;
	}

	public int askOverrideSequenceTypes(String serverName)
	{
		return OK_ACTION; // necessary for regression.SystemTest.test_defaultTableValues()
	}

	public int askOverrideDefaultValues(String serverName)
	{
		return OK_ACTION; // necessary for regression.SystemTest.test_defaultTableValues()
	}

	public int askUserImportPolicy()
	{
		return 0; // SKIP
	}

	public boolean getAddUsersToAdministratorGroup()
	{
		return false;
	}

	public int getAllowDataModelChange()
	{
		return OK_ACTION;
	}

	public boolean getDisplayDataModelChange()
	{
		return false;
	}

	public String askProtectionPassword(String solutionName)
	{
		return null;
	}

}

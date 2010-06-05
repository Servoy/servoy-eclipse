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
package com.servoy.eclipse.debug.script;

import java.util.HashMap;

import org.eclipse.dltk.javascript.typeinference.IScriptableTypeProvider;
import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.scripting.JSEvent;

public class CustomTypeProvider implements IScriptableTypeProvider
{

	private final HashMap<String, Class< ? >> types = new HashMap<String, Class< ? >>();

	/**
	 * @see org.eclipse.dltk.javascript.typeinference.IScriptableTypeProvider#createTypeReference(java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("nls")
	public Scriptable getType(String paramOrVarName, String type)
	{
		String typeLowerCase = type.toLowerCase();
		if (typeLowerCase.equals("jsevent"))
		{
			return new ScriptObjectClassScope(null, JSEvent.class, paramOrVarName);
		}
		if (typeLowerCase.equals("jsdatabasemanager"))
		{
			return new ScriptObjectClassScope(null, JSDatabaseManager.class, paramOrVarName);
		}
		if (typeLowerCase.equals("jsdataset"))
		{
			return new ScriptObjectClassScope(null, JSDataSet.class, paramOrVarName);
		}
		if (typeLowerCase.equals("jsrecord") || typeLowerCase.startsWith("jsrecord:"))
		{
			return new RecordScope(getFoundsetScope(type), null, null);
		}
		if (typeLowerCase.equals("foundset") || typeLowerCase.equals("jsfoundset") || typeLowerCase.startsWith("jsfoundset:"))
		{
			return getFoundsetScope(type);
		}
		if (typeLowerCase.equals("form") || typeLowerCase.startsWith("form:"))
		{
			Form form = null;
			int index = type.indexOf(':');
			if (index != -1)
			{
				String formName = type.substring(index + 1);
				form = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getForm(formName);
			}
			return new FormScope(null, form);
		}
		Class cls = types.get(typeLowerCase);
		if (cls != null)
		{
			return new ScriptObjectClassScope(null, cls, paramOrVarName);
		}
		return null;
	}

	/**
	 * @param type
	 */
	private FoundSetScope getFoundsetScope(String type)
	{
		int index = type.indexOf(':');
		if (index != -1)
		{
			int index2 = type.indexOf('.', index);
			if (index2 != -1)
			{
				String serverName = type.substring(index + 1, index2);
				String tableName = type.substring(index2 + 1);
				IServer server = ServoyModel.getServerManager().getServer(serverName);
				if (server != null)
				{
					try
					{
						ITable table = server.getTable(tableName);
						if (table != null)
						{
							return new FoundSetScope(null, FoundSet.class, table);
						}
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
		return new FoundSetScope(null, FoundSet.class, (ITable)null);
	}

	/**
	 * @param prefix
	 * @param element
	 */
	public void addType(String name, Class< ? > cls)
	{
		types.put(name.toLowerCase(), cls);
	}
}

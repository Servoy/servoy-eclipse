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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.ITypeProvider;
import org.eclipse.dltk.javascript.typeinfo.TypeMode;
import org.eclipse.dltk.javascript.typeinfo.model.Type;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

@SuppressWarnings("nls")
public class TypeProvider implements ITypeProvider
{
	private final TypeCreator TYPES = new TypeCreator();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.ITypeProvider#initialize(org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext)
	 */
	public boolean initialize(ITypeInfoContext context)
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.debug.script.TypeCreator#getType(org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext, java.lang.String)
	 */
	public Type getType(ITypeInfoContext context, TypeMode mode, String typeName)
	{
		String contextString = null;
		FlattenedSolution flattenedSolution = ElementResolver.getFlattenedSolution(context);
		if (flattenedSolution != null)
		{
			contextString = flattenedSolution.getSolution().getName();
		}
		return TYPES.findType(contextString, typeName);
	}

	public Set<String> listTypes(ITypeInfoContext context, TypeMode mode, String prefix)
	{
		Set<String> names = TYPES.getTypeNames(prefix);
		if (prefix != null)
		{
			String prefixLower = prefix.toLowerCase();
			String datasourcePrefix = null;
			if (Record.JS_RECORD.toLowerCase().startsWith(prefixLower))
			{
				names.add(Record.JS_RECORD);
				if (prefixLower.equalsIgnoreCase(Record.JS_RECORD)) datasourcePrefix = Record.JS_RECORD + '<';
			}
			if (FoundSet.JS_FOUNDSET.toLowerCase().startsWith(prefixLower))
			{
				names.add(FoundSet.JS_FOUNDSET);
				if (prefixLower.equalsIgnoreCase(FoundSet.JS_FOUNDSET)) datasourcePrefix = FoundSet.JS_FOUNDSET + '<';
			}
			if (datasourcePrefix != null && mode == TypeMode.JSDOC)
			{
				IServerManagerInternal serverManager = ServoyModel.getServerManager();
				String[] serverNames = serverManager.getServerNames(true, true, false, false);
				for (String serverName : serverNames)
				{
					try
					{
						List<String> tableAndViewNames = serverManager.getServer(serverName).getTableAndViewNames(true);
						for (String tableName : tableAndViewNames)
						{
							names.add(datasourcePrefix + "db:/" + serverName + '/' + tableName + '>');
						}
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}
			}
			if ("form".startsWith(prefixLower) || "runtimeform".startsWith(prefixLower))
			{
				names.add("RuntimeForm");
				if (mode == TypeMode.JSDOC)
				{
					FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
					Iterator<Form> forms = fs.getForms(false);
					for (Form form : Utils.iterate(forms))
					{
						names.add("RuntimeForm<" + form.getName() + '>');
					}
				}
			}
			if ("continuation".startsWith(prefixLower)) names.add("Continuation");
		}
		else
		{
			names.add(Record.JS_RECORD);
			names.add(FoundSet.JS_FOUNDSET);
//			names.add("Form");
			names.add("RuntimeForm");
			names.add("Continuation");
		}
		return names;
	}
}

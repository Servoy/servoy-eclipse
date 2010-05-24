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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.scripting.DefaultScope;

/**
 * @author jcompagner
 * 
 */
public class TableScope extends DefaultScope
{
	private final ITable table;

	/**
	 * @param parent
	 */
	public TableScope(Scriptable parent, ITable table)
	{
		super(parent);
		this.table = table;
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getClassName()
	 */
	@Override
	public String getClassName()
	{
		return "TableScope"; //$NON-NLS-1$
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getIds()
	 */
	@Override
	public Object[] getIds()
	{
		ArrayList<Object> al = new ArrayList<Object>();
		// data providers
		try
		{
			FlattenedSolution fs = FormDomProvider.CURRENT_PROJECT.get().getEditingFlattenedSolution();
			Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable((Table)table);
			if (allDataProvidersForTable != null) al.addAll(allDataProvidersForTable.keySet());

			Iterator<Relation> relations = fs.getRelations(table, true, true);
			while (relations.hasNext())
			{
				al.add(relations.next().getName());
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}


		return al.toArray();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		FlattenedSolution fs = FormDomProvider.CURRENT_PROJECT.get().getEditingFlattenedSolution();
		Object o = RecordScope.testForDataProvider(this, fs, (Table)table, name);
		if (o != null) return o;

		// relations
		Relation relation = fs.getRelation(name);
		if (relation != null && relation.isValid())
		{
			return new FoundSetScope(this, RelatedFoundSet.class, relation);
		}
		return super.get(name, start);
	}
}

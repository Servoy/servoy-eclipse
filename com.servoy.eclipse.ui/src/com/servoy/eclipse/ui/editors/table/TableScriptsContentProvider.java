/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
package com.servoy.eclipse.ui.editors.table;

import java.util.Iterator;
import java.util.List;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SortedTypeIterator;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.SortedList;

/**
 * Content provider for methods/calculations in table editor.
 * 
 * @author lvostinar, rgansevles
 *
 */
public class TableScriptsContentProvider extends FlatTreeContentProvider
{
	private final Table table;
	private final int typeId;

	public TableScriptsContentProvider(Table t, int typeId)
	{
		this.table = t;
		this.typeId = typeId;
	}

	@Override
	public Object[] getChildren(Object parentElement)
	{
		if (parentElement instanceof Solution)
		{
			Solution solution = (Solution)parentElement;
			try
			{
				List<IScriptProvider> scripts = new SortedList<IScriptProvider>(NameComparator.INSTANCE);
				Iterator<TableNode> tableNodes = solution.getTableNodes(table);
				while (tableNodes.hasNext())
				{
					Iterator<IPersist> scriptsIterator = SortedTypeIterator.createFilteredIterator(tableNodes.next().getAllObjectsAsList().iterator(), typeId);
					while (scriptsIterator.hasNext())
					{
						IPersist script = scriptsIterator.next();
						if (script instanceof IScriptProvider)
						{
							scripts.add((IScriptProvider)script);
						}
					}
				}
				return scripts.toArray();
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
		return null;
	}

	@Override
	public Object getParent(Object element)
	{
		if (element instanceof IScriptProvider)
		{
			return ((IScriptProvider)element).getAncestor(IRepository.SOLUTIONS);
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element)
	{
		return (element instanceof Solution);
	}
}

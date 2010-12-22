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
package com.servoy.eclipse.ui.editors.table;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;

public class AggregationContentProvider implements ITreeContentProvider
{
	private final Table table;

	public AggregationContentProvider(Table t)
	{
		this.table = t;
	}

	public Object[] getChildren(Object parentElement)
	{
		if (parentElement instanceof Solution)
		{
			Solution solution = (Solution)parentElement;
			ArrayList aggregates = new ArrayList();
			try
			{
				Iterator it = solution.getAggregateVariables(table, true);
				while (it.hasNext())
				{
					AggregateVariable element = (AggregateVariable)it.next();
					aggregates.add(element);
				}
				return aggregates.toArray();
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
		return null;
	}

	public Object getParent(Object element)
	{
		if (element instanceof AggregateVariable)
		{
			return ((AggregateVariable)element).getAncestor(IRepository.SOLUTIONS);
		}
		return null;
	}

	public boolean hasChildren(Object element)
	{
		if (element instanceof Solution)
		{
			return true;
		}
		return false;
	}

	public Object[] getElements(Object inputElement)
	{
		if (inputElement instanceof ArrayList)
		{
			return ((ArrayList)inputElement).toArray();
		}
		return new Object[0];
	}

	public void dispose()
	{

	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{

	}

}

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

import org.eclipse.core.databinding.observable.AbstractObservable;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.ChangeSupport;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.MethodDialog.MethodListOptions;
import com.servoy.eclipse.ui.editors.MethodCellEditor;
import com.servoy.eclipse.ui.editors.table.EventsComposite.EventNode;
import com.servoy.eclipse.ui.labelproviders.MethodLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.MethodPropertyController.MethodValueEditor;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;

public class EventsMethodEditingSupport extends EditingSupport
{
	private final Table table;
	private final IObservable observable;

	public EventsMethodEditingSupport(TreeViewer viewer, Table table)
	{
		super(viewer);
		this.table = table;
		changeSupport = new ChangeSupport(Realm.getDefault())
		{
			@Override
			protected void lastListenerRemoved()
			{
			}

			@Override
			protected void firstListenerAdded()
			{
			}
		};
		observable = new AbstractObservable(Realm.getDefault())
		{
			@Override
			public void addChangeListener(IChangeListener listener)
			{
				changeSupport.addChangeListener(listener);
			}

			@Override
			public void removeChangeListener(IChangeListener listener)
			{
				changeSupport.removeChangeListener(listener);
			}

			public boolean isStale()
			{
				return false;
			}
		};
	}

	@Override
	protected Object getValue(Object element)
	{
		EventNode node = (EventNode)element;
		if (node.isSolution()) return null;
		return node.getMethodWithArguments();
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		EventNode node = (EventNode)element;
		try
		{
			PersistPropertySource persistProperties = new PersistPropertySource(node.getSolution().getOrCreateTableNode(table.getDataSource()),
				node.getSolution(), false);
			persistProperties.setPropertyValue(node.getType().getProperty().getPropertyName(), value);
			if (persistProperties.getPersist().isChanged())
			{
				changeSupport.fireEvent(new ChangeEvent(observable));
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Could not save event method", e);
		}
		node.setMethodWithArguments((MethodWithArguments)value);
		getViewer().refresh(node);
	}

	private final ChangeSupport changeSupport;

	public void addChangeListener(IChangeListener listener)
	{
		observable.addChangeListener(listener);
	}

	public void removeChangeListener(IChangeListener listener)
	{
		observable.removeChangeListener(listener);
	}

	@Override
	protected CellEditor getCellEditor(Object element)
	{
		MethodCellEditor editor = null;
		EventNode node = (EventNode)element;
		if (!node.isSolution())
		{
			Solution solution = node.getSolution();
			TableNode tableNode;
			try
			{
				tableNode = solution.getOrCreateTableNode(table.getDataSource());
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
				return null;
			}
			PersistContext persistContext = PersistContext.create(tableNode, null);
			editor = new MethodCellEditor(((TreeViewer)getViewer()).getTree(), new SolutionContextDelegateLabelProvider(new MethodLabelProvider(persistContext,
				false, true), solution), new MethodValueEditor(persistContext), persistContext, node.getType().getProperty().getPropertyName(), false,
				new MethodListOptions(false, true, false, true, true, table));
		}
		return editor;
	}

	@Override
	protected boolean canEdit(Object element)
	{
		return !((EventNode)element).isSolution();
	}

}

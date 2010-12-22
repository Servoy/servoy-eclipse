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

import java.util.Iterator;

import org.eclipse.core.databinding.observable.AbstractObservable;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.ChangeSupport;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.IdentDocumentValidator;
import com.servoy.j2db.util.ValidatingDocument.IDocumentValidator;

public class CalculationNameEditingSupport extends EditingSupport
{
	private final ComboBoxCellEditor editor;
	private String[] columns;
	private final Table table;
	private final IObservable observable;

	public CalculationNameEditingSupport(TreeViewer viewer, Table table)
	{
		super(viewer);
		this.table = table;
		updateColumns();
		editor = new ComboBoxCellEditor(viewer.getTree(), columns, SWT.NONE);
		CCombo combo = (CCombo)editor.getControl();
		combo.addVerifyListener(new DocumentValidatorVerifyListener(new IDocumentValidator[] { new IdentDocumentValidator(IdentDocumentValidator.TYPE_SQL) }));
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
	protected boolean canEdit(Object element)
	{
		if (element instanceof Solution) return false;
		else return true;
	}

	@Override
	protected CellEditor getCellEditor(Object element)
	{
		updateColumns();
		CCombo combo = (CCombo)editor.getControl();
		combo.setItems(columns);
		return editor;
	}

	private void updateColumns()
	{
		columns = new String[table.getColumnCount() + 1];
		columns[0] = "type_here";
		int i = 1;
		Iterator<Column> it = table.getColumnsSortedByName();
		while (it.hasNext())
		{
			Column column = it.next();
			columns[i++] = column.getName();
		}
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof ScriptCalculation)
		{
			ScriptCalculation calculation = (ScriptCalculation)element;
			String name = calculation.getName();
			CCombo combo = (CCombo)editor.getControl();
			int index = -1;
			for (int i = 0; i < columns.length; i++)
			{
				if (columns[i].equals(name))
				{
					index = i;
					break;
				}
			}
			if (index >= 0)
			{
				return new Integer(index);
			}
			else
			{
				combo.removeAll();
				combo.setItems(columns);
				combo.setItem(0, name);
				return 0;
			}

		}
		return 0;
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof ScriptCalculation)
		{
			IValidateName nameValidator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
			CCombo combo = (CCombo)editor.getControl();
			ScriptCalculation calculation = (ScriptCalculation)element;
			try
			{
				if (!calculation.getName().equals(combo.getText()))
				{
					calculation.updateName(nameValidator, combo.getText());
					changeSupport.fireEvent(new ChangeEvent(observable));
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
				MessageDialog.openError(this.getViewer().getControl().getShell(), "Error", "Error while saving: " + e.getMessage());
			}

			getViewer().update(element, null);
		}

	}
}

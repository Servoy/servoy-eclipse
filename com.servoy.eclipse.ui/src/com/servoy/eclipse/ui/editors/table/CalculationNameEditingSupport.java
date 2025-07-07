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

import static com.servoy.eclipse.core.ServoyModelManager.getServoyModelManager;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;
import com.servoy.j2db.util.docvalidator.ValidatingDocument.IDocumentValidator;

/**
 * Edit calculation name in table editor.
 *
 * @author lvostinar
 *
 */
public class CalculationNameEditingSupport extends EditingSupport
{
	private final ComboBoxCellEditor editor;
	private String[] columns;
	private final ITable table;
	private final ChangeSupportObservable observable;

	public CalculationNameEditingSupport(TreeViewer viewer, ITable table)
	{
		super(viewer);
		this.table = table;
		updateColumns();
		editor = new ComboBoxCellEditor(viewer.getTree(), columns, SWT.NONE);
		CCombo combo = (CCombo)editor.getControl();
		combo.addVerifyListener(new DocumentValidatorVerifyListener(new IDocumentValidator[] { new IdentDocumentValidator(IdentDocumentValidator.TYPE_SQL) }));
		observable = new ChangeSupportObservable(new SimpleChangeSupport());
	}

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
		return !(element instanceof Solution);
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
		Iterator<Column> it = EditorUtil.getTableColumns(table, true);
		while (it.hasNext())
		{
			columns[i++] = it.next().getName();
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
			for (int i = 0; i < columns.length; i++)
			{
				if (columns[i].equals(name))
				{
					return Integer.valueOf(i);
				}
			}

			combo.removeAll();
			combo.setItems(columns);
			combo.setItem(0, name);
		}
		return Integer.valueOf(0);
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof ScriptCalculation)
		{
			IValidateName nameValidator = getServoyModelManager().getServoyModel().getNameValidator();
			CCombo combo = (CCombo)editor.getControl();
			ScriptCalculation calculation = (ScriptCalculation)element;
			try
			{
				if (!calculation.getName().equals(combo.getText()))
				{
					if (calculation.getTable().getColumnType(combo.getText()) != 0)
					{
						int type = calculation.getTable().getColumnType(combo.getText());
						Matcher m = Pattern.compile("return+.+;").matcher(calculation.getDeclaration());
						if (m.find() && m.group().equals(ScriptCalculation.getDefaultReturnMethodString(calculation.getType())))
						{
							calculation.setDeclaration(Pattern.compile("return+.+;").matcher(calculation.getDeclaration()).replaceFirst(
								ScriptCalculation.getDefaultReturnMethodString(type)));
						}
						if (type != calculation.getType())
						{
							calculation.setType(type);
						}
					}
					calculation.updateName(nameValidator, combo.getText());
					observable.fireChangeEvent();
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

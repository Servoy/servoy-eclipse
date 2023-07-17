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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.ui.util.FixedComboBoxCellEditor;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ScriptCalculation;

public class CalculationTypeEditingSupport extends EditingSupport
{
	private final CellEditor editor;
	private final ChangeSupportObservable observable;

	public CalculationTypeEditingSupport(TreeViewer tv)
	{
		super(tv);
		String[] types = new String[Column.allDefinedTypes.length];
		for (int i = 0; i < types.length; i++)
		{
			types[i] = Column.getDisplayTypeString(Column.allDefinedTypes[i]);
		}
		editor = new FixedComboBoxCellEditor(tv.getTree(), types, SWT.READ_ONLY);
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
	protected void setValue(Object element, Object value)
	{
		if (element instanceof ScriptCalculation)
		{
			ScriptCalculation calculation = (ScriptCalculation)element;
			int index = Integer.parseInt(value.toString());
			int type = calculation.getType();
			calculation.setTypeAndCheck(Column.allDefinedTypes[index], Activator.getDefault().getDesignClient());
			Matcher m = Pattern.compile("return+.+;").matcher(calculation.getDeclaration());

			if (m.find() && m.group().equals(ScriptCalculation.getDefaultReturnMethodString(type)))
			{
				calculation.setDeclaration(Pattern.compile("return+.+;").matcher(calculation.getDeclaration()).replaceFirst(
					ScriptCalculation.getDefaultReturnMethodString(calculation.getType())));
			}
			observable.fireChangeEvent();
			getViewer().update(element, null);
		}
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof ScriptCalculation)
		{
			ScriptCalculation calculation = (ScriptCalculation)element;
			int type = calculation.getType();
			int index = 0;
			for (int i = 0; i < Column.allDefinedTypes.length; i++)
			{
				if (Column.allDefinedTypes[i] == type)
				{
					index = i;
					break;
				}
			}
			return new Integer(index);
		}
		return null;
	}

	@Override
	protected CellEditor getCellEditor(Object element)
	{
		return editor;
	}

	@Override
	protected boolean canEdit(Object element)
	{
		if (element instanceof ScriptCalculation) return true;
		else return false;
	}
}

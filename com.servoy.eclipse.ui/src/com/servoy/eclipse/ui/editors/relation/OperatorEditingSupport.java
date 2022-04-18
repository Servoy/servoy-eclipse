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
package com.servoy.eclipse.ui.editors.relation;

import java.util.ArrayList;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;

import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.eclipse.ui.editors.RelationEditor;
import com.servoy.eclipse.ui.util.FixedComboBoxCellEditor;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.util.Utils;

public class OperatorEditingSupport extends EditingSupport
{
	private final ComboBoxCellEditor editor;
	private final RelationEditor relationEditor;
	private final ArrayList<String> items;

	public OperatorEditingSupport(RelationEditor re, TableViewer tv)
	{
		super(tv);
		relationEditor = re;
		items = new ArrayList<String>();
		for (int element : RelationItem.RELATION_OPERATORS)
		{
			if ((element & IBaseSQLCondition.OPERATOR_MASK) != IBaseSQLCondition.IN_OPERATOR)
			{
				items.add(RelationItem.getOperatorAsString(element));
			}
		}
		editor = new FixedComboBoxCellEditor(tv.getTable(), items.toArray(new String[] { }), SWT.READ_ONLY);
		if (editor.getControl() instanceof CCombo)
		{
			//((CCombo)editor.getControl()).setToolTipText("Special modifiers:\n\n# - case insensitive condition\n^|| - is null condition\n\nExample:\n\n#= - case insensitive equals\n^||= - equals or null\n^||#!= - case insensitive not equals or null");
		}
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof RelationRow)
		{
			RelationRow pi = (RelationRow)element;
			int listIndex = Utils.getAsInteger(value);
			String operatorString = items.get(listIndex);
			int validOperator = RelationItem.getValidOperator(operatorString, RelationItem.RELATION_OPERATORS, null);
			if (validOperator != -1)
			{
				Integer previousValue = pi.getOperator();
				Integer currentValue = Integer.valueOf(validOperator);
				pi.setOperator(currentValue);
				if (!Utils.equalObjects(previousValue, currentValue) && !(previousValue == null && currentValue.intValue() == 0))
					relationEditor.flagModified(true);
			}
			getViewer().update(element, null);
		}
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof RelationRow)
		{
			RelationRow pi = (RelationRow)element;
			Integer operatorCode = pi.getOperator() != null ? pi.getOperator() : new Integer(0);
			String operatorString = RelationItem.getOperatorAsString(operatorCode.intValue());
			return new Integer(items.indexOf(operatorString));
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
		if (element instanceof RelationRow && editor != null)
		{
			return relationEditor.canEdit(element);
		}
		return false;
	}
}

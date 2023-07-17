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

import static com.servoy.base.query.IBaseSQLCondition.IN_OPERATOR;
import static com.servoy.base.query.IBaseSQLCondition.OPERATOR_MASK;
import static com.servoy.j2db.persistence.RelationItem.RELATION_OPERATORS;
import static com.servoy.j2db.util.Utils.getAsInteger;
import static com.servoy.j2db.util.Utils.indexOf;
import static java.util.Arrays.stream;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;

import com.servoy.eclipse.ui.editors.RelationEditor;
import com.servoy.eclipse.ui.util.FixedComboBoxCellEditor;
import com.servoy.j2db.persistence.RelationItem;

public class OperatorEditingSupport extends EditingSupport
{
	private final ComboBoxCellEditor editor;
	private final RelationEditor relationEditor;
	private final int[] values;

	public OperatorEditingSupport(RelationEditor re, TableViewer tv)
	{
		super(tv);
		relationEditor = re;

		values = stream(RELATION_OPERATORS)
			.filter(element -> element != IN_OPERATOR && (element & OPERATOR_MASK) == element)
			.toArray();
		String[] items = stream(values)
			.mapToObj(OperatorEditingSupport::getColumnText)
			.toArray(String[]::new);

		editor = new FixedComboBoxCellEditor(tv.getTable(), items, SWT.READ_ONLY);
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof RelationRow)
		{
			RelationRow pi = (RelationRow)element;

			int newValue = values[getAsInteger(value)];
			int previousValue = pi.getMaskedOperator();
			if (previousValue != newValue)
			{
				if (indexOf(RELATION_OPERATORS, newValue | pi.getMask()) < 0)
				{
					// combination with mask is not valid, clear mask
					pi.setMask(0);
				}
				relationEditor.flagModified(true);

				pi.setMaskedOperator(newValue);
			}
		}
		getViewer().update(element, null);
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof RelationRow)
		{
			RelationRow pi = (RelationRow)element;
			int index = indexOf(values, pi.getMaskedOperator());
			if (index >= 0)
			{
				return Integer.valueOf(index);
			}
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

	public static String getColumnText(int maskedOperator)
	{
		return RelationItem.getOperatorAsString(maskedOperator);
	}
}

/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

import static com.servoy.base.query.IBaseSQLCondition.ALL_MODIFIERS;
import static com.servoy.base.query.IBaseSQLCondition.CASEINSENTITIVE_MODIFIER;
import static com.servoy.base.query.IBaseSQLCondition.OPERATOR_MASK;
import static com.servoy.base.query.IBaseSQLCondition.ORNULL_MODIFIER;
import static com.servoy.j2db.persistence.RelationItem.RELATION_OPERATORS;
import static com.servoy.j2db.util.Utils.getAsInteger;
import static com.servoy.j2db.util.Utils.indexOf;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.util.Objects;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;

import com.servoy.eclipse.ui.editors.RelationEditor;
import com.servoy.eclipse.ui.util.FixedComboBoxCellEditor;

public class OperatorMaskEditingSupport extends EditingSupport
{
	private static final String OR_IS_NULL_MODIFIER_STRING = "or-is-null";
	private static final String CASE_INSENSITIVE_MODIFIER_STRING = "case-insensitive";

	private final ComboBoxCellEditor editor;
	private final RelationEditor relationEditor;
	private final int[] values;

	public OperatorMaskEditingSupport(RelationEditor re, TableViewer tv)
	{
		super(tv);
		relationEditor = re;

		values = stream(RELATION_OPERATORS)
			.map(element -> element & ~OPERATOR_MASK)
			.distinct().toArray();
		String[] items = stream(values)
			.mapToObj(OperatorMaskEditingSupport::getColumnText)
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

			if (indexOf(RELATION_OPERATORS, newValue | pi.getMaskedOperator()) >= 0) // check if combination is valid
			{
				int previousValue = pi.getMask();
				if (previousValue != newValue)
				{
					relationEditor.flagModified(true);
				}
				pi.setMask(newValue);
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
			int index = indexOf(values, pi.getMask());
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

	public static String getColumnText(int mask)
	{
		return stream(ALL_MODIFIERS)
			.filter(elem -> (elem & mask) != 0)
			.mapToObj(value -> {
				switch (value)
				{
					case ORNULL_MODIFIER :
						return OR_IS_NULL_MODIFIER_STRING;
					case CASEINSENTITIVE_MODIFIER :
						return CASE_INSENSITIVE_MODIFIER_STRING;
				}
				return null;
			})
			.filter(Objects::nonNull)
			.collect(joining(", "));
	}

	public static String getTooltip()
	{
		return "Modifier to relation operator\n" +
			OR_IS_NULL_MODIFIER_STRING + ": select ... where from_column <op> to_column OR to_colum IS NULL\n" +
			CASE_INSENSITIVE_MODIFIER_STRING + ": compare from_column and to_column in a case insensitive way\n";
	}
}

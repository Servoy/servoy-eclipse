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

package com.servoy.eclipse.ui.dialogs.autowizard;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 *
 * @author emera
 */
final class DataproviderCellEditor extends EditingSupport
{
	private final TableViewer tableViewer;
	private final PropertyDescription dp;
	private final CheckboxCellEditor checkboxCellEditor;
	private final List<PropertyDescription> dataproviderProperties;

	DataproviderCellEditor(TableViewer autoWizardConfigurationViewer, ColumnViewer viewer, int style, PropertyDescription dp,
		List<PropertyDescription> dataproviderProperties)
	{
		super(viewer);
		this.tableViewer = autoWizardConfigurationViewer;
		this.dp = dp;
		this.dataproviderProperties = dataproviderProperties;
		checkboxCellEditor = new CheckboxCellEditor(this.tableViewer.getTable(), style);
	}

	@Override
	protected CellEditor getCellEditor(Object element)
	{
		return checkboxCellEditor;
	}

	@Override
	protected boolean canEdit(Object element)
	{
		return true;
	}

	@Override
	protected Object getValue(Object element)
	{
		Pair<String, Map<String, Object>> row = (Pair<String, Map<String, Object>>)element;
		return row.getRight().get(dp.getName()) != null; //TODO check equals pd.getDefaultValue()?
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		Pair<String, Map<String, Object>> row = (Pair<String, Map<String, Object>>)element;
		Map<String, Object> rowValue = row.getRight();
		if (Utils.getAsBoolean(value))
		{
			for (PropertyDescription pd : dataproviderProperties)
			{
				if (!pd.getName().equals(dp.getName()))
				{
					rowValue.put(pd.getName(), pd.getDefaultValue());
				}
				else
				{
					String dpValue = row.getLeft();
					rowValue.put(dp.getName(), dpValue);
				}
			}
		}
		getViewer().update(element, null);
	}
}
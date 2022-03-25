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

package com.servoy.eclipse.ui.dialogs;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * @author emera
 */
public class WizardConfigurationViewer extends TableViewer
{
	private final class PropertyCellEditor extends EditingSupport
	{

		private final PropertyDescription dp;
		private final CheckboxCellEditor checkboxCellEditor;
		private final List<PropertyDescription> dataproviderProperties;

		private PropertyCellEditor(ColumnViewer viewer, Composite parent, int style, PropertyDescription dp, List<PropertyDescription> dataproviderProperties)
		{
			super(viewer);
			this.dp = dp;
			this.dataproviderProperties = dataproviderProperties;
			checkboxCellEditor = new CheckboxCellEditor(parent, style);
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

	public WizardConfigurationViewer(Composite parent, List<PropertyDescription> dataproviderProperties, int style)
	{
		super(parent, style);
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		parent.setLayout(tableColumnLayout);
		getTable().setLinesVisible(true);
		getTable().setHeaderVisible(true);
		getTable().setToolTipText("The selected columns");
		setContentProvider(ArrayContentProvider.getInstance());

		TableColumn dataproviderColumn = new TableColumn(getTable(), SWT.LEFT);
		dataproviderColumn.setText("Columns");
		dataproviderColumn.setToolTipText("The dataprovider for which a column is created");

		TableViewerColumn dataproviderViewerColumn = new TableViewerColumn(this, dataproviderColumn);
		dataproviderViewerColumn.setLabelProvider(new ColumnLabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				Pair<String, Map<String, Object>> row = (Pair<String, Map<String, Object>>)element;
				return row.getLeft();
			}
		});
		tableColumnLayout.setColumnData(dataproviderColumn, new ColumnWeightData(40, 100, true));

		for (PropertyDescription dp : dataproviderProperties)
		{
			TableColumn col = new TableColumn(getTable(), SWT.CENTER);
			col.setText(dp.getName());
			col.setToolTipText(dp.getDocumentation());

			TableViewerColumn colViewer = new TableViewerColumn(this, col);
			colViewer.setEditingSupport(new PropertyCellEditor(this, parent, style, dp, dataproviderProperties));
			colViewer.setLabelProvider(new ColumnLabelProvider()
			{
				@Override
				public String getText(Object element)
				{
					return "";
				}

				@Override
				public Image getImage(Object element)
				{
					Pair<String, Map<String, Object>> row = (Pair<String, Map<String, Object>>)element;
					return row.getRight().get(dp.getName()) != null ? com.servoy.eclipse.ui.editors.table.ColumnLabelProvider.TRUE_RADIO
						: com.servoy.eclipse.ui.editors.table.ColumnLabelProvider.FALSE_RADIO;
				}
			});
			tableColumnLayout.setColumnData(col, new ColumnWeightData(40, 100, true));
		}
	}
}
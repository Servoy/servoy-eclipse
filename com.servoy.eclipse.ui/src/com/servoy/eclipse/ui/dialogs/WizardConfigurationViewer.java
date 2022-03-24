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

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.util.Pair;

/**
 * @author emera
 */
public class WizardConfigurationViewer extends TableViewer
{

	public WizardConfigurationViewer(Composite parent, int style)
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
				Pair<IDataProvider, Object> array = (Pair<IDataProvider, Object>)element;
				return array.getLeft().toString();
			}
		});
		tableColumnLayout.setColumnData(dataproviderColumn, new ColumnWeightData(40, 100, true));

		String[] dps = new String[] { "dataprovider1", "dataprovider2" }; //TODO PropertyDescription[]
		for (String dp : dps)
		{
			TableColumn col = new TableColumn(getTable(), SWT.CENTER);
			col.setText(dp); //TODO pd.getPropertyName ?
			col.setToolTipText("The dataprovider for which a column is created"); //TODO pd.getDescription()

			TableViewerColumn colViewer = new TableViewerColumn(this, dataproviderColumn);
			//TODO RADIO CELL EDITOR
			colViewer.setLabelProvider(new ColumnLabelProvider()
			{
				@Override
				public String getText(Object element)
				{
					return dp;
				}
			});
			tableColumnLayout.setColumnData(col, new ColumnWeightData(40, 100, true));
		}
	}
}